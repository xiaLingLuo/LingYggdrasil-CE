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
package im.xz.cn.server.handler.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.social.SocialIdentity;
import im.xz.cn.social.FriendGraphService;
import im.xz.cn.social.FriendRequestRepository;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class UserFriendGraphHandler {

    private final FriendGraphService service;
    private final im.xz.cn.social.SocialIdentityService identities;
    private final UserDao users;
    private final ProfileDao profiles;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserFriendGraphHandler(DatabaseManager database) {
        this.service = new FriendGraphService(database);
        this.identities = new im.xz.cn.social.SocialIdentityService(database,
                sha -> im.xz.cn.texture.UserTextureUrlResolver.faceUrl("SKIN", sha));
        this.users = new UserDao(database);
        this.profiles = new ProfileDao(database);
    }

    public void myInfo(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) return;
        User user = users.findById(userId);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "code", "USER_NOT_FOUND"));
            return;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        String friendCode = im.xz.cn.common.UuidUtil.generateFriendCode(user.getId());
        info.put("friendCode", friendCode);
        info.put("friendCodeFormatted", im.xz.cn.common.UuidUtil.formatFriendCode(friendCode));
        info.put("displayProfileId", user.getDisplayProfileId());
        String displayName = user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : user.getUsername();
        info.put("displayName", displayName == null ? "" : displayName);
        info.put("avatar", im.xz.cn.social.SocialIdentityService.DEFAULT_AVATAR);
        info.put("cancelled", false);
        identities.findByUserId(userId).ifPresent(identity -> {
            info.put("displayName", identity.displayName());
            info.put("avatar", identity.avatarUrl());
            info.put("cancelled", identity.cancelled());
        });
        List<Map<String, Object>> profileList = new java.util.ArrayList<>();
        for (PlayerProfile profile : profiles.findByUserId(userId)) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", profile.getId());
            p.put("name", profile.getName());
            profileList.add(p);
        }
        info.put("profiles", profileList);
        ctx.json(Map.of("success", true, "info", info));
    }

    public void updateDisplayProfile(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) return;
        Map<String, Object> body;
        try {
            body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid JSON body"));
            return;
        }
        String profileId = body.get("profileId") == null ? "" : String.valueOf(body.get("profileId"));
        if (!profileId.isEmpty()) {
            PlayerProfile profile = profiles.findById(profileId);
            if (profile == null || !userId.equals(profile.getUserId())) {
                ctx.status(400).json(Map.of("success", false, "code", "INVALID_PROFILE",
                        "message", "Profile not found or not owned by current user"));
                return;
            }
        }
        users.updateDisplayProfile(userId, profileId.isEmpty() ? null : profileId);
        ctx.json(Map.of("success", true, "displayProfileId", profileId));
    }

    public void listFriends(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
        for (String friendId : service.friends(userId)) {
            users.add(identityMap(friendId));
        }
        ctx.json(Map.of("success", true, "users", users));
    }

    public void listIncoming(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        boolean outgoing = "outgoing".equalsIgnoreCase(ctx.queryParam("direction"));
        var requests = outgoing ? service.outgoing(userId) : service.incoming(userId);
        for (FriendRequestRepository.FriendRequest request : requests) {
            Map<String, Object> item = toMap(request);
            if (outgoing) {
                item.put("toUser", identityMap(request.toUserId()));
            } else {
                item.put("fromUser", identityMap(request.fromUserId()));
            }
            items.add(item);
        }
        ctx.json(Map.of("success", true, "direction", outgoing ? "outgoing" : "incoming",
                "items", items));
    }

    public void sendRequest(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        String toUserId = readToUserId(ctx);
        if (toUserId == null) {
            return;
        }
        try {
            FriendRequestRepository.FriendRequest request = service.sendRequest(userId, toUserId);
            ctx.json(Map.of("success", true, "request", toMap(request)));
        } catch (IllegalArgumentException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    public void accept(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long id = readId(ctx);
        if (id < 0) {
            return;
        }
        switch (service.accept(id, userId)) {
            case ACCEPTED -> ctx.json(Map.of("success", true, "status", "ACCEPTED"));
            case NOT_FOUND -> ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Friend request not found"));
            case NOT_RECIPIENT -> ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN",
                    "message", "Only the recipient can accept"));
            case NOT_PENDING -> ctx.status(409).json(Map.of("success", false, "code", "CONFLICT",
                    "message", "Friend request is not pending"));
        }
    }

    public void reject(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long id = readId(ctx);
        if (id < 0) {
            return;
        }
        if (service.reject(id, userId)) {
            ctx.json(Map.of("success", true, "status", "REJECTED"));
        } else {
            ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Friend request not found or not pending"));
        }
    }

    public void cancel(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long id = readId(ctx);
        if (id < 0) {
            return;
        }
        if (service.cancelRequest(id, userId)) {
            ctx.json(Map.of("success", true, "status", "CANCELLED"));
        } else {
            ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Friend request not found or not pending"));
        }
    }

    public void block(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        String toUserId = readToUserId(ctx);
        if (toUserId == null) {
            return;
        }
        try {
            service.block(userId, toUserId);
            ctx.json(Map.of("success", true));
        } catch (IllegalArgumentException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    public void unblock(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        String other = ctx.pathParam("userId");
        if (other == null || other.isBlank()) {
            badRequest(ctx, "userId is required");
            return;
        }
        ctx.json(Map.of("success", true, "removed", service.unblock(userId, other)));
    }

    public void listBlocked(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
        for (String blockedId : service.blocked(userId)) {
            users.add(identityMap(blockedId));
        }
        ctx.json(Map.of("success", true, "users", users));
    }

    public void clearBlocked(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        ctx.json(Map.of("success", true, "removedCount", service.clearBlocked(userId)));
    }

    public void unblockByBody(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
            Object raw = body.get("userId");
            if (!(raw instanceof String other) || other.isBlank()) {
                badRequest(ctx, "userId is required");
                return;
            }
            ctx.json(Map.of("success", true, "removed", service.unblock(userId, other)));
        } catch (Exception error) {
            badRequest(ctx, "Invalid JSON body");
        }
    }

    public void removeFriend(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        String other = ctx.pathParam("userId");
        if (other == null || other.isBlank()) {
            badRequest(ctx, "userId is required");
            return;
        }
        if (service.removeFriend(userId, other)) {
            ctx.json(Map.of("success", true));
        } else {
            ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Friendship not found"));
        }
    }

    private String readToUserId(Context ctx) {
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
            Object value = body.get("toUserId");
            if (value instanceof String toUserId && !toUserId.isBlank()) {
                return toUserId;
            }
        } catch (Exception ignored) {
            
        }
        badRequest(ctx, "toUserId is required");
        return null;
    }

    private long readId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException error) {
            badRequest(ctx, "Invalid request id");
            return -1;
        }
    }

    private static String requireUser(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED",
                    "message", "Login required"));
        }
        return userId;
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                "message", message == null ? "Invalid request" : message));
    }

    private Map<String, Object> identityMap(String userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", userId);
        map.put("name", "");
        map.put("avatar", im.xz.cn.social.SocialIdentityService.DEFAULT_AVATAR);
        identities.findByUserId(userId).ifPresent(identity -> {
            map.put("code", identity.userCode());
            map.put("name", identity.displayName());
            map.put("avatar", identity.avatarUrl());
            map.put("cancelled", identity.cancelled());
        });
        return map;
    }

    private static Map<String, Object> toMap(FriendRequestRepository.FriendRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.id());
        map.put("fromUserId", request.fromUserId());
        map.put("toUserId", request.toUserId());
        map.put("state", request.state());
        map.put("createdAt", request.createdAt() == null ? null : request.createdAt().toString());
        return map;
    }
}
