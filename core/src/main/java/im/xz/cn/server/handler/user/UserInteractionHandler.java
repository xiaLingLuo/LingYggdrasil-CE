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
import im.xz.cn.infrastructure.persistence.UnitOfWork;
import im.xz.cn.texture.interaction.InteractionCounts;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.interaction.InteractionService;
import im.xz.cn.texture.publication.PublicationRepository;
import io.javalin.http.Context;

import java.util.Map;


public class UserInteractionHandler {

    private final InteractionService interactions;

    public UserInteractionHandler(DatabaseManager database) {
        this.interactions = new InteractionService(new UnitOfWork(database),
                new PublicationRepository(database), new InteractionRepository(database));
    }

    public void like(Context ctx) {
        handle(ctx, false, false);
    }

    public void unlike(Context ctx) {
        handle(ctx, false, true);
    }

    public void favorite(Context ctx) {
        handle(ctx, true, false);
    }

    public void unfavorite(Context ctx) {
        handle(ctx, true, true);
    }

    private void handle(Context ctx, boolean favorite, boolean remove) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED",
                    "message", "Login required"));
            return;
        }
        long publicationId;
        try {
            publicationId = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid publication id"));
            return;
        }
        try {
            InteractionCounts counts = toggle(favorite, remove, userId, publicationId);
            ctx.json(Map.of("success", true,
                    "likes", counts.likes(),
                    "favorites", counts.favorites()));
        } catch (IllegalArgumentException | IllegalStateException error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", error.getMessage() == null ? "Invalid interaction" : error.getMessage()));
        }
    }

    private InteractionCounts toggle(boolean favorite, boolean remove, String userId, long publicationId) {
        if (favorite) {
            return remove ? interactions.unfavorite(userId, publicationId)
                    : interactions.favorite(userId, publicationId);
        }
        return remove ? interactions.unlike(userId, publicationId)
                : interactions.like(userId, publicationId);
    }
}
