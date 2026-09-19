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
package im.xz.cn.server.handler.user;


import im.xz.cn.i18n.I18n;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.database.dao.FriendSharedTextureDao;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.database.dao.TextureDao;
import im.xz.cn.database.dao.TextureFavoriteDao;
import im.xz.cn.database.dao.TextureVisibilityDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.mail.MailService;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.texture.TextureService;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.security.PasswordValidator;
import im.xz.cn.common.VerificationCode;
import im.xz.cn.web.view.UserPage;
import im.xz.cn.web.Shared;

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
    private final TextureFavoriteDao favoriteDao;
    private final FriendSharedTextureDao friendSharedDao;
    private final TextureVisibilityDao visibilityDao;
    private final im.xz.cn.database.dao.FriendDao friendDao;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserDashboardHandler(AuthService authService, UserDao userDao, ProfileDao profileDao,
                                 TextureDao textureDao, TextureService textureService,
                                 CacheDao cacheDao, MailService mailService, SystemConfig sysConfig,
                                 TextureFavoriteDao favoriteDao, FriendSharedTextureDao friendSharedDao,
                                 TextureVisibilityDao visibilityDao, im.xz.cn.database.dao.FriendDao friendDao) {
        this.authService = authService;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.cacheDao = cacheDao;
        this.mailService = mailService;
        this.sysConfig = sysConfig;
        this.favoriteDao = favoriteDao;
        this.friendSharedDao = friendSharedDao;
        this.visibilityDao = visibilityDao;
        this.friendDao = friendDao;
    }

    public User checkAuth(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.redirect("/login");
            return null;
        }
        User user = userDao.findById(userId);
        if (user == null || !im.xz.cn.security.UserPermissions.isAccessible()) {
            SessionManager.invalidate(ctx);
            ctx.status(403);
            ctx.html(UserPage.bannedPage());
            return null;
        }
        if (!SessionManager.validateClientFingerprint(ctx)) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }

    public void handleDashboardPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        List<PlayerProfile> profiles = profileDao.findByUserId(user.getId());
        int skinCount = textureDao.countByUserId(user.getId(), "SKIN");
        int capeCount = textureDao.countByUserId(user.getId(), "CAPE");
        int publicCount = visibilityDao.countPublicByUserId(user.getId());
        int friendCount = friendDao.countByUserId(user.getId());
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderDashboardPage(
                csrfToken, sysConfig.getSiteName(),
                user.getDisplayName(), user.getUsername(), user.getEmail(),
                user.isEmailVerified(), user.getCreatedAt(),
                sysConfig.getApiDomain(), profiles.size(), skinCount, capeCount, publicCount, friendCount));
    }

    public void handleSettingsPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderSettingsPage(
                csrfToken, sysConfig.getSiteName(),
                user.getDisplayName(), user.getEmail()));
    }

    public void handleLogsPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        String log = im.xz.cn.logging.UserActionLogger.read(user.getId());
        long used = im.xz.cn.logging.UserActionLogger.sizeBytes(user.getId());
        long max = Math.max(1, sysConfig.getUserActionLogMaxKib()) * 1024L;
        int percent = (int) Math.min(100, Math.round(used * 100.0 / max));
        ctx.html(UserPage.renderLogsPage(csrfToken, sysConfig.getSiteName(), log, used, max, percent));
    }

    public void handleDownloadLog(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        int intervalMin = sysConfig.getUserActionLogDownloadIntervalMinutes();
        String key = "userlog_dl:" + user.getId();
        if (intervalMin > 0) {
            String last = cacheDao.get(key);
            if (last != null) {
                try {
                    long waitMs = intervalMin * 60_000L - (System.currentTimeMillis() - Long.parseLong(last));
                    if (waitMs > 0) {
                        long hours = waitMs / 3600_000L;
                        long minutes = (waitMs % 3600_000L) / 60_000L;
                        ctx.status(429).json(Map.of("success", false,
                                "message", I18n.t("msg.logDownloadLimited", hours + "h " + minutes + "m")));
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            cacheDao.put(key, String.valueOf(System.currentTimeMillis()), "userlog", intervalMin * 60);
        }
        byte[] data = im.xz.cn.logging.UserActionLogger.read(user.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ctx.header("Content-Disposition", "attachment; filename=\"user-log-" + user.getUsername() + ".txt\"");
        ctx.contentType("text/plain; charset=utf-8");
        ctx.result(data);
    }

    public void handleClearLog(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        int intervalMin = sysConfig.getUserActionLogClearIntervalMinutes();
        String key = "userlog_clr:" + user.getId();
        if (intervalMin > 0) {
            String last = cacheDao.get(key);
            if (last != null) {
                try {
                    long waitMs = intervalMin * 60_000L - (System.currentTimeMillis() - Long.parseLong(last));
                    if (waitMs > 0) {
                        long hours = waitMs / 3600_000L;
                        long minutes = (waitMs % 3600_000L) / 60_000L;
                        ctx.status(429).json(Map.of("success", false,
                                "message", I18n.t("msg.logClearLimited", hours + "h " + minutes + "m")));
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            cacheDao.put(key, String.valueOf(System.currentTimeMillis()), "userlog", intervalMin * 60);
        }
        im.xz.cn.logging.UserActionLogger.clear(user.getId());
        jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.logCleared")));
    }

    public void handleProfilesPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderProfilesPage(csrfToken, sysConfig.getSiteName(), sysConfig.getApiDomain()));
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.nicknameEmpty")));
                return;
            }
            if (nickname.length() > 32) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.nicknameTooLong")));
                return;
            }
            userDao.updateNickname(user.getId(), nickname);
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.nicknameUpdated")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.emailInvalid")));
                return;
            }
            if (password == null || password.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.currentPasswordRequired")));
                return;
            }
            if (verifyCode == null || verifyCode.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeRequired")));
                return;
            }
            if (!Argon2Hasher.verify(password, user.getPasswordHash())) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.passwordIncorrect")));
                return;
            }
            String emailAttemptKey = "verify_attempts_settings:" + user.getId() + ":email_change";
            String emailAttemptsStr = cacheDao.get(emailAttemptKey);
            int emailAttempts = (emailAttemptsStr != null) ? Integer.parseInt(emailAttemptsStr) : 0;
            if (emailAttempts >= 5) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooMany")));
                return;
            }
            cacheDao.put(emailAttemptKey, String.valueOf(emailAttempts + 1), "verify_attempt", 120);

            String cacheKey = "settings_code:" + user.getId() + ":email_change";
            String cached = cacheDao.get(cacheKey);
            if (cached == null || !cached.equals(verifyCode)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeExpired")));
                return;
            }
            cacheDao.delete(emailAttemptKey);
            if (userDao.findByEmail(newEmail) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.emailInUse")));
                return;
            }
            if (!sysConfig.isEmailDomainAllowed(newEmail)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.emailDomainNotAllowed")));
                return;
            }
            userDao.updateEmail(user.getId(), newEmail);
            cacheDao.delete(cacheKey);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), IpUtil.getClientIp(ctx), "email");
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.emailUpdated")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.currentPasswordRequired")));
                return;
            }
            String passwordError = PasswordValidator.validateUser(newPassword);
            if (passwordError != null) {
                jsonResponse(ctx, Map.of("success", false, "message", passwordError));
                return;
            }
            if (verifyCode == null || verifyCode.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeRequired")));
                return;
            }
            if (!Argon2Hasher.verify(currentPassword, user.getPasswordHash())) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.currentPasswordIncorrect")));
                return;
            }
            String passAttemptKey = "verify_attempts_settings:" + user.getId() + ":password_change";
            String passAttemptsStr = cacheDao.get(passAttemptKey);
            int passAttempts = (passAttemptsStr != null) ? Integer.parseInt(passAttemptsStr) : 0;
            if (passAttempts >= 5) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooMany")));
                return;
            }
            cacheDao.put(passAttemptKey, String.valueOf(passAttempts + 1), "verify_attempt", 120);

            String cacheKey = "settings_code:" + user.getId() + ":password_change";
            String cached = cacheDao.get(cacheKey);
            if (cached == null || !cached.equals(verifyCode)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeExpired")));
                return;
            }
            cacheDao.delete(passAttemptKey);
            userDao.updatePassword(user.getId(), Argon2Hasher.hash(newPassword));
            cacheDao.delete(cacheKey);
            authService.invalidateAllUserTokens(user.getId());
            AuditLogger.logPasswordChange(user.getUsername(), IpUtil.getClientIp(ctx));
            im.xz.cn.logging.UserActionLogger.log(user.getId(), IpUtil.getClientIp(ctx), "password");
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.passwordUpdated")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleSendSettingsCode(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String reqType = "";
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String type = (String) body.get("type");
            reqType = type;
            if ((!"email_change".equals(type) && !"password_change".equals(type))) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.invalidOperation")));
                return;
            }
            String resendKey = "settings_resend_cooldown:" + user.getId() + ":" + type;
            if (cacheDao.get(resendKey) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooFrequent")));
                return;
            }
            String failKey = resendKey + ":fail";
            if (cacheDao.get(failKey) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.sendFailedRetry")));
                return;
            }

            String dailyKey = "verify_daily:" + user.getEmail() + ":" + java.time.LocalDate.now();
            int dailyCount = cacheDao.incrementAndGet(dailyKey, "verify_daily", 86400);
            if (dailyCount > 5) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.dailyCodeLimit")));
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
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.codeSent")));
        } catch (Exception e) {
            if (!reqType.isEmpty()) {
                String rk = "settings_resend_cooldown:" + user.getId() + ":" + reqType;
                cacheDao.delete(rk);
                cacheDao.put(rk + ":fail", "1", "cooldown", 3);
            }
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleGetTheme(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String theme = user.getTheme();
        if (theme == null || theme.isBlank()) theme = "light";
        jsonResponse(ctx, Map.of("success", true, "theme", theme));
    }

    public void handleSetTheme(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {});
            String theme = (String) body.get("theme");
            if (theme == null || (!theme.equals("light") && !theme.equals("dark"))) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.invalidTheme")));
                return;
            }
            userDao.updateTheme(user.getId(), theme);
            jsonResponse(ctx, Map.of("success", true, "theme", theme));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleGetLanguage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String language = user.getLanguage();
        if (language == null || language.isBlank()) language = "zh-CN";
        jsonResponse(ctx, Map.of("success", true, "language", language));
    }

    public void handleSetLanguage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {});
            String language = (String) body.get("language");
            String normalized = im.xz.cn.i18n.LocaleResolver.normalize(language);
            if (language == null || !im.xz.cn.i18n.LocaleResolver.isSupported(normalized)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.invalidLanguage")));
                return;
            }
            userDao.updateLanguage(user.getId(), normalized);
            SessionManager.setLanguage(ctx, normalized);
            jsonResponse(ctx, Map.of("success", true, "language", normalized));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameEmpty")));
                return;
            }
            if (name.length() > 16) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameTooLong")));
                return;
            }
            if (!name.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameInvalid")));
                return;
            }
            if (sysConfig.isProfileNameBlacklisted(name)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameTaken")));
                return;
            }
            if (profileDao.existsByName(name)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameExists")));
                return;
            }
            int maxProfiles = sysConfig.getMaxProfilesPerUser();
            if (profileDao.countByUserId(user.getId()) >= maxProfiles) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.maxProfilesReachedParam", maxProfiles)));
                return;
            }
            String uuid = UuidUtil.generateProfileUuid(name, sysConfig.getUuidVersion());
            String createdAt = TimeUtil.now();
            String yggdrasilToken = AuthService.generateYggdrasilToken();
            PlayerProfile profile = new PlayerProfile(uuid, user.getId(), name, null, null, "default", yggdrasilToken, createdAt);
            profileDao.insert(profile);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), IpUtil.getClientIp(ctx), "profile");
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.profileCreated"), "profile",
                    Map.of("id", profile.getId(), "name", name, "uuid", uuid)));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileIdMissing")));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNotFound")));
                return;
            }
            profileDao.delete(id);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), IpUtil.getClientIp(ctx), "profile");
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.profileDeleted")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileIdMissing")));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNotFound")));
                return;
            }
            String newName = (String) body.get("name");
            String skinModel = (String) body.get("skinModel");
            String skinHash = (String) body.get("skinHash");
            String capeHash = (String) body.get("capeHash");
            if (newName != null && !newName.isBlank()) {
                if (newName.length() > 16) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameTooLong")));
                    return;
                }
                if (!newName.matches("^[a-zA-Z0-9_\u4e00-\u9fa5-]+$")) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameInvalid")));
                    return;
                }
                if (sysConfig.isProfileNameBlacklisted(newName)) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameTaken")));
                    return;
                }
                if (!newName.equals(profile.getName()) && profileDao.existsByName(newName)) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNameExists")));
                    return;
                }
                profile.setName(newName);
            }
            if (skinModel != null && !skinModel.isBlank()) {
                profile.setSkinModel(skinModel);
            }
            if (skinHash != null && !skinHash.isBlank()) {
                if (!canReferenceTexture(user, "SKIN", skinHash)) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.noSkinAccess")));
                    return;
                }
                profile.setSkinUrl(textureService.getPublicUrl("SKIN", skinHash));
            } else if (skinHash != null && skinHash.isEmpty()) {
                profile.setSkinUrl(null);
            }
            if (capeHash != null && !capeHash.isBlank()) {
                if (!canReferenceTexture(user, "CAPE", capeHash)) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.noCapeAccess")));
                    return;
                }
                profile.setCapeUrl(textureService.getPublicUrl("CAPE", capeHash));
            } else if (capeHash != null && capeHash.isEmpty()) {
                profile.setCapeUrl(null);
            }
            profileDao.update(profile);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), IpUtil.getClientIp(ctx), "profile");
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.profileUpdated")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleListProfiles(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<PlayerProfile> profiles = profileDao.findByUserId(user.getId());
        ctx.json(Map.of("success", true, "profiles", profiles.stream().map(p -> {
            String skinName = "";
            String capeName = "";
            String skinHash = "";
            String capeHash = "";
            String skinOriginal = "";
            String capeOriginal = "";
            String skinSource = "none";
            String capeSource = "none";
            String skinUrl = p.getSkinUrl() != null ? p.getSkinUrl() : "";
            String capeUrl = p.getCapeUrl() != null ? p.getCapeUrl() : "";
            if (!skinUrl.isEmpty()) {
                String hash = skinUrl.substring(Math.max(0, skinUrl.length() - 64));
                skinHash = hash;
                Texture tex = textureDao.findByUserAndHash(user.getId(), "SKIN", hash);
                if (tex == null) tex = textureDao.findByHash("SKIN", hash);
                if (tex != null) {
                    if (tex.getAlias() != null) skinName = tex.getAlias();
                    skinOriginal = tex.getOriginalName() != null ? tex.getOriginalName() : "";
                    skinSource = tex.getReferenceType() != null ? tex.getReferenceType() : "self";
                }
            }
            if (!capeUrl.isEmpty()) {
                String hash = capeUrl.substring(Math.max(0, capeUrl.length() - 64));
                capeHash = hash;
                Texture tex = textureDao.findByUserAndHash(user.getId(), "CAPE", hash);
                if (tex == null) tex = textureDao.findByHash("CAPE", hash);
                if (tex != null) {
                    if (tex.getAlias() != null) capeName = tex.getAlias();
                    capeOriginal = tex.getOriginalName() != null ? tex.getOriginalName() : "";
                    capeSource = tex.getReferenceType() != null ? tex.getReferenceType() : "self";
                }
            }
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("skinModel", p.getSkinModel() != null ? p.getSkinModel() : "default");
            m.put("skinUrl", skinUrl);
            m.put("capeUrl", capeUrl);
            m.put("skinHash", skinHash);
            m.put("capeHash", capeHash);
            m.put("skinName", skinName);
            m.put("capeName", capeName);
            m.put("skinOriginal", skinOriginal);
            m.put("capeOriginal", capeOriginal);
            m.put("skinSource", skinSource);
            m.put("capeSource", capeSource);
            m.put("yggdrasilToken", p.getYggdrasilToken() != null ? p.getYggdrasilToken() : "");
            m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt() : "");
            return m;
        }).toList()));
    }

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
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileIdMissing")));
                return;
            }
            PlayerProfile profile = profileDao.findById(id);
            if (profile == null || !profile.getUserId().equals(user.getId())) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.profileNotFound")));
                return;
            }
            String newToken = AuthService.generateYggdrasilToken();
            profileDao.updateToken(id, newToken);
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.tokenRegenerated"), "token", newToken));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleMyTextures(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<Texture> skins = textureDao.findSelfUploaded(user.getId(), "SKIN");
        List<Texture> capes = textureDao.findSelfUploaded(user.getId(), "CAPE");

        java.util.List<java.util.Map<String, Object>> favoriteSkins = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> favoriteCapes = new java.util.ArrayList<>();
        var favorites = favoriteDao.findByUserId(user.getId());
        for (var f : favorites) {
            String type = String.valueOf(f.get("type"));
            var item = new java.util.LinkedHashMap<String, Object>();
            item.put("id", String.valueOf(f.get("texture_id")));
            item.put("hash", String.valueOf(f.get("hash")));
            String fa = (String) f.get("alias");
            String ta = (String) f.get("texture_alias");
            String on = (String) f.get("original_name");
            String hash = String.valueOf(f.get("hash"));
            item.put("alias", displayAlias(fa, ta, on, hash, I18n.t("msg.aliasFavorite")));
            item.put("source", "favorite");
            if ("SKIN".equals(type)) favoriteSkins.add(item);
            else if ("CAPE".equals(type)) favoriteCapes.add(item);
        }

        java.util.List<java.util.Map<String, Object>> sharedSkins = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> sharedCapes = new java.util.ArrayList<>();
        var shared = friendSharedDao.findSharedByFriends(user.getId());
        for (var s : shared) {
            String type = String.valueOf(s.get("type"));
            var item = new java.util.LinkedHashMap<String, Object>();
            item.put("id", String.valueOf(s.get("texture_id")));
            item.put("hash", String.valueOf(s.get("hash")));
            String ta = (String) s.get("texture_alias");
            String on = (String) s.get("original_name");
            String hash = String.valueOf(s.get("hash"));
            item.put("alias", displayAlias(null, ta, on, hash, I18n.t("msg.aliasShared")));
            item.put("source", "shared");
            if ("SKIN".equals(type)) sharedSkins.add(item);
            else if ("CAPE".equals(type)) sharedCapes.add(item);
        }

        ctx.json(Map.of("success", true,
            "skins", skins.stream().map(t -> Map.of(
                "id", t.getId(), "hash", t.getHash(), "alias", t.getAlias() != null && !t.getAlias().isBlank() ? t.getAlias() : t.getHash(), "source", "mine"
            )).toList(),
            "capes", capes.stream().map(t -> Map.of(
                "id", t.getId(), "hash", t.getHash(), "alias", t.getAlias() != null && !t.getAlias().isBlank() ? t.getAlias() : t.getHash(), "source", "mine"
            )).toList(),
            "favoriteSkins", favoriteSkins,
            "favoriteCapes", favoriteCapes,
            "sharedSkins", sharedSkins,
            "sharedCapes", sharedCapes
        ));
    }

    private static String displayAlias(String favAlias, String texAlias, String origName, String hash, String prefix) {
        String alias = favAlias;
        if (alias == null || alias.isBlank()) alias = texAlias;
        if (alias == null || alias.isBlank()) alias = origName;
        if (alias == null || alias.isBlank()) {
            String safeHash = (hash != null && !"null".equals(hash) && hash.length() >= 8) ? hash.substring(0, 8) : I18n.t("msg.unknown");
            alias = prefix + "-" + safeHash;
        }
        return alias;
    }

    private boolean canReferenceTexture(User user, String type, String hash) {
        if (textureDao.hasReference(user.getId(), type, hash)) return true;
        List<Texture> textures = textureDao.findAllByHash(type, hash);
        for (Texture tex : textures) {
            if (visibilityDao.isPublic(tex.getUserId(), tex.getId())) return true;
            if (friendSharedDao.exists(tex.getUserId(), user.getId(), tex.getId())) return true;
        }
        return false;
    }

    private void jsonResponse(Context ctx, Map<String, Object> data) {
        ctx.json(data);
    }
}
