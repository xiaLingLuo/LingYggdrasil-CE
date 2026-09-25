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
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.web.view.UserPage;
import im.xz.cn.texture.TextureService;
import im.xz.cn.logging.logApi;
import im.xz.cn.common.TimeUtil;

import io.javalin.http.Context;

import java.util.*;

public class UserWorldHandler {
    private static final logApi log = logApi.getLogger(UserWorldHandler.class);
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
                int parsed = Integer.parseInt(limitParam);
                if (parsed > 0) limit = Math.min(parsed, 50);
            }
        } catch (NumberFormatException ignored) {}

        if (isAnonymous) {
            limit = 100;
            after = null;
        }

        List<TextureDao.PublicTexture> texturePage = textureDao.findPublicTextures(type, after, limit);
        List<Texture> textures = texturePage.stream().map(TextureDao.PublicTexture::texture).toList();

        List<String> textureIds = new ArrayList<>();
        Set<String> ownerIds = new HashSet<>();
        for (Texture t : textures) {
            textureIds.add(t.getId());
            ownerIds.add(t.getUserId());
        }

        Map<String, Integer> likeCounts = likeDao.batchCountByTextures(textureIds);
        Map<String, Integer> favCounts = favoriteDao.batchCountByTextures(textureIds);

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
            map.put("ownerName", ownerNames.getOrDefault(t.getUserId(), I18n.t("msg.unknownUser")));
            map.put("likeCount", likeCounts.getOrDefault(t.getId(), 0));
            map.put("favCount", favCounts.getOrDefault(t.getId(), 0));
            map.put("liked", likedIds.contains(t.getId()));
            map.put("favorited", favoritedIds.contains(t.getId()));
            map.put("thumbnailUrl", "/api/publicTexture/" + t.getType().toLowerCase() + "/" + t.getHash());
            map.put("detailUrl", "/api/" + ("CAPE".equals(t.getType()) ? "capes" : "skins") + "/download?id=" + t.getId());
            result.add(map);
        }

        boolean loginRequired = isAnonymous && textures.size() >= limit;
        String nextAfterV = (isAnonymous || texturePage.isEmpty())
                ? null : textureDao.encodeCursor(texturePage.getLast());
        boolean hasMoreV = !isAnonymous && textures.size() >= limit;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("textures", result);
        response.put("nextAfter", nextAfterV);
        response.put("hasMore", hasMoreV);
        response.put("loginRequired", loginRequired);
        ctx.json(response);
        } catch (Exception e) {
            log.error("[UserWorldHandler] getPublicTextures error: {}", e.getMessage(), e);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.loadTextureFailed", e.getClass().getSimpleName())));
        }
    }

    public void toggleLike(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
                return;
            }
            if (!visibilityDao.isPublic(texture.getUserId(), textureId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotPublic")));
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
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void toggleFavorite(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
                return;
            }
            boolean favorited = favoriteDao.exists(user.getId(), textureId);
            if (favorited) {
                textureDao.deleteAnyRef(user.getId(), texture.getType(), texture.getHash());
                profileDao.clearTextureRefByHash(user.getId(), texture.getType(), texture.getHash());
                favoriteDao.unfavorite(user.getId(), textureId);
                ctx.json(Map.of("success", true, "favorited", false));
            } else {
                int maxFavorites = sysConfig.getMaxFavorites();
                if (maxFavorites == 0) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.favoriteDisabled")));
                    return;
                }
                if (maxFavorites > 0) {
                    int currentCount = favoriteDao.countByUserId(user.getId());
                    if (currentCount >= maxFavorites) {
                        ctx.json(Map.of("success", false, "message", I18n.t("msg.maxFavoritesReached", maxFavorites)));
                        return;
                    }
                }
                Texture held = textureDao.findByUserAndHash(user.getId(), texture.getType(), texture.getHash());
                if (held != null) {
                    ctx.json(Map.of("success", false, "message",
                            I18n.t("msg.libraryAlreadyHas", displayAlias(held))));
                    return;
                }
                if (!canReference(user, texture)) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotPublicShared")));
                    return;
                }
                String alias = (String) body.get("alias");
                if (alias == null || alias.isBlank()) {
                    alias = texture.getAlias() != null ? texture.getAlias() : texture.getOriginalName();
                }
                favoriteDao.favorite(user.getId(), textureId, alias);
                Texture existing = textureDao.findByUserAndHash(user.getId(), texture.getType(), texture.getHash());
                if (existing == null) {
                    Texture refTex = new Texture(UUID.randomUUID().toString(), user.getId(),
                        texture.getType(), texture.getHash(), alias, null, 0,
                        "image/png", TimeUtil.now());
                    refTex.setReferenceType("public");
                    refTex.setRefOwnerId(texture.getUserId());
                    refTex.setRefCreatedAt(TimeUtil.now());
                    textureDao.insert(refTex);
                }
                ctx.json(Map.of("success", true, "favorited", true));
            }
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
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
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            if (!favoriteDao.exists(user.getId(), textureId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.notFavorited")));
                return;
            }
            favoriteDao.updateAlias(user.getId(), textureId, alias);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.aliasUpdated")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
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
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingIsPublic")));
                return;
            }
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            if (isPublic) {
                if (textureDao.countPublicByHash(texture.getType(), texture.getHash(), user.getId()) > 0) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.textureAlreadyPublic")));
                    return;
                }
            } else {
                textureDao.deleteRefsByOwner(texture.getType(), texture.getHash(), user.getId());
                profileDao.clearTextureRefByHashForAll(texture.getType(), texture.getHash(), user.getId());
            }
            visibilityDao.setVisibility(user.getId(), textureId, isPublic);
            ctx.json(Map.of("success", true, "message", isPublic ? I18n.t("msg.setPublic") : I18n.t("msg.setPrivate"), "isPublic", isPublic));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void getVisibility(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String textureId = ctx.queryParam("id");
        if (textureId == null || textureId.isBlank()) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
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
        String friendId = null;
        String textureType = null;
        String textureHash = null;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            friendId = (String) body.get("friendId");
            String textureId = (String) body.get("textureId");
            if (friendId == null || friendId.isBlank() || textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
                return;
            }
            if (!friendDao.existsFriendship(user.getId(), friendId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.notFriends")));
                return;
            }
            Texture texture = textureDao.findById(textureId);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            textureType = texture.getType();
            textureHash = texture.getHash();
            User friend = userDao.findById(friendId);
            if (friend == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.friendCodeNotFound")));
                return;
            }
            if (textureDao.holdsTexture(friendId, textureType, textureHash)) {
                ctx.json(Map.of("success", false, "message",
                        I18n.t("msg.textureAlreadyOwnedByFriend", displayName(friend))));
                return;
            }
            friendSharedDao.share(user.getId(), friendId, textureId);
            Texture refTex = new Texture(UUID.randomUUID().toString(), friendId,
                textureType, textureHash, texture.getAlias(), null, 0,
                "image/png", TimeUtil.now());
            refTex.setReferenceType(im.xz.cn.common.ReferenceType.friend(
                    im.xz.cn.common.UuidUtil.generateFriendCode(user.getId())));
            refTex.setRefOwnerId(user.getId());
            refTex.setRefCreatedAt(TimeUtil.now());
            textureDao.insert(refTex);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.sharedToFriend")));
        } catch (Exception e) {
            if (im.xz.cn.database.DatabaseManager.isDuplicateKeyViolation(e)) {
                ctx.json(Map.of("success", false, "message", duplicateShareMessage(friendId, textureType, textureHash)));
                return;
            }
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    private String duplicateShareMessage(String friendId, String type, String hash) {
        if (friendId != null && type != null && hash != null && !textureDao.holdsTexture(friendId, type, hash)) {
            return I18n.t("msg.alreadySharedToFriend");
        }
        User friend = friendId == null ? null : userDao.findById(friendId);
        return I18n.t("msg.textureAlreadyOwnedByFriend", displayName(friend));
    }

    public void unshareFromFriend(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String friendId = (String) body.get("friendId");
            String textureId = (String) body.get("textureId");
            if (friendId == null || friendId.isBlank() || textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
                return;
            }
            Texture t = textureDao.findById(textureId);
            if (t == null || !t.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            friendSharedDao.deleteByOwnerAndTexture(user.getId(), textureId);
            textureDao.deleteRefByOwner(friendId, t.getType(), t.getHash(), user.getId());
            profileDao.clearTextureRefByHash(friendId, t.getType(), t.getHash());
            ctx.json(Map.of("success", true, "message", I18n.t("msg.shareRevoked")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void deleteReceivedTexture(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            Texture t = textureDao.findById(textureId);
            if (t == null) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
                return;
            }
            String ownerId = t.getUserId();
            if (!friendSharedDao.exists(ownerId, user.getId(), textureId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
                return;
            }
            textureDao.deleteRefByOwner(user.getId(), t.getType(), t.getHash(), ownerId);
            profileDao.clearTextureRefByHash(user.getId(), t.getType(), t.getHash());
            friendSharedDao.deleteByOwnerAndTexture(ownerId, textureId);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.receivedTextureDeleted")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void updateReceivedAlias(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(ctx.body(), new TypeReference<>() {});
            String textureId = (String) body.get("textureId");
            String alias = (String) body.get("alias");
            if (textureId == null || textureId.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingTextureId")));
                return;
            }
            Texture t = textureDao.findById(textureId);
            if (t == null || !friendSharedDao.exists(t.getUserId(), user.getId(), textureId)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotExist")));
                return;
            }
            if (alias != null && !alias.isBlank()) {
                boolean blacklisted = "CAPE".equalsIgnoreCase(t.getType())
                        ? sysConfig.isCapeNameBlacklisted(alias.trim())
                        : sysConfig.isSkinNameBlacklisted(alias.trim());
                if (blacklisted) {
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.nameTaken")));
                    return;
                }
            }
            textureDao.updateRefAlias(user.getId(), t.getType(), t.getHash(), alias);
            ctx.json(Map.of("success", true, "message", I18n.t("msg.aliasUpdated")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void getOutgoingShared(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        var rows = friendSharedDao.findOutgoing(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            String type = String.valueOf(row.get("type"));
            map.put("textureId", String.valueOf(row.get("texture_id")));
            map.put("friendId", String.valueOf(row.get("friend_id")));
            map.put("type", type);
            map.put("hash", row.get("hash"));
            map.put("alias", row.get("texture_alias"));
            map.put("originalName", row.get("original_name"));
            map.put("size", row.get("size"));
            map.put("friendName", displayNameValues(row.get("friend_nickname"), row.get("friend_username")));
            map.put("friendCode", row.get("friend_code"));
            map.put("createdAt", String.valueOf(row.get("created_at")));
            map.put("thumbnailUrl", "/api/publicTexture/" + type.toLowerCase() + "/" + row.get("hash"));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    public void getIncomingShared(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        var rows = friendSharedDao.findIncoming(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            String type = String.valueOf(row.get("type"));
            map.put("textureId", String.valueOf(row.get("texture_id")));
            map.put("type", type);
            map.put("hash", row.get("hash"));
            map.put("alias", row.get("receiver_alias") != null ? row.get("receiver_alias") : row.get("texture_alias"));
            map.put("originalName", row.get("original_name"));
            map.put("size", row.get("size"));
            map.put("ownerName", displayNameValues(row.get("owner_nickname"), row.get("owner_username")));
            map.put("ownerCode", row.get("owner_friend_code"));
            map.put("createdAt", String.valueOf(row.get("created_at")));
            map.put("thumbnailUrl", "/api/publicTexture/" + type.toLowerCase() + "/" + row.get("hash"));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    public void getFriendSharedTextures(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String friendId = ctx.pathParam("friendId");
        if (friendId == null || friendId.isBlank()) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.missingFriendId")));
            return;
        }
        if (!friendDao.existsFriendship(user.getId(), friendId)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.notFriends")));
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
            ctx.json(Map.of("success", false, "message", I18n.t("msg.missingFriendId")));
            return;
        }
        if (!friendDao.existsFriendship(user.getId(), friendId)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.notFriends")));
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
        return friendSharedDao.exists(texture.getUserId(), user.getId(), texture.getId());
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
        Set<String> found = new HashSet<>();
        var rows = textureDao.queryRaw("SELECT id, nickname, username, perm_group FROM users WHERE id IN (" + placeholders + ")", ownerIds.toArray());
        if (rows != null) {
            for (var row : rows) {
                String id = String.valueOf(row.get("id"));
                found.add(id);
                String permGroup = (String) row.get("perm_group");
                if ("banned".equalsIgnoreCase(permGroup)) {
                    names.put(id, I18n.t("msg.bannedName"));
                    continue;
                }
                String nickname = (String) row.get("nickname");
                String username = (String) row.get("username");
                String name = nickname != null && !nickname.isBlank() ? nickname : username;
                if (name != null) names.put(id, name);
            }
        }
        for (String id : ownerIds) {
            if (!found.contains(id)) {
                names.put(id, I18n.t("msg.accountCancelled"));
            }
        }
        return names;
    }

    private static String displayName(User user) {
        if (user == null) return "";
        String nickname = user.getNickname();
        return nickname != null && !nickname.isBlank() ? nickname : user.getUsername();
    }

    private static String displayNameValues(Object nickname, Object username) {
        if (nickname != null && !String.valueOf(nickname).isBlank()) return String.valueOf(nickname);
        return username == null ? "" : String.valueOf(username);
    }

    private static String displayAlias(Texture texture) {
        if (texture.getAlias() != null && !texture.getAlias().isBlank()) return texture.getAlias();
        if (texture.getOriginalName() != null && !texture.getOriginalName().isBlank()) return texture.getOriginalName();
        return texture.getHash() == null ? "" : texture.getHash();
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
