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


import im.xz.cn.i18n.I18n;
import im.xz.cn.bootstrap.ServerFactory;
import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.i18n.LocaleContext;
import im.xz.cn.i18n.LocaleResolver;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.database.dao.ConfirmingFriendDao;
import im.xz.cn.database.dao.BlockDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.FriendDao;
import im.xz.cn.database.dao.FriendSharedTextureDao;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.database.dao.TextureFavoriteDao;
import im.xz.cn.database.dao.TextureLikeDao;
import im.xz.cn.database.dao.TextureVisibilityDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.User;
import im.xz.cn.mail.MailService;
import im.xz.cn.server.handler.user.UserAuthHandler;
import im.xz.cn.server.handler.user.UserCapeHandler;
import im.xz.cn.server.handler.user.UserDashboardHandler;
import im.xz.cn.server.handler.user.UserFriendHandler;
import im.xz.cn.server.handler.user.UserSkinHandler;
import im.xz.cn.server.handler.user.UserWorldHandler;
import im.xz.cn.web.view.UserAuth;
import im.xz.cn.web.Shared;
import im.xz.cn.common.FooterInfo;
import im.xz.cn.common.IpUtil;
import im.xz.cn.rate.RateLimiter;
import im.xz.cn.texture.TextureService;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

import java.util.Map;

import im.xz.cn.logging.logApi;

public class UserServer {
    private static final logApi logger = logApi.getLogger(UserServer.class);

    private final Javalin app;
    private final int port;

    public UserServer(int port, DatabaseManager db, MailService mailService) {
        this.port = port;

        AuthService authService = new AuthService(db);
        UserDao userDao = authService.getUserDao();
        ProfileDao profileDao = authService.getProfileDao();
        CacheDao cacheDao = authService.getCacheDao();
        SystemConfig sysConfig = SystemConfig.getInstance();
        im.xz.cn.database.dao.UserPermGroupDao userPermGroupDao = new im.xz.cn.database.dao.UserPermGroupDao(db);

        UserAuthHandler authHandler = new UserAuthHandler(authService, userDao, cacheDao, mailService, sysConfig);
        im.xz.cn.database.dao.TextureDao textureDao = new im.xz.cn.database.dao.TextureDao(db);
        TextureService textureService = new TextureService(sysConfig, cacheDao);
        TextureVisibilityDao visibilityDao = new TextureVisibilityDao(db);
        TextureFavoriteDao favoriteDao = new TextureFavoriteDao(db);
        FriendSharedTextureDao friendSharedDao = new FriendSharedTextureDao(db);
            UserSkinHandler skinHandler = new UserSkinHandler(textureDao, textureService, userDao, visibilityDao, sysConfig);
            UserCapeHandler capeHandler = new UserCapeHandler(textureDao, textureService, userDao, visibilityDao, sysConfig);
        FriendDao friendDao = new FriendDao(db);
        UserDashboardHandler dashHandler = new UserDashboardHandler(authService, userDao, profileDao, textureDao, textureService, cacheDao, mailService, sysConfig, favoriteDao, friendSharedDao, visibilityDao, friendDao);
        ConfirmingFriendDao confirmingDao = new im.xz.cn.database.dao.ConfirmingFriendDao(db);
        BlockDao blockDao = new im.xz.cn.database.dao.BlockDao(db);
        UserFriendHandler friendHandler = new UserFriendHandler(userDao, profileDao, friendDao, confirmingDao, blockDao, textureService, sysConfig, textureDao, visibilityDao, friendSharedDao);
        TextureLikeDao likeDao = new TextureLikeDao(db);
        UserWorldHandler worldHandler = new UserWorldHandler(userDao, profileDao, textureDao, likeDao, favoriteDao, visibilityDao, friendSharedDao, friendDao, textureService, sysConfig);
        RateLimiter rateLimiter = new RateLimiter(cacheDao);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            ServerFactory.configureSessionCookie(config, "LING_USER_SESSION");
            ServerFactory.configureThreadLocalCleanup(config);
            ServerFactory.configureSecurityHeaders(config);

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
            ServerFactory.registerIconRoutes(config.routes);

            config.jsonMapper(new JavalinJackson());


            config.routes.before(ctx -> im.xz.cn.logging.ServiceLog.setService(im.xz.cn.logging.ServiceLog.USER));

            config.routes.before(ctx -> {
                var session = ctx.req().getSession(false);
                if (session != null) {
                    session.setMaxInactiveInterval(30 * 24 * 60 * 60);
                }
            });

            config.routes.before(ctx -> {
                String lang = SessionManager.getLanguage(ctx);
                if (lang == null) lang = ctx.cookie("LING_USER_LANG");
                if (lang == null) lang = LocaleResolver.fromAcceptLanguage(ctx.header("Accept-Language"));
                if (lang == null) lang = sysConfig.getDefaultLanguage();
                LocaleContext.set(LocaleResolver.normalize(lang));
            });
            config.routes.after(ctx -> {
                LocaleContext.clear();
                im.xz.cn.security.UserPermissions.clear();
                im.xz.cn.logging.ServiceLog.clear();
            });

            config.routes.before(ctx -> {
                String userId = SessionManager.getUserId(ctx);
                if (userId != null) {
                    var u = userDao.findById(userId);
                    if (u != null) {
                        var group = userPermGroupDao.findByName(u.getPermGroup());
                        im.xz.cn.security.UserPermissions.set(group != null
                                ? im.xz.cn.security.UserPermissions.parse(group.getPermissions())
                                : java.util.Set.of());
                        return;
                    }
                }
                im.xz.cn.security.UserPermissions.set(java.util.Set.of());
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                String method = ctx.method().name();
                if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) {
                    if (!path.equals("/api/login") && !path.equals("/api/register")
                            && !path.equals("/api/verify-email") && !path.equals("/api/resend-code")) {
                        if (ctx.req().getSession(false) != null && !SessionManager.validateCsrfToken(ctx)) {
                            ctx.status(403);
                            ctx.json(Map.of("success", false, "message", I18n.t("msg.csrfFailed")));
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
                if (path.equals("/email-required") || path.equals("/logout") || path.equals("/world")) return;
                if (path.equals("/api/send-email-verify") || path.equals("/api/verify-my-email")) return;
                User user = userDao.findById(userId);
                if (user != null && !user.isEmailVerified()) {
                    if (path.startsWith("/api/")) {
                        ctx.status(403);
                        ctx.json(Map.of("success", false, "message", I18n.t("msg.emailVerifyRequired")));
                    } else {
                        ctx.redirect("/email-required");
                    }
                    ctx.skipRemainingHandlers();
                }
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                String method = ctx.method().name();
                String userId = SessionManager.getUserId(ctx);
                String key = (userId != null ? userId : IpUtil.getClientIp(ctx)) + ":" + method + ":" + path;

                if (method.equals("GET")) {
                    Integer maxReq = RateLimiter.getRateLimit(path);
                    if (maxReq != null && !rateLimiter.checkRate(key, maxReq, 60000)) {
                        ctx.status(429);
                        ctx.json(Map.of("success", false, "message", I18n.t("msg.tooFrequent")));
                        ctx.skipRemainingHandlers();
                    }
                } else if (method.equals("POST")) {
                    Integer interval = RateLimiter.getInterval(method, path);
                    if (interval != null && !rateLimiter.check(key, interval)) {
                        ctx.status(429);
                        ctx.json(Map.of("success", false, "message", I18n.t("msg.tooFrequent")));
                        ctx.skipRemainingHandlers();
                    }
                }
            });

            config.routes.get("/", ctx -> ctx.redirect("/meow"));

            config.routes.get("/meow", ctx -> {
                String userId = SessionManager.getUserId(ctx);
                User user = userId != null ? userDao.findById(userId) : null;
                String csrfToken = user != null ? SessionManager.getOrCreateCsrfToken(ctx) : null;
                ctx.html(im.xz.cn.web.view.HomePage.renderHomePage(user, csrfToken));
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
                ctx.html(UserAuth.renderEmailRequiredPage(u.getEmail(), csrfInject));
            });

            config.routes.get("/logout", authHandler::handleLogout);

            config.routes.get("/dashboard", dashHandler::handleDashboardPage);
            config.routes.get("/logs", dashHandler::handleLogsPage);
            config.routes.get("/logs/download", dashHandler::handleDownloadLog);
            config.routes.post("/logs/clear", dashHandler::handleClearLog);
            config.routes.get("/settings", dashHandler::handleSettingsPage);
            config.routes.get("/profiles", dashHandler::handleProfilesPage);
            config.routes.get("/skins", skinHandler::skinsPage);
            config.routes.get("/capes", capeHandler::capesPage);
            config.routes.get("/friends", friendHandler::friendsPage);

            config.routes.get("/world", worldHandler::worldPage);
            config.routes.get("/shared", worldHandler::sharedPage);
            config.routes.get("/api/world/textures", worldHandler::getPublicTextures);
            config.routes.post("/api/world/like", worldHandler::toggleLike);
            config.routes.post("/api/world/favorite", worldHandler::toggleFavorite);
            config.routes.post("/api/world/favorite/alias", worldHandler::updateFavoriteAlias);
            config.routes.post("/api/textures/visibility", worldHandler::setVisibility);
            config.routes.get("/api/textures/visibility", worldHandler::getVisibility);
            config.routes.get("/api/shared/my", worldHandler::getSharedTextures);
            config.routes.get("/api/friends/{friendId}/shared-textures", worldHandler::getFriendSharedTextures);
            config.routes.get("/api/friends/{friendId}/my-shared", worldHandler::getMySharedToFriend);
            config.routes.post("/api/friends/share-texture", worldHandler::shareToFriend);
            config.routes.post("/api/friends/unshare-texture", worldHandler::unshareFromFriend);
            config.routes.post("/api/friends/return-texture", worldHandler::returnSharedTexture);

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
            config.routes.get("/api/settings/theme", dashHandler::handleGetTheme);
            config.routes.post("/api/settings/theme", dashHandler::handleSetTheme);
            config.routes.get("/api/settings/language", dashHandler::handleGetLanguage);
            config.routes.post("/api/settings/language", dashHandler::handleSetLanguage);

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
            config.routes.get("/api/publicTexture/{type}/{hash}", friendHandler::serveTexture);
            config.routes.get("/api/announcement", ctx -> {
                SystemConfig sc = SystemConfig.getInstance();
                String userId = SessionManager.getUserId(ctx);
                ctx.json(Map.of("mode", sc.getAnnouncementMode(), "scope", sc.getAnnouncementScope(),
                        "content", sc.getAnnouncementContent(), "loggedIn", userId != null));
            });
            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
        });

        ServerFactory.registerPluginCatchAll(app, true);
    }

    public void start(String host, int port) {
        app.start(host, port);
        logger.info("User server started on {}:{}", host, port);
    }

    public void stop() {
        app.stop();
    }

    public Javalin getApp() {
        return app;
    }
}
