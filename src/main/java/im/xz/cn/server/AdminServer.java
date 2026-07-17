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

import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.*;
import im.xz.cn.model.Admin;
import im.xz.cn.server.handler.*;
import im.xz.cn.util.FooterInfo;
import im.xz.cn.util.TextureService;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminServer {
    private static final Logger logger = LoggerFactory.getLogger(AdminServer.class);

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

        TextureDao textureDao = new TextureDao(db);
        TextureService textureService = new TextureService(systemConfig, cacheDao);

        AdminAuthHandler authHandler = new AdminAuthHandler(authService, cacheDao);
        AdminDashboardHandler dashboardHandler = new AdminDashboardHandler(userDao, profileDao, tokenDao, adminDao);
        AdminSystemHandler systemHandler = new AdminSystemHandler(systemConfig, cacheDao, tokenDao, db);
        AdminSecurityHandler securityHandler = new AdminSecurityHandler(systemConfig, db);
        AdminUserHandler userHandler = new AdminUserHandler(userDao, adminDao, systemConfig);
        AdminAdminHandler adminAdminHandler = new AdminAdminHandler(adminDao);
        AdminAppInfoHandler appInfoHandler = new AdminAppInfoHandler();
        AdminProfilesHandler profilesHandler = new AdminProfilesHandler(profileDao, userDao, adminDao);
        AdminSkinHandler skinHandler = new AdminSkinHandler(textureDao, textureService, userDao, db);
        AdminCapeHandler capeHandler = new AdminCapeHandler(textureDao, textureService, userDao, db);
        AdminYggdrasilHandler yggdrasilHandler = new AdminYggdrasilHandler(systemConfig, db);

        this.app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            ServerFactory.configureSessionCookie(config);

            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(rule ->
                            rule.allowHost(
                                    "http://localhost:35565",
                                    "http://localhost:35577",
                                    "http://localhost:35599",
                                    "http://localhost:35598"
                            )
                    )
            );

            config.staticFiles.add("/static", Location.CLASSPATH);

            config.jsonMapper(new JavalinJackson());

            config.routes.before("/admin/*", ctx -> {
                var session = ctx.req().getSession(false);
                if (session != null) {
                    session.setMaxInactiveInterval(15 * 60);
                }
                String path = ctx.path();
                if (path.equals("/admin/login") || path.equals("/admin/api/login")) {
                    return;
                }
                String adminId = SessionManager.getAdminId(ctx);
                if (adminId == null) {
                    if (path.startsWith("/admin/api/")) {
                        ctx.status(401);
                        ctx.json(Map.of("success", false, "message", "未登录"));
                    } else {
                        ctx.redirect("/admin/login");
                    }
                    ctx.skipRemainingHandlers();
                    return;
                }
                Admin admin = adminDao.findById(adminId);
                if (admin == null) {
                    SessionManager.invalidateAdmin(ctx);
                    ctx.status(401);
                    ctx.json(Map.of("success", false, "message", "账户已失效"));
                    ctx.skipRemainingHandlers();
                    return;
                }
                SessionManager.setAdminRole(ctx, admin.getRole().name());

                if (!SessionManager.validateClientFingerprint(ctx)) {
                    SessionManager.invalidateAdmin(ctx);
                    ctx.status(401);
                    ctx.json(Map.of("success", false, "message", "Session 异常，请重新登录"));
                    ctx.skipRemainingHandlers();
                    return;
                }

                String method = ctx.method().name();
                if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
                    if (!SessionManager.validateCsrfToken(ctx)) {
                        ctx.status(403);
                        ctx.json(Map.of("success", false, "message", "CSRF 验证失败"));
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
            config.routes.get("/admin/admins", adminAdminHandler::adminsPage);
            config.routes.get("/admin/appinfo", appInfoHandler::appInfoPage);
            config.routes.get("/admin/yggdrasil", yggdrasilHandler::yggdrasilPage);

            config.routes.post("/admin/api/login", authHandler::login);

            config.routes.get("/admin/api/dashboard/stats", dashboardHandler::getStats);

            config.routes.get("/admin/api/system/settings", systemHandler::getSettings);
            config.routes.post("/admin/api/system/settings", systemHandler::updateSettings);
            config.routes.post("/admin/api/system/mail", systemHandler::updateMail);
            config.routes.post("/admin/api/system/cache/clear", systemHandler::clearCache);

            config.routes.get("/admin/api/security/settings", securityHandler::getSettings);
            config.routes.post("/admin/api/security/settings", securityHandler::updateSettings);

            config.routes.get("/admin/api/users", userHandler::getUsers);
            config.routes.post("/admin/api/users/ban", userHandler::banUser);
            config.routes.post("/admin/api/users/unban", userHandler::unbanUser);
            config.routes.post("/admin/api/users/delete", userHandler::deleteUser);
            config.routes.post("/admin/api/users/username", userHandler::updateUsername);
            config.routes.post("/admin/api/users/email", userHandler::updateEmail);

            config.routes.get("/admin/api/profiles", profilesHandler::getProfiles);
            config.routes.post("/admin/api/profiles/create", profilesHandler::createProfile);
            config.routes.post("/admin/api/profiles/delete", profilesHandler::deleteProfile);
            config.routes.post("/admin/api/profiles/update", profilesHandler::updateProfile);
            config.routes.post("/admin/api/profiles/transfer", profilesHandler::transferProfile);
            config.routes.post("/admin/api/profiles/clear-textures", profilesHandler::clearProfileTextures);

            config.routes.get("/admin/api/admins", adminAdminHandler::getAdmins);
            config.routes.post("/admin/api/admins/create", adminAdminHandler::createAdmin);
            config.routes.post("/admin/api/admins/delete", adminAdminHandler::deleteAdmin);
            config.routes.post("/admin/api/admins/update", adminAdminHandler::updateAdmin);

            config.routes.get("/admin/api/yggdrasil/settings", yggdrasilHandler::getSettings);
            config.routes.post("/admin/api/yggdrasil/settings", yggdrasilHandler::updateSettings);
            config.routes.post("/admin/api/yggdrasil/regenerate-keys", yggdrasilHandler::regenerateKeys);
            config.routes.post("/admin/api/yggdrasil/switch-mode", yggdrasilHandler::switchMode);

            config.routes.get("/admin/api/appinfo", appInfoHandler::getAppInfo);

            config.routes.get("/api/announcement", ctx -> {
                SystemConfig sc = SystemConfig.getInstance();
                String adminId = SessionManager.getAdminId(ctx);
                ctx.json(Map.of("mode", sc.getAnnouncementMode(), "scope", sc.getAnnouncementScope(),
                        "content", sc.getAnnouncementContent(), "loggedIn", adminId != null));
            });

            config.routes.get("/admin/skins", skinHandler::skinsPage);
            config.routes.get("/admin/capes", capeHandler::capesPage);

            config.routes.get("/admin/api/skins", skinHandler::getSkins);
            config.routes.post("/admin/api/skins/upload", skinHandler::uploadSkin);
            config.routes.post("/admin/api/skins/delete", skinHandler::deleteSkin);
            config.routes.post("/admin/api/skins/alias", skinHandler::updateAlias);
            config.routes.get("/admin/api/skins/download", skinHandler::downloadSkin);

            config.routes.get("/admin/api/capes", capeHandler::getCapes);
            config.routes.post("/admin/api/capes/upload", capeHandler::uploadCape);
            config.routes.post("/admin/api/capes/delete", capeHandler::deleteCape);
            config.routes.post("/admin/api/capes/alias", capeHandler::updateAlias);
            config.routes.get("/admin/api/capes/download", capeHandler::downloadCape);

            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
        });
    }

    public void start() {
        app.start(port);
        logger.info("[AdminServer] 管理后台已启动，端口: {}", port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }
}
