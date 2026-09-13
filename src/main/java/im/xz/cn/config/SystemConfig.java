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
package im.xz.cn.config;

import im.xz.cn.database.DatabaseManager;

import java.util.List;
import java.util.Map;
import im.xz.cn.logging.logApi;

public class SystemConfig {
    private static final logApi log = logApi.getLogger(SystemConfig.class);
    private static SystemConfig instance;

    private String siteName = "泠 Yggdrasil";
    private String siteDescription = "Minecraft Authentication System";
    private boolean registrationEnabled = true;
    private boolean emailVerificationEnabled = false;
    private boolean treasureEnabled = false;
    private boolean userActionLogEnabled = true;
    private int userActionLogMaxKib = 100;
    private int userActionLogRetentionDays = 30;
    private String userActionLogActions = "login,profile,texture,friend,password,blacklist,email";
    private int userActionLogDownloadIntervalMinutes = 480;
    private String uuidVersion = "v4";
    private String defaultLanguage = "zh-CN";
    private String mailTemplateVerify = defaultMailTemplate("邮箱验证");
    private String mailTemplateEmailChange = defaultMailTemplate("邮箱变更");
    private String mailTemplatePasswordChange = defaultMailTemplate("密码变更");
    private String mailTestContent = "Test Email";
    private String installedAt;

    private String userDomain = "";
    private String adminDomain = "";
    private String apiDomain = "";
    private String commonDomain = "";

    private String usernameBlacklist = "";
    private boolean usernameBlacklistCaseSensitive = false;
    private String profileNameBlacklist = "";
    private boolean profileNameBlacklistCaseSensitive = false;
    private String skinNameBlacklist = "";
    private boolean skinNameBlacklistCaseSensitive = false;
    private String capeNameBlacklist = "";
    private boolean capeNameBlacklistCaseSensitive = false;
    private String emailDomainList = "";
    private String emailDomainMode = "blacklist";
    private String icpRecord = "";
    private String publicSecurityRecord = "";

    private int skinMaxSize = 64;
    private int skinMaxCount = 10;
    private int skinMaxTotalSize = 640;
    private int skinRateLimit = 24;
    private String skinStoragePath = "skins";
    private int capeMaxSize = 64;
    private int capeMaxCount = 10;
    private int capeMaxTotalSize = 640;
    private int capeRateLimit = 24;
    private String capeStoragePath = "capes";
    private boolean allowDownloadSkin = true;
    private boolean allowDownloadCape = true;

    private int encryptionLevel = 1;

    private int tokenTempExpiry = 4320;
    private int tokenPermanentExpiry = 10080;
    private int maxTokensPerProfile = 12;
    private int authRateLimit = 1000;
    private int maxProfilesPerUser = 10;
    private int maxAccountsPerIp = 3;
    private int maxBlockedUsers = 2000;
    private int maxFavorites = 32;
    private String announcementMode = "off";
    private String announcementScope = "user";
    private String announcementContent = "";
    private int batchQueryMaxCount = 6;
    private String signatureMode = "rsa-sha1";
    private String yggdrasilPrivateKey = "";
    private String yggdrasilPublicKey = "";

    private SystemConfig() {}

    public static synchronized SystemConfig getInstance() {
        if (instance == null) {
            instance = new SystemConfig();
        }
        return instance;
    }

    public void loadFromDatabase(DatabaseManager db) {
        try {
            List<Map<String, Object>> rows = db.executeQuery("SELECT setting_key, setting_value FROM system_settings");
            for (Map<String, Object> row : rows) {
                String key = String.valueOf(row.get("setting_key"));
                String value = String.valueOf(row.get("setting_value"));
                applySetting(key, value);
            }
        } catch (Exception e) {
            log.error("Failed to load system config from database: {}", e.getMessage(), e);
        }
    }

    public void saveToDatabase(DatabaseManager db) {
        try {
            upsertSetting(db, "site_name", siteName);
            upsertSetting(db, "site_description", siteDescription);
            upsertSetting(db, "registration_enabled", String.valueOf(registrationEnabled));
            upsertSetting(db, "email_verification_enabled", String.valueOf(emailVerificationEnabled));
            upsertSetting(db, "treasure_enabled", String.valueOf(treasureEnabled));
            upsertSetting(db, "user_action_log_enabled", String.valueOf(userActionLogEnabled));
            upsertSetting(db, "user_action_log_max_kib", String.valueOf(userActionLogMaxKib));
            upsertSetting(db, "user_action_log_retention_days", String.valueOf(userActionLogRetentionDays));
            upsertSetting(db, "user_action_log_actions", userActionLogActions);
            upsertSetting(db, "user_action_log_download_interval_minutes", String.valueOf(userActionLogDownloadIntervalMinutes));
            upsertSetting(db, "uuid_version", uuidVersion);
            upsertSetting(db, "default_language", defaultLanguage);
            upsertSetting(db, "mail_template_verify", mailTemplateVerify);
            upsertSetting(db, "mail_template_email_change", mailTemplateEmailChange);
            upsertSetting(db, "mail_template_password_change", mailTemplatePasswordChange);
            upsertSetting(db, "mail_test_content", mailTestContent);
            upsertSetting(db, "user_domain", userDomain);
            upsertSetting(db, "admin_domain", adminDomain);
            upsertSetting(db, "api_domain", apiDomain);
            upsertSetting(db, "common_domain", commonDomain);
            upsertSetting(db, "username_blacklist", usernameBlacklist);
            upsertSetting(db, "username_blacklist_case_sensitive", String.valueOf(usernameBlacklistCaseSensitive));
            upsertSetting(db, "profile_name_blacklist", profileNameBlacklist);
            upsertSetting(db, "profile_name_blacklist_case_sensitive", String.valueOf(profileNameBlacklistCaseSensitive));
            upsertSetting(db, "skin_name_blacklist", skinNameBlacklist);
            upsertSetting(db, "skin_name_blacklist_case_sensitive", String.valueOf(skinNameBlacklistCaseSensitive));
            upsertSetting(db, "cape_name_blacklist", capeNameBlacklist);
            upsertSetting(db, "cape_name_blacklist_case_sensitive", String.valueOf(capeNameBlacklistCaseSensitive));
            upsertSetting(db, "email_domain_list", emailDomainList);
            upsertSetting(db, "email_domain_mode", emailDomainMode);
            upsertSetting(db, "icp_record", icpRecord);
            upsertSetting(db, "public_security_record", publicSecurityRecord);
            upsertSetting(db, "skin_max_size", String.valueOf(skinMaxSize));
            upsertSetting(db, "skin_max_count", String.valueOf(skinMaxCount));
            upsertSetting(db, "skin_max_total_size", String.valueOf(skinMaxTotalSize));
            upsertSetting(db, "skin_rate_limit", String.valueOf(skinRateLimit));
            upsertSetting(db, "skin_storage_path", skinStoragePath);
            upsertSetting(db, "cape_max_size", String.valueOf(capeMaxSize));
            upsertSetting(db, "cape_max_count", String.valueOf(capeMaxCount));
            upsertSetting(db, "cape_max_total_size", String.valueOf(capeMaxTotalSize));
            upsertSetting(db, "cape_rate_limit", String.valueOf(capeRateLimit));
            upsertSetting(db, "cape_storage_path", capeStoragePath);
            upsertSetting(db, "allow_download_skin", String.valueOf(allowDownloadSkin));
            upsertSetting(db, "allow_download_cape", String.valueOf(allowDownloadCape));
            upsertSetting(db, "encryption_level", String.valueOf(encryptionLevel));
            upsertSetting(db, "token_temp_expiry", String.valueOf(tokenTempExpiry));
            upsertSetting(db, "token_permanent_expiry", String.valueOf(tokenPermanentExpiry));
            upsertSetting(db, "max_tokens_per_profile", String.valueOf(maxTokensPerProfile));
            upsertSetting(db, "auth_rate_limit", String.valueOf(authRateLimit));
            upsertSetting(db, "max_profiles_per_user", String.valueOf(maxProfilesPerUser));
            upsertSetting(db, "max_accounts_per_ip", String.valueOf(maxAccountsPerIp));
            upsertSetting(db, "max_blocked_users", String.valueOf(maxBlockedUsers));
            upsertSetting(db, "max_favorites", String.valueOf(maxFavorites));
            upsertSetting(db, "announcement_mode", announcementMode);
            upsertSetting(db, "announcement_scope", announcementScope);
            upsertSetting(db, "announcement_content", announcementContent);
            upsertSetting(db, "batch_query_max_count", String.valueOf(batchQueryMaxCount));
            upsertSetting(db, "signature_mode", signatureMode);
            upsertSetting(db, "yggdrasil_private_key", yggdrasilPrivateKey);
            upsertSetting(db, "yggdrasil_public_key", yggdrasilPublicKey);
            if (installedAt != null) {
                upsertSetting(db, "installed_at", installedAt);
            }
        } catch (Exception e) {
            log.error("Failed to save system config to database: {}", e.getMessage(), e);
        }
    }

    public void updateSetting(String key, String value) {
        applySetting(key, value);
    }

    private void applySetting(String key, String value) {
        switch (key) {
            case "site_name" -> siteName = value;
            case "site_description" -> siteDescription = value;
            case "registration_enabled" -> registrationEnabled = Boolean.parseBoolean(value);
            case "email_verification_enabled" -> emailVerificationEnabled = Boolean.parseBoolean(value);
            case "treasure_enabled" -> treasureEnabled = Boolean.parseBoolean(value);
            case "user_action_log_enabled" -> userActionLogEnabled = Boolean.parseBoolean(value);
            case "user_action_log_max_kib" -> userActionLogMaxKib = parseInt(value, 100);
            case "user_action_log_retention_days" -> userActionLogRetentionDays = parseInt(value, 30);
            case "user_action_log_actions" -> userActionLogActions = value;
            case "user_action_log_download_interval_minutes" -> userActionLogDownloadIntervalMinutes = parseInt(value, 480);
            case "uuid_version" -> uuidVersion = value;
            case "default_language" -> defaultLanguage = value;
            case "mail_template_verify" -> mailTemplateVerify = value;
            case "mail_template_email_change" -> mailTemplateEmailChange = value;
            case "mail_template_password_change" -> mailTemplatePasswordChange = value;
            case "mail_test_content" -> mailTestContent = value;
            case "installed_at" -> installedAt = value;
            case "user_domain" -> userDomain = value;
            case "admin_domain" -> adminDomain = value;
            case "api_domain" -> apiDomain = value;
            case "common_domain" -> commonDomain = value;
            case "username_blacklist" -> usernameBlacklist = value;
            case "username_blacklist_case_sensitive" -> usernameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "profile_name_blacklist" -> profileNameBlacklist = value;
            case "profile_name_blacklist_case_sensitive" -> profileNameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "skin_name_blacklist" -> skinNameBlacklist = value;
            case "skin_name_blacklist_case_sensitive" -> skinNameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "cape_name_blacklist" -> capeNameBlacklist = value;
            case "cape_name_blacklist_case_sensitive" -> capeNameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "email_domain_list" -> emailDomainList = value;
            case "email_domain_mode" -> emailDomainMode = value;
            case "icp_record" -> icpRecord = value;
            case "public_security_record" -> publicSecurityRecord = value;
            case "skin_max_size" -> skinMaxSize = parseInt(value, 64);
            case "skin_max_count" -> skinMaxCount = parseInt(value, 10);
            case "skin_max_total_size" -> skinMaxTotalSize = parseInt(value, 640);
            case "skin_rate_limit" -> skinRateLimit = parseInt(value, 24);
            case "skin_storage_path" -> skinStoragePath = value;
            case "cape_max_size" -> capeMaxSize = parseInt(value, 64);
            case "cape_max_count" -> capeMaxCount = parseInt(value, 10);
            case "cape_max_total_size" -> capeMaxTotalSize = parseInt(value, 640);
            case "cape_rate_limit" -> capeRateLimit = parseInt(value, 24);
            case "cape_storage_path" -> capeStoragePath = value;
            case "allow_download_skin" -> allowDownloadSkin = Boolean.parseBoolean(value);
            case "allow_download_cape" -> allowDownloadCape = Boolean.parseBoolean(value);
            case "encryption_level" -> encryptionLevel = parseInt(value, 1);
            case "token_temp_expiry" -> tokenTempExpiry = parseInt(value, 4320);
            case "token_permanent_expiry" -> tokenPermanentExpiry = parseInt(value, 10080);
            case "max_tokens_per_profile" -> maxTokensPerProfile = parseInt(value, 12);
            case "auth_rate_limit" -> authRateLimit = parseInt(value, 1000);
            case "max_profiles_per_user" -> maxProfilesPerUser = parseInt(value, 10);
            case "max_accounts_per_ip" -> maxAccountsPerIp = parseInt(value, 3);
            case "max_blocked_users" -> maxBlockedUsers = Math.clamp(parseInt(value, 2000), 0, 5000);
            case "max_favorites" -> maxFavorites = Math.clamp(parseInt(value, 32), -1, 1000);
            case "announcement_mode" -> announcementMode = value;
            case "announcement_scope" -> announcementScope = value;
            case "announcement_content" -> announcementContent = value;
            case "batch_query_max_count" -> batchQueryMaxCount = parseInt(value, 6);
            case "signature_mode" -> signatureMode = value;
            case "yggdrasil_private_key" -> yggdrasilPrivateKey = value;
            case "yggdrasil_public_key" -> yggdrasilPublicKey = value;
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void upsertSetting(DatabaseManager db, String key, String value) {
        String dbType = db.getDbType();
        if ("mysql".equalsIgnoreCase(dbType)) {
            db.executeUpdate(
                "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)",
                key, value
            );
        } else if ("pgsql".equalsIgnoreCase(dbType)) {
            db.executeUpdate(
                "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value",
                key, value
            );
        } else {
            db.executeUpdate(
                "INSERT OR REPLACE INTO system_settings (setting_key, setting_value) VALUES (?, ?)",
                key, value
            );
        }
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getSiteDescription() { return siteDescription; }
    public void setSiteDescription(String siteDescription) { this.siteDescription = siteDescription; }

    public boolean isRegistrationEnabled() { return registrationEnabled; }
    public void setRegistrationEnabled(boolean registrationEnabled) { this.registrationEnabled = registrationEnabled; }

    public boolean isEmailVerificationEnabled() { return emailVerificationEnabled; }
    public void setEmailVerificationEnabled(boolean emailVerificationEnabled) { this.emailVerificationEnabled = emailVerificationEnabled; }

    public boolean isTreasureEnabled() { return treasureEnabled; }
    public void setTreasureEnabled(boolean treasureEnabled) { this.treasureEnabled = treasureEnabled; }

    public boolean isUserActionLogEnabled() { return userActionLogEnabled; }
    public void setUserActionLogEnabled(boolean userActionLogEnabled) { this.userActionLogEnabled = userActionLogEnabled; }

    public int getUserActionLogMaxKib() { return userActionLogMaxKib; }
    public void setUserActionLogMaxKib(int userActionLogMaxKib) { this.userActionLogMaxKib = userActionLogMaxKib; }

    public int getUserActionLogRetentionDays() { return userActionLogRetentionDays; }
    public void setUserActionLogRetentionDays(int userActionLogRetentionDays) { this.userActionLogRetentionDays = userActionLogRetentionDays; }

    public String getUserActionLogActions() { return userActionLogActions; }
    public void setUserActionLogActions(String userActionLogActions) { this.userActionLogActions = userActionLogActions; }

    public int getUserActionLogDownloadIntervalMinutes() { return userActionLogDownloadIntervalMinutes; }
    public void setUserActionLogDownloadIntervalMinutes(int userActionLogDownloadIntervalMinutes) { this.userActionLogDownloadIntervalMinutes = userActionLogDownloadIntervalMinutes; }

    public boolean isUserActionLoggable(String action) {
        if (action == null || userActionLogActions == null) return false;
        for (String part : userActionLogActions.split(",")) {
            if (part.trim().equals(action)) return true;
        }
        return false;
    }

    public String getUuidVersion() { return uuidVersion; }
    public void setUuidVersion(String uuidVersion) { this.uuidVersion = uuidVersion; }

    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }

    public String getMailTemplateVerify() { return mailTemplateVerify; }
    public void setMailTemplateVerify(String mailTemplateVerify) { this.mailTemplateVerify = mailTemplateVerify; }

    public String getMailTemplateEmailChange() { return mailTemplateEmailChange; }
    public void setMailTemplateEmailChange(String mailTemplateEmailChange) { this.mailTemplateEmailChange = mailTemplateEmailChange; }

    public String getMailTemplatePasswordChange() { return mailTemplatePasswordChange; }
    public void setMailTemplatePasswordChange(String mailTemplatePasswordChange) { this.mailTemplatePasswordChange = mailTemplatePasswordChange; }

    public String getMailTestContent() { return mailTestContent; }
    public void setMailTestContent(String mailTestContent) { this.mailTestContent = mailTestContent; }

    private static String defaultMailTemplate(String title) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#FFF0F5;font-family:'Segoe UI','Microsoft YaHei',Arial,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#FFF0F5;padding:32px 12px;">
                <tr><td align="center">
                  <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;width:100%;background:#FFFFFF;border-radius:20px;overflow:hidden;box-shadow:0 12px 40px rgba(255,105,180,0.18);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#FF69B4,#8B5CF6);padding:28px 32px;text-align:center;">
                        <div style="font-size:22px;font-weight:700;color:#FFFFFF;letter-spacing:1px;">✿ 泠 Yggdrasil ✿</div>
                        <div style="font-size:13px;color:rgba(255,255,255,0.85);margin-top:6px;">__TITLE__</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px 12px;text-align:center;">
                        <p style="margin:0 0 18px;color:#666666;font-size:15px;">您好，您的验证码是</p>
                        <div style="display:inline-block;padding:16px 28px;background:#FFF0F5;border:1px dashed #FFB6C1;border-radius:14px;font-size:34px;font-weight:700;letter-spacing:10px;color:#D63384;">{code}</div>
                        <p style="margin:22px 0 0;color:#999999;font-size:13px;">验证码 5 分钟内有效，请尽快使用</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:0 32px 32px;text-align:center;">
                        <p style="margin:0;color:#B0A7B8;font-size:12px;line-height:1.8;">请勿将验证码泄露给他人<br>此邮件由系统自动发送，请勿回复</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#FFFAFC;padding:16px 32px;text-align:center;border-top:1px solid #FFE3EF;">
                        <span style="color:#C0B6C8;font-size:12px;">© 泠 Yggdrasil</span>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.replace("__TITLE__", title);
    }

    public String getInstalledAt() { return installedAt; }
    public void setInstalledAt(String installedAt) { this.installedAt = installedAt; }

    public String getUserDomain() { return userDomain; }
    public void setUserDomain(String userDomain) { this.userDomain = userDomain; }

    public String getAdminDomain() { return adminDomain; }
    public void setAdminDomain(String adminDomain) { this.adminDomain = adminDomain; }

    public String getApiDomain() { return apiDomain; }
    public void setApiDomain(String apiDomain) { this.apiDomain = apiDomain; }

    public String getCommonDomain() { return commonDomain; }
    public void setCommonDomain(String commonDomain) { this.commonDomain = commonDomain; }

    public String getUsernameBlacklist() { return usernameBlacklist; }
    public void setUsernameBlacklist(String usernameBlacklist) { this.usernameBlacklist = usernameBlacklist; }

    public boolean isUsernameBlacklistCaseSensitive() { return usernameBlacklistCaseSensitive; }
    public void setUsernameBlacklistCaseSensitive(boolean usernameBlacklistCaseSensitive) { this.usernameBlacklistCaseSensitive = usernameBlacklistCaseSensitive; }

    public String getProfileNameBlacklist() { return profileNameBlacklist; }
    public void setProfileNameBlacklist(String profileNameBlacklist) { this.profileNameBlacklist = profileNameBlacklist; }

    public boolean isProfileNameBlacklistCaseSensitive() { return profileNameBlacklistCaseSensitive; }
    public void setProfileNameBlacklistCaseSensitive(boolean profileNameBlacklistCaseSensitive) { this.profileNameBlacklistCaseSensitive = profileNameBlacklistCaseSensitive; }

    public String getSkinNameBlacklist() { return skinNameBlacklist; }
    public void setSkinNameBlacklist(String skinNameBlacklist) { this.skinNameBlacklist = skinNameBlacklist; }

    public boolean isSkinNameBlacklistCaseSensitive() { return skinNameBlacklistCaseSensitive; }
    public void setSkinNameBlacklistCaseSensitive(boolean skinNameBlacklistCaseSensitive) { this.skinNameBlacklistCaseSensitive = skinNameBlacklistCaseSensitive; }

    public String getCapeNameBlacklist() { return capeNameBlacklist; }
    public void setCapeNameBlacklist(String capeNameBlacklist) { this.capeNameBlacklist = capeNameBlacklist; }

    public boolean isCapeNameBlacklistCaseSensitive() { return capeNameBlacklistCaseSensitive; }
    public void setCapeNameBlacklistCaseSensitive(boolean capeNameBlacklistCaseSensitive) { this.capeNameBlacklistCaseSensitive = capeNameBlacklistCaseSensitive; }

    public String getEmailDomainList() { return emailDomainList; }
    public void setEmailDomainList(String emailDomainList) { this.emailDomainList = emailDomainList; }

    public String getEmailDomainMode() { return emailDomainMode; }
    public void setEmailDomainMode(String emailDomainMode) { this.emailDomainMode = emailDomainMode; }

    public String getIcpRecord() { return icpRecord; }
    public void setIcpRecord(String icpRecord) { this.icpRecord = icpRecord; }

    public String getPublicSecurityRecord() { return publicSecurityRecord; }
    public void setPublicSecurityRecord(String publicSecurityRecord) { this.publicSecurityRecord = publicSecurityRecord; }

    public int getSkinMaxSize() { return skinMaxSize; }
    public void setSkinMaxSize(int skinMaxSize) { this.skinMaxSize = skinMaxSize; }

    public int getSkinMaxCount() { return skinMaxCount; }
    public void setSkinMaxCount(int skinMaxCount) { this.skinMaxCount = skinMaxCount; }

    public int getSkinMaxTotalSize() { return skinMaxTotalSize; }
    public void setSkinMaxTotalSize(int skinMaxTotalSize) { this.skinMaxTotalSize = skinMaxTotalSize; }

    public int getSkinRateLimit() { return skinRateLimit; }
    public void setSkinRateLimit(int skinRateLimit) { this.skinRateLimit = skinRateLimit; }

    public String getSkinStoragePath() { return skinStoragePath; }
    public void setSkinStoragePath(String skinStoragePath) { this.skinStoragePath = skinStoragePath; }

    public int getCapeMaxSize() { return capeMaxSize; }
    public void setCapeMaxSize(int capeMaxSize) { this.capeMaxSize = capeMaxSize; }

    public int getCapeMaxCount() { return capeMaxCount; }
    public void setCapeMaxCount(int capeMaxCount) { this.capeMaxCount = capeMaxCount; }

    public int getCapeMaxTotalSize() { return capeMaxTotalSize; }
    public void setCapeMaxTotalSize(int capeMaxTotalSize) { this.capeMaxTotalSize = capeMaxTotalSize; }

    public int getCapeRateLimit() { return capeRateLimit; }
    public void setCapeRateLimit(int capeRateLimit) { this.capeRateLimit = capeRateLimit; }

    public String getCapeStoragePath() { return capeStoragePath; }
    public void setCapeStoragePath(String capeStoragePath) { this.capeStoragePath = capeStoragePath; }

    public boolean isAllowDownloadSkin() { return allowDownloadSkin; }
    public void setAllowDownloadSkin(boolean allowDownloadSkin) { this.allowDownloadSkin = allowDownloadSkin; }

    public boolean isAllowDownloadCape() { return allowDownloadCape; }
    public void setAllowDownloadCape(boolean allowDownloadCape) { this.allowDownloadCape = allowDownloadCape; }

    public int getEncryptionLevel() { return encryptionLevel; }
    public void setEncryptionLevel(int encryptionLevel) { this.encryptionLevel = encryptionLevel; }

    public int getTokenTempExpiry() { return tokenTempExpiry; }
    public void setTokenTempExpiry(int tokenTempExpiry) { this.tokenTempExpiry = tokenTempExpiry; }

    public int getTokenPermanentExpiry() { return tokenPermanentExpiry; }
    public void setTokenPermanentExpiry(int tokenPermanentExpiry) { this.tokenPermanentExpiry = tokenPermanentExpiry; }

    public int getMaxTokensPerProfile() { return maxTokensPerProfile; }
    public void setMaxTokensPerProfile(int maxTokensPerProfile) { this.maxTokensPerProfile = maxTokensPerProfile; }

    public int getAuthRateLimit() { return authRateLimit; }
    public void setAuthRateLimit(int authRateLimit) { this.authRateLimit = authRateLimit; }

    public int getMaxProfilesPerUser() { return maxProfilesPerUser; }
    public void setMaxProfilesPerUser(int maxProfilesPerUser) { this.maxProfilesPerUser = maxProfilesPerUser; }

    public int getMaxAccountsPerIp() { return maxAccountsPerIp; }
    public void setMaxAccountsPerIp(int maxAccountsPerIp) { this.maxAccountsPerIp = maxAccountsPerIp; }

    public int getMaxBlockedUsers() { return maxBlockedUsers; }
    public void setMaxBlockedUsers(int maxBlockedUsers) { this.maxBlockedUsers = Math.clamp(maxBlockedUsers, 0, 5000); }

    public int getMaxFavorites() { return maxFavorites; }
    public void setMaxFavorites(int maxFavorites) { this.maxFavorites = Math.clamp(maxFavorites, -1, 1000); }

    public String getAnnouncementMode() { return announcementMode; }
    public void setAnnouncementMode(String announcementMode) { this.announcementMode = announcementMode; }
    public String getAnnouncementScope() { return announcementScope; }
    public void setAnnouncementScope(String announcementScope) { this.announcementScope = announcementScope; }
    public String getAnnouncementContent() { return announcementContent; }
    public void setAnnouncementContent(String announcementContent) { this.announcementContent = announcementContent; }

    public int getBatchQueryMaxCount() { return batchQueryMaxCount; }
    public void setBatchQueryMaxCount(int batchQueryMaxCount) { this.batchQueryMaxCount = batchQueryMaxCount; }

    public String getSignatureMode() { return signatureMode; }
    public void setSignatureMode(String signatureMode) { this.signatureMode = signatureMode; }

    public String getYggdrasilPrivateKey() { return yggdrasilPrivateKey; }
    public void setYggdrasilPrivateKey(String yggdrasilPrivateKey) { this.yggdrasilPrivateKey = yggdrasilPrivateKey; }

    public String getYggdrasilPublicKey() { return yggdrasilPublicKey; }
    public void setYggdrasilPublicKey(String yggdrasilPublicKey) { this.yggdrasilPublicKey = yggdrasilPublicKey; }

    public boolean isUsernameBlacklisted(String username) {
        return matchesBlacklist(username, usernameBlacklist, usernameBlacklistCaseSensitive);
    }

    public boolean isProfileNameBlacklisted(String name) {
        return matchesBlacklist(name, profileNameBlacklist, profileNameBlacklistCaseSensitive);
    }

    public boolean isSkinNameBlacklisted(String name) {
        return matchesBlacklist(name, skinNameBlacklist, skinNameBlacklistCaseSensitive);
    }

    public boolean isCapeNameBlacklisted(String name) {
        return matchesBlacklist(name, capeNameBlacklist, capeNameBlacklistCaseSensitive);
    }

    public String sanitizeTextureFileName(String name, boolean skin) {
        if (name == null || name.isEmpty()) return name;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        boolean blocked = skin ? isSkinNameBlacklisted(base) : isCapeNameBlacklisted(base);
        return blocked ? ("x" + ext) : name;
    }

    private static boolean matchesBlacklist(String value, String blacklist, boolean caseSensitive) {
        if (value == null || blacklist == null || blacklist.isEmpty()) return false;
        String[] list = blacklist.split("\\r?\\n");
        for (String item : list) {
            item = item.trim();
            if (item.isEmpty()) continue;
            if (item.contains("*")) {
                String regex = globToRegex(item);
                if (caseSensitive) {
                    if (value.matches(regex)) return true;
                } else {
                    if (value.toLowerCase().matches(regex.toLowerCase())) return true;
                }
            } else {
                if (caseSensitive) {
                    if (item.equals(value)) return true;
                } else {
                    if (item.equalsIgnoreCase(value)) return true;
                }
            }
        }
        return false;
    }

    public boolean isEmailDomainAllowed(String email) {
        if (emailDomainList == null || emailDomainList.isEmpty()) return true;
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        String[] list = emailDomainList.split("\\r?\\n");
        boolean found = false;
        for (String item : list) {
            item = item.trim().toLowerCase();
            if (item.isEmpty()) continue;
            if (item.contains("*")) {
                if (domain.matches(globToRegex(item))) { found = true; break; }
            } else {
                if (item.equals(domain)) { found = true; break; }
            }
        }
        if ("whitelist".equals(emailDomainMode)) {
            return found;
        } else {
            return !found;
        }
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': case '\\': case '+': case '(':
                case ')': case '{': case '}': case '[':
                case ']': case '^': case '$': case '|':
                    sb.append('\\').append(c); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
