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

import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.Admin;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.YggdrasilKeyManager;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminYggdrasilHandler {
    private final SystemConfig systemConfig;
    private final DatabaseManager db;
    private final AdminDao adminDao;

    public AdminYggdrasilHandler(SystemConfig systemConfig, DatabaseManager db) {
        this.systemConfig = systemConfig;
        this.db = db;
        this.adminDao = new AdminDao(db);
    }

    public void yggdrasilPage(Context ctx) {
        String adminUsername = getAdminUsername(ctx);
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderYggdrasilPage(adminUsername, adminRole, csrfToken));
    }

    public void getSettings(Context ctx) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("uuidVersion", systemConfig.getUuidVersion());
        settings.put("tokenTempExpiry", systemConfig.getTokenTempExpiry());
        settings.put("tokenPermanentExpiry", systemConfig.getTokenPermanentExpiry());
        settings.put("maxTokensPerProfile", systemConfig.getMaxTokensPerProfile());
        settings.put("authRateLimit", systemConfig.getAuthRateLimit());
        settings.put("batchQueryMaxCount", systemConfig.getBatchQueryMaxCount());
        settings.put("signatureMode", systemConfig.getSignatureMode());
        settings.put("yggdrasilPublicKey", systemConfig.getYggdrasilPublicKey());
        settings.put("yggdrasilPrivateKey", systemConfig.getYggdrasilPrivateKey());
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
            case "uuid_version":
                if (!value.equals("v3") && !value.equals("v4") && !value.equals("v5")) {
                    ctx.status(400);
                    ctx.json(Map.of("success", false, "message", "无效的 UUID 版本"));
                    return;
                }
                systemConfig.setUuidVersion(value);
                break;
            case "token_temp_expiry":
                systemConfig.setTokenTempExpiry(parseInt(value, 4320));
                break;
            case "token_permanent_expiry":
                systemConfig.setTokenPermanentExpiry(parseInt(value, 10080));
                break;
            case "max_tokens_per_profile":
                systemConfig.setMaxTokensPerProfile(parseInt(value, 12));
                break;
            case "auth_rate_limit":
                systemConfig.setAuthRateLimit(parseInt(value, 1000));
                break;
            case "batch_query_max_count":
                systemConfig.setBatchQueryMaxCount(parseInt(value, 6));
                break;
            case "yggdrasil_private_key":
                systemConfig.setYggdrasilPrivateKey(value);
                reloadKeyManager();
                break;
            case "yggdrasil_public_key":
                systemConfig.setYggdrasilPublicKey(value);
                reloadKeyManager();
                break;
            default:
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", "未知的设置项: " + key));
                return;
        }

        systemConfig.saveToDatabase(db);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_YGGDRASIL_SETTINGS:" + key, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "设置已保存"));
    }

    public void regenerateKeys(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        try {
            String mode = systemConfig.getSignatureMode();
            YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
            km.generateKeyPair(mode);

            String pubPem = km.getPublicKeyPem();
            String privPem = km.getPrivateKeyPem();
            systemConfig.setYggdrasilPublicKey(pubPem);
            systemConfig.setYggdrasilPrivateKey(privPem);
            systemConfig.saveToDatabase(db);

            AuditLogger.logSensitiveOperation(getAdminName(ctx), "REGENERATE_KEYS:" + mode, IpUtil.getClientIp(ctx));
            ctx.json(Map.of("success", true, "message", "密钥对已重新生成",
                    "publicKey", pubPem, "privateKey", privPem));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(Map.of("success", false, "message", "密钥生成失败: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    public void switchMode(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String newMode = body.get("mode");

        if (!YggdrasilKeyManager.MODE_ED448.equals(newMode)
                && !YggdrasilKeyManager.MODE_RSA_SHA512.equals(newMode)
                && !YggdrasilKeyManager.MODE_RSA_SHA1.equals(newMode)) {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", "无效的签名模式"));
            return;
        }

        try {
            systemConfig.setSignatureMode(newMode);
            YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
            km.generateKeyPair(newMode);

            String pubPem = km.getPublicKeyPem();
            String privPem = km.getPrivateKeyPem();
            systemConfig.setYggdrasilPublicKey(pubPem);
            systemConfig.setYggdrasilPrivateKey(privPem);
            systemConfig.saveToDatabase(db);

            AuditLogger.logSensitiveOperation(getAdminName(ctx), "SWITCH_SIGNATURE_MODE:" + newMode, IpUtil.getClientIp(ctx));
            ctx.json(Map.of("success", true, "message", "证书模式已切换为 " + newMode,
                    "publicKey", pubPem, "privateKey", privPem));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(Map.of("success", false, "message", "切换失败: " + e.getMessage()));
        }
    }

    private void reloadKeyManager() {
        YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
        km.loadFromPem(
                systemConfig.getYggdrasilPrivateKey(),
                systemConfig.getYggdrasilPublicKey(),
                systemConfig.getSignatureMode()
        );
    }

    private int parseInt(String value, int defaultValue) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
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
