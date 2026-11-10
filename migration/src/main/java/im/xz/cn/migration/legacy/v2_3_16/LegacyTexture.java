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
package im.xz.cn.migration.legacy.v2_3_16;


public record LegacyTexture(
        String id,
        String userId,
        String type,
        String hash,
        String alias,
        String originalName,
        Long size,
        String contentType,
        String createdAt,
        String referenceType,
        String refOwnerId
) {

    public boolean selfOwned() {
        return referenceType == null || referenceType.isBlank() || "self".equals(referenceType);
    }

    public boolean friendReference() {
        return referenceType != null && referenceType.startsWith("friend");
    }
}
