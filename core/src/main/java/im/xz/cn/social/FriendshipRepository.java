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
package im.xz.cn.social;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public final class FriendshipRepository {

    private final DatabaseManager database;

    public FriendshipRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean areFriends(String a, String b) {
        String[] pair = ordered(a, b);
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                pair[0], pair[1]) > 0;
    }

    
    public List<String> friendsOf(String userId) {
        List<String> result = new ArrayList<>();
        Jdbc.queryMany(database, resultSet -> new String[]{
                        resultSet.getString("user_low_id"), resultSet.getString("user_high_id")},
                "SELECT user_low_id, user_high_id FROM friendships"
                        + " WHERE user_low_id = ? OR user_high_id = ? ORDER BY created_at",
                userId, userId).forEach(pair ->
                result.add(pair[0].equals(userId) ? pair[1] : pair[0]));
        return List.copyOf(result);
    }

    public void add(String a, String b, Instant now) {
        String[] pair = ordered(a, b);
        if (areFriends(a, b)) {
            return;
        }
        Jdbc.update(database,
                "INSERT INTO friendships (user_low_id, user_high_id, created_at) VALUES (?, ?, ?)",
                pair[0], pair[1], now.toString());
    }

    public boolean remove(String a, String b) {
        String[] pair = ordered(a, b);
        return Jdbc.update(database,
                "DELETE FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                pair[0], pair[1]) > 0;
    }

    private static String[] ordered(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            throw new IllegalArgumentException("both users are required");
        }
        if (a.equals(b)) {
            throw new IllegalArgumentException("cannot be friends with yourself");
        }
        return a.compareTo(b) <= 0 ? new String[]{a, b} : new String[]{b, a};
    }
}
