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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import im.xz.cn.logging.logApi;

public class SystemConfig {
    private static final logApi log = logApi.getLogger(SystemConfig.class);
    private static SystemConfig instance;

    public static final String DEFAULT_CORS_ORIGINS =
            "http://localhost:35565\nhttp://localhost:35577\nhttp://localhost:35599\nhttp://localhost:35598";
    public static final String DEFAULT_HEADER_CSP =
            "default-src 'self'; script-src 'self' 'nonce-{nonce}'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data: https: http:; font-src 'self' data:; connect-src 'self'; "
                    + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none';";
    public static final String DEFAULT_HEADER_HSTS = "max-age=31536000; includeSubDomains; preload";
    public static final String DEFAULT_HEADER_CONTENT_TYPE_OPTIONS = "nosniff";
    public static final String DEFAULT_HEADER_FRAME_OPTIONS = "DENY";
    public static final String API_MODE_DEFAULT = "default";
    public static final String API_MODE_STANDALONE = "standalone";
    public static final String API_MODE_AGGREGATE = "aggregate";

    public static final String DEFAULT_HEADER_XSS_PROTECTION = "0";
    public static final String DEFAULT_HEADER_REFERRER_POLICY = "strict-origin-when-cross-origin";
    public static final String DEFAULT_HEADER_PERMISSIONS_POLICY =
            "geolocation=(), microphone=(), camera=(), payment=(), usb=()";
    public static final String DEFAULT_HEADER_CACHE_CONTROL = "no-store";
    public static final String DEFAULT_REQUEST_INTERVALS = """
            /api/register 300000
            /api/skins/upload 60000
            /api/capes/upload 60000
            /api/skins/delete 60000
            /api/capes/delete 60000
            /api/settings/email 5000
            /api/settings/password 5000
            /api/verify-my-email 5000
            /api/profiles/update 5000
            /api/profiles/create 5000
            /api/profiles/delete 5000
            /api/profiles/regenerate-token 5000
            /api/friends/block 1000
            /api/friends/unblock 1000
            /api/friends/blocked/clear 1000
            /api/friends/add 1000
            /api/friends/delete 1000
            /api/friends/request/accept 1000
            /api/friends/request/cancel 1000
            /api/settings/nickname 1000
            /api/world/like 500
            /api/world/favorite 500
            /api/world/favorite/alias 500
            /api/textures/visibility 500
            /api/friends/share-texture 500
            /api/friends/unshare-texture 500
            /api/friends/display-profile 500
            /api/skins/alias 500
            /api/capes/alias 500""";
    public static final String DEFAULT_REQUEST_RATES = """
            /api/world/textures* 30
            /api/friends 60
            /api/friends/my-info 60
            /api/friends/blocked 60
            */shared-textures 60
            */my-shared 60
            /api/skins 30
            /api/capes 30
            /api/profiles 30
            /api/textures/my 30
            /api/shared/my 30
            /api/skins/download 30
            /api/capes/download 30
            /api/textures/visibility 60
            /api/announcement 60
            /api/footer-info 60""";

    private String siteName = "泠 Yggdrasil";
    private String siteDescription = "Minecraft Authentication System";
    private String siteKeywords = "Minecraft,Authentication,Yggdrasil";
    private boolean registrationEnabled = true;
    private boolean emailVerificationEnabled = false;
    private boolean treasureEnabled = false;
    private boolean userActionLogEnabled = true;
    private int userActionLogMaxKib = 100;
    private int userActionLogRetentionDays = 30;
    private String userActionLogActions = "login,profile,texture,friend,password,blacklist,email";
    private int userActionLogDownloadIntervalMinutes = 480;
    private int userActionLogClearIntervalMinutes = 360;
    private String uuidVersion = "v4";
    private String mailTemplateVerify = defaultMailTemplate("邮箱验证");
    private String mailTemplateEmailChange = defaultMailTemplate("邮箱变更");
    private String mailTemplatePasswordChange = defaultMailTemplate("密码变更");
    private String mailTestContent = "Test Email";
    private String installedAt;

    private String usernameBlacklist = "";
    private boolean usernameBlacklistCaseSensitive = false;
    private String profileNameBlacklist = "";
    private boolean profileNameBlacklistCaseSensitive = false;
    private String skinNameBlacklist = "";
    private boolean skinNameBlacklistCaseSensitive = false;
    private String textureHashBlacklist = "";
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
    private boolean pngValidationEnabled = true;
    private int pngMaxWidth = 4096;
    private int pngMaxHeight = 4096;
    private int pngMaxPixels = 1_048_576;
    private int pngMaxChunkSizeKib = 1024;
    private boolean pngStrictChunkMode = true;
    private int pngMaxConcurrent = 4;

    private int minProfileNameLength = 1;
    private int maxProfileNameLength = 16;

    private String corsOrigins = DEFAULT_CORS_ORIGINS;
    private String headerCsp = DEFAULT_HEADER_CSP;
    private String headerHsts = DEFAULT_HEADER_HSTS;
    private String headerContentTypeOptions = DEFAULT_HEADER_CONTENT_TYPE_OPTIONS;
    private String headerFrameOptions = DEFAULT_HEADER_FRAME_OPTIONS;
    private String headerXssProtection = DEFAULT_HEADER_XSS_PROTECTION;
    private String headerReferrerPolicy = DEFAULT_HEADER_REFERRER_POLICY;
    private String headerPermissionsPolicy = DEFAULT_HEADER_PERMISSIONS_POLICY;
    private String headerCacheControl = DEFAULT_HEADER_CACHE_CONTROL;

    private int userSessionTimeoutSeconds = 2592000;
    private int adminSessionTimeoutSeconds = 900;
    private int loginMaxAttemptsPerIp = 20;
    private int loginMaxAttemptsPerAccount = 5;
    private int loginLockoutSeconds = 900;
    private int loginRateWindowSeconds = 60;
    private String requestIntervals = DEFAULT_REQUEST_INTERVALS;
    private String requestRates = DEFAULT_REQUEST_RATES;
    private volatile Map<String, Integer> requestIntervalMap = parseRequestMap(DEFAULT_REQUEST_INTERVALS);
    private volatile Map<String, Integer> requestRateMap = parseRequestMap(DEFAULT_REQUEST_RATES);

    private int tokenTempExpiry = 4320;
    private int tokenPermanentExpiry = 10080;
    private int maxTokensPerProfile = 12;
    private int authRateLimit = 1000;

    
    public static final int DEFAULT_WORLD_TREE_KEY_LENGTH = 32;
    public static final int MIN_WORLD_TREE_KEY_LENGTH = 16;
    public static final int MAX_WORLD_TREE_KEY_LENGTH = 256;
    private int worldTreeKeyLength = DEFAULT_WORLD_TREE_KEY_LENGTH;
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
    private String yggdrasilApiMode = API_MODE_DEFAULT;

    private SystemConfig() {}

    public static synchronized SystemConfig getInstance() {
        if (instance == null) {
            instance = new SystemConfig();
        }
        return instance;
    }

    public void loadFromDatabase(DatabaseManager db) {
        try {
            im.xz.cn.settings.SettingsRepository repository =
                    new im.xz.cn.settings.SettingsRepository(db);
            Map<String, String> values = repository.loadEffectiveOverrides();
            if (values.isEmpty()) {
                values = migrateLegacySettings(db, repository);
            }
            log.info("[Config] Loaded {} keys from canonical settings", values.size());
            if (!values.isEmpty()) {
                im.xz.cn.settings.SettingsRuntime.apply(db, values, false);
            }
            im.xz.cn.settings.SettingsSecretMigrator.migrate(db);
            im.xz.cn.settings.SecretStore.create(db)
                    .get("yggdrasil.private_key", "settings.yggdrasil")
                    .ifPresent(key -> yggdrasilPrivateKey = key);
        } catch (Exception e) {
            log.error("Failed to load system config from database: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to load system config from database", e);
        }
    }

    
    private Map<String, String> migrateLegacySettings(DatabaseManager db,
                                                       im.xz.cn.settings.SettingsRepository repository) {
        Map<String, String> values = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : db.executeQuery(
                "SELECT setting_key, setting_value FROM system_settings")) {
            String key = String.valueOf(row.get("setting_key"));
            String value = String.valueOf(row.get("setting_value"));
            values.put(key, value);
            String group = im.xz.cn.settings.SettingsGroups.classify(key);
            Map<String, String> merged = new java.util.LinkedHashMap<>(repository.load(group));
            merged.put(key, value);
            repository.save(group, merged, repository.revision(group));
        }
        return values;
    }

    public void saveToDatabase(DatabaseManager db) {
        try {
            Map<String, String> values = new java.util.LinkedHashMap<>();
            values.put("site_name", String.valueOf(siteName));
            values.put("site_description", String.valueOf(siteDescription));
            values.put("site_keywords", String.valueOf(siteKeywords));
            values.put("registration_enabled", String.valueOf(String.valueOf(registrationEnabled)));
            values.put("email_verification_enabled", String.valueOf(String.valueOf(emailVerificationEnabled)));
            values.put("treasure_enabled", String.valueOf(String.valueOf(treasureEnabled)));
            values.put("user_action_log_enabled", String.valueOf(String.valueOf(userActionLogEnabled)));
            values.put("user_action_log_max_kib", String.valueOf(String.valueOf(userActionLogMaxKib)));
            values.put("user_action_log_retention_days", String.valueOf(String.valueOf(userActionLogRetentionDays)));
            values.put("user_action_log_actions", String.valueOf(userActionLogActions));
            values.put("user_action_log_download_interval_minutes", String.valueOf(String.valueOf(userActionLogDownloadIntervalMinutes)));
            values.put("user_action_log_clear_interval_minutes", String.valueOf(String.valueOf(userActionLogClearIntervalMinutes)));
            values.put("uuid_version", String.valueOf(uuidVersion));
            values.put("mail_template_verify", String.valueOf(mailTemplateVerify));
            values.put("mail_template_email_change", String.valueOf(mailTemplateEmailChange));
            values.put("mail_template_password_change", String.valueOf(mailTemplatePasswordChange));
            values.put("mail_test_content", String.valueOf(mailTestContent));
            values.put("username_blacklist", String.valueOf(usernameBlacklist));
            values.put("username_blacklist_case_sensitive", String.valueOf(String.valueOf(usernameBlacklistCaseSensitive)));
            values.put("profile_name_blacklist", String.valueOf(profileNameBlacklist));
            values.put("profile_name_blacklist_case_sensitive", String.valueOf(String.valueOf(profileNameBlacklistCaseSensitive)));
            values.put("skin_name_blacklist", String.valueOf(skinNameBlacklist));
            values.put("skin_name_blacklist_case_sensitive", String.valueOf(String.valueOf(skinNameBlacklistCaseSensitive)));
            values.put("texture_hash_blacklist", String.valueOf(textureHashBlacklist));
            values.put("cape_name_blacklist", String.valueOf(capeNameBlacklist));
            values.put("cape_name_blacklist_case_sensitive", String.valueOf(String.valueOf(capeNameBlacklistCaseSensitive)));
            values.put("email_domain_list", String.valueOf(emailDomainList));
            values.put("email_domain_mode", String.valueOf(emailDomainMode));
            values.put("icp_record", String.valueOf(icpRecord));
            values.put("public_security_record", String.valueOf(publicSecurityRecord));
            values.put("skin_max_size", String.valueOf(String.valueOf(skinMaxSize)));
            values.put("skin_max_count", String.valueOf(String.valueOf(skinMaxCount)));
            values.put("skin_max_total_size", String.valueOf(String.valueOf(skinMaxTotalSize)));
            values.put("skin_rate_limit", String.valueOf(String.valueOf(skinRateLimit)));
            values.put("skin_storage_path", String.valueOf(skinStoragePath));
            values.put("cape_max_size", String.valueOf(String.valueOf(capeMaxSize)));
            values.put("cape_max_count", String.valueOf(String.valueOf(capeMaxCount)));
            values.put("cape_max_total_size", String.valueOf(String.valueOf(capeMaxTotalSize)));
            values.put("cape_rate_limit", String.valueOf(String.valueOf(capeRateLimit)));
            values.put("cape_storage_path", String.valueOf(capeStoragePath));
            values.put("allow_download_skin", String.valueOf(String.valueOf(allowDownloadSkin)));
            values.put("allow_download_cape", String.valueOf(String.valueOf(allowDownloadCape)));
            values.put("encryption_level", String.valueOf(String.valueOf(encryptionLevel)));
            values.put("png_validation_enabled", String.valueOf(String.valueOf(pngValidationEnabled)));
            values.put("png_max_width", String.valueOf(String.valueOf(pngMaxWidth)));
            values.put("png_max_height", String.valueOf(String.valueOf(pngMaxHeight)));
            values.put("png_max_pixels", String.valueOf(String.valueOf(pngMaxPixels)));
            values.put("png_max_chunk_size_kib", String.valueOf(String.valueOf(pngMaxChunkSizeKib)));
            values.put("png_strict_chunk_mode", String.valueOf(String.valueOf(pngStrictChunkMode)));
            values.put("png_max_concurrent", String.valueOf(String.valueOf(pngMaxConcurrent)));
            values.put("min_profile_name_length", String.valueOf(String.valueOf(minProfileNameLength)));
            values.put("max_profile_name_length", String.valueOf(String.valueOf(maxProfileNameLength)));
            values.put("cors_origins", String.valueOf(corsOrigins));
            values.put("header_csp", String.valueOf(headerCsp));
            values.put("header_hsts", String.valueOf(headerHsts));
            values.put("header_content_type_options", String.valueOf(headerContentTypeOptions));
            values.put("header_frame_options", String.valueOf(headerFrameOptions));
            values.put("header_xss_protection", String.valueOf(headerXssProtection));
            values.put("header_referrer_policy", String.valueOf(headerReferrerPolicy));
            values.put("header_permissions_policy", String.valueOf(headerPermissionsPolicy));
            values.put("header_cache_control", String.valueOf(headerCacheControl));
            values.put("user_session_timeout_seconds", String.valueOf(String.valueOf(userSessionTimeoutSeconds)));
            values.put("admin_session_timeout_seconds", String.valueOf(String.valueOf(adminSessionTimeoutSeconds)));
            values.put("login_max_attempts_per_ip", String.valueOf(String.valueOf(loginMaxAttemptsPerIp)));
            values.put("login_max_attempts_per_account", String.valueOf(String.valueOf(loginMaxAttemptsPerAccount)));
            values.put("login_lockout_seconds", String.valueOf(String.valueOf(loginLockoutSeconds)));
            values.put("login_rate_window_seconds", String.valueOf(String.valueOf(loginRateWindowSeconds)));
            values.put("request_intervals", String.valueOf(requestIntervals));
            values.put("request_rates", String.valueOf(requestRates));
            values.put("token_temp_expiry", String.valueOf(String.valueOf(tokenTempExpiry)));
            values.put("token_permanent_expiry", String.valueOf(String.valueOf(tokenPermanentExpiry)));
            values.put("max_tokens_per_profile", String.valueOf(String.valueOf(maxTokensPerProfile)));
            values.put("world_tree_key_length", String.valueOf(String.valueOf(worldTreeKeyLength)));
            values.put("auth_rate_limit", String.valueOf(String.valueOf(authRateLimit)));
            values.put("max_profiles_per_user", String.valueOf(String.valueOf(maxProfilesPerUser)));
            values.put("max_accounts_per_ip", String.valueOf(String.valueOf(maxAccountsPerIp)));
            values.put("max_blocked_users", String.valueOf(String.valueOf(maxBlockedUsers)));
            values.put("max_favorites", String.valueOf(String.valueOf(maxFavorites)));
            values.put("announcement_mode", String.valueOf(announcementMode));
            values.put("announcement_scope", String.valueOf(announcementScope));
            values.put("announcement_content", String.valueOf(announcementContent));
            values.put("batch_query_max_count", String.valueOf(String.valueOf(batchQueryMaxCount)));
            values.put("signature_mode", String.valueOf(signatureMode));
            values.put("yggdrasil_api_mode", String.valueOf(yggdrasilApiMode));
            values.put("yggdrasil_public_key", String.valueOf(yggdrasilPublicKey));
            values.put("installed_at", String.valueOf(installedAt));
            persistSettings(db, values);
            if (yggdrasilPrivateKey != null && !yggdrasilPrivateKey.isBlank()) {
                im.xz.cn.settings.SecretStore.create(db)
                        .put("yggdrasil.private_key", yggdrasilPrivateKey, "settings.yggdrasil");
            }
        } catch (Exception e) {
            log.error("Failed to save system config to database: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to save system config to database", e);
        }
    }

    
    private void persistSettings(DatabaseManager db, Map<String, String> values) {
        im.xz.cn.settings.SettingsRepository repository = new im.xz.cn.settings.SettingsRepository(db);
        Map<String, Map<String, String>> groups = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            groups.computeIfAbsent(im.xz.cn.settings.SettingsGroups.classify(entry.getKey()),
                    ignored -> new java.util.LinkedHashMap<>()).put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map<String, String>> group : groups.entrySet()) {
            Map<String, String> merged = new java.util.LinkedHashMap<>(repository.load(group.getKey()));
            merged.putAll(group.getValue());
            repository.save(group.getKey(), merged, repository.revision(group.getKey()));
        }
    }

    public void updateSetting(String key, String value) {
        applySetting(key, value);
    }

    private void applySetting(String key, String value) {
        switch (key) {
            case "site_name" -> siteName = value;
            case "site_description" -> siteDescription = value;
            case "site_keywords" -> siteKeywords = value;
            case "registration_enabled" -> registrationEnabled = Boolean.parseBoolean(value);
            case "email_verification_enabled" -> emailVerificationEnabled = Boolean.parseBoolean(value);
            case "treasure_enabled" -> treasureEnabled = Boolean.parseBoolean(value);
            case "user_action_log_enabled" -> userActionLogEnabled = Boolean.parseBoolean(value);
            case "user_action_log_max_kib" -> userActionLogMaxKib = parseInt(value, 100);
            case "user_action_log_retention_days" -> userActionLogRetentionDays = parseInt(value, 30);
            case "user_action_log_actions" -> userActionLogActions = value;
            case "user_action_log_download_interval_minutes" -> userActionLogDownloadIntervalMinutes = parseInt(value, 480);
            case "user_action_log_clear_interval_minutes" -> userActionLogClearIntervalMinutes = parseInt(value, 360);
            case "uuid_version" -> uuidVersion = value;
            case "mail_template_verify" -> mailTemplateVerify = value;
            case "mail_template_email_change" -> mailTemplateEmailChange = value;
            case "mail_template_password_change" -> mailTemplatePasswordChange = value;
            case "mail_test_content" -> mailTestContent = value;
            case "installed_at" -> installedAt = value;
            case "username_blacklist" -> usernameBlacklist = value;
            case "username_blacklist_case_sensitive" -> usernameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "profile_name_blacklist" -> profileNameBlacklist = value;
            case "profile_name_blacklist_case_sensitive" -> profileNameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "skin_name_blacklist" -> skinNameBlacklist = value;
            case "skin_name_blacklist_case_sensitive" -> skinNameBlacklistCaseSensitive = Boolean.parseBoolean(value);
            case "texture_hash_blacklist" -> textureHashBlacklist = value;
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
            case "png_validation_enabled" -> pngValidationEnabled = parseBoolean(value, true);
            case "png_max_width" -> setPngMaxWidth(parseInt(value, 4096));
            case "png_max_height" -> setPngMaxHeight(parseInt(value, 4096));
            case "png_max_pixels" -> setPngMaxPixels(parseInt(value, 1_048_576));
            case "png_max_chunk_size_kib" -> setPngMaxChunkSizeKib(parseInt(value, 1024));
            case "png_strict_chunk_mode" -> pngStrictChunkMode = parseBoolean(value, true);
            case "png_max_concurrent" -> setPngMaxConcurrent(parseInt(value, 4));
            case "min_profile_name_length" -> setMinProfileNameLength(parseInt(value, 1));
            case "max_profile_name_length" -> setMaxProfileNameLength(parseInt(value, 16));
            case "cors_origins" -> corsOrigins = value;
            case "header_csp" -> headerCsp = value;
            case "header_hsts" -> headerHsts = value;
            case "header_content_type_options" -> headerContentTypeOptions = value;
            case "header_frame_options" -> headerFrameOptions = value;
            case "header_xss_protection" -> headerXssProtection = value;
            case "header_referrer_policy" -> headerReferrerPolicy = value;
            case "header_permissions_policy" -> headerPermissionsPolicy = value;
            case "header_cache_control" -> headerCacheControl = value;
            case "user_session_timeout_seconds" -> userSessionTimeoutSeconds = parseInt(value, 2592000);
            case "admin_session_timeout_seconds" -> adminSessionTimeoutSeconds = parseInt(value, 900);
            case "login_max_attempts_per_ip" -> loginMaxAttemptsPerIp = parseInt(value, 20);
            case "login_max_attempts_per_account" -> loginMaxAttemptsPerAccount = parseInt(value, 5);
            case "login_lockout_seconds" -> loginLockoutSeconds = parseInt(value, 900);
            case "login_rate_window_seconds" -> loginRateWindowSeconds = parseInt(value, 60);
            case "request_intervals" -> setRequestIntervals(value);
            case "request_rates" -> setRequestRates(value);
            case "token_temp_expiry" -> tokenTempExpiry = parseInt(value, 4320);
            case "token_permanent_expiry" -> tokenPermanentExpiry = parseInt(value, 10080);
            case "max_tokens_per_profile" -> maxTokensPerProfile = parseInt(value, 12);
            case "world_tree_key_length" -> worldTreeKeyLength =
                    clampWorldTreeKeyLength(parseInt(value, DEFAULT_WORLD_TREE_KEY_LENGTH));
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
            case "yggdrasil_api_mode" -> {
                if (isValidApiMode(value)) yggdrasilApiMode = value;
            }
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

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return defaultValue;
    }

    public static boolean isValidApiMode(String mode) {
        return API_MODE_DEFAULT.equals(mode)
                || API_MODE_STANDALONE.equals(mode)
                || API_MODE_AGGREGATE.equals(mode);
    }

    public static boolean isValidCorsOrigin(String origin) {
        if (origin == null) return false;
        String value = origin.trim();
        if (value.isEmpty() || value.contains("*") || value.matches(".*\\s.*") || value.equalsIgnoreCase("null")) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) return false;
            String path = uri.getPath();
            return path == null || path.isEmpty() || "/".equals(path);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidRequestMap(String raw) {
        if (raw == null || raw.isBlank()) return true;
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int idx = trimmed.lastIndexOf(' ');
            if (idx <= 0) return false;
            if (trimmed.substring(0, idx).trim().isEmpty()) return false;
            try {
                if (Integer.parseInt(trimmed.substring(idx + 1).trim()) <= 0) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> parseRequestMap(String raw) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (raw == null) return map;
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int idx = trimmed.lastIndexOf(' ');
            if (idx <= 0) continue;
            String key = trimmed.substring(0, idx).trim();
            if (key.isEmpty()) continue;
            try {
                map.put(key, Integer.parseInt(trimmed.substring(idx + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }



    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getSiteDescription() { return siteDescription; }
    public void setSiteDescription(String siteDescription) { this.siteDescription = siteDescription; }

    public String getSiteKeywords() { return siteKeywords; }
    public void setSiteKeywords(String siteKeywords) { this.siteKeywords = siteKeywords; }

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

    public int getUserActionLogClearIntervalMinutes() { return userActionLogClearIntervalMinutes; }
    public void setUserActionLogClearIntervalMinutes(int userActionLogClearIntervalMinutes) { this.userActionLogClearIntervalMinutes = userActionLogClearIntervalMinutes; }

    public boolean isUserActionLoggable(String action) {
        if (action == null || userActionLogActions == null) return false;
        for (String part : userActionLogActions.split(",")) {
            if (part.trim().equals(action)) return true;
        }
        return false;
    }

    public String getUuidVersion() { return uuidVersion; }
    public void setUuidVersion(String uuidVersion) { this.uuidVersion = uuidVersion; }

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
                        <div style="font-size:22px;font-weight:700;color:#FFFFFF;letter-spacing:1px;">✿ {SiteName} ✿</div>
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
                        <span style="color:#C0B6C8;font-size:12px;">© {SiteName}</span>
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

    public String getTextureHashBlacklist() { return textureHashBlacklist; }
    public void setTextureHashBlacklist(String textureHashBlacklist) { this.textureHashBlacklist = textureHashBlacklist; }

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

    public boolean isPngValidationEnabled() { return pngValidationEnabled; }
    public void setPngValidationEnabled(boolean pngValidationEnabled) { this.pngValidationEnabled = pngValidationEnabled; }

    public int getPngMaxWidth() { return pngMaxWidth; }
    public void setPngMaxWidth(int pngMaxWidth) { this.pngMaxWidth = Math.clamp(pngMaxWidth, 1, 4096); }

    public int getPngMaxHeight() { return pngMaxHeight; }
    public void setPngMaxHeight(int pngMaxHeight) { this.pngMaxHeight = Math.clamp(pngMaxHeight, 1, 4096); }

    public int getPngMaxPixels() { return pngMaxPixels; }
    public void setPngMaxPixels(int pngMaxPixels) { this.pngMaxPixels = Math.clamp(pngMaxPixels, 1, 1_048_576); }

    public int getPngMaxChunkSizeKib() { return pngMaxChunkSizeKib; }
    public void setPngMaxChunkSizeKib(int pngMaxChunkSizeKib) { this.pngMaxChunkSizeKib = Math.clamp(pngMaxChunkSizeKib, 1, 16_384); }

    public boolean isPngStrictChunkMode() { return pngStrictChunkMode; }
    public void setPngStrictChunkMode(boolean pngStrictChunkMode) { this.pngStrictChunkMode = pngStrictChunkMode; }

    public int getPngMaxConcurrent() { return pngMaxConcurrent; }
    public void setPngMaxConcurrent(int pngMaxConcurrent) { this.pngMaxConcurrent = Math.clamp(pngMaxConcurrent, 1, 99999); }

    public int getMinProfileNameLength() { return minProfileNameLength; }
    public void setMinProfileNameLength(int minProfileNameLength) { this.minProfileNameLength = Math.clamp(minProfileNameLength, 1, 64); }

    public int getMaxProfileNameLength() { return maxProfileNameLength; }
    public void setMaxProfileNameLength(int maxProfileNameLength) { this.maxProfileNameLength = Math.clamp(maxProfileNameLength, 1, 64); }

    public String getCorsOrigins() { return corsOrigins; }
    public void setCorsOrigins(String corsOrigins) { this.corsOrigins = corsOrigins == null ? "" : corsOrigins; }

    public String getHeaderCsp() { return headerCsp; }
    public void setHeaderCsp(String headerCsp) { this.headerCsp = headerCsp == null ? "" : headerCsp; }
    public String getHeaderHsts() { return headerHsts; }
    public void setHeaderHsts(String headerHsts) { this.headerHsts = headerHsts == null ? "" : headerHsts; }
    public String getHeaderContentTypeOptions() { return headerContentTypeOptions; }
    public void setHeaderContentTypeOptions(String v) { this.headerContentTypeOptions = v == null ? "" : v; }
    public String getHeaderFrameOptions() { return headerFrameOptions; }
    public void setHeaderFrameOptions(String v) { this.headerFrameOptions = v == null ? "" : v; }
    public String getHeaderXssProtection() { return headerXssProtection; }
    public void setHeaderXssProtection(String v) { this.headerXssProtection = v == null ? "" : v; }
    public String getHeaderReferrerPolicy() { return headerReferrerPolicy; }
    public void setHeaderReferrerPolicy(String v) { this.headerReferrerPolicy = v == null ? "" : v; }
    public String getHeaderPermissionsPolicy() { return headerPermissionsPolicy; }
    public void setHeaderPermissionsPolicy(String v) { this.headerPermissionsPolicy = v == null ? "" : v; }
    public String getHeaderCacheControl() { return headerCacheControl; }
    public void setHeaderCacheControl(String v) { this.headerCacheControl = v == null ? "" : v; }

    public int getUserSessionTimeoutSeconds() { return userSessionTimeoutSeconds; }
    public void setUserSessionTimeoutSeconds(int v) { this.userSessionTimeoutSeconds = Math.max(1, v); }
    public int getAdminSessionTimeoutSeconds() { return adminSessionTimeoutSeconds; }
    public void setAdminSessionTimeoutSeconds(int v) { this.adminSessionTimeoutSeconds = Math.max(1, v); }

    public int getLoginMaxAttemptsPerIp() { return loginMaxAttemptsPerIp; }
    public void setLoginMaxAttemptsPerIp(int v) { this.loginMaxAttemptsPerIp = Math.max(1, v); }
    public int getLoginMaxAttemptsPerAccount() { return loginMaxAttemptsPerAccount; }
    public void setLoginMaxAttemptsPerAccount(int v) { this.loginMaxAttemptsPerAccount = Math.max(1, v); }
    public int getLoginLockoutSeconds() { return loginLockoutSeconds; }
    public void setLoginLockoutSeconds(int v) { this.loginLockoutSeconds = Math.max(1, v); }
    public int getLoginRateWindowSeconds() { return loginRateWindowSeconds; }
    public void setLoginRateWindowSeconds(int v) { this.loginRateWindowSeconds = Math.max(1, v); }

    public String getRequestIntervals() { return requestIntervals; }
    public void setRequestIntervals(String requestIntervals) {
        this.requestIntervals = requestIntervals == null ? "" : requestIntervals;
        this.requestIntervalMap = parseRequestMap(this.requestIntervals);
    }
    public String getRequestRates() { return requestRates; }
    public void setRequestRates(String requestRates) {
        this.requestRates = requestRates == null ? "" : requestRates;
        this.requestRateMap = parseRequestMap(this.requestRates);
    }
    public Map<String, Integer> getRequestIntervalMap() { return requestIntervalMap; }
    public Map<String, Integer> getRequestRateMap() { return requestRateMap; }

    public int getTokenTempExpiry() { return tokenTempExpiry; }
    public void setTokenTempExpiry(int tokenTempExpiry) { this.tokenTempExpiry = tokenTempExpiry; }

    public int getTokenPermanentExpiry() { return tokenPermanentExpiry; }
    public void setTokenPermanentExpiry(int tokenPermanentExpiry) { this.tokenPermanentExpiry = tokenPermanentExpiry; }

    public int getMaxTokensPerProfile() { return maxTokensPerProfile; }
    public void setMaxTokensPerProfile(int maxTokensPerProfile) { this.maxTokensPerProfile = maxTokensPerProfile; }

    public int getWorldTreeKeyLength() { return worldTreeKeyLength; }
    public void setWorldTreeKeyLength(int worldTreeKeyLength) {
        this.worldTreeKeyLength = clampWorldTreeKeyLength(worldTreeKeyLength);
    }

    public static int clampWorldTreeKeyLength(int length) {
        return Math.max(MIN_WORLD_TREE_KEY_LENGTH, Math.min(MAX_WORLD_TREE_KEY_LENGTH, length));
    }

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

    public String getYggdrasilApiMode() { return yggdrasilApiMode; }
    public void setYggdrasilApiMode(String yggdrasilApiMode) {
        if (isValidApiMode(yggdrasilApiMode)) this.yggdrasilApiMode = yggdrasilApiMode;
    }

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
        return isSkinNameBlacklisted(name);
    }

    
    public boolean isTextureHashBlacklisted(String hash) {
        if (hash == null || hash.isBlank() || textureHashBlacklist == null || textureHashBlacklist.isBlank()) {
            return false;
        }
        String normalized = hash.trim();
        for (String line : textureHashBlacklist.split("\\r?\\n")) {
            String item = line.trim();
            if (!item.isEmpty() && item.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
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
