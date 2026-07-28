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

public class FriendSharedTextureDao {
    private final DatabaseManager db;

    public FriendSharedTextureDao(DatabaseManager db) {
        this.db = db;
    }

    public boolean exists(String ownerId, String friendId, String textureId) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM friend_shared_textures WHERE owner_id = ? AND friend_id = ? AND texture_id = ?", ownerId, friendId, textureId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue() > 0;
        }
        return false;
    }

    public void share(String ownerId, String friendId, String textureId) {
        try {
            String id = UUID.randomUUID().toString();
            String now = TimeUtil.now();
            db.executeUpdate("INSERT INTO friend_shared_textures (id, owner_id, friend_id, texture_id, created_at) VALUES (?, ?, ?, ?, ?)", id, ownerId, friendId, textureId, now);
        } catch (Exception ignored) {
            System.err.println("[FriendSharedTextureDao] share failed (likely duplicate): " + ignored.getMessage());
        }
    }

    public void unshare(String ownerId, String friendId, String textureId) {
        db.executeUpdate("DELETE FROM friend_shared_textures WHERE owner_id = ? AND friend_id = ? AND texture_id = ?", ownerId, friendId, textureId);
    }

    public List<Map<String, Object>> findSharedToFriend(String ownerId, String friendId) {
        return db.executeQuery(
            "SELECT fst.*, t.type, t.hash, t.original_name, t.size, t.content_type, t.alias AS texture_alias " +
            "FROM friend_shared_textures fst " +
            "JOIN textures t ON fst.texture_id = t.id " +
            "WHERE fst.owner_id = ? AND fst.friend_id = ? ORDER BY fst.created_at DESC", ownerId, friendId);
    }

    public List<Map<String, Object>> findSharedByFriends(String userId) {
        return db.executeQuery(
            "SELECT fst.*, t.type, t.hash, t.original_name, t.size, t.content_type, t.alias AS texture_alias, " +
            "u.nickname AS owner_nickname, u.username AS owner_username " +
            "FROM friend_shared_textures fst " +
            "JOIN textures t ON fst.texture_id = t.id " +
            "LEFT JOIN users u ON fst.owner_id = u.id " +
            "WHERE fst.friend_id = ? ORDER BY fst.created_at DESC", userId);
    }

    public Set<String> findSharedTextureIdsByFriends(String userId) {
        var rows = db.executeQuery("SELECT texture_id FROM friend_shared_textures WHERE friend_id = ?", userId);
        Set<String> ids = new HashSet<>();
        for (var row : rows) {
            ids.add(String.valueOf(row.get("texture_id")));
        }
        return ids;
    }
}
