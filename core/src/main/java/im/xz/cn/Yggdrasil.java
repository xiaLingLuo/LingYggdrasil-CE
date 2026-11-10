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
import im.xz.cn.config.ServerConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.SchemaBaseline;
import im.xz.cn.infrastructure.sql.SqlDialects;
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
import im.xz.cn.runtime.ManagedHost;
import im.xz.cn.runtime.ServiceId;
import im.xz.cn.runtime.ServiceState;
import im.xz.cn.runtime.ServiceStatus;
import im.xz.cn.runtime.ServiceSupervisor;

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
    private static ServiceSupervisor supervisor;
    private static DatabaseManager databaseManager;
    private static im.xz.cn.plugin.runtime.PluginHost spiPluginHost;
    private static im.xz.cn.plugin.runtime.PluginDirectoryLoader.ScanResult spiPlugins;
    private static im.xz.cn.plugin.bridge.LegacyPluginBridgeLoader legacyPluginLoader;
    private static im.xz.cn.plugin.bridge.LegacyPluginBridgeLoader.ScanResult spiLegacyPlugins;
    private static Treasure treasure;
    private static ScheduledExecutorService scheduler;
    private static im.xz.cn.notification.NotificationWorker notificationWorker;

    public static void main(String[] args) {
        log.info("╔═══════════════════════════════════════╗");
        log.info("║             LingYggdrasil             ║");
        log.info("║                Welcome!               ║");
        log.info("╚═══════════════════════════════════════╝");

        AppConfig config = AppConfig.getInstance();
        try {
            ServerConfig.getInstance().load();
            im.xz.cn.i18n.I18n.releaseBundles();
            validateLanguagePacks();
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

        log.info("欢迎使用 LingYggdrasil！程序现在正处于安装模式。");
        log.info("请使用浏览器访问端口35598，完成安装。");
        log.info("Welcome to LingYggdrasil! The program is currently in installation mode.");
        log.info("Please access port 35598 to complete the installation.");
    }

    private static void startFullMode(AppConfig config) {
        log.info("[Running] System installed. Starting all services...");

        config.loadConfig();

        databaseManager = new DatabaseManager(config.getDatabaseConfig());
        im.xz.cn.infrastructure.persistence.LegacyUpgradeGuard.requireMigrated(databaseManager);
        databaseManager.initializeSchema();
        if (!SchemaBaseline.isCurrent(databaseManager)) {
            new SchemaBaseline(SqlDialects.of(databaseManager.getDbType())).apply(databaseManager);
            log.info("[DB] New schema baseline applied.");
        } else {
            log.info("[DB] Schema baseline already current; DDL skipped.");
        }
        log.info("[DB] Database initialized.");
        initSpiPlugins();

        ServerConfig serverConfig = ServerConfig.getInstance();
        serverConfig.migrateLegacyDomains(databaseManager);
        if (serverConfig.getEffectiveUserDomain().isBlank()) {
            log.warn("[Config] services.user.domain and services.user.commonDomain are empty; user-facing links may be invalid");
        }
        if (serverConfig.getEffectiveYggdrasilDomain().isBlank()) {
            log.warn("[Config] services.yggdrasil.domain and services.user.commonDomain are empty; external login textures may not work");
        }

        im.xz.cn.logging.UserActionLogger.init(new im.xz.cn.database.dao.UserLogDao(databaseManager));
        im.xz.cn.logging.ServiceLog.init();

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

        
        MailService mailService = serverConfig.isUserEnabled()
                ? new MailService(config.getMailConfig()) : null;
        if (mailService != null) {
            notificationWorker = new im.xz.cn.notification.NotificationWorker(
                    new im.xz.cn.notification.NotificationJobRepository(databaseManager),
                    job -> im.xz.cn.notification.EmailNotifications.dispatch(job, mailService),
                    5, java.time.Duration.ofSeconds(5));
            notificationWorker.start();
            log.info("[Notification] Email outbox worker started.");
        }

        
        ServiceSupervisor supervisor = new ServiceSupervisor();
        Yggdrasil.supervisor = supervisor;

        if (serverConfig.isUserEnabled()) {
            supervisor.register(new ManagedHost() {
                @Override
                public ServiceId id() {
                    return ServiceId.USER;
                }

                @Override
                public void start() {
                    userServer = new UserServer(serverConfig.getUserPort(), databaseManager, mailService);
                    userServer.start(serverConfig.getUserIp(), serverConfig.getUserPort());
                }

                @Override
                public void stop() {
                    if (userServer != null) {
                        userServer.stop();
                    }
                }

                @Override
                public boolean healthy() {
                    return userServer != null;
                }
            });
        } else {
            log.info("  User Dashboard disabled by config.yml");
        }

        if (serverConfig.isYggdrasilEnabled()) {
            supervisor.register(new ManagedHost() {
                @Override
                public ServiceId id() {
                    return ServiceId.YGGDRASIL;
                }

                @Override
                public void start() {
                    yggdrasilServer = new YggdrasilServer(databaseManager);
                    yggdrasilServer.start(serverConfig.getYggdrasilIp(), serverConfig.getYggdrasilPort());
                }

                @Override
                public void stop() {
                    if (yggdrasilServer != null) {
                        yggdrasilServer.stop();
                    }
                }

                @Override
                public boolean healthy() {
                    return yggdrasilServer != null;
                }
            });
        } else {
            log.info("  Yggdrasil API disabled by config.yml");
        }

        if (serverConfig.isAdminEnabled()) {
            supervisor.register(new ManagedHost() {
                @Override
                public ServiceId id() {
                    return ServiceId.ADMIN;
                }

                @Override
                public void start() {
                    adminServer = new AdminServer(serverConfig.getAdminPort(), databaseManager);
                    adminServer.start(serverConfig.getAdminIp(), serverConfig.getAdminPort());
                }

                @Override
                public void stop() {
                    if (adminServer != null) {
                        adminServer.stop();
                    }
                }

                @Override
                public boolean healthy() {
                    return adminServer != null;
                }
            });
        } else {
            log.info("  Admin Panel disabled by config.yml");
        }

        supervisor.startAll();
        for (ServiceStatus status : supervisor.statuses()) {
            String detail = switch (status.id()) {
                case USER -> "port " + serverConfig.getUserPort();
                case YGGDRASIL -> "port " + serverConfig.getYggdrasilPort();
                case ADMIN -> "port " + serverConfig.getAdminPort();
                case INSTALL -> "install wizard";
            };
            if (status.state() == ServiceState.READY) {
                log.info("  {} started ({})", status.id().code(), detail);
            } else {
                log.warn("  {} not ready: {} ({}; {})", status.id().code(), status.state(), detail, status.failure());
            }
        }

        try {
            PluginManager.getInstance().bootstrap(databaseManager);
            log.info("  Plugin system initialized");
        } catch (Throwable t) {
            log.error("[Plugin] Failed to initialize plugin system: {}", t.getMessage(), t);
        }

        if (supervisor.anyReady()) {
            log.info("[OK] All services started!");
        } else {
            log.error("[Supervisor] No host is ready; check config.yml/ports. Status: {}", supervisor.summary());
        }


        log.info(
                """
                          _       _                  __   __                      _                        _   _\s
                         | |     (_)  _ __     __ _  \\ \\ / /   __ _    __ _    __| |  _ __    __ _   ___  (_) | |
                         | |     | | | '_ \\   / _` |  \\ V /   / _` |  / _` |  / _` | | '__|  / _` | / __| | | | |
                         | |___  | | | | | | | (_| |   | |   | (_| | | (_| | | (_| | | |    | (_| | \\__ \\ | | | |
                         |_____| |_| |_| |_|  \\__, |   |_|    \\__, |  \\__, |  \\__,_| |_|     \\__,_| |___/ |_| |_|
                                              |___/           |___/   |___/                                      \
                        """);

        log.info("欢迎使用 LingYggdrasil！程序现在已完全启动。你可使用浏览器访问：");
        log.info("用户进程（对外公开）运行在端口35565上；");
        log.info("API进程（对外公开）运行在端口35577上；");
        log.info("管理进程（需要保密）运行在端口35599上。");
        log.info("Welcome to LingYggdrasil! The program has now been fully launched. \nYou can using browsers access followings:");
        log.info("User App(Public Access) is running with port 35565;");
        log.info("API App(Public Access) is running with port 35577;");
        log.info("Admin App(Private Access) is running with port 35599.");




    }

    
    private static void validateLanguagePacks() {
        im.xz.cn.i18n.LanguagePackValidator.Report report =
                im.xz.cn.i18n.LanguagePackValidator.validateBundled();
        if (report.ok()) {
            log.info("[i18n] Language packs OK: {}", report.locales());
            return;
        }
        for (im.xz.cn.i18n.LanguagePackValidator.Problem problem : report.problems()) {
            log.warn("[i18n] Language pack problem: locale={} resource={} reason={}",
                    problem.locale(), problem.resource(), problem.reason());
        }
        log.warn("[i18n] {} language pack problem(s); default locale is {}",
                report.problems().size(), report.defaultLocale());
    }

    
    private static void initSpiPlugins() {
        try {
            java.nio.file.Path pluginsDir = java.nio.file.Path.of(
                    System.getProperty("user.dir"), "plugins", "spi");
            java.nio.file.Path dataDir = java.nio.file.Path.of(
                    System.getProperty("user.dir"), "plugins", "data");
            spiPluginHost = new im.xz.cn.plugin.runtime.PluginHost(databaseManager, dataDir);
            im.xz.cn.plugin.runtime.PluginDirectoryLoader loader =
                    new im.xz.cn.plugin.runtime.PluginDirectoryLoader();
            spiPlugins = loader.registerAll(pluginsDir, spiPluginHost);
            legacyPluginLoader = new im.xz.cn.plugin.bridge.LegacyPluginBridgeLoader();
            spiLegacyPlugins = legacyPluginLoader.registerAll(
                    java.nio.file.Path.of(System.getProperty("user.dir"), "plugins", "legacy"),
                    spiPluginHost, databaseManager);
            spiPluginHost.loadAll();
            spiPluginHost.enableAll();
            log.info("[Plugin] SPI plugins loaded: {} (rejected={})",
                    spiPlugins.loaded().size(), spiPlugins.rejected().size());
            log.info("[Plugin] Legacy plugins bridged: {} (rejected={})",
                    spiLegacyPlugins.loaded().size(), spiLegacyPlugins.rejected().size());
        } catch (RuntimeException error) {
            log.warn("[Plugin] SPI plugin initialization failed: {}", error.getMessage());
        }
    }

    private static void shutdown() {
        log.info("Shutting down LingYggdrasil...");
        try {
            if (notificationWorker != null) {
                notificationWorker.close();
                notificationWorker = null;
            }
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
            
            if (supervisor != null) {
                supervisor.stopAll();
                supervisor = null;
            }
            if (spiPluginHost != null) {
                spiPluginHost.close();
                spiPluginHost = null;
            }
            if (spiLegacyPlugins != null && legacyPluginLoader != null) {
                legacyPluginLoader.close(spiLegacyPlugins);
                spiLegacyPlugins = null;
                legacyPluginLoader = null;
            }
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
