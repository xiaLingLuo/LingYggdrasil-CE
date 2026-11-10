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
package im.xz.cn.texture.interaction;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;


public final class InteractionRepository {

    private final DatabaseManager database;

    public InteractionRepository(DatabaseManager database) {
        this.database = database;
    }

    
    public void like(String userId, long publicationId, java.time.Instant now) {
        if (hasLike(userId, publicationId)) {
            return;
        }
        try {
            Jdbc.update(database,
                    "INSERT INTO publication_likes (user_id, publication_id, created_at) VALUES (?, ?, ?)",
                    userId, publicationId, Jdbc.writeInstant(now));
        } catch (RuntimeException error) {
            if (!DatabaseManager.isDuplicateKeyViolation(error)) {
                throw error;
            }
        }
    }

    public void unlike(String userId, long publicationId) {
        Jdbc.update(database,
                "DELETE FROM publication_likes WHERE user_id = ? AND publication_id = ?",
                userId, publicationId);
    }

    public boolean hasLike(String userId, long publicationId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM publication_likes WHERE user_id = ? AND publication_id = ?",
                userId, publicationId) > 0;
    }

    
    public void favorite(String userId, long publicationId, java.time.Instant now) {
        if (hasFavorite(userId, publicationId)) {
            return;
        }
        try {
            Jdbc.update(database,
                    "INSERT INTO publication_favorites (user_id, publication_id, created_at) VALUES (?, ?, ?)",
                    userId, publicationId, Jdbc.writeInstant(now));
        } catch (RuntimeException error) {
            if (!DatabaseManager.isDuplicateKeyViolation(error)) {
                throw error;
            }
        }
    }

    public void unfavorite(String userId, long publicationId) {
        Jdbc.update(database,
                "DELETE FROM publication_favorites WHERE user_id = ? AND publication_id = ?",
                userId, publicationId);
    }

    public boolean hasFavorite(String userId, long publicationId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM publication_favorites WHERE user_id = ? AND publication_id = ?",
                userId, publicationId) > 0;
    }

    public long countLikes(long publicationId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM publication_likes WHERE publication_id = ?", publicationId);
    }

    public long countFavorites(long publicationId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM publication_favorites WHERE publication_id = ?", publicationId);
    }

    public InteractionCounts counts(long publicationId) {
        return new InteractionCounts(countLikes(publicationId), countFavorites(publicationId));
    }

    public long countFavoritesByUser(String userId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM publication_favorites WHERE user_id = ?", userId);
    }
}
