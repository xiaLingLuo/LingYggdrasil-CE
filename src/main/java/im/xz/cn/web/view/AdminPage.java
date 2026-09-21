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

package im.xz.cn.web.view;

import static im.xz.cn.web.Shared.esc;
import im.xz.cn.common.FooterInfo;
import im.xz.cn.web.PageRenderer;
import im.xz.cn.web.Shared;

public class AdminPage {

    private static final class I18n {
        static String t(String key, Object... args) {
            return im.xz.cn.i18n.AdminI18n.t(key, args);
        }

        static String tOrNull(String key) {
            return im.xz.cn.i18n.AdminI18n.tOrNull(key);
        }
    }

    private static final java.util.regex.Pattern I18N_TOKEN =
            java.util.regex.Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");

    private static String tr(String template) {
        java.util.regex.Matcher m = I18N_TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(I18n.t(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content) {
        return renderAdminLayout(currentPage, adminUsername, adminRole, content, null);
    }

    public static String renderAdminLayout(String currentPage, String adminUsername, String roleLabel, String content, String csrfToken) {
        String roleDisplay = (roleLabel == null || roleLabel.isBlank()) ? "Root" : roleLabel;
        String safeUsername = esc(adminUsername);
        String safeInitial = adminUsername != null && !adminUsername.isEmpty() ? esc(adminUsername.substring(0, 1).toUpperCase()) : "A";
        String csrfInject = Shared.csrfInject(csrfToken);

        StringBuilder nav = new StringBuilder();
        appendIfPermitted(nav, "admin.dashboard.view", "/admin/dashboard", "dashboard", "adminSidebar.dashboard", "fa-gauge-high", currentPage);
        appendIfPermitted(nav, "admin.system.view", "/admin/system", "system", "adminSidebar.system", "fa-gear", currentPage);
        appendIfPermitted(nav, "admin.yggdrasil.view", "/admin/yggdrasil", "yggdrasil", "adminSidebar.yggdrasil", "fa-tree", currentPage);
        appendIfPermitted(nav, "admin.users.view", "/admin/users", "users", "adminSidebar.users", "fa-users", currentPage);
        appendIfPermitted(nav, "admin.profiles.view", "/admin/profiles", "profiles", "adminSidebar.profiles", "fa-gamepad", currentPage);
        appendIfPermitted(nav, "admin.skins.view", "/admin/skins", "skins", "adminSidebar.skins", "fa-palette", currentPage);
        appendIfPermitted(nav, "admin.capes.view", "/admin/capes", "capes", "adminSidebar.capes", "fa-mask", currentPage);
        appendIfPermitted(nav, "admin.security.view", "/admin/security", "security", "adminSidebar.security", "fa-lock", currentPage);
        appendIfPermitted(nav, "admin.admins.view", "/admin/admins", "admins", "adminSidebar.admins", "fa-user-shield", currentPage);
        if (im.xz.cn.security.AdminPermissions.has("admin.plugin.overall.view")) {
            String pluginPage = (currentPage != null && currentPage.startsWith("plugin:")) ? "plugins" : currentPage;
            nav.append(adminSidebarItem("/admin/plugins", "plugins", "adminSidebar.plugins", "fa-puzzle-piece", pluginPage));
        }
        appendIfPermitted(nav, "admin.appinfo.view", "/admin/appinfo", "appinfo", "adminSidebar.appinfo", "fa-circle-info", currentPage);

        String layout = csrfInject + """
            <div class="admin-layout-wrap">
                <aside class="admin-sidebar">
                    <div class="sidebar-brand">
                        <img src="/icons/app.ico" class="logo-icon sidebar-logo" alt="LingYggdrasil">
                        <span class="sidebar-title">%s</span>
                    </div>
                    <nav class="sidebar-nav">
                        %s
                    </nav>
                    <div class="sidebar-footer">
                        <div class="sidebar-user">
                            <div class="sidebar-avatar">%s</div>
                            <div class="sidebar-user-info">
                                <div class="sidebar-username">%s</div>
                                <div class="sidebar-role">%s</div>
                            </div>
                        </div>
                        <div class="sidebar-actions">
                            <a href="/admin/logout" class="sidebar-action sidebar-action-logout" aria-label="%s" title="%s"><i class="fas fa-right-from-bracket"></i></a>
                            <button type="button" class="sidebar-action" id="adminThemeToggle" data-action="toggleTheme" aria-label="%s" title="%s"><i class="fas fa-moon"></i></button>
                            %s
                        </div>
                    </div>
                </aside>
                <main class="admin-main">
                    <header class="admin-topbar">
                        <button type="button" class="menu-toggle" data-action="toggleSidebar" aria-label="%s"><i class="fas fa-bars"></i></button>
                        <span class="admin-topbar-title">%s</span>
                        %s
                        <button type="button" class="nav-link theme-toggle" data-action="toggleTheme" aria-label="%s" title="%s"><i class="fas fa-moon"></i></button>
                    </header>
                    <div class="admin-content">
                        %s
                    </div>
                    <script src="/js/admin-subnav.js"></script>
                </main>
            </div>
            """.formatted(
                "LingYggdrasil",
                nav.toString(),
                adminUsername != null && !adminUsername.isEmpty() ? safeInitial : "A",
                adminUsername != null ? safeUsername : "Admin",
                roleDisplay,
                I18n.t("adminSidebar.logout"),
                I18n.t("adminSidebar.logout"),
                I18n.t("adminSidebar.switchTheme"),
                I18n.t("adminSidebar.switchTheme"),
                PageRenderer.renderAdminLanguageSwitcher(true),
                I18n.t("nav.menu"),
                I18n.t("nav.admin"),
                PageRenderer.renderAdminLanguageSwitcher(false),
                I18n.t("nav.theme"),
                I18n.t("nav.theme"),
                content);

        return FooterInfo.injectFooterRecords(layout);
    }

    private static void appendIfPermitted(StringBuilder nav, String perm, String href, String page,
                                          String labelKey, String icon, String currentPage) {
        if (im.xz.cn.security.AdminPermissions.has(perm)) {
            nav.append(adminSidebarItem(href, page, labelKey, icon, currentPage));
        }
    }

    private static String adminSidebarItem(String href, String page, String labelKey, String icon, String currentPage) {
        return """
            <a href="%s" class="sidebar-link %s">
                <span class="sidebar-link-icon"><i class="fas %s"></i></span>
                <span>%s</span>
            </a>
            """.formatted(href, page.equals(currentPage) ? "active" : "", icon, I18n.t(labelKey));
    }

    public static String renderLoginPage() {
        String body = tr("""
            <div class="admin-login-container">
                <div class="admin-login-card">
                    <div class="login-header">
                        <img src="/icons/app.ico" class="logo-icon login-logo" alt="LingYggdrasil">
                        <h1>{{admin.login.title}}</h1>
                        <p class="login-subtitle">Administration Console</p>
                    </div>
                    <form id="loginForm" class="login-form">
                        <div class="form-group">
                            <label class="form-label">{{admin.login.username}}</label>
                            <input type="text" id="username" class="form-input" placeholder="{{admin.login.usernamePlaceholder}}" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.login.password}}</label>
                            <input type="password" id="password" class="form-input" placeholder="{{admin.login.passwordPlaceholder}}" required>
                        </div>
                        <div id="errorMsg" class="error-msg" style="display:none;"></div>
                        <button type="submit" class="btn btn-primary btn-login">{{admin.login.submit}}</button>
                    </form>
                </div>
            </div>
            <script src="/js/admin-login.js"></script>
            """);

        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.login.pageTitle"), body, "admin-login", css);
    }

    public static String dashboardContent() {
        return tr("""
            <div class="page-header">
                <h2>{{admin.dashboard.title}}</h2>
                <p class="page-desc">{{admin.dashboard.desc}}</p>
            </div>
            <div class="stats-grid" data-sortable="admin-stats">
                <div class="stat-card" id="stat-users">
                    <div class="stat-icon"><i class="fas fa-users"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="userCount">--</div>
                        <div class="stat-label">{{admin.dashboard.users}}</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-profiles">
                    <div class="stat-icon"><i class="fas fa-gamepad"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="profileCount">--</div>
                        <div class="stat-label">{{admin.dashboard.profiles}}</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-tokens">
                    <div class="stat-icon"><i class="fas fa-key"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="activeTokenCount">--</div>
                        <div class="stat-label">{{admin.dashboard.tokens}}</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-admins">
                    <div class="stat-icon"><i class="fas fa-user-shield"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="adminCount">--</div>
                        <div class="stat-label">{{admin.dashboard.admins}}</div>
                    </div>
                </div>
            </div>
            <script src="/js/admin-dashboard.js"></script>
            """);
    }

    public static String renderDashboardPage(String adminUsername, String adminRole, String csrfToken) {
        String body = renderAdminLayout("dashboard", adminUsername, adminRole, dashboardContent(), csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.dashboard.title"), body, "admin", css);
    }

    public static String renderSkinsPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">%s</h3></div>
                <div class="card-body" style="padding:0;">
            <table class="table admin-table">
                <thead>
                    <tr>
                        <th>Hash</th>
                        <th>%s</th>
                        <th>%s</th>
                        <th>%s</th>
                        <th>%s</th>
                        <th>%s</th>
                    </tr>
                </thead>
                <tbody id="skinTableBody">
                    <tr><td colspan="6" class="text-center">%s</td></tr>
                </tbody>
            </table>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.skins.title"),
                I18n.t("admin.skins.desc"),
                I18n.t("admin.skins.roster"),
                I18n.t("admin.skins.originalName"),
                I18n.t("admin.skins.size"),
                I18n.t("admin.skins.refUsers"),
                I18n.t("admin.skins.uploadedAt"),
                I18n.t("admin.common.operation"),
                I18n.t("admin.common.loading"))
            + skinSettingsCard() + skinNameBlacklistCard() + moreOpsCard("SKIN") + """
            <div id="toast" class="toast" style="display:none;"></div>
            <div id="aliasModal" class="modal-overlay" style="display:none;">
                <div class="modal-box">
                    <h3>%s</h3>
                    <div class="form-group">
                        <label class="form-label">%s</label>
                        <input type="text" class="form-input" id="newAlias" placeholder="%s">
                    </div>
                    <input type="hidden" id="editSkinId">
                    <div id="aliasMsg" class="msg-area"></div>
                    <div class="modal-actions">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["aliasModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitAlias">%s</button>
                    </div>
                </div>
            </div>
            <script src="/js/admin-system.js"></script>
            <script src="/js/skinview3d.bundle.js"></script>
            <script src="/js/admin-skins.js"></script>
            """.formatted(
                I18n.t("admin.skins.editAliasTitle"),
                I18n.t("admin.skins.newAlias"),
                I18n.t("admin.skins.newAliasPlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.save"));
        String body = renderAdminLayout("skins", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.skins.title"), body, "admin", css);
    }

    public static String renderCapesPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">%s</h3></div>
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>Hash</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                            </tr>
                        </thead>
                        <tbody id="capeTableBody">
                            <tr><td colspan="6" class="text-center">%s</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.capes.title"),
                I18n.t("admin.capes.desc"),
                I18n.t("admin.capes.roster"),
                I18n.t("admin.capes.originalName"),
                I18n.t("admin.capes.size"),
                I18n.t("admin.capes.refUsers"),
                I18n.t("admin.capes.uploadedAt"),
                I18n.t("admin.common.operation"),
                I18n.t("admin.common.loading"))
            + capeSettingsCard() + capeNameBlacklistCard() + moreOpsCard("CAPE") + """
            <div id="toast" class="toast" style="display:none;"></div>
            <div id="aliasModal" class="modal-overlay" style="display:none;">
                <div class="modal-box">
                    <h3>%s</h3>
                    <div class="form-group">
                        <label class="form-label">%s</label>
                        <input type="text" class="form-input" id="newAlias" placeholder="%s">
                    </div>
                    <input type="hidden" id="editSkinId">
                    <div id="aliasMsg" class="msg-area"></div>
                    <div class="modal-actions">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["aliasModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitAlias">%s</button>
                    </div>
                </div>
            </div>
            <script src="/js/admin-system.js"></script>
            <script src="/js/skinview3d.bundle.js"></script>
            <script src="/js/admin-capes.js"></script>
            """.formatted(
                I18n.t("admin.capes.editAliasTitle"),
                I18n.t("admin.capes.newAlias"),
                I18n.t("admin.capes.newAliasPlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.save"));
        String body = renderAdminLayout("capes", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.capes.title"), body, "admin", css);
    }

    public static String renderAppInfoPage(String adminUsername, String adminRole, String csrfToken) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.appinfo.title}}</h2>
                <p class="page-desc">{{admin.appinfo.desc}}</p>
            </div>
            <div class="appinfo-card card">
                <div class="card-header"><h3 class="card-title">{{admin.appinfo.systemInfo}}</h3></div>
                <div class="card-body">
                    <div class="info-grid">
                        <div class="info-item">
                            <div class="info-label">{{admin.appinfo.programName}}</div>
                            <div class="info-value appinfo-name"><img src="/builtin-icons/app.ico" class="logo-icon appinfo-icon" alt="LingYggdrasil"><span id="appName">{{admin.common.loading}}</span></div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">{{admin.appinfo.version}}</div>
                            <div class="info-value" id="appVersion">{{admin.common.loading}}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">{{admin.appinfo.repo}}</div>
                            <div class="info-value" id="appRepo">{{admin.common.loading}}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">{{admin.appinfo.installedAt}}</div>
                            <div class="info-value" id="installedAt">{{admin.common.loading}}</div>
                        </div>
                    </div>
                </div>
            </div>
            """)
            + startupParamsCard()
            + tr("""
            <div class="appinfo-card card" style="margin-top:20px;">
                <div class="card-header"><h3 class="card-title">{{admin.appinfo.starTitle}}</h3></div>
                <div class="card-body">
                    <p style="margin:0 0 16px;color:var(--color-text-muted);line-height:1.7;">{{admin.appinfo.starDesc}}</p>
                    <a class="btn btn-primary" href="https://github.com/xiaLingLuo/LingYggdrasil-CE" target="_blank" rel="noopener">
                        <i class="fab fa-github"></i> {{admin.appinfo.starBtn}}
                    </a>
                </div>
            </div>
            <script src="/js/admin-appinfo.js"></script>
            """);
        String body = renderAdminLayout("appinfo", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.appinfo.title"), body, "admin", css);
    }

    private static String startupParamsCard() {
        im.xz.cn.config.ServerConfig cfg = im.xz.cn.config.ServerConfig.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"appinfo-card card\" style=\"margin-top:20px;\">");
        sb.append("<div class=\"card-header\"><h3 class=\"card-title\">")
                .append(esc(I18n.t("admin.appinfo.startupParams"))).append("</h3></div>");
        sb.append("<div class=\"card-body\">");
        sb.append("<p class=\"startup-desc\">")
                .append(esc(I18n.t("admin.appinfo.startupParamsDesc"))).append("</p>");

        sb.append("<div class=\"startup-section\">");
        sb.append("<div class=\"startup-section-title\"><i class=\"fas fa-server\"></i>")
                .append(esc(I18n.t("admin.appinfo.systems"))).append("</div>");
        sb.append("<div class=\"startup-grid\">");
        sb.append(startupServiceCard("admin.appinfo.systemUser", cfg.isUserEnabled(), cfg.getUserIp(),
                cfg.getUserPort(), cfg.getUserLogRetentionDays()));
        sb.append(startupServiceCard("admin.appinfo.systemYggdrasil", cfg.isYggdrasilEnabled(), cfg.getYggdrasilIp(),
                cfg.getYggdrasilPort(), cfg.getYggdrasilLogRetentionDays()));
        sb.append(startupServiceCard("admin.appinfo.systemAdmin", cfg.isAdminEnabled(), cfg.getAdminIp(),
                cfg.getAdminPort(), cfg.getAdminLogRetentionDays()));
        sb.append("</div></div>");

        sb.append("<div class=\"startup-section\">");
        sb.append("<div class=\"startup-section-title\"><i class=\"fas fa-scroll\"></i>")
                .append(esc(I18n.t("admin.appinfo.logging"))).append("</div>");
        sb.append("<div class=\"startup-grid\">");
        sb.append("<div class=\"startup-service\">");
        sb.append("<div class=\"startup-service-head\"><span class=\"startup-service-name\">")
                .append(esc(I18n.t("admin.appinfo.logLevel"))).append("</span></div>");
        sb.append("<div class=\"startup-rows\"><span class=\"startup-value-lg\">")
                .append(esc(cfg.getLogLevel())).append("</span></div>");
        sb.append("</div>");
        sb.append(startupToggleCard("admin.appinfo.auditLog", cfg.isAuditLogEnabled(),
                cfg.getAuditLogRetentionDays()));
        sb.append(startupToggleCard("admin.appinfo.pluginSystemLog", cfg.isPluginSystemLogEnabled(),
                cfg.getPluginSystemLogRetentionDays()));
        sb.append("</div></div>");

        sb.append("</div></div>");
        return sb.toString();
    }

    private static String startupServiceCard(String nameKey, boolean enabled, String ip, int port, int retentionDays) {
        return "<div class=\"startup-service\">"
                + "<div class=\"startup-service-head\">"
                + "<span class=\"startup-service-name\">" + esc(I18n.t(nameKey)) + "</span>"
                + startupStatusBadge(enabled)
                + "</div>"
                + "<div class=\"startup-rows\">"
                + startupRow("admin.appinfo.ip", ip)
                + startupRow("admin.appinfo.port", String.valueOf(port))
                + startupRow("admin.appinfo.retention", String.valueOf(retentionDays))
                + "</div></div>";
    }

    private static String startupToggleCard(String nameKey, boolean enabled, int retentionDays) {
        return "<div class=\"startup-service\">"
                + "<div class=\"startup-service-head\">"
                + "<span class=\"startup-service-name\">" + esc(I18n.t(nameKey)) + "</span>"
                + startupStatusBadge(enabled)
                + "</div>"
                + "<div class=\"startup-rows\">"
                + startupRow("admin.appinfo.retention", String.valueOf(retentionDays))
                + "</div></div>";
    }

    private static String startupStatusBadge(boolean enabled) {
        return enabled
                ? "<span class=\"badge badge-success\"><i class=\"fas fa-circle-check\"></i>"
                        + esc(I18n.t("admin.appinfo.enabled")) + "</span>"
                : "<span class=\"badge badge-danger\"><i class=\"fas fa-circle-xmark\"></i>"
                        + esc(I18n.t("admin.appinfo.disabled")) + "</span>";
    }

    private static String startupRow(String keyKey, String value) {
        return "<div class=\"startup-row\"><span class=\"startup-key\">" + esc(I18n.t(keyKey))
                + "</span><span class=\"startup-val\">" + esc(value) + "</span></div>";
    }

    public static String renderSecurityPage(String adminUsername, String adminRole, String csrfToken) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.security.title}}</h2>
                <p class="page-desc">{{admin.security.desc}}</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.encryptionLevel}}</h3></div>
                <div class="card-body">
                    <p style="margin-bottom:16px;color:#666;font-size:14px;">{{admin.security.intro1}}</p>
                    <p style="margin-bottom:16px;color:#666;font-size:14px;">{{admin.security.intro2}}</p>
                    <div id="encryptionLevelList" class="encryption-level-list">
                    </div>
                    <input type="hidden" id="encryptionLevel" value="1">
                </div>
            </div>
            <div class="settings-actions">
                <button class="btn btn-primary" data-action="saveSettings">{{admin.security.save}}</button>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-security.js"></script>
            """);
        String body = renderAdminLayout("security", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.security.title"), body, "admin", css);
    }

    public static String renderUsersPage(String adminUsername, String adminRole, String csrfToken) {
        String createUserBtn = im.xz.cn.security.AdminPermissions.has("admin.users.create")
                ? "<button class=\"btn btn-primary\" style=\"white-space:nowrap;flex-shrink:0\" data-action=\"openCreateUserModal\">"
                        + I18n.t("admin.users.createBtn") + "</button>"
                : "";
        String content = """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            <div class="settings-card card">
                <div class="card-header" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title">%s</h3>
                    <div style="display:flex;align-items:center;gap:12px;">
                        <input type="text" id="searchInput" class="form-input search-input" placeholder="%s" data-action="filterUsers">
                        %s
                    </div>
                </div>
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>%s</th>
                                <th>UUID</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                            </tr>
                        </thead>
                        <tbody id="userTableBody">
                            <tr><td colspan="8" class="text-center">%s</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.users.title"),
                I18n.t("admin.users.desc"),
                I18n.t("admin.users.roster"),
                I18n.t("admin.users.searchPlaceholder"),
                createUserBtn,
                I18n.t("admin.users.username"),
                I18n.t("admin.users.email"),
                I18n.t("admin.users.nickname"),
                I18n.t("admin.admins.permGroup"),
                I18n.t("admin.users.emailVerified"),
                I18n.t("admin.users.registeredAt"),
                I18n.t("admin.common.operation"),
                I18n.t("admin.common.loading"))
            + usernameBlacklistCard() + userPermGroupsCard() + userLogsCard() + tr("""
            <div id="createUserModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["createUserModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["createUserModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="createUsername" class="form-input" placeholder="%s" maxlength="32">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="email" id="createUserEmail" class="form-input" placeholder="%s">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="createUserPassword" class="form-input" placeholder="%s">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="createUserNickname" class="form-input" placeholder="%s" maxlength="32">
                        </div>
                        <div class="form-group" style="display:flex;align-items:center;gap:8px;">
                            <input type="checkbox" id="createUserVerified" checked>
                            <label class="form-label" style="margin:0;">%s</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["createUserModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitCreateUser">%s</button>
                    </div>
                </div>
            </div>

            <div id="usernameModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["usernameModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["usernameModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editUserId">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="newUsername" class="form-input" placeholder="%s">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["usernameModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitUsername">%s</button>
                    </div>
                </div>
            </div>

            <div id="emailModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["emailModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["emailModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editEmailUserId">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="email" id="newEmail" class="form-input" placeholder="%s">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["emailModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitEmail">%s</button>
                    </div>
                </div>
            </div>

            <div id="nicknameModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["nicknameModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["nicknameModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editNicknameUserId">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="newNickname" class="form-input" placeholder="%s" maxlength="32">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["nicknameModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitNickname">%s</button>
                    </div>
                </div>
            </div>

            <div id="userGroupModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["userGroupModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.admins.createGroupTitle}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["userGroupModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.groupName}}</label>
                            <input type="text" id="userGroupNameInput" class="form-input" placeholder="{{admin.admins.groupNamePlaceholder}}">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["userGroupModal"]'>{{admin.common.cancel}}</button>
                        <button class="btn btn-primary" data-action="submitCreateUserGroup">{{admin.common.create}}</button>
                    </div>
                </div>
            </div>

            <div id="userSetGroupModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["userSetGroupModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.admins.permGroup}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["userSetGroupModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="setGroupUserId">
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.permGroup}}</label>
                            <select id="setGroupSelect" class="form-input"></select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["userSetGroupModal"]'>{{admin.common.cancel}}</button>
                        <button class="btn btn-primary" data-action="submitSetUserGroup">{{admin.common.confirmEdit}}</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-system.js"></script>
            <script src="/js/user-perm-groups.js"></script>
            <script src="/js/admin-users.js"></script>
            """.formatted(
                I18n.t("admin.users.createTitle"),
                I18n.t("admin.users.username"),
                I18n.t("admin.users.createUsernamePlaceholder"),
                I18n.t("admin.users.email"),
                I18n.t("admin.users.emailPlaceholder"),
                I18n.t("admin.users.password"),
                I18n.t("admin.users.passwordPlaceholder"),
                I18n.t("admin.users.nicknameOptional"),
                I18n.t("admin.users.nicknamePlaceholder"),
                I18n.t("admin.users.createVerified"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.create"),
                I18n.t("admin.users.editUsernameTitle"),
                I18n.t("admin.users.newUsername"),
                I18n.t("admin.users.newUsernamePlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.users.confirmEdit"),
                I18n.t("admin.users.editEmailTitle"),
                I18n.t("admin.users.newEmail"),
                I18n.t("admin.users.newEmailPlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.confirmEdit"),
                I18n.t("admin.users.editNicknameTitle"),
                I18n.t("admin.users.newNickname"),
                I18n.t("admin.users.newNicknamePlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.confirmEdit")));
        String body = renderAdminLayout("users", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.users.title"), body, "admin", css);
    }

    public static String renderAdminsPage(String adminUsername, String adminRole, String csrfToken) {
        String createBtn = im.xz.cn.security.AdminPermissions.has("admin.admins.create")
                ? "<button class=\"btn btn-primary\" data-action=\"openCreateModal\">" + I18n.t("admin.admins.createBtn") + "</button>"
                : "";
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.admins.title}}</h2>
                <p class="page-desc">{{admin.admins.desc}}</p>
            </div>
            <div class="settings-card card">
                <div class="card-header" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title">{{admin.admins.roster}}</h3>
                    %s
                </div>
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>{{admin.admins.username}}</th>
                                <th>{{admin.admins.email}}</th>
                                <th>{{admin.admins.permGroup}}</th>
                                <th>{{admin.admins.createdAt}}</th>
                                <th>{{admin.common.operation}}</th>
                            </tr>
                        </thead>
                        <tbody id="adminTableBody">
                            <tr><td colspan="5" class="text-center">{{admin.common.loading}}</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="settings-card card">
                <div class="card-header" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title">{{admin.admins.permGroups}}</h3>
                    <button class="btn btn-primary" style="white-space:nowrap;flex-shrink:0" data-action="openGroupCreateModal">{{admin.admins.createGroup}}</button>
                </div>
                <div class="card-body">
                    <div id="permGroupList"><p class="text-muted">{{admin.common.loading}}</p></div>
                </div>
            </div>

            <div id="createModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["createModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.admins.createTitle}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["createModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.username}}</label>
                            <input type="text" id="createUsername" class="form-input" placeholder="{{admin.admins.createUsernamePlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.email}}</label>
                            <input type="email" id="createEmail" class="form-input" placeholder="{{admin.admins.createEmailPlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.password}}</label>
                            <input type="password" id="createPassword" class="form-input" placeholder="{{admin.admins.createPasswordPlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.permGroup}}</label>
                            <select id="createPermGroup" class="form-input"></select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["createModal"]'>{{admin.common.cancel}}</button>
                        <button class="btn btn-primary" data-action="submitCreate">{{admin.common.create}}</button>
                    </div>
                </div>
            </div>

            <div id="editModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["editModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.admins.editTitle}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["editModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editAdminId">
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.username}}</label>
                            <input type="text" id="editUsername" class="form-input" placeholder="{{admin.admins.editUsernamePlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.email}}</label>
                            <input type="email" id="editEmail" class="form-input" placeholder="{{admin.admins.editEmailPlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.newPassword}}</label>
                            <input type="password" id="editPassword" class="form-input" placeholder="{{admin.admins.editPasswordPlaceholder}}">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.permGroup}}</label>
                            <select id="editPermGroup" class="form-input"></select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["editModal"]'>{{admin.common.cancel}}</button>
                        <button class="btn btn-primary" data-action="submitEdit">{{admin.admins.saveEdit}}</button>
                    </div>
                </div>
            </div>

            <div id="groupModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["groupModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.admins.createGroupTitle}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["groupModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">{{admin.admins.groupName}}</label>
                            <input type="text" id="groupNameInput" class="form-input" placeholder="{{admin.admins.groupNamePlaceholder}}">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["groupModal"]'>{{admin.common.cancel}}</button>
                        <button class="btn btn-primary" data-action="submitCreateGroup">{{admin.common.create}}</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-perm-groups.js"></script>
            <script src="/js/admin-admins.js"></script>
            """.formatted(createBtn));
        String body = renderAdminLayout("admins", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.admins.title"), body, "admin", css);
    }

    public static String renderAdminProfilesPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            <div class="settings-card card">
                <div class="card-header" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title">%s</h3>
                    <div style="display:flex;align-items:center;gap:12px;">
                        <input type="text" id="profileSearchInput" class="form-input search-input" placeholder="%s" data-action="filterProfiles">
                        <button class="btn btn-primary" style="white-space:nowrap;flex-shrink:0" data-action="openCreateModal">%s</button>
                    </div>
                </div>
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>%s</th>
                                <th>%s</th>
                                <th>UUID</th>
                                <th>%s</th>
                                <th>%s</th>
                                <th>%s</th>
                            </tr>
                        </thead>
                        <tbody id="profileTableBody">
                            <tr><td colspan="6" class="text-center">%s</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.profiles.title"),
                I18n.t("admin.profiles.desc"),
                I18n.t("admin.profiles.roster"),
                I18n.t("admin.profiles.searchPlaceholder"),
                I18n.t("admin.profiles.createBtn"),
                I18n.t("admin.profiles.name"),
                I18n.t("admin.profiles.owner"),
                I18n.t("admin.profiles.model"),
                I18n.t("admin.profiles.createdAt"),
                I18n.t("admin.common.operation"),
                I18n.t("admin.common.loading"))
            + profileSettingsCard() + profileNameBlacklistCard() + """
            <div id="createModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["createModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["createModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="createName" class="form-input" placeholder="%s" maxlength="24">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <div style="display:flex;gap:8px;align-items:center">
                                <input type="text" id="createUuid" class="form-input" placeholder="%s" style="flex:1">
                                <button type="button" class="btn btn-secondary" data-action="generateUuid">%s</button>
                            </div>
                        </div>
                        <p class="text-muted" style="font-size:12px;margin:0">%s</p>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["createModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitCreate">%s</button>
                    </div>
                </div>
            </div>

            <div id="updateModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["updateModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["updateModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editProfileId">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="newName" class="form-input" placeholder="%s" maxlength="24">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["updateModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitUpdate">%s</button>
                    </div>
                </div>
            </div>

            <div id="transferModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["transferModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>%s</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["transferModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="transferProfileId">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" id="transferUserId" class="form-input" placeholder="%s">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-action="closeModal" data-args='["transferModal"]'>%s</button>
                        <button class="btn btn-primary" data-action="submitTransfer">%s</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-system.js"></script>
            <script src="/js/admin-profiles.js"></script>
            """.formatted(
                I18n.t("admin.profiles.createTitle"),
                I18n.t("admin.profiles.nameLabel"),
                I18n.t("admin.profiles.namePlaceholder"),
                I18n.t("admin.profiles.uuidLabel"),
                I18n.t("admin.profiles.uuidPlaceholder"),
                I18n.t("admin.profiles.randomUuid"),
                I18n.t("admin.profiles.createHint"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.create"),
                I18n.t("admin.profiles.updateTitle"),
                I18n.t("admin.profiles.newName"),
                I18n.t("admin.profiles.newNamePlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.confirmEdit"),
                I18n.t("admin.profiles.transferTitle"),
                I18n.t("admin.profiles.transferLabel"),
                I18n.t("admin.profiles.transferPlaceholder"),
                I18n.t("admin.common.cancel"),
                I18n.t("admin.common.transfer"));
        String body = renderAdminLayout("profiles", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.profiles.title"), body, "admin", css);
    }

    public static String renderYggdrasilPage(String adminUsername, String adminRole, String csrfToken) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.yggdrasil.title}}</h2>
                <p class="page-desc">{{admin.yggdrasil.desc}}</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.yggdrasil.uuidSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.uuidVersion}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.uuidVersionDesc}}</div>
                        </div>
                        <select id="uuidVersion" class="form-input setting-select">
                            <option value="v3">v3 (MD5)</option>
                            <option value="v4">v4 (Random)</option>
                            <option value="v5">v5 (SHA-1)</option>
                        </select>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.yggdrasil.tokenSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.tokenTempExpiry}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.tokenTempExpiryDesc}}</div>
                        </div>
                        <input type="number" id="tokenTempExpiry" class="form-input setting-input" placeholder="4320">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.tokenPermExpiry}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.tokenPermExpiryDesc}}</div>
                        </div>
                        <input type="number" id="tokenPermanentExpiry" class="form-input setting-input" placeholder="10080">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.maxTokens}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.maxTokensDesc}}</div>
                        </div>
                        <input type="number" id="maxTokensPerProfile" class="form-input setting-input" placeholder="12">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.yggdrasil.authLimitSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.authRateLimit}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.authRateLimitDesc}}</div>
                        </div>
                        <input type="number" id="authRateLimit" class="form-input setting-input" placeholder="1000">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.batchQuery}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.batchQueryDesc}}</div>
                        </div>
                        <input type="number" id="batchQueryMaxCount" class="form-input setting-input" placeholder="6">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.yggdrasil.signSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.signMode}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.signModeDesc}}</div>
                        </div>
                        <select id="signatureMode" class="form-input setting-select" data-action="onModeChange">
                            <option value="ed448">{{admin.yggdrasil.signModeEd448}}</option>
                            <option value="rsa-sha512">{{admin.yggdrasil.signModeRsaSha512}}</option>
                            <option value="rsa-sha1">{{admin.yggdrasil.signModeRsaSha1}}</option>
                        </select>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.publicKey}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.publicKeyDesc}}</div>
                        </div>
                        <textarea id="yggdrasilPublicKey" class="form-input setting-textarea" rows="6" readonly placeholder="{{admin.yggdrasil.keyPlaceholder}}"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.privateKey}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.privateKeyDesc}}</div>
                        </div>
                        <textarea id="yggdrasilPrivateKey" class="form-input setting-textarea" rows="6" placeholder="{{admin.yggdrasil.keyPlaceholder}}"></textarea>
                    </div>
                    <div style="display:flex;gap:10px;margin-top:12px;">
                        <button class="btn btn-primary" data-action="regenerateKeys">{{admin.yggdrasil.regenerate}}</button>
                        <button class="btn btn-warning" id="switchModeBtn" data-action="confirmSwitchMode" style="display:none;">{{admin.yggdrasil.switchMode}}</button>
                    </div>
                </div>
            </div>
            <div class="settings-actions">
                <button class="btn btn-primary" data-action="saveSettings">{{admin.yggdrasil.save}}</button>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-yggdrasil.js"></script>
            """);
        String body = renderAdminLayout("yggdrasil", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.yggdrasil.title"), body, "admin", css);
    }

    private static String userPermGroupsCard() {
        return tr("""
            <div class="settings-card card">
                <div class="card-header" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title">{{admin.users.permGroups}}</h3>
                    <button class="btn btn-primary" style="white-space:nowrap;flex-shrink:0" data-action="openUserGroupCreateModal">{{admin.admins.createGroup}}</button>
                </div>
                <div class="card-body">
                    <div id="userPermGroupList"><p class="text-muted">{{admin.common.loading}}</p></div>
                </div>
            </div>
            """);
    }

    private static String userLogsCard() {
        return tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.users.logs}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logEnabled}}</div>
                            <div class="setting-desc">{{admin.users.logEnabledDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="userActionLogEnabled" data-setting-key="user_action_log_enabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logMaxKib}}</div>
                            <div class="setting-desc">{{admin.users.logMaxKibDesc}}</div>
                        </div>
                        <input type="number" id="userActionLogMaxKib" class="form-input setting-input" data-setting-key="user_action_log_max_kib" min="8" placeholder="100">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logRetentionDays}}</div>
                            <div class="setting-desc">{{admin.users.logRetentionDaysDesc}}</div>
                        </div>
                        <input type="number" id="userActionLogRetentionDays" class="form-input setting-input" data-setting-key="user_action_log_retention_days" min="1" placeholder="30">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logDownloadInterval}}</div>
                            <div class="setting-desc">{{admin.users.logDownloadIntervalDesc}}</div>
                        </div>
                        <input type="number" id="userActionLogDownloadIntervalMinutes" class="form-input setting-input" data-setting-key="user_action_log_download_interval_minutes" min="1" placeholder="480">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logClearInterval}}</div>
                            <div class="setting-desc">{{admin.users.logClearIntervalDesc}}</div>
                        </div>
                        <input type="number" id="userActionLogClearIntervalMinutes" class="form-input setting-input" data-setting-key="user_action_log_clear_interval_minutes" min="1" placeholder="360">
                    </div>
                    <div class="setting-item" style="display:block">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.users.logActions}}</div>
                            <div class="setting-desc">{{admin.users.logActionsDesc}}</div>
                        </div>
                        <div style="display:flex;flex-wrap:wrap;gap:12px;margin-top:8px">
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="login" class="log-action-check"> {{userlog.login}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="profile" class="log-action-check"> {{userlog.profile}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="texture" class="log-action-check"> {{userlog.texture}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="friend" class="log-action-check"> {{userlog.friend}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="password" class="log-action-check"> {{userlog.password}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="blacklist" class="log-action-check"> {{userlog.blacklist}}</label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer"><input type="checkbox" value="email" class="log-action-check"> {{userlog.email}}</label>
                        </div>
                        <input type="hidden" id="userActionLogActions" data-setting-key="user_action_log_actions">
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            """);
    }

    private static String usernameBlacklistCard() {
        return """
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">%s</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">%s</div>
                            <div class="setting-desc">%s</div>
                        </div>
                        <textarea id="usernameBlacklist" class="form-input setting-textarea" data-setting-key="username_blacklist" rows="5" placeholder="admin&#10;root&#10;system"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">%s</div>
                            <div class="setting-desc">%s</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="usernameBlacklistCaseSensitive" data-setting-key="username_blacklist_case_sensitive">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">%s</button>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.users.blacklistTitle"),
                I18n.t("admin.users.blacklistLabel"),
                I18n.t("admin.users.blacklistDesc"),
                I18n.t("admin.users.caseSensitive"),
                I18n.t("admin.users.caseSensitiveDesc"),
                I18n.t("admin.common.save"));
    }

    private static String skinSettingsCard() {
        return tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.skins.settings}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.maxSize}}</div>
                            <div class="setting-desc">{{admin.skins.maxSizeDesc}}</div>
                        </div>
                        <input type="number" id="skinMaxSize" class="form-input setting-input" data-setting-key="skin_max_size" placeholder="64">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.maxCount}}</div>
                            <div class="setting-desc">{{admin.skins.maxCountDesc}}</div>
                        </div>
                        <input type="number" id="skinMaxCount" class="form-input setting-input" data-setting-key="skin_max_count" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.maxTotalSize}}</div>
                            <div class="setting-desc">{{admin.skins.maxTotalSizeDesc}}</div>
                        </div>
                        <input type="number" id="skinMaxTotalSize" class="form-input setting-input" data-setting-key="skin_max_total_size" placeholder="640">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.rateLimit}}</div>
                            <div class="setting-desc">{{admin.skins.rateLimitDesc}}</div>
                        </div>
                        <input type="number" id="skinRateLimit" class="form-input setting-input" data-setting-key="skin_rate_limit" placeholder="24">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.storagePath}}</div>
                            <div class="setting-desc">{{admin.skins.storagePathDesc}}</div>
                        </div>
                        <input type="text" id="skinStoragePath" class="form-input setting-input" data-setting-key="skin_storage_path" placeholder="skins">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.allowDownload}}</div>
                            <div class="setting-desc">{{admin.skins.allowDownloadDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="allowDownloadSkin" data-setting-key="allow_download_skin">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.skins.maxFavorites}}</div>
                            <div class="setting-desc">{{admin.skins.maxFavoritesDesc}}</div>
                        </div>
                        <input type="number" class="form-input setting-input" id="maxFavorites" data-setting-key="max_favorites" value="32" min="-1" max="1000" style="width:120px">
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            """);
    }

    private static String capeSettingsCard() {
        return tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.capes.settings}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.maxSize}}</div>
                            <div class="setting-desc">{{admin.capes.maxSizeDesc}}</div>
                        </div>
                        <input type="number" id="capeMaxSize" class="form-input setting-input" data-setting-key="cape_max_size" placeholder="64">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.maxCount}}</div>
                            <div class="setting-desc">{{admin.capes.maxCountDesc}}</div>
                        </div>
                        <input type="number" id="capeMaxCount" class="form-input setting-input" data-setting-key="cape_max_count" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.maxTotalSize}}</div>
                            <div class="setting-desc">{{admin.capes.maxTotalSizeDesc}}</div>
                        </div>
                        <input type="number" id="capeMaxTotalSize" class="form-input setting-input" data-setting-key="cape_max_total_size" placeholder="640">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.rateLimit}}</div>
                            <div class="setting-desc">{{admin.capes.rateLimitDesc}}</div>
                        </div>
                        <input type="number" id="capeRateLimit" class="form-input setting-input" data-setting-key="cape_rate_limit" placeholder="24">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.storagePath}}</div>
                            <div class="setting-desc">{{admin.capes.storagePathDesc}}</div>
                        </div>
                        <input type="text" id="capeStoragePath" class="form-input setting-input" data-setting-key="cape_storage_path" placeholder="capes">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.capes.allowDownload}}</div>
                            <div class="setting-desc">{{admin.capes.allowDownloadDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="allowDownloadCape" data-setting-key="allow_download_cape">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            """);
    }

    private static String profileNameBlacklistCard() {
        return blacklistCard("profileNameBlacklist", "profile_name_blacklist", "profile_name_blacklist_case_sensitive",
                "admin.nameBlacklist.profileLabel", "admin.nameBlacklist.profileDesc");
    }

    private static String skinNameBlacklistCard() {
        return blacklistCard("skinNameBlacklist", "skin_name_blacklist", "skin_name_blacklist_case_sensitive",
                "admin.nameBlacklist.skinLabel", "admin.nameBlacklist.skinDesc");
    }

    private static String capeNameBlacklistCard() {
        return blacklistCard("capeNameBlacklist", "cape_name_blacklist", "cape_name_blacklist_case_sensitive",
                "admin.nameBlacklist.capeLabel", "admin.nameBlacklist.capeDesc");
    }

    private static String blacklistCard(String inputId, String key, String caseKey, String labelKey, String descKey) {
        return """
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">%s</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">%s</div>
                            <div class="setting-desc">%s</div>
                        </div>
                        <textarea id="%s" class="form-input setting-textarea" data-setting-key="%s" rows="4" placeholder="admin&#10;root"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">%s</div>
                            <div class="setting-desc">%s</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="%sCaseSensitive" data-setting-key="%s">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">%s</button>
                </div>
            </div>
            """.formatted(
                I18n.t("admin.nameBlacklist.title"),
                I18n.t(labelKey),
                I18n.t(descKey),
                inputId,
                key,
                I18n.t("admin.nameBlacklist.caseSensitive"),
                I18n.t("admin.nameBlacklist.caseSensitiveDesc"),
                inputId,
                caseKey,
                I18n.t("admin.common.save"));
    }

    private static String moreOpsCard(String type) {
        boolean skin = "SKIN".equals(type);
        String prefix = skin ? "admin.skins." : "admin.capes.";
        String fn = skin ? "deleteOrphanSkins" : "deleteOrphanCapes";
        return """
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">%s</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">%s</div>
                            <div class="setting-desc">%s</div>
                        </div>
                        <button class="btn btn-danger" data-action="%s">%s</button>
                    </div>
                </div>
            </div>
            """.formatted(
                I18n.t(prefix + "moreOps"),
                I18n.t(prefix + "deleteOrphan"),
                I18n.t(prefix + "deleteOrphanDesc"),
                fn,
                I18n.t(prefix + "deleteOrphan"));
    }

    private static String profileSettingsCard() {
        return tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.profiles.settings}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.profiles.maxProfilesPerUser}}</div>
                            <div class="setting-desc">{{admin.profiles.maxProfilesPerUserDesc}}</div>
                        </div>
                        <input type="number" id="maxProfilesPerUser" class="form-input setting-input" data-setting-key="max_profiles_per_user" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.profiles.maxAccountsPerIp}}</div>
                            <div class="setting-desc">{{admin.profiles.maxAccountsPerIpDesc}}</div>
                        </div>
                        <input type="number" id="maxAccountsPerIp" class="form-input setting-input" data-setting-key="max_accounts_per_ip" placeholder="3">
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            """);
    }

    public static String renderSystemPage(String adminUsername, String adminRole, String csrfToken) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.system.title}}</h2>
                <p class="page-desc">{{admin.system.desc}}</p>
            </div>
            """) + tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.basic}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.siteName}}</div>
                            <div class="setting-desc">{{admin.system.siteNameDesc}}</div>
                        </div>
                        <input type="text" id="siteName" class="form-input setting-input" data-setting-key="site_name" placeholder="{{admin.system.siteNamePlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.siteDesc}}</div>
                            <div class="setting-desc">{{admin.system.siteDescDesc}}</div>
                        </div>
                        <textarea id="siteDescription" class="form-input setting-textarea" data-setting-key="site_description" rows="3" placeholder="{{admin.system.siteDescPlaceholder}}"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.announcementMode}}</div>
                            <div class="setting-desc">{{admin.system.announcementModeDesc}}</div>
                        </div>
                        <select id="announcementMode" class="form-input setting-select" data-setting-key="announcement_mode">
                            <option value="off">{{admin.system.annOff}}</option>
                            <option value="toast">{{admin.system.annToast}}</option>
                            <option value="modal">{{admin.system.annModal}}</option>
                            <option value="top">{{admin.system.annTop}}</option>
                            <option value="top_force">{{admin.system.annTopForce}}</option>
                        </select>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.announcementScope}}</div>
                            <div class="setting-desc">{{admin.system.announcementScopeDesc}}</div>
                        </div>
                        <div style="display:flex;flex-wrap:wrap;gap:12px">
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer">
                                <input type="checkbox" value="user" class="announcement-scope-check"> {{admin.system.scopeUser}}
                            </label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer">
                                <input type="checkbox" value="admin" class="announcement-scope-check"> {{admin.system.scopeAdmin}}
                            </label>
                            <label style="display:flex;align-items:center;gap:6px;cursor:pointer">
                                <input type="checkbox" value="home" class="announcement-scope-check"> {{admin.system.scopeHome}}
                            </label>
                        </div>
                        <input type="hidden" id="announcementScope" data-setting-key="announcement_scope">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.announcementContent}}</div>
                            <div class="setting-desc">{{admin.system.announcementContentDesc}}</div>
                        </div>
                        <textarea id="announcementContent" class="form-input setting-textarea" data-setting-key="announcement_content" rows="4" placeholder="{{admin.system.announcementContentPlaceholder}}" maxlength="10000"></textarea>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.features}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.registration}}</div>
                            <div class="setting-desc">{{admin.system.registrationDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="registrationEnabled" data-setting-key="registration_enabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.domains}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.userDomain}}</div>
                            <div class="setting-desc">{{admin.system.userDomainDesc}}</div>
                        </div>
                        <input type="text" id="userDomain" class="form-input setting-input" data-setting-key="user_domain" placeholder="{{admin.system.domainPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.adminDomain}}</div>
                            <div class="setting-desc">{{admin.system.adminDomainDesc}}</div>
                        </div>
                        <input type="text" id="adminDomain" class="form-input setting-input" data-setting-key="admin_domain" placeholder="{{admin.system.adminDomainPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.apiDomain}}</div>
                            <div class="setting-desc">{{admin.system.apiDomainDesc}}</div>
                        </div>
                        <input type="text" id="apiDomain" class="form-input setting-input" data-setting-key="api_domain" placeholder="{{admin.system.apiDomainPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.commonDomain}}</div>
                            <div class="setting-desc">{{admin.system.commonDomainDesc}}</div>
                        </div>
                        <input type="text" id="commonDomain" class="form-input setting-input" data-setting-key="common_domain" placeholder="{{admin.system.domainPlaceholder}}">
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div class="settings-card card" data-section="mail">
                <div class="card-header"><h3 class="card-title">{{admin.system.mailSection}}</h3></div>
                <div class="card-body">
                    <div class="mail-subsection" data-save-scope>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.emailVerification}}</div>
                            <div class="setting-desc">{{admin.system.emailVerificationDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="emailVerificationEnabled" data-setting-key="email_verification_enabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailEnabled}}</div>
                            <div class="setting-desc">{{admin.system.mailEnabledDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="mailEnabled" data-setting-key="mail_enabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailHost}}</div>
                            <div class="setting-desc">{{admin.system.mailHostDesc}}</div>
                        </div>
                        <input type="text" id="mailHost" class="form-input setting-input" data-setting-key="mail_host" placeholder="{{admin.system.mailHostPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailPort}}</div>
                            <div class="setting-desc">{{admin.system.mailPortDesc}}</div>
                        </div>
                        <input type="number" id="mailPort" class="form-input setting-input" data-setting-key="mail_port" placeholder="465">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailUsername}}</div>
                            <div class="setting-desc">{{admin.system.mailUsernameDesc}}</div>
                        </div>
                        <input type="text" id="mailUsername" class="form-input setting-input" data-setting-key="mail_username" placeholder="{{admin.system.mailUsernamePlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailPassword}}</div>
                            <div class="setting-desc">{{admin.system.mailPasswordDesc}}</div>
                        </div>
                        <input type="password" id="mailPassword" class="form-input setting-input" data-setting-key="mail_password" placeholder="{{admin.system.mailPasswordPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailFrom}}</div>
                            <div class="setting-desc">{{admin.system.mailFromDesc}}</div>
                        </div>
                        <input type="email" id="mailFrom" class="form-input setting-input" data-setting-key="mail_from" placeholder="{{admin.system.mailFromPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailTls}}</div>
                            <div class="setting-desc">{{admin.system.mailTlsDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="mailTls" data-setting-key="mail_tls">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                        <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                    </div>
                    <div class="mail-subsection" data-save-scope>
                    <div class="setting-item" style="display:block">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailTestTarget}}</div>
                            <div class="setting-desc">{{admin.system.mailTestTargetDesc}}</div>
                        </div>
                        <div style="display:flex;gap:8px;margin-top:8px;flex-wrap:wrap;align-items:center;">
                            <input type="email" id="mailTestTarget" class="form-input setting-input" placeholder="{{admin.system.mailTestTargetPlaceholder}}" style="flex:1;min-width:220px;">
                            <button class="btn btn-primary" data-action="sendTestMail" style="white-space:nowrap;">{{admin.system.mailTestSend}}</button>
                        </div>
                        <div style="margin-top:10px;">
                            <div class="setting-desc" style="margin-bottom:4px;">{{admin.system.mailTestContent}}</div>
                            <textarea id="mailTestContent" class="form-input setting-textarea" data-setting-key="mail_test_content" rows="4" style="max-width:100%;width:100%;font-family:Consolas,monospace;font-size:12px" placeholder="Test Email"></textarea>
                        </div>
                    </div>
                        <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                    </div>
                    <div class="mail-subsection" data-save-scope>
                    <div class="setting-item" style="display:block">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailTemplateVerify}}</div>
                            <div class="setting-desc">{{admin.system.mailTemplateDesc}}</div>
                        </div>
                        <textarea id="mailTemplateVerify" class="form-input setting-textarea" data-setting-key="mail_template_verify" rows="8" style="max-width:100%;width:100%;margin-top:8px;font-family:Consolas,monospace;font-size:12px"></textarea>
                        <div style="margin-top:6px;color:#DC2626;font-size:12px;font-weight:600;"><i class="fas fa-triangle-exclamation"></i> {{admin.common.htmlWarning}}</div>
                    </div>
                        <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                    </div>
                    <div class="mail-subsection" data-save-scope>
                    <div class="setting-item" style="display:block">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailTemplateEmailChange}}</div>
                            <div class="setting-desc">{{admin.system.mailTemplateDesc}}</div>
                        </div>
                        <textarea id="mailTemplateEmailChange" class="form-input setting-textarea" data-setting-key="mail_template_email_change" rows="8" style="max-width:100%;width:100%;margin-top:8px;font-family:Consolas,monospace;font-size:12px"></textarea>
                        <div style="margin-top:6px;color:#DC2626;font-size:12px;font-weight:600;"><i class="fas fa-triangle-exclamation"></i> {{admin.common.htmlWarning}}</div>
                    </div>
                        <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                    </div>
                    <div class="mail-subsection" data-save-scope>
                    <div class="setting-item" style="display:block">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.mailTemplatePasswordChange}}</div>
                            <div class="setting-desc">{{admin.system.mailTemplateDesc}}</div>
                        </div>
                        <textarea id="mailTemplatePasswordChange" class="form-input setting-textarea" data-setting-key="mail_template_password_change" rows="8" style="max-width:100%;width:100%;margin-top:8px;font-family:Consolas,monospace;font-size:12px"></textarea>
                        <div style="margin-top:6px;color:#DC2626;font-size:12px;font-weight:600;"><i class="fas fa-triangle-exclamation"></i> {{admin.common.htmlWarning}}</div>
                    </div>
                        <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.emailDomainSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.emailDomainList}}</div>
                            <div class="setting-desc">{{admin.system.emailDomainListDesc}}</div>
                        </div>
                        <textarea id="emailDomainList" class="form-input setting-textarea" data-setting-key="email_domain_list" rows="5" placeholder="example.com&#10;test.com"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.emailDomainMode}}</div>
                            <div class="setting-desc">{{admin.system.emailDomainModeDesc}}</div>
                        </div>
                        <select id="emailDomainMode" class="form-input setting-select" data-setting-key="email_domain_mode">
                            <option value="blacklist">{{admin.system.emailModeBlacklist}}</option>
                            <option value="whitelist">{{admin.system.emailModeWhitelist}}</option>
                        </select>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.icpSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.icpRecord}}</div>
                            <div class="setting-desc">{{admin.system.icpRecordDesc}}</div>
                        </div>
                        <input type="text" id="icpRecord" class="form-input setting-input" data-setting-key="icp_record" placeholder="{{admin.system.icpRecordPlaceholder}}">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.publicSecurityRecord}}</div>
                            <div class="setting-desc">{{admin.system.publicSecurityRecordDesc}}</div>
                        </div>
                        <input type="text" id="publicSecurityRecord" class="form-input setting-input" data-setting-key="public_security_record" placeholder="{{admin.system.publicSecurityRecordPlaceholder}}">
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.system.more}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.treasure}}</div>
                            <div class="setting-desc">{{admin.system.treasureDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="treasureEnabled" data-setting-key="treasure_enabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.system.clearCache}}</div>
                            <div class="setting-desc">{{admin.system.clearCacheDesc}}</div>
                        </div>
                        <button class="btn btn-danger" data-action="clearCache">{{admin.system.clearCache}}</button>
                    </div>
                    <button class="btn btn-primary" data-action="saveSection" data-this style="margin-top:16px">{{admin.common.save}}</button>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-system.js"></script>
            """);
        String body = renderAdminLayout("system", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.system.title"), body, "admin", css);
    }

    private static String pluginSubnav(String currentPage) {
        java.util.List<im.xz.cn.plugin.PluginMenuEntry> entries =
                im.xz.cn.plugin.PluginManager.getInstance().menuEntries();
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"subnav\">");
        boolean overviewActive = "plugins".equals(currentPage);
        sb.append("<a href=\"/admin/plugins\" class=\"subnav-item")
                .append(overviewActive ? " active" : "")
                .append("\">").append(esc(I18n.t("admin.plugins.overview"))).append("</a>");
        for (im.xz.cn.plugin.PluginMenuEntry entry : entries) {
            String pageId = "plugin:" + entry.pluginName() + ":" + entry.menuId();
            String href = "/admin/plugins/" + urlEncode(entry.pluginName()) + "/" + urlEncode(entry.menuId());
            sb.append("<a href=\"").append(href).append("\" class=\"subnav-item")
                    .append(pageId.equals(currentPage) ? " active" : "")
                    .append("\">").append(esc(entry.title())).append("</a>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    public static String renderPluginsPage(String adminUsername, String adminRole, String csrfToken) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.plugins.title}}</h2>
                <p class="page-desc">{{admin.plugins.desc}}</p>
            </div>
            """)
            + pluginSubnav("plugins")
            + tr("""
            <div id="pluginList" class="plugin-list"></div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-plugins.js"></script>
            """);
        String body = renderAdminLayout("plugins", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.plugins.title"), body, "admin", css);
    }

    public static String renderPluginMenuPage(String pluginName, String menuId, String title,
                                              String fragment, String adminUsername, String adminRole,
                                              String csrfToken) {
        String currentPage = "plugin:" + pluginName + ":" + menuId;
        String content = pluginSubnav(currentPage) + (fragment == null ? "" : fragment);
        String body = renderAdminLayout(currentPage, adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        String pageTitle = (title == null || title.isBlank()) ? pluginName : title;
        return PageRenderer.renderAdminPage(pageTitle, body, "admin", css);
    }
}
