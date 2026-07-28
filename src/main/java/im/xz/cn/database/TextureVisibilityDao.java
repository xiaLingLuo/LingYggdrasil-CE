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

public class TextureVisibilityDao {
    private final DatabaseManager db;

    public TextureVisibilityDao(DatabaseManager db) {
        this.db = db;
    }

    public boolean isPublic(String userId, String textureId) {
        var row = db.executeQuerySingle("SELECT is_public FROM texture_visibility WHERE user_id = ? AND texture_id = ?", userId, textureId);
        if (row != null && row.get("is_public") != null) {
            Object val = row.get("is_public");
            if (val instanceof Boolean b) return b;
            if (val instanceof Number n) return n.intValue() == 1;
            return "1".equals(String.valueOf(val)) || "true".equalsIgnoreCase(String.valueOf(val));
        }
        return false;
    }

    public void setVisibility(String userId, String textureId, boolean isPublic) {
        var existing = db.executeQuerySingle("SELECT id FROM texture_visibility WHERE user_id = ? AND texture_id = ?", userId, textureId);
        if (existing != null) {
            db.executeUpdate("UPDATE texture_visibility SET is_public = ? WHERE user_id = ? AND texture_id = ?", isPublic ? 1 : 0, userId, textureId);
        } else {
            try {
                String id = UUID.randomUUID().toString();
                String now = TimeUtil.now();
                db.executeUpdate("INSERT INTO texture_visibility (id, user_id, texture_id, is_public, created_at) VALUES (?, ?, ?, ?, ?)", id, userId, textureId, isPublic ? 1 : 0, now);
            } catch (Exception e) {
                db.executeUpdate("UPDATE texture_visibility SET is_public = ? WHERE user_id = ? AND texture_id = ?", isPublic ? 1 : 0, userId, textureId);
            }
        }
    }

    public Map<String, Boolean> batchGetVisibility(String userId, List<String> textureIds) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (textureIds == null || textureIds.isEmpty()) return result;
        for (String tid : textureIds) result.put(tid, false);
        int batchSize = 500;
        for (int offset = 0; offset < textureIds.size(); offset += batchSize) {
            int end = Math.min(offset + batchSize, textureIds.size());
            List<String> batch = textureIds.subList(offset, end);
            var placeholders = new StringBuilder();
            for (int i = 0; i < batch.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            Object[] params = new Object[batch.size() + 1];
            params[0] = userId;
            for (int i = 0; i < batch.size(); i++) params[i + 1] = batch.get(i);
            var rows = db.executeQuery("SELECT texture_id, is_public FROM texture_visibility WHERE user_id = ? AND texture_id IN (" + placeholders + ")", params);
            for (var row : rows) {
                String tid = String.valueOf(row.get("texture_id"));
                Object val = row.get("is_public");
                boolean pub = false;
                if (val instanceof Boolean b) pub = b;
                else if (val instanceof Number n) pub = n.intValue() == 1;
                else pub = "1".equals(String.valueOf(val)) || "true".equalsIgnoreCase(String.valueOf(val));
                result.put(tid, pub);
            }
        }
        return result;
    }
}
