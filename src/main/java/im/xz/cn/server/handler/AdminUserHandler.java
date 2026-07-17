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

import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.User;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.model.enums.UserRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;

import io.javalin.http.Context;

import java.util.*;

public class AdminUserHandler {
    private final UserDao userDao;
    private final AdminDao adminDao;
    private final SystemConfig systemConfig;

    public AdminUserHandler(UserDao userDao, AdminDao adminDao, SystemConfig systemConfig) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.systemConfig = systemConfig;
    }

    public void usersPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderUsersPage(adminUsername, adminRole, csrfToken));
    }

    public void getUsers(Context ctx) {
        List<User> users = userDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("nickname", u.getNickname());
            map.put("role", u.getRole().name());
            map.put("emailVerified", u.isEmailVerified());
            map.put("createdAt", u.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void banUser(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", "用户不存在"));
            return;
        }
        userDao.updateRole(id, UserRole.BANNED.toDbValue());
        AuditLogger.logPermissionChange(getAdminName(ctx), "user:" + id, "BAN_USER", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "用户已封禁"));
    }

    @SuppressWarnings("unchecked")
    public void unbanUser(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", "用户不存在"));
            return;
        }
        userDao.updateRole(id, UserRole.DEFAULT.toDbValue());
        AuditLogger.logPermissionChange(getAdminName(ctx), "user:" + id, "UNBAN_USER", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "用户已解封"));
    }

    @SuppressWarnings("unchecked")
    public void deleteUser(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", "用户不存在"));
            return;
        }
        userDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_USER:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "用户已删除"));
    }

    @SuppressWarnings("unchecked")
    public void updateUsername(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String username = body.get("username");
        if (id == null || username == null || username.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        if (username.length() < 3 || username.length() > 32) {
            ctx.status(400).json(Map.of("success", false, "message", "用户名长度需在3-32位之间"));
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", "用户名只能包含字母、数字和下划线"));
            return;
        }
        User existing = userDao.findByUsername(username);
        if (existing != null && !existing.getId().equals(id)) {
            ctx.status(400).json(Map.of("success", false, "message", "用户名已被使用"));
            return;
        }
        if (systemConfig.isUsernameBlacklisted(username)) {
            ctx.status(400).json(Map.of("success", false, "message", "用户名被占用"));
            return;
        }
        userDao.updateUsername(id, username);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_USERNAME:" + id + "->" + username, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "用户名已修改"));
    }

    @SuppressWarnings("unchecked")
    public void updateEmail(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String email = body.get("email");
        if (id == null || email == null || email.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            ctx.status(400).json(Map.of("success", false, "message", "邮箱格式不正确"));
            return;
        }
        User existing = userDao.findByEmail(email);
        if (existing != null && !existing.getId().equals(id)) {
            ctx.status(400).json(Map.of("success", false, "message", "邮箱已被使用"));
            return;
        }
        if (!systemConfig.isEmailDomainAllowed(email)) {
            ctx.status(400).json(Map.of("success", false, "message", "该邮箱域名不允许注册"));
            return;
        }
        userDao.updateEmail(id, email);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_EMAIL:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "邮箱已修改"));
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
