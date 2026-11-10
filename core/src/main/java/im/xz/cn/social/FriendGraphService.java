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
import im.xz.cn.infrastructure.persistence.UnitOfWork;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class FriendGraphService {

    public enum AcceptResult {
        ACCEPTED,
        NOT_FOUND,
        NOT_RECIPIENT,
        NOT_PENDING
    }

    private final UnitOfWork unitOfWork;
    private final FriendRequestRepository requests;
    private final FriendshipRepository friendships;
    private final UserBlockRepository blocks;
    private final DatabaseManager database;
    private final im.xz.cn.database.dao.UserDao users;
    private final im.xz.cn.database.dao.UserPermGroupDao groups;

    public FriendGraphService(DatabaseManager database) {
        this.database = database;
        this.users = new im.xz.cn.database.dao.UserDao(database);
        this.groups = new im.xz.cn.database.dao.UserPermGroupDao(database);
        this.unitOfWork = new UnitOfWork(database);
        this.requests = new FriendRequestRepository(database);
        this.friendships = new FriendshipRepository(database);
        this.blocks = new UserBlockRepository(database);
    }

    public FriendRequestRepository.FriendRequest sendRequest(String fromUserId, String toUserId) {
        if (blocks.isBlocked(fromUserId, toUserId) || blocks.isBlocked(toUserId, fromUserId)) {
            throw new IllegalArgumentException("blocked relationship");
        }
        if (friendships.areFriends(fromUserId, toUserId)) {
            throw new IllegalArgumentException("already friends");
        }
        try {
            var target = users.findById(toUserId);
            if (target != null && im.xz.cn.security.UserPermissions.isCancelled(groups, target)) {
                throw new IllegalArgumentException("target user cannot be followed");
            }
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (RuntimeException ignored) {
            
        }
        return requests.createPending(fromUserId, toUserId, Instant.now());
    }

    public AcceptResult accept(long requestId, String recipientId) {
        return unitOfWork.execute(connection -> {
            Optional<FriendRequestRepository.FriendRequest> found = requests.find(requestId);
            if (found.isEmpty()) {
                return AcceptResult.NOT_FOUND;
            }
            FriendRequestRepository.FriendRequest request = found.get();
            if (!request.toUserId().equals(recipientId)) {
                return AcceptResult.NOT_RECIPIENT;
            }
            if (blocks.isBlocked(request.fromUserId(), recipientId)
                    || blocks.isBlocked(recipientId, request.fromUserId())) {
                return AcceptResult.NOT_PENDING;
            }
            if (!requests.markAccepted(requestId, recipientId)) {
                return AcceptResult.NOT_PENDING;
            }
            friendships.add(request.fromUserId(), request.toUserId(), Instant.now());
            return AcceptResult.ACCEPTED;
        });
    }

    public boolean reject(long requestId, String recipientId) {
        return requests.markRejected(requestId, recipientId);
    }

    public boolean removeFriend(String userId, String otherUserId) {
        boolean removed = friendships.remove(userId, otherUserId);
        revokeGrantsBetween(userId, otherUserId);
        return removed;
    }

    
    public int revokeGrantsBetween(String userA, String userB) {
        var lifecycle = new im.xz.cn.texture.access.TextureAccessLifecycle(database);
        java.util.List<Long> assetIds = im.xz.cn.infrastructure.persistence.Jdbc.queryMany(database,
                rs -> rs.getLong("asset_id"),
                "SELECT DISTINCT asset_id FROM texture_share_grants"
                        + " WHERE state = 'ACTIVE' AND ((owner_id = ? AND recipient_id = ?)"
                        + " OR (owner_id = ? AND recipient_id = ?))",
                userA, userB, userB, userA);
        int revoked = im.xz.cn.infrastructure.persistence.Jdbc.update(database,
                "UPDATE texture_share_grants SET state = 'OWNER_REVOKED', ended_at = ?"
                        + " WHERE state = 'ACTIVE' AND ((owner_id = ? AND recipient_id = ?)"
                        + " OR (owner_id = ? AND recipient_id = ?))",
                im.xz.cn.infrastructure.persistence.Jdbc.writeInstant(Instant.now()),
                userA, userB, userB, userA);
        for (Long assetId : assetIds) {
            lifecycle.reconcileAsset(assetId, "FRIENDSHIP_ENDED");
        }
        return revoked;
    }

    public List<String> friends(String userId) {
        return friendships.friendsOf(userId);
    }

    public void block(String userId, String otherUserId) {
        blocks.block(userId, otherUserId, Instant.now());
        
        friendships.remove(userId, otherUserId);
        requests.cancelBetween(userId, otherUserId);
        revokeGrantsBetween(userId, otherUserId);
    }

    public boolean unblock(String userId, String otherUserId) {
        return blocks.unblock(userId, otherUserId);
    }

    public List<String> blocked(String userId) {
        return blocks.blockedBy(userId);
    }

    public int clearBlocked(String userId) {
        return blocks.deleteAll(userId);
    }

    public List<FriendRequestRepository.FriendRequest> incoming(String userId) {
        return requests.pendingFor(userId);
    }

    public List<FriendRequestRepository.FriendRequest> outgoing(String userId) {
        return requests.pendingFrom(userId);
    }

    public boolean cancelRequest(long requestId, String senderId) {
        return requests.cancel(requestId, senderId);
    }
}
