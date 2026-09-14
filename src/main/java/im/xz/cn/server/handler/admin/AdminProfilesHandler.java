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


import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseSchema;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;

import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminProfilesHandler {
    private final ProfileDao profileDao;
    private final UserDao userDao;
    private final AdminDao adminDao;
    private final SystemConfig systemConfig;

    public AdminProfilesHandler(ProfileDao profileDao, UserDao userDao, AdminDao adminDao, SystemConfig systemConfig) {
        this.profileDao = profileDao;
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.systemConfig = systemConfig;
    }

    public void profilesPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAdminProfilesPage(adminUsername, adminRole, csrfToken));
    }

    public void getProfiles(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.view")) return;
        List<PlayerProfile> profiles = profileDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayerProfile p : profiles) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("userId", p.getUserId());
            if (DatabaseSchema.UNASSIGNED_USER_ID.equals(p.getUserId())) {
                map.put("username", I18n.t("msg.unassigned"));
            } else {
                User user = userDao.findById(p.getUserId());
                map.put("username", user != null ? user.getUsername() : I18n.t("msg.unknownUser"));
            }
            map.put("skinModel", p.getSkinModel());
            map.put("createdAt", p.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void createProfile(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.create")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String name = body.get("name");
        String uuidRaw = body.get("uuid");

        if (name == null || name.isEmpty() || uuidRaw == null || uuidRaw.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        if (name.length() > 24) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.profiles.nameTooLong")));
            return;
        }
        if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameInvalid")));
            return;
        }
        if (systemConfig.isProfileNameBlacklisted(name)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameTaken")));
            return;
        }

        String id = normalizeProfileUuid(uuidRaw);
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileUuidInvalid")));
            return;
        }

        if (profileDao.existsByName(name)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameExistsAdmin")));
            return;
        }
        if (profileDao.findById(id) != null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileUuidExists")));
            return;
        }

        String createdAt = TimeUtil.now();
        String yggdrasilToken = AuthService.generateYggdrasilToken();
        PlayerProfile profile = new PlayerProfile(id, DatabaseSchema.UNASSIGNED_USER_ID, name, null, null, "default", yggdrasilToken, createdAt);
        profileDao.insert(profile);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CREATE_PROFILE:" + name + "->unassigned", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.profileCreated")));
    }

    private String normalizeProfileUuid(String raw) {
        if (raw == null) return null;
        String hex = raw.trim().replace("-", "").toLowerCase();
        if (!hex.matches("^[0-9a-f]{32}$")) return null;
        return hex;
    }

    @SuppressWarnings("unchecked")
    public void deleteProfile(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.delete")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.profileNotExist")));
            return;
        }
        profileDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_PROFILE:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.profileDeleted")));
    }

    @SuppressWarnings("unchecked")
    public void updateProfile(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String name = body.get("name");
        String model = body.get("model");

        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.profileNotExist")));
            return;
        }

        if (name != null && !name.isEmpty()) {
            if (name.length() > 24) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.profiles.nameTooLong")));
                return;
            }
            if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameInvalid")));
                return;
            }
            if (systemConfig.isProfileNameBlacklisted(name)) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameTaken")));
                return;
            }
            if (!profile.getName().equals(name) && profileDao.existsByName(name)) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.profileNameExistsAdmin")));
                return;
            }
            profile.setName(name);
        }

        if (model != null && !model.isEmpty()) {
            if (!"default".equals(model) && !"slim".equals(model)) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidModel")));
                return;
            }
            profile.setSkinModel(model);
        }

        profileDao.update(profile);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_PROFILE:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.profileUpdated")));
    }

    @SuppressWarnings("unchecked")
    public void transferProfile(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.transfer")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String userId = body.get("userId");

        if (id == null || id.isEmpty() || userId == null || userId.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.profileNotExist")));
            return;
        }

        User user = findUserByIdentifier(userId);
        if (user == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.targetUserNotFound")));
            return;
        }

        profileDao.updateUserId(id, user.getId());
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "TRANSFER_PROFILE:" + id + "->" + user.getId(), IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.transferSuccess")));
    }

    @SuppressWarnings("unchecked")
    public void clearProfileTextures(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.reset")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");

        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.profileNotExist")));
            return;
        }

        profileDao.clearTextures(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CLEAR_PROFILE_TEXTURES:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.profileTexturesCleared")));
    }

    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;
        User user = userDao.findById(identifier);
        if (user != null) return user;
        user = userDao.findByUsername(identifier);
        if (user != null) return user;
        user = userDao.findByEmail(identifier);
        return user;
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
