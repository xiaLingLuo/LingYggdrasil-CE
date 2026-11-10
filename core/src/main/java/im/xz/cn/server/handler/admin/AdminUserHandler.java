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
    private final im.xz.cn.database.dao.UserLogDao userLogDao;

    public AdminUserHandler(UserDao userDao, AdminDao adminDao, SystemConfig systemConfig,
                            im.xz.cn.database.dao.UserPermGroupDao userPermGroupDao,
                            im.xz.cn.database.dao.UserLogDao userLogDao) {
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.systemConfig = systemConfig;
        this.userPermGroupDao = userPermGroupDao;
        this.userLogDao = userLogDao;
    }

    public void usersPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderUsersPage(adminUsername, adminRole, csrfToken));
    }

    public void getUsers(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.view")) return;
        AdminPageQuery query = AdminPageQuery.from(ctx);
        int total = userDao.countMatching(query.search());
        int page = query.pageForTotal(total);
        List<User> users = userDao.findPage(query.search(), query.pageSize(), query.offsetForPage(page));
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
            map.put("lastLogin", u.getLastLogin());
            result.add(map);
        }
        ctx.json(Map.of("items", result, "total", total, "page", page, "pageSize", query.pageSize()));
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
        try {
            cleanupNewModel(id);
        } catch (RuntimeException ignored) {
            
        }
        try {
            userLogDao.clear(id);
        } catch (RuntimeException ignored) {
        }
        cleanupRemainingUserRows(id);
        try {
            userDao.delete(id);
        } catch (RuntimeException error) {
            ctx.status(409).json(Map.of("success", false, "message",
                    "删除失败"));
            return;
        }
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_USER:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.userDeleted")));
    }


    
    private void cleanupRemainingUserRows(String userId) {
        var db = userDao.database();
        String[] byUserId = {
                "access_sessions", "auth_tokens", "texture_access_events",
                "texture_favorites", "texture_likes", "texture_visibility"
        };
        for (String table : byUserId) {
            try {
                im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                        "DELETE FROM " + table + " WHERE user_id = ?", userId);
            } catch (RuntimeException ignored) {
                
            }
        }
        try {
            im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                    "DELETE FROM verification_challenges WHERE subject_id = ?", userId);
        } catch (RuntimeException ignored) {
        }
        try {
            im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                    "DELETE FROM friend_shared_textures WHERE owner_id = ? OR friend_id = ?", userId, userId);
        } catch (RuntimeException ignored) {
        }
        try {
            im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                    "DELETE FROM player_profiles WHERE user_id = ? OR owner_user_id = ?", userId, userId);
        } catch (RuntimeException ignored) {
        }
    }

    
    private void cleanupNewModel(String userId) {
        var db = userDao.database();
        var lifecycle = new im.xz.cn.texture.access.TextureAccessLifecycle(db);
        var uploads = new im.xz.cn.texture.library.TextureUploadRepository(db);
        var publications = new im.xz.cn.texture.publication.PublicationRepository(db);
        java.util.Set<Long> touchedAssets = new java.util.LinkedHashSet<>();
        for (var upload : uploads.findActiveByUser(userId)) {
            try {
                uploads.revoke(upload.id());
                lifecycle.revokeGrantsForAsset(userId, upload.assetId(), "ACCOUNT_DELETED");
                touchedAssets.add(upload.assetId());
            } catch (RuntimeException ignored) {
            }
        }
        for (var publication : publications.findAvailableByPublisher(userId, 500, 0)) {
            try {
                publications.withdraw(publication.id(), java.time.Instant.now());
                touchedAssets.add(publication.assetId());
            } catch (RuntimeException ignored) {
            }
        }
        for (String table : new String[]{"publication_likes", "publication_favorites"}) {
            im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                    "DELETE FROM " + table + " WHERE user_id = ?", userId);
        }
        im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                "UPDATE texture_share_grants SET state = 'OWNER_REVOKED', ended_at = ?"
                        + " WHERE state = 'ACTIVE' AND (owner_id = ? OR recipient_id = ?)",
                im.xz.cn.infrastructure.persistence.Jdbc.writeInstant(java.time.Instant.now()),
                userId, userId);
        im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                "DELETE FROM library_entries WHERE user_id = ?", userId);
        im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                "DELETE FROM profile_texture_bindings WHERE profile_id IN"
                        + " (SELECT id FROM player_profiles WHERE user_id = ? OR owner_user_id = ?)",
                userId, userId);
        for (String table : new String[]{"user_preferences", "user_social_profiles",
                "profile_credentials", "friendships", "friend_requests", "user_blocks"}) {
            try {
                if ("friendships".equals(table)) {
                    im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                            "DELETE FROM friendships WHERE user_low_id = ? OR user_high_id = ?",
                            userId, userId);
                } else if ("friend_requests".equals(table)) {
                    im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                            "DELETE FROM friend_requests WHERE from_user_id = ? OR to_user_id = ?",
                            userId, userId);
                } else if ("user_blocks".equals(table)) {
                    im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                            "DELETE FROM user_blocks WHERE blocker_id = ? OR blocked_id = ?",
                            userId, userId);
                } else if ("profile_credentials".equals(table)) {
                    im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                            "DELETE FROM profile_credentials WHERE profile_id IN"
                                    + " (SELECT id FROM player_profiles WHERE user_id = ? OR owner_user_id = ?)",
                            userId, userId);
                } else {
                    im.xz.cn.infrastructure.persistence.Jdbc.update(db,
                            "DELETE FROM " + table + " WHERE user_id = ?", userId);
                }
            } catch (RuntimeException ignored) {
                
            }
        }
        for (Long assetId : touchedAssets) {
            try {
                lifecycle.reconcileAsset(assetId, "ACCOUNT_DELETED");
            } catch (RuntimeException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void setEmailVerified(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.verify")) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        Object idRaw = body.get("id");
        Object verifiedRaw = body.get("verified");
        String id = idRaw == null ? null : String.valueOf(idRaw);
        if (id == null || id.isEmpty() || verifiedRaw == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        boolean verified;
        if (verifiedRaw instanceof Boolean flag) {
            verified = flag;
        } else {
            verified = Boolean.parseBoolean(String.valueOf(verifiedRaw));
        }
        userDao.setEmailVerified(id, verified);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "SET_EMAIL_VERIFIED:" + id + ":" + verified, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", verified ? I18n.t("msg.emailVerified") : I18n.t("msg.emailUnverified"), "emailVerified", verified));
    }

    @SuppressWarnings("unchecked")
    public void updateUsername(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.edit")) return;
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
    public void updateNickname(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.edit")) return;
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String nickname = body.get("nickname");
        if (id == null || nickname == null || nickname.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.nicknameEmpty")));
            return;
        }
        if (nickname.length() > 32) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.nicknameTooLong")));
            return;
        }
        User user = userDao.findById(id);
        if (user == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        userDao.updateNickname(id, nickname);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_NICKNAME:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.nicknameUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void createUser(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.users.create")) return;
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
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
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
        String passwordError = im.xz.cn.security.AdminPasswordValidator.validateUser(password);
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
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String email = body.get("email");
        if (id == null || email == null || email.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
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
}
