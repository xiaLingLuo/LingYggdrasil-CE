/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.migration.cli;

import im.xz.cn.config.DatabaseConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.SchemaBaseline;
import im.xz.cn.infrastructure.sql.SqlDialects;
import im.xz.cn.migration.MigrationReport;
import im.xz.cn.migration.detection.LegacySchemaDetector;
import im.xz.cn.migration.plan.MigrationPlan;
import im.xz.cn.migration.plan.MigrationPlanner;
import im.xz.cn.migration.steps.texture.TextureMigrationStep;
import im.xz.cn.migration.verification.TextureReconciliation;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;


public final class MigratorCli {

    private MigratorCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream out) {
        Map<String, String> options = parse(args);
        String command = options.get("command");
        if (command == null || "help".equals(command) || "--help".equals(command)) {
            printUsage(out);
            return command == null ? 2 : 0;
        }
        DatabaseConfig config = resolveConfig(options, out);
        if (config == null) {
            printUsage(out);
            return 2;
        }

        DatabaseManager database = new DatabaseManager(config);
        try {
            return switch (command) {
                case "detect" -> {
                    out.print(new LegacySchemaDetector(database).detect().toText());
                    yield 0;
                }
                case "plan" -> {
                    MigrationPlan plan = new MigrationPlanner(database)
                            .plan(options.containsKey("keyfile"));
                    out.print(plan.toText());
                    String outFile = options.get("out");
                    if (outFile != null && !outFile.isBlank()) {
                        try {
                            java.nio.file.Files.writeString(java.nio.file.Path.of(outFile), plan.toText());
                            out.println("plan written to " + outFile);
                        } catch (java.io.IOException error) {
                            out.println("cannot write plan: " + error.getMessage());
                            yield 2;
                        }
                    }
                    yield 0;
                }
                case "texture" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new TextureMigrationStep(database).run();
                    out.print(report.toText());
                    yield report.hasFailures() ? 1 : 0;
                }
                case "profiles" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.texture
                            .ProfileBindingMigrationStep(database).run();
                    out.print(report.toText());
                    yield report.hasFailures() ? 1 : 0;
                }
                case "secrets" -> {
                    String keyfile = options.get("keyfile");
                    if (keyfile == null || keyfile.isBlank()) {
                        out.println("missing --keyfile <path> (legacy .lingkey)");
                        yield 2;
                    }
                    byte[] key;
                    try {
                        key = java.util.Base64.getDecoder().decode(
                                java.nio.file.Files.readString(java.nio.file.Path.of(keyfile)).trim());
                    } catch (java.io.IOException error) {
                        out.println("cannot read keyfile: " + error.getMessage());
                        yield 2;
                    }
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.configuration
                            .SecretMigrationStep(database, key).run();
                    out.print(report.toText());
                    yield 0;
                }
                case "config" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.configuration
                            .SettingsMigrationStep(database).run();
                    out.print(report.toText());
                    yield 0;
                }
                case "identity" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.identity
                            .PermissionGroupMigrationStep(database).run();
                    out.print(report.toText());
                    yield 0;
                }
                case "social" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.social
                            .SocialMigrationStep(database).run();
                    out.print(report.toText());
                    yield 0;
                }
                case "sessions" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport report = new im.xz.cn.migration.steps.sessions
                            .SessionInvalidationStep(database).run();
                    out.print(report.toText());
                    yield 0;
                }
                case "all" -> {
                    new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    MigrationReport texture = new TextureMigrationStep(database).run();
                    MigrationReport profiles = new im.xz.cn.migration.steps.texture
                            .ProfileBindingMigrationStep(database).run();
                    MigrationReport configReport = new im.xz.cn.migration.steps.configuration
                            .SettingsMigrationStep(database).run();
                    MigrationReport identity = new im.xz.cn.migration.steps.identity
                            .PermissionGroupMigrationStep(database).run();
                    MigrationReport social = new im.xz.cn.migration.steps.social
                            .SocialMigrationStep(database).run();
                    MigrationReport sessions = new im.xz.cn.migration.steps.sessions
                            .SessionInvalidationStep(database).run();
                    for (MigrationReport report : new MigrationReport[]{
                            texture, profiles, configReport, identity, social, sessions}) {
                        out.print(report.toText());
                    }
                    out.println("note: secrets 需单独执行并提供 --keyfile");
                    boolean failed = texture.hasFailures() || profiles.hasFailures()
                            || configReport.hasFailures() || identity.hasFailures()
                            || social.hasFailures() || sessions.hasFailures();
                    yield failed ? 1 : 0;
                }
                case "verify" -> {
                    
                    if (options.containsKey("fix-schema")) {
                        new SchemaBaseline(SqlDialects.of(database.getDbType())).apply(database);
                    }
                    TextureReconciliation.Result result = new TextureReconciliation(database).reconcile();
                    out.print(result.toText());
                    yield result.ok() ? 0 : 1;
                }
                default -> {
                    out.println("unknown command: " + command);
                    printUsage(out);
                    yield 2;
                }
            };
        } catch (RuntimeException error) {
            out.println("ERROR: " + error.getMessage());
            error.printStackTrace(out);
            return 1;
        } finally {
            database.close();
        }
    }

    
    static DatabaseConfig resolveConfig(Map<String, String> options, PrintStream out) {
        String type = options.getOrDefault("db", "sqlite").trim().toLowerCase(java.util.Locale.ROOT);
        DatabaseConfig config = new DatabaseConfig();
        if ("sqlite".equals(type)) {
            String path = options.get("sqlite");
            if (path == null || path.isBlank()) {
                out.println("SQLite requires --sqlite <path> (or use --db mysql)");
                return null;
            }
            config.setType("sqlite");
            config.setSqlitePath(path);
            return config;
        }
        if ("mysql".equals(type)) {
            String database = options.get("database");
            if (database == null || database.isBlank()) {
                out.println("MySQL requires --database <schema>");
                return null;
            }
            int port = 3306;
            String portText = options.get("port");
            if (portText != null && !portText.isBlank()) {
                try {
                    port = Integer.parseInt(portText.trim());
                } catch (NumberFormatException error) {
                    out.println("invalid --port: " + portText);
                    return null;
                }
            }
            config.setType("mysql");
            config.setHost(options.getOrDefault("host", "localhost"));
            config.setPort(port);
            config.setDatabase(database);
            config.setUsername(options.get("user"));
            config.setPassword(options.get("password"));
            return config;
        }
        out.println("unsupported database type: " + type + " (supported: sqlite, mysql)");
        return null;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.isBlank()) {
                continue;
            }
            if (arg.startsWith("--")) {
                int equals = arg.indexOf('=');
                if (equals > 2) {
                    options.put(arg.substring(2, equals), arg.substring(equals + 1));
                } else {
                    String key = arg.substring(2);
                    String value = (i + 1 < args.length && !args[i + 1].startsWith("--"))
                            ? args[++i] : "true";
                    options.put(key, value);
                }
            } else if (!options.containsKey("command")) {
                options.put("command", arg);
            }
        }
        return options;
    }

    private static void printUsage(PrintStream out) {
        out.println("LingYggdrasil migrator");
        out.println("  usage: <detect|plan|texture|profiles|identity|social|config|secrets|sessions|all|verify>");
        out.println("         (--sqlite <path> | --db mysql --host <h> --port <p> --database <schema> --user <u> --password <p>) [--keyfile <f>] [--out <f>]");
        out.println("  detect   只读检测旧结构与数据量");
        out.println("  plan     输出迁移计划（dry-run，不写库；--out <file> 可落盘）");
        out.println("  texture  执行纹理迁移（幂等，可重复执行）");
        out.println("  profiles 迁移角色纹理绑定（skin_url/hash -> binding）");
        out.println("  identity 迁移用户权限组与权限节点");
        out.println("  config   迁移旧 system_settings 到类型化配置组");
        out.println("  secrets  迁移旧 ENC: 密文到 secret_store（需 --keyfile 指向旧 .lingkey）");
        out.println("  social   迁移好友/申请/拉黑关系");
        out.println("  sessions 清除旧会话令牌（升级后需重新登录）");
        out.println("  all      依次执行 texture/profiles/config/identity/social/sessions");
        out.println("  verify   对账并输出缺失明细；存在缺失时退出码 1");
    }
}
