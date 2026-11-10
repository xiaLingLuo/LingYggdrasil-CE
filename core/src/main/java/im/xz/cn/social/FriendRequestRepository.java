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
import java.util.Optional;


public final class FriendRequestRepository {

    public record FriendRequest(long id, String fromUserId, String toUserId, String state,
                                Instant createdAt) {
    }

    private final DatabaseManager database;

    public FriendRequestRepository(DatabaseManager database) {
        this.database = database;
    }

    
    public FriendRequest createPending(String fromUserId, String toUserId, Instant now) {
        if (fromUserId == null || toUserId == null || fromUserId.isBlank() || toUserId.isBlank()) {
            throw new IllegalArgumentException("both users are required");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("cannot request yourself");
        }
        Optional<FriendRequest> existing = findPending(fromUserId, toUserId);
        if (existing.isPresent()) {
            return existing.get();
        }
        long id = Jdbc.insert(database,
                "INSERT INTO friend_requests (from_user_id, to_user_id, state, created_at)"
                        + " VALUES (?, ?, 'PENDING', ?)", fromUserId, toUserId, now.toString());
        return new FriendRequest(id, fromUserId, toUserId, "PENDING", now);
    }

    public Optional<FriendRequest> findPending(String fromUserId, String toUserId) {
        return Optional.ofNullable(Jdbc.queryOne(database, FriendRequestRepository::map,
                "SELECT * FROM friend_requests WHERE from_user_id = ? AND to_user_id = ?"
                        + " AND state = 'PENDING' ORDER BY id LIMIT 1", fromUserId, toUserId));
    }

    public Optional<FriendRequest> find(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, FriendRequestRepository::map,
                "SELECT * FROM friend_requests WHERE id = ?", id));
    }

    public List<FriendRequest> pendingFor(String toUserId) {
        return Jdbc.queryMany(database, FriendRequestRepository::map,
                "SELECT * FROM friend_requests WHERE to_user_id = ? AND state = 'PENDING'"
                        + " ORDER BY created_at, id", toUserId);
    }

    public List<FriendRequest> pendingFrom(String fromUserId) {
        return Jdbc.queryMany(database, FriendRequestRepository::map,
                "SELECT * FROM friend_requests WHERE from_user_id = ? AND state = 'PENDING'"
                        + " ORDER BY created_at, id", fromUserId);
    }

    
    public boolean cancel(long id, String fromUserId) {
        return Jdbc.update(database,
                "UPDATE friend_requests SET state = 'CANCELLED'"
                        + " WHERE id = ? AND from_user_id = ? AND state = 'PENDING'",
                id, fromUserId) > 0;
    }

    
    public int cancelBetween(String userA, String userB) {
        return Jdbc.update(database,
                "UPDATE friend_requests SET state = 'CANCELLED' WHERE state = 'PENDING'"
                        + " AND ((from_user_id = ? AND to_user_id = ?)"
                        + " OR (from_user_id = ? AND to_user_id = ?))",
                userA, userB, userB, userA);
    }

    
    public boolean markAccepted(long id, String recipientId) {
        return Jdbc.update(database,
                "UPDATE friend_requests SET state = 'ACCEPTED'"
                        + " WHERE id = ? AND to_user_id = ? AND state = 'PENDING'",
                id, recipientId) > 0;
    }

    public boolean markRejected(long id, String recipientId) {
        return Jdbc.update(database,
                "UPDATE friend_requests SET state = 'REJECTED'"
                        + " WHERE id = ? AND to_user_id = ? AND state = 'PENDING'",
                id, recipientId) > 0;
    }

    private static FriendRequest map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new FriendRequest(
                resultSet.getLong("id"),
                resultSet.getString("from_user_id"),
                resultSet.getString("to_user_id"),
                resultSet.getString("state"),
                Jdbc.readInstant(resultSet, "created_at"));
    }
}
