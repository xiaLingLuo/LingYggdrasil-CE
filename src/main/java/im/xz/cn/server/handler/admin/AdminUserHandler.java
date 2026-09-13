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
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.User;
import im.xz.cn.security.PasswordValidator;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;

import io.javalin.http.Context;

import java.util.*;

public class AdminUserHandler {
    private final UserDao userDao;
    private final AdminDao adminDao;
    private final SystemConfig systemConfig;
    private final im.xz.cn.database.dao.UserPermGroupDao userPermGroupDao;

    public AdminUserHandler(UserDao userDao, AdminDao adminDao, SystemConfig systemConfig,
                            im.xz.cn.database.dao.UserPermGroupDao userPermGroupDao) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.systemConfig = systemConfig;
        this.userPermGroupDao = userPermGroupDao;
    }

    public void usersPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderUsersPage(adminUsername, adminRole, csrfToken));
    }

    public void getUsers(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.view")) return;
        List<User> users = userDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("nickname", u.getNickname());
            map.put("permGroup", u.getPermGroup());
            map.put("emailVerified", u.isEmailVerified());
            map.put("createdAt", u.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void updateUserPermGroup(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.edit")) return;
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String group = body.get("permGroup");
        if (id == null || group == null || group.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        if (userPermGroupDao.findByName(group) == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.groupNotFound")));
            return;
        }
        userDao.updatePermGroup(id, group);
        AuditLogger.logPermissionChange(getAdminName(ctx), "user:" + id, "SET_USER_PERM_GROUP:" + group, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.groupUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void deleteUser(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.delete")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        userDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_USER:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.userDeleted")));
    }

    @SuppressWarnings("unchecked")
    public void setEmailVerified(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.verify")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String verifiedRaw = body.get("verified");
        if (id == null || verifiedRaw == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        boolean verified = Boolean.parseBoolean(verifiedRaw);
        userDao.setEmailVerified(id, verified);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "SET_EMAIL_VERIFIED:" + id + ":" + verified, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", verified ? I18n.t("msg.emailVerified") : I18n.t("msg.emailUnverified"), "emailVerified", verified));
    }

    @SuppressWarnings("unchecked")
    public void updateUsername(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String username = body.get("username");
        if (id == null || username == null || username.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        if (username.length() < 3 || username.length() > 32) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameLength")));
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameCharset")));
            return;
        }
        User existing = userDao.findByUsername(username);
        if (existing != null && !existing.getId().equals(id)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameInUse")));
            return;
        }
        if (systemConfig.isUsernameBlacklisted(username)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameTaken")));
            return;
        }
        userDao.updateUsername(id, username);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_USERNAME:" + id + "->" + username, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.usernameUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void createUser(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.create")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String nickname = body.get("nickname");
        String verifiedRaw = body.get("emailVerified");

        if (username == null || username.isEmpty() || email == null || email.isEmpty()
                || password == null || password.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.createUserFillAll")));
            return;
        }
        if (username.length() < 3 || username.length() > 32) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameLength")));
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameCharset")));
            return;
        }
        if (userDao.findByUsername(username) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameInUse")));
            return;
        }
        if (systemConfig.isUsernameBlacklisted(username)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameTaken")));
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailInvalid")));
            return;
        }
        if (userDao.findByEmail(email) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailInUse")));
            return;
        }
        if (!systemConfig.isEmailDomainAllowed(email)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailDomainNotAllowed")));
            return;
        }
        String passwordError = PasswordValidator.validateUser(password);
        if (passwordError != null) {
            ctx.status(400).json(Map.of("success", false, "message", passwordError));
            return;
        }

        boolean verified = "true".equalsIgnoreCase(verifiedRaw) || "1".equals(verifiedRaw);
        String id = UuidUtil.generateUserUuid();
        String passwordHash = Argon2Hasher.hash(password);
        String createdAt = TimeUtil.now();
        User user = new User(id, username, email, passwordHash,
                nickname == null ? "" : nickname, verified,
                createdAt, null, IpUtil.getClientIp(ctx));
        try {
            userDao.insert(user);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", I18n.t("msg.createUserFailed")));
            return;
        }
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CREATE_USER:" + username, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.userCreated")));
    }

    @SuppressWarnings("unchecked")
    public void updateEmail(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String email = body.get("email");
        if (id == null || email == null || email.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailInvalid")));
            return;
        }
        User existing = userDao.findByEmail(email);
        if (existing != null && !existing.getId().equals(id)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailInUse")));
            return;
        }
        if (!systemConfig.isEmailDomainAllowed(email)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.emailDomainNotAllowed")));
            return;
        }
        userDao.updateEmail(id, email);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_EMAIL:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.emailUpdated")));
    }

    private String getAdminName(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return "unknown";
        Admin admin = adminDao.findById(adminId);
        return admin != null ? admin.getUsername() : "unknown";
    }

    private boolean isRoot(Context ctx) {
        return SessionManager.isAdminRoot(ctx);
    }
}
