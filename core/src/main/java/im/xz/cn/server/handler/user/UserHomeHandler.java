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

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.library.LibraryEntryRepository;
import im.xz.cn.texture.publication.PublicProfileQueryService;
import im.xz.cn.texture.publication.PublicTextureSummary;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.social.SocialIdentity;
import im.xz.cn.social.SocialIdentityService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class UserHomeHandler {

    private final SocialIdentityService identities;
    private final PublicProfileQueryService publicQuery;

    public UserHomeHandler(DatabaseManager database, Function<String, String> skinUrlResolver) {
        this.identities = new SocialIdentityService(database, skinUrlResolver);
        this.publicQuery = new PublicProfileQueryService(
                new PublicationRepository(database),
                new TextureAssetRepository(database),
                new TextureBlobRepository(database),
                new LibraryEntryRepository(database),
                new InteractionRepository(database));
    }

    public void pageProfile(Context ctx) {
        SocialIdentity identity = identities.findByCode(ctx.pathParam("code")).orElse(null);
        if (identity == null) {
            im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
            return;
        }
        ctx.html(im.xz.cn.web.view.UserHomePage.renderProfile(
                im.xz.cn.auth.SessionManager.getOrCreateCsrfToken(ctx),
                im.xz.cn.config.SystemConfig.getInstance().getSiteName(),
                identity,
                publicQuery.listPublic(identity.userId(), 60, 0),
                UserHomeHandler::textureUrl));
    }

    public void pagePublication(Context ctx) {
        SocialIdentity identity = identities.findByCode(ctx.pathParam("code")).orElse(null);
        if (identity == null) {
            im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
            return;
        }
        TextureType type = null;
        String typeParam = ctx.queryParam("type");
        if (typeParam != null && !typeParam.isBlank()) {
            try {
                type = TextureType.parse(typeParam);
            } catch (IllegalArgumentException ignored) {
                im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
                return;
            }
        }
        PublicTextureSummary summary = publicQuery
                .findPublic(identity.userId(), ctx.pathParam("hash"), type)
                .orElse(null);
        if (summary == null) {
            im.xz.cn.web.ErrorPageHandler.html(ctx, 404);
            return;
        }
        ctx.html(im.xz.cn.web.view.UserHomePage.renderPublication(
                im.xz.cn.auth.SessionManager.getOrCreateCsrfToken(ctx),
                im.xz.cn.config.SystemConfig.getInstance().getSiteName(),
                identity, summary, textureUrl(summary.type().name() + "/" + summary.hash())));
    }

    
    private static String textureUrl(String scope) {
        if (scope == null) {
            return null;
        }
        int slash = scope.indexOf('/');
        if (slash <= 0 || slash >= scope.length() - 1) {
            return null;
        }
        return im.xz.cn.texture.UserTextureUrlResolver.url(
                scope.substring(0, slash), scope.substring(slash + 1));
    }

    public void getIdentity(Context ctx) {
        String code = ctx.pathParam("code");
        SocialIdentity identity = identities.findByCode(code).orElse(null);
        if (identity == null) {
            notFound(ctx, "USERHOME_NOT_FOUND", "User not found");
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("userId", identity.userId());
        body.put("userCode", identity.userCode());
        body.put("displayName", identity.displayName());
        body.put("avatarUrl", identity.avatarUrl());
        body.put("canBeFollowed", identity.canBeFollowed());
        body.put("cancelled", identity.cancelled());
        ctx.json(body);
    }

    public void listPublications(Context ctx) {
        SocialIdentity identity = identities.findByCode(ctx.pathParam("code")).orElse(null);
        if (identity == null) {
            notFound(ctx, "USERHOME_NOT_FOUND", "User not found");
            return;
        }
        int limit = parseInt(ctx.queryParam("limit"), 20);
        int offset = parseInt(ctx.queryParam("offset"), 0);
        List<Map<String, Object>> items = new ArrayList<>();
        for (PublicTextureSummary summary : publicQuery.listPublic(identity.userId(), limit, offset)) {
            items.add(toMap(summary));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("userCode", identity.userCode());
        body.put("items", items);
        ctx.json(body);
    }

    public void getPublication(Context ctx) {
        SocialIdentity identity = identities.findByCode(ctx.pathParam("code")).orElse(null);
        if (identity == null) {
            notFound(ctx, "USERHOME_NOT_FOUND", "User not found");
            return;
        }
        TextureType type = null;
        String typeParam = ctx.queryParam("type");
        if (typeParam != null && !typeParam.isBlank()) {
            try {
                type = TextureType.parse(typeParam);
            } catch (IllegalArgumentException error) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "code", "INVALID_TEXTURE_TYPE",
                        "message", "Unknown texture type"));
                return;
            }
        }
        PublicTextureSummary summary = publicQuery
                .findPublic(identity.userId(), ctx.pathParam("hash"), type)
                .orElse(null);
        if (summary == null) {
            notFound(ctx, "PUBLICATION_NOT_FOUND", "Publication not found");
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("userCode", identity.userCode());
        body.put("publication", toMap(summary));
        ctx.json(body);
    }

    private static Map<String, Object> toMap(PublicTextureSummary summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("publicationId", summary.publicationId());
        item.put("assetId", summary.assetId());
        item.put("type", summary.type().name());
        item.put("hash", summary.hash());
        item.put("name", summary.displayName());
        item.put("author", summary.authorName());
        item.put("likes", summary.likes());
        item.put("favorites", summary.favorites());
        item.put("heat", summary.heat());
        item.put("firstPublishedAt", summary.firstPublishedAt() == null
                ? null : summary.firstPublishedAt().toString());
        item.put("lastPublishedAt", summary.lastPublishedAt() == null
                ? null : summary.lastPublishedAt().toString());
        return item;
    }

    private static void notFound(Context ctx, String code, String message) {
        ctx.status(404);
        ctx.json(Map.of("success", false, "code", code, "message", message));
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
}
