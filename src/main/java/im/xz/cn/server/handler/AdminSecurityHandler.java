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

import io.javalin.http.Context;

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
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSecurityPage(adminUsername, adminRole, csrfToken));
    }

    public void getSettings(Context ctx) {
        ctx.json(Map.of("encryptionLevel", systemConfig.getEncryptionLevel()));
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

        if (key.equals("encryption_level")) {
            int level = Integer.parseInt(value);
            if (level < 1 || level > 6) {
                ctx.status(400);
                ctx.json(Map.of("success", false, "message", "无效的加密等级，必须为 1-6"));
                return;
            }
            systemConfig.setEncryptionLevel(level);
        } else {
            ctx.status(400);
            ctx.json(Map.of("success", false, "message", "未知的设置项: " + key));
            return;
        }

        systemConfig.saveToDatabase(db);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "UPDATE_SECURITY:" + key, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "设置已保存"));
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
