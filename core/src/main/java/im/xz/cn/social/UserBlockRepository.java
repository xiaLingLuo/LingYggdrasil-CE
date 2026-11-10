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
import java.util.List;


public final class UserBlockRepository {

    private final DatabaseManager database;

    public UserBlockRepository(DatabaseManager database) {
        this.database = database;
    }

    public void block(String blockerId, String blockedId, Instant now) {
        if (blockerId == null || blockedId == null || blockerId.isBlank() || blockedId.isBlank()) {
            throw new IllegalArgumentException("both users are required");
        }
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("cannot block yourself");
        }
        if (isBlocked(blockerId, blockedId)) {
            return;
        }
        Jdbc.update(database,
                "INSERT INTO user_blocks (blocker_id, blocked_id, created_at) VALUES (?, ?, ?)",
                blockerId, blockedId, now.toString());
    }

    public boolean unblock(String blockerId, String blockedId) {
        return Jdbc.update(database,
                "DELETE FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?",
                blockerId, blockedId) > 0;
    }

    public boolean isBlocked(String blockerId, String blockedId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?",
                blockerId, blockedId) > 0;
    }

    public List<String> blockedBy(String blockerId) {
        return Jdbc.queryMany(database, resultSet -> resultSet.getString("blocked_id"),
                "SELECT blocked_id FROM user_blocks WHERE blocker_id = ? ORDER BY created_at",
                blockerId);
    }

    public int deleteAll(String blockerId) {
        return Jdbc.update(database, "DELETE FROM user_blocks WHERE blocker_id = ?", blockerId);
    }

    public long countBy(String blockerId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM user_blocks WHERE blocker_id = ?", blockerId);
    }
}
