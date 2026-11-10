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
package im.xz.cn.texture.publication;

import java.time.Instant;


public record TexturePublication(
        long id,
        String publisherId,
        long assetId,
        PublicationState state,
        Instant firstPublishedAt,
        Instant lastPublishedAt,
        Instant withdrawnAt,
        long version
) {

    public TexturePublication {
        if (publisherId == null || publisherId.isBlank()) {
            throw new IllegalArgumentException("publisherId is required");
        }
        if (assetId <= 0) {
            throw new IllegalArgumentException("assetId is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
    }

    public boolean available() {
        return state.available();
    }

    public TexturePublication withState(PublicationState newState, Instant now, long newVersion) {
        Instant withdrawn = newState == PublicationState.PUBLIC ? null
                : (withdrawnAt != null ? withdrawnAt : now);
        Instant lastPublished = newState == PublicationState.PUBLIC ? now : lastPublishedAt;
        return new TexturePublication(id, publisherId, assetId, newState,
                firstPublishedAt, lastPublished, withdrawn, newVersion);
    }
}
