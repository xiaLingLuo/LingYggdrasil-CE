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

import im.xz.cn.web.view.Css;
import im.xz.cn.common.FooterInfo;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.i18n.I18n;
import im.xz.cn.i18n.LocaleContext;

public class PageRenderer {

    private static final java.util.regex.Pattern I18N_TOKEN =
            java.util.regex.Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");

    private static final java.util.regex.Pattern INLINE_SCRIPT =
            java.util.regex.Pattern.compile("<script(?![^>]*\\bsrc=)");

    public static String tr(String template) {
        java.util.regex.Matcher m = I18N_TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(I18n.t(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    private static String getBaseCss() {
        return Css.getBaseCss();
    }

    public static String renderPage(String title, String bodyContent, String pageType, String... extraCss) {
        StringBuilder linkTags = new StringBuilder();
        StringBuilder inlineCss = new StringBuilder();
        for (String c : extraCss) {
            String trimmed = c.trim();
            if (trimmed.startsWith("<link")) {
                linkTags.append(trimmed).append("\n");
            } else if (trimmed.startsWith("@import")) {
                inlineCss.append(trimmed).append("\n");
            } else {
                inlineCss.append(c).append("\n");
            }
        }

        String inlineStyleBlock = !inlineCss.isEmpty()
                ? "<style>\n" + inlineCss.toString() + "</style>\n"
                : "";

        String locale = LocaleContext.get();
        String langCookie = (pageType != null && pageType.startsWith("admin")) ? "LING_ADMIN_LANG"
                : "install".equals(pageType) ? "LING_INSTALL_LANG"
                : "LING_USER_LANG";
        String settingsBase = (pageType != null && pageType.startsWith("admin")) ? "/admin" : "";
        String permsScript = (pageType != null && pageType.startsWith("admin"))
                ? "window.__ADMIN_PERMS__=" + im.xz.cn.security.AdminPermissions.currentJson() + ";"
                : "";
        String i18nScript = "<script>window.__LOCALE__='" + locale + "';window.__LANG_COOKIE__='"
                + langCookie + "';window.__SETTINGS_BASE__='" + settingsBase + "';" + permsScript
                + "window.__I18N__=" + I18n.rawJson(locale) + ";</script>";

        String footerHtml = "admin".equals(pageType) ? "" : renderFooter();

        String html = """
            <!DOCTYPE html>
            <html lang="%s">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="icon" type="image/x-icon" href="/icons/app.ico">
                <title>%s - %s</title>
                <script>(function(){try{var t=localStorage.getItem('ling-theme');if(!t&&window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches)t='dark';document.documentElement.setAttribute('data-theme',t==='dark'?'dark':'light');}catch(e){}})();</script>
                %s
                <link rel="stylesheet" href="/css/tokens.css">
                <link rel="stylesheet" href="/css/all.min.css">
                <link rel="stylesheet" href="/css/announcement.css">
                %s
                <style>
                    %s
                </style>
                %s
            </head>
            <body>
                <script src="/js/common.js"></script>
                %s
                <script src="/js/announcement.js"></script>
                %s
            </body>
            </html>
            """.formatted(
                locale,
                escapeHtml(title),
                "LingYggdrasil",
                i18nScript,
                linkTags.toString(),
                getBaseCss(),
                inlineStyleBlock,
                bodyContent,
                footerHtml);

        String result = FooterInfo.injectFooterRecords(html);
        String nonce = Csp.current();
        if (nonce != null && !nonce.isEmpty()) {
            result = INLINE_SCRIPT.matcher(result)
                    .replaceAll(java.util.regex.Matcher.quoteReplacement("<script nonce=\"" + nonce + "\""));
        }
        return result;
    }

    public static String renderFooter() {
        String siteName = SystemConfig.getInstance().getSiteName();
        return """
            <footer class="page-footer">
                <p>%s, Powered by <a href="https://github.com/xiaLingLuo/LingYggdrasil-CE" target="_blank" rel="noopener">LingYggdrasil</a> %s</p>
                <p class="footer-records" style="margin-top:4px;font-size:12px;color:#999;">%s</p>
            </footer>
            """.formatted(escapeHtml(siteName), escapeHtml(AppConfig.APP_VERSION), FooterInfo.FOOTER_PLACEHOLDER);
    }

    public static String renderNavbar(String siteName, String currentPage, boolean isAdmin) {
        String adminLink = isAdmin ?
            """
            <a href="/admin" class="nav-link %s">%s</a>
            """.formatted("admin".equals(currentPage) ? "active" : "", I18n.t("nav.admin")) : "";

        String worldLink = "";
        if (!isAdmin) {
            worldLink = """
            <a href="/world" class="nav-link">%s</a>
            """.formatted(I18n.t("nav.world"));
        }

        return """
            <nav class="navbar">
                <div class="navbar-inner">
                    <div class="navbar-brand">
                        <button type="button" class="menu-toggle" data-action="toggleSidebar" aria-label="%s"><i class="fas fa-bars"></i></button>
                        <img src="/icons/app.ico" class="logo-icon brand-icon" alt="LingYggdrasil">
                        <span class="brand-text">%s</span>
                    </div>
                    <div class="navbar-links">
                        <a href="/" class="nav-link %s">%s</a>
                        %s
                        %s
                        %s
                        <button type="button" class="nav-link theme-toggle" data-action="toggleTheme" aria-label="%s" title="%s"><i class="fas fa-moon"></i></button>
                    </div>
                </div>
            </nav>
            """.formatted(
                I18n.t("nav.menu"),
                escapeHtml(siteName),
                "home".equals(currentPage) ? "active" : "",
                I18n.t("nav.home"),
                worldLink,
                adminLink,
                renderLanguageSwitcher(false),
                I18n.t("nav.theme"),
                I18n.t("nav.theme"));
    }

    public static String renderLanguageSwitcher(boolean sidebar) {
        String current = LocaleContext.get();
        StringBuilder items = new StringBuilder();
        for (I18n.LocaleOption opt : I18n.supportedLocales()) {
            boolean active = opt.code().equalsIgnoreCase(current);
            items.append("<button type=\"button\" class=\"lang-item")
                    .append(active ? " active" : "")
                    .append("\" data-action=\"setLanguage\" data-args='[\"").append(opt.code()).append("\"]'>")
                    .append("<span class=\"lang-item-main\">")
                    .append("<img class=\"lang-icon\" src=\"/icons/lang-").append(opt.code())
                    .append(".svg\" alt=\"\" width=\"18\" height=\"18\" loading=\"lazy\">")
                    .append(escapeHtml(opt.label()))
                    .append("</span>");
            if (active) items.append(" <i class=\"fas fa-check\"></i>");
            items.append("</button>");
        }

        String button = sidebar
                ? """
                  <button type="button" class="sidebar-link lang-toggle" data-action="toggleLangMenu" data-event>
                      <span class="sidebar-link-icon"><i class="fas fa-language"></i></span>
                      <span>%s</span>
                  </button>
                  """.formatted(I18n.t("nav.language"))
                : """
                  <button type="button" class="nav-link lang-toggle" data-action="toggleLangMenu" data-event aria-label="%s" title="%s"><i class="fas fa-language"></i></button>
                  """.formatted(I18n.t("nav.language"), I18n.t("nav.language"));

        return """
            <div class="lang-dropdown">
                %s
                <div class="lang-menu">%s</div>
            </div>
            """.formatted(button, items.toString());
    }

    public static String renderSidebar(String currentPage, boolean isAdmin, boolean isRoot) {
        StringBuilder items = new StringBuilder();

        items.append(sidebarItem("/admin/dashboard", "dashboard", I18n.t("adminSidebar.dashboard"), currentPage));
        items.append(sidebarItem("/admin/users", "users", I18n.t("adminSidebar.users"), currentPage));
        items.append(sidebarItem("/admin/profiles", "profiles", I18n.t("adminSidebar.profiles"), currentPage));
        items.append(sidebarItem("/admin/system", "system", I18n.t("adminSidebar.system"), currentPage));

        if (isRoot) {
            items.append(sidebarItem("/admin/admins", "admins", I18n.t("adminSidebar.admins"), currentPage));
            items.append(sidebarItem("/admin/yggdrasil", "yggdrasil", I18n.t("adminSidebar.yggdrasil"), currentPage));
        }

        return """
            <aside class="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-icon"><i class="fas fa-bars"></i></span>
                    <span>""" + I18n.t("adminSidebar.menu") + """
            </span>
                </div>
                <div class="sidebar-menu">
                    %s
                </div>
            </aside>
            """.formatted(items.toString());
    }

    public static String renderCard(String title, String content) {
        return """
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">%s</h3>
                </div>
                <div class="card-body">
                    %s
                </div>
            </div>
            """.formatted(escapeHtml(title), content);
    }

    public static String renderAlert(String message, String type) {
        String icon = switch (type) {
            case "success" -> "<i class=\"fas fa-circle-check\"></i>";
            case "error" -> "<i class=\"fas fa-circle-xmark\"></i>";
            case "warning" -> "<i class=\"fas fa-triangle-exclamation\"></i>";
            default -> "<i class=\"fas fa-circle-info\"></i>";
        };
        return """
            <div class="alert alert-%s">
                <span class="alert-icon">%s</span>
                <span class="alert-message">%s</span>
            </div>
            """.formatted(type, icon, escapeHtml(message));
    }

    private static String sidebarItem(String href, String page, String label, String currentPage) {
        String active = page.equals(currentPage) ? "active" : "";
        return """
            <a href="%s" class="sidebar-item %s">
                <span>%s</span>
            </a>
            """.formatted(href, active, label);
    }
}
