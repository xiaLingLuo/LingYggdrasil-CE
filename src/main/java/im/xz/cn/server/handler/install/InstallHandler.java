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
package im.xz.cn.server.handler.install;


import im.xz.cn.i18n.I18n;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.DatabaseConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.Admin;
import im.xz.cn.security.PasswordValidator;
import im.xz.cn.server.InstallServer;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.web.view.InstallPage;
import im.xz.cn.rate.InstallRateLimiter;
import io.javalin.http.Context;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import im.xz.cn.logging.logApi;

public class InstallHandler {
    private static final logApi logger = logApi.getLogger(InstallHandler.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static String installToken;
    private static InstallServer installServer;
    private static final InstallRateLimiter rateLimiter = new InstallRateLimiter();

    public static void setInstallToken(String token) {
        installToken = token;
    }

    public static void setInstallServer(InstallServer server) {
        installServer = server;
    }

    private static boolean validateToken(String token) {
        return installToken != null && installToken.equals(token);
    }

    public static void renderInstallPage(Context ctx) {
        if (AppConfig.getInstance().isInstalled()) {
            ctx.status(403).result(I18n.t("msg.installInstalled"));
            return;
        }

        ctx.html(InstallPage.generateInstallPage(installToken));
    }

    public static void getStatus(Context ctx) {
        if (AppConfig.getInstance().isInstalled()) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.systemInstalled")));
            return;
        }

        String token = ctx.header("X-Install-Token");
        if (!validateToken(token)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.invalidInstallToken")));
            return;
        }

        ctx.json(Map.of("installed", false));
    }

    public static void doInstall(Context ctx) {
        if (AppConfig.getInstance().isInstalled()) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.systemInstalled")));
            return;
        }

        String token = ctx.header("X-Install-Token");
        if (!validateToken(token)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.invalidInstallToken")));
            return;
        }

        String clientIp = ctx.ip();
        if (!rateLimiter.allow(clientIp)) {
            ctx.status(429).json(Map.of("success", false, "message", I18n.t("msg.installTooFrequent")));
            return;
        }

        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.requestBodyError")));
            return;
        }

        String rootUsername = textOrNull(body, "rootUsername");
        String rootPassword = textOrNull(body, "rootPassword");
        String rootEmail    = textOrNull(body, "rootEmail");
        String dbType       = textOrNull(body, "dbType");

        if (rootUsername == null || rootUsername.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.adminUsernameEmpty")));
            return;
        }
        String passwordError = PasswordValidator.validate(rootPassword);
        if (passwordError != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.adminPasswordError", passwordError)));
            return;
        }
        if (rootEmail == null || !rootEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.adminEmailInvalid")));
            return;
        }
        if ("pgsql".equals(dbType)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.pgsqlUnavailable")));
            return;
        }
        if (dbType == null || (!dbType.equals("sqlite") && !dbType.equals("mysql"))) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidDbType")));
            return;
        }

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setType(dbType);
        if ("sqlite".equals(dbType)) {
            String sqlitePath = textOrDefault(body, "sqlitePath", "./data.db");

            try {
                Path rawPath = Paths.get(sqlitePath);
                if (rawPath.isAbsolute()) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.sqliteAbsolutePath")));
                    return;
                }
                String userDir = System.getProperty("user.dir");
                Path projectPath = Paths.get(userDir).toAbsolutePath().normalize();
                Path normalizedPath = projectPath.resolve(sqlitePath).toAbsolutePath().normalize();

                if (!normalizedPath.startsWith(projectPath)) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.sqlitePathTraversal")));
                    return;
                }
            } catch (Exception e) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.sqlitePathInvalid")));
                return;
            }

            dbConfig.setSqlitePath(sqlitePath);
        } else {
            String dbHost = textOrDefault(body, "dbHost", "localhost");
            int dbPort    = intOrDefault(body, "dbPort", "mysql".equals(dbType) ? 3306 : 5432);
            String dbName = textOrDefault(body, "dbName", "yggdrasil");
            String dbUser = textOrNull(body, "dbUsername");
            String dbPass = textOrNull(body, "dbPassword");
            dbConfig.setHost(dbHost);
            dbConfig.setPort(dbPort);
            dbConfig.setDatabase(dbName);
            dbConfig.setUsername(dbUser != null ? dbUser : "");
            dbConfig.setPassword(dbPass != null ? dbPass : "");
        }

        boolean emailEnabled = body.has("emailEnabled") && body.get("emailEnabled").asBoolean();
        MailConfig mailConfig = new MailConfig();
        mailConfig.setEnabled(emailEnabled);
        if (emailEnabled) {
            mailConfig.setHost(textOrDefault(body, "emailHost", ""));
            mailConfig.setPort(intOrDefault(body, "emailPort", 587));
            mailConfig.setUsername(textOrDefault(body, "emailUsername", ""));
            mailConfig.setPassword(textOrDefault(body, "emailPassword", ""));
            mailConfig.setFrom(textOrDefault(body, "emailFrom", ""));
        }

        DatabaseManager dbManager;
        try {
            dbManager = new DatabaseManager(dbConfig);
            try (Connection conn = dbManager.getConnection()) {
                assert true;
            }catch (NullPointerException ignored){

            }
        } catch (Exception e) {
            logger.error("Database connection failed: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false,
                "message", I18n.t("msg.dbConnectFailed")));
            return;
        }

        try {
            dbManager.initializeSchema();

            String adminId    = UuidUtil.generateAdminUuid();
            String passwordHash = Argon2Hasher.hash(rootPassword);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            im.xz.cn.model.RootInfo root = new im.xz.cn.model.RootInfo(adminId, rootUsername, rootEmail, passwordHash, now);
            new im.xz.cn.database.dao.RootInfoDao(dbManager).insert(root);

            SystemConfig sysConfig = SystemConfig.getInstance();
            sysConfig.setSiteName("泠 Yggdrasil");
            sysConfig.setRegistrationEnabled(true);
            sysConfig.setEmailVerificationEnabled(emailEnabled);
            sysConfig.setUuidVersion("v4");
            sysConfig.setEncryptionLevel(1);
            sysConfig.setInstalledAt(now);
            sysConfig.saveToDatabase(dbManager);

            AppConfig appConfig = AppConfig.getInstance();
            appConfig.setDatabaseConfig(dbConfig);
            appConfig.setMailConfig(mailConfig);
            appConfig.saveConfig();

            appConfig.createInstalledFile();

            InstallServer.deleteTokenFile();
            if (installServer != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    installServer.stop();
                }).start();
            }

            ctx.json(Map.of("success", true, "message", I18n.t("msg.installSuccessLong")));
        } catch (Exception e) {
            logger.error("Installation failed: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false,
                "message", I18n.t("msg.installError")));
        } finally {
            dbManager.close();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private static String textOrDefault(JsonNode node, String field, String def) {
        String v = textOrNull(node, field);
        return (v != null && !v.isBlank()) ? v : def;
    }

    private static int intOrDefault(JsonNode node, String field, int def) {
        if (!node.has(field)) return def;
        return node.get(field).asInt(def);
    }

}
