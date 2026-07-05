package im.xz.cn.server;

import io.javalin.Javalin;
import io.javalin.http.Context;
import im.xz.cn.auth.AuthService;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.CacheDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.TextureDao;
import im.xz.cn.server.handler.YggdrasilAuthHandler;
import im.xz.cn.server.handler.YggdrasilSessionHandler;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TextureService;
import im.xz.cn.util.UuidUtil;
import im.xz.cn.util.YggdrasilKeyManager;
import im.xz.cn.util.YggdrasilUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class YggdrasilServer {
    private static final Logger logger = LoggerFactory.getLogger(YggdrasilServer.class);
    private final Javalin app;
    private final AuthService authService;
    private final DatabaseManager db;

    public YggdrasilServer(DatabaseManager db) {
        this.db = db;
        this.authService = new AuthService(db);

        YggdrasilAuthHandler.init(authService, SystemConfig.getInstance());
        YggdrasilSessionHandler.init(authService);

        this.app = ServerFactory.create(35577, null, routes -> {
            routes.before(ctx -> {
                String path = ctx.path();
                String newPath = path;

                // 归一
                while (newPath.startsWith("//")) {
                    newPath = newPath.substring(1);
                }

                if (newPath.length() > 1 && newPath.endsWith("/")) {
                    newPath = newPath.substring(0, newPath.length() - 1);
                }

                if (!newPath.equals(path)) {
                    String qs = ctx.queryString();
                    String redirect = qs != null ? newPath + "?" + qs : newPath;
                    logger.debug("[Yggdrasil] 路径归一化: {} {} -> {}", ctx.method(), path, redirect);
                    ctx.redirect(redirect, io.javalin.http.HttpStatus.TEMPORARY_REDIRECT);
                    return;
                }

                logger.info("[Yggdrasil] 收到请求: {} {} (Content-Type: {}, Remote: {})",
                        ctx.method(), ctx.fullUrl(),
                        ctx.contentType(), IpUtil.getClientIp(ctx));
            });

            routes.get("/", this::handleMetadata);

            routes.post("/authserver/authenticate", YggdrasilAuthHandler::authenticate);
            routes.post("/authserver/refresh", YggdrasilAuthHandler::refresh);
            routes.post("/authserver/validate", YggdrasilAuthHandler::validate);
            routes.post("/authserver/invalidate", YggdrasilAuthHandler::invalidate);
            routes.post("/authserver/signout", YggdrasilAuthHandler::signout);

            routes.post("/sessionserver/session/minecraft/join", YggdrasilSessionHandler::join);
            routes.get("/sessionserver/session/minecraft/hasJoined", YggdrasilSessionHandler::hasJoined);
            routes.get("/sessionserver/session/minecraft/profile/{uuid}", YggdrasilSessionHandler::getProfile);

            routes.get("/api/profiles/lookup/minecraft/name/{username}", ctx -> {
                String username = ctx.pathParam("username");
                logger.info("[Yggdrasil] 查询角色名: {}", username);
                ProfileDao profileDao = new ProfileDao(db);
                PlayerProfile profile = profileDao.findByName(username);
                if (profile != null) {
                    ctx.contentType("application/json");
                    ctx.json(Map.of(
                        "id", UuidUtil.toHex(profile.getId()),
                        "name", profile.getName()
                    ));
                } else {
                    ctx.status(204);
                }
            });

            routes.post("/api/profiles/lookup/minecraft", ctx -> {
                logger.info("[Yggdrasil] 批量查询角色");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                List<String> names = mapper.readValue(ctx.body(), List.class);
                ProfileDao profileDao = new ProfileDao(db);
                List<PlayerProfile> profiles = profileDao.findByNames(names);
                List<Map<String, String>> result = new ArrayList<>();
                for (PlayerProfile p : profiles) {
                    result.add(Map.of(
                        "id", UuidUtil.toHex(p.getId()),
                        "name", p.getName()
                    ));
                }
                ctx.contentType("application/json");
                ctx.json(result);
            });

            routes.get("/api/user/profile/profiles/minecraft/{uuid}", ctx -> {
                String uuidRaw = ctx.pathParam("uuid");
                logger.info("[Yggdrasil] 查询UUID对应角色: {}", uuidRaw);
                String uuidHex = UuidUtil.toHex(uuidRaw);
                ProfileDao profileDao = new ProfileDao(db);
                PlayerProfile profile = profileDao.findById(uuidHex);
                if (profile == null) {
                    profile = profileDao.findById(UuidUtil.fromHex(uuidHex));
                }
                if (profile != null) {
                    ctx.contentType("application/json");
                    ctx.json(YggdrasilUtil.buildProfileResponse(profile));
                } else {
                    ctx.status(204);
                }
            });

            routes.get("/textures/{type}/{hash}", ctx -> {
                String type = ctx.pathParam("type").toUpperCase();
                String hash = ctx.pathParam("hash");
                SystemConfig sysConfig = SystemConfig.getInstance();
                if ("SKIN".equals(type) && !sysConfig.isAllowDownloadSkin()) {
                    ctx.status(403).json(Map.of("error", "Skin download is disabled"));
                    return;
                }
                if ("CAPE".equals(type) && !sysConfig.isAllowDownloadCape()) {
                    ctx.status(403).json(Map.of("error", "Cape download is disabled"));
                    return;
                }
                TextureDao textureDao = new TextureDao(db);
                CacheDao cacheDao = authService.getCacheDao();
                TextureService textureService = new TextureService(sysConfig, cacheDao);
                byte[] data = textureService.readFile(type, hash);
                if (data == null) {
                    ctx.status(404).json(Map.of("error", "Texture not found"));
                    return;
                }
                ctx.contentType("image/png");
                ctx.result(data);
            });

            routes.error(404, ctx -> {
                logger.warn("[Yggdrasil] 404 Route not found: {} {} (from {})",
                        ctx.method(), ctx.req().getRequestURI(), IpUtil.getClientIp(ctx));
                ctx.contentType("application/json");
                ctx.json(YggdrasilUtil.buildErrorResponse(
                    "NotFoundException", "Route not found", ""));
            });
        });
    }

    public void start() {
        app.start(35577);
    }

    public void stop() {
        app.stop();
    }

    public AuthService getAuthService() {
        return authService;
    }

    private void handleMetadata(Context ctx) {
        SystemConfig sysConfig = SystemConfig.getInstance();

        String homepage = sysConfig.getUserDomain();
        if (homepage.isEmpty()) {
            homepage = sysConfig.getCommonDomain();
        }
        if (homepage.isEmpty()) {
            homepage = "http://localhost:35565";
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("serverName", sysConfig.getSiteName());
        meta.put("implementationName", "LingYggdrasil");
        meta.put("implementationVersion", AppConfig.APP_VERSION);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("homepage", homepage);
        meta.put("links", links);

        List<String> skinDomains = new ArrayList<>();
        String apiDomain = sysConfig.getApiDomain();
        String commonDomain = sysConfig.getCommonDomain();
        if (!apiDomain.isEmpty()) {
            addDomainWithPort(skinDomains, apiDomain);
        } else if (!commonDomain.isEmpty()) {
            addDomainWithPort(skinDomains, commonDomain);
        } else {
            skinDomains.add("localhost");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("meta", meta);
        response.put("skinDomains", skinDomains);

        YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
        String pubKeyBase64 = km.getPublicKeyBase64();
        if (pubKeyBase64 != null && !pubKeyBase64.isEmpty()) {
            response.put("signaturePublickeys", List.of(pubKeyBase64));
        } else {
            response.put("signaturePublickeys", List.of());
        }

        ctx.contentType("application/json");
        ctx.json(response);
    }

    private void addDomainWithPort(List<String> domains, String domainUrl) {
        if (domainUrl == null || domainUrl.isEmpty()) return;
        try {
            java.net.URI uri = new java.net.URI(domainUrl);
            String host = uri.getHost();
            if (host != null) {
                int port = uri.getPort();
                String entry = (port > 0 && port != 80 && port != 443) ? host + ":" + port : host;
                if (!domains.contains(entry)) {
                    domains.add(entry);
                }
            }
        } catch (Exception ignored) {}
    }
}
