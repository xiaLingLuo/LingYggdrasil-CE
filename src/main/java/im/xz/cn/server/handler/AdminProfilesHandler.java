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

import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TimeUtil;
import im.xz.cn.util.UuidUtil;

import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminProfilesHandler {
    private final ProfileDao profileDao;
    private final UserDao userDao;
    private final AdminDao adminDao;

    public AdminProfilesHandler(ProfileDao profileDao, UserDao userDao, AdminDao adminDao) {
        this.profileDao = profileDao;
        this.userDao = userDao;
        this.adminDao = adminDao;
    }

    public void profilesPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAdminProfilesPage(adminUsername, adminRole, csrfToken));
    }

    public void getProfiles(Context ctx) {
        List<PlayerProfile> profiles = profileDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PlayerProfile p : profiles) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("userId", p.getUserId());
            User user = userDao.findById(p.getUserId());
            map.put("username", user != null ? user.getUsername() : "未知用户");
            map.put("skinModel", p.getSkinModel());
            map.put("createdAt", p.getCreatedAt());
            result.add(map);
        }
        ctx.json(result);
    }

    @SuppressWarnings("unchecked")
    public void createProfile(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String userId = body.get("userId");
        String name = body.get("name");

        if (userId == null || userId.isEmpty() || name == null || name.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        if (name.length() > 24) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称不能超过24个字符"));
            return;
        }
        if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称只能包含字母、数字、下划线、中文和连字符"));
            return;
        }

        User user = findUserByIdentifier(userId);
        if (user == null) {
            ctx.status(400).json(Map.of("success", false, "message", "用户不存在"));
            return;
        }

        if (profileDao.existsByName(name)) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称已存在"));
            return;
        }

        String id = UuidUtil.generateProfileUuid(name, "v4");
        String createdAt = TimeUtil.now();
        String yggdrasilToken = AuthService.generateYggdrasilToken();
        PlayerProfile profile = new PlayerProfile(id, user.getId(), name, null, null, "default", yggdrasilToken, createdAt);
        profileDao.insert(profile);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CREATE_PROFILE:" + name + "->" + user.getUsername(), IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "角色创建成功"));
    }

    @SuppressWarnings("unchecked")
    public void deleteProfile(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", "角色不存在"));
            return;
        }
        profileDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_PROFILE:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "角色已删除"));
    }

    @SuppressWarnings("unchecked")
    public void updateProfile(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String name = body.get("name");

        if (id == null || id.isEmpty() || name == null || name.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        if (name.length() > 24) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称不能超过24个字符"));
            return;
        }
        if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称只能包含字母、数字、下划线、中文和连字符"));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", "角色不存在"));
            return;
        }

        if (!profile.getName().equals(name) && profileDao.existsByName(name)) {
            ctx.status(400).json(Map.of("success", false, "message", "角色名称已存在"));
            return;
        }

        profileDao.updateName(id, name);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_PROFILE:" + id + "->" + name, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "角色名称已修改"));
    }

    @SuppressWarnings("unchecked")
    public void transferProfile(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String userId = body.get("userId");

        if (id == null || id.isEmpty() || userId == null || userId.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", "角色不存在"));
            return;
        }

        User user = findUserByIdentifier(userId);
        if (user == null) {
            ctx.status(400).json(Map.of("success", false, "message", "目标用户不存在"));
            return;
        }

        profileDao.updateUserId(id, user.getId());
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "TRANSFER_PROFILE:" + id + "->" + user.getId(), IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "所有权已转移"));
    }

    @SuppressWarnings("unchecked")
    public void clearProfileTextures(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");

        if (id == null || id.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }

        PlayerProfile profile = profileDao.findById(id);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "message", "角色不存在"));
            return;
        }

        profileDao.clearTextures(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CLEAR_PROFILE_TEXTURES:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "皮肤与披风已清除"));
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
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return false;
        Admin admin = adminDao.findById(adminId);
        return admin != null && admin.getRole() == AdminRole.ROOT;
    }
}
