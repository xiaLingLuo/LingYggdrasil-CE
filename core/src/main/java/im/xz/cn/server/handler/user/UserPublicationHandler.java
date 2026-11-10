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
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class UserPublicationHandler {

    private final PublicationRepository publications;
    private final TextureUploadRepository uploads;
    private final im.xz.cn.texture.access.TextureAccessLifecycle lifecycle;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserPublicationHandler(DatabaseManager database) {
        this.publications = new PublicationRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.lifecycle = new im.xz.cn.texture.access.TextureAccessLifecycle(database);
    }

    public void mine(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        int limit = clamp(parseInt(ctx.queryParam("limit"), 60), 1, 200);
        int offset = Math.max(0, parseInt(ctx.queryParam("offset"), 0));
        List<Map<String, Object>> items = new ArrayList<>();
        for (TexturePublication publication : publications.findAvailableByPublisher(userId, limit, offset)) {
            items.add(toMap(publication));
        }
        ctx.json(Map.of("success", true, "items", items));
    }

    public void publish(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long assetId;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
            Object value = body.get("assetId");
            if (!(value instanceof Number number)) {
                badRequest(ctx, "assetId is required");
                return;
            }
            assetId = number.longValue();
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
            TexturePublication publication = publications.publish(userId, assetId, Instant.now());
            ctx.json(Map.of("success", true, "publication", toMap(publication)));
        } catch (IllegalArgumentException | IllegalStateException error) {
            badRequest(ctx, error.getMessage());
        }
    }

    public void withdraw(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        long publicationId;
        try {
            publicationId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException error) {
            badRequest(ctx, "Invalid publication id");
            return;
        }
        Optional<TexturePublication> found = publications.findById(publicationId);
        if (found.isEmpty()) {
            ctx.status(404).json(Map.of("success", false, "code", "NOT_FOUND",
                    "message", "Publication not found"));
            return;
        }
        if (!found.get().publisherId().equals(userId)) {
            ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN",
                    "message", "Only the publisher can withdraw"));
            return;
        }
        TexturePublication withdrawn = publications.withdraw(publicationId, Instant.now());
        lifecycle.reconcileAsset(withdrawn.assetId(), "PUBLICATION_WITHDRAWN");
        ctx.json(Map.of("success", true, "publication", toMap(withdrawn)));
    }

    private static Map<String, Object> toMap(TexturePublication publication) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", publication.id());
        map.put("assetId", publication.assetId());
        map.put("state", publication.state() == null ? null : publication.state().name());
        map.put("firstPublishedAt", publication.firstPublishedAt() == null
                ? null : publication.firstPublishedAt().toString());
        map.put("lastPublishedAt", publication.lastPublishedAt() == null
                ? null : publication.lastPublishedAt().toString());
        return map;
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
