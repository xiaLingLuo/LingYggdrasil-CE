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
import im.xz.cn.logging.logApi;
import im.xz.cn.common.TimeUtil;

import java.util.*;

public class FriendSharedTextureDao {
    private static final logApi log = logApi.getLogger(FriendSharedTextureDao.class);
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
        } catch (RuntimeException e) {
            if (!DatabaseManager.isDuplicateKeyViolation(e)) throw e;
            log.warn("[FriendSharedTextureDao] share failed (likely duplicate): {}", e.getMessage(), e);
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

    public List<Map<String, Object>> findOutgoing(String ownerId) {
        return db.executeQuery(
            "SELECT fst.texture_id, fst.friend_id, fst.created_at, t.user_id AS owner_user_id, t.type, t.hash, t.alias AS texture_alias, t.original_name, t.size, " +
            "u.nickname AS friend_nickname, u.username AS friend_username, u.friend_code AS friend_code " +
            "FROM friend_shared_textures fst " +
            "JOIN textures t ON fst.texture_id = t.id " +
            "LEFT JOIN users u ON fst.friend_id = u.id " +
            "WHERE fst.owner_id = ? ORDER BY fst.created_at DESC", ownerId);
    }

    public List<Map<String, Object>> findIncoming(String friendId) {
        return db.executeQuery(
            "SELECT fst.texture_id, fst.created_at, t.type, t.hash, t.alias AS texture_alias, t.original_name, t.size, " +
            "rt.alias AS receiver_alias, " +
            "u.nickname AS owner_nickname, u.username AS owner_username, u.friend_code AS owner_friend_code " +
            "FROM friend_shared_textures fst " +
            "JOIN textures t ON fst.texture_id = t.id " +
            "LEFT JOIN textures rt ON rt.user_id = fst.friend_id AND rt.type = t.type AND rt.hash = t.hash " +
            "LEFT JOIN users u ON fst.owner_id = u.id " +
            "WHERE fst.friend_id = ? ORDER BY fst.created_at DESC", friendId);
    }

    public void deleteBetween(String userA, String userB) {
        db.executeUpdate(
            "DELETE FROM friend_shared_textures WHERE (owner_id = ? AND friend_id = ?) OR (owner_id = ? AND friend_id = ?)",
            userA, userB, userB, userA);
    }

    public void deleteByOwnerAndTexture(String ownerId, String textureId) {
        db.executeUpdate("DELETE FROM friend_shared_textures WHERE owner_id = ? AND texture_id = ?", ownerId, textureId);
    }

    public void deleteByFriendAndTexture(String friendId, String textureId) {
        db.executeUpdate("DELETE FROM friend_shared_textures WHERE friend_id = ? AND texture_id = ?", friendId, textureId);
    }
}
