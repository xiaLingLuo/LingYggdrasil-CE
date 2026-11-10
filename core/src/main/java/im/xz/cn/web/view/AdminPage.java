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
import im.xz.cn.config.ServerConfig;
import im.xz.cn.config.SystemConfig;
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
        appendIfPermitted(nav, "admin.system.view", "/admin/system", "system", "adminSidebar.system", "fa-sliders", currentPage);
        appendIfPermitted(nav, "admin.yggdrasil.view", "/admin/yggdrasil", "yggdrasil", "adminSidebar.yggdrasil", "fa-tree", currentPage);
        appendIfPermitted(nav, "admin.users.view", "/admin/users", "users", "adminSidebar.users", "fa-users", currentPage);
        appendIfPermitted(nav, "admin.profiles.view", "/admin/profiles", "profiles", "adminSidebar.profiles", "fa-gamepad", currentPage);
        if (im.xz.cn.security.AdminPermissions.has("admin.skins.view")
                || im.xz.cn.security.AdminPermissions.has("admin.capes.view")) {
            nav.append(adminSidebarItem("/admin/skins", "skins",
                    "adminSidebar.skins", "fa-images", currentPage));
        }
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
                        <img src="/icons/app.ico" class="logo-icon sidebar-logo" alt="%s">
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
                    <script src="/js/admin-ui.js"></script>
                    <script src="/js/admin-save-cards.js"></script>
                </main>
            </div>
            """.formatted(
                esc(im.xz.cn.config.SystemConfig.getInstance().getSiteName()),
                esc(im.xz.cn.config.SystemConfig.getInstance().getSiteName()),
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
                        <img src="/icons/app.ico" class="logo-icon login-logo" alt="">
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

    
    public static String renderTextureAssetsPage(String adminUsername, String adminRole, String csrfToken) {
        return renderTextureRosterPage(adminUsername, adminRole, csrfToken, "SKIN");
    }

    public static String renderTextureRosterPage(String adminUsername, String adminRole, String csrfToken, String type) {
        boolean cape = "CAPE".equalsIgnoreCase(type);
        String pageTitle = cape ? "披风名册" : "皮肤名册";
        String title = pageTitle;
        String pageDesc = cape ? "管理披风资产。" : "管理皮肤资产。";
        String typeHeader = cape ? "" : "<th>皮肤类型</th>";
        String content = """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            %s
            <div class="settings-card card" id="textureRosterRoot" data-type="%s">
                <div class="card-body">
                    <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;margin-bottom:12px">
                        <input id="textureRosterSearch" class="form-input" style="max-width:300px" placeholder="搜索名称/作者/哈希">
                        <button class="btn btn-primary" data-action="textureRosterSearch">查询</button>
                    </div>
                    <div style="overflow:auto">
                        <table class="table" style="width:100%%">
                            <thead><tr><th>HASH</th><th>名称</th><th>作者</th>%s<th>大小</th><th>引用</th><th>创建时间</th><th>操作</th></tr></thead>
                            <tbody id="textureRosterBody"></tbody>
                        </table>
                    </div>
                    <div id="textureRosterPagination" class="admin-table-pagination"></div>
                </div>
            </div>
            <script src="/js/skinview3d.bundle.js"></script>
            <script src="/js/admin-texture-roster.js"></script>
            """.formatted(title, pageDesc, textureSubnav(type), cape ? "CAPE" : "SKIN", typeHeader);
        String body = renderAdminLayout("skins", adminUsername, adminRole, content, csrfToken);
        return PageRenderer.renderAdminPage(pageTitle, body, "admin", Css.getAdminCssLink());
    }

    private static String textureSubnav(String type) {
        StringBuilder nav = new StringBuilder("<div class=\"subnav\" style=\"margin-bottom:16px\">");
        if (im.xz.cn.security.AdminPermissions.has("admin.skins.view")) {
            nav.append("<a class=\"subnav-item").append("SKIN".equalsIgnoreCase(type) ? " active" : "")
                    .append("\" href=\"/admin/skins\">皮肤名册</a>");
        }
        if (im.xz.cn.security.AdminPermissions.has("admin.capes.view")) {
            nav.append("<a class=\"subnav-item").append("CAPE".equalsIgnoreCase(type) ? " active" : "")
                    .append("\" href=\"/admin/capes\">披风名册</a>");
        }
        if (im.xz.cn.security.AdminPermissions.has("admin.system.view")) {
            nav.append("<a class=\"subnav-item").append("SETTINGS".equalsIgnoreCase(type) ? " active" : "")
                    .append("\" href=\"/admin/texture-settings\">纹理设置</a>");
            nav.append("<a class=\"subnav-item").append("BLACKLIST".equalsIgnoreCase(type) ? " active" : "")
                    .append("\" href=\"/admin/texture-blacklist\">纹理黑名单</a>");
        }
        nav.append("</div>");
        return nav.toString();
    }
    
    public static String renderSystemPage(String adminUsername, String adminRole, String csrfToken) {
        return renderSystemPage(adminUsername, adminRole, csrfToken, null, "", null, null,
                "系统管理", "修改系统配置并保存。");
    }

    
    public static String renderTextureSettingsPage(String adminUsername, String adminRole, String csrfToken) {
        return renderSystemPage(adminUsername, adminRole, csrfToken, "texture", textureSubnav("SETTINGS"),
                null, "texture_hash_blacklist", "纹理设置",
                "皮肤与披风共用以下设置。");
    }

    
    public static String renderTextureBlacklistPage(String adminUsername, String adminRole, String csrfToken) {
        return renderSystemPage(adminUsername, adminRole, csrfToken, "texture", textureSubnav("BLACKLIST"),
                "texture_hash_blacklist", null, "纹理黑名单",
                "每行一条 HASH，上传命中时拒绝。");
    }

    private static String renderSystemPage(String adminUsername, String adminRole, String csrfToken,
                                           String onlyGroup, String prefix, String onlyField, String hiddenField,
                                           String headerTitle, String headerDescription) {
        String content = """
            <div class="page-header">
                <h2>系统管理</h2>
                <p class="page-desc">修改系统配置并保存。</p>
            </div>
            <div id="settingsApp">
                <div id="settingsNav" class="subnav" aria-label="系统设置分区"></div>
                <div id="settingsPanels">
                    <p class="empty-hint">正在加载设置...</p>
                </div>
            </div>
            <script>
            (function () {
                var state = { schema: null, values: {}, revisions: {}, initial: {}, active: null };
                var root = document.getElementById('settingsPanels');
                var nav = document.getElementById('settingsNav');

                function esc(value) {
                    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
                    });
                }
                function headers() {
                    return { 'Content-Type': 'application/json', 'X-CSRF-Token': window.CSRF_TOKEN || '' };
                }
                function request(url, options) {
                    return fetch(url, options || { headers: headers() }).then(function (res) {
                        return res.json().then(function (body) {
                            if (!res.ok || body.success === false) {
                                var error = new Error(body.message || ('HTTP ' + res.status));
                                error.status = res.status;
                                error.body = body;
                                throw error;
                            }
                            return body;
                        });
                    });
                }
                function setToast(text, ok) {
                    if (!text) return;
                    if (typeof showToast === 'function') {
                        showToast(text, ok ? 'success' : 'error');
                    }
                }
                function valueOf(field) {
                    var el = document.querySelector('[data-setting-key="' + field.key + '"]');
                    if (!el) return '';
                    if (field.type === 'boolean') return el.checked ? 'true' : 'false';
                    if (field.type === 'multiselect') {
                        return Array.prototype.slice.call(el.querySelectorAll('input:checked')).map(function (cb) {
                            return cb.value;
                        }).join('|');
                    }
                    return el.value == null ? '' : String(el.value);
                }
                function setValue(field, value) {
                    var el = document.querySelector('[data-setting-key="' + field.key + '"]');
                    if (!el) return;
                    value = value == null ? '' : String(value);
                    if (field.type === 'boolean') {
                        el.checked = (value === 'true' || value === true);
                    } else if (field.type === 'multiselect') {
                        el.querySelectorAll('input').forEach(function (cb) {
                            cb.checked = value.split('|').indexOf(cb.value) >= 0;
                        });
                    } else if (field.secret) {
                        el.value = '';
                    } else {
                        el.value = value;
                    }
                }
                function snapshot(group) {
                    var result = {};
                    state.schema.groups.filter(function (g) { return g.name === group; })[0].fields.forEach(function (field) {
                        result[field.key] = valueOf(field);
                    });
                    return result;
                }
                function isDirty(group) {
                    var current = snapshot(group);
                    var initial = state.initial[group] || {};
                    return Object.keys(current).some(function (key) { return current[key] !== initial[key]; });
                }
                function updateStatus(group) {
                    var panel = document.querySelector('[data-settings-panel="' + group + '"]');
                    if (!panel) return;
                    var status = panel.querySelector('[data-settings-status]');
                    var save = panel.querySelector('[data-settings-save]');
                    var reset = panel.querySelector('[data-settings-reset]');
                    var dirty = isDirty(group);
                    if (save) save.disabled = !dirty;
                    if (reset) reset.disabled = !dirty;
                    var tab = nav.querySelector('[data-settings-tab="' + group + '"]');
                    if (tab) tab.classList.toggle('has-changes', dirty);
                    if (status) {
                        status.textContent = dirty ? '有未保存更改' : '未修改';
                        status.className = 'settings-status settings-status-' + (dirty ? 'dirty' : 'clean');
                    }
                }
                function fieldHtml(field) {
                    var input;
                    if (field.type === 'boolean') {
                        input = '<label class="toggle-switch"><input type="checkbox" data-setting-key="' + esc(field.key) + '"><span class="toggle-slider"></span></label>';
                    } else if (field.type === 'textarea') {
                        input = '<textarea class="form-input setting-textarea setting-textarea-wide" rows="5" data-setting-key="' + esc(field.key) + '" placeholder="' + esc(field.placeholder) + '"></textarea>';
                    } else if (field.type === 'select') {
                        input = '<select class="form-input setting-select" data-setting-key="' + esc(field.key) + '">';
                        (field.options || []).forEach(function (option) {
                            input += '<option value="' + esc(option.value) + '">' + esc(option.label) + '</option>';
                        });
                        input += '</select>';
                    } else if (field.type === 'multiselect') {
                        input = '<div class="settings-inline-checks" data-setting-key="' + esc(field.key) + '">';
                        (field.options || []).forEach(function (option) {
                            input += '<label><input type="checkbox" value="' + esc(option.value) + '"> ' + esc(option.label) + '</label>';
                        });
                        input += '</div>';
                    } else {
                        input = '<input type="' + esc(field.type === 'password' ? 'password' : field.type) + '" class="form-input setting-input" data-setting-key="' + esc(field.key) + '" placeholder="' + esc(field.secret && field.description ? '已设置时留空不变' : field.placeholder) + '"';
                        if (field.min != null) input += ' min="' + esc(field.min) + '"';
                        if (field.max != null) input += ' max="' + esc(field.max) + '"';
                        input += '>';
                    }
                    return '<div class="setting-item ' + (field.type === 'textarea' ? 'setting-item-stack' : '') + '">'
                        + '<div class="setting-info"><div class="setting-label">' + esc(field.label) + (field.description ? ' <button type="button" class="ui-help" tabindex="0" aria-label="字段说明" data-tooltip="' + esc(field.description) + '">?</button>' : '') + '</div>'
                        + '</div>' + input + '</div>';
                }
                function renderPanel(group) {
                    var fields = group.fields.filter(function (field) { return !field.advanced; });
                    var advanced = group.fields.filter(function (field) { return field.advanced; });
                    var html = '<section class="settings-panel active" data-settings-panel="' + esc(group.name) + '">'
                        + '<div class="settings-panel-head"><h3>' + esc(group.label) + '</h3>'
                        + (group.description ? '<p>' + esc(group.description) + '</p>' : '') + '</div>'
                        + '<div class="settings-card card"><div class="card-body">'
                        + fields.map(fieldHtml).join('')
                        + '</div></div>';
                    if (advanced.length) {
                        html += '<details class="settings-card card"><summary class="settings-details-summary">高级选项</summary><div class="card-body">'
                            + advanced.map(fieldHtml).join('') + '</div></details>';
                    }
                    if ((group.actions || []).indexOf('test-mail') >= 0) {
                        html += '<div class="settings-card card"><div class="card-body"><div class="setting-item setting-item-stack">'
                            + '<div class="setting-info"><div class="setting-label">发送测试邮件</div><div class="setting-desc">用当前已保存的 SMTP 配置发送测试邮件。</div></div>'
                            + '<div class="settings-inline-actions"><input class="form-input" id="settingsTestMailTo" placeholder="目标邮箱">'
                            + '<button type="button" class="btn btn-secondary" data-settings-test-mail>发送测试</button></div></div></div></div>';
                    }
                    html += '<div class="settings-toolbar"><span class="settings-status" data-settings-status>未修改</span>'
                        + '<button type="button" class="btn btn-secondary" data-settings-reset>放弃更改</button>'
                        + '<button type="button" class="btn btn-primary" data-settings-save disabled>保存更改</button></div>'
                        + '</section>';
                    return html;
                }
                function bindPanel(group) {
                    var panel = document.querySelector('[data-settings-panel="' + group.name + '"]');
                    panel.querySelectorAll('input,textarea,select').forEach(function (el) {
                        el.addEventListener('input', function () { updateStatus(group.name); });
                        el.addEventListener('change', function () { updateStatus(group.name); });
                    });
                    panel.querySelector('[data-settings-save]').addEventListener('click', function () { save(group.name); });
                    panel.querySelector('[data-settings-reset]').addEventListener('click', function () { reset(group.name); });
                    var test = panel.querySelector('[data-settings-test-mail]');
                    if (test) test.addEventListener('click', function () { testMail(panel); });
                }
                function reset(group) {
                    var initial = state.initial[group] || {};
                    state.schema.groups.filter(function (g) { return g.name === group; })[0].fields.forEach(function (field) {
                        setValue(field, initial[field.key]);
                    });
                    updateStatus(group);
                }
                function save(group) {
                    var panel = document.querySelector('[data-settings-panel="' + group + '"]');
                    var body = { revision: state.revisions[group] || 0, values: {} };
                    var changed = 0;
                    state.schema.groups.filter(function (g) { return g.name === group; })[0].fields.forEach(function (field) {
                        var value = valueOf(field);
                        if (field.secret && value === '') return;
                        if (value !== (state.initial[group] || {})[field.key]) {
                            body.values[field.key] = value;
                            changed++;
                        }
                    });
                    if (changed === 0) { updateStatus(group); return; }
                    panel.querySelector('[data-settings-save]').disabled = true;
                    var status = panel.querySelector('[data-settings-status]');
                    status.textContent = '正在保存...';
                    function attempt(retryOnConflict) {
                        body.revision = state.revisions[group] || 0;
                        request('/admin/api/settings/' + encodeURIComponent(group), {
                            method: 'PUT', headers: headers(), body: JSON.stringify(body)
                        }).then(function (result) {
                            state.revisions[group] = result.group.revision;
                            Object.assign(state.values[group], result.group.values);
                            state.schema.groups.filter(function (g) { return g.name === group; })[0].fields.forEach(function (field) {
                                if (field.secret) setValue(field, '');
                            });
                            state.initial[group] = snapshot(group);
                            setToast('设置已保存', true);
                            updateStatus(group);
                            status.textContent = '已保存';
                            status.className = 'settings-status settings-status-saved';
                        }).catch(function (error) {
                            if (error.status === 409 && retryOnConflict) {
                                var actual = error.body && error.body.revision;
                                if (actual != null) {
                                    state.revisions[group] = actual;
                                    attempt(false);
                                    return;
                                }
                            }
                            function showFailure() {
                                if (error.status === 409) {
                                    setToast('配置已被其他管理员修改，请刷新页面后重试', false);
                                } else {
                                    setToast(error.message || '保存失败', false);
                                }
                                var saveButton = panel.querySelector('[data-settings-save]');
                                if (saveButton) saveButton.disabled = !isDirty(group);
                                status.textContent = error.message || '保存失败';
                                status.className = 'settings-status settings-status-failed';
                            }
                            request('/admin/api/settings/' + encodeURIComponent(group)).then(function (latest) {
                                var latestValues = latest.group.values || {};
                                var persisted = Object.keys(body.values).every(function (key) {
                                    return String(latestValues[key]) === String(body.values[key]);
                                });
                                if (!persisted) { showFailure(); return; }
                                state.revisions[group] = latest.group.revision;
                                Object.assign(state.values[group], latestValues);
                                state.schema.groups.filter(function (g) { return g.name === group; })[0].fields.forEach(function (field) {
                                    if (field.secret) setValue(field, '');
                                });
                                state.initial[group] = snapshot(group);
                                var runtimeWarning = error.message && error.message !== 'HTTP ' + error.status;
                                setToast(runtimeWarning
                                        ? '设置已保存，但运行时应用失败：' + error.message
                                        : '设置已保存', !runtimeWarning);
                                updateStatus(group);
                                status.textContent = runtimeWarning ? '已保存（运行时未应用）' : '已保存';
                                status.className = 'settings-status settings-status-' + (runtimeWarning ? 'failed' : 'saved');
                            }).catch(showFailure);
                        });
                    }
                    attempt(true);
                }
                function testMail(panel) {
                    var input = panel.querySelector('#settingsTestMailTo');
                    var to = input ? input.value.trim() : '';
                    if (!to) { setToast('请输入测试收件邮箱', false); return; }
                    request('/admin/api/settings/mail/test', {
                        method: 'POST', headers: headers(), body: JSON.stringify({ to: to })
                    }).then(function () { setToast('测试邮件已发送', true); })
                      .catch(function (error) { setToast(error.message || '发送失败', false); });
                }
                function activate(name) {
                    state.active = name;
                    nav.querySelectorAll('[data-settings-tab]').forEach(function (btn) {
                        btn.classList.toggle('active', btn.getAttribute('data-settings-tab') === name);
                    });
                    root.querySelectorAll('[data-settings-panel]').forEach(function (panel) {
                        panel.classList.toggle('active', panel.getAttribute('data-settings-panel') === name);
                    });
                    if (history.replaceState) history.replaceState(null, '', '#' + name);
                }
                function render() {
                    nav.innerHTML = '';
                    root.innerHTML = '';
                    state.schema.groups.forEach(function (group, index) {
                        var btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'subnav-item';
                        btn.setAttribute('data-settings-tab', group.name);
                        btn.innerHTML = '<span>' + esc(group.label) + '</span>';
                        btn.addEventListener('click', function () { activate(group.name); });
                        nav.appendChild(btn);
                        root.insertAdjacentHTML('beforeend', renderPanel(group));
                        bindPanel(group);
                    });
                    state.schema.groups.forEach(function (group) {
                        var panel = document.querySelector('[data-settings-panel="' + group.name + '"]');
                        state.initial[group.name] = snapshot(group.name);
                        group.fields.forEach(function (field) {
                            setValue(field, state.values[group.name][field.key]);
                        });
                        state.initial[group.name] = snapshot(group.name);
                        updateStatus(group.name);
                    });
                    activate(state.schema.groups[0].name);
                }
                (async function load() {
                    try {
                        var schema = await request('/admin/api/settings/schema');
                        var list = await request('/admin/api/settings');
                        if (window.SETTINGS_ONLY_GROUP) {
                            schema.groups = schema.groups.filter(function (group) {
                                return group.name === window.SETTINGS_ONLY_GROUP;
                            });
                        }
                        if (window.SETTINGS_ONLY_FIELD || window.SETTINGS_HIDDEN_FIELD) {
                            schema.groups.forEach(function (group) {
                                group.fields = group.fields.filter(function (field) {
                                    if (window.SETTINGS_ONLY_FIELD) {
                                        return field.key === window.SETTINGS_ONLY_FIELD;
                                    }
                                    return field.key !== window.SETTINGS_HIDDEN_FIELD;
                                });
                            });
                        }
                        state.schema = schema;
                        for (var i = 0; i < schema.groups.length; i++) {
                            var name = schema.groups[i].name;
                            var group = await request('/admin/api/settings/' + encodeURIComponent(name));
                            state.values[name] = group.group.values || {};
                            state.revisions[name] = group.group.revision;
                        }
                        render();
                    } catch (error) {
                        root.innerHTML = '<p class="empty-hint" style="color:#c62828">设置加载失败：' + esc(error.message) + '</p>';
                        setToast('设置加载失败：' + error.message, false);
                    }
                })();
            })();
            </script>
            """;
        String pageTitle = headerTitle == null ? "系统管理" : headerTitle;
        String currentPage = "system";
        if (onlyGroup != null) {
            currentPage = "skins";
            String scope = "window.SETTINGS_ONLY_GROUP = '" + onlyGroup + "';";
            if (onlyField != null) {
                scope += " window.SETTINGS_ONLY_FIELD = '" + onlyField + "';";
            }
            if (hiddenField != null) {
                scope += " window.SETTINGS_HIDDEN_FIELD = '" + hiddenField + "';";
            }
            content = content
                    .replace("<h2>系统管理</h2>", "<h2>" + esc(pageTitle) + "</h2>")
                    .replace("修改系统配置并保存。", esc(headerDescription))
                    .replace("<div id=\"settingsNav\" class=\"subnav\" aria-label=\"系统设置分区\"></div>",
                            prefix + "<div id=\"settingsNav\" style=\"display:none\"></div>")
                    .replace("(function () {", "(function () {\n                " + scope);
        }
        String body = renderAdminLayout(currentPage, adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(pageTitle, body, "admin", css);
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
                            <div class="info-value appinfo-version"><span id="appVersion">{{admin.common.loading}}</span><button class="btn btn-secondary" data-action="openUpdateModal">{{admin.appinfo.checkUpdate}}</button></div>
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
            <div id="updateModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["updateModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>{{admin.appinfo.updateTitle}}</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["updateModal"]'>&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="update-source-btns">
                            <button class="btn btn-secondary" data-action="checkUpdate" data-args='["github"]'>{{admin.appinfo.updateSourceGithub}}</button>
                            <button class="btn btn-secondary" data-action="checkUpdate" data-args='["cn"]'>{{admin.appinfo.updateSourceCn}}</button>
                            <div id="updateLoading" class="update-loading" style="display:none;"><i class="fas fa-spinner fa-spin"></i><span>{{admin.appinfo.updateChecking}}</span></div>
                        </div>
                        <div id="updateResult" class="update-result">{{admin.appinfo.updateNotChecked}}</div>
                    </div>
                </div>
            </div>
            <script src="/js/admin-appinfo.js"></script>
            """);
        String body = renderAdminLayout("appinfo", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderAdminPage(I18n.t("admin.appinfo.title"), body, "admin", css);
    }

    private static String startupParamsCard() {
        ServerConfig cfg = ServerConfig.getInstance();
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

    public static String renderSecurityPage(String adminUsername, String adminRole, String csrfToken, boolean isRoot) {
        String rootManagementCard = isRoot ? tr("""
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.rootManagement}}</h3></div>
                <div class="card-body" id="rootManagementSettings">
                    <p class="setting-desc" style="margin-bottom:16px">{{admin.security.rootManagementIntro}}</p>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.rootUsername}}</div>
                            <div class="setting-desc">{{admin.security.rootUsernameDesc}}</div>
                        </div>
                        <input type="text" id="rootNewUsername" class="form-input setting-input" required autocomplete="username">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.rootCurrentPassword}}</div>
                            <div class="setting-desc">{{admin.security.rootCurrentPasswordDesc}}</div>
                        </div>
                        <input type="password" id="rootCurrentPassword" class="form-input setting-input" required autocomplete="current-password">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.rootNewPassword}}</div>
                            <div class="setting-desc">{{admin.security.rootNewPasswordDesc}}</div>
                        </div>
                        <input type="password" id="rootNewPassword" class="form-input setting-input" maxlength="128" autocomplete="new-password">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.rootConfirmPassword}}</div>
                        </div>
                        <input type="password" id="rootConfirmPassword" class="form-input setting-input" maxlength="128" autocomplete="new-password">
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveRootSettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            """) : "";
        String contentTemplate = tr("""
            <div class="page-header">
                <h2>{{admin.security.title}}</h2>
                <p class="page-desc">{{admin.security.desc}}</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.pngUploadManagement}}</h3></div>
                <div class="card-body" id="pngUploadSettings">
                    <p class="setting-desc" style="margin-bottom:16px">{{admin.security.pngUploadIntro}}</p>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngValidationEnabled}}</div>
                            <div class="setting-desc">{{admin.security.pngValidationEnabledDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="pngValidationEnabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngMaxWidth}}</div>
                            <div class="setting-desc">{{admin.security.pngDimensionDesc}}</div>
                        </div>
                        <input type="number" id="pngMaxWidth" class="form-input setting-input" min="1" max="4096" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngMaxHeight}}</div>
                            <div class="setting-desc">{{admin.security.pngDimensionDesc}}</div>
                        </div>
                        <input type="number" id="pngMaxHeight" class="form-input setting-input" min="1" max="4096" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngMaxPixels}}</div>
                            <div class="setting-desc">{{admin.security.pngMaxPixelsDesc}}</div>
                        </div>
                        <input type="number" id="pngMaxPixels" class="form-input setting-input" min="1" max="1048576" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngMaxChunkSize}}</div>
                            <div class="setting-desc">{{admin.security.pngMaxChunkSizeDesc}}</div>
                        </div>
                        <input type="number" id="pngMaxChunkSizeKib" class="form-input setting-input" min="1" max="16384" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngStrictChunkMode}}</div>
                            <div class="setting-desc">{{admin.security.pngStrictChunkModeDesc}}</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="pngStrictChunkMode">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.pngMaxConcurrent}}</div>
                            <div class="setting-desc">{{admin.security.pngMaxConcurrentDesc}}</div>
                        </div>
                        <input type="number" id="pngMaxConcurrent" class="form-input setting-input" min="1" max="99999" step="1" required>
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="savePngSettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.encryptionLevel}}</h3></div>
                <div class="card-body">
                    <p class="setting-desc" style="margin-bottom:16px;">{{admin.security.intro1}}</p>
                    <p class="setting-desc" style="margin-bottom:16px;">{{admin.security.intro2}}</p>
                    <div id="encryptionLevelList" class="encryption-level-list">
                    </div>
                    <input type="hidden" id="encryptionLevel" value="1">
                    <div class="settings-toolbar" data-save-card data-save-action="saveSettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.frequencySettings}}</h3></div>
                <div class="card-body" id="frequencySettings">
                    <p class="setting-desc" style="margin-bottom:16px">{{admin.security.frequencyIntro}}</p>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.userSessionTimeout}}</div>
                            <div class="setting-desc">{{admin.security.userSessionTimeoutDesc}}</div>
                        </div>
                        <input type="number" id="userSessionTimeoutSeconds" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.adminSessionTimeout}}</div>
                            <div class="setting-desc">{{admin.security.adminSessionTimeoutDesc}}</div>
                        </div>
                        <input type="number" id="adminSessionTimeoutSeconds" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.loginMaxAttemptsPerIp}}</div>
                            <div class="setting-desc">{{admin.security.loginMaxAttemptsPerIpDesc}}</div>
                        </div>
                        <input type="number" id="loginMaxAttemptsPerIp" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.loginMaxAttemptsPerAccount}}</div>
                            <div class="setting-desc">{{admin.security.loginMaxAttemptsPerAccountDesc}}</div>
                        </div>
                        <input type="number" id="loginMaxAttemptsPerAccount" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.loginLockoutSeconds}}</div>
                            <div class="setting-desc">{{admin.security.loginLockoutSecondsDesc}}</div>
                        </div>
                        <input type="number" id="loginLockoutSeconds" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.loginRateWindowSeconds}}</div>
                            <div class="setting-desc">{{admin.security.loginRateWindowSecondsDesc}}</div>
                        </div>
                        <input type="number" id="loginRateWindowSeconds" class="form-input setting-input" min="1" step="1" required>
                    </div>
                    <div class="setting-item" style="align-items:flex-start">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.requestIntervals}}</div>
                            <div class="setting-desc">{{admin.security.requestIntervalsDesc}}</div>
                        </div>
                    </div>
                    <textarea id="requestIntervals" class="form-input setting-textarea" rows="8"></textarea>
                    <div class="setting-item" style="align-items:flex-start;margin-top:16px">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.requestRates}}</div>
                            <div class="setting-desc">{{admin.security.requestRatesDesc}}</div>
                        </div>
                    </div>
                    <textarea id="requestRates" class="form-input setting-textarea" rows="8"></textarea>
                    <div class="settings-toolbar" data-save-card data-save-action="saveFrequencySettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.security.corsSettings}}</h3></div>
                <div class="card-body" id="corsSettings">
                    <div class="setting-desc" style="color:#DC2626;font-weight:700;margin-bottom:16px">{{admin.security.dangerWarning}}</div>
                    <p class="setting-desc" style="margin-bottom:16px">{{admin.security.corsIntro}}</p>
                    <div class="setting-item" style="align-items:flex-start">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.corsOrigins}}</div>
                            <div class="setting-desc">{{admin.security.corsOriginsDesc}}</div>
                        </div>
                    </div>
                    <textarea id="corsOrigins" class="form-input setting-textarea" rows="5"></textarea>
                    <div class="setting-item" style="align-items:flex-start;margin-top:16px">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerCsp}}</div>
                            <div class="setting-desc">{{admin.security.headerCspDesc}}</div>
                        </div>
                    </div>
                    <textarea id="headerCsp" class="form-input setting-textarea" rows="3"></textarea>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerHsts}}</div>
                        </div>
                        <input type="text" id="headerHsts" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerContentTypeOptions}}</div>
                        </div>
                        <input type="text" id="headerContentTypeOptions" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerFrameOptions}}</div>
                        </div>
                        <input type="text" id="headerFrameOptions" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerXssProtection}}</div>
                        </div>
                        <input type="text" id="headerXssProtection" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerReferrerPolicy}}</div>
                        </div>
                        <input type="text" id="headerReferrerPolicy" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerPermissionsPolicy}}</div>
                        </div>
                        <input type="text" id="headerPermissionsPolicy" class="form-input setting-input">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.security.headerCacheControl}}</div>
                        </div>
                        <input type="text" id="headerCacheControl" class="form-input setting-input">
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="confirmCorsSettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            __ROOT_MANAGEMENT_CARD__
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-security.js"></script>
            """);
        String content = contentTemplate.replace("__ROOT_MANAGEMENT_CARD__", rootManagementCard);
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
                                <th>%s</th>
                            </tr>
                        </thead>
                        <tbody id="userTableBody">
                            <tr><td colspan="9" class="text-center">%s</td></tr>
                        </tbody>
                    </table>
                    <div id="userPagination" class="admin-table-pagination"></div>
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
                I18n.t("admin.users.lastActive"),
                I18n.t("admin.common.operation"),
                I18n.t("admin.common.loading"))
            + userPermGroupsCard() + tr("""
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
                            <div class="setting-desc" style="margin-top:8px;">{{admin.admins.groupNameHint}}</div>
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

            <div id="userUuidModal" class="modal" style="display:none;">
                <div class="modal-overlay" data-action="closeModal" data-args='["userUuidModal"]'></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>UUID</h3>
                        <button class="modal-close" data-action="closeModal" data-args='["userUuidModal"]'>&times;</button>
                    </div>
                    <div class="modal-body" id="userUuidText" style="white-space:pre-line;"></div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
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
                    <div id="adminPagination" class="admin-table-pagination"></div>
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
                            <div class="setting-desc" style="margin-top:8px;">{{admin.admins.groupNameHint}}</div>
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
            <div class="subnav" style="margin-bottom:16px">
                <a class="subnav-item active" href="/admin/profiles">角色名册</a>
                <a class="subnav-item" href="/admin/profiles/settings">角色设置</a>
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
                    <div id="profilePagination" class="admin-table-pagination"></div>
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
            + """
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


    
    public static String renderProfileSettingsPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>角色设置</h2>
                <p class="page-desc">管理角色名称与数量限制。</p>
            </div>
            <div class="subnav" style="margin-bottom:16px">
                <a class="subnav-item" href="/admin/profiles">角色名册</a>
                <a class="subnav-item active" href="/admin/profiles/settings">角色设置</a>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">角色限制</h3></div>
                <div class="card-body">
                    <div class="setting-item setting-item-stack">
                        <div class="setting-info"><div class="setting-label">%s</div></div>
                        <textarea id="profileNameBlacklist" class="form-input setting-textarea setting-textarea-wide" rows="5" placeholder="admin&#10;root"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info"><div class="setting-label">%s</div></div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="profileNameBlacklistCaseSensitive">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info"><div class="setting-label">%s</div></div>
                        <input type="number" id="maxProfilesPerUser" class="form-input setting-input" min="0" max="10000">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info"><div class="setting-label">%s</div></div>
                        <div style="display:flex;align-items:center;gap:8px">
                            <input type="number" id="minProfileNameLength" class="form-input setting-input" min="1" max="64" style="width:100px">
                            <span style="color:#999">~</span>
                            <input type="number" id="maxProfileNameLength" class="form-input setting-input" min="1" max="64" style="width:100px">
                        </div>
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveProfileSettings">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <script>
            (function () {
                var revision = 0;
                function input(id) { return document.getElementById(id); }
                function set(id, value) { var el = input(id); if (el) el.value = value == null ? '' : value; }
                function setCheck(id, value) { var el = input(id); if (el) el.checked = value === 'true' || value === true; }
                function load() {
                    fetch('/admin/api/profiles/settings').then(function (r) { return r.json(); }).then(function (data) {
                        if (!data.success) return;
                        revision = data.revision || 0;
                        var values = data.values || {};
                        set('profileNameBlacklist', values.profile_name_blacklist);
                        setCheck('profileNameBlacklistCaseSensitive', values.profile_name_blacklist_case_sensitive);
                        set('maxProfilesPerUser', values.max_profiles_per_user);
                        set('minProfileNameLength', values.min_profile_name_length);
                        set('maxProfileNameLength', values.max_profile_name_length);
                        if (window.refreshAdminSaveCards) window.refreshAdminSaveCards();
                    }).catch(function () {});
                }
                window.saveProfileSettings = function () {
                    var values = {
                        profile_name_blacklist: input('profileNameBlacklist').value,
                        profile_name_blacklist_case_sensitive: input('profileNameBlacklistCaseSensitive').checked ? 'true' : 'false',
                        max_profiles_per_user: input('maxProfilesPerUser').value,
                        min_profile_name_length: input('minProfileNameLength').value,
                        max_profile_name_length: input('maxProfileNameLength').value
                    };
                    if (Number(values.min_profile_name_length) > Number(values.max_profile_name_length)) {
                        if (window.showToast) showToast('最小长度不能大于最大长度', 'error');
                        return false;
                    }
                    return fetch('/admin/api/profiles/settings', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': window.CSRF_TOKEN || '' },
                        body: JSON.stringify({ revision: revision, values: values })
                    }).then(function (r) {
                        return r.json().then(function (data) { return { ok: r.ok, data: data }; });
                    }).then(function (result) {
                        if (result.ok && result.data.success) {
                            revision = result.data.revision;
                            if (window.showToast) showToast('角色设置已保存', 'success');
                            return true;
                        }
                        if (result.data && result.data.revision != null) revision = result.data.revision;
                        return verifyPersisted(result.data && result.data.message);
                    }).catch(function () {
                        return verifyPersisted('保存失败');
                    });
                    function verifyPersisted(message) {
                        return fetch('/admin/api/profiles/settings').then(function (r) { return r.json(); }).then(function (data) {
                            var actual = data.values || {};
                            var persisted = Object.keys(values).every(function (key) {
                                return String(actual[key]) === String(values[key]);
                            });
                            if (persisted) {
                                revision = data.revision;
                                if (window.showToast) showToast('角色设置已保存', 'success');
                                return true;
                            }
                            if (window.showToast) showToast(message || '保存失败', 'error');
                            return false;
                        }).catch(function () {
                            if (window.showToast) showToast(message || '保存失败', 'error');
                            return false;
                        });
                    }
                };
                load();
            })();
            </script>
            """.formatted(
                I18n.t("admin.nameBlacklist.profileLabel"),
                I18n.t("admin.nameBlacklist.caseSensitive"),
                I18n.t("admin.profiles.maxProfilesPerUser"),
                I18n.t("admin.profiles.nameLengthRange"));
        String body = renderAdminLayout("profiles", adminUsername, adminRole, content, csrfToken);
        return PageRenderer.renderAdminPage(I18n.t("admin.profiles.settings"), body, "admin", Css.getAdminCssLink());
    }

    public static String renderYggdrasilPage(String adminUsername, String adminRole, String csrfToken,
                                             String apiUrl, String userUrl, String apiMode) {
        String content = tr("""
            <div class="page-header">
                <h2>{{admin.yggdrasil.title}}</h2>
                <p class="page-desc">{{admin.yggdrasil.desc}}</p>
            </div>
            <div class="settings-card card">
                <div class="card-header" style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;">
                    <h3 class="card-title"><i class="fas fa-globe" style="margin-right:8px;"></i>{{admin.yggdrasil.connectivityTitle}}</h3>
                    <span class="badge yggdrasil-connectivity-badge">{{admin.yggdrasil.reviewRequired}}</span>
                </div>
                <div class="card-body">
                    <div class="yggdrasil-connectivity-warning">
                        <strong>{{admin.yggdrasil.connectivityWarning}}</strong>
                        <div class="yggdrasil-connectivity-link">{{admin.yggdrasil.domainSettingsLink}}</div>
                    </div>
                    <div class="yggdrasil-connectivity-grid">
                        <div class="setting-item yggdrasil-connectivity-item">
                            <div class="setting-info">
                                <div class="setting-label">{{admin.yggdrasil.effectiveApiUrl}}</div>
                                <div class="setting-desc">{{admin.yggdrasil.effectiveApiUrlDesc}}</div>
                            </div>
                            <div class="form-input yggdrasil-connectivity-value">__EFFECTIVE_API_URL__</div>
                            __API_URL_WARNING__
                        </div>
                        <div class="setting-item yggdrasil-connectivity-item">
                            <div class="setting-info">
                                <div class="setting-label">{{admin.yggdrasil.effectiveUserUrl}}</div>
                                <div class="setting-desc">{{admin.yggdrasil.effectiveUserUrlDesc}}</div>
                            </div>
                            <div class="form-input yggdrasil-connectivity-value">__EFFECTIVE_USER_URL__</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title"><i class="fas fa-diagram-project" style="margin-right:8px;"></i>{{admin.yggdrasil.apiModeSection}}</h3></div>
                <div class="card-body">
                    <p class="setting-desc" style="margin-bottom:14px">{{admin.yggdrasil.apiModeDesc}}</p>
                    <div class="api-mode-toggle" id="apiModeToggle" data-value="default">
                        <span class="api-mode-thumb" id="apiModeThumb"></span>
                        <button type="button" class="api-mode-btn active" data-mode="default" data-action="selectApiMode" data-args='["default"]'>{{admin.yggdrasil.apiModeDefault}}</button>
                        <button type="button" class="api-mode-btn" data-mode="standalone" data-action="selectApiMode" data-args='["standalone"]'>{{admin.yggdrasil.apiModeStandalone}}</button>
                        <button type="button" class="api-mode-btn" data-mode="aggregate" data-action="selectApiMode" data-args='["aggregate"]'>{{admin.yggdrasil.apiModeAggregate}}</button>
                    </div>
                    <input type="hidden" id="apiModeValue" data-ygg-key="api_mode" value="default">
                    <div class="api-mode-note">{{admin.yggdrasil.apiModeRecommendHint}}</div>
                    <div class="api-mode-note">{{admin.yggdrasil.apiModeRestartHint}}</div>
                    <div class="api-mode-detail" id="apiModeDetail">
                        <div class="api-mode-detail-item" data-mode="default">
                            <div class="api-mode-detail-title">{{admin.yggdrasil.apiModeDefault}} <span class="api-mode-badge">{{admin.yggdrasil.apiModeBadgeDefault}}</span></div>
                            <p>{{admin.yggdrasil.apiModeDefaultDesc}}</p>
                        </div>
                        <div class="api-mode-detail-item" data-mode="standalone">
                            <div class="api-mode-detail-title">{{admin.yggdrasil.apiModeStandalone}} <span class="api-mode-badge api-mode-badge-rec">{{admin.yggdrasil.apiModeBadgeRecommended}}</span></div>
                            <p>{{admin.yggdrasil.apiModeStandaloneDesc}}</p>
                        </div>
                        <div class="api-mode-detail-item" data-mode="aggregate">
                            <div class="api-mode-detail-title">{{admin.yggdrasil.apiModeAggregate}} <span class="api-mode-badge api-mode-badge-no">{{admin.yggdrasil.apiModeBadgeDeprecated}}</span></div>
                            <p>{{admin.yggdrasil.apiModeAggregateDesc}}</p>
                        </div>
                    </div>
                    <p class="setting-desc" style="margin-top:10px">{{admin.yggdrasil.apiModeSaveHint}}</p>
                    <div class="settings-toolbar" data-save-card data-save-action="saveYggdrasilSection">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">{{admin.yggdrasil.uuidSection}}</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.uuidVersion}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.uuidVersionDesc}}</div>
                        </div>
                        <select id="uuidVersion" class="form-input setting-select" data-ygg-key="uuid_version">
                            <option value="v3">v3 (MD5)</option>
                            <option value="v4">v4 (Random)</option>
                            <option value="v5">v5 (SHA-1)</option>
                        </select>
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveYggdrasilSection">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
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
                        <input type="number" id="tokenTempExpiry" class="form-input setting-input" data-ygg-key="token_temp_expiry" placeholder="4320">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.tokenPermExpiry}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.tokenPermExpiryDesc}}</div>
                        </div>
                        <input type="number" id="tokenPermanentExpiry" class="form-input setting-input" data-ygg-key="token_permanent_expiry" placeholder="10080">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.maxTokens}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.maxTokensDesc}}</div>
                        </div>
                        <input type="number" id="maxTokensPerProfile" class="form-input setting-input" data-ygg-key="max_tokens_per_profile" placeholder="12">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.worldTreeKeyLength}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.worldTreeKeyLengthDesc}}</div>
                        </div>
                        <input type="number" id="worldTreeKeyLength" class="form-input setting-input" data-ygg-key="world_tree_key_length" placeholder="32" min="16" max="256">
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveYggdrasilSection">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
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
                        <input type="number" id="authRateLimit" class="form-input setting-input" data-ygg-key="auth_rate_limit" placeholder="1000">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.batchQuery}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.batchQueryDesc}}</div>
                        </div>
                        <input type="number" id="batchQueryMaxCount" class="form-input setting-input" data-ygg-key="batch_query_max_count" placeholder="6">
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveYggdrasilSection">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
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
                        <select id="signatureMode" class="form-input setting-select" data-action="onModeChange" data-save-ignore>
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
                        <textarea id="yggdrasilPublicKey" class="form-input setting-textarea" rows="6" readonly data-ygg-key="yggdrasil_public_key" placeholder="{{admin.yggdrasil.keyPlaceholder}}"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">{{admin.yggdrasil.privateKey}}</div>
                            <div class="setting-desc">{{admin.yggdrasil.privateKeyDesc}}</div>
                        </div>
                        <textarea id="yggdrasilPrivateKey" class="form-input setting-textarea" rows="6" data-ygg-key="yggdrasil_private_key" placeholder="{{admin.yggdrasil.keyPlaceholder}}"></textarea>
                    </div>
                    <div style="display:flex;gap:10px;margin-top:12px;">
                        <button class="btn btn-primary" data-action="regenerateKeys">{{admin.yggdrasil.regenerate}}</button>
                        <button class="btn btn-warning" id="switchModeBtn" data-action="confirmSwitchMode" style="display:none;">{{admin.yggdrasil.switchMode}}</button>
                    </div>
                    <div class="settings-toolbar" data-save-card data-save-action="saveYggdrasilSection">
                        <span class="settings-status" data-save-status>未修改</span>
                        <button type="button" class="btn btn-secondary" data-save-reset disabled>放弃更改</button>
                        <button type="button" class="btn btn-primary" data-save-submit disabled>保存更改</button>
                    </div>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-yggdrasil.js"></script>
            """);
        String apiUrlWarning = SystemConfig.API_MODE_AGGREGATE.equals(apiMode)
                ? "<div class=\"yggdrasil-api-warning\"><i class=\"fas fa-triangle-exclamation\"></i> "
                        + esc(I18n.t("admin.yggdrasil.apiUrlAggregateNotice")) + "</div>"
                : "";
        content = content.replace("__EFFECTIVE_API_URL__", esc(apiUrl == null || apiUrl.isBlank() ? I18n.t("msg.unknown") : apiUrl))
                .replace("__EFFECTIVE_USER_URL__", esc(userUrl == null || userUrl.isBlank() ? I18n.t("msg.unknown") : userUrl))
                .replace("__API_URL_WARNING__", apiUrlWarning);
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
