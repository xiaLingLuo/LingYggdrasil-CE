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


public record SocialIdentity(
        String userId,
        String userCode,
        String displayName,
        String avatarUrl,
        boolean canBeFollowed,
        boolean cancelled
) {

    public SocialIdentity {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (displayName == null) {
            displayName = "";
        }
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = SocialIdentityService.DEFAULT_AVATAR;
        }
    }
}
