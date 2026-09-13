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


import im.xz.cn.i18n.I18n;
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.dao.RootInfoDao;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.security.PasswordValidator;
import im.xz.cn.model.Admin;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;

import io.javalin.http.Context;

import java.util.*;

public class AdminAdminHandler {
    private final AdminDao adminDao;
    private final RootInfoDao rootInfoDao;

    public AdminAdminHandler(AdminDao adminDao, RootInfoDao rootInfoDao) {
        this.adminDao = adminDao;
        this.rootInfoDao = rootInfoDao;
    }

    public void adminsPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        boolean isRoot = SessionManager.isAdminRoot(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAdminsPage(adminUsername, adminRole, isRoot, csrfToken));
    }

    public void getAdmins(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.admins.view")) return;
        List<Admin> admins = adminDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Admin a : admins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("username", a.getUsername());
            map.put("email", a.getEmail());
            map.put("permGroup", a.getPermGroup());
            map.put("createdAt", a.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void createAdmin(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.admins.create")) return;

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String permGroup = body.get("permGroup");

        if (username == null || username.isEmpty() || email == null || email.isEmpty()
                || password == null || password.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.adminFillAll")));
            return;
        }

        String passwordError = PasswordValidator.validate(password);
        if (passwordError != null) {
            ctx.status(400).json(Map.of("success", false, "message", passwordError));
            return;
        }

        if (adminDao.findByUsername(username) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameExists")));
            return;
        }

        String id = UuidUtil.generateAdminUuid();
        String passwordHash = Argon2Hasher.hash(password);
        String createdAt = TimeUtil.now();
        Admin admin = new Admin(id, username, email, passwordHash, createdAt);
        admin.setPermGroup(permGroup == null || permGroup.isBlank() ? "op" : permGroup.trim());
        try {
            adminDao.insert(admin);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameExists")));
                return;
            }
            throw e;
        }
        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + username, "CREATE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.adminCreated")));
    }

    @SuppressWarnings("unchecked")
    public void deleteAdmin(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.admins.delete")) return;

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String targetId = body.get("id");
        if (targetId == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        String currentAdminId = SessionManager.getAdminId(ctx);
        if (targetId.equals(currentAdminId)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.cannotDeleteSelf")));
            return;
        }

        Admin target = adminDao.findById(targetId);
        if (target == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.adminNotFound")));
            return;
        }
        adminDao.delete(targetId);
        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + targetId, "DELETE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.adminDeleted")));
    }

    @SuppressWarnings("unchecked")
    public void updateAdmin(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.admins.edit")) return;

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String targetId = body.get("id");
        if (targetId == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        Admin target = adminDao.findById(targetId);
        if (target == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.adminNotFound")));
            return;
        }
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String permGroup = body.get("permGroup");

        if (username != null && !username.isEmpty()) {
            Admin existing = adminDao.findByUsername(username);
            if (existing != null && !existing.getId().equals(targetId)) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameExists")));
                return;
            }
            try {
                adminDao.updateUsername(targetId, username);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameExists")));
                    return;
                }
                throw e;
            }
        }
        if (email != null && !email.isEmpty()) {
            adminDao.updateEmail(targetId, email);
        }
        if (password != null && !password.isEmpty()) {
            String passwordError = PasswordValidator.validate(password);
            if (passwordError != null) {
                ctx.status(400).json(Map.of("success", false, "message", passwordError));
                return;
            }
            String passwordHash = Argon2Hasher.hash(password);
            adminDao.updatePassword(targetId, passwordHash);
        }
        if (permGroup != null && !permGroup.isBlank()) {
            adminDao.updatePermGroup(targetId, permGroup.trim());
        }

        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + targetId, "UPDATE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.adminUpdated")));
    }

    private String getAdminName(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return "unknown";
        if (SessionManager.isAdminRoot(ctx)) {
            var root = rootInfoDao.findById(adminId);
            return root != null ? root.getUsername() : "unknown";
        }
        Admin admin = adminDao.findById(adminId);
        return admin != null ? admin.getUsername() : "unknown";
    }
}
