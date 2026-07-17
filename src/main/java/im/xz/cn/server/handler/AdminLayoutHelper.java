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

import im.xz.cn.something.web.AdminPage;

public class AdminLayoutHelper {

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content) {
        return AdminPage.renderAdminLayout(currentPage, adminUsername, adminRole, content);
    }

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content, String csrfToken) {
        return AdminPage.renderAdminLayout(currentPage, adminUsername, adminRole, content, csrfToken);
    }
}
