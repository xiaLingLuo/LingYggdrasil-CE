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

import im.xz.cn.common.Treasure;
import im.xz.cn.security.YggdrasilKeyManager;
import im.xz.cn.database.dao.TokenDao;
import im.xz.cn.plugin.PluginManager;

import im.xz.cn.logging.logApi;

public class Yggdrasil {
    private static final logApi log = logApi.getLogger(Yggdrasil.class);

    static {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.stdout.encoding", "UTF-8");
        System.setProperty("sun.stderr.encoding", "UTF-8");
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception _) {
        }
    }

    private static InstallServer installServer;
    private static UserServer userServer;
    private static YggdrasilServer yggdrasilServer;
    private static AdminServer adminServer;
    private static DatabaseManager databaseManager;
    private static Treasure treasure;
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        log.info("╔═══════════════════════════════════════╗");
        log.info("║             LingYggdrasil             ║");
        log.info("║                Welcome!               ║");
        log.info("╚═══════════════════════════════════════╝");

        AppConfig config = AppConfig.getInstance();
        try {
            im.xz.cn.config.ServerConfig.getInstance().load();
            im.xz.cn.i18n.I18n.releaseBundles();
            im.xz.cn.common.AppIcons.init();
            Runtime.getRuntime().addShutdownHook(new Thread(Yggdrasil::shutdown));

            if (!config.isInstalled()) {
                startInstallMode();
            } else {
                startFullMode(config);
            }
        } catch (Exception e) {
            log.error("[Fatal] Startup failed: {}", e.getMessage(), e);
            System.exit(1);
        }

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void startInstallMode() {
        log.info("[Install Mode] Starting installation wizard...");
        installServer = new InstallServer();
        installServer.start();
        log.info("[Install Mode] Installation wizard: http://localhost:35598");
        log.info(
                """
                          _       _                  __   __                      _                        _   _\s
                         | |     (_)  _ __     __ _  \\ \\ / /   __ _    __ _    __| |  _ __    __ _   ___  (_) | |
                         | |     | | | '_ \\   / _` |  \\ V /   / _` |  / _` |  / _` | | '__|  / _` | / __| | | | |
                         | |___  | | | | | | | (_| |   | |   | (_| | | (_| | | (_| | | |    | (_| | \\__ \\ | | | |
                         |_____| |_| |_| |_|  \\__, |   |_|    \\__, |  \\__, |  \\__,_| |_|     \\__,_| |___/ |_| |_|
                                              |___/           |___/   |___/                                      \
                        """);

        log.info("欢迎使用 泠 Yggdrasil ！程序现在正处于安装模式。");
        log.info("请使用浏览器访问端口35598，完成安装。");
        log.info("Welcome to LingYggdrasil! The program is currently in installation mode.");
        log.info("Please access port 35598 to complete the installation.");
    }

    private static void startFullMode(AppConfig config) {
        log.info("[Running] System installed. Starting all services...");

        config.loadConfig();

        databaseManager = new DatabaseManager(config.getDatabaseConfig());
        databaseManager.initializeSchema();
        log.info("[DB] Database initialized.");

        im.xz.cn.logging.UserActionLogger.init(new im.xz.cn.database.dao.UserLogDao(databaseManager));
        im.xz.cn.logging.ServiceLog.init();

        im.xz.cn.config.ServerConfig serverConfig = im.xz.cn.config.ServerConfig.getInstance();
        im.xz.cn.logging.ServiceLog.configure(serverConfig.getLogLevel(),
                serverConfig.isAuditLogEnabled(), serverConfig.isPluginSystemLogEnabled(),
                serverConfig.logRetention());

        SystemConfig sysConfig = SystemConfig.getInstance();
        sysConfig.loadFromDatabase(databaseManager);
        log.info("[Config] System config loaded from database.");

        YggdrasilKeyManager keyManager = YggdrasilKeyManager.getInstance();
        if (sysConfig.getYggdrasilPrivateKey().isEmpty() && sysConfig.getYggdrasilPublicKey().isEmpty()) {
            try {
                keyManager.generateKeyPair(sysConfig.getSignatureMode());
                sysConfig.setYggdrasilPrivateKey(keyManager.getPrivateKeyPem());
                sysConfig.setYggdrasilPublicKey(keyManager.getPublicKeyPem());
                sysConfig.saveToDatabase(databaseManager);
                log.info("[KeyManager] 已自动生成 {} 密钥对", sysConfig.getSignatureMode());
            } catch (Exception e) {
                log.error("[KeyManager] 自动生成密钥对失败: {}", e.getMessage(), e);
            }
        } else {
            keyManager.loadFromPem(sysConfig.getYggdrasilPrivateKey(), sysConfig.getYggdrasilPublicKey(), sysConfig.getSignatureMode());
            log.info("[KeyManager] 已加载 {} 签名密钥", sysConfig.getSignatureMode());
        }

        if (SystemConfig.getInstance().isTreasureEnabled()) {
            treasure = Treasure.init(databaseManager);
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
                log.error("[TokenCleaner] Error cleaning expired tokens: {}", e.getMessage(), e);
            }
        }, 1, 1, TimeUnit.HOURS);
        log.info("[Scheduler] Expired token cleanup task scheduled (interval: 1 hour).");

        MailService mailService = new MailService(config.getMailConfig());

        if (serverConfig.isUserEnabled()) {
            userServer = new UserServer(serverConfig.getUserPort(), databaseManager, mailService);
            userServer.start(serverConfig.getUserIp(), serverConfig.getUserPort());
            log.info("  User Dashboard started with port {}", serverConfig.getUserPort());
        } else {
            log.info("  User Dashboard disabled by config.yml");
        }

        if (serverConfig.isYggdrasilEnabled()) {
            yggdrasilServer = new YggdrasilServer(databaseManager);
            yggdrasilServer.start(serverConfig.getYggdrasilIp(), serverConfig.getYggdrasilPort());
            log.info("  Yggdrasil API started with port {}", serverConfig.getYggdrasilPort());
        } else {
            log.info("  Yggdrasil API disabled by config.yml");
        }

        if (serverConfig.isAdminEnabled()) {
            adminServer = new AdminServer(serverConfig.getAdminPort(), databaseManager);
            adminServer.start(serverConfig.getAdminIp(), serverConfig.getAdminPort());
            log.info("  Admin Panel started with port {}", serverConfig.getAdminPort());
        } else {
            log.info("  Admin Panel disabled by config.yml");
        }

        try {
            PluginManager.getInstance().bootstrap(databaseManager);
            log.info("  Plugin system initialized");
        } catch (Throwable t) {
            log.error("[Plugin] Failed to initialize plugin system: {}", t.getMessage(), t);
        }

        log.info("[OK] All services started!");


        log.info(
                """
                          _       _                  __   __                      _                        _   _\s
                         | |     (_)  _ __     __ _  \\ \\ / /   __ _    __ _    __| |  _ __    __ _   ___  (_) | |
                         | |     | | | '_ \\   / _` |  \\ V /   / _` |  / _` |  / _` | | '__|  / _` | / __| | | | |
                         | |___  | | | | | | | (_| |   | |   | (_| | | (_| | | (_| | | |    | (_| | \\__ \\ | | | |
                         |_____| |_| |_| |_|  \\__, |   |_|    \\__, |  \\__, |  \\__,_| |_|     \\__,_| |___/ |_| |_|
                                              |___/           |___/   |___/                                      \
                        """);

        log.info("欢迎使用 泠 Yggdrasil ！程序现在已完全启动。你可使用浏览器访问：");
        log.info("用户进程（对外公开）运行在端口35565上；");
        log.info("API进程（对外公开）运行在端口35577上；");
        log.info("管理进程（需要保密）运行在端口35599上。");
        log.info("Welcome to LingYggdrasil! The program has now been fully launched. \nYou can using browsers access followings:");
        log.info("User App(Public Access) is running with port 35565;");
        log.info("API App(Public Access) is running with port 35577;");
        log.info("Admin App(Private Access) is running with port 35599.");




    }

    private static void shutdown() {
        log.info("Shutting down LingYggdrasil...");
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
            PluginManager.getInstance().shutdownAll();
            if (databaseManager != null) databaseManager.close();
            Treasure.shutdown();
            im.xz.cn.logging.ServiceLog.shutdown();
        } catch (Exception e) {
            log.error("Error during shutdown: {}", e.getMessage(), e);
        }
        log.info("Shutdown complete.");
    }
}
