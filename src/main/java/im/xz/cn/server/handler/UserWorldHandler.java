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
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.model.enums.UserRole;
import im.xz.cn.something.web.UserPage;
import im.xz.cn.util.TextureService;

import io.javalin.http.Context;

import java.util.*;

public class UserWorldHandler {
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final TextureDao textureDao;
    private final TextureLikeDao likeDao;
    private final TextureFavoriteDao favoriteDao;
    private final TextureVisibilityDao visibilityDao;
    private final FriendSharedTextureDao friendSharedDao;
    private final FriendDao friendDao;
    private final TextureService textureService;
    private final SystemConfig sysConfig;

    public UserWorldHandler(UserDao userDao, ProfileDao profileDao, TextureDao textureDao,
                            TextureLikeDao likeDao, TextureFavoriteDao favoriteDao,
                            TextureVisibilityDao visibilityDao, FriendSharedTextureDao friendSharedDao,
                            FriendDao friendDao, TextureService textureService, SystemConfig sysConfig) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.textureDao = textureDao;
        this.likeDao = likeDao;
        this.favoriteDao = favoriteDao;
        this.visibilityDao = visibilityDao;
        this.friendSharedDao = friendSharedDao;
        this.friendDao = friendDao;
        this.textureService = textureService;
        this.sysConfig = sysConfig;
    }

    public void worldPage(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderWorldPage(csrfToken, userId));
    }

    public void sharedPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderSharedPage(csrfToken));
    }

    public void getPublicTextures(Context ctx) {
        try {
        String userId = SessionManager.getUserId(ctx);
        User user = userId != null ? userDao.findById(userId) : null;
        boolean isAnonymous = (user == null);

        String type = ctx.queryParam("type");
        String after = ctx.queryParam("after");
        int limit = 20;
        try {
            String limitParam = ctx.queryParam("limit");
            if (limitParam != null) {
                limit = Math.min(Integer.parseInt(limitParam), 50);
            }
        } catch (NumberFormatException ignored) {}

        if (isAnonymous) {
            limit = 100;
            after = null;
        }

        List<Texture> textures = textureDao.findPublicTextures(type, after, limit);

        List<String> textureIds = new ArrayList<>();
        Set<String> ownerIds = new HashSet<>();
        for (Texture t : textures) {
            textureIds.add(t.getId());
            ownerIds.add(t.getUserId());
        }

        Map<String, Integer> likeCounts = likeDao.batchCountByTextures(textureIds);

        Set<String> likedIds = new HashSet<>();
        Set<String> favoritedIds = new HashSet<>();
        if (user != null) {
            likedIds = batchCheckLikes(user.getId(), textureIds);
            favoritedIds = favoriteDao.findFavoriteTextureIds(user.getId());
        }

        Map<String, String> ownerNames = batchOwnerNames(new ArrayList<>(ownerIds));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Texture t : textures) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("type", t.getType());
            map.put("hash", t.getHash());
            map.put("alias", t.getAlias());
            map.put("originalName", t.getOriginalName());
            map.put("size", t.getSize());
            map.put("createdAt", t.getCreatedAt());
            map.put("ownerId", t.getUserId());
            map.put("ownerName", ownerNames.getOrDefault(t.getUserId(), "未知用户"));
            map.put("likeCount", likeCounts.getOrDefault(t.getId(), 0));
            map.put("liked", likedIds.contains(t.getId()));
            map.put("favorited", favoritedIds.contains(t.getId()));
            map.put("thumbnailUrl", "/api/publicTexture/" + t.getType().toLowerCase() + "/" + t.getHash());
            map.put("detailUrl", "/api/" + ("CAPE".equals(t.getType()) ? "capes" : "skins") + "/download?id=" + t.getId());
            result.add(map);
        }

        boolean loginRequired = isAnonymous && textures.size() >= limit;
        String nextAfterV = (isAnonymous || textures.isEmpty()) ? null : textures.get(textures.size() - 1).getCreatedAt();
        boolean hasMoreV = !isAnonymous && textures.size() >= limit;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("textures", result);
        response.put("nextAfter", nextAfterV);
        response.put("hasMore", hasMoreV);
        response.put("loginRequired", loginRequired);
        ctx.json(response);
        } catch (Exception e) {
            System.err.println("[UserWorldHandler] getPublicTextures error: " + e.getMessage());
            e.printStackTrace();
            ctx.json(Map.of("success", false, "message", "加载材质失败: " + e.getClass().getSimpleName()));
        }
    }

    public void toggleLike(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少材质ID"));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null) {
                ctx.json(Map.of("success", false, "message", "材质不存在"));
                return;
            }
            if (!visibilityDao.isPublic(texture.getUserId(), textureId)) {
                ctx.json(Map.of("success", false, "message", "该材质未公开"));
                return;
            }
            boolean liked = likeDao.exists(user.getId(), textureId);
            if (liked) {
                likeDao.unlike(user.getId(), textureId);
            } else {
                likeDao.like(user.getId(), textureId);
            }
            ctx.json(Map.of("success", true, "liked", !liked));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void toggleFavorite(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少材质ID"));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null) {
                ctx.json(Map.of("success", false, "message", "材质不存在"));
                return;
            }
            boolean favorited = favoriteDao.exists(user.getId(), textureId);
            if (favorited) {
                favoriteDao.unfavorite(user.getId(), textureId);
                ctx.json(Map.of("success", true, "favorited", false));
            } else {
                int maxFavorites = sysConfig.getMaxFavorites();
                if (maxFavorites == 0) {
                    ctx.json(Map.of("success", false, "message", "收藏功能已被关闭"));
                    return;
                }
                if (maxFavorites > 0) {
                    int currentCount = favoriteDao.countByUserId(user.getId());
                    if (currentCount >= maxFavorites) {
                        ctx.json(Map.of("success", false, "message", "收藏数量已达上限 (" + maxFavorites + " 个)"));
                        return;
                    }
                }
                if (!canReference(user, texture)) {
                    ctx.json(Map.of("success", false, "message", "该材质未公开且未通过好友共享"));
                    return;
                }
                String alias = (String) body.get("alias");
                if (alias == null || alias.isBlank()) {
                    alias = texture.getAlias() != null ? texture.getAlias() : texture.getOriginalName();
                }
                favoriteDao.favorite(user.getId(), textureId, alias);
                ctx.json(Map.of("success", true, "favorited", true));
            }
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void updateFavoriteAlias(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            String alias = (String) body.get("alias");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少材质ID"));
                return;
            }
            if (!favoriteDao.exists(user.getId(), textureId)) {
                ctx.json(Map.of("success", false, "message", "未收藏该材质"));
                return;
            }
            favoriteDao.updateAlias(user.getId(), textureId, alias);
            ctx.json(Map.of("success", true, "message", "别名已更新"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void setVisibility(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            Object isPublicRaw = body.get("isPublic");
            boolean isPublic;
            if (isPublicRaw instanceof Boolean b) {
                isPublic = b;
            } else if (isPublicRaw instanceof Number n) {
                isPublic = n.intValue() != 0;
            } else if (isPublicRaw instanceof String s) {
                isPublic = "true".equalsIgnoreCase(s) || "1".equals(s);
            } else {
                ctx.json(Map.of("success", false, "message", "缺少isPublic参数"));
                return;
            }
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少材质ID"));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", "材质不存在或无权操作"));
                return;
            }
            visibilityDao.setVisibility(user.getId(), textureId, isPublic);
            ctx.json(Map.of("success", true, "message", isPublic ? "已设为公开" : "已设为私有", "isPublic", isPublic));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void getVisibility(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String textureId = ctx.queryParam("id");
        if (textureId == null || textureId.isBlank()) {
            ctx.json(Map.of("success", false, "message", "缺少材质ID"));
            return;
        }
        boolean isPublic = visibilityDao.isPublic(user.getId(), textureId);
        ctx.json(Map.of("success", true, "isPublic", isPublic));
    }

    public void getSharedTextures(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        var friendSharedList = friendSharedDao.findSharedByFriends(user.getId());
        var favoriteList = favoriteDao.findByUserId(user.getId());

        ctx.json(Map.of("success", true,
            "friendShared", formatSharedTextureList(friendSharedList),
            "favorites", formatSharedTextureList(favoriteList)));
    }

    public void shareToFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendId = (String) body.get("friendId");
            String textureId = (String) body.get("textureId");
            if (friendId == null || friendId.isBlank() || textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少参数"));
                return;
            }
            if (!friendDao.existsFriendship(user.getId(), friendId)) {
                ctx.json(Map.of("success", false, "message", "不是好友关系"));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", "材质不存在或无权操作"));
                return;
            }
            friendSharedDao.share(user.getId(), friendId, textureId);
            ctx.json(Map.of("success", true, "message", "已共享给好友"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void unshareFromFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendId = (String) body.get("friendId");
            String textureId = (String) body.get("textureId");
            if (friendId == null || friendId.isBlank() || textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", "缺少参数"));
                return;
            }
            friendSharedDao.unshare(user.getId(), friendId, textureId);
            ctx.json(Map.of("success", true, "message", "已取消共享"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void getFriendSharedTextures(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String friendId = ctx.pathParam("friendId");
        if (friendId == null || friendId.isBlank()) {
            ctx.json(Map.of("success", false, "message", "缺少好友ID"));
            return;
        }
        if (!friendDao.existsFriendship(user.getId(), friendId)) {
            ctx.json(Map.of("success", false, "message", "不是好友关系"));
            return;
        }
        var sharedList = friendSharedDao.findSharedToFriend(friendId, user.getId());
        var ownerTextures = textureDao.findByUserId(friendId);
        List<Map<String, Object>> ownerResult = new ArrayList<>();
        for (Texture t : ownerTextures) {
            boolean isPublic = visibilityDao.isPublic(friendId, t.getId());
            if (isPublic) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", t.getId());
                map.put("type", t.getType());
                map.put("hash", t.getHash());
                map.put("alias", t.getAlias());
                map.put("originalName", t.getOriginalName());
                map.put("size", t.getSize());
                map.put("shared", false);
                map.put("thumbnailUrl", "/api/publicTexture/" + t.getType().toLowerCase() + "/" + t.getHash());
                ownerResult.add(map);
            }
        }
        List<Map<String, Object>> sharedResult = formatSharedTextureList(sharedList);
        ctx.json(Map.of("success", true, "publicTextures", ownerResult, "sharedTextures", sharedResult));
    }

    public void getMySharedToFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String friendId = ctx.pathParam("friendId");
        if (friendId == null || friendId.isBlank()) {
            ctx.json(Map.of("success", false, "message", "缺少好友ID"));
            return;
        }
        if (!friendDao.existsFriendship(user.getId(), friendId)) {
            ctx.json(Map.of("success", false, "message", "不是好友关系"));
            return;
        }
        var sharedList = friendSharedDao.findSharedToFriend(user.getId(), friendId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : sharedList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", String.valueOf(row.get("texture_id")));
            map.put("type", row.get("type"));
            map.put("hash", row.get("hash"));
            map.put("alias", row.get("texture_alias"));
            map.put("originalName", row.get("original_name"));
            map.put("thumbnailUrl", "/api/publicTexture/" + String.valueOf(row.get("type")).toLowerCase() + "/" + row.get("hash"));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    private boolean canReference(User user, Texture texture) {
        if (texture.getUserId().equals(user.getId())) return true;
        if (visibilityDao.isPublic(texture.getUserId(), texture.getId())) return true;
        if (friendSharedDao.exists(texture.getUserId(), user.getId(), texture.getId())) return true;
        return false;
    }

    private List<Map<String, Object>> formatSharedTextureList(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            String textureId = String.valueOf(row.get("texture_id"));
            map.put("id", textureId);
            map.put("type", row.get("type"));
            map.put("hash", row.get("hash"));
            map.put("alias", row.get("texture_alias"));
            map.put("originalName", row.get("original_name"));
            map.put("favoriteAlias", row.get("alias"));
            map.put("ownerName", row.containsKey("owner_nickname") ? (row.get("owner_nickname") != null ? row.get("owner_nickname") : row.get("owner_username")) : null);
            map.put("thumbnailUrl", "/api/publicTexture/" + String.valueOf(row.get("type")).toLowerCase() + "/" + row.get("hash"));
            map.put("downloadUrl", "/api/skins/download?id=" + textureId);
            result.add(map);
        }
        return result;
    }

    private Set<String> batchCheckLikes(String userId, List<String> textureIds) {
        Set<String> liked = new HashSet<>();
        if (textureIds.isEmpty()) return liked;
        int batchSize = 500;
        for (int offset = 0; offset < textureIds.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, textureIds.size());
            List<String> batch = textureIds.subList(offset, end);
            var placeholders = new StringBuilder();
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            Object[] params = new Object[batch.size() + 1];
            params[0] = userId;
            for (int i = 0; i < batch.size(); i++) params[i + 1] = batch.get(i);
            var rows = textureDao.queryRaw("SELECT texture_id FROM texture_likes WHERE user_id = ? AND texture_id IN (" + placeholders + ")", params);
            if (rows != null) {
                for (var row : rows) {
                    liked.add(String.valueOf(row.get("texture_id")));
                }
            }
        }
        return liked;
    }

    private Map<String, String> batchOwnerNames(List<String> ownerIds) {
        Map<String, String> names = new LinkedHashMap<>();
        if (ownerIds.isEmpty()) return names;
        var placeholders = new StringBuilder();
        for (int i = 0; i < ownerIds.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        var rows = textureDao.queryRaw("SELECT id, nickname, username FROM users WHERE id IN (" + placeholders + ")", ownerIds.toArray());
        if (rows != null) {
            for (var row : rows) {
                String id = String.valueOf(row.get("id"));
                String nickname = (String) row.get("nickname");
                String username = (String) row.get("username");
                String name = nickname != null && !nickname.isBlank() ? nickname : username;
                if (name != null) names.put(id, name);
            }
        }
        return names;
    }

    private User checkAuth(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.redirect("/login");
            return null;
        }
        User user = userDao.findById(userId);
        if (user == null || user.getRole() == UserRole.BANNED) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }
}
