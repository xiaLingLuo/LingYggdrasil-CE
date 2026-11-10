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
package im.xz.cn.database.dao;

import im.xz.cn.database.DatabaseManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserLogDao {
    private final DatabaseManager db;

    public UserLogDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(String userId, String action, String message, String createdAt) {
        db.executeUpdate("INSERT INTO user_logs (id, user_id, action, message, created_at) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), userId, action, message, createdAt);
    }

    public List<Map<String, Object>> findByUserId(String userId) {
        return db.executeQuery(
                "SELECT message, created_at FROM user_logs WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                userId);
    }

    public long totalBytes(String userId) {
        Map<String, Object> row = db.executeQuerySingle(
                "SELECT COALESCE(SUM(" + byteLengthExpr("message") + "), 0) AS total FROM user_logs WHERE user_id = ?",
                userId);
        if (row != null && row.get("total") != null) {
            return ((Number) row.get("total")).longValue();
        }
        return 0;
    }

    public void clear(String userId) {
        db.executeUpdate("DELETE FROM user_logs WHERE user_id = ?", userId);
    }

    public void deleteBefore(String userId, String cutoff) {
        db.executeUpdate("DELETE FROM user_logs WHERE user_id = ? AND created_at < ?", userId, cutoff);
    }

    public void deleteOldestToFit(String userId, long maxBytes) {
        String len = byteLengthExpr("message");
        String sql = "DELETE FROM user_logs WHERE user_id = ? AND id IN ("
                + " SELECT id FROM ("
                + "  SELECT id, SUM(" + len + ") OVER (ORDER BY created_at DESC, id DESC) AS cum"
                + "  FROM user_logs WHERE user_id = ?"
                + " ) t WHERE t.cum > ?"
                + ") AND id <> ("
                + " SELECT id FROM ("
                + "  SELECT id FROM user_logs WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT 1"
                + " ) x"
                + ")";
        db.executeUpdate(sql, userId, userId, maxBytes, userId);
    }

    private String byteLengthExpr(String column) {
        String type = db.getDbType();
        if ("mysql".equalsIgnoreCase(type)) return "LENGTH(" + column + ")";
        if ("pgsql".equalsIgnoreCase(type)) return "OCTET_LENGTH(" + column + ")";
        return "LENGTH(CAST(" + column + " AS BLOB))";
    }
}
