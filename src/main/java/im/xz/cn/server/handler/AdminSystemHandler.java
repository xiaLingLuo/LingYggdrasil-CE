package im.xz.cn.server.handler;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.CacheDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.TokenDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSystemHandler {
    private final SystemConfig systemConfig;
    private final CacheDao cacheDao;
    private final TokenDao tokenDao;
    private final DatabaseManager db;
    private final AdminDao adminDao;

    public AdminSystemHandler(SystemConfig systemConfig, CacheDao cacheDao, TokenDao tokenDao, DatabaseManager db) {
        this.systemConfig = systemConfig;
        this.cacheDao = cacheDao;
        this.tokenDao = tokenDao;
        this.db = db;
        this.adminDao = new AdminDao(db);
    }

    public void systemPage(Context ctx) {
        String adminUsername = getAdminUsername(ctx);
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSystemPage(adminUsername, adminRole, csrfToken));
    }

    public void getSettings(Context ctx) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("siteName", systemConfig.getSiteName());
        settings.put("siteDescription", systemConfig.getSiteDescription());
        settings.put("registrationEnabled", systemConfig.isRegistrationEnabled());
        settings.put("emailVerificationEnabled", systemConfig.isEmailVerificationEnabled());
        settings.put("uuidVersion", systemConfig.getUuidVersion());

        settings.put("userDomain", systemConfig.getUserDomain());
        settings.put("adminDomain", systemConfig.getAdminDomain());
        settings.put("apiDomain", systemConfig.getApiDomain());
        settings.put("commonDomain", systemConfig.getCommonDomain());

        settings.put("usernameBlacklist", systemConfig.getUsernameBlacklist());
        settings.put("usernameBlacklistCaseSensitive", systemConfig.isUsernameBlacklistCaseSensitive());
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
        settings.put("maxAccountsPerIp", systemConfig.getMaxAccountsPerIp());

        MailConfig mail = AppConfig.getInstance().getMailConfig();
        settings.put("mailEnabled", mail.isEnabled());
        settings.put("mailHost", mail.getHost() != null ? mail.getHost() : "");
        settings.put("mailPort", mail.getPort());
        settings.put("mailUsername", mail.getUsername() != null ? mail.getUsername() : "");
        settings.put("mailFrom", mail.getFrom() != null ? mail.getFrom() : "");
        settings.put("mailTls", mail.isTls());

        ctx.json(settings);
    }

    @SuppressWarnings("unchecked")
    public void updateSettings(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String key = body.get("key");
        String value = body.get("value");

        if (key == null || value == null) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", "参数缺失"));
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
            case "email_domain_list":
                systemConfig.setEmailDomainList(value);
                break;
            case "email_domain_mode":
                if (!value.equals("blacklist") && !value.equals("whitelist")) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", "无效的域名过滤模式"));
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
            case "max_accounts_per_ip":
                systemConfig.setMaxAccountsPerIp(Integer.parseInt(value));
                break;
            default:
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", "未知的设置项: " + key));
                return;
        }
        systemConfig.saveToDatabase(db);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_SETTINGS:" + key, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "设置已保存"));
    }

    @SuppressWarnings("unchecked")
    public void updateMail(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, Object> body = ctx.bodyAsClass(Map.class);

        MailConfig mail = AppConfig.getInstance().getMailConfig();

        if (body.containsKey("enabled")) mail.setEnabled(Boolean.parseBoolean(String.valueOf(body.get("enabled"))));
        if (body.containsKey("host")) mail.setHost(String.valueOf(body.get("host")));
        if (body.containsKey("port")) {
            try { mail.setPort(Integer.parseInt(String.valueOf(body.get("port")))); } catch (NumberFormatException ignored) {}
        }
        if (body.containsKey("username")) mail.setUsername(String.valueOf(body.get("username")));
        if (body.containsKey("password")) {
            String pwd = String.valueOf(body.get("password"));
            if (!pwd.isEmpty()) mail.setPassword(pwd);
        }
        if (body.containsKey("from")) mail.setFrom(String.valueOf(body.get("from")));
        if (body.containsKey("tls")) mail.setTls(Boolean.parseBoolean(String.valueOf(body.get("tls"))));

        if (body.containsKey("enabled")) {
            systemConfig.setEmailVerificationEnabled(mail.isEnabled());
            systemConfig.saveToDatabase(db);
        }

        AppConfig.getInstance().saveConfig();
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_MAIL_CONFIG", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "邮箱配置已保存"));
    }

    public void clearCache(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        cacheDao.cleanExpired();
        tokenDao.cleanExpired();
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "CLEAR_CACHE", IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "缓存已清除"));
    }

    private String getAdminUsername(Context ctx) {
        String username = ctx.sessionAttribute("adminUsername");
        return username != null ? username : "Admin";
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
