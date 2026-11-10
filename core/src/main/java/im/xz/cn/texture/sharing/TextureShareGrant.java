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

import java.time.Instant;


public record TextureShareGrant(
        long id,
        long ownerUploadId,
        String ownerId,
        String recipientId,
        long assetId,
        GrantState state,
        Instant createdAt,
        Instant endedAt
) {

    public TextureShareGrant {
        if (ownerId == null || ownerId.isBlank() || recipientId == null || recipientId.isBlank()) {
            throw new IllegalArgumentException("ownerId and recipientId are required");
        }
        if (ownerId.equals(recipientId)) {
            throw new IllegalArgumentException("cannot share with yourself");
        }
        if (assetId <= 0 || ownerUploadId <= 0) {
            throw new IllegalArgumentException("ownerUploadId and assetId are required");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
    }

    public boolean active() {
        return state.active();
    }
}
