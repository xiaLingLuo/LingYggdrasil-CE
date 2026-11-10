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
package im.xz.cn.permission;

public record PermissionNode(
        String key,
        PermissionType type,
        String category,
        String source,
        String description,
        boolean highRisk
) {
    public PermissionNode {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("permission key must not be blank");
        }
        if (type == null) type = PermissionType.ADMIN;
        if (source == null || source.isBlank()) source = "Unknown";
    }

    public static PermissionNode admin(String key, String category) {
        return new PermissionNode(key, PermissionType.ADMIN, category, "LingYggdrasil", null, false);
    }

    public static PermissionNode admin(String key, String category, boolean highRisk) {
        return new PermissionNode(key, PermissionType.ADMIN, category, "LingYggdrasil", null, highRisk);
    }

    public static PermissionNode user(String key, String category) {
        return new PermissionNode(key, PermissionType.USER, category, "LingYggdrasil", null, false);
    }
}
