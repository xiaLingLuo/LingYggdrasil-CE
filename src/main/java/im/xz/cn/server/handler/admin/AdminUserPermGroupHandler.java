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
package im.xz.cn.server.handler.admin;

import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.database.dao.UserPermGroupDao;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.model.PermGroup;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.security.UserPermissions;

import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AdminUserPermGroupHandler {
    private static final java.util.regex.Pattern NAME_PATTERN = java.util.regex.Pattern.compile("^[a-z0-9]+$");

    private final UserPermGroupDao userPermGroupDao;

    public AdminUserPermGroupHandler(UserPermGroupDao userPermGroupDao) {
        this.userPermGroupDao = userPermGroupDao;
    }

    public void getCatalogue(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.usergroups.view")) return;
        List<Map<String, Object>> perms = new ArrayList<>();
        for (UserPermissions.Perm p : UserPermissions.ALL) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", p.key());
            m.put("category", p.category());
            m.put("highRisk", AdminPermissions.isHighRisk(p.key()));
            perms.add(m);
        }
        ctx.json(Map.of("success", true, "permissions", perms));
    }

    public void getGroups(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.usergroups.view")) return;
        List<Map<String, Object>> result = new ArrayList<>();
        for (PermGroup g : userPermGroupDao.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("alias", aliasOf(g.getName()));
            m.put("permissions", g.getPermissions());
            m.put("builtin", "default".equals(g.getName()) || "banned".equals(g.getName()));
            m.put("immutable", "banned".equals(g.getName()));
            m.put("userCount", userPermGroupDao.countUsersUsing(g.getName()));
            result.add(m);
        }
        ctx.json(Map.of("success", true, "groups", result));
    }

    @SuppressWarnings("unchecked")
    public void createGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.usergroups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String name = body.get("name") == null ? null : String.valueOf(body.get("name")).trim();
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupNameInvalid")));
            return;
        }
        if (userPermGroupDao.findByName(name) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupExists")));
            return;
        }
        Set<String> perms = parsePermissions(body.get("permissions"));
        userPermGroupDao.insert(new PermGroup(UUID.randomUUID().toString(), name,
                UserPermissions.serialize(perms), TimeUtil.now()));
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CREATE_USER_PERM_GROUP:" + name, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupCreated")));
    }

    @SuppressWarnings("unchecked")
    public void updateGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.usergroups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id") == null ? null : String.valueOf(body.get("id"));
        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        PermGroup group = userPermGroupDao.findById(id);
        if (group == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.groupNotFound")));
            return;
        }
        if ("banned".equals(group.getName())) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupImmutable")));
            return;
        }
        if (body.containsKey("permissions")) {
            userPermGroupDao.updatePermissions(id, UserPermissions.serialize(parsePermissions(body.get("permissions"))));
        }
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_USER_PERM_GROUP:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void deleteGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.usergroups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id") == null ? null : String.valueOf(body.get("id"));
        PermGroup group = id != null ? userPermGroupDao.findById(id) : null;
        if (group == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.groupNotFound")));
            return;
        }
        if ("default".equals(group.getName()) || "banned".equals(group.getName())) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.cannotDeleteBuiltinGroup")));
            return;
        }
        if (userPermGroupDao.countUsersUsing(group.getName()) > 0) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.userGroupInUse")));
            return;
        }
        userPermGroupDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_USER_PERM_GROUP:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupDeleted")));
    }

    @SuppressWarnings("unchecked")
    private Set<String> parsePermissions(Object raw) {
        Set<String> perms = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                String key = String.valueOf(o).trim();
                if (UserPermissions.WILDCARD.equals(key)) perms.add(UserPermissions.WILDCARD);
                else if (UserPermissions.validKeys().contains(key)) perms.add(key);
            }
        } else if (raw instanceof String s) {
            perms.addAll(UserPermissions.parse(s));
        }
        return perms;
    }

    private String aliasOf(String name) {
        String alias = I18n.tOrNull("permGroup." + name);
        return alias != null ? alias : name;
    }

    private String getAdminName(Context ctx) {
        return ctx.sessionAttribute("adminUsername");
    }
}
