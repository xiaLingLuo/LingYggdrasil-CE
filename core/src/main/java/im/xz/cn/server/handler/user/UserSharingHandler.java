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
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.sharing.ShareGrantRepository;
import im.xz.cn.texture.sharing.TextureShareGrant;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class UserSharingHandler {

    private final DatabaseManager database;
    private final ShareGrantRepository grants;
    private final im.xz.cn.texture.asset.TextureAssetRepository assets;
    private final im.xz.cn.texture.asset.TextureBlobRepository blobs;
    private final im.xz.cn.texture.TextureService textureService;
    private final TextureUploadRepository uploads;
    private final im.xz.cn.texture.library.LibraryEntryRepository library;
    private final im.xz.cn.texture.access.TextureAccessLifecycle lifecycle;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserSharingHandler(DatabaseManager database) {
        this.database = database;
        this.grants = new ShareGrantRepository(database);
        this.lifecycle = new im.xz.cn.texture.access.TextureAccessLifecycle(database);
        this.assets = new im.xz.cn.texture.asset.TextureAssetRepository(database);
        this.blobs = new im.xz.cn.texture.asset.TextureBlobRepository(database);
        this.textureService = new im.xz.cn.texture.TextureService(
                im.xz.cn.config.SystemConfig.getInstance(), new im.xz.cn.database.dao.CacheDao(database));
        this.uploads = new TextureUploadRepository(database);
        this.library = new im.xz.cn.texture.library.LibraryEntryRepository(database);
    }

    public void list(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        boolean received = "received".equalsIgnoreCase(ctx.queryParam("direction"));
        List<TextureShareGrant> items = received
                ? grants.findActiveByRecipient(userId)
                : grants.findActiveByOwner(userId);
        List<Map<String, Object>> body = new ArrayList<>();
        for (TextureShareGrant grant : items) {
            body.add(toMap(grant, userId));
        }
        ctx.json(Map.of("success", true, "items", body));
    }

    public void create(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        Map<String, Object> payload;
        try {
            payload = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            badRequest(ctx, "Invalid JSON body");
            return;
        }
        Object assetValue = payload.get("assetId");
        Object recipientValue = payload.get("recipientUserId");
        if (!(assetValue instanceof Number assetNumber)
                || !(recipientValue instanceof String recipient) || recipient.isBlank()) {
            badRequest(ctx, "assetId and recipientUserId are required");
            return;
        }
        long assetId = assetNumber.longValue();
        if (recipient.equals(userId)) {
            badRequest(ctx, "Cannot share with yourself");
            return;
        }
        if (!recipientCanReceive(recipient)) {
            badRequest(ctx, "Recipient account is not available");
            return;
        }
        TextureUpload upload = uploads.findByUserAndAsset(userId, assetId).orElse(null);
        if (upload == null || !upload.active()) {
            badRequest(ctx, "You do not own an active upload for this asset");
            return;
        }
        if (!lifecycle.deliverable(assetId)) {
            badRequest(ctx, "Asset is not available");
            return;
        }
        if (!areFriends(userId, recipient)) {
            badRequest(ctx, "Recipient is not your friend");
            return;
        }
        if (isBlocked(userId, recipient) || isBlocked(recipient, userId)) {
            badRequest(ctx, "Blocked relationship");
            return;
        }
        try {
            TextureShareGrant grant = grants.create(upload.id(), userId, recipient, assetId, Instant.now());
            ctx.json(Map.of("success", true, "grant", toMap(grant, userId)));
        } catch (IllegalArgumentException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    public void revoke(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long grantId;
        try {
            grantId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException error) {
            badRequest(ctx, "Invalid grant id");
            return;
        }
        TextureShareGrant grant = grants.findById(grantId).orElse(null);
        if (grant == null) {
            ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Share grant not found"));
            return;
        }
        try {
            TextureShareGrant updated;
            if (grant.ownerId().equals(userId)) {
                updated = grants.revokeByOwner(userId, grantId, Instant.now());
            } else if (grant.recipientId().equals(userId)) {
                updated = grants.removeByRecipient(userId, grantId, Instant.now());
            } else {
                ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN",
                        "message", "Only owner or recipient can remove this share"));
                return;
            }
            if (!updated.state().active()) {
                lifecycle.reconcileAsset(updated.assetId(), "SHARE_REVOKED");
            }
            ctx.json(Map.of("success", true, "grant", toMap(updated, userId)));
        } catch (SecurityException error) {
            ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN",
                    "message", "Only owner or recipient can remove this share"));
        } catch (IllegalArgumentException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    
    private boolean recipientCanReceive(String recipientId) {
        var userDao = new im.xz.cn.database.dao.UserDao(database);
        var user = userDao.findById(recipientId);
        if (user == null) {
            return false;
        }
        if (im.xz.cn.security.UserPermissions.isCancelled(
                new im.xz.cn.database.dao.UserPermGroupDao(database), user)) {
            return false;
        }
        return im.xz.cn.security.UserPermissions.isAccessible(
                new im.xz.cn.database.dao.UserPermGroupDao(database), user);
    }

    private boolean isBlocked(String a, String b) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?", a, b) > 0;
    }

    private boolean areFriends(String a, String b) {
        String low = a.compareTo(b) <= 0 ? a : b;
        String high = a.compareTo(b) <= 0 ? b : a;
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                low, high) > 0;
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

    private Map<String, Object> toMap(TextureShareGrant grant, String viewerId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", grant.id());
        map.put("assetId", grant.assetId());
        map.put("ownerId", grant.ownerId());
        map.put("recipientId", grant.recipientId());
        map.put("state", grant.state() == null ? null : grant.state().name());
        map.put("createdAt", grant.createdAt() == null ? null : grant.createdAt().toString());
        map.put("available", lifecycle.deliverable(grant.assetId()));
        String counterpartId = viewerId != null && viewerId.equals(grant.recipientId())
                ? grant.ownerId() : grant.recipientId();
        map.put("counterpartId", counterpartId);
        var identity = new im.xz.cn.social.SocialIdentityService(database,
                sha -> im.xz.cn.texture.UserTextureUrlResolver.faceUrl("SKIN", sha))
                .findByUserId(counterpartId);
        identity.ifPresent(value -> {
            map.put("counterpartName", value.displayName());
            map.put("counterpartCode", value.userCode());
        });
        assets.findById(grant.assetId()).ifPresent(asset -> {
            map.put("type", asset.type() == null ? null : asset.type().name());
            map.put("width", asset.width());
            map.put("height", asset.height());
            map.put("displayName", asset.adminFileName());
            map.put("author", asset.authorDisplayName());
            library.findByUserAndAsset(viewerId, asset.id()).ifPresent(entry ->
                    map.put("alias", entry.alias()));
            blobs.findById(asset.blobId()).ifPresent(blob -> {
                map.put("hash", blob.sha256());
                map.put("size", blob.sizeBytes());
                map.put("url", im.xz.cn.texture.UserTextureUrlResolver.url(
                        asset.type().name(), blob.sha256()));
            });
        });
        return map;
    }

    private static Map<String, Object> unusedToMap(TextureShareGrant grant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", grant.id());
        map.put("assetId", grant.assetId());
        map.put("ownerId", grant.ownerId());
        map.put("recipientId", grant.recipientId());
        map.put("state", grant.state() == null ? null : grant.state().name());
        map.put("createdAt", grant.createdAt() == null ? null : grant.createdAt().toString());
        return map;
    }
}
