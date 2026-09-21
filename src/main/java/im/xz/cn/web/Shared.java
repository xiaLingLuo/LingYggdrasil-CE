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

package im.xz.cn.web;

import im.xz.cn.web.PageRenderer;
import im.xz.cn.i18n.I18n;

public class Shared {

    public static String buildUserSidebar(String currentPage) {
        StringBuilder items = new StringBuilder();
        items.append(userSidebarItem("/dashboard", "dashboard", "fa-gauge-high", I18n.t("sidebar.dashboard"), currentPage));
        if (im.xz.cn.security.UserPermissions.has("user.profiles")) {
            items.append(userSidebarItem("/profiles", "profiles", "fa-users", I18n.t("sidebar.profiles"), currentPage));
        }
        if (im.xz.cn.security.UserPermissions.has("user.skins")) {
            items.append(userSidebarItem("/skins", "skins", "fa-shirt", I18n.t("sidebar.skins"), currentPage));
        }
        if (im.xz.cn.security.UserPermissions.has("user.capes")) {
            items.append(userSidebarItem("/capes", "capes", "fa-vest-patches", I18n.t("sidebar.capes"), currentPage));
        }
        if (im.xz.cn.security.UserPermissions.has("user.world")) {
            items.append(userSidebarItem("/shared", "shared", "fa-share-nodes", I18n.t("sidebar.shared"), currentPage));
        }
        if (im.xz.cn.security.UserPermissions.has("user.friends")) {
            items.append(userSidebarItem("/friends", "friends", "fa-user-group", I18n.t("sidebar.friends"), currentPage));
        }
        if (im.xz.cn.security.UserPermissions.has("user.settings")) {
            items.append(userSidebarItem("/settings", "settings", "fa-gear", I18n.t("sidebar.settings"), currentPage));
            items.append(userSidebarItem("/logs", "logs", "fa-clipboard-list", I18n.t("sidebar.logs"), currentPage));
        }
        items.append("<a href=\"/logout\" class=\"sidebar-item\"><span><i class=\"fas fa-right-from-bracket\"></i> ")
                .append(I18n.t("sidebar.logout")).append("</span></a>");
        return """
            <aside class="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-icon"><i class="fas fa-bars"></i></span>
                    <span>%s</span>
                </div>
                <div class="sidebar-menu">
                    %s
                </div>
            </aside>
            """.formatted(I18n.t("sidebar.userMenu"), items.toString());
    }

    private static String userSidebarItem(String href, String page, String icon, String label, String currentPage) {
        String active = page.equals(currentPage) ? "active" : "";
        return "<a href=\"" + href + "\" class=\"sidebar-item " + active
                + "\"><span><i class=\"fas " + icon + "\"></i> " + label + "</span></a>\n";
    }

    public static String buildUserLayout(String siteName, String currentPage, String content, String jsName) {
        String navbar = PageRenderer.renderNavbar(siteName, "user", false);
        String sidebar = buildUserSidebar(currentPage);
        return navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            <script src="/js/user-%s.js"></script>
            """.formatted(sidebar, content, jsName);
    }

    public static String csrfInject(String csrfToken) {
        if (csrfToken == null) return "";
        String safeToken = esc(csrfToken);
        String jsToken = csrfToken.replace("\\", "\\\\").replace("'", "\\'");
        return "<meta name=\"csrf-token\" content=\"" + safeToken + "\"><script>window.CSRF_TOKEN='" + jsToken + "';</script>";
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
