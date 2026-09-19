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

import java.util.ArrayList;
import java.util.List;

public final class BuiltinPermissions {

    private BuiltinPermissions() {}

    public static List<PermissionNode> nodes() {
        List<PermissionNode> nodes = new ArrayList<>();

        nodes.add(PermissionNode.admin("admin.dashboard.view", "dashboard"));
        nodes.add(PermissionNode.admin("admin.users.view", "users"));
        nodes.add(PermissionNode.admin("admin.users.create", "users"));
        nodes.add(PermissionNode.admin("admin.users.edit", "users"));
        nodes.add(PermissionNode.admin("admin.users.ban", "users"));
        nodes.add(PermissionNode.admin("admin.users.verify", "users"));
        nodes.add(PermissionNode.admin("admin.users.delete", "users"));
        nodes.add(PermissionNode.admin("admin.profiles.view", "profiles"));
        nodes.add(PermissionNode.admin("admin.profiles.create", "profiles"));
        nodes.add(PermissionNode.admin("admin.profiles.edit", "profiles"));
        nodes.add(PermissionNode.admin("admin.profiles.transfer", "profiles"));
        nodes.add(PermissionNode.admin("admin.profiles.reset", "profiles"));
        nodes.add(PermissionNode.admin("admin.profiles.delete", "profiles"));
        nodes.add(PermissionNode.admin("admin.skins.view", "skins"));
        nodes.add(PermissionNode.admin("admin.skins.download", "skins"));
        nodes.add(PermissionNode.admin("admin.skins.alias", "skins"));
        nodes.add(PermissionNode.admin("admin.skins.delete", "skins"));
        nodes.add(PermissionNode.admin("admin.skins.orphans", "skins"));
        nodes.add(PermissionNode.admin("admin.capes.view", "capes"));
        nodes.add(PermissionNode.admin("admin.capes.download", "capes"));
        nodes.add(PermissionNode.admin("admin.capes.alias", "capes"));
        nodes.add(PermissionNode.admin("admin.capes.delete", "capes"));
        nodes.add(PermissionNode.admin("admin.capes.orphans", "capes"));
        nodes.add(PermissionNode.admin("admin.admins.view", "admins"));
        nodes.add(PermissionNode.admin("admin.admins.create", "admins", true));
        nodes.add(PermissionNode.admin("admin.admins.edit", "admins", true));
        nodes.add(PermissionNode.admin("admin.admins.delete", "admins", true));
        nodes.add(PermissionNode.admin("admin.groups.view", "groups"));
        nodes.add(PermissionNode.admin("admin.groups.edit", "groups", true));
        nodes.add(PermissionNode.admin("admin.usergroups.view", "usergroups"));
        nodes.add(PermissionNode.admin("admin.usergroups.edit", "usergroups", true));
        nodes.add(PermissionNode.admin("admin.security.view", "security"));
        nodes.add(PermissionNode.admin("admin.security.edit", "security"));
        nodes.add(PermissionNode.admin("admin.yggdrasil.view", "yggdrasil"));
        nodes.add(PermissionNode.admin("admin.yggdrasil.edit", "yggdrasil"));
        nodes.add(PermissionNode.admin("admin.yggdrasil.keys", "yggdrasil"));
        nodes.add(PermissionNode.admin("admin.system.view", "system"));
        nodes.add(PermissionNode.admin("admin.system.edit", "system"));
        nodes.add(PermissionNode.admin("admin.appinfo.view", "appinfo"));
        nodes.add(PermissionNode.admin("admin.plugin.overall.view", "plugins"));
        nodes.add(PermissionNode.admin("admin.plugin.overall.edit", "plugins"));

        nodes.add(PermissionNode.user("user.accessible", "access"));
        nodes.add(PermissionNode.user("user.profiles", "features"));
        nodes.add(PermissionNode.user("user.skins", "features"));
        nodes.add(PermissionNode.user("user.capes", "features"));
        nodes.add(PermissionNode.user("user.friends", "features"));
        nodes.add(PermissionNode.user("user.world", "features"));
        nodes.add(PermissionNode.user("user.settings", "features"));

        return nodes;
    }

    public static void register() {
        PermissionRegistry.getInstance().registerSource(PermissionRegistry.BUILTIN_SOURCE, nodes());
    }
}
