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

public class BlockDao {
    private final DatabaseManager db;

    public BlockDao(DatabaseManager db) {
        this.db = db;
    }

    public void block(String blockerId, String blockedId) {
        if (isBlocked(blockerId, blockedId)) return;
        String id = UUID.randomUUID().toString();
        String now = TimeUtil.now();
        db.executeUpdate(
                "INSERT INTO blocked_users (id, blocker_id, blocked_id, created_at) VALUES (?, ?, ?, ?)",
                id, blockerId, blockedId, now);
    }

    public void unblock(String blockerId, String blockedId) {
        db.executeUpdate(
                "DELETE FROM blocked_users WHERE blocker_id = ? AND blocked_id = ?",
                blockerId, blockedId);
    }

    public boolean isBlocked(String blockerId, String blockedId) {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM blocked_users WHERE blocker_id = ? AND blocked_id = ?",
                blockerId, blockedId);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public int countByBlocker(String blockerId) {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM blocked_users WHERE blocker_id = ?", blockerId);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public List<Map<String, Object>> findByBlocker(String blockerId) {
        String sql = """
            SELECT bu.blocked_id, u.friend_code
            FROM blocked_users bu
            JOIN users u ON bu.blocked_id = u.id
            WHERE bu.blocker_id = ?
            ORDER BY bu.created_at ASC
            """;
        var rows = db.executeQuery(sql, blockerId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("blockedId", String.valueOf(row.get("blocked_id")));
            map.put("friendCode", row.get("friend_code"));
            list.add(map);
        }
        return list;
    }

    public void clearAll(String blockerId) {
        db.executeUpdate("DELETE FROM blocked_users WHERE blocker_id = ?", blockerId);
    }
}
