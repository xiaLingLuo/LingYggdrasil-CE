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
package im.xz.cn.common;

public final class ReferenceType {
    public static final String SELF = "self";
    public static final String PUBLIC = "public";
    public static final String FRIEND_PREFIX = "friend:";

    private ReferenceType() {}

    public static String friend(String friendCode) {
        return friendCode == null ? FRIEND_PREFIX : FRIEND_PREFIX + friendCode;
    }

    public static boolean isSelf(String type) {
        return type == null || type.isBlank() || SELF.equalsIgnoreCase(type);
    }

    public static boolean isPublic(String type) {
        return PUBLIC.equalsIgnoreCase(type);
    }

    public static boolean isFriend(String type) {
        return type != null && type.regionMatches(true, 0, FRIEND_PREFIX, 0, FRIEND_PREFIX.length());
    }

    public static String friendCode(String type) {
        return isFriend(type) ? type.substring(FRIEND_PREFIX.length()) : null;
    }
}
