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
        String dashActive = "dashboard".equals(currentPage) ? "active" : "";
        String profActive = "profiles".equals(currentPage) ? "active" : "";
        String skinActive = "skins".equals(currentPage) ? "active" : "";
        String capeActive = "capes".equals(currentPage) ? "active" : "";
        String sharedActive = "shared".equals(currentPage) ? "active" : "";
        String friendsActive = "friends".equals(currentPage) ? "active" : "";
        String setActive = "settings".equals(currentPage) ? "active" : "";
        String logsActive = "logs".equals(currentPage) ? "active" : "";
        return """
            <aside class="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-icon"><i class="fas fa-bars"></i></span>
                    <span>%s</span>
                </div>
                <div class="sidebar-menu">
                    <a href="/dashboard" class="sidebar-item %s"><span><i class="fas fa-gauge-high"></i> %s</span></a>
                    <a href="/profiles" class="sidebar-item %s"><span><i class="fas fa-users"></i> %s</span></a>
                    <a href="/skins" class="sidebar-item %s"><span><i class="fas fa-shirt"></i> %s</span></a>
                    <a href="/capes" class="sidebar-item %s"><span><i class="fas fa-vest-patches"></i> %s</span></a>
                    <a href="/shared" class="sidebar-item %s"><span><i class="fas fa-share-nodes"></i> %s</span></a>
                    <a href="/friends" class="sidebar-item %s"><span><i class="fas fa-user-group"></i> %s</span></a>
                    <a href="/settings" class="sidebar-item %s"><span><i class="fas fa-gear"></i> %s</span></a>
                    <a href="/logs" class="sidebar-item %s"><span><i class="fas fa-clipboard-list"></i> %s</span></a>
                    <a href="/logout" class="sidebar-item"><span><i class="fas fa-right-from-bracket"></i> %s</span></a>
                </div>
            </aside>
            """.formatted(
                I18n.t("sidebar.userMenu"),
                dashActive, I18n.t("sidebar.dashboard"),
                profActive, I18n.t("sidebar.profiles"),
                skinActive, I18n.t("sidebar.skins"),
                capeActive, I18n.t("sidebar.capes"),
                sharedActive, I18n.t("sidebar.shared"),
                friendsActive, I18n.t("sidebar.friends"),
                setActive, I18n.t("sidebar.settings"),
                logsActive, I18n.t("sidebar.logs"),
                I18n.t("sidebar.logout"));
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
