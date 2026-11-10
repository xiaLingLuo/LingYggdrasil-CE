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
package im.xz.cn.server.handler.admin;

import im.xz.cn.common.IpUtil;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.texture.asset.TextureAssetManagementService;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlobRepository;
import io.javalin.http.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class AdminTextureAssetHandler {

    private final TextureAssetManagementService service;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminTextureAssetHandler(DatabaseManager database) {
        this.service = new TextureAssetManagementService(database);
        this.assets = new TextureAssetRepository(database);
        this.blobs = new TextureBlobRepository(database);
    }

    public void list(Context ctx) {
        String type = normalizeType(ctx.queryParam("type"));
        boolean skins = AdminPermissions.has("admin.skins.view");
        boolean capes = AdminPermissions.has("admin.capes.view");
        if (type == null) {
            if (!skins && !capes) {
                forbidden(ctx);
                return;
            }
            
            if (!skins) {
                type = "CAPE";
            } else if (!capes) {
                type = "SKIN";
            }
        } else if (!AdminPermissions.require(ctx, node(type, "view"))) {
            return;
        }
        String search = ctx.queryParam("search");
        int limit = Math.max(1, Math.min(parseInt(ctx.queryParam("limit"), 100), 500));
        long offset = Math.max(0, parseLong(ctx.queryParam("offset"), 0L));
        List<Map<String, Object>> items = new ArrayList<>();
        for (TextureAssetManagementService.AssetRow row : service.page(search, type, limit, offset)) {
            items.add(toItem(row));
        }
        ctx.json(Map.of("success", true, "total", service.count(search, type),
                "limit", limit, "offset", offset, "items", items));
    }

    public void get(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null || !requireAsset(ctx, assetId, "view")) {
            return;
        }
        TextureAssetManagementService.AssetRow row = service.find(assetId);
        if (row == null) {
            ctx.status(404).json(Map.of("success", false, "message", "Asset not found"));
            return;
        }
        ctx.json(Map.of("success", true, "item", toItem(row)));
    }

    private Map<String, Object> toItem(TextureAssetManagementService.AssetRow row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.id());
        item.put("blobId", row.blobId());
        item.put("hash", row.hash());
        item.put("type", row.type());
        item.put("width", row.width());
        item.put("height", row.height());
        item.put("adminFileName", row.adminFileName());
        item.put("name", row.adminFileName());
        item.put("authorName", row.authorName());
        item.put("authorDisplayName", row.authorDisplayName());
        item.put("skinModel", row.skinModel());
        item.put("author", row.authorDisplayName());
        item.put("displayName", row.adminFileName());
        item.put("state", row.state());
        item.put("firstUploadName", row.firstUploadName());
        item.put("referenceCount", row.referenceCount());
        item.put("createdAt", row.createdAt() == null ? null : row.createdAt().toString());
        assets.findById(row.id()).ifPresent(asset -> blobs.findById(asset.blobId())
                .ifPresent(blob -> {
                    item.put("size", blob.sizeBytes());
                    item.put("url", "/admin/api/texture-assets/" + row.id() + "/file");
                }));
        return item;
    }

    public void updateAuthor(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null || !requireAsset(ctx, assetId, "alias")) {
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            Object raw = body.get("author");
            String author = raw == null ? null : String.valueOf(raw);
            service.updateAuthor(assetId, author);
            AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_TEXTURE_AUTHOR:" + assetId,
                    IpUtil.getClientIp(ctx));
            ctx.json(Map.of("success", true));
        } catch (Exception error) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", error.getMessage()));
        }
    }

    public void updateName(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null || !requireAsset(ctx, assetId, "alias")) {
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            Object raw = body.get("name");
            service.updateDisplayName(assetId, raw == null ? null : String.valueOf(raw));
            AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_TEXTURE_NAME:" + assetId,
                    IpUtil.getClientIp(ctx));
            ctx.json(Map.of("success", true));
        } catch (Exception error) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", error.getMessage()));
        }
    }

    public void updateSkinModel(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null || !requireAsset(ctx, assetId, "alias")) {
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            service.updateSkinModel(assetId, body.get("model") == null ? null : String.valueOf(body.get("model")));
            ctx.json(Map.of("success", true));
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "message", error.getMessage()));
        }
    }

    public void delete(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null) {
            return;
        }
        String type = service.typeOf(assetId);
        if (type == null) {
            ctx.json(Map.of("success", true, "alreadyDeleted", true));
            return;
        }
        if (!AdminPermissions.require(ctx, node(type, "delete"))) {
            return;
        }
        service.delete(assetId);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_TEXTURE_ASSET:" + assetId,
                IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true));
    }

    
    public void file(Context ctx) {
        Long assetId = assetId(ctx);
        if (assetId == null || !requireAsset(ctx, assetId, "download")) {
            return;
        }
        var asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            ctx.status(404);
            return;
        }
        var blob = blobs.findById(asset.blobId()).orElse(null);
        if (blob == null) {
            ctx.status(404);
            return;
        }
        try {
            File file = new File(blob.storageKey());
            if (!file.isFile()) {
                ctx.status(404);
                return;
            }
            ctx.contentType("image/png");
            ctx.header("Content-Disposition", "inline; filename=\"" + assetId + ".png\"");
            ctx.result(Files.readAllBytes(file.toPath()));
        } catch (Exception error) {
            ctx.status(404);
        }
    }

    private boolean requireAsset(Context ctx, long assetId, String action) {
        String type = service.typeOf(assetId);
        if (type == null) {
            ctx.status(404).json(Map.of("success", false, "message", "Asset not found"));
            return false;
        }
        if (!AdminPermissions.require(ctx, node(type, action))) {
            return false;
        }
        return true;
    }

    private static String node(String type, String action) {
        String prefix = "CAPE".equalsIgnoreCase(type) ? "admin.capes." : "admin.skins.";
        return prefix + action;
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return "SKIN".equals(value) || "CAPE".equals(value) ? value : null;
    }

    private static void forbidden(Context ctx) {
        ctx.status(403).json(Map.of("success", false, "message", "No permission"));
    }

    private Long assetId(Context ctx) {
        long assetId = parseLong(ctx.pathParam("id"), -1L);
        if (assetId <= 0) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", "Invalid asset id"));
            return null;
        }
        return assetId;
    }

    private static String getAdminName(Context ctx) {
        String name = im.xz.cn.auth.SessionManager.getAdminId(ctx);
        return name == null ? "admin" : name;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Long.parseLong(raw.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }
}
