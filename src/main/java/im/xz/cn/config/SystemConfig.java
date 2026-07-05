package im.xz.cn.config;

import im.xz.cn.database.DatabaseManager;

import java.util.List;
import java.util.Map;

public class SystemConfig {
    private static SystemConfig instance;

    private String siteName = "泠 Yggdrasil";
    private String siteDescription = "Minecraft Authentication System";
    private boolean registrationEnabled = true;
    private boolean emailVerificationEnabled = false;
    private String uuidVersion = "v4";
    private String installedAt;

    private String userDomain = "";
    private String adminDomain = "";
    private String apiDomain = "";
    private String commonDomain = "";

    private String usernameBlacklist = "";
    private boolean usernameBlacklistCaseSensitive = false;
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
    private int batchQueryMaxCount = 6;
    private String signatureMode = "ed448";
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
            System.err.println("Failed to load system config from database: " + e.getMessage());
        }
    }

    public void saveToDatabase(DatabaseManager db) {
        try {
            upsertSetting(db, "site_name", siteName);
            upsertSetting(db, "site_description", siteDescription);
            upsertSetting(db, "registration_enabled", String.valueOf(registrationEnabled));
            upsertSetting(db, "email_verification_enabled", String.valueOf(emailVerificationEnabled));
            upsertSetting(db, "uuid_version", uuidVersion);
            upsertSetting(db, "user_domain", userDomain);
            upsertSetting(db, "admin_domain", adminDomain);
            upsertSetting(db, "api_domain", apiDomain);
            upsertSetting(db, "common_domain", commonDomain);
            upsertSetting(db, "username_blacklist", usernameBlacklist);
            upsertSetting(db, "username_blacklist_case_sensitive", String.valueOf(usernameBlacklistCaseSensitive));
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
            upsertSetting(db, "batch_query_max_count", String.valueOf(batchQueryMaxCount));
            upsertSetting(db, "signature_mode", signatureMode);
            upsertSetting(db, "yggdrasil_private_key", yggdrasilPrivateKey);
            upsertSetting(db, "yggdrasil_public_key", yggdrasilPublicKey);
            if (installedAt != null) {
                upsertSetting(db, "installed_at", installedAt);
            }
        } catch (Exception e) {
            System.err.println("Failed to save system config to database: " + e.getMessage());
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
            case "uuid_version" -> uuidVersion = value;
            case "installed_at" -> installedAt = value;
            case "user_domain" -> userDomain = value;
            case "admin_domain" -> adminDomain = value;
            case "api_domain" -> apiDomain = value;
            case "common_domain" -> commonDomain = value;
            case "username_blacklist" -> usernameBlacklist = value;
            case "username_blacklist_case_sensitive" -> usernameBlacklistCaseSensitive = Boolean.parseBoolean(value);
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

    public String getUuidVersion() { return uuidVersion; }
    public void setUuidVersion(String uuidVersion) { this.uuidVersion = uuidVersion; }

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

    public int getBatchQueryMaxCount() { return batchQueryMaxCount; }
    public void setBatchQueryMaxCount(int batchQueryMaxCount) { this.batchQueryMaxCount = batchQueryMaxCount; }

    public String getSignatureMode() { return signatureMode; }
    public void setSignatureMode(String signatureMode) { this.signatureMode = signatureMode; }

    public String getYggdrasilPrivateKey() { return yggdrasilPrivateKey; }
    public void setYggdrasilPrivateKey(String yggdrasilPrivateKey) { this.yggdrasilPrivateKey = yggdrasilPrivateKey; }

    public String getYggdrasilPublicKey() { return yggdrasilPublicKey; }
    public void setYggdrasilPublicKey(String yggdrasilPublicKey) { this.yggdrasilPublicKey = yggdrasilPublicKey; }

    public boolean isUsernameBlacklisted(String username) {
        if (username == null || usernameBlacklist == null || usernameBlacklist.isEmpty()) return false;
        String[] list = usernameBlacklist.split("\\r?\\n");
        for (String item : list) {
            item = item.trim();
            if (item.isEmpty()) continue;
            if (item.contains("*")) {
                String regex = globToRegex(item);
                if (usernameBlacklistCaseSensitive) {
                    if (username.matches(regex)) return true;
                } else {
                    if (username.toLowerCase().matches(regex.toLowerCase())) return true;
                }
            } else {
                if (usernameBlacklistCaseSensitive) {
                    if (item.equals(username)) return true;
                } else {
                    if (item.equalsIgnoreCase(username)) return true;
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
