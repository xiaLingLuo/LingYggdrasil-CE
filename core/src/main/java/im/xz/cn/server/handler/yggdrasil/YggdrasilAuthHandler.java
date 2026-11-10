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
import im.xz.cn.config.SystemConfig;
import im.xz.cn.model.AuthToken;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.yggdrasil.YggdrasilUtil;
import im.xz.cn.yggdrasil.YggdrasilLoginingDelAts;
import im.xz.cn.identity.AccountPasswordLoginService;
import im.xz.cn.identity.UserPreferenceRepository;
import io.javalin.http.Context;

import im.xz.cn.logging.logApi;

import java.util.*;

public class YggdrasilAuthHandler {
    private static final logApi logger = logApi.getLogger(YggdrasilAuthHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static AuthService authService;
    private static SystemConfig sysConfig;
    private static im.xz.cn.auth.LoginRateLimiter rateLimiter;

    public static void init(AuthService service, SystemConfig config) {
        authService = service;
        sysConfig = config;
        rateLimiter = new im.xz.cn.auth.LoginRateLimiter(service.getCacheDao());
    }

    @SuppressWarnings("unchecked")
    public static void authenticate(Context ctx) {
        try {
            logger.info("[Yggdrasil Auth] === authenticate 请求 === Remote: {}", IpUtil.getClientIp(ctx));
            if (!checkContentType(ctx)) return;

            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                logger.warn("[Yggdrasil Auth] authenticate JSON解析失败: {}", e.getMessage());
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String profileName = body.get("username") instanceof String usernameText ? usernameText : null;
            String token = body.get("password") instanceof String passwordText ? passwordText : null;
            String clientToken = body.get("clientToken") instanceof String clientText ? clientText : null;
            Boolean requestUser = body.get("requestUser") instanceof Boolean requestValue ? requestValue : null;

            logger.info("[Yggdrasil Auth] authenticate 参数: profileName={}, requestUser={}",
                    profileName, requestUser);

            if (profileName == null || token == null) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "username/password must be strings");
                return;
            }

            
            String originalName = profileName == null ? null : profileName.trim();
            String roleName = YggdrasilLoginingDelAts.normalize(originalName);
            token = token.trim();

            if (originalName == null || originalName.isEmpty()) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }
            profileName = roleName == null || roleName.isEmpty() ? originalName : roleName;

            String clientIp = IpUtil.getClientIp(ctx);
            String rateLimitMessage = rateLimiter.checkRateLimit("ygg:" + profileName, clientIp);
            if (rateLimitMessage != null) {
                logger.warn("[Yggdrasil Auth] authenticate 被限流: name={}", profileName);
                sendError(ctx, 429, "ForbiddenOperationException", rateLimitMessage);
                return;
            }

            PlayerProfile selectedProfile = authService.getProfileDao().findByNameAndWorldTreeKey(profileName, token);
            if (selectedProfile == null) {
                
                if (authenticateWithAccountPassword(ctx, originalName, token, clientToken, requestUser)) {
                    return;
                }
                rateLimiter.recordFailure("ygg:" + profileName, clientIp);
                logger.warn("[Yggdrasil Auth] authenticate 失败: 凭证无效, name={}", profileName);
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }
            logger.info("[Yggdrasil Auth] authenticate Profile找到: name={}, id={}", selectedProfile.getName(), UuidUtil.toHex(selectedProfile.getId()));

            User user = authService.getUserDao().findById(selectedProfile.getUserId());
            if (user == null || "-99".equals(user.getId())) {
                rateLimiter.recordFailure("ygg:" + profileName, clientIp);
                logger.warn("[Yggdrasil Auth] authenticate 失败: 用户不存在或角色未分配, userId={}", selectedProfile.getUserId());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            if (!authService.isUserAccessible(user)) {
                rateLimiter.recordFailure("ygg:" + profileName, clientIp);
                logger.warn("[Yggdrasil Auth] authenticate 失败: 用户已被禁止访问, user={}", user.getUsername());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            if (sysConfig != null && sysConfig.isEmailVerificationEnabled() && !user.isEmailVerified()) {
                rateLimiter.recordFailure("ygg:" + profileName, clientIp);
                logger.warn("[Yggdrasil Auth] authenticate 失败: 邮箱未验证, user={}", user.getUsername());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Email not verified");
                return;
            }

            List<PlayerProfile> profiles = authService.getProfileDao().findByUserId(user.getId());
            if (profiles.isEmpty()) {
                rateLimiter.recordFailure("ygg:" + profileName, clientIp);
                logger.warn("[Yggdrasil Auth] authenticate 失败: 无可用角色");
                sendError(ctx, 404, "PlayerNotFoundException",
                        "No profile available");
                return;
            }
            logger.info("[Yggdrasil Auth] authenticate 可用角色数: {}", profiles.size());

            if (clientToken == null || clientToken.isEmpty()) {
                clientToken = UUID.randomUUID().toString().replace("-", "");
            }

            String profileId = UuidUtil.toHex(selectedProfile.getId());
            AuthToken authToken = authService.createToken(user.getId(), clientToken, profileId, "WORLD_TREE");
            
            logger.info("[Yggdrasil Auth] authenticate 签发访问会话: profileId={}", profileId);

            Map<String, Object> response = buildAuthResponse(
                    authToken, profiles, selectedProfile, user,
                    requestUser != null && requestUser);

            authService.getUserDao().updateLastLogin(user.getId(), TimeUtil.now());
            rateLimiter.recordSuccess("ygg:" + profileName, clientIp);
            ctx.contentType("application/json");
            ctx.json(response);
            logger.info("[Yggdrasil Auth] authenticate 成功: 角色={}, availableProfiles={}",
                    selectedProfile.getName(), profiles.size());

        } catch (Exception e) {
            logger.error("[Yggdrasil Auth] authenticate 异常: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    @SuppressWarnings("unchecked")
    public static void refresh(Context ctx) {
        try {
            logger.info("[Yggdrasil Auth] refresh 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            if (!checkContentType(ctx)) return;

            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String accessToken = body.get("accessToken") instanceof String tokenValue ? tokenValue : null;
            String clientToken = body.get("clientToken") instanceof String clientValue ? clientValue : null;
            Boolean requestUser = body.get("requestUser") instanceof Boolean requestValue ? requestValue : null;

            Object selectedProfileValue = body.get("selectedProfile");
            Map<?, ?> selectedProfileMap = null;
            if (selectedProfileValue != null) {
                if (!(selectedProfileValue instanceof Map<?, ?> map)) {
                    sendError(ctx, 400, "IllegalArgumentException",
                            "selectedProfile must be an object");
                    return;
                }
                selectedProfileMap = map;
                Object idValue = map.get("id");
                if (!(idValue instanceof String idText)
                        || !idText.replace("-", "").matches("[0-9a-fA-F]{32}")) {
                    sendError(ctx, 400, "IllegalArgumentException",
                            "selectedProfile.id must be a valid UUID");
                    return;
                }
            }

            if (accessToken == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }

            AuthToken oldToken = authService.getTokenDao().findByAccessToken(accessToken);
            if (oldToken == null || oldToken.isExpired()) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }

            if (clientToken != null && !clientToken.isEmpty()) {
                if (!clientToken.equals(oldToken.getClientToken())) {
                    sendError(ctx, 403, "ForbiddenOperationException",
                            "Invalid token clientToken mismatch");
                    return;
                }
            }

            String userId = oldToken.getUserId();

            String profileId = null;
            PlayerProfile selectedProfile = null;
            List<PlayerProfile> profiles = authService.getProfileDao().findByUserId(userId);

            if (selectedProfileMap != null) {
                String requestedId = String.valueOf(selectedProfileMap.get("id"));
                String hexId = UuidUtil.toHex(requestedId);
                for (PlayerProfile p : profiles) {
                    if (UuidUtil.toHex(p.getId()).equals(hexId)) {
                        selectedProfile = p;
                        profileId = hexId;
                        break;
                    }
                }
                if (selectedProfile == null) {
                    sendError(ctx, 403, "ForbiddenOperationException",
                            "Selected profile does not belong to user");
                    return;
                }
            } else {
                profileId = oldToken.getProfileId();
                if (profileId != null) {
                    for (PlayerProfile p : profiles) {
                        if (UuidUtil.toHex(p.getId()).equals(profileId)) {
                            selectedProfile = p;
                            break;
                        }
                    }
                }
            }

            AuthToken newToken = authService.refreshToken(accessToken, clientToken, profileId);
            if (newToken == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Failed to refresh token");
                return;
            }

            User user = authService.getUserDao().findById(userId);
            if (user == null || !authService.isUserAccessible(user)) {
                logger.warn("[Yggdrasil Auth] refresh 失败: 用户已被禁止访问或不存在, userId={}", userId);
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }
            Map<String, Object> response = buildAuthResponse(
                    newToken, profiles, selectedProfile, user,
                    requestUser != null && requestUser);

            ctx.contentType("application/json");
            ctx.json(response);

        } catch (Exception e) {
            logger.error("refresh error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    @SuppressWarnings("unchecked")
    public static void validate(Context ctx) {
        try {
            logger.info("[Yggdrasil Auth] validate 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            if (!checkContentType(ctx)) return;

            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String accessToken = (String) body.get("accessToken");
            String clientToken = (String) body.get("clientToken");

            if (accessToken == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }

            boolean valid = authService.validateToken(accessToken, clientToken);
            logger.info("[Yggdrasil Auth] validate 结果: {}", valid);
            if (valid) {
                ctx.status(204);
            } else {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
            }

        } catch (Exception e) {
            logger.error("validate error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    @SuppressWarnings("unchecked")
    public static void invalidate(Context ctx) {
        try {
            if (!checkContentType(ctx)) return;

            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                ctx.status(204);
                return;
            }

            String accessToken = (String) body.get("accessToken");
            if (accessToken != null) {
                authService.invalidateToken(accessToken);
            }

            ctx.status(204);

        } catch (Exception e) {
            logger.error("invalidate error: {}", e.getMessage(), e);
            ctx.status(204);
        }
    }

    @SuppressWarnings("unchecked")
    public static void signout(Context ctx) {
        try {
            if (!checkContentType(ctx)) return;

            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String profileName = (String) body.get("username");
            String token = (String) body.get("password");

            if (profileName == null || token == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            String identifier = profileName.trim();
            token = token.trim();
            if (identifier.isEmpty()) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            PlayerProfile profile = authService.getProfileDao()
                    .findByNameAndWorldTreeKey(YggdrasilLoginingDelAts.normalize(identifier), token);
            String userId = profile == null ? null : profile.getUserId();
            if (userId == null) {
                AccountPasswordLoginService service = new AccountPasswordLoginService(
                        authService,
                        new UserPreferenceRepository(authService.getDatabaseManager()),
                        sysConfig != null && sysConfig.isEmailVerificationEnabled());
                AccountPasswordLoginService.Result result =
                        service.login(identifier, token, IpUtil.getClientIp(ctx));
                if (result.status() == AccountPasswordLoginService.Status.OK) {
                    userId = result.user().getId();
                }
            }
            if (userId == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            authService.invalidateAllUserTokens(userId);

            ctx.status(204);

        } catch (Exception e) {
            logger.error("signout error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    public static boolean checkContentType(Context ctx) {
        String contentType = ctx.contentType();
        if (contentType == null || !contentType.contains("application/json")) {
            ctx.status(415);
            ctx.contentType("application/json");
            ctx.json(YggdrasilUtil.buildErrorResponse(
                    "UnsupportedMediaTypeException",
                    "Content-Type must be application/json",
                    ""));
            return false;
        }
        return true;
    }

    private static void sendError(Context ctx, int status, String error,
                                   String errorMessage) {
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.json(YggdrasilUtil.buildErrorResponse(error, errorMessage, ""));
    }

    
    private static boolean authenticateWithAccountPassword(Context ctx, String identifier, String password,
                                                           String clientToken, Boolean requestUser) {
        AccountPasswordLoginService service = new AccountPasswordLoginService(
                authService,
                new UserPreferenceRepository(authService.getDatabaseManager()),
                sysConfig != null && sysConfig.isEmailVerificationEnabled());
        AccountPasswordLoginService.Result result =
                service.login(identifier, password, IpUtil.getClientIp(ctx));
        switch (result.status()) {
            case INVALID_CREDENTIALS -> {
                return false;
            }
            case DISABLED -> {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Account password login is disabled for this account.");
                return true;
            }
            case ACCOUNT_INACCESSIBLE -> {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return true;
            }
            case EMAIL_UNVERIFIED -> {
                sendError(ctx, 403, "ForbiddenOperationException", "Email not verified");
                return true;
            }
            case NO_PROFILES -> {
                sendError(ctx, 404, "PlayerNotFoundException", "404Player not exist");
                return true;
            }
            case OK -> {
                
                rateLimiter.recordSuccess("ygg:" + identifier, IpUtil.getClientIp(ctx));
            }
        }

        String effectiveClientToken = (clientToken == null || clientToken.isEmpty())
                ? UUID.randomUUID().toString().replace("-", "") : clientToken;
        List<PlayerProfile> profiles = result.profiles();
        PlayerProfile selected = profiles.size() == 1 ? profiles.get(0) : null;
        String boundProfileId = selected == null ? null : UuidUtil.toHex(selected.getId());
        AuthToken token = authService.createToken(result.user().getId(), effectiveClientToken, boundProfileId, "ACCOUNT_PASSWORD");
        Map<String, Object> response = buildAuthResponse(token, profiles, selected, result.user(),
                requestUser != null && requestUser);
        authService.getUserDao().updateLastLogin(result.user().getId(), TimeUtil.now());
        ctx.contentType("application/json");
        ctx.json(response);
        logger.info("[Yggdrasil Auth] account-password 登录成功: profiles={}, bound={}",
                profiles.size(), selected != null);
        return true;
    }

    private static Map<String, Object> buildAuthResponse(
            AuthToken token,
            List<PlayerProfile> profiles,
            PlayerProfile selectedProfile,
            User user,
            boolean includeUser) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", token.getAccessToken());
        response.put("clientToken", token.getClientToken());

        List<Map<String, String>> availableProfiles = new ArrayList<>();
        for (PlayerProfile p : profiles) {
            Map<String, String> profileMap = new LinkedHashMap<>();
            profileMap.put("id", UuidUtil.toHex(p.getId()));
            profileMap.put("name", p.getName());
            availableProfiles.add(profileMap);
        }
        response.put("availableProfiles", availableProfiles);

        if (selectedProfile != null) {
            Map<String, String> selected = new LinkedHashMap<>();
            selected.put("id", UuidUtil.toHex(selectedProfile.getId()));
            selected.put("name", selectedProfile.getName());
            response.put("selectedProfile", selected);
        }

        if (includeUser && user != null) {
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("id", UuidUtil.toHex(user.getId()));
            userMap.put("properties", List.of());
            response.put("user", userMap);
        }

        return response;
    }
}
