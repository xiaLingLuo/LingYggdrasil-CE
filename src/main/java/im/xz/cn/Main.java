package im.xz.cn;

import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.mail.MailService;
import im.xz.cn.server.AdminServer;
import im.xz.cn.server.InstallServer;
import im.xz.cn.server.UserServer;
import im.xz.cn.server.YggdrasilServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import im.xz.cn.util.YggdrasilKeyManager;
import im.xz.cn.database.TokenDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 不闻
        }
    }

    private static InstallServer installServer;
    private static UserServer userServer;
    private static YggdrasilServer yggdrasilServer;
    private static AdminServer adminServer;
    private static DatabaseManager databaseManager;
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        logger.info("╔═══════════════════════════════════════╗");
        logger.info("║             LingYggdrasil             ║");
        logger.info("║                Welcome!               ║");
        logger.info("╚═══════════════════════════════════════╝");

        AppConfig config = AppConfig.getInstance();

        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

        try {
            if (!config.isInstalled()) {
                startInstallMode();
            } else {
                startFullMode(config);
            }
        } catch (Exception e) {
            logger.error("[Fatal] Startup failed: {}", e.getMessage(), e);
            System.exit(1);
        }

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startInstallMode() {
        logger.info("[Install Mode] Starting installation wizard...");
        installServer = new InstallServer();
        installServer.start();
        logger.info("[Install Mode] Installation wizard: http://localhost:35598");
        System.out.println(
                "  _       _                  __   __                      _                        _   _ \n" +
                        " | |     (_)  _ __     __ _  \\ \\ / /   __ _    __ _    __| |  _ __    __ _   ___  (_) | |\n" +
                        " | |     | | | '_ \\   / _` |  \\ V /   / _` |  / _` |  / _` | | '__|  / _` | / __| | | | |\n" +
                        " | |___  | | | | | | | (_| |   | |   | (_| | | (_| | | (_| | | |    | (_| | \\__ \\ | | | |\n" +
                        " |_____| |_| |_| |_|  \\__, |   |_|    \\__, |  \\__, |  \\__,_| |_|     \\__,_| |___/ |_| |_|\n" +
                        "                      |___/           |___/   |___/                                      ");

        System.out.println("欢迎使用 泠 Yggdrasil ！程序现在正处于安装模式。");
        System.out.println("请使用浏览器访问端口35598，完成安装。");
        System.out.println("Welcome to LingYggdrasil! The program is currently in installation mode.");
        System.out.println("Please access port 35598 to complete the installation.");
    }

    private static void startFullMode(AppConfig config) {
        logger.info("[Running] System installed. Starting all services...");

        config.loadConfig();

        databaseManager = new DatabaseManager(config.getDatabaseConfig());
        databaseManager.initializeSchema();
        logger.info("[DB] Database initialized.");

        SystemConfig sysConfig = SystemConfig.getInstance();
        sysConfig.loadFromDatabase(databaseManager);
        logger.info("[Config] System config loaded from database.");

        YggdrasilKeyManager keyManager = YggdrasilKeyManager.getInstance();
        if (sysConfig.getYggdrasilPrivateKey().isEmpty() && sysConfig.getYggdrasilPublicKey().isEmpty()) {
            try {
                keyManager.generateKeyPair(sysConfig.getSignatureMode());
                sysConfig.setYggdrasilPrivateKey(keyManager.getPrivateKeyPem());
                sysConfig.setYggdrasilPublicKey(keyManager.getPublicKeyPem());
                sysConfig.saveToDatabase(databaseManager);
                logger.info("[KeyManager] 已自动生成 {} 密钥对", sysConfig.getSignatureMode());
            } catch (Exception e) {
                logger.error("[KeyManager] 自动生成密钥对失败: {}", e.getMessage(), e);
            }
        } else {
            keyManager.loadFromPem(sysConfig.getYggdrasilPrivateKey(), sysConfig.getYggdrasilPublicKey(), sysConfig.getSignatureMode());
            logger.info("[KeyManager] 已加载 {} 签名密钥", sysConfig.getSignatureMode());
        }

        TokenDao tokenDao = new TokenDao(databaseManager);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-cleaner");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                tokenDao.cleanExpired();
            } catch (Exception e) {
                logger.error("[TokenCleaner] Error cleaning expired tokens: {}", e.getMessage(), e);
            }
        }, 1, 1, TimeUnit.HOURS);
        logger.info("[Scheduler] Expired token cleanup task scheduled (interval: 1 hour).");

        MailService mailService = new MailService(config.getMailConfig());

        userServer = new UserServer(35565, databaseManager, mailService);
        userServer.start();
        logger.info("  User Dashboard started with port 35565");

        yggdrasilServer = new YggdrasilServer(databaseManager);
        yggdrasilServer.start();
        logger.info("  Yggdrasil API started with port 35577");

        adminServer = new AdminServer(35599, databaseManager);
        adminServer.start();
        logger.info("  Admin Panel started with port 35599");

        logger.info("[OK] All services started!");


        System.out.println(
                "  _       _                  __   __                      _                        _   _ \n" +
                " | |     (_)  _ __     __ _  \\ \\ / /   __ _    __ _    __| |  _ __    __ _   ___  (_) | |\n" +
                " | |     | | | '_ \\   / _` |  \\ V /   / _` |  / _` |  / _` | | '__|  / _` | / __| | | | |\n" +
                " | |___  | | | | | | | (_| |   | |   | (_| | | (_| | | (_| | | |    | (_| | \\__ \\ | | | |\n" +
                " |_____| |_| |_| |_|  \\__, |   |_|    \\__, |  \\__, |  \\__,_| |_|     \\__,_| |___/ |_| |_|\n" +
                "                      |___/           |___/   |___/                                      ");

        System.out.println("欢迎使用 泠 Yggdrasil ！程序现在已完全启动。你可使用浏览器访问：");
        System.out.println("用户进程（对外公开）运行在端口35565上；");
        System.out.println("API进程（对外公开）运行在端口35577上；");
        System.out.println("管理进程（需要保密）运行在端口35599上。");
        System.out.println("Welcome to LingYggdrasil! The program has now been fully launched. \nYou can using browsers access followings:");
        System.out.println("User App(Public Access) is running with port 35565;");
        System.out.println("API App(Public Access) is running with port 35577;");
        System.out.println("Admin App(Private Access) is running with port 35599.");




    }

    private static void shutdown() {
        logger.info("Shutting down LingYggdrasil...");
        try {
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            if (installServer != null) installServer.stop();
            if (userServer != null) userServer.stop();
            if (yggdrasilServer != null) yggdrasilServer.stop();
            if (adminServer != null) adminServer.stop();
            if (databaseManager != null) databaseManager.close();
        } catch (Exception e) {
            logger.error("Error during shutdown: {}", e.getMessage(), e);
        }
        logger.info("Shutdown complete.");
    }
}
