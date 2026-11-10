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
package im.xz.cn.texture.sharing;

import im.xz.cn.infrastructure.persistence.UnitOfWork;
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;

import java.time.Clock;
import java.time.Instant;


public final class TextureSharingService {

    
    @FunctionalInterface
    public interface FriendshipPolicy {
        boolean areFriends(String userA, String userB);
    }

    private final UnitOfWork unitOfWork;
    private final TextureUploadRepository uploads;
    private final ShareGrantRepository grants;
    private final FriendshipPolicy friendships;
    private final Clock clock;

    public TextureSharingService(UnitOfWork unitOfWork,
                                 TextureUploadRepository uploads,
                                 ShareGrantRepository grants,
                                 FriendshipPolicy friendships) {
        this(unitOfWork, uploads, grants, friendships, Clock.systemUTC());
    }

    public TextureSharingService(UnitOfWork unitOfWork,
                                 TextureUploadRepository uploads,
                                 ShareGrantRepository grants,
                                 FriendshipPolicy friendships,
                                 Clock clock) {
        this.unitOfWork = unitOfWork;
        this.uploads = uploads;
        this.grants = grants;
        this.friendships = friendships;
        this.clock = clock;
    }

    public TextureShareGrant share(String ownerId, String recipientId, long assetId) {
        if (ownerId.equals(recipientId)) {
            throw new IllegalArgumentException("cannot share with yourself");
        }
        if (!uploads.hasActiveUpload(ownerId, assetId)) {
            throw new IllegalStateException("only own active uploads can be shared");
        }
        if (!friendships.areFriends(ownerId, recipientId)) {
            throw new SecurityException("share target is not a friend");
        }
        TextureUpload upload = uploads.findByUserAndAsset(ownerId, assetId).orElseThrow();
        Instant now = clock.instant();
        return unitOfWork.execute(connection ->
                grants.create(upload.id(), ownerId, recipientId, assetId, now));
    }

    public TextureShareGrant revoke(String ownerId, long grantId) {
        Instant now = clock.instant();
        return unitOfWork.execute(connection -> grants.revokeByOwner(ownerId, grantId, now));
    }

    public TextureShareGrant receiverRemove(String recipientId, long grantId) {
        Instant now = clock.instant();
        return unitOfWork.execute(connection -> grants.removeByRecipient(recipientId, grantId, now));
    }
}
