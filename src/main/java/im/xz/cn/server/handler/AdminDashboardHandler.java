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
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.database.TokenDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.something.web.AdminPage;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminDashboardHandler {
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final TokenDao tokenDao;
    private final AdminDao adminDao;

    public AdminDashboardHandler(UserDao userDao, ProfileDao profileDao, TokenDao tokenDao, AdminDao adminDao) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.tokenDao = tokenDao;
        this.adminDao = adminDao;
    }

    public void dashboardPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);

        ctx.html(AdminPage.renderDashboardPage(adminUsername, adminRole, csrfToken));
    }

    public void getStats(Context ctx) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userDao.count());
        stats.put("profileCount", profileDao.count());
        stats.put("activeTokenCount", tokenDao.countActive());
        stats.put("adminCount", adminDao.count());
        ctx.json(stats);
    }


}
