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

import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.texture.TextureService;
import im.xz.cn.texture.UserTextureUrlResolver;
import im.xz.cn.texture.access.TextureAccessLifecycle;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.asset.TextureUploadService;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.library.LibraryEntryRepository;
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;
import im.xz.cn.texture.sharing.ShareGrantRepository;
import io.javalin.http.Context;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class UserTextureUploadHandler {

    private final DatabaseManager database;
    private final TextureService textureService;
    private final TextureUploadService uploadService;
    private final TextureBlobRepository blobs;
    private final TextureAssetRepository assets;
    private final TextureUploadRepository uploads;
    private final LibraryEntryRepository library;
    private final PublicationRepository publications;
    private final InteractionRepository interactions;
    private final ShareGrantRepository grants;
    private final TextureAccessLifecycle lifecycle;

    public UserTextureUploadHandler(DatabaseManager database) {
        this.database = database;
        this.textureService = new TextureService(SystemConfig.getInstance(), new CacheDao(database));
        this.uploadService = new TextureUploadService(database, this.textureService);
        this.blobs = new TextureBlobRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.library = new LibraryEntryRepository(database);
        this.publications = new PublicationRepository(database);
        this.interactions = new InteractionRepository(database);
        this.grants = new ShareGrantRepository(database);
        this.lifecycle = new TextureAccessLifecycle(database);
    }

    
    public void list(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (TextureUpload upload : uploads.findActiveByUser(userId)) {
            assets.findById(upload.assetId())
                    .filter(asset -> asset.state() == TextureAsset.AssetState.READY)
                    .ifPresent(asset -> items.add(item(userId, upload, asset)));
        }
        ctx.json(Map.of("success", true, "items", items));
    }

    
    public void detail(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        Long assetId = assetId(ctx);
        if (assetId == null) {
            return;
        }
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            notFound(ctx, "Asset not found");
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, asset.type())) {
            return;
        }
        TextureUpload upload = uploads.findByUserAndAsset(userId, assetId)
                .filter(TextureUpload::active)
                .orElse(null);
        if (upload == null) {
            notFound(ctx, "Texture not found");
            return;
        }
        ctx.json(Map.of("success", true, "item", item(userId, upload, asset)));
    }

    
    public void visibility(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        Long assetId = assetId(ctx);
        if (assetId == null) {
            return;
        }
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            notFound(ctx, "Asset not found");
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, asset.type())) {
            return;
        }
        boolean isPublic;
        try {
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(ctx.body(), Map.class);
            Object raw = body.get("isPublic");
            if (!(raw instanceof Boolean value)) {
                badRequest(ctx, "isPublic must be boolean");
                return;
            }
            isPublic = value;
        } catch (Exception error) {
            badRequest(ctx, "Invalid JSON body");
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
        try {
            if (isPublic) {
                publications.publish(userId, assetId, Instant.now());
            } else {
                publications.findByPublisherAndAsset(userId, assetId)
                        .ifPresent(value -> {
                            publications.withdraw(value.id(), Instant.now());
                            lifecycle.reconcileAsset(assetId, "PUBLICATION_WITHDRAWN");
                        });
            }
            ctx.json(Map.of("success", true, "isPublic", isPublic));
        } catch (IllegalArgumentException | IllegalStateException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    
    public void delete(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        Long assetId = assetId(ctx);
        if (assetId == null) {
            return;
        }
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            notFound(ctx, "Asset not found");
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, asset.type())) {
            return;
        }
        TextureUpload upload = uploads.findByUserAndAsset(userId, assetId).orElse(null);
        if (upload == null || !upload.active()) {
            notFound(ctx, "Texture not found");
            return;
        }
        uploads.revoke(upload.id());
        publications.findByPublisherAndAsset(userId, assetId).ifPresent(value ->
                publications.withdraw(value.id(), Instant.now()));
        lifecycle.revokeGrantsForAsset(userId, assetId, "UPLOAD_REVOKED");
        library.findByUserAndAsset(userId, assetId).ifPresent(value -> library.delete(value.id()));
        lifecycle.reconcileAsset(assetId, "UPLOAD_REVOKED");
        ctx.json(Map.of("success", true));
    }

    
    public void alias(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        Long assetId = assetId(ctx);
        if (assetId == null) {
            return;
        }
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            notFound(ctx, "Asset not found");
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, asset.type())) {
            return;
        }
        TextureUpload upload = uploads.findByUserAndAsset(userId, assetId)
                .filter(TextureUpload::active).orElse(null);
        boolean favorite = false;
        if (upload == null) {
            
            for (TexturePublication publication : favoritesFor(userId)) {
                if (publication.assetId() == assetId) {
                    favorite = true;
                    break;
                }
            }
            if (!favorite) {
                favorite = grants.hasActiveForRecipient(userId, assetId);
            }
        }
        if (upload == null && !favorite) {
            notFound(ctx, "Texture not found");
            return;
        }
        String alias;
        try {
            Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(ctx.body(), Map.class);
            Object raw = body.get("alias");
            alias = raw == null ? null : String.valueOf(raw).trim();
        } catch (Exception error) {
            badRequest(ctx, "Invalid JSON body");
            return;
        }
        if (alias == null || alias.isEmpty()) {
            badRequest(ctx, "名称不能为空");
            return;
        }
        if (alias.length() > 64) {
            badRequest(ctx, "名称过长");
            return;
        }
        var entry = library.findByUserAndAsset(userId, assetId);
        if (entry.isEmpty()) {
            entry = java.util.Optional.of(library.ensure(userId, assetId, Instant.now()));
        }
        library.updateAlias(entry.get().id(), alias);
        ctx.json(Map.of("success", true, "alias", alias));
    }

    
    public void mine(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        List<Map<String, Object>> skins = new ArrayList<>();
        List<Map<String, Object>> capes = new ArrayList<>();
        for (TextureUpload upload : uploads.findActiveByUser(userId)) {
            assets.findById(upload.assetId())
                    .filter(asset -> asset.state() == TextureAsset.AssetState.READY)
                    .ifPresent(asset -> {
                        Map<String, Object> item = item(userId, upload, asset);
                        item.put("id", asset.id());
                        (asset.type() == TextureType.CAPE ? capes : skins).add(item);
                    });
        }

        List<Map<String, Object>> favoriteSkins = new ArrayList<>();
        List<Map<String, Object>> favoriteCapes = new ArrayList<>();
        for (Map<String, Object> favorite : favorites(userId)) {
            if ("CAPE".equals(favorite.get("type"))) {
                favoriteCapes.add(favorite);
            } else {
                favoriteSkins.add(favorite);
            }
        }

        List<Map<String, Object>> sharedSkins = new ArrayList<>();
        List<Map<String, Object>> sharedCapes = new ArrayList<>();
        for (var grant : grants.findActiveByRecipient(userId)) {
            assets.findById(grant.assetId()).ifPresent(asset -> {
                Map<String, Object> item = sharedItem(userId, asset, grant.id());
                (asset.type() == TextureType.CAPE ? sharedCapes : sharedSkins).add(item);
            });
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("skins", skins);
        body.put("capes", capes);
        body.put("favoriteSkins", favoriteSkins);
        body.put("favoriteCapes", favoriteCapes);
        body.put("sharedSkins", sharedSkins);
        body.put("sharedCapes", sharedCapes);
        ctx.json(body);
    }

    public void upload(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        TextureType type;
        try {
            type = TextureType.valueOf(
                    String.valueOf(ctx.queryParam("type")).toUpperCase(Locale.ROOT));
        } catch (RuntimeException error) {
            badRequest(ctx, "type must be SKIN or CAPE");
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, type)) {
            return;
        }
        if (!textureService.tryRecordUpload(userId, type.name())) {
            ctx.status(429).json(Map.of("success", false, "code", "TOO_MANY_REQUESTS",
                    "message", "Upload limit reached for today"));
            return;
        }
        String fileName = decodeFileName(ctx);
        try (InputStream input = ctx.bodyInputStream()) {
            TextureUploadService.UploadResult result = uploadService.upload(
                    userId, type.name(), fileName, input);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("assetId", result.assetId());
            body.put("uploadId", result.uploadId());
            body.put("hash", result.hash());
            body.put("url", UserTextureUrlResolver.url(type.name(), result.hash()));
            body.put("width", result.width());
            body.put("height", result.height());
            ctx.json(body);
        } catch (TextureUploadService.UploadException error) {
            int status = switch (error.code()) {
                case "UNAUTHORIZED" -> 401;
                case "PAYLOAD_TOO_LARGE" -> 413;
                case "BUSY" -> 503;
                case "QUOTA_EXCEEDED" -> 409;
                case "HASH_BLOCKED" -> 403;
                default -> 400;
            };
            ctx.status(status).json(Map.of("success", false, "code", error.code(),
                    "message", error.getMessage()));
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Upload failed: " + error.getMessage()));
        }
    }

    private Map<String, Object> item(String userId, TextureUpload upload, TextureAsset asset) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("uploadId", upload.id());
        item.put("assetId", asset.id());
        item.put("type", asset.type() == null ? null : asset.type().name());
        item.put("width", asset.width());
        item.put("height", asset.height());
        item.put("originalName", upload.originalName());
        item.put("displayName", asset.adminFileName());
        item.put("adminFileName", asset.adminFileName());
        item.put("author", asset.authorName());
        item.put("authorName", asset.authorName());
        item.put("authorDisplayName", asset.authorDisplayName());
        item.put("createdAt", upload.createdAt() == null ? null : upload.createdAt().toString());
        item.put("alias", library.findByUserAndAsset(userId, asset.id())
                .map(value -> value.alias()).orElse(null));
        item.put("isPublic", publications.findByPublisherAndAsset(userId, asset.id())
                .map(TexturePublication::available).orElse(false));
        item.put("available", asset.state() == TextureAsset.AssetState.READY);
        blobs.findById(asset.blobId()).ifPresent(blob -> {
            item.put("hash", blob.sha256());
            item.put("size", blob.sizeBytes());
            item.put("url", UserTextureUrlResolver.url(asset.type().name(), blob.sha256()));
        });
        return item;
    }

    private Map<String, Object> sharedItem(String userId, TextureAsset asset, long grantId) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", asset.id());
        item.put("assetId", asset.id());
        item.put("grantId", grantId);
        item.put("type", asset.type() == null ? null : asset.type().name());
        item.put("width", asset.width());
        item.put("height", asset.height());
        item.put("displayName", asset.adminFileName());
        item.put("author", asset.authorDisplayName());
        item.put("alias", library.findByUserAndAsset(userId, asset.id())
                .map(value -> value.alias()).orElse(null));
        item.put("source", "SHARE");
        item.put("available", asset.state() == TextureAsset.AssetState.READY);
        blobs.findById(asset.blobId()).ifPresent(blob -> {
            item.put("hash", blob.sha256());
            item.put("size", blob.sizeBytes());
            item.put("url", UserTextureUrlResolver.url(asset.type().name(), blob.sha256()));
        });
        return item;
    }

    private List<Map<String, Object>> favorites(String userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var publication : favoritesFor(userId)) {
            assets.findById(publication.assetId()).ifPresent(asset -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", asset.id());
                item.put("assetId", asset.id());
                item.put("publicationId", publication.id());
                item.put("type", asset.type() == null ? null : asset.type().name());
                item.put("width", asset.width());
                item.put("height", asset.height());
                item.put("displayName", asset.adminFileName());
                item.put("author", asset.authorDisplayName());
                item.put("alias", library.findByUserAndAsset(userId, asset.id())
                        .map(value -> value.alias()).orElse(null));
                item.put("source", "FAVORITE");
                item.put("available", publication.available()
                        && asset.state() == TextureAsset.AssetState.READY);
                blobs.findById(asset.blobId()).ifPresent(blob -> {
                    item.put("hash", blob.sha256());
                    item.put("size", blob.sizeBytes());
                    item.put("url", UserTextureUrlResolver.url(asset.type().name(), blob.sha256()));
                });
                result.add(item);
            });
        }
        return result;
    }

    private List<TexturePublication> favoritesFor(String userId) {
        List<Long> ids = im.xz.cn.infrastructure.persistence.Jdbc.queryMany(database,
                rs -> rs.getLong("publication_id"),
                "SELECT publication_id FROM publication_favorites WHERE user_id = ?"
                        + " ORDER BY created_at DESC, publication_id DESC", userId);
        List<TexturePublication> result = new ArrayList<>();
        for (Long id : ids) {
            publications.findById(id).ifPresent(result::add);
        }
        return result;
    }

    private static String decodeFileName(Context ctx) {
        
        String raw = ctx.queryParam("name");
        if (raw == null || raw.isBlank()) {
            raw = ctx.header("X-File-Name");
        }
        return raw == null || raw.isBlank() ? null : raw;
    }

    private Long assetId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("assetId"));
        } catch (RuntimeException error) {
            badRequest(ctx, "Invalid asset id");
            return null;
        }
    }

    private static String requireUser(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            unauthorized(ctx);
        }
        return userId;
    }

    private static void unauthorized(Context ctx) {
        ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED",
                "message", "Login required"));
    }

    private static void notFound(Context ctx, String message) {
        ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND", "message", message));
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                "message", message == null ? "Invalid request" : message));
    }
}
