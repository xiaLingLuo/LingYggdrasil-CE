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
package im.xz.cn.server.handler.yggdrasil;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.AuthService;
import im.xz.cn.model.AuthToken;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.yggdrasil.YggdrasilUtil;
import io.javalin.http.Context;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import im.xz.cn.logging.logApi;

public class YggdrasilSessionHandler {
    private static final logApi logger = logApi.getLogger(YggdrasilSessionHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static AuthService authService;
    private static im.xz.cn.profile.ProfileTextureResolver textureResolver;

    public static void init(AuthService service) {
        authService = service;
        textureResolver = new im.xz.cn.profile.ProfileTextureResolver(service.getDatabaseManager());
    }

    @SuppressWarnings("unchecked")
    public static void join(Context ctx) {
        try {
            if (!YggdrasilAuthHandler.checkContentType(ctx)) return;
            logger.info("[Yggdrasil Session] === join 请求 === Remote: {}", IpUtil.getClientIp(ctx));
            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                logger.warn("[Yggdrasil Session] join JSON解析失败: {}", e.getMessage());
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String accessToken = body.get("accessToken") instanceof String value ? value : null;
            Object selectedProfileValue = body.get("selectedProfile");
            if (selectedProfileValue != null && !(selectedProfileValue instanceof String)) {
                sendError(ctx, 400, "IllegalArgumentException", "selectedProfile must be a string");
                return;
            }
            String selectedProfileRaw = (String) selectedProfileValue;
            String serverId = body.get("serverId") instanceof String serverValue ? serverValue : null;

            logger.info("[Yggdrasil Session] join 参数: selectedProfile={}, serverId={}",
                    selectedProfileRaw, serverId);

            if (accessToken == null || serverId == null || serverId.isBlank()) {
                logger.warn("[Yggdrasil Session] join 参数缺失: accessToken={}, selectedProfile={}, serverId={}",
                        accessToken != null, selectedProfileRaw != null, serverId != null);
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid request");
                return;
            }

            if (selectedProfileRaw == null) {
                sendError(ctx, 403, "ForbiddenOperationException", "Invalid request");
                return;
            }
            if (selectedProfileRaw.isBlank()) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "selectedProfile must be a valid UUID");
                return;
            }

            if (!selectedProfileRaw.replace("-", "").matches("[0-9a-fA-F]{32}")) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "selectedProfile must be a valid UUID");
                return;
            }

            String profileId = UuidUtil.toHex(selectedProfileRaw);
            logger.info("[Yggdrasil Session] join profileId转换: raw={} -> hex={}", selectedProfileRaw, profileId);

            AuthToken token = authService.getTokenDao().findByAccessToken(accessToken);
            if (token == null || token.isExpired() || !authService.isAuthSourceAllowed(token)) {
                logger.warn("[Yggdrasil Session] join Token无效: token={}, expired={}",
                        token != null, token != null ? token.isExpired() : "N/A");
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }
            logger.info("[Yggdrasil Session] join Token有效: userId={}, tokenProfileId={}",
                    token.getUserId(), token.getProfileId());

            PlayerProfile profile = authService.getProfileDao().findById(profileId);
            if (profile == null) {
                String hexWithDash = UuidUtil.fromHex(profileId);
                logger.info("[Yggdrasil Session] join 首次查找失败(hex={}), 尝试dash格式: {}", profileId, hexWithDash);
                profile = authService.getProfileDao().findById(hexWithDash);
            }
            if (profile == null || !profile.getUserId().equals(token.getUserId())) {
                logger.warn("[Yggdrasil Session] join Profile验证失败: profile={}, profileUserId={}, tokenUserId={}",
                        profile != null, profile != null ? profile.getUserId() : "N/A", token.getUserId());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Profile does not belong to token owner");
                return;
            }
            logger.info("[Yggdrasil Session] join Profile匹配: name={}, userId={}", profile.getName(), profile.getUserId());

            im.xz.cn.model.User joinUser = authService.getUserDao().findById(token.getUserId());
            if (joinUser == null || !authService.isUserAccessible(joinUser)) {
                logger.warn("[Yggdrasil Session] join 失败: 用户已被禁止访问, userId={}", token.getUserId());
                sendError(ctx, 403, "ForbiddenOperationException", "Invalid token");
                return;
            }

            String boundProfileId = token.getProfileId();
            if (boundProfileId == null || boundProfileId.isBlank()) {
                logger.warn("[Yggdrasil Session] join 失败: 会话尚未选择角色, userId={}", token.getUserId());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Profile not selected; select it via refresh first");
                return;
            }
            if (!UuidUtil.toHex(boundProfileId).equals(profileId)) {
                logger.warn("[Yggdrasil Session] join 失败: 会话已绑定其他角色, bound={}, requested={}",
                        boundProfileId, profileId);
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Token is bound to another profile");
                return;
            }

            
            authService.getTokenDao().updateJoinTicket(accessToken, serverId, profileId,
                    im.xz.cn.common.TimeUtil.plusSeconds(30), IpUtil.getClientIp(ctx));

            ctx.status(204);
            logger.info("[Yggdrasil Session] join 成功 -> profile={}, serverId={}", profile.getName(), serverId);

        } catch (Exception e) {
            logger.error("[Yggdrasil Session] join 异常: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    public static void hasJoined(Context ctx) {
        try {
            String username = ctx.queryParam("username");
            String serverId = ctx.queryParam("serverId");
            String ip = ctx.queryParam("ip");
            logger.info("[Yggdrasil Session] === hasJoined 请求 === username={}, serverId={}, ip={}, Remote={}",
                    username, serverId, ip, IpUtil.getClientIp(ctx));

            if (username == null || serverId == null) {
                logger.warn("[Yggdrasil Session] hasJoined 参数缺失");
                ctx.status(204);
                return;
            }

            PlayerProfile profile = authService.hasJoinedServer(username, serverId, ip);
            if (profile != null) {
                im.xz.cn.model.User owner = authService.getUserDao().findById(profile.getUserId());
                if (owner == null || !authService.isUserAccessible(owner)) {
                    logger.warn("[Yggdrasil Session] hasJoined 失败: 用户已被禁止访问, username={}", username);
                    ctx.status(204);
                    return;
                }
                logProfileTextures("[Yggdrasil Session] hasJoined", profile);
                Map<String, Object> response = YggdrasilUtil.buildProfileResponse(profile, false, textureResolver);
                logger.info("[Yggdrasil Session] hasJoined 成功 -> name={}, id={}, hasProperties={}",
                        profile.getName(), UuidUtil.toHex(profile.getId()),
                        response.get("properties") != null);
                logResponseTextures("[Yggdrasil Session] hasJoined", response);
                ctx.contentType("application/json");
                ctx.json(response);
            } else {
                logger.warn("[Yggdrasil Session] hasJoined 失败: username={}, serverId={}", username, serverId);
                ctx.status(204);
            }

        } catch (Exception e) {
            logger.error("[Yggdrasil Session] hasJoined 异常: {}", e.getMessage(), e);
            ctx.status(204);
        }
    }

    public static void getProfile(Context ctx) {
        try {
            String uuidRaw = ctx.pathParam("uuid");
            String unsignedParam = ctx.queryParam("unsigned");
            boolean unsigned = "true".equalsIgnoreCase(unsignedParam);
            logger.info("[Yggdrasil Session] === getProfile 请求 === uuidRaw={}, unsignedParam={}, unsigned={}, Remote={}",
                    uuidRaw, unsignedParam, unsigned, IpUtil.getClientIp(ctx));

            String uuidHex = UuidUtil.toHex(uuidRaw);
            logger.info("[Yggdrasil Session] getProfile UUID转换: raw={} -> hex={}", uuidRaw, uuidHex);

            PlayerProfile profile = authService.getProfileDao().findById(uuidHex);
            if (profile == null) {
                String hexWithDash = UuidUtil.fromHex(uuidHex);
                logger.info("[Yggdrasil Session] getProfile hex查找失败, 尝试dash格式: {}", hexWithDash);
                profile = authService.getProfileDao().findById(hexWithDash);
            }

            if (profile != null) {
                logProfileTextures("[Yggdrasil Session] getProfile", profile);
                Map<String, Object> response = YggdrasilUtil.buildProfileResponse(profile, unsigned, textureResolver);
                logger.info("[Yggdrasil Session] getProfile 成功 -> name={}, id={}, unsigned={}, hasProperties={}",
                        profile.getName(), UuidUtil.toHex(profile.getId()), unsigned,
                        response.get("properties") != null);
                logResponseTextures("[Yggdrasil Session] getProfile", response);
                ctx.contentType("application/json");
                ctx.json(response);
            } else {
                logger.warn("[Yggdrasil Session] getProfile Profile未找到: uuidRaw={}, uuidHex={}",
                        uuidRaw, uuidHex);
                ctx.status(204);
            }

        } catch (Exception e) {
            logger.error("[Yggdrasil Session] getProfile 异常: {}", e.getMessage(), e);
            ctx.status(204);
        }
    }

    private static void logProfileTextures(String tag, PlayerProfile profile) {
        logger.info("{} Profile详情: id={}, name={}, skinHash={}, capeHash={}, skinModel={}",
                tag,
                UuidUtil.toHex(profile.getId()),
                profile.getName(),
                profile.getSkinHash() != null ? profile.getSkinHash() : "(null)",
                profile.getCapeHash() != null ? profile.getCapeHash() : "(null)",
                profile.getSkinModel());
    }

    @SuppressWarnings("unchecked")
    private static void logResponseTextures(String tag, Map<String, Object> response) {
        try {
            java.util.List<Map<String, String>> properties = (java.util.List<Map<String, String>>) response.get("properties");
            if (properties != null) {
                for (Map<String, String> prop : properties) {
                    if ("textures".equals(prop.get("name"))) {
                        String value = prop.get("value");
                        String signature = prop.get("signature");
                        if (value != null && !value.isEmpty()) {
                            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                            logger.info("{} 响应Textures解码: {}", tag, decoded);
                        } else {
                            logger.warn("{} 响应Textures为空!", tag);
                        }
                        logger.info("{} 响应Signature: {}", tag,
                                signature != null && !signature.isEmpty() ? "存在(长度=" + signature.length() + ")" : "缺失");
                    }
                }
            } else {
                logger.warn("{} 响应没有properties字段!", tag);
            }
        } catch (Exception e) {
            logger.error("{} 解析响应Textures失败: {}", tag, e.getMessage());
        }
    }

    private static String maskToken(String token) {
        if (token == null) return "(null)";
        if (token.length() <= 8) return token.substring(0, Math.min(4, token.length())) + "...";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private static void sendError(Context ctx, int status, String error,
                                   String errorMessage) {
        logger.warn("[Yggdrasil Session] 返回错误: status={}, error={}, message={}", status, error, errorMessage);
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.json(YggdrasilUtil.buildErrorResponse(error, errorMessage, ""));
    }
}
