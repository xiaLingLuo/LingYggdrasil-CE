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
package im.xz.cn.server.handler;

import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.AdminDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TimeUtil;
import im.xz.cn.util.UuidUtil;

import io.javalin.http.Context;

import java.util.*;

public class AdminAdminHandler {
    private final AdminDao adminDao;

    public AdminAdminHandler(AdminDao adminDao) {
        this.adminDao = adminDao;
    }

    public void adminsPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        boolean isRoot = "ROOT".equalsIgnoreCase(adminRole);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAdminsPage(adminUsername, adminRole, isRoot, csrfToken));
    }

    public void getAdmins(Context ctx) {
        List<Admin> admins = adminDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Admin a : admins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("username", a.getUsername());
            map.put("email", a.getEmail());
            map.put("role", a.getRole().name());
            map.put("createdAt", a.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void createAdmin(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || username.isEmpty() || email == null || email.isEmpty()
                || password == null || password.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "请填写完整信息"));
            return;
        }

        if (adminDao.findByUsername(username) != null) {
            ctx.status(400).json(Map.of("success", false, "message", "用户名已存在"));
            return;
        }

        String id = UuidUtil.generateAdminUuid();
        String passwordHash = Argon2Hasher.hash(password);
        String createdAt = TimeUtil.now();
        Admin admin = new Admin(id, username, email, passwordHash, AdminRole.OP, createdAt);
        try {
            adminDao.insert(admin);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                ctx.status(400).json(Map.of("success", false, "message", "用户名已存在"));
                return;
            }
            throw e;
        }
        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + username, "CREATE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "管理员已创建"));
    }

    @SuppressWarnings("unchecked")
    public void deleteAdmin(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String targetId = body.get("id");
        if (targetId == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        String currentAdminId = SessionManager.getAdminId(ctx);
        if (targetId.equals(currentAdminId)) {
            ctx.status(400).json(Map.of("success", false, "message", "不可删除自己的账号"));
            return;
        }

        Admin target = adminDao.findById(targetId);
        if (target == null) {
            ctx.status(404).json(Map.of("success", false, "message", "管理员不存在"));
            return;
        }
        if (target.getRole() == AdminRole.ROOT) {
            ctx.status(403).json(Map.of("success", false, "message", "不可操作 root 管理员账号"));
            return;
        }

        adminDao.delete(targetId);
        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + targetId, "DELETE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "管理员已删除"));
    }

    @SuppressWarnings("unchecked")
    public void updateAdmin(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }

        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String targetId = body.get("id");
        if (targetId == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        Admin target = adminDao.findById(targetId);
        if (target == null) {
            ctx.status(404).json(Map.of("success", false, "message", "管理员不存在"));
            return;
        }
        if (target.getRole() == AdminRole.ROOT) {
            ctx.status(403).json(Map.of("success", false, "message", "不可操作 root 管理员账号"));
            return;
        }

        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username != null && !username.isEmpty()) {
            Admin existing = adminDao.findByUsername(username);
            if (existing != null && !existing.getId().equals(targetId)) {
                ctx.status(400).json(Map.of("success", false, "message", "用户名已存在"));
                return;
            }
            try {
                adminDao.updateUsername(targetId, username);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof java.sql.SQLIntegrityConstraintViolationException) {
                    ctx.status(400).json(Map.of("success", false, "message", "用户名已存在"));
                    return;
                }
                throw e;
            }
        }
        if (email != null && !email.isEmpty()) {
            adminDao.updateEmail(targetId, email);
        }
        if (password != null && !password.isEmpty()) {
            String passwordHash = Argon2Hasher.hash(password);
            adminDao.updatePassword(targetId, passwordHash);
        }

        AuditLogger.logPermissionChange(getAdminName(ctx), "admin:" + targetId, "UPDATE_ADMIN", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "管理员信息已更新"));
    }

    private String getAdminName(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return "unknown";
        Admin admin = adminDao.findById(adminId);
        return admin != null ? admin.getUsername() : "unknown";
    }

    private boolean isRoot(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return false;
        Admin admin = adminDao.findById(adminId);
        return admin != null && admin.getRole() == AdminRole.ROOT;
    }
}
