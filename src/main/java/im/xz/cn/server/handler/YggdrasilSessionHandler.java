package im.xz.cn.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.AuthService;
import im.xz.cn.model.AuthToken;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.UuidUtil;
import im.xz.cn.util.YggdrasilUtil;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class YggdrasilSessionHandler {
    private static final Logger logger = LoggerFactory.getLogger(YggdrasilSessionHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static AuthService authService;

    public static void init(AuthService service) {
        authService = service;
    }

    // POST /sessionserver/session/minecraft/join
    @SuppressWarnings("unchecked")
    public static void join(Context ctx) {
        try {
            logger.info("[Yggdrasil Session] join 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            logger.debug("[Yggdrasil Session] Join Body: {}", ctx.body());
            Map<String, Object> body;
            try {
                body = mapper.readValue(ctx.body(), Map.class);
            } catch (Exception e) {
                sendError(ctx, 400, "IllegalArgumentException",
                        "Invalid request body");
                return;
            }

            String accessToken = (String) body.get("accessToken");
            String selectedProfileRaw = (String) body.get("selectedProfile");
            String serverId = (String) body.get("serverId");

            if (accessToken == null || selectedProfileRaw == null || serverId == null) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid request");
                return;
            }

            String profileId = UuidUtil.toHex(selectedProfileRaw);

            AuthToken token = authService.getTokenDao().findByAccessToken(accessToken);
            if (token == null || token.isExpired()) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Invalid token");
                return;
            }

            PlayerProfile profile = authService.getProfileDao().findById(profileId);
            if (profile == null) {
                profile = authService.getProfileDao().findById(UuidUtil.fromHex(profileId));
            }
            if (profile == null || !profile.getUserId().equals(token.getUserId())) {
                sendError(ctx, 403, "ForbiddenOperationException",
                        "Profile does not belong to token owner");
                return;
            }

            authService.getTokenDao().updateServerId(accessToken, serverId);
            authService.getTokenDao().updateProfileId(accessToken, profileId);

            ctx.status(204);
            logger.info("[Yggdrasil Session] join 成功, profile: {}, serverId: {}",
                    selectedProfileRaw, serverId);

        } catch (Exception e) {
            logger.error("join error: {}", e.getMessage(), e);
            sendError(ctx, 500, "InternalServerException",
                    "Internal server error");
        }
    }

    // GET /sessionserver/session/minecraft/hasJoined
    public static void hasJoined(Context ctx) {
        try {
            logger.info("[Yggdrasil Session] hasJoined 请求开始, Remote: {}", IpUtil.getClientIp(ctx));
            String username = ctx.queryParam("username");
            String serverId = ctx.queryParam("serverId");

            if (username == null || serverId == null) {
                ctx.status(204);
                return;
            }

            PlayerProfile profile = authService.hasJoinedServer(username, serverId);
            logger.info("[Yggdrasil Session] hasJoined 查询: username={}, serverId={}, 结果: {}",
                    username, serverId, profile != null ? "成功(" + profile.getName() + ")" : "失败");
            if (profile != null) {
                ctx.contentType("application/json");
                ctx.json(YggdrasilUtil.buildProfileResponse(profile));
            } else {
                ctx.status(204);
            }

        } catch (Exception e) {
            logger.error("hasJoined error: {}", e.getMessage(), e);
            ctx.status(204);
        }
    }

    // GET /sessionserver/session/minecraft/profile/{uuid}
    public static void getProfile(Context ctx) {
        try {
            String uuidRaw = ctx.pathParam("uuid");
            logger.info("[Yggdrasil Session] getProfile 请求: uuid={}, Remote: {}",
                    uuidRaw, IpUtil.getClientIp(ctx));
            String uuidHex = UuidUtil.toHex(uuidRaw);

            PlayerProfile profile = authService.getProfileDao().findById(uuidHex);
            if (profile == null) {
                profile = authService.getProfileDao().findById(UuidUtil.fromHex(uuidHex));
            }

            if (profile != null) {
                ctx.contentType("application/json");
                ctx.json(YggdrasilUtil.buildProfileResponse(profile));
            } else {
                ctx.status(204);
            }

        } catch (Exception e) {
            logger.error("getProfile error: {}", e.getMessage(), e);
            ctx.status(204);
        }
    }

    private static void sendError(Context ctx, int status, String error,
                                   String errorMessage) {
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.json(YggdrasilUtil.buildErrorResponse(error, errorMessage, ""));
    }
}
