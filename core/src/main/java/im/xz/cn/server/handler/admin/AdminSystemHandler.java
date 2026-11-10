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
import im.xz.cn.config.ServerConfig;
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
            language = LocaleResolver.normalize(r != null ? r.getLanguage() : null);
        } else {
            Admin admin = adminId != null ? adminDao.findById(adminId) : null;
            language = LocaleResolver.normalize(admin != null ? admin.getLanguage() : null);
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
        if (language == null || !LocaleResolver.isSupported(language)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.invalidLanguage")));
            return;
        }
        String normalized = LocaleResolver.normalize(language);
        if (isRoot(ctx)) {
            rootInfoDao.updateLanguage(adminId, normalized);
        } else {
            adminDao.updateLanguage(adminId, normalized);
        }
        SessionManager.setLanguage(ctx, normalized);
        ctx.json(Map.of("success", true, "language", normalized));
    }



    public void systemPage(Context ctx) {
        String adminUsername = getAdminUsername(ctx);
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSystemPage(adminUsername, adminRole, csrfToken));
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
