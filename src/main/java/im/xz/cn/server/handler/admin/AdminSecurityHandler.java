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


import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.AdminDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.Admin;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;

import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSecurityHandler {
    private final SystemConfig systemConfig;
    private final DatabaseManager db;
    private final AdminDao adminDao;

    public AdminSecurityHandler(SystemConfig systemConfig, DatabaseManager db) {
        this.systemConfig = systemConfig;
        this.db = db;
        this.adminDao = new AdminDao(db);
    }

    public void securityPage(Context ctx) {
        String adminUsername = getAdminUsername(ctx);
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSecurityPage(adminUsername, adminRole, csrfToken, SessionManager.isAdminRoot(ctx)));
    }

    public void getSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.security.view")) return;
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("encryptionLevel", systemConfig.getEncryptionLevel());
        settings.put("pngValidationEnabled", systemConfig.isPngValidationEnabled());
        settings.put("pngMaxWidth", systemConfig.getPngMaxWidth());
        settings.put("pngMaxHeight", systemConfig.getPngMaxHeight());
        settings.put("pngMaxPixels", systemConfig.getPngMaxPixels());
        settings.put("pngMaxChunkSizeKib", systemConfig.getPngMaxChunkSizeKib());
        settings.put("pngStrictChunkMode", systemConfig.isPngStrictChunkMode());
        settings.put("pngMaxConcurrent", systemConfig.getPngMaxConcurrent());
        settings.put("corsOrigins", systemConfig.getCorsOrigins());
        settings.put("headerCsp", systemConfig.getHeaderCsp());
        settings.put("headerHsts", systemConfig.getHeaderHsts());
        settings.put("headerContentTypeOptions", systemConfig.getHeaderContentTypeOptions());
        settings.put("headerFrameOptions", systemConfig.getHeaderFrameOptions());
        settings.put("headerXssProtection", systemConfig.getHeaderXssProtection());
        settings.put("headerReferrerPolicy", systemConfig.getHeaderReferrerPolicy());
        settings.put("headerPermissionsPolicy", systemConfig.getHeaderPermissionsPolicy());
        settings.put("headerCacheControl", systemConfig.getHeaderCacheControl());
        settings.put("userSessionTimeoutSeconds", systemConfig.getUserSessionTimeoutSeconds());
        settings.put("adminSessionTimeoutSeconds", systemConfig.getAdminSessionTimeoutSeconds());
        settings.put("loginMaxAttemptsPerIp", systemConfig.getLoginMaxAttemptsPerIp());
        settings.put("loginMaxAttemptsPerAccount", systemConfig.getLoginMaxAttemptsPerAccount());
        settings.put("loginLockoutSeconds", systemConfig.getLoginLockoutSeconds());
        settings.put("loginRateWindowSeconds", systemConfig.getLoginRateWindowSeconds());
        settings.put("requestIntervals", systemConfig.getRequestIntervals());
        settings.put("requestRates", systemConfig.getRequestRates());
        ctx.json(settings);
    }

    @SuppressWarnings("unchecked")
    public void updateSettings(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.security.edit")) return;
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (body == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        Object keyValue = body.get("key");
        String key = keyValue instanceof String ? (String) keyValue : null;

        if (key == null) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        if (key.equals("encryption_level")) {
            Integer level = parseInteger(body.get("value"));
            if (level == null || level < 1 || level > 6) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("msg.invalidEncryptionLevel")));
                return;
            }
            systemConfig.setEncryptionLevel(level);
        } else if (key.equals("png_upload_settings")) {
            Boolean enabled = parseBoolean(body.get("pngValidationEnabled"));
            Integer maxWidth = parseInteger(body.get("pngMaxWidth"));
            Integer maxHeight = parseInteger(body.get("pngMaxHeight"));
            Integer maxPixels = parseInteger(body.get("pngMaxPixels"));
            Integer maxChunkSizeKib = parseInteger(body.get("pngMaxChunkSizeKib"));
            Boolean strictChunkMode = parseBoolean(body.get("pngStrictChunkMode"));
            Integer maxConcurrent = parseInteger(body.get("pngMaxConcurrent"));
            if (enabled == null || maxWidth == null || maxWidth < 1 || maxWidth > 4096
                    || maxHeight == null || maxHeight < 1 || maxHeight > 4096
                    || maxPixels == null || maxPixels < 1 || maxPixels > 1_048_576
                    || maxChunkSizeKib == null || maxChunkSizeKib < 1 || maxChunkSizeKib > 16_384
                    || maxConcurrent == null || maxConcurrent < 1 || maxConcurrent > 99999
                    || strictChunkMode == null) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("admin.security.invalidPngSettings")));
                return;
            }
            systemConfig.setPngValidationEnabled(enabled);
            systemConfig.setPngMaxWidth(maxWidth);
            systemConfig.setPngMaxHeight(maxHeight);
            systemConfig.setPngMaxPixels(maxPixels);
            systemConfig.setPngMaxChunkSizeKib(maxChunkSizeKib);
            systemConfig.setPngStrictChunkMode(strictChunkMode);
            systemConfig.setPngMaxConcurrent(maxConcurrent);
        } else if (key.equals("frequency_settings")) {
            Integer userSession = parseInteger(body.get("userSessionTimeoutSeconds"));
            Integer adminSession = parseInteger(body.get("adminSessionTimeoutSeconds"));
            Integer perIp = parseInteger(body.get("loginMaxAttemptsPerIp"));
            Integer perAccount = parseInteger(body.get("loginMaxAttemptsPerAccount"));
            Integer lockout = parseInteger(body.get("loginLockoutSeconds"));
            Integer window = parseInteger(body.get("loginRateWindowSeconds"));
            String intervals = stringValue(body.get("requestIntervals"));
            String rates = stringValue(body.get("requestRates"));
            if (userSession == null || userSession < 1
                    || adminSession == null || adminSession < 1
                    || perIp == null || perIp < 1
                    || perAccount == null || perAccount < 1
                    || lockout == null || lockout < 1
                    || window == null || window < 1
                    || intervals == null || rates == null) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("admin.security.invalidFrequencySettings")));
                return;
            }
            if (intervals.isBlank()) intervals = SystemConfig.DEFAULT_REQUEST_INTERVALS;
            if (rates.isBlank()) rates = SystemConfig.DEFAULT_REQUEST_RATES;
            if (!SystemConfig.isValidRequestMap(intervals) || !SystemConfig.isValidRequestMap(rates)) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("admin.security.invalidRequestMap")));
                return;
            }
            systemConfig.setUserSessionTimeoutSeconds(userSession);
            systemConfig.setAdminSessionTimeoutSeconds(adminSession);
            systemConfig.setLoginMaxAttemptsPerIp(perIp);
            systemConfig.setLoginMaxAttemptsPerAccount(perAccount);
            systemConfig.setLoginLockoutSeconds(lockout);
            systemConfig.setLoginRateWindowSeconds(window);
            systemConfig.setRequestIntervals(intervals);
            systemConfig.setRequestRates(rates);
        } else if (key.equals("cors_security_settings")) {
            String cors = stringValue(body.get("corsOrigins"));
            String csp = stringValue(body.get("headerCsp"));
            String hsts = stringValue(body.get("headerHsts"));
            String contentTypeOptions = stringValue(body.get("headerContentTypeOptions"));
            String frameOptions = stringValue(body.get("headerFrameOptions"));
            String xssProtection = stringValue(body.get("headerXssProtection"));
            String referrerPolicy = stringValue(body.get("headerReferrerPolicy"));
            String permissionsPolicy = stringValue(body.get("headerPermissionsPolicy"));
            String cacheControl = stringValue(body.get("headerCacheControl"));
            if (cors == null || csp == null || hsts == null || contentTypeOptions == null
                    || frameOptions == null || xssProtection == null || referrerPolicy == null
                    || permissionsPolicy == null || cacheControl == null) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("admin.security.invalidCorsSettings")));
                return;
            }
            if (!csp.contains("{nonce}")) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", I18n.t("admin.security.cspRequiresNonce")));
                return;
            }
            for (String line : cors.split("\\r?\\n")) {
                String origin = line.trim();
                if (!origin.isEmpty() && !SystemConfig.isValidCorsOrigin(origin)) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", I18n.t("admin.security.invalidCorsOrigins")));
                    return;
                }
            }
            systemConfig.setCorsOrigins(cors);
            systemConfig.setHeaderCsp(csp);
            systemConfig.setHeaderHsts(hsts);
            systemConfig.setHeaderContentTypeOptions(contentTypeOptions);
            systemConfig.setHeaderFrameOptions(frameOptions);
            systemConfig.setHeaderXssProtection(xssProtection);
            systemConfig.setHeaderReferrerPolicy(referrerPolicy);
            systemConfig.setHeaderPermissionsPolicy(permissionsPolicy);
            systemConfig.setHeaderCacheControl(cacheControl);
        } else {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.unknownSetting", key)));
            return;
        }

        systemConfig.saveToDatabase(db);
        String auditKey = switch (key) {
            case "png_upload_settings" -> "PNG_UPLOAD_SETTINGS";
            case "frequency_settings" -> "FREQUENCY_SETTINGS";
            case "cors_security_settings" -> "CORS_SECURITY_SETTINGS";
            default -> key;
        };
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_SECURITY:" + auditKey, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.saveSuccess")));
    }

    private static Integer parseInteger(Object value) {
        if (!(value instanceof Number) && !(value instanceof String)) return null;
        try {
            return new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string.trim())) return true;
            if ("false".equalsIgnoreCase(string.trim())) return false;
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : null;
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
        return SessionManager.isAdminRoot(ctx);
    }
}
