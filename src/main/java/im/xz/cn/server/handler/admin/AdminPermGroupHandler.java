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
import im.xz.cn.database.dao.PermGroupDao;
import im.xz.cn.database.dao.RootInfoDao;
import im.xz.cn.i18n.I18n;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.model.PermGroup;
import im.xz.cn.security.AdminPermissions;

import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AdminPermGroupHandler {
    private static final java.util.regex.Pattern NAME_PATTERN = java.util.regex.Pattern.compile("^[a-z0-9]+$");

    private final PermGroupDao permGroupDao;
    private final RootInfoDao rootInfoDao;

    public AdminPermGroupHandler(PermGroupDao permGroupDao, RootInfoDao rootInfoDao) {
        this.permGroupDao = permGroupDao;
        this.rootInfoDao = rootInfoDao;
    }

    public void getCatalogue(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.groups.view")) return;
        List<Map<String, Object>> perms = new ArrayList<>();
        for (AdminPermissions.Perm p : AdminPermissions.ALL) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", p.key());
            m.put("category", p.category());
            m.put("highRisk", AdminPermissions.isHighRisk(p.key()));
            perms.add(m);
        }
        ctx.json(Map.of("success", true, "permissions", perms));
    }

    public void getGroups(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.groups.view")) return;
        List<Map<String, Object>> result = new ArrayList<>();
        for (PermGroup g : permGroupDao.findAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("alias", aliasOf(g.getName()));
            m.put("permissions", g.getPermissions());
            m.put("builtin", "op".equals(g.getName()));
            m.put("adminCount", permGroupDao.countAdminsUsing(g.getName()));
            result.add(m);
        }
        ctx.json(Map.of("success", true, "groups", result));
    }

    @SuppressWarnings("unchecked")
    public void createGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.groups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String name = body.get("name") == null ? null : String.valueOf(body.get("name")).trim();
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupNameInvalid")));
            return;
        }
        if (permGroupDao.findByName(name) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupExists")));
            return;
        }
        Set<String> perms = parsePermissions(body.get("permissions"));
        permGroupDao.insert(new PermGroup(UUID.randomUUID().toString(), name,
                AdminPermissions.serialize(perms), TimeUtil.now()));
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CREATE_PERM_GROUP:" + name, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupCreated")));
    }

    @SuppressWarnings("unchecked")
    public void updateGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.groups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id") == null ? null : String.valueOf(body.get("id"));
        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        PermGroup group = permGroupDao.findById(id);
        if (group == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.groupNotFound")));
            return;
        }
            if (body.containsKey("permissions")) {
            permGroupDao.updatePermissions(id, AdminPermissions.serialize(parsePermissions(body.get("permissions"))));
        }
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_PERM_GROUP:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void deleteGroup(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.groups.edit")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id") == null ? null : String.valueOf(body.get("id"));
        PermGroup group = id != null ? permGroupDao.findById(id) : null;
        if (group == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.groupNotFound")));
            return;
        }
        if ("op".equals(group.getName())) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.cannotDeleteBuiltinGroup")));
            return;
        }
        if (permGroupDao.countAdminsUsing(group.getName()) > 0) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupInUse")));
            return;
        }
        permGroupDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_PERM_GROUP:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupDeleted")));
    }

    @SuppressWarnings("unchecked")
    private Set<String> parsePermissions(Object raw) {
        Set<String> perms = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                String key = String.valueOf(o).trim();
                if (AdminPermissions.WILDCARD.equals(key)) perms.add(AdminPermissions.WILDCARD);
                else if (AdminPermissions.validKeys().contains(key)) perms.add(key);
            }
        } else if (raw instanceof String s) {
            perms.addAll(AdminPermissions.parse(s));
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
