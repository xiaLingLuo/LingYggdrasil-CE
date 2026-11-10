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
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.social.SocialIdentity;
import im.xz.cn.social.SocialIdentityService;
import im.xz.cn.texture.TextureService;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.interaction.InteractionCounts;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class PublicGalleryHandler {

    private final PublicationRepository publications;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;
    private final SocialIdentityService identities;
    private final TextureService textureService;
    private final InteractionRepository interactions;

    public PublicGalleryHandler(DatabaseManager database, TextureService textureService) {
        this.publications = new PublicationRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.blobs = new TextureBlobRepository(database);
        this.identities = new SocialIdentityService(database,
                sha -> im.xz.cn.texture.UserTextureUrlResolver.faceUrl("SKIN", sha));
        this.textureService = textureService;
        this.interactions = new InteractionRepository(database);
    }

    public void list(Context ctx) {
        int limit = clamp(parseInt(ctx.queryParam("limit"), 20), 1, 200);
        int offset = Math.max(0, parseInt(ctx.queryParam("offset"), 0));
        String type = ctx.queryParam("type");
        if (type != null && type.isBlank()) {
            type = null;
        }
        if (type != null && !"SKIN".equalsIgnoreCase(type) && !"CAPE".equalsIgnoreCase(type)) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "type must be SKIN or CAPE"));
            return;
        }
        boolean hot = "hot".equalsIgnoreCase(ctx.queryParam("sort"))
                || "heat".equalsIgnoreCase(ctx.queryParam("sort"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (TexturePublication publication : publications.findAvailablePage(limit, offset, type, hot)) {
            TextureAsset asset = assets.findById(publication.assetId())
                    .filter(value -> value.state() == TextureAsset.AssetState.READY)
                    .orElse(null);
            if (asset == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("publicationId", publication.id());
            item.put("assetId", asset.id());
            item.put("type", asset.type() == null ? null : asset.type().name());
            item.put("displayName", asset.adminFileName());
            item.put("author", asset.authorDisplayName());
            item.put("width", asset.width());
            item.put("height", asset.height());
            item.put("publisherId", publication.publisherId());
            identities.findByUserId(publication.publisherId()).ifPresent(identity -> {
                item.put("publisherCode", identity.userCode());
                item.put("publisherName", identity.displayName());
                item.put("cancelled", identity.cancelled());
            });
            blobs.findById(asset.blobId()).ifPresent(blob -> {
                item.put("hash", blob.sha256());
                item.put("url", im.xz.cn.texture.UserTextureUrlResolver.url(asset.type().name(), blob.sha256()));
            });
            InteractionCounts counts = interactions.counts(publication.id());
            item.put("likes", counts.likes());
            item.put("favorites", counts.favorites());
            item.put("heat", counts.heat());
            items.add(item);
        }
        ctx.json(Map.of("success", true, "items", items, "limit", limit, "offset", offset,
                "total", publications.countAvailable(type)));
    }

    public void detail(Context ctx) {
        long publicationId;
        try {
            publicationId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid publication id"));
            return;
        }
        TexturePublication publication = publications.findById(publicationId)
                .filter(TexturePublication::available)
                .orElse(null);
        if (publication == null) {
            ctx.status(404).json(Map.of("success", false, "code", "PUBLICATION_NOT_FOUND",
                    "message", "Material not found"));
            return;
        }
        TextureAsset asset = assets.findById(publication.assetId())
                .filter(value -> value.state() == TextureAsset.AssetState.READY)
                .orElse(null);
        if (asset == null) {
            ctx.status(404).json(Map.of("success", false, "code", "ASSET_NOT_FOUND",
                    "message", "Material not found"));
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("publicationId", publication.id());
        item.put("assetId", asset.id());
        item.put("type", asset.type() == null ? null : asset.type().name());
        item.put("displayName", asset.adminFileName());
        item.put("author", asset.authorDisplayName());
        item.put("width", asset.width());
        item.put("height", asset.height());
        item.put("publishedAt", publication.lastPublishedAt() == null ? null : publication.lastPublishedAt().toString());
        item.put("publisherId", publication.publisherId());
        identities.findByUserId(publication.publisherId()).ifPresent(identity -> {
            item.put("publisherCode", identity.userCode());
            item.put("publisherName", identity.displayName());
            item.put("publisherAvatar", identity.avatarUrl());
            item.put("cancelled", identity.cancelled());
        });
        blobs.findById(asset.blobId()).ifPresent(blob -> {
            item.put("hash", blob.sha256());
            item.put("url", im.xz.cn.texture.UserTextureUrlResolver.url(asset.type().name(), blob.sha256()));
        });
        InteractionCounts counts = interactions.counts(publicationId);
        item.put("likes", counts.likes());
        item.put("favorites", counts.favorites());
        String userId = SessionManager.getUserId(ctx);
        if (userId != null) {
            item.put("liked", interactions.hasLike(userId, publicationId));
            item.put("favorited", interactions.hasFavorite(userId, publicationId));
        }
        ctx.json(Map.of("success", true, "item", item));
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
