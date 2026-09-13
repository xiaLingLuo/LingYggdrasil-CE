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
import im.xz.cn.i18n.I18n;
import im.xz.cn.web.PageRenderer;
import im.xz.cn.web.Shared;

public class UserPage {

    public static String welcomeSection(String displayName, String username, String email, boolean emailVerified, String createdAt) {
        String emailBadge = emailVerified
                ? "<span class='badge badge-success'>" + I18n.t("user.dashboard.verified") + "</span>"
                : "<span class='badge badge-warning'>" + I18n.t("user.dashboard.unverified") + "</span>";
        String date = createdAt != null ? createdAt.substring(0, Math.min(10, createdAt.length())) : "-";
        return """
            <div class="welcome-section">
                <h2>%s <i class="fas fa-wand-magic-sparkles"></i></h2>
                <p class="text-muted">%s</p>
                <p class="text-muted">%s</p>
            </div>
            """.formatted(
                I18n.t("user.dashboard.welcome", esc(displayName)),
                I18n.t("user.dashboard.account", esc(username), esc(email), emailBadge),
                I18n.t("user.dashboard.registeredAt", date));
    }

    public static String pageHeader(String title, String desc) {
        return """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            """.formatted(esc(title), esc(desc));
    }

    public static String yggdrasilGuide(String apiDomain) {
        String displayUrl = (apiDomain != null && !apiDomain.isBlank())
                ? esc(apiDomain)
                : "<span style='color:#999'>" + I18n.t("user.guide.apiMissing") + "</span>";
        return PageRenderer.tr(String.format("""
            <div class="yggdrasil-guide">
                <div class="guide-step">
                    <div class="guide-step-num">1</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">{{user.guide.step1Title}}</div>
                        <div class="guide-step-desc">{{user.guide.step1Desc}}</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">2</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">{{user.guide.step2Title}}</div>
                        <div class="guide-step-desc">{{user.guide.step2Desc}}</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">3</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">{{user.guide.step3Title}}</div>
                        <div class="guide-step-desc">{{user.guide.step3Desc}}</div>
                        <div class="guide-code" id="yggdrasilUrl">%s</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">4</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">{{user.guide.step4Title}}</div>
                        <div class="guide-step-desc">{{user.guide.step4Desc}}</div>
                    </div>
                </div>
                <div class="guide-tips">
                    <div class="guide-tips-title"><i class="fas fa-lightbulb"></i> {{user.guide.tipsTitle}}</div>
                    <ul>
                        <li>{{user.guide.tip1}}</li>
                        <li>{{user.guide.tip2}}</li>
                        <li>{{user.guide.tip3}}</li>
                    </ul>
                </div>
            </div>
            """, displayUrl));
    }

    public static String accountInfoContent(String username, String email, int profileCount,
                                            int skinCount, int capeCount, int publicCount, int friendCount) {
        return """
            <div class="account-info">
                <div class="account-field">
                    <div class="account-field-label">%s</div>
                    <div class="account-field-value">%s</div>
                </div>
                <div class="account-field">
                    <div class="account-field-label">%s</div>
                    <div class="account-field-value">%s</div>
                </div>
            </div>
            <div class="account-stats">
                %s
                %s
                %s
                %s
                %s
            </div>
            """.formatted(
                I18n.t("user.account.username"), esc(username),
                I18n.t("user.account.email"), esc(email),
                statTile(profileCount, "user.account.profileCount"),
                statTile(skinCount, "user.account.skinCount"),
                statTile(capeCount, "user.account.capeCount"),
                statTile(publicCount, "user.account.publicCount"),
                statTile(friendCount, "user.account.friendCount"));
    }

    private static String formatKiB(long bytes) {
        return String.format("%.1f KiB", bytes / 1024.0);
    }

    private static String statTile(int value, String labelKey) {
        return "<div class=\"account-stat\"><div class=\"account-stat-num\">" + value
                + "</div><div class=\"account-stat-label\">" + I18n.t(labelKey) + "</div></div>";
    }

    public static String nicknameForm(String currentNickname) {
        return PageRenderer.tr("""
            <form id="nicknameForm">
                <div class="form-group">
                    <label class="form-label">{{user.settings.currentNickname}}</label>
                    <input type="text" class="form-input" value="%s" disabled>
                </div>
                <div class="form-group">
                    <label class="form-label">{{user.settings.newNickname}}</label>
                    <input type="text" class="form-input" id="newNickname" placeholder="{{user.settings.newNicknamePlaceholder}}" maxlength="32">
                </div>
                <button type="submit" class="btn btn-primary">{{user.settings.saveNickname}}</button>
            </form>
            <div id="nicknameMsg" class="msg-area"></div>
            """.formatted(esc(currentNickname)));
    }

    public static String emailForm(String currentEmail) {
        return PageRenderer.tr("""
            <form id="emailForm">
                <div class="form-group">
                    <label class="form-label">{{user.settings.currentEmail}}</label>
                    <input type="text" class="form-input" value="%s" disabled>
                </div>
                <div class="form-group">
                    <label class="form-label">{{user.settings.newEmail}}</label>
                    <input type="email" class="form-input" id="newEmail" placeholder="{{user.settings.newEmailPlaceholder}}">
                </div>
                <div class="form-group">
                    <label class="form-label">{{user.settings.currentPassword}}</label>
                    <input type="password" class="form-input" id="emailPassword" placeholder="{{user.settings.currentPasswordPlaceholder}}">
                </div>
                <div class="form-group form-row">
                    <input type="text" class="form-input" id="emailVerifyCode" placeholder="{{user.settings.verifyCodePlaceholder}}" style="flex:1">
                    <button type="button" class="btn btn-secondary" id="sendEmailCodeBtn" data-action="sendSettingsCode" data-args='["email_change"]'>{{user.settings.sendCode}}</button>
                </div>
                <button type="submit" class="btn btn-primary">{{user.settings.changeEmail}}</button>
            </form>
            <div id="emailMsg" class="msg-area"></div>
            """.formatted(esc(currentEmail)));
    }

    public static String passwordForm() {
        return PageRenderer.tr("""
            <form id="passwordForm">
                <div class="form-group">
                    <label class="form-label">{{user.settings.currentPassword}}</label>
                    <input type="password" class="form-input" id="currentPassword" placeholder="{{user.settings.currentPasswordPlaceholder}}">
                </div>
                <div class="form-group">
                    <label class="form-label">{{user.settings.newPassword}}</label>
                    <input type="password" class="form-input" id="newPassword" placeholder="{{user.settings.newPasswordPlaceholder}}">
                    <div class="form-hint">{{user.settings.passwordHint}}<span>@</span><span>$</span><span>!</span><span>%</span><span>*</span><span>?</span><span>&amp;</span></div>
                </div>
                <div class="form-group">
                    <label class="form-label">{{user.settings.confirmPassword}}</label>
                    <input type="password" class="form-input" id="confirmPassword" placeholder="{{user.settings.confirmPasswordPlaceholder}}">
                </div>
                <div class="form-group form-row">
                    <input type="text" class="form-input" id="passVerifyCode" placeholder="{{user.settings.verifyCodePlaceholder}}" style="flex:1">
                    <button type="button" class="btn btn-secondary" id="sendPassCodeBtn" data-action="sendSettingsCode" data-args='["password_change"]'>{{user.settings.sendCode}}</button>
                </div>
                <button type="submit" class="btn btn-primary">{{user.settings.changePassword}}</button>
            </form>
            <div id="passwordMsg" class="msg-area"></div>
            """);
    }

    public static String createProfileForm() {
        return PageRenderer.tr("""
            <form id="createProfileForm" class="inline-form">
                <input type="text" class="form-input" id="newProfileName" placeholder="{{user.profiles.name}}" maxlength="16" style="flex:1">
                <button type="submit" class="btn btn-primary">{{user.profiles.create}}</button>
            </form>
            <div id="createMsg" class="msg-area"></div>
            """);
    }

    public static String skinsContent() {
        return """
            <input type="file" id="skinFile" accept="image/png" style="display:none">
            <div id="uploadMsg" class="msg-area" style="display:none"></div>
            """ +
            "<div class='texture-grid' id='skinList'>" +
            "<div class='upload-tile card-animate' id='skinUploadTile'>" +
            "<div class='upload-tile-icon'>" +
            "<svg viewBox='0 0 24 24'><path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><polyline points='17 8 12 3 7 8'/><line x1='12' y1='3' x2='12' y2='15'/></svg>" +
            "</div>" +
            "<div class='upload-tile-text'>" + I18n.t("texture.uploadSkin") + "</div>" +
            "<div class='upload-tile-hint'>" + I18n.t("texture.pngHint") + "</div>" +
            "</div>" +
            "<p class='text-muted' style='grid-column:1/-1'>" + I18n.t("common.loading") + "</p>" +
            "</div>";
    }

    public static String capesContent() {
        return """
            <input type="file" id="capeFile" accept="image/png" style="display:none">
            <div id="uploadMsg" class="msg-area" style="display:none"></div>
            """ +
            "<div class='texture-grid' id='capeList'>" +
            "<div class='upload-tile card-animate' id='capeUploadTile'>" +
            "<div class='upload-tile-icon'>" +
            "<svg viewBox='0 0 24 24'><path d='M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4'/><polyline points='17 8 12 3 7 8'/><line x1='12' y1='3' x2='12' y2='15'/></svg>" +
            "</div>" +
            "<div class='upload-tile-text'>" + I18n.t("texture.uploadCape") + "</div>" +
            "<div class='upload-tile-hint'>" + I18n.t("texture.pngHint") + "</div>" +
            "</div>" +
            "<p class='text-muted' style='grid-column:1/-1'>" + I18n.t("common.loading") + "</p>" +
            "</div>";
    }

    public static String bannedPage() {
        return PageRenderer.tr("""
            <!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><title>{{user.banned.pageTitle}}</title>
            <link rel="stylesheet" href="/css/all.min.css">
            <style>body{font-family:'Segoe UI','Microsoft YaHei',sans-serif;background:linear-gradient(135deg,#FFF0F5,#F5F0FF);
            display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;color:#333}
            .box{background:#fff;border-radius:16px;padding:60px 48px;box-shadow:0 8px 32px rgba(255,105,180,0.12);text-align:center;max-width:420px}
            h1{color:#FF69B4;margin-bottom:16px}p{color:#666;line-height:1.6}</style></head>
            <body><div class="box"><h1><i class="fas fa-circle-xmark"></i> {{user.banned.title}}</h1><p>{{user.banned.line1}}</p>
            <p>{{user.banned.line2}}</p></div></body></html>
            """);
    }

    public static String renderDashboardPage(String csrfToken, String siteName,
                                              String displayName, String username, String email,
                                              boolean emailVerified, String createdAt,
                                              String apiDomain, int profileCount,
                                              int skinCount, int capeCount, int publicCount, int friendCount) {
        String welcome = welcomeSection(displayName, username, email, emailVerified, createdAt);
        String guide = yggdrasilGuide(apiDomain);
        String guideSection = PageRenderer.renderCard(I18n.t("user.dashboard.guideTitle"), guide);
        String accountSection = PageRenderer.renderCard(I18n.t("user.dashboard.accountTitle"),
                accountInfoContent(username, email, profileCount, skinCount, capeCount, publicCount, friendCount));
        String widgets = "<div class='card-grid dashboard-grid' data-sortable='user-dashboard'>"
                + "<div class='sortable-item' data-widget='guide'>" + guideSection + "</div>"
                + "<div class='sortable-item' data-widget='account'>" + accountSection + "</div>"
                + "</div>";
        String body = Shared.buildUserLayout(siteName, "dashboard", welcome + widgets, "dashboard");
        String csrf = Shared.csrfInject(csrfToken);
        return PageRenderer.renderPage(I18n.t("user.dashboard.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderSettingsPage(String csrfToken, String siteName, String nickname, String email) {
        String content = pageHeader(I18n.t("user.settings.title"), I18n.t("user.settings.desc"))
                + PageRenderer.renderCard(I18n.t("user.settings.nicknameCard"), nicknameForm(nickname))
                + "<br>" + PageRenderer.renderCard(I18n.t("user.settings.emailCard"), emailForm(email))
                + "<br>" + PageRenderer.renderCard(I18n.t("user.settings.passwordCard"), passwordForm())
                + "<br>" + PageRenderer.renderCard(I18n.t("user.settings.blockedCard"),
                    "<div style='text-align:center'>"
                    + "<p id='blockedCountText' style='color:#999;margin-bottom:12px'>" + I18n.t("common.loading") + "</p>"
                    + "<div style='display:flex;gap:10px;justify-content:center'>"
                    + "<button class='btn btn-danger' id='clearBlockedBtn'>" + I18n.t("user.settings.clearBlocked") + "</button>"
                    + "<button class='btn btn-secondary' id='manageBlockedBtn'>" + I18n.t("user.settings.manageBlocked") + "</button>"
                    + "</div></div>");
        String body = Shared.buildUserLayout(siteName, "settings", content, "settings");
        String csrf = Shared.csrfInject(csrfToken);
        return PageRenderer.renderPage(I18n.t("user.settings.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderLogsPage(String csrfToken, String siteName, String logContent,
                                        long usedBytes, long maxBytes, int percent) {
        String usage = """
            <div class="log-usage">
                <div class="log-usage-head">
                    <span>%s</span>
                    <span>%s / %s</span>
                </div>
                <div class="log-usage-bar"><div class="log-usage-fill" style="width:%d%%"></div></div>
                <div class="log-usage-actions">
                    <button class="btn btn-secondary" data-action="downloadLog">%s</button>
                    <button class="btn btn-danger" data-action="clearLog">%s</button>
                </div>
            </div>
            """.formatted(
                I18n.t("user.logs.usage"),
                formatKiB(usedBytes), formatKiB(maxBytes), percent,
                I18n.t("user.logs.download"), I18n.t("user.logs.clear"));
        String inner = usage + ((logContent == null || logContent.isBlank())
                ? "<p class='text-muted'>" + I18n.t("user.logs.empty") + "</p>"
                : "<textarea class='action-log' readonly>" + esc(logContent) + "</textarea>");
        String content = pageHeader(I18n.t("user.logs.title"), I18n.t("user.logs.desc"))
                + PageRenderer.renderCard(I18n.t("user.logs.title"), inner);
        String body = Shared.buildUserLayout(siteName, "logs", content, "logs");
        String csrf = Shared.csrfInject(csrfToken);
        return PageRenderer.renderPage(I18n.t("user.logs.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderProfilesPage(String csrfToken, String siteName, String apiDomain) {
        String content = pageHeader(I18n.t("user.profiles.title"), I18n.t("user.profiles.desc"))
                + "<div id='profileList'><p class='text-muted'>" + I18n.t("common.loading") + "</p></div>"
                + "<div id='toast' class='toast' style='display:none;'></div>";
        String body = Shared.buildUserLayout(siteName, "profiles", content, "profiles");
        String apiDomainInject = "<script>window.YGGDRASIL_API_DOMAIN='" + esc(apiDomain != null ? apiDomain : "") + "';</script>";
        String csrf = Shared.csrfInject(csrfToken)
                + apiDomainInject
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.profiles.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderSkinsPage(String csrfToken) {
        String content = pageHeader(I18n.t("user.skins.title"), I18n.t("user.skins.desc"))
                + skinsContent();
        String navbar = PageRenderer.renderNavbar(I18n.t("nav.userCenter"), "user", false);
        String sidebar = Shared.buildUserSidebar("skins");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            <script src="/js/user-skins.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.skins.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }

    public static String renderCapesPage(String csrfToken) {
        String content = pageHeader(I18n.t("user.capes.title"), I18n.t("user.capes.desc"))
                + capesContent();
        String navbar = PageRenderer.renderNavbar(I18n.t("nav.userCenter"), "user", false);
        String sidebar = Shared.buildUserSidebar("capes");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            <script src="/js/user-capes.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.capes.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }

    public static String friendsContent() {
        return PageRenderer.tr("""
            <div class="friend-grid" id="friendList">
            <div class="friend-card friend-my-card card-animate" id="friendMyCard">
            <div class="friend-card-skin" id="mySkinPreview"></div>
            <div class="friend-card-info">
            <div class="friend-card-name" id="myName">{{friends.me}}</div>
            <div class="friend-card-code" id="myCode">----</div>
            </div>
            </div>
            <div class="friend-add-tile card-animate" id="friendAddTile">
            <div class="friend-add-tile-icon">
            <i class="fas fa-user-plus"></i>
            </div>
            <div class="friend-add-tile-text">{{friends.add}}</div>
            <div class="friend-add-tile-hint">{{friends.enterCode}}</div>
            </div>
            <p class="text-muted" style="grid-column:1/-1">{{common.loading}}</p>
            </div>
            <div id="toast" class="toast" style="display:none"></div>
            """);
    }

    public static String renderFriendsPage(String csrfToken) {
        String content = pageHeader(I18n.t("user.friends.title"), I18n.t("user.friends.desc"))
                + friendsContent();
        String navbar = PageRenderer.renderNavbar(I18n.t("nav.userCenter"), "user", false);
        String sidebar = Shared.buildUserSidebar("friends");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            <script src="/js/user-friends.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.friends.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }

    public static String renderWorldPage(String csrfToken, String userId) {
        String content = pageHeader(I18n.t("user.world.title"), I18n.t("user.world.desc"))
                + PageRenderer.tr("""
            <div class="world-tabs" id="worldTabs">
                <button class="world-tab active" data-type="">{{world.all}}</button>
                <button class="world-tab" data-type="SKIN">{{world.skin}}</button>
                <button class="world-tab" data-type="CAPE">{{world.cape}}</button>
            </div>
            <div class="texture-grid" id="worldTextureGrid"></div>
            <div id="worldLoading" class="world-loading" style="display:none">
                <div class="spinner"></div><span>{{common.loading}}</span>
            </div>
            <div id="worldEnd" class="world-end" style="display:none">{{world.noMore}}</div>
            <div id="toast" class="toast" style="display:none"></div>
            """);

        String siteName = im.xz.cn.config.SystemConfig.getInstance().getSiteName();
        String navbar = """
            <nav class="navbar">
                <div class="navbar-inner">
                    <div class="navbar-brand">
                        <img src="/icons/app.ico" class="logo-icon brand-icon" alt="LingYggdrasil">
                        <span class="brand-text">%s</span>
                    </div>
                    <div class="navbar-links">
                        <a href="/world" class="nav-link active">%s</a>
                        <a href="/dashboard" class="nav-link">%s</a>
                        <a href="/" class="nav-link">%s</a>
                        <button type="button" class="nav-link theme-toggle" data-action="toggleTheme" aria-label="%s" title="%s"><i class="fas fa-moon"></i></button>
                    </div>
                </div>
            </nav>
            """.formatted(esc(siteName), I18n.t("nav.world"), I18n.t("nav.personalCenter"),
                    I18n.t("nav.home"), I18n.t("nav.theme"), I18n.t("nav.theme"));

        String body = navbar + """
            <div class="container">%s</div>
            <script src="/js/user-world.js"></script>
            """.formatted(content);

        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.world.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss(), Css.getWorldCss());
    }

    public static String renderSharedPage(String csrfToken) {
        String content = pageHeader(I18n.t("user.shared.title"), I18n.t("user.shared.desc"))
                + """
            <div class="texture-grid" id="sharedGrid"><p class="text-muted" style="grid-column:1/-1">""" + I18n.t("common.loading") + """
            </p></div>
            <div id="toast" class="toast" style="display:none"></div>
            """;
        String navbar = PageRenderer.renderNavbar(I18n.t("nav.userCenter"), "user", false);
        String sidebar = Shared.buildUserSidebar("shared");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            <script src="/js/user-shared.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.shared.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss(), Css.getWorldCss());
    }
}
