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


import im.xz.cn.i18n.I18n;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.*;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.web.view.UserPage;
import im.xz.cn.texture.TextureService;
import im.xz.cn.common.UuidUtil;

import io.javalin.http.Context;

import java.util.*;

public class UserFriendHandler {
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final FriendDao friendDao;
    private final ConfirmingFriendDao confirmingDao;
    private final BlockDao blockDao;
    private final TextureService textureService;
    private final SystemConfig sysConfig;
    private final TextureDao textureDao;
    private final TextureVisibilityDao visibilityDao;
    private final FriendSharedTextureDao friendSharedDao;

    public UserFriendHandler(UserDao userDao, ProfileDao profileDao, FriendDao friendDao,
                              ConfirmingFriendDao confirmingDao, BlockDao blockDao,
                              TextureService textureService, SystemConfig sysConfig,
                              TextureDao textureDao, TextureVisibilityDao visibilityDao,
                              FriendSharedTextureDao friendSharedDao) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.friendDao = friendDao;
        this.confirmingDao = confirmingDao;
        this.blockDao = blockDao;
        this.textureService = textureService;
        this.sysConfig = sysConfig;
        this.textureDao = textureDao;
        this.visibilityDao = visibilityDao;
        this.friendSharedDao = friendSharedDao;
    }

    public void friendsPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderFriendsPage(csrfToken));
    }

    public void getFriends(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<Map<String, Object>> list = new ArrayList<>();

        List<Map<String, Object>> rawFriends = friendDao.findByUserId(user.getId());
        for (var f : rawFriends) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "confirmed");
            map.put("userId", f.get("userId"));
            String nickname = (String) f.get("nickname");
            String username = (String) f.get("username");
            map.put("displayName", nickname != null ? nickname : username);
            map.put("username", username);
            map.put("friendCode", f.get("friendCode"));
            map.put("friendCodeFormatted", UuidUtil.formatFriendCode((String) f.get("friendCode")));
            map.put("skinUrl", proxyTextureUrl((String) f.get("skinUrl")));
            map.put("skinModel", f.get("skinModel") != null ? f.get("skinModel") : "default");
            list.add(map);
        }

        List<Map<String, Object>> pending = confirmingDao.findByUser(user.getId());
        for (var p : pending) {
            String otherId = (String) p.get("userId");
            String senderId = (String) p.get("senderId");
            String receiverId = (String) p.get("receiverId");
            boolean imSender = user.getId().equals(senderId);

            if (imSender && blockDao.isBlocked(receiverId, senderId)) continue;
            if (!imSender && blockDao.isBlocked(senderId, receiverId)) continue;
            if (!imSender && blockDao.isBlocked(user.getId(), senderId)) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", imSender ? "pending_sent" : "pending_received");
            map.put("requestId", p.get("requestId"));
            map.put("userId", otherId);
            String nickname = (String) p.get("nickname");
            String username = (String) p.get("username");
            map.put("displayName", nickname != null ? nickname : username);
            map.put("username", username);
            map.put("friendCode", p.get("friendCode"));
            map.put("friendCodeFormatted", UuidUtil.formatFriendCode((String) p.get("friendCode")));
            list.add(map);
        }

        ctx.json(Map.of("success", true, "friends", list));
    }

    public void getMyInfo(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String friendCode = user.getId() != null ? UuidUtil.generateFriendCode(user.getId()) : null;
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("friendCode", friendCode);
        info.put("friendCodeFormatted", UuidUtil.formatFriendCode(friendCode));
        info.put("displayProfileId", user.getDisplayProfileId());

        if (user.getDisplayProfileId() != null) {
            PlayerProfile profile = profileDao.findById(user.getDisplayProfileId());
            if (profile != null) {
                info.put("displayProfileName", profile.getName());
                info.put("skinUrl", proxyTextureUrl(profile.getSkinUrl()));
                info.put("skinModel", profile.getSkinModel() != null ? profile.getSkinModel() : "default");
            }
        }

        List<PlayerProfile> profiles = profileDao.findByUserId(user.getId());
        List<Map<String, Object>> profileList = new ArrayList<>();
        for (var p : profiles) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.getId());
            pm.put("name", p.getName());
            pm.put("skinUrl", proxyTextureUrl(p.getSkinUrl()));
            pm.put("skinModel", p.getSkinModel() != null ? p.getSkinModel() : "default");
            profileList.add(pm);
        }
        info.put("profiles", profileList);

        ctx.json(Map.of("success", true, "info", info));
    }

    public void updateDisplayProfile(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String profileId = (String) body.get("profileId");
            if (profileId != null && !profileId.isBlank()) {
                PlayerProfile profile = profileDao.findById(profileId);
                if (profile == null || !profile.getUserId().equals(user.getId())) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.profileNotFound")));
                    return;
                }
            }
            userDao.updateDisplayProfile(user.getId(), profileId != null && !profileId.isBlank() ? profileId : null);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.displayProfileUpdated")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void addFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendCode = (String) body.get("friendCode");
            if (friendCode == null || friendCode.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.enterFriendCode")));
                return;
            }
            friendCode = friendCode.replace("-", "").trim();
            if (friendCode.length() != 16 || !friendCode.matches("\\d+")) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.friendCodeInvalid")));
                return;
            }
            User target = userDao.findByFriendCode(friendCode);
            if (target == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.friendCodeNotFound")));
                return;
            }
            if (target.getId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.cannotAddSelf")));
                return;
            }
            if (friendDao.existsFriendship(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.alreadyFriends")));
                return;
            }
            if (blockDao.isBlocked(target.getId(), user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.blockedByPeer")));
                return;
            }
            if (blockDao.isBlocked(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.removeFromBlocklist")));
                return;
            }
            if (confirmingDao.exists(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.friendRequestPending")));
                return;
            }
            confirmingDao.createRequest(user.getId(), target.getId());
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "friend");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.friendRequestSent")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void acceptRequest(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String requestId = (String) body.get("requestId");
            if (requestId == null || requestId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingRequestId")));
                return;
            }
            var req = confirmingDao.findById(requestId);
            if (req == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.requestNotFound")));
                return;
            }
            if (!user.getId().equals(String.valueOf(req.get("receiver_id")))) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.onlyReceiverAccept")));
                return;
            }
            String senderId = String.valueOf(req.get("sender_id"));
            friendDao.addFriend(user.getId(), senderId);
            confirmingDao.deleteById(requestId);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "friend");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.friendAdded")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void cancelRequest(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String requestId = (String) body.get("requestId");
            if (requestId == null || requestId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingRequestId")));
                return;
            }
            var req = confirmingDao.findById(requestId);
            if (req == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.requestNotFound")));
                return;
            }
            String senderId = String.valueOf(req.get("sender_id"));
            String receiverId = String.valueOf(req.get("receiver_id"));
            if (!user.getId().equals(senderId) && !user.getId().equals(receiverId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.noPermissionRequest")));
                return;
            }
            confirmingDao.deleteById(requestId);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.requestCancelled")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void deleteFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendId = (String) body.get("friendId");
            if (friendId == null || friendId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingFriendId")));
                return;
            }
            if (!friendDao.existsFriendship(user.getId(), friendId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.notFriends")));
                return;
            }
            friendDao.deleteFriend(user.getId(), friendId);
            textureDao.deleteFriendRefs(user.getId(), friendId);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "friend");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.friendDeleted")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void blockUser(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String targetId = (String) body.get("userId");
            if (targetId == null || targetId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingUserId")));
                return;
            }
            if (blockDao.isBlocked(user.getId(), targetId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.alreadyBlocked")));
                return;
            }
            int current = blockDao.countByBlocker(user.getId());
            if (current >= sysConfig.getMaxBlockedUsers()) {
                if (sysConfig.getMaxBlockedUsers() == 0) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.blocklistDisabled")));
                } else {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.blocklistFull", sysConfig.getMaxBlockedUsers())));
                }
                return;
            }
            blockDao.block(user.getId(), targetId);
            textureDao.deleteAllRefsBetweenUsers(user.getId(), targetId);
            var pending = confirmingDao.findByUser(user.getId());
            for (var p : pending) {
                String otherId = String.valueOf(p.get("userId"));
                if (otherId.equals(targetId)) {
                    confirmingDao.deleteById(String.valueOf(p.get("requestId")));
                }
            }
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "blacklist");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.blocked")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void unblockUser(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String targetId = (String) body.get("userId");
            if (targetId == null || targetId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingUserId")));
                return;
            }
            blockDao.unblock(user.getId(), targetId);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "blacklist");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.removedFromBlocklist")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void getBlocked(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        var list = blockDao.findByBlocker(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (var b : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("userId", b.get("blockedId"));
            String code = (String) b.get("friendCode");
            map.put("friendCode", code);
            map.put("friendCodeFormatted", UuidUtil.formatFriendCode(code));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "blocked", result));
    }

    public void getBlockedCount(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        int count = blockDao.countByBlocker(user.getId());
        ctx.json(Map.of("success", true, "count", count));
    }

    public void clearBlocked(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        blockDao.clearAll(user.getId());
        im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "blacklist");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.blocklistCleared")));
    }

    public void serveTexture(Context ctx) {
        String type = ctx.pathParam("type").toUpperCase();
        String hash = ctx.pathParam("hash");
        if (!"SKIN".equals(type) && !"CAPE".equals(type)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidTextureType")));
            return;
        }
        byte[] data = textureService.readFile(type, hash);
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
            return;
        }
        String userId = SessionManager.getUserId(ctx);
        boolean allowed = false;
        java.util.List<Texture> textures = textureDao.findAllByHash(type, hash);
        for (Texture t : textures) {
            if (visibilityDao.isPublic(t.getUserId(), t.getId())) {
                allowed = true;
                break;
            }
            if (userId != null && textureDao.hasReference(userId, type, hash)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.noTextureAccess")));
            return;
        }
        ctx.contentType("image/png");
        ctx.result(data);
    }

    private String proxyTextureUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return null;
        int idx = publicUrl.indexOf("/textures/");
        if (idx < 0) return publicUrl;
        return "/api/friends/texture/" + publicUrl.substring(idx + "/textures/".length());
    }

    private User checkAuth(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.redirect("/login");
            return null;
        }
        User user = userDao.findById(userId);
        if (user == null || !im.xz.cn.security.UserPermissions.isAccessible()) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }
}
