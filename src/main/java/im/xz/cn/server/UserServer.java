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
import im.xz.cn.database.CacheDao;
import im.xz.cn.database.ConfirmingFriendDao;
import im.xz.cn.database.BlockDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.FriendDao;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.User;
import im.xz.cn.mail.MailService;
import im.xz.cn.server.handler.UserAuthHandler;
import im.xz.cn.server.handler.UserCapeHandler;
import im.xz.cn.server.handler.UserDashboardHandler;
import im.xz.cn.server.handler.UserFriendHandler;
import im.xz.cn.server.handler.UserSkinHandler;
import im.xz.cn.something.web.UserAuth;
import im.xz.cn.something.web.Shared;
import im.xz.cn.util.FooterInfo;
import im.xz.cn.util.TextureService;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServer {
    private static final Logger logger = LoggerFactory.getLogger(UserServer.class);

    private final Javalin app;
    private final int port;

    public UserServer(int port, DatabaseManager db, MailService mailService) {
        this.port = port;

        AuthService authService = new AuthService(db);
        UserDao userDao = authService.getUserDao();
        ProfileDao profileDao = authService.getProfileDao();
        CacheDao cacheDao = authService.getCacheDao();
        SystemConfig sysConfig = SystemConfig.getInstance();

        UserAuthHandler authHandler = new UserAuthHandler(authService, userDao, cacheDao, mailService, sysConfig);
        im.xz.cn.database.TextureDao textureDao = new im.xz.cn.database.TextureDao(db);
        TextureService textureService = new TextureService(sysConfig, cacheDao);
        UserSkinHandler skinHandler = new UserSkinHandler(textureDao, textureService, userDao);
        UserCapeHandler capeHandler = new UserCapeHandler(textureDao, textureService, userDao);
        UserDashboardHandler dashHandler = new UserDashboardHandler(authService, userDao, profileDao, textureDao, textureService, cacheDao, mailService, sysConfig);
        FriendDao friendDao = new FriendDao(db);
        ConfirmingFriendDao confirmingDao = new im.xz.cn.database.ConfirmingFriendDao(db);
        BlockDao blockDao = new im.xz.cn.database.BlockDao(db);
        UserFriendHandler friendHandler = new UserFriendHandler(userDao, profileDao, friendDao, confirmingDao, blockDao, textureService, sysConfig);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            ServerFactory.configureSessionCookie(config);

            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.allowHost(
                        "http://localhost:35565",
                        "http://localhost:35577",
                        "http://localhost:35599",
                        "http://localhost:35598"
                );
                rule.allowCredentials = true;
            }));

            config.staticFiles.add("/static", Location.CLASSPATH);

            config.jsonMapper(new JavalinJackson());


            config.routes.before(ctx -> {
                var session = ctx.req().getSession(false);
                if (session != null) {
                    session.setMaxInactiveInterval(30 * 60);
                }
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                String method = ctx.method().name();
                if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
                    if (!path.equals("/api/login") && !path.equals("/api/register")
                            && !path.equals("/api/verify-email") && !path.equals("/api/resend-code")) {
                        if (!SessionManager.validateCsrfToken(ctx)) {
                            ctx.status(403);
                            ctx.json(Map.of("success", false, "message", "CSRF 验证失败"));
                            ctx.skipRemainingHandlers();
                        }
                    }
                }
            });

            config.routes.before(ctx -> {
                if (!sysConfig.isEmailVerificationEnabled()) return;
                String userId = SessionManager.getUserId(ctx);
                if (userId == null) return;
                String path = ctx.path();
                if (path.equals("/email-required") || path.equals("/logout")) return;
                if (path.equals("/api/send-email-verify") || path.equals("/api/verify-my-email")) return;
                User user = userDao.findById(userId);
                if (user != null && !user.isEmailVerified()) {
                    if (path.startsWith("/api/")) {
                        ctx.status(403);
                        ctx.json(Map.of("success", false, "message", "需要先完成邮箱验证才能执行操作"));
                    } else {
                        ctx.redirect("/email-required");
                    }
                    ctx.skipRemainingHandlers();
                }
            });

            config.routes.get("/", ctx -> ctx.redirect("/meow"));

            config.routes.get("/meow", ctx -> {
                try (var is = UserServer.class.getResourceAsStream("/index.html")) {
                    if (is != null) {
                        String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        html = html.replace("</head>", "<link rel=\"stylesheet\" href=\"/css/announcement.css\"></head>");
                        html = html.replace("</body>", "<script src=\"/js/announcement.js\"></script></body>");
                        html = FooterInfo.injectFooterRecords(html);
                        ctx.contentType("text/html; charset=utf-8");
                        ctx.result(html);
                    } else {
                        ctx.status(404).result("Not found");
                    }
                }
            });

            config.routes.get("/login", ctx -> {
                if (SessionManager.isLoggedIn(ctx)) {
                    String uid = SessionManager.getUserId(ctx);
                    if (sysConfig.isEmailVerificationEnabled()) {
                        User u = userDao.findById(uid);
                        if (u != null && !u.isEmailVerified()) {
                            ctx.redirect("/email-required");
                            return;
                        }
                    }
                    ctx.redirect("/dashboard");
                    return;
                }
                ctx.html(UserAuth.renderLoginPage(sysConfig.getSiteName()));
            });

            config.routes.get("/register", ctx -> {
                if (SessionManager.isLoggedIn(ctx)) {
                    ctx.redirect("/dashboard");
                    return;
                }
                ctx.html(UserAuth.renderRegisterPage(sysConfig.getSiteName(), sysConfig.isRegistrationEnabled(), sysConfig.isEmailVerificationEnabled()));
            });

            config.routes.get("/verify-email", ctx -> {
                if (SessionManager.isLoggedIn(ctx)) {
                    ctx.redirect("/dashboard");
                    return;
                }
                ctx.html(UserAuth.renderVerifyEmailPage());
            });

            config.routes.get("/email-required", ctx -> {
                String uid = SessionManager.getUserId(ctx);
                if (uid == null) {
                    ctx.redirect("/login");
                    return;
                }
                User u = userDao.findById(uid);
                if (u == null) {
                    ctx.redirect("/login");
                    return;
                }
                if (!sysConfig.isEmailVerificationEnabled() || u.isEmailVerified()) {
                    ctx.redirect("/dashboard");
                    return;
                }
                String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
                String csrfInject = Shared.csrfInject(csrfToken);
                ctx.html(csrfInject + UserAuth.renderEmailRequiredPage(u.getEmail()));
            });

            config.routes.get("/logout", authHandler::handleLogout);

            config.routes.get("/dashboard", dashHandler::handleDashboardPage);
            config.routes.get("/settings", dashHandler::handleSettingsPage);
            config.routes.get("/profiles", dashHandler::handleProfilesPage);
            config.routes.get("/skins", skinHandler::skinsPage);
            config.routes.get("/capes", capeHandler::capesPage);
            config.routes.get("/friends", friendHandler::friendsPage);

            config.routes.post("/api/login", authHandler::handleLogin);
            config.routes.post("/api/register", authHandler::handleRegister);
            config.routes.post("/api/verify-email", authHandler::handleVerifyEmail);
            config.routes.post("/api/resend-code", authHandler::handleResendCode);

            config.routes.post("/api/send-email-verify", authHandler::handleSendEmailVerify);
            config.routes.post("/api/verify-my-email", authHandler::handleVerifyMyEmail);

            config.routes.post("/api/settings/nickname", dashHandler::handleChangeNickname);
            config.routes.post("/api/settings/email", dashHandler::handleChangeEmail);
            config.routes.post("/api/settings/password", dashHandler::handleChangePassword);
            config.routes.post("/api/settings/send-verify-code", dashHandler::handleSendSettingsCode);

            config.routes.post("/api/profiles/create", dashHandler::handleCreateProfile);
            config.routes.post("/api/profiles/delete", dashHandler::handleDeleteProfile);
            config.routes.post("/api/profiles/update", dashHandler::handleUpdateProfile);
            config.routes.post("/api/profiles/regenerate-token", dashHandler::handleRegenerateToken);
            config.routes.get("/api/profiles", dashHandler::handleListProfiles);
            config.routes.get("/api/textures/my", dashHandler::handleMyTextures);

            config.routes.get("/api/skins", skinHandler::getSkins);
            config.routes.post("/api/skins/upload", skinHandler::uploadSkin);
            config.routes.post("/api/skins/delete", skinHandler::deleteSkin);
            config.routes.post("/api/skins/alias", skinHandler::updateAlias);
            config.routes.get("/api/skins/download", skinHandler::downloadSkin);

            config.routes.get("/api/capes", capeHandler::getCapes);
            config.routes.post("/api/capes/upload", capeHandler::uploadCape);
            config.routes.post("/api/capes/delete", capeHandler::deleteCape);
            config.routes.post("/api/capes/alias", capeHandler::updateAlias);
            config.routes.get("/api/capes/download", capeHandler::downloadCape);
            config.routes.get("/api/friends", friendHandler::getFriends);
            config.routes.get("/api/friends/my-info", friendHandler::getMyInfo);
            config.routes.post("/api/friends/display-profile", friendHandler::updateDisplayProfile);
            config.routes.post("/api/friends/add", friendHandler::addFriend);
            config.routes.post("/api/friends/delete", friendHandler::deleteFriend);
            config.routes.post("/api/friends/request/accept", friendHandler::acceptRequest);
            config.routes.post("/api/friends/request/cancel", friendHandler::cancelRequest);
            config.routes.post("/api/friends/block", friendHandler::blockUser);
            config.routes.post("/api/friends/unblock", friendHandler::unblockUser);
            config.routes.get("/api/friends/blocked", friendHandler::getBlocked);
            config.routes.get("/api/friends/blocked/count", friendHandler::getBlockedCount);
            config.routes.post("/api/friends/blocked/clear", friendHandler::clearBlocked);
            config.routes.get("/api/friends/texture/{type}/{hash}", friendHandler::serveTexture);
            config.routes.get("/api/announcement", ctx -> {
                SystemConfig sc = SystemConfig.getInstance();
                String userId = SessionManager.getUserId(ctx);
                ctx.json(Map.of("mode", sc.getAnnouncementMode(), "scope", sc.getAnnouncementScope(),
                        "content", sc.getAnnouncementContent(), "loggedIn", userId != null));
            });
            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
        });
    }

    public void start() {
        app.start(port);
        logger.info("User server started on port {}", port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }
}
