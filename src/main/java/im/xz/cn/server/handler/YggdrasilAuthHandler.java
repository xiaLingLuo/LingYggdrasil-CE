package im.xz.cn.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.AuthService;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.model.AuthToken;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.UuidUtil;
import im.xz.cn.util.YggdrasilUtil;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class YggdrasilAuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(YggdrasilAuthHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static AuthService authService;
    private static SystemConfig sysConfig;

    public static void init(AuthService service, SystemConfig config) {
        authService = service;
        sysConfig = config;
    }

    @SuppressWarnings("unchecked")
    public static void authenticate(Context ctx) {
        try {
            logger.info("[Yggdrasil Auth] authenticate 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            logger.debug("[Yggdrasil Auth] Request Body: {}", ctx.body());
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
            String clientToken = (String) body.get("clientToken");
            Boolean requestUser = (Boolean) body.get("requestUser");

            if (profileName == null || token == null) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "Missing username or password");
                return;
            }

            logger.info("[Yggdrasil Auth] authenticate 角色名: {}", profileName);

            profileName = profileName.trim();
            token = token.trim();

            PlayerProfile selectedProfile = authService.getProfileDao().findByNameAndToken(profileName, token);
            if (selectedProfile == null) {
                logger.warn("[Yggdrasil Auth] 认证失败: 角色名或Token无效, 角色名='{}'", profileName);
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            User user = authService.getUserDao().findById(selectedProfile.getUserId());
            if (user == null) {
                logger.warn("[Yggdrasil Auth] 认证失败: 角色所属用户不存在, userId={}", selectedProfile.getUserId());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            if (sysConfig != null && sysConfig.isEmailVerificationEnabled() && !user.isEmailVerified()) {
                logger.warn("[Yggdrasil Auth] 认证失败: 用户邮箱未验证, user={}", user.getUsername());
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Email not verified");
                return;
            }

            logger.info("[Yggdrasil Auth] 认证成功: 角色={}, 用户={}", profileName, user.getUsername());

            List<PlayerProfile> profiles = authService.getProfileDao().findByUserId(user.getId());
            if (profiles.isEmpty()) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "No profile available");
                return;
            }

            if (clientToken == null || clientToken.isEmpty()) {
                clientToken = UUID.randomUUID().toString().replace("-", "");
            }

            String profileId = UuidUtil.toHex(selectedProfile.getId());
            AuthToken authToken = authService.createToken(
                    user.getId(), clientToken, profileId);

            Map<String, Object> response = buildAuthResponse(
                    authToken, profiles, selectedProfile, user,
                    requestUser != null && requestUser);

            ctx.contentType("application/json");
            ctx.json(response);
            logger.info("[Yggdrasil Auth] authenticate 成功, 角色: {}", selectedProfile.getName());

        } catch (Exception e) {
            logger.error("authenticate error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    @SuppressWarnings("unchecked")
    public static void refresh(Context ctx) {
        try {
            logger.info("[Yggdrasil Auth] refresh 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            logger.debug("[Yggdrasil Auth] Refresh Body: {}", ctx.body());
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
            Boolean requestUser = (Boolean) body.get("requestUser");

            @SuppressWarnings("rawtypes")
            Map selectedProfileMap = (Map) body.get("selectedProfile");

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

    // POST /authserver/validate
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

    // POST /authserver/invalidate
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

            PlayerProfile profile = authService.getProfileDao().findByNameAndToken(profileName.trim(), token.trim());
            if (profile == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid credentials. Invalid username or password.");
                return;
            }

            authService.invalidateAllUserTokens(profile.getUserId());

            ctx.status(204);

        } catch (Exception e) {
            logger.error("signout error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    private static boolean checkContentType(Context ctx) {
        String contentType = ctx.contentType();
        if (contentType == null || !contentType.contains("application/json")) {
            ctx.status(400);
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
