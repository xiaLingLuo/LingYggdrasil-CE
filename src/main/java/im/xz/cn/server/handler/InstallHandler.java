package im.xz.cn.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.DatabaseConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.Admin;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.util.PasswordValidator;
import im.xz.cn.server.InstallServer;
import im.xz.cn.util.UuidUtil;
import im.xz.cn.something.web.Css;
import im.xz.cn.something.web.InstallPage;
import im.xz.cn.web.PageRenderer;
import io.javalin.http.Context;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallHandler {
    private static final Logger logger = LoggerFactory.getLogger(InstallHandler.class);

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
            ctx.status(403).result("系统已安装，如需重新安装请删除 .INSTALLED 文件。");
            return;
        }

        String token = ctx.queryParam("token");

        // 安装认证暂时先不用

        //if (!validateToken(token)) {
        //    ctx.status(403).result("无效的安装令牌，请从控制台获取正确的安装地址。");
        //    return;
        //}



        String html = PageRenderer.renderPage(
            "安装向导",
            InstallPage.generateInstallPageContent(installToken),
            "install",
            Css.getInstallCssImport()
        );
        ctx.html(html);
    }

    public static void getStatus(Context ctx) {
        if (AppConfig.getInstance().isInstalled()) {
            ctx.status(403).json(Map.of("success", false, "message", "系统已安装"));
            return;
        }

        String token = ctx.header("X-Install-Token");
        if (!validateToken(token)) {
            ctx.status(403).json(Map.of("success", false, "message", "无效的安装令牌"));
            return;
        }

        ctx.json(Map.of("installed", false));
    }

    public static void doInstall(Context ctx) {
        if (AppConfig.getInstance().isInstalled()) {
            ctx.status(403).json(Map.of("success", false, "message", "系统已安装"));
            return;
        }

        String token = ctx.header("X-Install-Token");
        if (!validateToken(token)) {
            ctx.status(403).json(Map.of("success", false, "message", "无效的安装令牌"));
            return;
        }

        String clientIp = ctx.ip();
        if (!rateLimiter.allow(clientIp)) {
            ctx.status(429).json(Map.of("success", false, "message", "安装尝试过于频繁，请一小时后再试"));
            return;
        }

        JsonNode body;
        try {
            body = MAPPER.readTree(ctx.body());
        } catch (Exception e) {
            ctx.status(400).json(Map.of("success", false, "message", "请求体格式错误"));
            return;
        }

        String rootUsername = textOrNull(body, "rootUsername");
        String rootPassword = textOrNull(body, "rootPassword");
        String rootEmail    = textOrNull(body, "rootEmail");
        String dbType       = textOrNull(body, "dbType");

        if (rootUsername == null || rootUsername.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", "管理员用户名不能为空"));
            return;
        }
        String passwordError = PasswordValidator.validate(rootPassword);
        if (passwordError != null) {
            ctx.status(400).json(Map.of("success", false, "message", "管理员" + passwordError));
            return;
        }
        if (rootEmail == null || !rootEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", "管理员邮箱格式不正确"));
            return;
        }
        if (dbType == null || (!dbType.equals("sqlite") && !dbType.equals("mysql") && !dbType.equals("pgsql"))) {
            ctx.status(400).json(Map.of("success", false, "message", "数据库类型无效"));
            return;
        }

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setType(dbType);
        if ("sqlite".equals(dbType)) {
            String sqlitePath = textOrDefault(body, "sqlitePath", "./data.db");

            try {
                Path rawPath = Paths.get(sqlitePath);
                if (rawPath.isAbsolute()) {
                    ctx.status(400).json(Map.of("success", false, "message", "SQLite 数据库路径非法，不允许使用绝对路径"));
                    return;
                }
                String userDir = System.getProperty("user.dir");
                Path projectPath = Paths.get(userDir).toAbsolutePath().normalize();
                Path normalizedPath = projectPath.resolve(sqlitePath).toAbsolutePath().normalize();

                if (!normalizedPath.startsWith(projectPath)) {
                    ctx.status(400).json(Map.of("success", false, "message", "SQLite 数据库路径非法，禁止路径穿越"));
                    return;
                }
            } catch (Exception e) {
                ctx.status(400).json(Map.of("success", false, "message", "SQLite 数据库路径格式无效"));
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
                "message", "数据库连接失败，请检查配置"));
            return;
        }

        try {
            dbManager.initializeSchema();

            String adminId    = UuidUtil.generateAdminUuid();
            String passwordHash = Argon2Hasher.hash(rootPassword);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Admin root = new Admin(adminId, rootUsername, rootEmail, passwordHash, AdminRole.ROOT, now);
            new AdminDao(dbManager).insert(root);

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

            ctx.json(Map.of("success", true, "message", "安装成功！安装向导已关闭，请重启程序以进入正常模式。"));
        } catch (Exception e) {
            logger.error("Installation failed: {}", e.getMessage(), e);
            ctx.status(500).json(Map.of("success", false,
                "message", "安装过程中发生错误，请检查日志"));
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
