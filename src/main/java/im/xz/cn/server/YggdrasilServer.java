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
                logger.info("[Yggdrasil] === 查询角色名 === username={}, Remote={}", username, IpUtil.getClientIp(ctx));
                ProfileDao profileDao = new ProfileDao(db);
                PlayerProfile profile = profileDao.findByName(username);
                if (profile != null) {
                    logger.info("[Yggdrasil] 查询角色名 成功: name={}, id={}", profile.getName(), UuidUtil.toHex(profile.getId()));
                    ctx.contentType("application/json");
                    ctx.json(Map.of(
                        "id", UuidUtil.toHex(profile.getId()),
                        "name", profile.getName()
                    ));
                } else {
                    logger.warn("[Yggdrasil] 查询角色名 未找到: {}", username);
                    ctx.status(204);
                }
            });

            routes.post("/api/profiles/lookup/minecraft", ctx -> {
                logger.info("[Yggdrasil] === 批量查询角色 === Body: {}", ctx.body());
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                List<String> names = mapper.readValue(ctx.body(), List.class);
                logger.info("[Yggdrasil] 批量查询角色 名称列表: {}", names);
                ProfileDao profileDao = new ProfileDao(db);
                List<PlayerProfile> profiles = profileDao.findByNames(names);
                List<Map<String, String>> result = new ArrayList<>();
                for (PlayerProfile p : profiles) {
                    result.add(Map.of(
                        "id", UuidUtil.toHex(p.getId()),
                        "name", p.getName()
                    ));
                }
                logger.info("[Yggdrasil] 批量查询角色 结果: 请求{}个, 找到{}个", names.size(), result.size());
                ctx.contentType("application/json");
                ctx.json(result);
            });

            routes.get("/api/user/profile/profiles/minecraft/{uuid}", ctx -> {
                String uuidRaw = ctx.pathParam("uuid");
                logger.info("[Yggdrasil] === 查询UUID角色 === uuidRaw={}, Remote={}", uuidRaw, IpUtil.getClientIp(ctx));
                String uuidHex = UuidUtil.toHex(uuidRaw);
                String unsignedParam = ctx.queryParam("unsigned");
                boolean unsigned = unsignedParam == null || "true".equals(unsignedParam);
                logger.info("[Yggdrasil] 查询UUID角色: uuidHex={}, unsigned={}", uuidHex, unsigned);
                ProfileDao profileDao = new ProfileDao(db);
                PlayerProfile profile = profileDao.findById(uuidHex);
                if (profile == null) {
                    String hexWithDash = UuidUtil.fromHex(uuidHex);
                    logger.info("[Yggdrasil] 查询UUID角色 hex查找失败, 尝试dash: {}", hexWithDash);
                    profile = profileDao.findById(hexWithDash);
                }
                if (profile != null) {
                    logger.info("[Yggdrasil] 查询UUID角色 成功: name={}, skinUrl={}",
                            profile.getName(), profile.getSkinUrl());
                    ctx.contentType("application/json");
                    ctx.json(YggdrasilUtil.buildProfileResponse(profile, unsigned));
                } else {
                    logger.warn("[Yggdrasil] 查询UUID角色 未找到: uuidRaw={}, uuidHex={}", uuidRaw, uuidHex);
                    ctx.status(204);
                }
            });

            routes.get("/textures/{type}/{hash}", ctx -> {
                String type = ctx.pathParam("type").toUpperCase();
                String hash = ctx.pathParam("hash");
                logger.info("[Yggdrasil] === 材质下载 === type={}, hash={}, Remote={}",
                        type, hash, IpUtil.getClientIp(ctx));
                SystemConfig sysConfig = SystemConfig.getInstance();
                if ("SKIN".equals(type) && !sysConfig.isAllowDownloadSkin()) {
                    logger.warn("[Yggdrasil] 材质下载 被禁止: 皮肤下载已禁用");
                    ctx.status(403).json(Map.of("error", "Skin download is disabled"));
                    return;
                }
                if ("CAPE".equals(type) && !sysConfig.isAllowDownloadCape()) {
                    logger.warn("[Yggdrasil] 材质下载 被禁止: 披风下载已禁用");
                    ctx.status(403).json(Map.of("error", "Cape download is disabled"));
                    return;
                }
                TextureDao textureDao = new TextureDao(db);
                CacheDao cacheDao = authService.getCacheDao();
                TextureService textureService = new TextureService(sysConfig, cacheDao);
                byte[] data = textureService.readFile(type, hash);
                if (data == null) {
                    String storageDir = "CAPE".equalsIgnoreCase(type) ? sysConfig.getCapeStoragePath() : sysConfig.getSkinStoragePath();
                    logger.warn("[Yggdrasil] 材质下载 文件不存在: type={}, hash={}, storageDir={}",
                            type, hash, storageDir);
                    ctx.status(404).json(Map.of("error", "Texture not found"));
                    return;
                }
                logger.info("[Yggdrasil] 材质下载 成功: type={}, hash={}, size={}bytes", type, hash, data.length);
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
        logger.info("[Yggdrasil] === Metadata === apiDomain='{}', commonDomain='{}'", apiDomain, commonDomain);
        if (!apiDomain.isEmpty()) {
            addDomainToSkinDomains(skinDomains, apiDomain);
        } else if (!commonDomain.isEmpty()) {
            addDomainToSkinDomains(skinDomains, commonDomain);
        } else {
            skinDomains.add("localhost");
        }
        logger.info("[Yggdrasil] Metadata skinDomains={}", skinDomains);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("meta", meta);
        response.put("skinDomains", skinDomains);

        YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
        String pubKeyPem = km.getPublicKeyPem();
        if (pubKeyPem != null && !pubKeyPem.isEmpty()) {
            response.put("signaturePublickey", pubKeyPem);
            response.put("signaturePublickeys", List.of(pubKeyPem));
            logger.info("[Yggdrasil] Metadata signaturePublickey: PEM格式(长度={}), 签名模式={}",
                    pubKeyPem.length(), km.getMode());
        } else {
            response.put("signaturePublickeys", List.of());
            logger.warn("[Yggdrasil] Metadata signaturePublickey: 缺失! 签名模块未加载");
        }

        logger.info("[Yggdrasil] Metadata 完整响应: skinDomains={}, hasSignature={}, apiDomain={}",
                skinDomains, pubKeyPem != null && !pubKeyPem.isEmpty(), apiDomain);
        ctx.contentType("application/json");
        ctx.json(response);
    }

    private void addDomainToSkinDomains(List<String> domains, String domainUrl) {
        if (domainUrl == null || domainUrl.isEmpty()) return;
        try {
            java.net.URI uri = new java.net.URI(domainUrl);
            String host = uri.getHost();
            if (host != null && !domains.contains(host)) {
                domains.add(host);
            }
        } catch (Exception ignored) {}
    }
}
