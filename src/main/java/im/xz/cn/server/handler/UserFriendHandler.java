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
package im.xz.cn.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.*;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.something.web.UserPage;
import im.xz.cn.util.TextureService;
import im.xz.cn.util.UuidUtil;

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

    public UserFriendHandler(UserDao userDao, ProfileDao profileDao, FriendDao friendDao,
                              ConfirmingFriendDao confirmingDao, BlockDao blockDao,
                              TextureService textureService, SystemConfig sysConfig) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.friendDao = friendDao;
        this.confirmingDao = confirmingDao;
        this.blockDao = blockDao;
        this.textureService = textureService;
        this.sysConfig = sysConfig;
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

        // Confirmed friends
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

        // Pending requests (filter out blocked)
        List<Map<String, Object>> pending = confirmingDao.findByUser(user.getId());
        for (var p : pending) {
            String otherId = (String) p.get("userId");
            String senderId = (String) p.get("senderId");
            String receiverId = (String) p.get("receiverId");
            boolean imSender = user.getId().equals(senderId);

            // If I'm blocked by the other person, skip
            if (imSender && blockDao.isBlocked(receiverId, senderId)) continue;
            if (!imSender && blockDao.isBlocked(senderId, receiverId)) continue;
            // If I blocked them, skip their incoming request
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
                    ctx.json(Map.of("success", false, "message", "角色不存在或无权操作"));
                    return;
                }
            }
            userDao.updateDisplayProfile(user.getId(), profileId != null && !profileId.isBlank() ? profileId : null);
            ctx.json(Map.of("success", true, "message", "展示角色已更新"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void addFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendCode = (String) body.get("friendCode");
            if (friendCode == null || friendCode.isBlank()) {
                ctx.json(Map.of("success", false, "message", "请输入好友代码"));
                return;
            }
            friendCode = friendCode.replace("-", "").trim();
            if (friendCode.length() != 16 || !friendCode.matches("\\d+")) {
                ctx.json(Map.of("success", false, "message", "好友代码格式不正确"));
                return;
            }
            User target = userDao.findByFriendCode(friendCode);
            if (target == null) {
                ctx.json(Map.of("success", false, "message", "未找到该好友代码对应的用户"));
                return;
            }
            if (target.getId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", "不能添加自己为好友"));
                return;
            }
            if (friendDao.existsFriendship(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", "已经是好友了"));
                return;
            }
            if (blockDao.isBlocked(target.getId(), user.getId())) {
                ctx.json(Map.of("success", false, "message", "你已被对方拉黑，无法发送请求"));
                return;
            }
            if (blockDao.isBlocked(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", "请先将其移出黑名单再发送请求"));
                return;
            }
            if (confirmingDao.exists(user.getId(), target.getId())) {
                ctx.json(Map.of("success", false, "message", "已存在待处理的好友请求"));
                return;
            }
            confirmingDao.createRequest(user.getId(), target.getId());
            ctx.json(Map.of("success", true, "message", "好友请求已发送"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void acceptRequest(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String requestId = (String) body.get("requestId");
            if (requestId == null || requestId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少请求ID"));
                return;
            }
            var req = confirmingDao.findById(requestId);
            if (req == null) {
                ctx.json(Map.of("success", false, "message", "请求不存在或已过期"));
                return;
            }
            if (!user.getId().equals(String.valueOf(req.get("receiver_id")))) {
                ctx.json(Map.of("success", false, "message", "仅请求接收者可接受"));
                return;
            }
            String senderId = String.valueOf(req.get("sender_id"));
            friendDao.addFriend(user.getId(), senderId);
            confirmingDao.deleteById(requestId);
            ctx.json(Map.of("success", true, "message", "已添加为好友"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void cancelRequest(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String requestId = (String) body.get("requestId");
            if (requestId == null || requestId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少请求ID"));
                return;
            }
            var req = confirmingDao.findById(requestId);
            if (req == null) {
                ctx.json(Map.of("success", false, "message", "请求不存在或已过期"));
                return;
            }
            String senderId = String.valueOf(req.get("sender_id"));
            String receiverId = String.valueOf(req.get("receiver_id"));
            if (!user.getId().equals(senderId) && !user.getId().equals(receiverId)) {
                ctx.json(Map.of("success", false, "message", "无权操作此请求"));
                return;
            }
            confirmingDao.deleteById(requestId);
            ctx.json(Map.of("success", true, "message", "请求已取消"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void deleteFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendId = (String) body.get("friendId");
            if (friendId == null || friendId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少好友ID"));
                return;
            }
            if (!friendDao.existsFriendship(user.getId(), friendId)) {
                ctx.json(Map.of("success", false, "message", "不是好友关系"));
                return;
            }
            friendDao.deleteFriend(user.getId(), friendId);
            ctx.json(Map.of("success", true, "message", "好友已删除"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void blockUser(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String targetId = (String) body.get("userId");
            if (targetId == null || targetId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少用户ID"));
                return;
            }
            if (blockDao.isBlocked(user.getId(), targetId)) {
                ctx.json(Map.of("success", false, "message", "已在此黑名单中"));
                return;
            }
            int current = blockDao.countByBlocker(user.getId());
            if (current >= sysConfig.getMaxBlockedUsers()) {
                if (sysConfig.getMaxBlockedUsers() == 0) {
                    ctx.json(Map.of("success", false, "message", "黑名单功能已被管理员禁用"));
                } else {
                    ctx.json(Map.of("success", false, "message", "黑名单已满（上限 " + sysConfig.getMaxBlockedUsers() + " 人）"));
                }
                return;
            }
            blockDao.block(user.getId(), targetId);
            // Clean up any pending request with this person
            var pending = confirmingDao.findByUser(user.getId());
            for (var p : pending) {
                String otherId = String.valueOf(p.get("userId"));
                if (otherId.equals(targetId)) {
                    confirmingDao.deleteById(String.valueOf(p.get("requestId")));
                }
            }
            ctx.json(Map.of("success", true, "message", "已拉黑"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void unblockUser(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String targetId = (String) body.get("userId");
            if (targetId == null || targetId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少用户ID"));
                return;
            }
            blockDao.unblock(user.getId(), targetId);
            ctx.json(Map.of("success", true, "message", "已移出黑名单"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
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
        ctx.json(Map.of("success", true, "message", "黑名单已清空"));
    }

    public void serveTexture(Context ctx) {
        String type = ctx.pathParam("type").toUpperCase();
        String hash = ctx.pathParam("hash");
        if (!"SKIN".equals(type) && !"CAPE".equals(type)) {
            ctx.status(400).json(Map.of("success", false, "message", "无效的材质类型"));
            return;
        }
        byte[] data = textureService.readFile(type, hash);
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", "材质不存在"));
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
        if (user == null) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }
}
