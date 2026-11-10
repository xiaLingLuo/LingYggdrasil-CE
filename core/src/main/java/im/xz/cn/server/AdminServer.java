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
package im.xz.cn.server;


import im.xz.cn.i18n.AdminI18n;
import im.xz.cn.bootstrap.ServerFactory;
import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.i18n.LocaleContext;
import im.xz.cn.i18n.LocaleResolver;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.*;
import im.xz.cn.model.Admin;
import im.xz.cn.model.PermGroup;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.security.RootIntegrityGuard;
import im.xz.cn.server.handler.admin.*;
import im.xz.cn.common.FooterInfo;
import im.xz.cn.texture.TextureService;
import im.xz.cn.web.ErrorPageHandler;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import java.util.Map;
import java.util.Set;

import im.xz.cn.logging.logApi;

public class AdminServer {
    private static final logApi logger = logApi.getLogger(AdminServer.class);

    private static final Map<String, String> PAGE_VIEW_PERMS = Map.ofEntries(
            Map.entry("/admin/dashboard", "admin.dashboard.view"),
            Map.entry("/admin/users", "admin.users.view"),
            Map.entry("/admin/profiles", "admin.profiles.view"),
            Map.entry("/admin/profiles/settings", "admin.profiles.view"),
            Map.entry("/admin/skins", "admin.skins.view"),
            Map.entry("/admin/capes", "admin.capes.view"),
            Map.entry("/admin/security", "admin.security.view"),
            Map.entry("/admin/admins", "admin.admins.view"),
            Map.entry("/admin/appinfo", "admin.appinfo.view"),
            Map.entry("/admin/yggdrasil", "admin.yggdrasil.view"),
            Map.entry("/admin/system", "admin.system.view"),
            Map.entry("/admin/texture-settings", "admin.system.view"),
            Map.entry("/admin/texture-blacklist", "admin.system.view"),
            Map.entry("/admin/plugins", "admin.plugin.overall.view")
    );

    private final Javalin app;
    private final int port;

    public AdminServer(int port, DatabaseManager db) {
        this.port = port;

        AuthService authService = new AuthService(db);
        AdminDao adminDao = authService.getAdminDao();
        UserDao userDao = authService.getUserDao();
        ProfileDao profileDao = authService.getProfileDao();
        TokenDao tokenDao = authService.getTokenDao();
        CacheDao cacheDao = authService.getCacheDao();
        SystemConfig systemConfig = SystemConfig.getInstance();

        TextureService textureService = new TextureService(systemConfig, cacheDao);
        im.xz.cn.database.dao.RootInfoDao rootInfoDao = new im.xz.cn.database.dao.RootInfoDao(db);
        im.xz.cn.database.dao.PermGroupDao permGroupDao = new im.xz.cn.database.dao.PermGroupDao(db);
        im.xz.cn.database.dao.UserPermGroupDao userPermGroupDao = new im.xz.cn.database.dao.UserPermGroupDao(db);

        AdminAuthHandler authHandler = new AdminAuthHandler(authService, cacheDao, rootInfoDao);
        AdminDashboardHandler dashboardHandler = new AdminDashboardHandler(userDao, profileDao, tokenDao, adminDao);
        AdminSystemHandler systemHandler = new AdminSystemHandler(systemConfig, cacheDao, tokenDao, db);
        AdminSecurityHandler securityHandler = new AdminSecurityHandler(systemConfig, db);
        RootManagementHandler rootManagementHandler = new RootManagementHandler(db);
        im.xz.cn.database.dao.UserLogDao userLogDao = new im.xz.cn.database.dao.UserLogDao(db);
        AdminUserHandler userHandler = new AdminUserHandler(userDao, adminDao, systemConfig, userPermGroupDao, userLogDao);
        im.xz.cn.server.handler.admin.AdminUserPermGroupHandler userPermGroupHandler =
                new im.xz.cn.server.handler.admin.AdminUserPermGroupHandler(userPermGroupDao);
        AdminAdminHandler adminAdminHandler = new AdminAdminHandler(adminDao, rootInfoDao, permGroupDao);
        im.xz.cn.server.handler.admin.AdminPermGroupHandler permGroupHandler =
                new im.xz.cn.server.handler.admin.AdminPermGroupHandler(permGroupDao, rootInfoDao);
        AdminAppInfoHandler appInfoHandler = new AdminAppInfoHandler(cacheDao);
        AdminProfilesHandler profilesHandler = new AdminProfilesHandler(profileDao, userDao, adminDao, systemConfig, db);
        AdminYggdrasilHandler yggdrasilHandler = new AdminYggdrasilHandler(systemConfig, db);
        AdminPluginHandler pluginHandler = new AdminPluginHandler();
        AdminTextureAssetHandler textureAssetHandler = new AdminTextureAssetHandler(db);
        AdminSettingsHandler settingsHandler = new AdminSettingsHandler(db);
        AdminProfileTransferHandler profileTransferHandler = new AdminProfileTransferHandler(db);
        AdminAuditHandler auditHandler = new AdminAuditHandler(db);
        AdminTextureStorageHandler textureStorageHandler = new AdminTextureStorageHandler(db);

        this.app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";
            ServerFactory.configureErrorHandling(config);
            ServerFactory.configureErrorHandling(config);

            ServerFactory.configureSessionCookie(config, "LING_ADMIN_SESSION",
                    ServerFactory.isHttpsDomain(im.xz.cn.config.ServerConfig.getInstance().getEffectiveAdminDomain()));
            ServerFactory.configureThreadLocalCleanup(config);
            ServerFactory.configureSecurityHeaders(config);

            ServerFactory.configureCors(config, port);

            ServerFactory.registerStaticRoutes(config.routes);
            ServerFactory.registerIconRoutes(config.routes);

            config.jsonMapper(new JavalinJackson());

            config.routes.before(ctx -> {
                im.xz.cn.logging.ServiceLog.setService(im.xz.cn.logging.ServiceLog.ADMIN);
                AdminPermissions.clear();
                String lang = SessionManager.getLanguage(ctx);
                if (lang == null) lang = ctx.cookie("LING_ADMIN_LANG");
                if (lang == null) lang = LocaleResolver.fromAcceptLanguage(ctx.header("Accept-Language"));
                LocaleContext.set(LocaleResolver.normalize(lang));
            });
            config.routes.after(ctx -> {
                LocaleContext.clear();
                AdminPermissions.clear();
                im.xz.cn.logging.ServiceLog.clear();
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                if (path.startsWith("/css/") || path.startsWith("/js/")
                        || path.startsWith("/img/") || path.startsWith("/icons/")
                        || path.startsWith("/builtin-icons/") || path.equals("/favicon.ico")) {
                    return;
                }
                if (!RootIntegrityGuard.isValid(db)) {
                    if (path.startsWith("/admin/api/")) {
                        ctx.status(500).json(Map.of("success", false, "message", RootIntegrityGuard.MESSAGE));
                    } else {
                        ctx.status(500).result(RootIntegrityGuard.MESSAGE);
                    }
                    ctx.skipRemainingHandlers();
                }
            });

            config.routes.before("/admin/*", ctx -> {
                var session = ctx.req().getSession(false);
                if (session != null) {
                    session.setMaxInactiveInterval(systemConfig.getAdminSessionTimeoutSeconds());
                }
                String path = ctx.path();
                if (path.length() > 1 && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (path.equals("/admin/login") || path.equals("/admin/api/login")) {
                    return;
                }
                String adminId = SessionManager.getAdminId(ctx);
                if (adminId == null) {
                    if (path.startsWith("/admin/api/")) {
                        ctx.status(401);
                        ctx.json(Map.of("success", false, "message", AdminI18n.t("msg.notLoggedIn")));
                    } else {
                        ctx.redirect("/admin/login");
                    }
                    ctx.skipRemainingHandlers();
                    return;
                }

                if (SessionManager.isAdminRoot(ctx)) {
                    if (rootInfoDao.findById(adminId) == null) {
                        SessionManager.invalidateAdmin(ctx);
                        ctx.status(401);
                        ctx.json(Map.of("success", false, "message", AdminI18n.t("msg.accountInvalid")));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    AdminPermissions.set(Set.of(AdminPermissions.WILDCARD));
                } else {
                    Admin admin = adminDao.findById(adminId);
                    if (admin == null) {
                        SessionManager.invalidateAdmin(ctx);
                        ctx.status(401);
                        ctx.json(Map.of("success", false, "message", AdminI18n.t("msg.accountInvalid")));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    SessionManager.setAdminPermGroup(ctx, admin.getPermGroup());
                    PermGroup group = permGroupDao.findByName(admin.getPermGroup());
                    AdminPermissions.set(group != null
                            ? AdminPermissions.parse(group.getPermissions())
                            : Set.of());
                }

                String viewPerm = PAGE_VIEW_PERMS.get(path);
                boolean textureAllowed = "/admin/texture-assets".equals(path)
                        && (AdminPermissions.has("admin.skins.view")
                        || AdminPermissions.has("admin.capes.view"));
                if (viewPerm != null && !AdminPermissions.has(viewPerm)) {
                    ErrorPageHandler.html(ctx, 403);
                    ctx.skipRemainingHandlers();
                    return;
                }
                if ("/admin/texture-assets".equals(path) && !textureAllowed) {
                    ErrorPageHandler.html(ctx, 403);
                    ctx.skipRemainingHandlers();
                    return;
                }

                if (!SessionManager.validateClientFingerprint(ctx)) {
                    SessionManager.invalidateAdmin(ctx);
                    ctx.status(401);
                    ctx.json(Map.of("success", false, "message", AdminI18n.t("msg.sessionError")));
                    ctx.skipRemainingHandlers();
                    return;
                }

                String method = ctx.method().name();
                if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
                    if (!SessionManager.validateCsrfToken(ctx)) {
                        ctx.status(403);
                        ctx.json(Map.of("success", false, "message", AdminI18n.t("msg.csrfFailed")));
                        ctx.skipRemainingHandlers();
                    }
                }
            });

            config.routes.get("/", ctx -> {
                if (SessionManager.isAdminLoggedIn(ctx)) {
                    ctx.redirect("/admin/dashboard");
                } else {
                    ctx.redirect("/admin/login");
                }
            });

            config.routes.get("/admin/login", authHandler::loginPage);
            config.routes.get("/admin/logout", authHandler::logout);
            config.routes.get("/admin/dashboard", dashboardHandler::dashboardPage);
            
        config.routes.get("/admin/system", systemHandler::systemPage);
            config.routes.get("/admin/security", securityHandler::securityPage);
            config.routes.get("/admin/users", userHandler::usersPage);
            config.routes.get("/admin/profiles", profilesHandler::profilesPage);
            config.routes.get("/admin/profiles/settings", profilesHandler::profileSettingsPage);
            config.routes.get("/admin/admins", adminAdminHandler::adminsPage);
            config.routes.get("/admin/appinfo", appInfoHandler::appInfoPage);
            config.routes.get("/admin/yggdrasil", yggdrasilHandler::yggdrasilPage);

            config.routes.post("/admin/api/login", authHandler::login);

            config.routes.get("/admin/api/dashboard/stats", dashboardHandler::getStats);
            config.routes.post("/admin/api/system/cache/clear", systemHandler::clearCache);
            config.routes.get("/admin/api/preferences/theme", systemHandler::handleGetTheme);
            config.routes.post("/admin/api/preferences/theme", systemHandler::handleSetTheme);
            config.routes.get("/admin/api/preferences/language", systemHandler::handleGetLanguage);
            config.routes.post("/admin/api/preferences/language", systemHandler::handleSetLanguage);

            config.routes.get("/admin/api/security/settings", securityHandler::getSettings);
            config.routes.post("/admin/api/security/settings", securityHandler::updateSettings);
            config.routes.get("/admin/api/security/root", rootManagementHandler::getSettings);
            config.routes.post("/admin/api/security/root", rootManagementHandler::updateSettings);

            config.routes.get("/admin/api/users", userHandler::getUsers);
            config.routes.post("/admin/api/users/delete", userHandler::deleteUser);
            config.routes.post("/admin/api/users/username", userHandler::updateUsername);
            config.routes.post("/admin/api/users/nickname", userHandler::updateNickname);
            config.routes.post("/admin/api/users/verify-email", userHandler::setEmailVerified);
            config.routes.post("/admin/api/users/email", userHandler::updateEmail);
            config.routes.post("/admin/api/users/create", userHandler::createUser);
            config.routes.post("/admin/api/users/perm-group", userHandler::updateUserPermGroup);

            config.routes.get("/admin/api/user-perm-groups/catalogue", userPermGroupHandler::getCatalogue);
            config.routes.get("/admin/api/user-perm-groups", userPermGroupHandler::getGroups);
            config.routes.post("/admin/api/user-perm-groups/create", userPermGroupHandler::createGroup);
            config.routes.post("/admin/api/user-perm-groups/update", userPermGroupHandler::updateGroup);
            config.routes.post("/admin/api/user-perm-groups/delete", userPermGroupHandler::deleteGroup);

            config.routes.get("/admin/api/profiles", profilesHandler::getProfiles);
            config.routes.post("/admin/api/profiles/create", profilesHandler::createProfile);
            config.routes.post("/admin/api/profiles/delete", profilesHandler::deleteProfile);
            config.routes.post("/admin/api/profiles/update", profilesHandler::updateProfile);
            config.routes.post("/admin/api/profiles/clear-textures", profilesHandler::clearProfileTextures);
            config.routes.get("/admin/api/profiles/settings", profilesHandler::getSettings);
            config.routes.post("/admin/api/profiles/settings", profilesHandler::updateSettings);

            config.routes.get("/admin/api/admins", adminAdminHandler::getAdmins);
            config.routes.post("/admin/api/admins/create", adminAdminHandler::createAdmin);
            config.routes.post("/admin/api/admins/delete", adminAdminHandler::deleteAdmin);
            config.routes.post("/admin/api/admins/update", adminAdminHandler::updateAdmin);

            config.routes.get("/admin/api/perm-groups/catalogue", permGroupHandler::getCatalogue);
            config.routes.get("/admin/api/perm-groups", permGroupHandler::getGroups);
            config.routes.post("/admin/api/perm-groups/create", permGroupHandler::createGroup);
            config.routes.post("/admin/api/perm-groups/update", permGroupHandler::updateGroup);
            config.routes.post("/admin/api/perm-groups/delete", permGroupHandler::deleteGroup);

            config.routes.get("/admin/api/yggdrasil/settings", yggdrasilHandler::getSettings);
            config.routes.post("/admin/api/yggdrasil/settings", yggdrasilHandler::updateSettings);
            config.routes.post("/admin/api/yggdrasil/regenerate-keys", yggdrasilHandler::regenerateKeys);
            config.routes.post("/admin/api/yggdrasil/switch-mode", yggdrasilHandler::switchMode);

            config.routes.get("/admin/api/appinfo", appInfoHandler::getAppInfo);
            config.routes.post("/admin/api/appinfo/check-update", appInfoHandler::checkUpdate);

            config.routes.get("/admin/plugins", pluginHandler::pluginsPage);
            config.routes.get("/admin/plugins/{plugin}/{menu}", pluginHandler::pluginMenuPage);
            config.routes.get("/admin/api/plugins", pluginHandler::list);
            config.routes.post("/admin/api/plugins/{name}/enable", pluginHandler::enable);
            config.routes.post("/admin/api/plugins/{name}/disable", pluginHandler::disable);
            config.routes.post("/admin/api/plugins/{name}/reload", pluginHandler::reload);
            config.routes.get("/admin/api/plugins/{name}/icon", pluginHandler::icon);
            for (String pattern : new String[]{"/admin/api/plugins/{name}/api", "/admin/api/plugins/{name}/api/*"}) {
                config.routes.get(pattern, pluginHandler::api);
                config.routes.post(pattern, pluginHandler::api);
                config.routes.put(pattern, pluginHandler::api);
                config.routes.delete(pattern, pluginHandler::api);
                config.routes.patch(pattern, pluginHandler::api);
            }

            config.routes.get("/api/announcement", ctx -> {
                SystemConfig sc = SystemConfig.getInstance();
                String adminId = SessionManager.getAdminId(ctx);
                ctx.json(Map.of("mode", sc.getAnnouncementMode(), "scope", sc.getAnnouncementScope(),
                        "content", sc.getAnnouncementContent(), "loggedIn", adminId != null));
            });

        config.routes.get("/admin/skins", ctx -> {
            String adminUsername = ctx.sessionAttribute("adminUsername");
            String adminRole = im.xz.cn.server.handler.admin.AdminLayoutHelper.roleLabel(ctx);
            String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
            ctx.html(im.xz.cn.web.view.AdminPage.renderTextureRosterPage(adminUsername, adminRole, csrfToken, "SKIN"));
        });
        config.routes.get("/admin/capes", ctx -> {
            String adminUsername = ctx.sessionAttribute("adminUsername");
            String adminRole = im.xz.cn.server.handler.admin.AdminLayoutHelper.roleLabel(ctx);
            String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
            ctx.html(im.xz.cn.web.view.AdminPage.renderTextureRosterPage(adminUsername, adminRole, csrfToken, "CAPE"));
        });
        config.routes.get("/admin/texture-settings", ctx -> {
            String adminUsername = ctx.sessionAttribute("adminUsername");
            String adminRole = im.xz.cn.server.handler.admin.AdminLayoutHelper.roleLabel(ctx);
            String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
            ctx.html(im.xz.cn.web.view.AdminPage.renderTextureSettingsPage(adminUsername, adminRole, csrfToken));
        });
        config.routes.get("/admin/texture-blacklist", ctx -> {
            String adminUsername = ctx.sessionAttribute("adminUsername");
            String adminRole = im.xz.cn.server.handler.admin.AdminLayoutHelper.roleLabel(ctx);
            String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
            ctx.html(im.xz.cn.web.view.AdminPage.renderTextureBlacklistPage(adminUsername, adminRole, csrfToken));
        });
        config.routes.get("/admin/texture-assets", ctx -> ctx.redirect(
                im.xz.cn.security.AdminPermissions.has("admin.skins.view") ? "/admin/skins" : "/admin/capes"));
        config.routes.get("/admin/api/texture-assets", textureAssetHandler::list);
        config.routes.get("/admin/api/texture-assets/{id}", textureAssetHandler::get);
        config.routes.get("/admin/api/texture-assets/{id}/file", textureAssetHandler::file);
        config.routes.post("/admin/api/texture-assets/{id}/author", textureAssetHandler::updateAuthor);
        config.routes.post("/admin/api/texture-assets/{id}/name", textureAssetHandler::updateName);
        config.routes.post("/admin/api/texture-assets/{id}/skin-model", textureAssetHandler::updateSkinModel);
        config.routes.post("/admin/api/texture-assets/{id}/delete", textureAssetHandler::delete);
        
        config.routes.post("/admin/api/profiles/transfer", profileTransferHandler::transfer);
        config.routes.get("/admin/api/audit", auditHandler::list);
        config.routes.get("/admin/api/texture-storage/orphans", textureStorageHandler::report);
        config.routes.post("/admin/api/texture-storage/orphans/cleanup", textureStorageHandler::cleanup);
        
        config.routes.get("/admin/api/settings/schema", settingsHandler::schema);
        config.routes.get("/admin/api/settings", settingsHandler::list);
        config.routes.get("/admin/api/settings/{group}", settingsHandler::get);
        config.routes.put("/admin/api/settings/{group}", settingsHandler::update);
        config.routes.post("/admin/api/settings/mail/test", settingsHandler::testMail);

            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
            ErrorPageHandler.register(config.routes);
        });
    }

    public void start(String host, int port) {
        app.start(host, port);
        logger.info("[AdminServer] 管理后台已启动，{}:{}", host, port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }
}
