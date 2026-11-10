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
package im.xz.cn.texture.asset;

import java.time.Instant;


public record TextureBlob(
        long id,
        String sha256,
        long sizeBytes,
        String contentType,
        String storageKey,
        BlobState state,
        Instant createdAt
) {

    public TextureBlob {
        if (sha256 == null || sha256.length() != 64) {
            throw new IllegalArgumentException("sha256 must be 64 hex characters");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be >= 0");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
    }

    public static TextureBlob newBlob(String sha256, long sizeBytes, String contentType,
                                      String storageKey, Instant createdAt) {
        return new TextureBlob(0L, sha256, sizeBytes, contentType, storageKey, BlobState.STAGING, createdAt);
    }
}
