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

public class ConfirmingFriendDao {
    private final DatabaseManager db;

    public ConfirmingFriendDao(DatabaseManager db) {
        this.db = db;
    }

    public void createRequest(String senderId, String receiverId) {
        String id = UUID.randomUUID().toString();
        String now = TimeUtil.now();
        db.executeUpdate(
                "INSERT INTO confirming_friends (id, sender_id, receiver_id, created_at) VALUES (?, ?, ?, ?)",
                id, senderId, receiverId, now);
    }

    public Map<String, Object> findById(String requestId) {
        return db.executeQuerySingle("SELECT * FROM confirming_friends WHERE id = ?", requestId);
    }

    public boolean exists(String userA, String userB) {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM confirming_friends WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)",
                userA, userB, userB, userA);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public List<Map<String, Object>> findByUser(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT cf.id AS request_id, cf.sender_id, cf.receiver_id, cf.created_at,
                   u.id AS user_id, u.username, u.nickname, u.friend_code
            FROM confirming_friends cf
            JOIN users u ON (CASE WHEN cf.sender_id = ? THEN cf.receiver_id ELSE cf.sender_id END) = u.id
            WHERE (cf.sender_id = ? OR cf.receiver_id = ?)
            ORDER BY cf.created_at ASC
            """;
        var rows = db.executeQuery(sql, userId, userId, userId);
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("requestId", String.valueOf(row.get("request_id")));
            map.put("senderId", String.valueOf(row.get("sender_id")));
            map.put("receiverId", String.valueOf(row.get("receiver_id")));
            map.put("createdAt", String.valueOf(row.get("created_at")));
            map.put("userId", String.valueOf(row.get("user_id")));
            map.put("username", row.get("username"));
            map.put("nickname", row.get("nickname"));
            map.put("friendCode", row.get("friend_code"));
            list.add(map);
        }
        return list;
    }

    public void deleteById(String requestId) {
        db.executeUpdate("DELETE FROM confirming_friends WHERE id = ?", requestId);
    }

    public void deleteExpired() {
        String cutoff = TimeUtil.plusSeconds(-7L * 24 * 3600);
        db.executeUpdate("DELETE FROM confirming_friends WHERE created_at < ?", cutoff);
    }

    public int countByUser(String userId) {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM confirming_friends WHERE sender_id = ? OR receiver_id = ?",
                userId, userId);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }
}
