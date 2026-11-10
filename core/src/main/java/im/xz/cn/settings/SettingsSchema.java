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
package im.xz.cn.settings;

import java.util.List;


public final class SettingsSchema {

    public record Option(String value, String labelKey) {
    }

    public record Field(String key, String type, String labelKey, String descriptionKey,
                        String placeholderKey, List<Option> options,
                        Integer min, Integer max, boolean secret, boolean advanced,
                        boolean restartRequired) {
    }

    public record Group(String name, String labelKey, String descriptionKey,
                        List<Field> fields, List<String> actions) {
    }

    public static final List<Group> GROUPS = List.of(
            new Group("site", "admin.system.basic", "admin.system.basicDesc", List.of(
                    field("site_name", "text", "admin.system.siteName", "admin.system.siteNameDesc", "admin.system.siteNamePlaceholder"),
                    field("site_description", "textarea", "admin.system.siteDesc", "admin.system.siteDescDesc", "admin.system.siteDescPlaceholder"),
                    field("site_keywords", "text", "admin.system.siteKeywords", "admin.system.siteKeywordsDesc", "admin.system.siteKeywordsPlaceholder"),
                    new Field("announcement_mode", "select", "admin.system.announcementMode", "admin.system.announcementModeDesc", null,
                            List.of(new Option("off", "admin.system.annOff"), new Option("toast", "admin.system.annToast"),
                                    new Option("modal", "admin.system.annModal"), new Option("top", "admin.system.annTop"),
                                    new Option("top_force", "admin.system.annTopForce")), null, null, false, false, false),
                    new Field("announcement_scope", "multiselect", "admin.system.announcementScope", "admin.system.announcementScopeDesc", null,
                            List.of(new Option("user", "admin.system.scopeUser"), new Option("admin", "admin.system.scopeAdmin"),
                                    new Option("home", "admin.system.scopeHome")), null, null, false, false, false),
                    field("announcement_content", "textarea", "admin.system.announcementContent", "admin.system.announcementContentDesc", "admin.system.announcementContentPlaceholder"),
                    field("icp_record", "text", "admin.system.icpRecord", "admin.system.icpRecordDesc", "admin.system.icpRecordPlaceholder"),
                    field("public_security_record", "text", "admin.system.publicSecurityRecord", "admin.system.publicSecurityRecordDesc", "admin.system.publicSecurityRecordPlaceholder"),
                    field("user_domain", "url", "admin.system.userDomain", "admin.system.userDomainDesc", "admin.system.domainPlaceholder"),
                    field("admin_domain", "url", "admin.system.adminDomain", "admin.system.adminDomainDesc", "admin.system.adminDomainPlaceholder"),
                    field("api_domain", "url", "admin.system.apiDomain", "admin.system.apiDomainDesc", "admin.system.apiDomainPlaceholder"),
                    field("common_domain", "url", "admin.system.commonDomain", "admin.system.commonDomainDesc", "admin.system.domainPlaceholder")
            ), List.of()),
            new Group("identity", "admin.system.features", "admin.system.featuresDesc", List.of(
                    new Field("registration_enabled", "boolean", "admin.system.registration", "admin.system.registrationDesc", null,
                            null, null, null, false, false, false),
                    new Field("email_verification_enabled", "boolean", "admin.system.emailVerification", "admin.system.emailVerificationDesc", null,
                            null, null, null, false, false, false),
                    new Field("email_domain_mode", "select", "admin.system.emailDomainMode", "admin.system.emailDomainModeDesc", null,
                            List.of(new Option("blacklist", "admin.system.emailModeBlacklist"), new Option("whitelist", "admin.system.emailModeWhitelist")),
                            null, null, false, false, false),
                    field("email_domain_list", "textarea", "admin.system.emailDomainList", "admin.system.emailDomainListDesc", null),
                    field("username_blacklist", "textarea", "admin.users.blacklistLabel", "admin.users.blacklistDesc", null),
                    new Field("username_blacklist_case_sensitive", "boolean", "admin.users.caseSensitive", "admin.users.caseSensitiveDesc", null,
                            null, null, null, false, false, false),
                    new Field("max_accounts_per_ip", "number", "admin.profiles.maxAccountsPerIp", "admin.profiles.maxAccountsPerIpDesc", null,
                            null, 0, 10000, false, false, false),
                    new Field("max_blocked_users", "number", "admin.users.blacklistTitle", "admin.users.blacklistDesc", null,
                            null, 0, 5000, false, false, false)
            ), List.of()),
            new Group("mail", "admin.system.mailSection", "admin.system.mailSectionDesc", List.of(
                    new Field("mail_enabled", "boolean", "admin.system.mailEnabled", "admin.system.mailEnabledDesc", null,
                            null, null, null, false, false, false),
                    field("mail_host", "text", "admin.system.mailHost", "admin.system.mailHostDesc", "admin.system.mailHostPlaceholder"),
                    new Field("mail_port", "number", "admin.system.mailPort", "admin.system.mailPortDesc", null,
                            null, 1, 65535, false, false, false),
                    field("mail_username", "text", "admin.system.mailUsername", "admin.system.mailUsernameDesc", "admin.system.mailUsernamePlaceholder"),
                    new Field("mail_password", "password", "admin.system.mailPassword", "admin.system.mailPasswordDesc", null,
                            null, null, null, true, false, false),
                    field("mail_from", "email", "admin.system.mailFrom", "admin.system.mailFromDesc", "admin.system.mailFromPlaceholder"),
                    new Field("mail_tls", "boolean", "admin.system.mailTls", "admin.system.mailTlsDesc", null,
                            null, null, null, false, false, false),
                    new Field("mail_template_verify", "textarea", "admin.system.mailTemplateVerify", "admin.system.mailTemplateDesc", null,
                            null, null, null, false, true, false),
                    new Field("mail_template_email_change", "textarea", "admin.system.mailTemplateEmailChange", "admin.system.mailTemplateDesc", null,
                            null, null, null, false, true, false),
                    new Field("mail_template_password_change", "textarea", "admin.system.mailTemplatePasswordChange", "admin.system.mailTemplateDesc", null,
                            null, null, null, false, true, false)
            ), List.of("test-mail")),
            new Group("logging", "日志与审计", "用户操作日志的容量、保留与记录范围。", List.of(
                    new Field("user_action_log_enabled", "boolean", "admin.users.logEnabled", "admin.users.logEnabledDesc", null,
                            null, null, null, false, false, false),
                    new Field("user_action_log_max_kib", "number", "admin.users.logMaxKib", "admin.users.logMaxKibDesc", null,
                            null, 8, 1048576, false, false, false),
                    new Field("user_action_log_retention_days", "number", "admin.users.logRetentionDays", "admin.users.logRetentionDaysDesc", null,
                            null, 1, 36500, false, false, false),
                    new Field("user_action_log_download_interval_minutes", "number", "admin.users.logDownloadInterval", "admin.users.logDownloadIntervalDesc", null,
                            null, 1, 100000, false, false, false),
                    new Field("user_action_log_clear_interval_minutes", "number", "admin.users.logClearInterval", "admin.users.logClearIntervalDesc", null,
                            null, 1, 100000, false, false, false),
                    new Field("user_action_log_actions", "multiselect", "admin.users.logActions", "admin.users.logActionsDesc", null,
                            List.of(new Option("login", "userlog.login"), new Option("profile", "userlog.profile"),
                                    new Option("texture", "userlog.texture"), new Option("friend", "userlog.friend"),
                                    new Option("password", "userlog.password"), new Option("blacklist", "userlog.blacklist"),
                                    new Option("email", "userlog.email")), null, null, false, false, false)
            ), List.of()),
            new Group("texture", "纹理设置", "皮肤与披风共用同一份上传、下载、存储与命名限制。", List.of(
                    new Field("skin_max_size", "number", "admin.skins.maxSize", "admin.skins.maxSizeDesc", null, null, 1, 1024, false, false, false),
                    new Field("skin_max_count", "number", "admin.skins.maxCount", "admin.skins.maxCountDesc", null, null, 0, 100000, false, false, false),
                    new Field("skin_max_total_size", "number", "admin.skins.maxTotalSize", "admin.skins.maxTotalSizeDesc", null, null, 0, 10485760, false, false, false),
                    new Field("skin_rate_limit", "number", "admin.skins.rateLimit", "admin.skins.rateLimitDesc", null, null, -1, 100000, false, false, false),
                    field("skin_storage_path", "text", "admin.skins.storagePath", "admin.skins.storagePathDesc", null),
                    new Field("allow_download_skin", "boolean", "admin.skins.allowDownload", "admin.skins.allowDownloadDesc", null, null, null, null, false, false, false),
                    new Field("max_favorites", "number", "admin.skins.maxFavorites", "admin.skins.maxFavoritesDesc", null, null, -1, 1000, false, false, false),
                    field("skin_name_blacklist", "textarea", "admin.nameBlacklist.skinLabel", "admin.nameBlacklist.skinDesc", null),
                    new Field("skin_name_blacklist_case_sensitive", "boolean", "admin.nameBlacklist.caseSensitive", "admin.nameBlacklist.caseSensitiveDesc", null, null, null, null, false, false, false),
                    field("texture_hash_blacklist", "textarea", "admin.nameBlacklist.textureHashLabel", "admin.nameBlacklist.textureHashDesc", null)
            ), List.of()),
            new Group("legacy", "admin.system.more", "admin.system.moreDesc", List.of(
                    new Field("treasure_enabled", "boolean", "admin.system.treasure", "admin.system.treasureDesc", null,
                            null, null, null, false, false, false)
            ), List.of())
    );

    private SettingsSchema() {
    }

    private static Field field(String key, String type, String labelKey, String descriptionKey, String placeholderKey) {
        return new Field(key, type, labelKey, descriptionKey, placeholderKey, null, null, null, false, false, false);
    }

    public static Group group(String name) {
        return GROUPS.stream().filter(group -> group.name().equals(name)).findFirst().orElse(null);
    }
}
