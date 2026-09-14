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
import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.AdminLoginRateLimiter;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.database.dao.RootInfoDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.RootInfo;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.web.PageRenderer;

import io.javalin.http.Context;

import java.util.Map;

public class AdminAuthHandler {
    private final AuthService authService;
    private final AdminLoginRateLimiter rateLimiter;
    private final RootInfoDao rootInfoDao;

    public AdminAuthHandler(AuthService authService, CacheDao cacheDao, RootInfoDao rootInfoDao) {
        this.authService = authService;
        this.rateLimiter = new AdminLoginRateLimiter(cacheDao);
        this.rootInfoDao = rootInfoDao;
    }

    public void loginPage(Context ctx) {
        ctx.html(AdminPage.renderLoginPage());
    }

    public void login(Context ctx) {
        var body = ctx.bodyAsClass(Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.usernamePasswordEmpty")));
            return;
        }

        String clientIp = IpUtil.getClientIp(ctx);
        String rateLimitMsg = rateLimiter.checkRateLimit("admin:" + username, clientIp);
        if (rateLimitMsg != null) {
            ctx.json(Map.of("success", false, "message", rateLimitMsg));
            return;
        }

        RootInfo root = rootInfoDao.findByUsername(username);
        if (root != null) {
            if (!Argon2Hasher.verify(password, root.getPasswordHash())) {
                AuditLogger.logLogin("admin:" + username, clientIp, false);
                ctx.json(Map.of("success", false, "message", I18n.t("msg.usernamePasswordWrong")));
                return;
            }
            rateLimiter.recordSuccess("admin:" + username);
            AuditLogger.logLogin("admin:" + username, clientIp, true);
            SessionManager.invalidateAndRenewAdmin(ctx);
            SessionManager.setAdminId(ctx, root.getId());
            SessionManager.setAdminRoot(ctx, true);
            SessionManager.setAdminPermGroup(ctx, null);
            ctx.sessionAttribute("adminUsername", root.getUsername());
            SessionManager.setLanguage(ctx, LocaleResolver.normalize(root.getLanguage()));
            SessionManager.bindClientFingerprint(ctx);
            try {
                rootInfoDao.updateLastLogin(root.getId(), TimeUtil.now());
            } catch (Exception ignored) {  }
            ctx.json(Map.of("success", true, "redirect", "/admin/dashboard"));
            return;
        }

        Admin admin = authService.authenticateAdmin(username, password);
        if (admin == null) {
            AuditLogger.logLogin("admin:" + username, clientIp, false);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.usernamePasswordWrong")));
            return;
        }

        rateLimiter.recordSuccess("admin:" + username);
        AuditLogger.logLogin("admin:" + username, clientIp, true);

        SessionManager.invalidateAndRenewAdmin(ctx);
        SessionManager.setAdminId(ctx, admin.getId());
        SessionManager.setAdminRoot(ctx, false);
        SessionManager.setAdminPermGroup(ctx, admin.getPermGroup());
        ctx.sessionAttribute("adminUsername", admin.getUsername());
        SessionManager.setLanguage(ctx, LocaleResolver.normalize(admin.getLanguage()));
        SessionManager.bindClientFingerprint(ctx);
        try {
            authService.getAdminDao().updateLastLogin(admin.getId(), TimeUtil.now());
        } catch (Exception ignored) {  }
        ctx.json(Map.of("success", true, "redirect", "/admin/dashboard"));
    }

    public void logout(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String ip = IpUtil.getClientIp(ctx);
        AuditLogger.logLogout("admin:" + (adminUsername != null ? adminUsername : "unknown"), ip);
        SessionManager.invalidateAdmin(ctx);
        ctx.redirect("/admin/login");
    }
}
