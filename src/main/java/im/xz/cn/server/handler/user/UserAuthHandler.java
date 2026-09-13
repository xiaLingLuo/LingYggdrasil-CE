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
import im.xz.cn.auth.LoginRateLimiter;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.mail.MailService;
import im.xz.cn.model.User;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.security.PasswordValidator;
import im.xz.cn.common.VerificationCode;

import io.javalin.http.Context;

import java.util.Map;

public class UserAuthHandler {
    private final AuthService authService;
    private final UserDao userDao;
    private final CacheDao cacheDao;
    private final MailService mailService;
    private final SystemConfig sysConfig;
    private final ObjectMapper mapper = new ObjectMapper();
    private final LoginRateLimiter rateLimiter;

    public UserAuthHandler(AuthService authService, UserDao userDao, CacheDao cacheDao,
                           MailService mailService, SystemConfig sysConfig) {
        this.authService = authService;
        this.userDao = userDao;
        this.cacheDao = cacheDao;
        this.mailService = mailService;
        this.sysConfig = sysConfig;
        this.rateLimiter = new LoginRateLimiter(cacheDao);
    }

    public void handleLogin(Context ctx) {
        try {
            Map<String, Object> body = mapper.readValue(
                    ctx.body(),
                    new TypeReference< >() {}
            );
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.fillUsernamePassword")));
                return;
            }

            User user = userDao.findByUsernameOrEmail(username);
            if (user != null && !authService.isUserAccessible(user)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.accountBanned")));
                return;
            }

            String clientIp = IpUtil.getClientIp(ctx);
            String rateLimitMsg = rateLimiter.checkRateLimit(username, clientIp);
            if (rateLimitMsg != null) {
                jsonResponse(ctx, Map.of("success", false, "message", rateLimitMsg));
                return;
            }

            User authed = authService.authenticateUser(username, password, clientIp);
            if (authed == null) {
                AuditLogger.logLogin(username, clientIp, false);
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.usernamePasswordWrong")));
                return;
            }

            rateLimiter.recordSuccess(username);
            AuditLogger.logLogin(username, clientIp, true);
            im.xz.cn.logging.UserActionLogger.log(authed.getId(), clientIp, "login");

            SessionManager.invalidateAndRenewUser(ctx);
            SessionManager.setUserId(ctx, authed.getId());
            SessionManager.setLanguage(ctx, im.xz.cn.i18n.LocaleResolver.normalize(authed.getLanguage()));
            SessionManager.bindClientFingerprint(ctx);

            if (sysConfig.isEmailVerificationEnabled() && !authed.isEmailVerified()) {
                jsonResponse(ctx, Map.of("success", true, "redirect", "/email-required", "needEmailVerify", true));
                return;
            }

            jsonResponse(ctx, Map.of("success", true, "redirect", "/dashboard"));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    @SuppressWarnings("unchecked")
    public void handleRegister(Context ctx) {
        try {
            if (!sysConfig.isRegistrationEnabled()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.registrationClosed")));
                return;
            }

            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String username = (String) body.get("username");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String nickname = (String) body.get("nickname");
            String verifyCode = (String) body.get("verifyCode");

            if (username == null || username.length() < 3 || username.length() > 16) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.usernameLength")));
                return;
            }
            if (email == null || !isValidEmail(email)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.emailInvalid")));
                return;
            }
            if (password == null || password.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.passwordEmpty")));
                return;
            }
            String passwordError = PasswordValidator.validateUser(password);
            if (passwordError != null) {
                jsonResponse(ctx, Map.of("success", false, "message", passwordError));
                return;
            }
            if (userDao.findByUsername(username) != null || userDao.findByEmail(email) != null) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.usernameOrEmailInUse")));
                return;
            }

            if (sysConfig.isUsernameBlacklisted(username)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.usernameTaken")));
                return;
            }
            if (!sysConfig.isEmailDomainAllowed(email)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.emailDomainNotAllowed")));
                return;
            }

            String clientIp = IpUtil.getClientIp(ctx);
            int maxAccountsPerIp = sysConfig.getMaxAccountsPerIp();
            if (userDao.countByIp(clientIp) >= maxAccountsPerIp) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.maxAccountsPerIpReached", maxAccountsPerIp)));
                return;
            }

            if (sysConfig.isEmailVerificationEnabled()) {
                String cacheKey = "pending_reg:" + email;

                if (verifyCode != null && !verifyCode.isBlank()) {
                    String attemptKey = "verify_attempts:" + email;
                    String attemptsStr = cacheDao.get(attemptKey);
                    int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;
                    if (attempts >= 5) {
                        jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooMany")));
                        return;
                    }
                    cacheDao.put(attemptKey, String.valueOf(attempts + 1), "verify_attempt", 120);

                    String cached = cacheDao.get(cacheKey);
                    if (cached == null) {
                        jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeExpiredRegister")));
                        return;
                    }
                    Map<String, Object> pending = mapper.readValue(cached, Map.class);
                    String cachedCode = (String) pending.get("code");
                    if (!verifyCode.equals(cachedCode)) {
                        jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeWrong")));
                        return;
                    }
                    cacheDao.delete(attemptKey);

                    User user = authService.registerUserWithHashedPassword(
                            (String) pending.get("username"),
                            email,
                            (String) pending.get("password"),
                            (String) pending.get("nickname"),
                            clientIp
                    );
                    if (user == null) {
                        jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.registerFailed")));
                        return;
                    }
                    userDao.setEmailVerified(user.getId(), true);
                    cacheDao.delete(cacheKey);
                    jsonResponse(ctx, Map.of("success", true, "redirect", "/login"));
                } else {
                    String code = VerificationCode.generate();
                    String passwordHash = Argon2Hasher.hash(password);
                    Map<String, String> pending = Map.of(
                            "username", username, "email", email,
                            "password", passwordHash, "code", code,
                            "nickname", nickname != null ? nickname : ""
                    );
                    cacheDao.put(cacheKey, mapper.writeValueAsString(pending),
                            "pending_registration", VerificationCode.getExpirySeconds());

                    if (mailService.isEnabled()) {
                        mailService.sendVerificationCode(email, code);
                    }
                    jsonResponse(ctx, Map.of("success", false, "needVerify", true,
                            "message", I18n.t("msg.codeSentRegister")));
                }
            } else {
                User user = authService.registerUser(username, email, password, nickname, clientIp);
                if (user == null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.registerFailed")));
                    return;
                }
                jsonResponse(ctx, Map.of("success", true, "redirect", "/login"));
            }
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    @SuppressWarnings("unchecked")
    public void handleVerifyEmail(Context ctx) {
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String email = (String) body.get("email");
            String code = (String) body.get("code");
            String type = (String) body.getOrDefault("type", "registration");

            if (email == null || code == null || email.isBlank() || code.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.fillEmailCode")));
                return;
            }

            if ("registration".equals(type)) {
                String attemptKey = "verify_attempts:" + email;
                String attemptsStr = cacheDao.get(attemptKey);
                int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;
                if (attempts >= 5) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooMany")));
                    return;
                }
                cacheDao.put(attemptKey, String.valueOf(attempts + 1), "verify_attempt", 120);

                String cacheKey = "pending_reg:" + email;
                String cached = cacheDao.get(cacheKey);
                if (cached == null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.verifyInfoExpired")));
                    return;
                }

                Map<String, Object> pending = mapper.readValue(cached, Map.class);
                if (!code.equals(pending.get("code"))) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeWrong")));
                    return;
                }
                cacheDao.delete(attemptKey);

                String clientIp = IpUtil.getClientIp(ctx);
                int maxAccountsPerIp = sysConfig.getMaxAccountsPerIp();
                if (userDao.countByIp(clientIp) >= maxAccountsPerIp) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.maxAccountsPerIpReached", maxAccountsPerIp)));
                    return;
                }

                User user = authService.registerUserWithHashedPassword(
                        (String) pending.get("username"), email,
                        (String) pending.get("password"), (String) pending.get("nickname"),
                        clientIp
                );
                if (user == null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.registerFailed")));
                    return;
                }
                userDao.setEmailVerified(user.getId(), true);
                cacheDao.delete(cacheKey);
                jsonResponse(ctx, Map.of("success", true, "redirect", "/login"));
            } else {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.unsupportedVerifyType")));
            }
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    @SuppressWarnings("unchecked")
    public void handleResendCode(Context ctx) {
        String resendEmail = null;
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String email = (String) body.get("email");
            String type = (String) body.getOrDefault("type", "registration");
            resendEmail = email;

            if (email == null || email.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.provideEmail")));
                return;
            }

            if ("registration".equals(type)) {
                String cacheKey = "pending_reg:" + email;
                String cached = cacheDao.get(cacheKey);
                if (cached == null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.registerInfoExpired")));
                    return;
                }

                String resendKey = "resend_cooldown:" + email;
                if (cacheDao.get(resendKey) != null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooFrequent")));
                    return;
                }
                String failKey = resendKey + ":fail";
                if (cacheDao.get(failKey) != null) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.sendFailedRetry")));
                    return;
                }

                String dailyKey = "verify_daily:" + email + ":" + java.time.LocalDate.now();
                int dailyCount = cacheDao.incrementAndGet(dailyKey, "verify_daily", 86400);
                if (dailyCount > 5) {
                    jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.dailyCodeLimit")));
                    return;
                }

                Map<String, Object> pending = mapper.readValue(cached, Map.class);
                String newCode = VerificationCode.generate();
                pending.put("code", newCode);
                cacheDao.put(cacheKey, mapper.writeValueAsString(pending),
                        "pending_registration", VerificationCode.getExpirySeconds());

                if (mailService.isEnabled()) {
                    mailService.sendVerificationCode(email, newCode);
                }
                cacheDao.put(resendKey, "1", "cooldown", 300);
                jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.codeResent")));
            } else {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.unsupportedOperation")));
            }
        } catch (Exception e) {
            if (resendEmail != null) {
                cacheDao.delete("resend_cooldown:" + resendEmail);
                cacheDao.put("resend_cooldown:" + resendEmail + ":fail", "1", "cooldown", 3);
            }
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleSendEmailVerify(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        User user = userDao.findById(userId);
        if (user == null) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        if (user.isEmailVerified()) {
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.emailVerified")));
            return;
        }
        String resendKey = "email_verify_cooldown:" + userId;
        try {
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
            String cacheKey = "email_verify:" + userId;
            cacheDao.put(cacheKey, code, "email_verification", VerificationCode.getExpirySeconds());
            cacheDao.put(resendKey, "1", "cooldown", 300);

            if (mailService.isEnabled()) {
                mailService.sendVerificationCode(user.getEmail(), code);
            }
            jsonResponse(ctx, Map.of("success", true, "message", I18n.t("msg.codeSent")));
        } catch (Exception e) {
            cacheDao.delete(resendKey);
            cacheDao.put(resendKey + ":fail", "1", "cooldown", 3);
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.sendFailedRetry")));
        }
    }

    @SuppressWarnings("unchecked")
    public void handleVerifyMyEmail(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        User user = userDao.findById(userId);
        if (user == null) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.userNotFound")));
            return;
        }
        if (user.isEmailVerified()) {
            jsonResponse(ctx, Map.of("success", true, "redirect", "/dashboard", "message", I18n.t("msg.emailVerified")));
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), Map.class);
            String code = (String) body.get("code");
            if (code == null || code.isBlank()) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeRequired")));
                return;
            }

            String attemptKey = "email_verify_attempts:" + userId;
            String attemptsStr = cacheDao.get(attemptKey);
            int attempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;
            if (attempts >= 5) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeTooMany")));
                return;
            }
            cacheDao.put(attemptKey, String.valueOf(attempts + 1), "verify_attempt", 120);

            String cacheKey = "email_verify:" + userId;
            String cached = cacheDao.get(cacheKey);
            if (cached == null || !cached.equals(code)) {
                jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.codeExpired")));
                return;
            }

            cacheDao.delete(attemptKey);
            cacheDao.delete(cacheKey);
            userDao.setEmailVerified(userId, true);
            jsonResponse(ctx, Map.of("success", true, "redirect", "/dashboard", "message", I18n.t("msg.emailVerifySuccess")));
        } catch (Exception e) {
            jsonResponse(ctx, Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void handleLogout(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        String ip = IpUtil.getClientIp(ctx);
        AuditLogger.logLogout(userId != null ? userId : "unknown", ip);
        SessionManager.invalidate(ctx);
        ctx.redirect("/login");
    }

    private void jsonResponse(Context ctx, Map<String, Object> data) {
        ctx.json(data);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
