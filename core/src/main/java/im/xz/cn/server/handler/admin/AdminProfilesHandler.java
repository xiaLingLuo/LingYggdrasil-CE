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
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.profile.ProfileTextureBindingRepository;
import im.xz.cn.settings.SettingsService;
import im.xz.cn.settings.SettingsRuntime;
import im.xz.cn.settings.RevisionConflictException;
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
    private final ProfileTextureBindingRepository textureBindings;
    private final DatabaseManager database;
    private final SettingsService settings;

    public AdminProfilesHandler(ProfileDao profileDao, UserDao userDao, AdminDao adminDao,
                                SystemConfig systemConfig, DatabaseManager database) {
        this.profileDao = profileDao;
        this.userDao = userDao;
        this.adminDao = adminDao;
        this.systemConfig = systemConfig;
        this.textureBindings = new ProfileTextureBindingRepository(database);
        this.database = database;
        this.settings = SettingsService.create(database);
    }

    public void profilesPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAdminProfilesPage(adminUsername, adminRole, csrfToken));
    }

    public void profileSettingsPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderProfileSettingsPage(adminUsername, adminRole, csrfToken));
    }

    public void getProfiles(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.view")) return;
        AdminPageQuery query = AdminPageQuery.from(ctx);
        int total = profileDao.countAdminProfiles(query.search());
        int page = query.pageForTotal(total);
        List<ProfileDao.AdminProfileRow> profiles = profileDao.findAdminPage(
                query.search(), query.pageSize(), query.offsetForPage(page));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProfileDao.AdminProfileRow row : profiles) {
            PlayerProfile p = row.profile();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("userId", p.getUserId());
            if (p.getUserId() == null || p.getUserId().isBlank()
                    || DatabaseSchema.UNASSIGNED_USER_ID.equals(p.getUserId())) {
                map.put("username", I18n.t("msg.unassigned"));
            } else {
                map.put("username", row.ownerUsername() != null ? row.ownerUsername() : I18n.t("msg.unknownUser"));
            }
            map.put("skinModel", p.getSkinModel());
            map.put("createdAt", p.getCreatedAt());
            result.add(map);
        }
        ctx.json(Map.of("items", result, "total", total, "page", page, "pageSize", query.pageSize()));
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

        if (name.length() < systemConfig.getMinProfileNameLength()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.profiles.nameTooShort")));
            return;
        }
        if (name.length() > systemConfig.getMaxProfileNameLength()) {
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
        PlayerProfile profile = new PlayerProfile(id, null, name, null, null, "default", yggdrasilToken, createdAt);
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
            if (name.length() < systemConfig.getMinProfileNameLength()) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.profiles.nameTooShort")));
                return;
            }
            if (name.length() > systemConfig.getMaxProfileNameLength()) {
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

        textureBindings.removeByProfile(id);
        profileDao.clearTextures(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CLEAR_PROFILE_TEXTURES:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.profileTexturesCleared")));
    }


    private static final java.util.Set<String> PROFILE_SETTING_KEYS = java.util.Set.of(
            "profile_name_blacklist", "profile_name_blacklist_case_sensitive",
            "max_profiles_per_user", "min_profile_name_length", "max_profile_name_length");

    public void getSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.view")) return;
        SettingsService.GroupSnapshot snapshot = settings.snapshot("identity");
        ctx.json(Map.of("success", true, "revision", snapshot.revision(),
                "values", profileSettingsValues(snapshot)));
    }

    @SuppressWarnings("unchecked")
    public void updateSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.edit")) return;
        Map<String, Object> body;
        try {
            body = ctx.bodyAsClass(Map.class);
        } catch (RuntimeException error) {
            ctx.status(400).json(Map.of("success", false, "message", "Invalid request body"));
            return;
        }
        Object revisionValue = body.get("revision");
        Object valuesValue = body.get("values");
        if (!(revisionValue instanceof Number revisionNumber) || !(valuesValue instanceof Map<?, ?> rawValues)) {
            ctx.status(400).json(Map.of("success", false, "message", "revision and values are required"));
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawValues.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (!PROFILE_SETTING_KEYS.contains(key)) {
                ctx.status(400).json(Map.of("success", false, "message", "Unknown setting: " + key));
                return;
            }
            values.put(key, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        String error = validateProfileSettings(values);
        if (error != null) {
            ctx.status(422).json(Map.of("success", false, "message", error));
            return;
        }
        try {
            SettingsService.GroupSnapshot snapshot = settings.commit("identity", values, revisionNumber.longValue());
            SettingsRuntime.apply(database, values, true, true);
            AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_PROFILE_SETTINGS", IpUtil.getClientIp(ctx));
            ctx.json(Map.of("success", true, "revision", snapshot.revision(),
                    "values", profileSettingsValues(snapshot)));
        } catch (RevisionConflictException conflict) {
            ctx.status(409).json(Map.of("success", false, "message", "Settings were modified concurrently",
                    "revision", conflict.actualRevision()));
        } catch (RuntimeException failure) {
            ctx.status(422).json(Map.of("success", false, "message", failure.getMessage()));
        }
    }


    private Map<String, String> profileSettingsValues(SettingsService.GroupSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("profile_name_blacklist", snapshot.values().getOrDefault("profile_name_blacklist", ""));
        values.put("profile_name_blacklist_case_sensitive",
                snapshot.values().getOrDefault("profile_name_blacklist_case_sensitive", "false"));
        values.put("max_profiles_per_user",
                snapshot.values().getOrDefault("max_profiles_per_user", "10"));
        values.put("min_profile_name_length",
                snapshot.values().getOrDefault("min_profile_name_length",
                        String.valueOf(systemConfig.getMinProfileNameLength())));
        values.put("max_profile_name_length",
                snapshot.values().getOrDefault("max_profile_name_length",
                        String.valueOf(systemConfig.getMaxProfileNameLength())));
        return values;
    }

    private String validateProfileSettings(Map<String, String> values) {
        String maxProfiles = values.get("max_profiles_per_user");
        if (maxProfiles != null && !validInt(maxProfiles, 0, 10000)) {
            return "每用户最大角色数量必须在 0-10000 之间";
        }
        String min = values.get("min_profile_name_length");
        String max = values.get("max_profile_name_length");
        if (min != null && !validInt(min, 1, 64)) return "角色名最小长度必须在 1-64 之间";
        if (max != null && !validInt(max, 1, 64)) return "角色名最大长度必须在 1-64 之间";
        if (min != null && max != null && Integer.parseInt(min) > Integer.parseInt(max)) {
            return "角色名最小长度不能大于最大长度";
        }
        String caseSensitive = values.get("profile_name_blacklist_case_sensitive");
        if (caseSensitive != null && !"true".equalsIgnoreCase(caseSensitive)
                && !"false".equalsIgnoreCase(caseSensitive)) {
            return "大小写严格模式必须是布尔值";
        }
        return null;
    }

    private static boolean validInt(String raw, int min, int max) {
        try {
            int value = Integer.parseInt(raw);
            return value >= min && value <= max;
        } catch (NumberFormatException error) {
            return false;
        }
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

