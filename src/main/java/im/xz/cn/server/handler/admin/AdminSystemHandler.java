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


import im.xz.cn.i18n.LocaleResolver;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.mail.MailService;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.TokenDao;
import im.xz.cn.model.Admin;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSystemHandler {
    private final SystemConfig systemConfig;
    private final CacheDao cacheDao;
    private final TokenDao tokenDao;
    private final DatabaseManager db;
    private final AdminDao adminDao;
    private final im.xz.cn.database.dao.RootInfoDao rootInfoDao;

    public AdminSystemHandler(SystemConfig systemConfig, CacheDao cacheDao, TokenDao tokenDao, DatabaseManager db) {
        this.systemConfig = systemConfig;
        this.cacheDao = cacheDao;
        this.tokenDao = tokenDao;
        this.db = db;
        this.adminDao = new AdminDao(db);
        this.rootInfoDao = new im.xz.cn.database.dao.RootInfoDao(db);
    }

    public void systemPage(Context ctx) {
        String adminUsername = getAdminUsername(ctx);
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSystemPage(adminUsername, adminRole, csrfToken));
    }

    private boolean isRoot(Context ctx) {
        return SessionManager.isAdminRoot(ctx);
    }

    private boolean adminAccessible(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return false;
        if (SessionManager.isAdminRoot(ctx)) {
            return rootInfoDao.findById(adminId) != null;
        }
        return adminDao.findById(adminId) != null;
    }

    public void handleGetTheme(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null || !adminAccessible(ctx)) {
            ctx.status(401).json(Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        String theme;
        if (isRoot(ctx)) {
            im.xz.cn.model.RootInfo r = rootInfoDao.findById(adminId);
            theme = (r != null && r.getTheme() != null && !r.getTheme().isBlank()) ? r.getTheme() : "light";
        } else {
            Admin admin = adminDao.findById(adminId);
            theme = (admin != null && admin.getTheme() != null && !admin.getTheme().isBlank())
                    ? admin.getTheme() : "light";
        }
        ctx.json(Map.of("success", true, "theme", theme));
    }

    @SuppressWarnings("unchecked")
    public void handleSetTheme(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null || !adminAccessible(ctx)) {
            ctx.status(401).json(Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String theme = body.get("theme");
        if (theme == null || (!theme.equals("light") && !theme.equals("dark"))) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidTheme")));
            return;
        }
        if (isRoot(ctx)) {
            rootInfoDao.updateTheme(adminId, theme);
        } else {
            adminDao.updateTheme(adminId, theme);
        }
        ctx.json(Map.of("success", true, "theme", theme));
    }

    public void handleGetLanguage(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (!adminAccessible(ctx)) {
            ctx.status(401).json(Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        String language;
        if (isRoot(ctx)) {
            im.xz.cn.model.RootInfo r = adminId != null ? rootInfoDao.findById(adminId) : null;
            language = (r != null && r.getLanguage() != null && !r.getLanguage().isBlank()) ? r.getLanguage() : "zh-CN";
        } else {
            Admin admin = adminId != null ? adminDao.findById(adminId) : null;
            language = (admin != null && admin.getLanguage() != null && !admin.getLanguage().isBlank())
                    ? admin.getLanguage() : "zh-CN";
        }
        ctx.json(Map.of("success", true, "language", language));
    }

    @SuppressWarnings("unchecked")
    public void handleSetLanguage(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null || !adminAccessible(ctx)) {
            ctx.status(401).json(Map.of("success", false, "message", I18n.t("msg.notLoggedIn")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String language = body.get("language");
        String normalized = LocaleResolver.normalize(language);
        if (language == null || !LocaleResolver.isSupported(normalized)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidLanguage")));
            return;
        }
        if (isRoot(ctx)) {
            rootInfoDao.updateLanguage(adminId, normalized);
        } else {
            adminDao.updateLanguage(adminId, normalized);
        }
        SessionManager.setLanguage(ctx, normalized);
        ctx.json(Map.of("success", true, "language", normalized));
    }

    public void getSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.view")) return;
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("siteName", systemConfig.getSiteName());
        settings.put("siteDescription", systemConfig.getSiteDescription());
        settings.put("registrationEnabled", systemConfig.isRegistrationEnabled());
        settings.put("emailVerificationEnabled", systemConfig.isEmailVerificationEnabled());
        settings.put("treasureEnabled", systemConfig.isTreasureEnabled());
        settings.put("userActionLogEnabled", systemConfig.isUserActionLogEnabled());
        settings.put("userActionLogMaxKib", systemConfig.getUserActionLogMaxKib());
        settings.put("userActionLogRetentionDays", systemConfig.getUserActionLogRetentionDays());
        settings.put("userActionLogActions", systemConfig.getUserActionLogActions());
        settings.put("userActionLogDownloadIntervalMinutes", systemConfig.getUserActionLogDownloadIntervalMinutes());
        settings.put("userActionLogClearIntervalMinutes", systemConfig.getUserActionLogClearIntervalMinutes());
        settings.put("uuidVersion", systemConfig.getUuidVersion());

        settings.put("userDomain", systemConfig.getUserDomain());
        settings.put("adminDomain", systemConfig.getAdminDomain());
        settings.put("apiDomain", systemConfig.getApiDomain());
        settings.put("commonDomain", systemConfig.getCommonDomain());

        settings.put("usernameBlacklist", systemConfig.getUsernameBlacklist());
        settings.put("usernameBlacklistCaseSensitive", systemConfig.isUsernameBlacklistCaseSensitive());
        settings.put("profileNameBlacklist", systemConfig.getProfileNameBlacklist());
        settings.put("profileNameBlacklistCaseSensitive", systemConfig.isProfileNameBlacklistCaseSensitive());
        settings.put("skinNameBlacklist", systemConfig.getSkinNameBlacklist());
        settings.put("skinNameBlacklistCaseSensitive", systemConfig.isSkinNameBlacklistCaseSensitive());
        settings.put("capeNameBlacklist", systemConfig.getCapeNameBlacklist());
        settings.put("capeNameBlacklistCaseSensitive", systemConfig.isCapeNameBlacklistCaseSensitive());
        settings.put("emailDomainList", systemConfig.getEmailDomainList());
        settings.put("emailDomainMode", systemConfig.getEmailDomainMode());

        settings.put("icpRecord", systemConfig.getIcpRecord());
        settings.put("publicSecurityRecord", systemConfig.getPublicSecurityRecord());

        settings.put("skinMaxSize", systemConfig.getSkinMaxSize());
        settings.put("skinMaxCount", systemConfig.getSkinMaxCount());
        settings.put("skinMaxTotalSize", systemConfig.getSkinMaxTotalSize());
        settings.put("skinRateLimit", systemConfig.getSkinRateLimit());
        settings.put("skinStoragePath", systemConfig.getSkinStoragePath());
        settings.put("capeMaxSize", systemConfig.getCapeMaxSize());
        settings.put("capeMaxCount", systemConfig.getCapeMaxCount());
        settings.put("capeMaxTotalSize", systemConfig.getCapeMaxTotalSize());
        settings.put("capeRateLimit", systemConfig.getCapeRateLimit());
        settings.put("capeStoragePath", systemConfig.getCapeStoragePath());
        settings.put("allowDownloadSkin", systemConfig.isAllowDownloadSkin());
        settings.put("allowDownloadCape", systemConfig.isAllowDownloadCape());

        settings.put("maxProfilesPerUser", systemConfig.getMaxProfilesPerUser());
        settings.put("minProfileNameLength", systemConfig.getMinProfileNameLength());
        settings.put("maxProfileNameLength", systemConfig.getMaxProfileNameLength());
        settings.put("maxAccountsPerIp", systemConfig.getMaxAccountsPerIp());
        settings.put("maxBlockedUsers", systemConfig.getMaxBlockedUsers());
        settings.put("maxFavorites", systemConfig.getMaxFavorites());
        settings.put("announcementMode", systemConfig.getAnnouncementMode());
        settings.put("announcementScope", systemConfig.getAnnouncementScope());
        settings.put("announcementContent", systemConfig.getAnnouncementContent());

        MailConfig mail = AppConfig.getInstance().getMailConfig();
        settings.put("mailEnabled", mail.isEnabled());
        settings.put("mailHost", mail.getHost() != null ? mail.getHost() : "");
        settings.put("mailPort", mail.getPort());
        settings.put("mailUsername", mail.getUsername() != null ? mail.getUsername() : "");
        settings.put("mailFrom", mail.getFrom() != null ? mail.getFrom() : "");
        settings.put("mailTls", mail.isTls());
        settings.put("mailTemplateVerify", systemConfig.getMailTemplateVerify());
        settings.put("mailTemplateEmailChange", systemConfig.getMailTemplateEmailChange());
        settings.put("mailTemplatePasswordChange", systemConfig.getMailTemplatePasswordChange());
        settings.put("mailTestContent", systemConfig.getMailTestContent());

        ctx.json(settings);
    }

    @SuppressWarnings("unchecked")
    public void updateSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String key = body.get("key");
        String value = body.get("value");

        if (key == null || value == null) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        switch (key) {
            case "site_name":
                systemConfig.setSiteName(value);
                break;
            case "site_description":
                systemConfig.setSiteDescription(value);
                break;
            case "registration_enabled":
                systemConfig.setRegistrationEnabled(Boolean.parseBoolean(value));
                break;
            case "email_verification_enabled":
                systemConfig.setEmailVerificationEnabled(Boolean.parseBoolean(value));
                break;
            case "treasure_enabled":
                boolean treasure = Boolean.parseBoolean(value);
                systemConfig.setTreasureEnabled(treasure);
                im.xz.cn.common.Treasure.setEnabled(db, treasure);
                break;
            case "user_action_log_enabled":
                systemConfig.setUserActionLogEnabled(Boolean.parseBoolean(value));
                break;
            case "user_action_log_max_kib":
                int logMaxKib = Integer.parseInt(value);
                if (logMaxKib < 8) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.logMaxKibMin")));
                    return;
                }
                systemConfig.setUserActionLogMaxKib(logMaxKib);
                break;
            case "user_action_log_retention_days":
                int logRetention = Integer.parseInt(value);
                if (logRetention < 1) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.logRetentionMin")));
                    return;
                }
                systemConfig.setUserActionLogRetentionDays(logRetention);
                break;
            case "user_action_log_actions":
                systemConfig.setUserActionLogActions(value);
                break;
            case "user_action_log_download_interval_minutes":
                int logInterval = Integer.parseInt(value);
                if (logInterval < 1) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.logIntervalMin")));
                    return;
                }
                systemConfig.setUserActionLogDownloadIntervalMinutes(logInterval);
                break;
            case "user_action_log_clear_interval_minutes":
                int clearInterval = Integer.parseInt(value);
                if (clearInterval < 1) {
                    ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.logIntervalMin")));
                    return;
                }
                systemConfig.setUserActionLogClearIntervalMinutes(clearInterval);
                break;
            case "user_domain":
                systemConfig.setUserDomain(value);
                break;
            case "admin_domain":
                systemConfig.setAdminDomain(value);
                break;
            case "api_domain":
                systemConfig.setApiDomain(value);
                break;
            case "common_domain":
                systemConfig.setCommonDomain(value);
                break;
            case "username_blacklist":
                systemConfig.setUsernameBlacklist(value);
                break;
            case "username_blacklist_case_sensitive":
                systemConfig.setUsernameBlacklistCaseSensitive(Boolean.parseBoolean(value));
                break;
            case "profile_name_blacklist":
                systemConfig.setProfileNameBlacklist(value);
                break;
            case "profile_name_blacklist_case_sensitive":
                systemConfig.setProfileNameBlacklistCaseSensitive(Boolean.parseBoolean(value));
                break;
            case "skin_name_blacklist":
                systemConfig.setSkinNameBlacklist(value);
                break;
            case "skin_name_blacklist_case_sensitive":
                systemConfig.setSkinNameBlacklistCaseSensitive(Boolean.parseBoolean(value));
                break;
            case "cape_name_blacklist":
                systemConfig.setCapeNameBlacklist(value);
                break;
            case "cape_name_blacklist_case_sensitive":
                systemConfig.setCapeNameBlacklistCaseSensitive(Boolean.parseBoolean(value));
                break;
            case "email_domain_list":
                systemConfig.setEmailDomainList(value);
                break;
            case "email_domain_mode":
                if (!value.equals("blacklist") && !value.equals("whitelist")) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.invalidDomainMode")));
                    return;
                }
                systemConfig.setEmailDomainMode(value);
                break;
            case "icp_record":
                systemConfig.setIcpRecord(value);
                break;
            case "public_security_record":
                systemConfig.setPublicSecurityRecord(value);
                break;
            case "skin_max_size":
                systemConfig.setSkinMaxSize(Integer.parseInt(value));
                break;
            case "skin_max_count":
                systemConfig.setSkinMaxCount(Integer.parseInt(value));
                break;
            case "skin_max_total_size":
                systemConfig.setSkinMaxTotalSize(Integer.parseInt(value));
                break;
            case "skin_rate_limit":
                systemConfig.setSkinRateLimit(Integer.parseInt(value));
                break;
            case "skin_storage_path":
                systemConfig.setSkinStoragePath(value);
                break;
            case "cape_max_size":
                systemConfig.setCapeMaxSize(Integer.parseInt(value));
                break;
            case "cape_max_count":
                systemConfig.setCapeMaxCount(Integer.parseInt(value));
                break;
            case "cape_max_total_size":
                systemConfig.setCapeMaxTotalSize(Integer.parseInt(value));
                break;
            case "cape_rate_limit":
                systemConfig.setCapeRateLimit(Integer.parseInt(value));
                break;
            case "cape_storage_path":
                systemConfig.setCapeStoragePath(value);
                break;
            case "allow_download_skin":
                systemConfig.setAllowDownloadSkin(Boolean.parseBoolean(value));
                break;
            case "allow_download_cape":
                systemConfig.setAllowDownloadCape(Boolean.parseBoolean(value));
                break;
            case "max_profiles_per_user":
                systemConfig.setMaxProfilesPerUser(Integer.parseInt(value));
                break;
            case "profile_name_length_range": {
                String[] parts = value.split(",", -1);
                if (parts.length != 2) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.profileNameLengthRange")));
                    return;
                }
                int minNameLength;
                int maxNameLength;
                try {
                    minNameLength = Integer.parseInt(parts[0].trim());
                    maxNameLength = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.profileNameLengthRange")));
                    return;
                }
                if (minNameLength < 1 || minNameLength > 64 || maxNameLength < 1 || maxNameLength > 64) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.profileNameLengthRange")));
                    return;
                }
                if (maxNameLength < minNameLength) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.profileNameRangeInvalid")));
                    return;
                }
                systemConfig.setMinProfileNameLength(minNameLength);
                systemConfig.setMaxProfileNameLength(maxNameLength);
                break;
            }
            case "max_accounts_per_ip":
                systemConfig.setMaxAccountsPerIp(Integer.parseInt(value));
                break;
            case "max_blocked_users":
                int blocked = Integer.parseInt(value);
                if (blocked < 0 || blocked > 5000) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.blockedRange")));
                    return;
                }
                systemConfig.setMaxBlockedUsers(blocked);
                break;
            case "max_favorites":
                int favs = Integer.parseInt(value);
                if (favs < -1 || favs > 1000) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.favoriteRange")));
                    return;
                }
                systemConfig.setMaxFavorites(favs);
                break;
            case "announcement_mode":
                if (!value.matches("off|toast|modal|top|top_force")) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.invalidAnnouncementMode")));
                    return;
                }
                systemConfig.setAnnouncementMode(value);
                break;
            case "announcement_scope":
                systemConfig.setAnnouncementScope(value);
                break;
            case "announcement_content":
                if (value != null && value.length() > 10000) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("msg.announcementTooLong")));
                    return;
                }
                systemConfig.setAnnouncementContent(value);
                break;
            default:
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("msg.unknownSetting", key)));
                return;
        }
        systemConfig.saveToDatabase(db);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_SETTINGS:" + key, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.saveSuccess")));
    }

    @SuppressWarnings("unchecked")
    public void updateMail(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        MailConfig mail = AppConfig.getInstance().getMailConfig();

        if (has(body, "enabled")) mail.setEnabled(Boolean.parseBoolean(str(body, "enabled")));
        if (has(body, "host")) mail.setHost(str(body, "host"));
        if (has(body, "port")) {
            try { mail.setPort(Integer.parseInt(str(body, "port"))); } catch (NumberFormatException ignored) {}
        }
        if (has(body, "username")) mail.setUsername(str(body, "username"));
        if (has(body, "password")) {
            String pwd = str(body, "password");
            if (!pwd.isEmpty()) mail.setPassword(pwd);
        }
        if (has(body, "from")) mail.setFrom(str(body, "from"));
        if (has(body, "tls")) mail.setTls(Boolean.parseBoolean(str(body, "tls")));

        boolean templateChanged = false;
        if (has(body, "template_verify")) { systemConfig.setMailTemplateVerify(str(body, "template_verify")); templateChanged = true; }
        if (has(body, "template_email_change")) { systemConfig.setMailTemplateEmailChange(str(body, "template_email_change")); templateChanged = true; }
        if (has(body, "template_password_change")) { systemConfig.setMailTemplatePasswordChange(str(body, "template_password_change")); templateChanged = true; }
        if (has(body, "test_content")) { systemConfig.setMailTestContent(str(body, "test_content")); templateChanged = true; }
        if (templateChanged) {
            systemConfig.saveToDatabase(db);
        }

        if (has(body, "enabled")) {
            systemConfig.setEmailVerificationEnabled(mail.isEnabled());
            systemConfig.saveToDatabase(db);
        }

        if (has(body, "email_verification_enabled")) {
            systemConfig.setEmailVerificationEnabled(Boolean.parseBoolean(str(body, "email_verification_enabled")));
            systemConfig.saveToDatabase(db);
        }

        AppConfig.getInstance().saveConfig();
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_MAIL_CONFIG", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.mailSaved")));
    }

    @SuppressWarnings("unchecked")
    public void sendTestMail(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        Object toRaw = body.get("to");
        String to = toRaw == null ? "" : String.valueOf(toRaw).trim();
        if (to.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.mailTestTargetRequired")));
            return;
        }
        if (!to.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.mailTestInvalidTarget")));
            return;
        }

        MailConfig mail = AppConfig.getInstance().getMailConfig();
        if (!mail.isEnabled()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.mailDisabled")));
            return;
        }

        Object contentRaw = body.get("content");
        String content = contentRaw == null ? "" : String.valueOf(contentRaw);

        try {
            MailService mailService = new MailService(mail);
            boolean sent = mailService.sendTestEmail(to, content);
            AuditLogger.logSensitiveOperation(getAdminName(ctx), "SEND_TEST_MAIL", IpUtil.getClientIp(ctx));
            if (sent) {
                ctx.json(Map.of("success", true, "message", I18n.t("msg.mailTestSent")));
            } else {
                ctx.status(500).json(Map.of("success", false, "message", I18n.t("msg.mailTestFailed")));
            }
        } catch (Exception e) {
            ctx.status(500).json(Map.of("success", false, "message", I18n.t("msg.mailTestFailed")));
        }
    }

    private static boolean has(Map<String, Object> body, String key) {
        return body.containsKey(key) || body.containsKey("mail_" + key);
    }

    private static String str(Map<String, Object> body, String key) {
        Object value = body.containsKey(key) ? body.get(key) : body.get("mail_" + key);
        return value == null ? "" : String.valueOf(value);
    }

    public void clearCache(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        cacheDao.cleanExpired();
        tokenDao.cleanExpired();
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CLEAR_CACHE", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.cacheCleared")));
    }

    private String getAdminUsername(Context ctx) {
        String username = ctx.sessionAttribute("adminUsername");
        return username != null ? username : "Admin";
    }

    private String getAdminName(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return "unknown";
        if (isRoot(ctx)) {
            im.xz.cn.model.RootInfo root = rootInfoDao.findById(adminId);
            return root != null ? root.getUsername() : "unknown";
        }
        Admin admin = adminDao.findById(adminId);
        return admin != null ? admin.getUsername() : "unknown";
    }
}
