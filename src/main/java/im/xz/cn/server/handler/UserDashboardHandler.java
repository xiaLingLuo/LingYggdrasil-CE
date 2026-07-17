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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.CacheDao;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.database.TextureDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.mail.MailService;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.model.enums.UserRole;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TextureService;
import im.xz.cn.util.TimeUtil;
import im.xz.cn.util.UuidUtil;
import im.xz.cn.util.PasswordValidator;
import im.xz.cn.util.VerificationCode;
import im.xz.cn.something.web.UserPage;
import im.xz.cn.something.web.Shared;

import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class UserDashboardHandler {
    private final AuthService authService;
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final CacheDao cacheDao;
    private final MailService mailService;
    private final SystemConfig sysConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserDashboardHandler(AuthService authService, UserDao userDao, ProfileDao profileDao,
                                 TextureDao textureDao, TextureService textureService,
                                 CacheDao cacheDao, MailService mailService, SystemConfig sysConfig) {
        this.authService = authService;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.cacheDao = cacheDao;
        this.mailService = mailService;
        this.sysConfig = sysConfig;
    }

    public User checkAuth(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.redirect("/login");
            return null;
        }
        User user = userDao.findById(userId);
        if (user == null || user.getRole() == UserRole.BANNED) {
            SessionManager.invalidate(ctx);
            ctx.status(403);
            ctx.html(UserPage.bannedPage());
            return null;
        }
        // 会话指纹
        if (!SessionManager.validateClientFingerprint(ctx)) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }

    // GET /dashboard
    public void handleDashboardPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        List<PlayerProfile> profiles = profileDao.findByUserId(user.getId());
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderDashboardPage(
                csrfToken, sysConfig.getSiteName(),
                user.getDisplayName(), user.getUsername(), user.getEmail(),
                user.isEmailVerified(), user.getCreatedAt(),
                sysConfig.getApiDomain(), profiles.size()));
    }

    // GET /settings
    public void handleSettingsPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderSettingsPage(
                csrfToken, sysConfig.getSiteName(),
                user.getDisplayName(), user.getEmail()));
    }

    // GET /profiles
    public void handleProfilesPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderProfilesPage(csrfToken, sysConfig.getSiteName(), sysConfig.getApiDomain()));
    }

    // POST /api/settings/nickname
    public void handleChangeNickname(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String nickname = (String) body.get("nickname");
            if (nickname == null || nickname.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "昵称不能为空"));
                return;
            }
            if (nickname.length() > 32) {
                jsonResponse(ctx, Map.of("success", false, "message", "昵称最长32个字符"));
                return;
            }
            userDao.updateNickname(user.getId(), nickname);
            jsonResponse(ctx, Map.of("success", true, "message", "昵称已更新"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/settings/email
    @SuppressWarnings("unchecked")
    public void handleChangeEmail(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String newEmail = (String) body.get("newEmail");
            String password = (String) body.get("password");
            String verifyCode = (String) body.get("verifyCode");

            if (newEmail == null || !newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                jsonResponse(ctx, Map.of("success", false, "message", "邮箱格式不正确"));
                return;
            }
            if (password == null || password.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "请输入当前密码"));
                return;
            }
            if (verifyCode == null || verifyCode.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "请输入验证码"));
                return;
            }
            if (!Argon2Hasher.verify(password, user.getPasswordHash())) {
                jsonResponse(ctx, Map.of("success", false, "message", "密码不正确"));
                return;
            }
            String emailAttemptKey = "verify_attempts_settings:" + user.getId() + ":email_change";
            String emailAttemptsStr = cacheDao.get(emailAttemptKey);
            int emailAttempts = (emailAttemptsStr != null) ? Integer.parseInt(emailAttemptsStr) : 0;
            if (emailAttempts >= 5) {
                jsonResponse(ctx, Map.of("success", false, "message", "验证码尝试次数过多，请重新获取"));
                return;
            }
            cacheDao.put(emailAttemptKey, String.valueOf(emailAttempts + 1), "verify_attempt", 120);

            String cacheKey = "settings_code:" + user.getId() + ":email_change";
            String cached = cacheDao.get(cacheKey);
            if (cached == null || !cached.equals(verifyCode)) {
                jsonResponse(ctx, Map.of("success", false, "message", "验证码错误或已过期"));
                return;
            }
            cacheDao.delete(emailAttemptKey);
            if (userDao.findByEmail(newEmail) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", "该邮箱已被使用"));
                return;
            }
            if (!sysConfig.isEmailDomainAllowed(newEmail)) {
                jsonResponse(ctx, Map.of("success", false, "message", "该邮箱域名不允许注册"));
                return;
            }
            userDao.updateEmail(user.getId(), newEmail);
            cacheDao.delete(cacheKey);
            jsonResponse(ctx, Map.of("success", true, "message", "邮箱已更新"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/settings/password
    @SuppressWarnings("unchecked")
    public void handleChangePassword(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String currentPassword = (String) body.get("currentPassword");
            String newPassword = (String) body.get("newPassword");
            String verifyCode = (String) body.get("verifyCode");

            if (currentPassword == null || currentPassword.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "请输入当前密码"));
                return;
            }
            String passwordError = PasswordValidator.validate(newPassword);
            if (passwordError != null) {
                jsonResponse(ctx, Map.of("success", false, "message", passwordError));
                return;
            }
            if (verifyCode == null || verifyCode.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "请输入验证码"));
                return;
            }
            if (!Argon2Hasher.verify(currentPassword, user.getPasswordHash())) {
                jsonResponse(ctx, Map.of("success", false, "message", "当前密码不正确"));
                return;
            }
            String passAttemptKey = "verify_attempts_settings:" + user.getId() + ":password_change";
            String passAttemptsStr = cacheDao.get(passAttemptKey);
            int passAttempts = (passAttemptsStr != null) ? Integer.parseInt(passAttemptsStr) : 0;
            if (passAttempts >= 5) {
                jsonResponse(ctx, Map.of("success", false, "message", "验证码尝试次数过多，请重新获取"));
                return;
            }
            cacheDao.put(passAttemptKey, String.valueOf(passAttempts + 1), "verify_attempt", 120);

            String cacheKey = "settings_code:" + user.getId() + ":password_change";
            String cached = cacheDao.get(cacheKey);
            if (cached == null || !cached.equals(verifyCode)) {
                jsonResponse(ctx, Map.of("success", false, "message", "验证码错误或已过期"));
                return;
            }
            cacheDao.delete(passAttemptKey);
            userDao.updatePassword(user.getId(), Argon2Hasher.hash(newPassword));
            cacheDao.delete(cacheKey);
            authService.invalidateAllUserTokens(user.getId());
            AuditLogger.logPasswordChange(user.getUsername(), IpUtil.getClientIp(ctx));
            jsonResponse(ctx, Map.of("success", true, "message", "密码已更新，所有设备已登出"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/settings/send-verify-code
    public void handleSendSettingsCode(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String type = (String) body.get("type");
            if ((!"email_change".equals(type) && !"password_change".equals(type))) {
                jsonResponse(ctx, Map.of("success", false, "message", "无效的操作类型"));
                return;
            }
            String resendKey = "settings_resend_cooldown:" + user.getId() + ":" + type;
            if (cacheDao.get(resendKey) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", "验证码发送过于频繁，请稍后再试"));
                return;
            }
            String code = VerificationCode.generate();
            String cacheKey = "settings_code:" + user.getId() + ":" + type;
            cacheDao.put(cacheKey, code, "settings_verification", VerificationCode.getExpirySeconds());
            cacheDao.put(resendKey, "1", "cooldown", 300);

            if (mailService.isEnabled()) {
                if ("email_change".equals(type)) {
                    mailService.sendEmailChangeVerification(user.getEmail(), code);
                } else {
                    mailService.sendPasswordChangeVerification(user.getEmail(), code);
                }
            }
            jsonResponse(ctx, Map.of("success", true, "message", "验证码已发送到您的邮箱"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/profiles/create
    public void handleCreateProfile(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String name = (String) body.get("name");
            if (name == null || name.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色名称不能为空"));
                return;
            }
            if (name.length() > 16) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色名称最长16个字符"));
                return;
            }
            if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色名称只能包含字母、数字、下划线、中文和连字符"));
                return;
            }
            if (profileDao.existsByName(name)) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色名称已被使用"));
                return;
            }
            int maxProfiles = sysConfig.getMaxProfilesPerUser();
            if (profileDao.countByUserId(user.getId()) >= maxProfiles) {
                jsonResponse(ctx, Map.of("success", false, "message", "已达到最大角色数量限制（" + maxProfiles + " 个）"));
                return;
            }
            String uuid = UuidUtil.generateProfileUuid(name, sysConfig.getUuidVersion());
            String createdAt = TimeUtil.now();
            String yggdrasilToken = AuthService.generateYggdrasilToken();
            PlayerProfile profile = new PlayerProfile(uuid, user.getId(), name, null, null, "default", yggdrasilToken, createdAt);
            profileDao.insert(profile);
            jsonResponse(ctx, Map.of("success", true, "message", "角色创建成功", "profile",
                    Map.of("id", profile.getId(), "name", name, "uuid", uuid)));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/profiles/delete
    public void handleDeleteProfile(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String id = (String) body.get("id");
            if (id == null || id.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "缺少角色ID"));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色不存在或无权操作"));
                return;
            }
            profileDao.delete(id);
            jsonResponse(ctx, Map.of("success", true, "message", "角色已删除"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // POST /api/profiles/update
    public void handleUpdateProfile(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String id = (String) body.get("id");
            if (id == null || id.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "缺少角色ID"));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色不存在或无权操作"));
                return;
            }
            String newName = (String) body.get("name");
            String skinModel = (String) body.get("skinModel");
            String skinHash = (String) body.get("skinHash");
            String capeHash = (String) body.get("capeHash");
            if (newName != null && !newName.isBlank()) {
                if (newName.length() > 16) {
                    jsonResponse(ctx, Map.of("success", false, "message", "角色名称最长16个字符"));
                    return;
                }
                if (!newName.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
                    jsonResponse(ctx, Map.of("success", false, "message", "角色名称只能包含字母、数字、下划线、中文和连字符"));
                    return;
                }
                if (!newName.equals(profile.getName()) && profileDao.existsByName(newName)) {
                    jsonResponse(ctx, Map.of("success", false, "message", "角色名称已被使用"));
                    return;
                }
                profile.setName(newName);
            }
            if (skinModel != null && !skinModel.isBlank()) {
                profile.setSkinModel(skinModel);
            }
            if (skinHash != null && !skinHash.isBlank()) {
                profile.setSkinUrl(textureService.getPublicUrl("SKIN", skinHash));
            } else if (skinHash != null && skinHash.isEmpty()) {
                profile.setSkinUrl(null);
            }
            if (capeHash != null && !capeHash.isBlank()) {
                profile.setCapeUrl(textureService.getPublicUrl("CAPE", capeHash));
            } else if (capeHash != null && capeHash.isEmpty()) {
                profile.setCapeUrl(null);
            }
            profileDao.update(profile);
            jsonResponse(ctx, Map.of("success", true, "message", "角色已更新"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // GET /api/profiles
    public void handleListProfiles(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<PlayerProfile> profiles = profileDao.findByUserId(user.getId());
        ctx.json(Map.of("success", true, "profiles", profiles.stream().map(p -> {
            String skinName = "";
            String capeName = "";
            String skinUrl = p.getSkinUrl() != null ? p.getSkinUrl() : "";
            String capeUrl = p.getCapeUrl() != null ? p.getCapeUrl() : "";
            if (!skinUrl.isEmpty()) {
                String hash = skinUrl.substring(Math.max(0, skinUrl.length() - 64));
                Texture tex = textureDao.findByHash("SKIN", hash);
                if (tex != null && tex.getAlias() != null) skinName = tex.getAlias();
            }
            if (!capeUrl.isEmpty()) {
                String hash = capeUrl.substring(Math.max(0, capeUrl.length() - 64));
                Texture tex = textureDao.findByHash("CAPE", hash);
                if (tex != null && tex.getAlias() != null) capeName = tex.getAlias();
            }
            return Map.of(
                "id", p.getId(), "name", p.getName(),
                "skinModel", p.getSkinModel() != null ? p.getSkinModel() : "default",
                "skinUrl", skinUrl,
                "capeUrl", capeUrl,
                "skinName", skinName,
                "capeName", capeName,
                "yggdrasilToken", p.getYggdrasilToken() != null ? p.getYggdrasilToken() : "",
                "createdAt", p.getCreatedAt() != null ? p.getCreatedAt() : ""
            );
        }).toList()));
    }

    // POST /api/profiles/regenerate-token
    public void handleRegenerateToken(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String id = (String) body.get("id");
            if (id == null || id.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", "缺少角色ID"));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", "角色不存在或无权操作"));
                return;
            }
            String newToken = AuthService.generateYggdrasilToken();
            profileDao.updateToken(id, newToken);
            jsonResponse(ctx, Map.of("success", true, "message", "Token 已重新生成", "token", newToken));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", "请求格式错误"));
        }
    }

    // GET /api/textures/my
    public void handleMyTextures(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<Texture> skins = textureDao.findByUserId(user.getId(), "SKIN");
        List<Texture> capes = textureDao.findByUserId(user.getId(), "CAPE");
        ctx.json(Map.of("success", true,
            "skins", skins.stream().map(t -> Map.of(
                "id", t.getId(), "hash", t.getHash(), "alias", t.getAlias() != null ? t.getAlias() : t.getHash()
            )).toList(),
            "capes", capes.stream().map(t -> Map.of(
                "id", t.getId(), "hash", t.getHash(), "alias", t.getAlias() != null ? t.getAlias() : t.getHash()
            )).toList()
        ));
    }

    private void jsonResponse(Context ctx, Map<String, Object> data) {
        ctx.json(data);
    }
}
