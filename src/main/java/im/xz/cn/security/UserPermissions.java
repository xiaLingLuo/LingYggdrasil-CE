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
package im.xz.cn.security;

import im.xz.cn.database.dao.UserPermGroupDao;
import im.xz.cn.model.User;
import im.xz.cn.permission.PermissionRegistry;
import im.xz.cn.permission.PermissionType;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UserPermissions {
    public static final String WILDCARD = "*";
    public static final String ACCESSIBLE = "user.accessible";

    private static final ThreadLocal<Set<String>> CURRENT = new ThreadLocal<>();

    private UserPermissions() {}

    public static Set<String> validKeys() {
        return PermissionRegistry.getInstance().validKeys(PermissionType.USER);
    }

    public static void set(Set<String> permissions) {
        CURRENT.set(permissions);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static boolean has(String key) {
        Set<String> p = CURRENT.get();
        return p != null && (p.contains(WILDCARD) || p.contains(key));
    }

    public static boolean isAccessible() {
        return has(ACCESSIBLE);
    }

    public static boolean isAccessible(UserPermGroupDao dao, User user) {
        if (dao == null || user == null) return false;
        var group = dao.findByName(user.getPermGroup());
        Set<String> perms = group != null ? parse(group.getPermissions()) : Set.of();
        return perms.contains(WILDCARD) || perms.contains(ACCESSIBLE);
    }

    public static Set<String> parse(String stored) {
        Set<String> set = new LinkedHashSet<>();
        if (stored == null || stored.isBlank()) return set;
        Set<String> valid = validKeys();
        for (String part : stored.split(",")) {
            String key = part.trim();
            if (key.isEmpty()) continue;
            if (WILDCARD.equals(key)) { set.add(WILDCARD); continue; }
            if (valid.contains(key)) set.add(key);
        }
        return set;
    }

    public static String serialize(Set<String> set) {
        if (set.contains(WILDCARD)) return WILDCARD;
        return String.join(",", set);
    }
}
