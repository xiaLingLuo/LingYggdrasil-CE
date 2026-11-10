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
import im.xz.cn.config.ServerConfig;
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

import im.xz.cn.server.handler.user.UserDashboardHandler;
import im.xz.cn.server.handler.user.UserTextureFileHandler;


import im.xz.cn.server.handler.user.PublicGalleryHandler;
import im.xz.cn.server.handler.user.UserHomeHandler;
import im.xz.cn.server.handler.user.UserInteractionHandler;
import im.xz.cn.server.handler.user.UserLibraryHandler;
import im.xz.cn.server.handler.user.UserFriendGraphHandler;
import im.xz.cn.server.handler.user.UserProfileTextureHandler;
import im.xz.cn.server.handler.user.UserPreferenceHandler;
import im.xz.cn.server.handler.user.UserPublicationHandler;
import im.xz.cn.server.handler.user.UserSharingHandler;
import im.xz.cn.server.handler.user.UserTextureUploadHandler;

import im.xz.cn.web.view.UserAuth;
import im.xz.cn.web.Shared;
import im.xz.cn.common.FooterInfo;
import im.xz.cn.common.IpUtil;
import im.xz.cn.rate.RateLimiter;
import im.xz.cn.texture.TextureService;
import im.xz.cn.web.ErrorPageHandler;

import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.json.JavalinJackson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import im.xz.cn.logging.logApi;

public class UserServer {
    private static final logApi logger = logApi.getLogger(UserServer.class);
    private static final String YGGDRASIL_PROXY_PREFIX = "/api/yggdrasil";
    private static final Set<String> PROXY_HOP_HEADERS = Set.of(
            "connection", "content-length", "expect", "host", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "proxy-connection", "te", "trailer", "transfer-encoding", "upgrade",
            "x-forwarded-for", "x-forwarded-host", "x-forwarded-prefix", "x-forwarded-proto");
    private static final HandlerType[] YGGDRASIL_PROXY_METHODS = {
            HandlerType.GET, HandlerType.HEAD, HandlerType.POST, HandlerType.PUT,
            HandlerType.PATCH, HandlerType.DELETE, HandlerType.OPTIONS
    };
    private static final HttpClient YGGDRASIL_PROXY_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final String[] FEATURE_ROOTS = {
            "/profiles", "/skins", "/capes", "/world", "/friends", "/shared", "/settings", "/logs",
            "/api/profiles", "/api/skins", "/api/capes",
            "/api/settings", "/api/textures", "/api/v2"
    };

    private static final String[] FEATURE_PUBLIC = {
            "/world",
            "/api/skins/download", "/api/capes/download",
            "/api/settings/theme", "/api/settings/language",
            "/api/world/textures", "/api/friends/texture",
            
            "/api/v2/texture-files"
    };

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

        FriendDao friendDao = new FriendDao(db);
        UserDashboardHandler dashHandler = new UserDashboardHandler(authService, userDao, profileDao, textureDao, textureService, cacheDao, mailService, sysConfig, favoriteDao, friendSharedDao, visibilityDao, friendDao, new im.xz.cn.profile.ProfileCredentialService(db), new im.xz.cn.profile.ProfileTextureResolver(db));
        ConfirmingFriendDao confirmingDao = new im.xz.cn.database.dao.ConfirmingFriendDao(db);
        BlockDao blockDao = new im.xz.cn.database.dao.BlockDao(db);
        UserTextureFileHandler textureFileHandler = new UserTextureFileHandler(db);

        TextureLikeDao likeDao = new TextureLikeDao(db);
        UserHomeHandler userHomeHandler = new UserHomeHandler(db,
                sha -> im.xz.cn.texture.UserTextureUrlResolver.faceUrl("SKIN", sha));
        UserInteractionHandler interactionHandler = new UserInteractionHandler(db);
        UserLibraryHandler libraryHandler = new UserLibraryHandler(db);
        UserSharingHandler sharingHandler = new UserSharingHandler(db);
        UserPublicationHandler publicationHandler = new UserPublicationHandler(db);
        UserFriendGraphHandler friendGraphHandler = new UserFriendGraphHandler(db);
        UserPreferenceHandler preferenceHandler = new UserPreferenceHandler(db);
        UserTextureUploadHandler textureUploadHandler = new UserTextureUploadHandler(db);
        UserProfileTextureHandler profileTextureHandler = new UserProfileTextureHandler(db);
        PublicGalleryHandler publicGalleryHandler = new PublicGalleryHandler(db, textureService);

        RateLimiter rateLimiter = new RateLimiter(cacheDao);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";
            config.http.maxRequestSize = Math.max(config.http.maxRequestSize,
                    TextureService.MAX_UPLOAD_BYTES + 1024L * 1024L);

            ServerFactory.configureSessionCookie(config, "LING_USER_SESSION",
                    ServerFactory.isHttpsDomain(im.xz.cn.config.ServerConfig.getInstance().getEffectiveUserDomain()));
            ServerFactory.configureThreadLocalCleanup(config);
            ServerFactory.configureSecurityHeaders(config);

            ServerFactory.configureCors(config, port);

            ServerFactory.registerStaticRoutes(config.routes);
            ServerFactory.registerIconRoutes(config.routes);
            ServerConfig serverConfig = ServerConfig.getInstance();
            boolean proxyYggdrasil = serverConfig.isYggdrasilEnabled()
                    && !SystemConfig.API_MODE_STANDALONE.equals(
                            sysConfig.getYggdrasilApiMode());
            registerYggdrasilRoute(config.routes, proxyYggdrasil, serverConfig.getYggdrasilPort());

            config.jsonMapper(new JavalinJackson());


            config.routes.before(ctx -> im.xz.cn.logging.ServiceLog.setService(im.xz.cn.logging.ServiceLog.USER));

            config.routes.before(ctx -> {
                if (isYggdrasilProxyPath(ctx.path())) return;
                var session = ctx.req().getSession(false);
                if (session != null) {
                    session.setMaxInactiveInterval(sysConfig.getUserSessionTimeoutSeconds());
                }
            });

            config.routes.before(ctx -> {
                if (isYggdrasilProxyPath(ctx.path())) return;
                String lang = SessionManager.getLanguage(ctx);
                if (lang == null) lang = ctx.cookie("LING_USER_LANG");
                if (lang == null) lang = LocaleResolver.fromAcceptLanguage(ctx.header("Accept-Language"));
                if (lang == null) lang = LocaleResolver.defaultLocale();
                LocaleContext.set(LocaleResolver.normalize(lang));
            });
            config.routes.after(ctx -> {
                LocaleContext.clear();
                im.xz.cn.security.UserPermissions.clear();
                im.xz.cn.logging.ServiceLog.clear();
            });

            config.routes.before(ctx -> {
                if (isYggdrasilProxyPath(ctx.path())) return;
                String userId = SessionManager.getUserId(ctx);
                if (userId != null) {
                    var u = userDao.findById(userId);
                    if (u != null) {
                        var group = userPermGroupDao.findByName(u.getPermGroup());
                        java.util.Set<String> parsed = group != null
                                ? im.xz.cn.security.UserPermissions.parse(group.getPermissions())
                                : java.util.Set.of();
                        boolean cancelled = hasNode(group == null ? null : group.getPermissions(),
                                im.xz.cn.security.UserPermissions.STATE_CANCELLED)
                                || (group != null && "cancelled".equalsIgnoreCase(group.getName()));
                        im.xz.cn.security.UserPermissions.set(parsed);
                        ctx.attribute("currentUser", u);
                        ctx.attribute("currentUserCancelled", cancelled);
                        return;
                    }
                }
                im.xz.cn.security.UserPermissions.set(java.util.Set.of());
            });

            config.routes.before(ctx -> {
                if (isYggdrasilProxyPath(ctx.path())) return;
                String userId = SessionManager.getUserId(ctx);
                if (userId == null) return;
                String path = ctx.path();
                String p = (path.length() > 1 && path.endsWith("/")) ? path.substring(0, path.length() - 1) : path;
                boolean protectedPath = isProtectedFeaturePath(p);
                if (!protectedPath) return;

                boolean cancelled = Boolean.TRUE.equals(ctx.attribute("currentUserCancelled"));
                if (cancelled) {
                    deny(ctx, path, I18n.t("msg.accountCancelled"));
                    return;
                }
                
                if (!im.xz.cn.security.UserPermissions.isAccessible()) {
                    deny(ctx, path, I18n.t("msg.noPermission"));
                    return;
                }
                String[] required = requiredUserPerms(p);
                if (required != null) {
                    for (String perm : required) {
                        if (im.xz.cn.security.UserPermissions.has(perm)) return;
                    }
                    deny(ctx, path, I18n.t("msg.noPermission"));
                }
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                String method = ctx.method().name();
                if (isYggdrasilProxyPath(path)) return;
                if (SessionManager.getUserId(ctx) == null || !isMutation(method)) return;
                if (!path.startsWith("/api/v2")) return;
                boolean cancelled = Boolean.TRUE.equals(ctx.attribute("currentUserCancelled"));
                if (cancelled || !im.xz.cn.security.UserPermissions.isAccessible()) {
                    deny(ctx, path, cancelled ? I18n.t("msg.accountCancelled") : I18n.t("msg.noPermission"));
                    return;
                }
                String p = path.length() > 1 && path.endsWith("/")
                        ? path.substring(0, path.length() - 1) : path;
                String[] required = requiredUserPerms(p);
                if (required != null) {
                    for (String perm : required) {
                        if (im.xz.cn.security.UserPermissions.has(perm)) return;
                    }
                    deny(ctx, path, I18n.t("msg.noPermission"));
                }
            });

            config.routes.before(ctx -> {
                String path = ctx.path();
                String method = ctx.method().name();
                if (isYggdrasilProxyPath(path)) return;
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
                if (isYggdrasilProxyPath(ctx.path())) return;
                if (!sysConfig.isEmailVerificationEnabled()) return;
                String userId = SessionManager.getUserId(ctx);
                if (userId == null) return;
                String path = ctx.path();
                if (path.equals("/email-required") || path.equals("/logout")
                        || path.equals("/world") || path.startsWith("/world/")) return;
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
                if (isYggdrasilProxyPath(ctx.path())) return;
                String path = ctx.path();
                String ratePath = path.length() > 1 && path.endsWith("/")
                        ? path.substring(0, path.length() - 1) : path;
                String method = ctx.method().name();
                String userId = SessionManager.getUserId(ctx);
                String key = (userId != null ? userId : IpUtil.getClientIp(ctx)) + ":" + method + ":" + ratePath;

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
                        boolean textureUpload = ratePath.equals("/api/skins/upload")
                                || ratePath.equals("/api/capes/upload");
                        if (textureUpload && userId != null) {
                            ctx.attribute("uploadRateLimited", true);
                            return;
                        }
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
            
        config.routes.get("/skins", ctx -> ctx.html(
                im.xz.cn.web.view.UserPage.renderTexturesV2Page(
                        SessionManager.getOrCreateCsrfToken(ctx), "SKIN")));
        config.routes.get("/capes", ctx -> ctx.html(
                im.xz.cn.web.view.UserPage.renderTexturesV2Page(
                        SessionManager.getOrCreateCsrfToken(ctx), "CAPE")));
        config.routes.get("/friends", ctx -> ctx.html(
                im.xz.cn.web.view.UserPage.renderFriendsV2Page(SessionManager.getOrCreateCsrfToken(ctx))));
        config.routes.get("/shared", ctx -> ctx.html(
                im.xz.cn.web.view.UserPage.renderSharedV2Page(SessionManager.getOrCreateCsrfToken(ctx))));
        config.routes.get("/world", ctx -> ctx.html(
                im.xz.cn.web.view.UserPage.renderWorldV2Page(SessionManager.getOrCreateCsrfToken(ctx))));
        config.routes.get("/world/{id}", ctx -> {
            
            long publicationId;
            try {
                publicationId = Long.parseLong(ctx.pathParam("id"));
            } catch (NumberFormatException error) {
                im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
                return;
            }
            var publication = new im.xz.cn.texture.publication.PublicationRepository(db)
                    .findById(publicationId)
                    .filter(im.xz.cn.texture.publication.TexturePublication::available)
                    .orElse(null);
            if (publication == null) {
                im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
                return;
            }
            var asset = new im.xz.cn.texture.asset.TextureAssetRepository(db)
                    .findById(publication.assetId()).orElse(null);
            if (asset == null || asset.state() != im.xz.cn.texture.asset.TextureAsset.AssetState.READY) {
                im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
                return;
            }
            var blob = new im.xz.cn.texture.asset.TextureBlobRepository(db)
                    .findById(asset.blobId()).orElse(null);
            String code = im.xz.cn.infrastructure.persistence.Jdbc.queryOne(db,
                    rs -> rs.getString("friend_code"),
                    "SELECT friend_code FROM users WHERE id = ?", publication.publisherId());
            if (blob == null || code == null) {
                im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
                return;
            }
            ctx.redirect("/userhome/" + code + "/public/" + blob.sha256()
                    + "?type=" + asset.type().name());
        });
        config.routes.get("/world/texture/{hash}", ctx -> ctx.redirect("/world"));




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
            config.routes.get("/api/settings/external-login", dashHandler::handleGetExternalLoginOptions);
            config.routes.post("/api/settings/external-login", dashHandler::handleSetExternalLoginOptions);
            config.routes.post("/api/settings/cancel-account/challenge",
                    dashHandler::handleCancellationChallenge);
            config.routes.post("/api/settings/cancel-account", dashHandler::handleCancelAccount);

            config.routes.post("/api/profiles/create", dashHandler::handleCreateProfile);
            config.routes.post("/api/profiles/delete", dashHandler::handleDeleteProfile);
            config.routes.post("/api/profiles/update", dashHandler::handleUpdateProfile);
            config.routes.post("/api/profiles/regenerate-token", dashHandler::handleRegenerateToken);
        config.routes.post("/api/profiles/{id}/world-tree-key/rotate", dashHandler::handleRotateWorldTreeKey);
            config.routes.get("/api/profiles", dashHandler::handleListProfiles);
            config.routes.get("/api/profiles/{id}/world-tree-key", dashHandler::handleGetWorldTreeKey);
        config.routes.get("/api/profiles/{id}/textures", profileTextureHandler::list);
        config.routes.put("/api/profiles/{id}/textures/{slot}", profileTextureHandler::apply);
        config.routes.delete("/api/profiles/{id}/textures/{slot}", profileTextureHandler::delete);
            config.routes.get("/api/userhomes/{code}", userHomeHandler::getIdentity);
            config.routes.get("/api/userhomes/{code}/publications", userHomeHandler::listPublications);
            config.routes.get("/api/userhomes/{code}/public/{hash}", userHomeHandler::getPublication);
        
        config.routes.get("/api/v2/library", libraryHandler::list);
        config.routes.get("/api/v2/publications", publicGalleryHandler::list);
        config.routes.get("/api/v2/textures", textureUploadHandler::list);
        config.routes.get("/api/v2/textures/my", textureUploadHandler::mine);
        config.routes.post("/api/v2/textures/{assetId}/alias", textureUploadHandler::alias);
        config.routes.get("/api/v2/textures/{assetId}", textureUploadHandler::detail);
        config.routes.post("/api/v2/textures/{assetId}/visibility", textureUploadHandler::visibility);
        config.routes.delete("/api/v2/textures/{assetId}", textureUploadHandler::delete);
        config.routes.post("/api/v2/textures", textureUploadHandler::upload);
        config.routes.get("/api/v2/preferences/account-password-login", preferenceHandler::get);
        config.routes.put("/api/v2/preferences/account-password-login", preferenceHandler::update);
        config.routes.get("/api/v2/friends/my-info", friendGraphHandler::myInfo);
        config.routes.post("/api/v2/friends/display-profile", friendGraphHandler::updateDisplayProfile);
        config.routes.get("/api/v2/friends", friendGraphHandler::listFriends);
        config.routes.get("/api/v2/blocks", friendGraphHandler::listBlocked);
        config.routes.post("/api/v2/blocks", friendGraphHandler::block);
        config.routes.post("/api/v2/blocks/clear", friendGraphHandler::clearBlocked);
        config.routes.delete("/api/v2/blocks/{userId}", friendGraphHandler::unblock);
        config.routes.delete("/api/v2/friends/{userId}", friendGraphHandler::removeFriend);
        config.routes.get("/api/v2/friend-requests", friendGraphHandler::listIncoming);
        config.routes.post("/api/v2/friend-requests", friendGraphHandler::sendRequest);
        config.routes.post("/api/v2/friend-requests/{id}/accept", friendGraphHandler::accept);
        config.routes.post("/api/v2/friend-requests/{id}/reject", friendGraphHandler::reject);
        config.routes.delete("/api/v2/friend-requests/{id}", friendGraphHandler::cancel);
        config.routes.get("/api/v2/publications/mine", publicationHandler::mine);
        config.routes.get("/api/v2/publications/{id}", publicGalleryHandler::detail);
        config.routes.post("/api/v2/publications", publicationHandler::publish);
        config.routes.delete("/api/v2/publications/{id}", publicationHandler::withdraw);
        config.routes.get("/api/v2/shares", sharingHandler::list);
        config.routes.post("/api/v2/shares", sharingHandler::create);
        config.routes.delete("/api/v2/shares/{id}", sharingHandler::revoke);
        config.routes.post("/api/v2/publications/{id}/like", interactionHandler::like);
        config.routes.delete("/api/v2/publications/{id}/like", interactionHandler::unlike);
        config.routes.post("/api/v2/publications/{id}/favorite", interactionHandler::favorite);
        config.routes.delete("/api/v2/publications/{id}/favorite", interactionHandler::unfavorite);
            config.routes.get("/userhome/{code}", userHomeHandler::pageProfile);
            config.routes.get("/userhome/{code}/public/{hash}", userHomeHandler::pagePublication);






















            config.routes.get("/api/v2/texture-files/{type}/{hash}/face", textureFileHandler::serveFace);
            config.routes.get("/api/v2/texture-files/{type}/{hash}", textureFileHandler::serve);
            config.routes.get("/api/announcement", ctx -> {
                SystemConfig sc = SystemConfig.getInstance();
                String userId = SessionManager.getUserId(ctx);
                ctx.json(Map.of("mode", sc.getAnnouncementMode(), "scope", sc.getAnnouncementScope(),
                        "content", sc.getAnnouncementContent(), "loggedIn", userId != null));
            });
            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
            ErrorPageHandler.register(config.routes);
        });

        ServerFactory.registerPluginCatchAll(app, true);
    }

    private static void registerYggdrasilRoute(RoutesConfig routes, boolean proxy, int port) {
        io.javalin.http.Handler handler = proxy
                ? ctx -> proxyYggdrasilRequest(ctx, port)
                : ctx -> ErrorPageHandler.handle(ctx, 404);
        for (HandlerType method : YGGDRASIL_PROXY_METHODS) {
            routes.addHttpHandler(method, YGGDRASIL_PROXY_PREFIX, handler);
            routes.addHttpHandler(method, YGGDRASIL_PROXY_PREFIX + "/{path}*", handler);
        }
    }

    private static void proxyYggdrasilRequest(Context ctx, int port) {
        try {
            String requestPath = ctx.req().getRequestURI();
            String upstreamPath = requestPath.startsWith(YGGDRASIL_PROXY_PREFIX)
                    ? requestPath.substring(YGGDRASIL_PROXY_PREFIX.length()) : "/";
            if (upstreamPath.isEmpty()) upstreamPath = "/";
            if (!upstreamPath.startsWith("/")) upstreamPath = "/" + upstreamPath;

            StringBuilder target = new StringBuilder("http://127.0.0.1:")
                    .append(port).append(upstreamPath);
            String query = ctx.req().getQueryString();
            if (query != null && !query.isEmpty()) target.append('?').append(query);

            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(target.toString()))
                    .timeout(Duration.ofSeconds(30));
            ctx.headerMap().forEach((name, value) -> {
                if (!PROXY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    request.header(name, value);
                }
            });

            String forwardedFor = ctx.header("X-Forwarded-For");
            request.header("X-Forwarded-For", forwardedFor == null || forwardedFor.isBlank() ? ctx.ip() : forwardedFor);
            request.header("X-Forwarded-Host", ctx.header("Host") == null ? ctx.host() : ctx.header("Host"));
            String forwardedProto = ctx.header("X-Forwarded-Proto");
            request.header("X-Forwarded-Proto", forwardedProto == null || forwardedProto.isBlank() ? ctx.scheme() : forwardedProto);
            request.header("X-Forwarded-Prefix", YGGDRASIL_PROXY_PREFIX);

            String method = ctx.method().name();
            if ("GET".equals(method)) {
                request.GET();
            } else if ("HEAD".equals(method)) {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                request.method(method, HttpRequest.BodyPublishers.ofByteArray(ctx.bodyAsBytes()));
            }

            HttpResponse<byte[]> response = YGGDRASIL_PROXY_CLIENT.send(
                    request.build(), HttpResponse.BodyHandlers.ofByteArray());
            ctx.status(response.statusCode());
            response.headers().map().forEach((name, values) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                if (PROXY_HOP_HEADERS.contains(lower)) return;
                for (int i = 0; i < values.size(); i++) {
                    String value = "location".equals(lower)
                            ? rewriteProxyLocation(values.get(i), port) : values.get(i);
                    if ("content-type".equals(lower)) {
                        ctx.contentType(value);
                    } else if (i == 0) {
                        ctx.header(name, value);
                    } else {
                        ctx.addHeader(name, value);
                    }
                }
            });
            ctx.result(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[Yggdrasil Proxy] Request interrupted: {} {}", ctx.method(), ctx.path());
            ErrorPageHandler.handle(ctx, 502);
        } catch (Exception e) {
            logger.warn("[Yggdrasil Proxy] Upstream request failed: {} {} ({})",
                    ctx.method(), ctx.path(), e.getMessage());
            ErrorPageHandler.handle(ctx, 502);
        }
    }

    private static String rewriteProxyLocation(String location, int port) {
        try {
            URI uri = URI.create(location);
            String path = uri.getRawPath();
            if (uri.isAbsolute()) {
                if (!"127.0.0.1".equals(uri.getHost()) || uri.getPort() != port) return location;
            } else if (path == null || !path.startsWith("/")) {
                return location;
            }
            if (path == null || path.isEmpty()) path = "/";
            String publicPath = path.equals(YGGDRASIL_PROXY_PREFIX)
                    || path.startsWith(YGGDRASIL_PROXY_PREFIX + "/")
                    ? path : YGGDRASIL_PROXY_PREFIX + path;
            if (uri.getRawQuery() != null) publicPath += "?" + uri.getRawQuery();
            if (uri.getRawFragment() != null) publicPath += "#" + uri.getRawFragment();
            return publicPath;
        } catch (Exception e) {
            return location;
        }
    }

    private static boolean isYggdrasilProxyPath(String path) {
        return YGGDRASIL_PROXY_PREFIX.equals(path) || path.startsWith(YGGDRASIL_PROXY_PREFIX + "/");
    }

    private static boolean isProtectedFeaturePath(String p) {
        boolean underRoot = false;
        for (String root : FEATURE_ROOTS) {
            if (p.equals(root) || p.startsWith(root + "/")) { underRoot = true; break; }
        }
        if (!underRoot) return false;
        
        if (p.equals("/api/v2/publications") || p.matches("/api/v2/publications/\\d+")) {
            return false;
        }
        for (String pub : FEATURE_PUBLIC) {
            if (p.equals(pub) || p.startsWith(pub + "/")) return false;
        }
        return true;
    }

    private static boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private static boolean hasNode(String stored, String node) {
        if (stored == null || stored.isBlank()) return false;
        for (String part : stored.split(",")) {
            if (node.equals(part.trim())) return true;
        }
        return false;
    }

    private static void deny(Context ctx, String path, String message) {
        if (path.startsWith("/api/")) {
            ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN", "message", message));
        } else {
            ctx.redirect("/dashboard");
        }
        ctx.skipRemainingHandlers();
    }

    private static String[] requiredUserPerms(String path) {
        String p = path;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        switch (p) {
            case "/profiles": return new String[]{"user.profiles"};
            case "/skins": return new String[]{"user.skins"};
            case "/capes": return new String[]{"user.capes"};
            case "/shared": return new String[]{"user.world"};
            case "/friends": return new String[]{"user.friends"};
            case "/settings":
            case "/logs":
            case "/logs/download":
            case "/logs/clear": return new String[]{"user.settings"};
            default: break;
        }
        if (p.startsWith("/api/v2/preferences")) {
            return new String[]{"user.settings"};
        }
        if (p.startsWith("/api/v2/textures") || p.startsWith("/api/v2/library")) {
            return new String[]{"user.skins", "user.capes"};
        }
        if (p.startsWith("/api/v2/publications")) {
            return new String[]{"user.world"};
        }
        if (p.startsWith("/api/v2/shares")) {
            return new String[]{"user.friends", "user.world"};
        }
        if (p.startsWith("/api/v2/friends") || p.startsWith("/api/v2/friend-requests")) {
            return new String[]{"user.friends"};
        }
        if (p.startsWith("/api/v2/blocks")) {
            return new String[]{"user.friends", "user.settings"};
        }
        if (p.startsWith("/profiles/") && p.matches("/profiles/[^/]+/textures.*")) {
            return new String[]{"user.profiles"};
        }
        if (p.startsWith("/api/profiles") || p.equals("/api/textures/my")) {
            return new String[]{"user.profiles"};
        }
        if (p.startsWith("/api/skins")) return new String[]{"user.skins"};
        if (p.startsWith("/api/capes")) return new String[]{"user.capes"};
        if (p.startsWith("/api/friends/share-texture")
                || p.startsWith("/api/friends/unshare-texture")
                || p.startsWith("/api/friends/received-texture")
                || p.contains("/shared-textures")
                || p.contains("/my-shared")) {
            return new String[]{"user.friends", "user.world"};
        }
        if (p.startsWith("/api/friends/blocked") || p.equals("/api/friends/unblock")) {
            return new String[]{"user.friends", "user.settings"};
        }
        if (p.startsWith("/api/friends")) return new String[]{"user.friends"};
        if (p.startsWith("/api/shared")
                || p.startsWith("/api/world/like")
                || p.startsWith("/api/world/favorite")) {
            return new String[]{"user.world"};
        }
        if (p.startsWith("/api/settings/nickname")
                || p.startsWith("/api/settings/email")
                || p.startsWith("/api/settings/password")
                || p.startsWith("/api/settings/send-verify-code")) {
            return new String[]{"user.settings"};
        }
        
        if (p.startsWith("/api/settings")) {
            return new String[]{"user.settings"};
        }
        return null;
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
