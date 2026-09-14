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
import io.javalin.http.Context;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminPermissions {
    public static final String WILDCARD = "*";

    public record Perm(String key, String category) {}

    public static final List<Perm> ALL = List.of(
            new Perm("admin.dashboard.view", "dashboard"),
            new Perm("admin.users.view", "users"),
            new Perm("admin.users.create", "users"),
            new Perm("admin.users.edit", "users"),
            new Perm("admin.users.ban", "users"),
            new Perm("admin.users.verify", "users"),
            new Perm("admin.users.delete", "users"),
            new Perm("admin.profiles.view", "profiles"),
            new Perm("admin.profiles.create", "profiles"),
            new Perm("admin.profiles.edit", "profiles"),
            new Perm("admin.profiles.transfer", "profiles"),
            new Perm("admin.profiles.reset", "profiles"),
            new Perm("admin.profiles.delete", "profiles"),
            new Perm("admin.skins.view", "skins"),
            new Perm("admin.skins.download", "skins"),
            new Perm("admin.skins.alias", "skins"),
            new Perm("admin.skins.delete", "skins"),
            new Perm("admin.skins.orphans", "skins"),
            new Perm("admin.capes.view", "capes"),
            new Perm("admin.capes.download", "capes"),
            new Perm("admin.capes.alias", "capes"),
            new Perm("admin.capes.delete", "capes"),
            new Perm("admin.capes.orphans", "capes"),
            new Perm("admin.admins.view", "admins"),
            new Perm("admin.admins.create", "admins"),
            new Perm("admin.admins.edit", "admins"),
            new Perm("admin.admins.delete", "admins"),
            new Perm("admin.groups.view", "groups"),
            new Perm("admin.groups.edit", "groups"),
            new Perm("admin.usergroups.view", "usergroups"),
            new Perm("admin.usergroups.edit", "usergroups"),
            new Perm("admin.security.view", "security"),
            new Perm("admin.security.edit", "security"),
            new Perm("admin.yggdrasil.view", "yggdrasil"),
            new Perm("admin.yggdrasil.edit", "yggdrasil"),
            new Perm("admin.yggdrasil.keys", "yggdrasil"),
            new Perm("admin.system.view", "system"),
            new Perm("admin.system.edit", "system"),
            new Perm("admin.appinfo.view", "appinfo")
    );

    private static final Set<String> VALID = new LinkedHashSet<>();
    static {
        for (Perm p : ALL) VALID.add(p.key());
    }

    private static final Set<String> HIGH_RISK = Set.of(
            "admin.admins.create",
            "admin.admins.edit",
            "admin.admins.delete",
            "admin.groups.edit",
            "admin.usergroups.edit"
    );

    public static boolean isHighRisk(String key) {
        return HIGH_RISK.contains(key);
    }

    private static final ThreadLocal<Set<String>> CURRENT = new ThreadLocal<>();

    private AdminPermissions() {}

    public static Set<String> validKeys() {
        return VALID;
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
        for (String part : stored.split(",")) {
            String key = part.trim();
            if (key.isEmpty()) continue;
            if (WILDCARD.equals(key)) { set.add(WILDCARD); continue; }
            if (VALID.contains(key)) set.add(key);
        }
        return set;
    }

    public static String serialize(Set<String> set) {
        if (set.contains(WILDCARD)) return WILDCARD;
        return String.join(",", set);
    }

    public static String currentJson() {
        Set<String> p = CURRENT.get();
        StringBuilder sb = new StringBuilder("[");
        if (p != null) {
            boolean first = true;
            for (String k : p) {
                if (!first) sb.append(',');
                sb.append('"').append(k).append('"');
                first = false;
            }
        }
        return sb.append(']').toString();
    }

    public static boolean require(Context ctx, String key) {
        if (has(key)) return true;
        ctx.status(403).json(Map.of("success", false, "message", AdminI18n.t("msg.noPermission")));
        return false;
    }
}
