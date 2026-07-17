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
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.something.web.AdminPage;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminAppInfoHandler {

    public AdminAppInfoHandler() {}

    public void appInfoPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAppInfoPage(adminUsername, adminRole, csrfToken));
    }

    public void getAppInfo(Context ctx) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("appName", AppConfig.APP_NAME);
        info.put("appVersion", AppConfig.APP_VERSION);
        info.put("appRepo", AppConfig.APP_REPO);
        info.put("repoUrl", "https://github.com/xiaLingLuo/LingYggdrasil-CE");
        info.put("installedAt", sysConfig.getInstalledAt() != null ? sysConfig.getInstalledAt() : "未知");
        ctx.json(info);
    }
}
