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

import im.xz.cn.i18n.AdminI18n;
import im.xz.cn.permission.PermissionRegistry;
import im.xz.cn.permission.PermissionType;
import io.javalin.http.Context;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AdminPermissions {
    public static final String WILDCARD = "*";

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private static final ThreadLocal<Set<String>> CURRENT = new ThreadLocal<>();

    private AdminPermissions() {}

    public static Set<String> validKeys() {
        return PermissionRegistry.getInstance().validKeys(PermissionType.ADMIN);
    }

    public static boolean isHighRisk(String key) {
        return PermissionRegistry.getInstance().isHighRisk(key);
    }

    public static void set(Set<String> permissions) {
        CURRENT.set(permissions);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static boolean isRoot() {
        Set<String> p = CURRENT.get();
        return p != null && p.contains(WILDCARD);
    }

    public static boolean has(String key) {
        Set<String> p = CURRENT.get();
        return p != null && (p.contains(WILDCARD) || p.contains(key));
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

    public static String currentJson() {
        Set<String> p = CURRENT.get();
        try {
            return JSON_MAPPER.writeValueAsString(p == null ? Set.of() : p);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static boolean require(Context ctx, String key) {
        if (has(key)) return true;
        ctx.status(403).json(Map.of("success", false, "message", AdminI18n.t("msg.noPermission")));
        return false;
    }
}
