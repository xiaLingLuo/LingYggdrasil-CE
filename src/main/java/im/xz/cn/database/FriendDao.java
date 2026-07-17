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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class FriendDao {
    private final DatabaseManager db;

    public FriendDao(DatabaseManager db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByUserId(String userId) {
        List<Map<String, Object>> friends = new ArrayList<>();
        String sql = """
            SELECT u.id AS user_id, u.username, u.nickname, u.friend_code, u.display_profile_id,
                   p.id AS profile_id, p.name AS profile_name, p.skin_url, p.skin_model, p.cape_url
            FROM friends f
            JOIN users u ON (CASE WHEN f.user_id_lower = ? THEN f.user_id_higher ELSE f.user_id_lower END) = u.id
            LEFT JOIN player_profiles p ON u.display_profile_id = p.id
            WHERE ? IN (f.user_id_lower, f.user_id_higher)
            ORDER BY f.created_at ASC
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("userId", rs.getString("user_id"));
                    map.put("username", rs.getString("username"));
                    map.put("nickname", rs.getString("nickname"));
                    map.put("friendCode", rs.getString("friend_code"));
                    map.put("displayProfileId", rs.getString("display_profile_id"));
                    map.put("profileId", rs.getString("profile_id"));
                    map.put("profileName", rs.getString("profile_name"));
                    map.put("skinUrl", rs.getString("skin_url"));
                    map.put("skinModel", rs.getString("skin_model"));
                    map.put("capeUrl", rs.getString("cape_url"));
                    friends.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("FriendDao.findByUserId failed: " + e.getMessage());
        }
        return friends;
    }

    public boolean existsFriendship(String userA, String userB) {
        String lower = userA.compareTo(userB) < 0 ? userA : userB;
        String higher = userA.compareTo(userB) < 0 ? userB : userA;
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM friends WHERE user_id_lower = ? AND user_id_higher = ?",
                lower, higher);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public void addFriend(String userA, String userB) {
        String lower = userA.compareTo(userB) < 0 ? userA : userB;
        String higher = userA.compareTo(userB) < 0 ? userB : userA;
        String id = UUID.randomUUID().toString();
        String now = TimeUtil.now();
        db.executeUpdate(
                "INSERT INTO friends (id, user_id_lower, user_id_higher, created_at) VALUES (?, ?, ?, ?)",
                id, lower, higher, now);
    }

    public void deleteFriend(String userA, String userB) {
        String lower = userA.compareTo(userB) < 0 ? userA : userB;
        String higher = userA.compareTo(userB) < 0 ? userB : userA;
        db.executeUpdate(
                "DELETE FROM friends WHERE user_id_lower = ? AND user_id_higher = ?",
                lower, higher);
    }

    public int countByUserId(String userId) {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM friends WHERE user_id_lower = ? OR user_id_higher = ?",
                userId, userId);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }
}
