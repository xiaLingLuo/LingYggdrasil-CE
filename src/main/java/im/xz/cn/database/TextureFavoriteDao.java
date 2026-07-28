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
package im.xz.cn.database;

import im.xz.cn.util.TimeUtil;

import java.util.*;

public class TextureFavoriteDao {
    private final DatabaseManager db;

    public TextureFavoriteDao(DatabaseManager db) {
        this.db = db;
    }

    public boolean exists(String userId, String textureId) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM texture_favorites WHERE user_id = ? AND texture_id = ?", userId, textureId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public void favorite(String userId, String textureId, String alias) {
        try {
            String id = UUID.randomUUID().toString();
            String now = TimeUtil.now();
            db.executeUpdate("INSERT INTO texture_favorites (id, user_id, texture_id, alias, created_at) VALUES (?, ?, ?, ?, ?)", id, userId, textureId, alias, now);
        } catch (Exception ignored) {
            System.err.println("[TextureFavoriteDao] favorite failed (likely duplicate): " + ignored.getMessage());
        }
    }

    public void unfavorite(String userId, String textureId) {
        db.executeUpdate("DELETE FROM texture_favorites WHERE user_id = ? AND texture_id = ?", userId, textureId);
    }

    public void updateAlias(String userId, String textureId, String alias) {
        db.executeUpdate("UPDATE texture_favorites SET alias = ? WHERE user_id = ? AND texture_id = ?", alias, userId, textureId);
    }

    public List<Map<String, Object>> findByUserId(String userId) {
        return db.executeQuery(
            "SELECT f.*, t.type, t.hash, t.original_name, t.size, t.content_type, t.alias AS texture_alias, u.nickname AS owner_nickname, u.username AS owner_username " +
            "FROM texture_favorites f " +
            "JOIN textures t ON f.texture_id = t.id " +
            "LEFT JOIN users u ON t.user_id = u.id " +
            "WHERE f.user_id = ? ORDER BY f.created_at DESC", userId);
    }

    public int countByUserId(String userId) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM texture_favorites WHERE user_id = ?", userId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    public Set<String> findFavoriteTextureIds(String userId) {
        var rows = db.executeQuery("SELECT texture_id FROM texture_favorites WHERE user_id = ?", userId);
        Set<String> ids = new HashSet<>();
        for (var row : rows) {
            ids.add(String.valueOf(row.get("texture_id")));
        }
        return ids;
    }
}
