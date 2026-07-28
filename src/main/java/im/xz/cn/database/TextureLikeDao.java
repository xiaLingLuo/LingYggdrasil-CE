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

import java.util.UUID;

public class TextureLikeDao {
    private final DatabaseManager db;

    public TextureLikeDao(DatabaseManager db) {
        this.db = db;
    }

    public boolean exists(String userId, String textureId) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM texture_likes WHERE user_id = ? AND texture_id = ?", userId, textureId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public void like(String userId, String textureId) {
        try {
            String id = UUID.randomUUID().toString();
            String now = TimeUtil.now();
            db.executeUpdate("INSERT INTO texture_likes (id, user_id, texture_id, created_at) VALUES (?, ?, ?, ?)", id, userId, textureId, now);
        } catch (Exception ignored) {
            System.err.println("[TextureLikeDao] like failed (likely duplicate): " + ignored.getMessage());
        }
    }

    public void unlike(String userId, String textureId) {
        db.executeUpdate("DELETE FROM texture_likes WHERE user_id = ? AND texture_id = ?", userId, textureId);
    }

    public int countByTexture(String textureId) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM texture_likes WHERE texture_id = ?", textureId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    public java.util.Map<String, Integer> batchCountByTextures(java.util.List<String> textureIds) {
        java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
        if (textureIds == null || textureIds.isEmpty()) return result;
        int batchSize = 500;
        for (int offset = 0; offset < textureIds.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, textureIds.size());
            java.util.List<String> batch = textureIds.subList(offset, end);
            var placeholders = new StringBuilder();
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            var rows = db.executeQuery("SELECT texture_id, COUNT(*) AS cnt FROM texture_likes WHERE texture_id IN (" + placeholders + ") GROUP BY texture_id", batch.toArray());
            for (var row : rows) {
                result.put(String.valueOf(row.get("texture_id")), ((Number) row.get("cnt")).intValue());
            }
        }
        return result;
    }
}
