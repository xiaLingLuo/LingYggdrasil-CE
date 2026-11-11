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

    public static String welcomeSection(String displayName) {
        return """
            <div class="welcome-section">
                <h2>%s <i class="fas fa-wand-magic-sparkles"></i></h2>
            </div>
            """.formatted(I18n.t("user.dashboard.welcome", esc(displayName)));
    }

    public static String pageHeader(String title, String desc) {
        return """
            <div class="page-header">
                <h2>%s</h2>
                <p class="page-desc">%s</p>
            </div>
            """.formatted(esc(title), esc(desc));
    }

    
    private static String label(String key, String fallback) {
        String value = I18n.t(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }

    private static String settingsTr(String template) {
        String html = PageRenderer.tr(template);
        return html.replace("{SiteName}", esc(im.xz.cn.config.SystemConfig.getInstance().getSiteName()));
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

    public static String accountInfoContent(String username, String permGroup, String email,
                                            boolean emailVerified, String createdAt,
                                            int profileCount, int skinCount, int capeCount,
                                            int publicCount, int friendCount) {
        String emailBadge = emailVerified
                ? "<span class='badge badge-success'>" + I18n.t("user.dashboard.verified") + "</span>"
                : "<span class='badge badge-warning'>" + I18n.t("user.dashboard.unverified") + "</span>";
        String groupName = (permGroup != null && !permGroup.isBlank()) ? permGroup : "default";
        String groupAlias = I18n.tOrNull("permGroup." + groupName);
        if (groupAlias == null || groupAlias.isBlank()) groupAlias = groupName;
        String groupBadge = "<span class='badge badge-info'>" + esc(groupAlias) + "</span>";
        String date = createdAt != null ? createdAt.substring(0, Math.min(10, createdAt.length())) : "-";
        return """
            <div class="account-info">
                <div class="account-field">
                    <div class="account-field-label">%s</div>
                    <div class="account-field-value">%s %s</div>
                </div>
                <div class="account-field">
                    <div class="account-field-label">%s</div>
                    <div class="account-field-value">%s %s</div>
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
                I18n.t("user.account.username"), esc(username), groupBadge,
                I18n.t("user.account.email"), esc(email), emailBadge,
                I18n.t("user.account.registeredAt"), date,
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

    
    private static String cancelAccountForm() {
        return settingsTr("""
            <p class="form-hint">{{settings.cancelAccountDesc}}</p>
            <div class="inline-form account-cancel-form">
                <input type="password" class="form-input" id="cancelAccountPassword" placeholder="{{user.settings.currentPasswordPlaceholder}}" data-tooltip="{{settings.currentPasswordHint}}">
                <button type="button" class="btn btn-danger" id="cancelAccountBtn">{{settings.cancelAccountButton}}</button>
            </div>
            <div id="cancelAccountMsg" class="msg-area"></div>
            """);
    }

    
    private static String externalLoginForm() {
        return settingsTr("""
            <label style="display:flex;align-items:flex-start;gap:10px;cursor:pointer">
                <input type="checkbox" id="accountPasswordLoginToggle" style="margin-top:3px;width:18px;height:18px">
                <span>
                    <strong>{{settings.externalLoginToggle}}</strong><br>
                    <span class="form-hint">{{settings.externalLoginDesc}}</span>
                </span>
            </label>
            <div id="externalLoginMsg" class="msg-area"></div>
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



    public static String renderDashboardPage(String csrfToken, String siteName,
                                              String displayName, String username, String email,
                                              boolean emailVerified, String createdAt, String permGroup,
                                              String apiDomain, int profileCount,
                                              int skinCount, int capeCount, int publicCount, int friendCount) {
        String welcome = welcomeSection(displayName);
        String guide = yggdrasilGuide(apiDomain);
        String guideSection = PageRenderer.renderCard(I18n.t("user.dashboard.guideTitle"), guide);
        String accountSection = PageRenderer.renderCard(I18n.t("user.dashboard.accountTitle"),
                accountInfoContent(username, permGroup, email, emailVerified, createdAt,
                        profileCount, skinCount, capeCount, publicCount, friendCount));
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
                    + "</div></div>")
                + "<br>" + PageRenderer.renderCard(label("settings.externalLoginCard", "{{settings.externalLoginCard}}"),
                    externalLoginForm())
                + "<br>" + PageRenderer.renderCard(label("settings.cancelAccountCard", "{{settings.cancelAccountCard}}"),
                    cancelAccountForm());
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
        String nameLengthInject = "<script>window.PROFILE_NAME_MIN_LENGTH="
                + im.xz.cn.config.SystemConfig.getInstance().getMinProfileNameLength()
                + ";window.PROFILE_NAME_MAX_LENGTH="
                + im.xz.cn.config.SystemConfig.getInstance().getMaxProfileNameLength() + ";</script>";
        String csrf = Shared.csrfInject(csrfToken)
                + apiDomainInject
                + nameLengthInject
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(I18n.t("user.profiles.title"), csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    
    public static String renderFriendsV2Page(String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>{{friends.title}}</h2>
                <p class="page-desc">{{friends.pageDesc}}</p>
            </div>
            <div id="friendsV2Msg" class="msg-area"></div>
            <section class="friend-section">
                <div class="friend-grid" id="friendsV2List"></div>
            </section>
            <script>
            (function () {
                var myInfo = null;
                var friends = [];
                var requests = [];
                var outgoing = [];
                function headers() {
                    var h = { 'Content-Type': 'application/json' };
                    if (window.CSRF_TOKEN) { h['X-CSRF-Token'] = window.CSRF_TOKEN; }
                    return h;
                }
                function esc(value) {
                    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
                    });
                }
                function msg(text, ok) {
                    if (text && typeof showToast === 'function') showToast(text, ok ? 'success' : 'error');
                    var el = document.getElementById('friendsV2Msg');
                    if (el) { el.textContent = ''; el.className = 'msg-area'; }
                }
                function avatarMarkup(name, url) {
                    var src = url || '/img/juststeve_face.png';
                    return '<div class="friend-card-avatar"><img src="' + esc(src) + '" alt="'
                        + esc(name || 'avatar') + '" style="width:100%;height:100%;object-fit:cover;'
                        + 'object-position:center top;image-rendering:pixelated;"></div>';
                }
                function ownCard() {
                    var name = (myInfo && (myInfo.displayName || myInfo.friendCode)) || '{{friends.myCard}}';
                    var code = (myInfo && (myInfo.friendCodeFormatted || myInfo.friendCode)) || '----';
                    return '<div class="friend-card friend-my-card card-animate" id="friendMyCard">'
                        + avatarMarkup(name, myInfo && myInfo.avatar)
                        + '<div class="friend-card-info"><div class="friend-card-name">' + esc(name) + '</div>'
                        + '<div class="friend-card-code">' + esc(code) + '</div></div>'
                        + '<div class="friend-card-label">{{friends.myCard}}</div></div>';
                }
                function friendCard(user) {
                    var name = user.name || user.code || user.userId || '?';
                    return '<div class="friend-card card-animate">'
                        + avatarMarkup(name, user.avatar)
                        + '<div class="friend-card-info"><div class="friend-card-name">' + esc(name) + '</div>'
                        + '<div class="friend-card-code">' + esc(user.code || user.userId || '') + '</div></div>'
                        + '<div class="friend-card-actions">'
                        + '<a class="btn btn-sm" href="/userhome/' + esc(user.code || '') + '">{{common.view}}</a>'
                        + '<button class="btn btn-sm" data-friend-share="' + esc(user.userId) + '">{{shared.share}}</button>'
                        + '<button class="btn btn-sm" data-friend-block="' + esc(user.userId) + '">{{friends.blockLabel}}</button>'
                        + '<button class="btn btn-sm btn-danger" data-friend-remove="' + esc(user.userId) + '">{{common.delete}}</button>'
                        + '</div></div>';
                }
                function requestCard(request, direction) {
                    var other = (direction === 'outgoing' ? request.toUser : request.fromUser) || {};
                    var name = other.name || other.code || (direction === 'outgoing' ? request.toUserId : request.fromUserId) || '?';
                    var actions = direction === 'outgoing'
                        ? '<button class="btn btn-sm btn-danger" data-request-cancel="' + esc(request.id) + '">{{friends.cancelRequestLabel}}</button>'
                        : '<button class="btn btn-sm btn-primary" data-request-accept="' + esc(request.id) + '">{{friends.accept}}</button>'
                          + '<button class="btn btn-sm" data-request-reject="' + esc(request.id) + '">{{friends.reject}}</button>';
                    return '<div class="friend-card friend-request-card card-animate">'
                        + avatarMarkup(name, other.avatar)
                        + '<div class="friend-card-info"><div class="friend-card-name">' + esc(name) + '</div>'
                        + '<div class="friend-card-code">' + esc(other.code || '') + '</div>'
                        + '<div class="friend-card-label">' + (direction === 'outgoing' ? '{{friends.requestSentLabel}}' : '{{friends.request}}') + '</div></div>'
                        + '<div class="friend-card-actions">' + actions + '</div></div>';
                }
                function addTile() {
                    return '<div class="friend-add-tile card-animate" id="friendAddTile" role="button" tabindex="0">'
                        + '<div class="friend-add-tile-icon"><span class="friend-add-tile-plus">+</span></div>'
                        + '<div class="friend-add-tile-text">{{friends.add}}</div>'
                        + '<div class="friend-add-tile-hint">{{friends.enterCodeShort}}</div></div>';
                }
                function render() {
                    var list = document.getElementById('friendsV2List');
                    if (!list) return;
                    var html = myInfo ? ownCard() : '';
                    friends.forEach(function (user) { html += friendCard(user); });
                    requests.forEach(function (request) { html += requestCard(request, 'incoming'); });
                    outgoing.forEach(function (request) { html += requestCard(request, 'outgoing'); });
                    html += addTile();
                    list.innerHTML = html;
                    var mine = document.getElementById('friendMyCard');
                    if (mine) mine.addEventListener('click', showMyProfileModal);
                    var add = document.getElementById('friendAddTile');
                    if (add) add.addEventListener('click', showAddFriendModal);
                }
                function loadAll() {
                    Promise.all([
                        fetch('/api/v2/friends/my-info', { headers: headers() }).then(function (r) { return r.json(); }),
                        fetch('/api/v2/friends', { headers: headers() }).then(function (r) { return r.json(); }),
                        fetch('/api/v2/friend-requests', { headers: headers() }).then(function (r) { return r.json(); }),
                        fetch('/api/v2/friend-requests?direction=outgoing', { headers: headers() })
                            .then(function (r) { return r.json(); })
                    ]).then(function (results) {
                        myInfo = results[0] && results[0].success ? results[0].info : null;
                        friends = (results[1] && results[1].users) || [];
                        requests = (results[2] && results[2].items) || [];
                        outgoing = (results[3] && results[3].items) || [];
                        render();
                    }).catch(function () { msg('{{friends.loadFailedRetryText}}', false); });
                }
                function showMyProfileModal() {
                    if (!myInfo) return;
                    var existing = document.getElementById('myProfileModal');
                    if (existing) existing.remove();
                    var profiles = myInfo.profiles || [];
                    var options = '<option value="">{{texture.hidden}}</option>';
                    profiles.forEach(function (profile) {
                        options += '<option value="' + esc(profile.id) + '"'
                            + (myInfo.displayProfileId === profile.id ? ' selected' : '') + '>'
                            + esc(profile.name) + '</option>';
                    });
                    var overlay = document.createElement('div');
                    overlay.className = 'modal-overlay';
                    overlay.id = 'myProfileModal';
                    var box = document.createElement('div');
                    box.className = 'modal-box';
                    var friendCode = myInfo.friendCodeFormatted || myInfo.friendCode || '----';
                    box.innerHTML = '<button class="modal-close-btn" id="myProfileClose">&times;</button>'
                        + '<h3>{{friends.myCard}}</h3>'
                        + '<div class="form-group"><label class="form-label">{{friends.code}}</label>'
                        + '<div class="friend-code-display"><span id="myProfileCode">' + esc(friendCode) + '</span>'
                        + '<button type="button" class="btn btn-secondary btn-small" id="myProfileCopy" '
                        + 'data-tooltip="{{friends.copyCode}}" aria-label="{{friends.copyCode}}"><i class="fas fa-copy"></i></button></div></div>'
                        + '<div class="form-group"><label class="form-label">{{texture.displayProfile}}</label>'
                        + '<select class="form-input" id="myProfileSelect">' + options + '</select></div>'
                        + '<div id="myProfileMsg" class="msg-area"></div>'
                        + '<div class="modal-actions"><button class="btn btn-secondary" id="myProfileCancel">{{common.cancel}}</button>'
                        + '<button class="btn btn-primary" id="myProfileSave">{{common.save}}</button></div>';
                    overlay.appendChild(box);
                    document.body.appendChild(overlay);
                    function close() { overlay.remove(); }
                    box.querySelector('#myProfileClose').addEventListener('click', close);
                    box.querySelector('#myProfileCancel').addEventListener('click', close);
                    overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
                    box.querySelector('#myProfileCopy').addEventListener('click', function () {
                        var text = myInfo.friendCodeFormatted || myInfo.friendCode || '';
                        function copied() { showToast('{{friends.codeCopied}}', 'success'); }
                        function fallback() {
                            var input = document.createElement('textarea');
                            input.value = text;
                            input.style.position = 'fixed';
                            input.style.opacity = '0';
                            document.body.appendChild(input);
                            input.select();
                            try { document.execCommand('copy'); copied(); }
                            catch (error) { showToast('{{friends.copyFailedManual}}', 'error'); }
                            input.remove();
                        }
                        if (navigator.clipboard && window.isSecureContext) {
                            navigator.clipboard.writeText(text).then(copied).catch(fallback);
                        } else {
                            fallback();
                        }
                    });
                    box.querySelector('#myProfileSave').addEventListener('click', function () {
                        var value = box.querySelector('#myProfileSelect').value;
                        fetch('/api/v2/friends/display-profile', {
                            method: 'POST', headers: headers(),
                            body: JSON.stringify({ profileId: value })
                        }).then(function (r) { return r.json(); }).then(function (data) {
                            if (data.success) { close(); loadAll(); }
                            else { box.querySelector('#myProfileMsg').textContent = data.message || '{{common.saveFailed}}'; }
                        }).catch(function () { box.querySelector('#myProfileMsg').textContent = '{{common.networkErrorShort}}'; });
                    });
                }
                function showAddFriendModal() {
                    var existing = document.getElementById('friendAddModal');
                    if (existing) existing.remove();
                    var overlay = document.createElement('div');
                    overlay.className = 'modal-overlay';
                    overlay.id = 'friendAddModal';
                    var box = document.createElement('div');
                    box.className = 'modal-box';
                    box.innerHTML = '<button class="modal-close-btn" id="friendAddClose">&times;</button>'
                        + '<h3>{{friends.add}}</h3>'
                        + '<p class="form-hint">{{friends.addHintText}}</p>'
                        + '<div class="form-group"><label class="form-label">{{friends.code}}</label>'
                        + '<input class="form-input" id="friendCodeInput" placeholder="{{common.example}} ABCD-EFGH-IJKL"></div>'
                        + '<div id="friendAddMsg" class="msg-area"></div>'
                        + '<div class="modal-actions"><button class="btn btn-secondary" id="friendAddCancel">{{common.cancel}}</button>'
                        + '<button class="btn btn-primary" id="friendAddConfirm">{{friends.sendRequest}}</button></div>';
                    overlay.appendChild(box);
                    document.body.appendChild(overlay);
                    function close() { overlay.remove(); }
                    box.querySelector('#friendAddClose').addEventListener('click', close);
                    box.querySelector('#friendAddCancel').addEventListener('click', close);
                    overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
                    box.querySelector('#friendAddConfirm').addEventListener('click', function () {
                        var code = (box.querySelector('#friendCodeInput').value || '').trim();
                        var msgBox = box.querySelector('#friendAddMsg');
                        if (!code) { if (window.showToast) showToast('{{friends.enterCode}}', 'warning'); if (msgBox) msgBox.textContent = ''; return; }
                        fetch('/api/userhomes/' + encodeURIComponent(code), { headers: headers() })
                            .then(function (r) { return r.json(); })
                            .then(function (data) {
                                if (!data || !data.userId) throw new Error('{{friends.codeNotFound}}');
                                return fetch('/api/v2/friend-requests', {
                                    method: 'POST', headers: headers(), body: JSON.stringify({ toUserId: data.userId })
                                });
                            })
                            .then(function (r) { return r.json(); })
                            .then(function (data) {
                                if (data && data.success) { close(); loadAll(); }
                                else { if (window.showToast) showToast(data.message || '{{common.sendFailed}}', 'error'); if (msgBox) msgBox.textContent = ''; }
                            })
                            .catch(function (error) {
                                if (window.showToast) showToast(error.message || '{{common.sendFailed}}', 'error');
                                if (msgBox) msgBox.textContent = '';
                            });
                    });
                    box.querySelector('#friendCodeInput').focus();
                }
                function removeFriend(anchor, userId) {
                    showPopconfirm(anchor, '{{friends.deleteShortConfirm}}', function () {
                        fetch('/api/v2/friends/' + encodeURIComponent(userId), { method: 'DELETE', headers: headers() })
                            .then(function (r) { return r.json(); }).then(function (data) {
                                msg(data.success ? '{{msg.friendDeleted}}' : (data.message || '{{common.deleteFailed}}'), data.success);
                                loadAll();
                            }).catch(function () { msg('{{texture.deleteRequestFailed}}', false); });
                    }, { confirmText: '{{friends.deleteFriend}}' });
                }
                function shareTexture(userId) {
                    fetch('/api/v2/textures/my', { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var items = (data.skins || []).concat(data.capes || []);
                            if (!items.length) { msg('{{friends.noShareable}}', false); return; }
                            var names = items.map(function (item, index) {
                                return (index + 1) + '. ' + (item.alias || item.displayName || item.id);
                            }).join('\\n');
                            showPromptDialog({
                                title: '{{shared.shareTitle}}', message: '{{shared.sharePrompt}}',
                                detail: names, value: '1'
                            }, function (choice) {
                                if (!choice) return false;
                                var item = items[Number(choice) - 1];
                                if (!item) { msg('{{msg.invalidTextureId}}', false); return false; }
                                fetch('/api/v2/shares', {
                                    method: 'POST', headers: headers(),
                                    body: JSON.stringify({ assetId: item.id, recipientUserId: userId })
                                }).then(function (r) { return r.json(); }).then(function (d) {
                                    msg(d.success ? '{{shared.shared}}' : ('{{shared.shareFailed}}' + (d.message || '')), d.success);
                                });
                                return true;
                            });
                        });
                }
                function blockUser(userId) {
                    showPopconfirm(document.body, '{{friends.blockConfirmCurrent}}', function () {
                        fetch('/api/v2/blocks', {
                            method: 'POST', headers: headers(),
                            body: JSON.stringify({ toUserId: userId })
                        }).then(function (r) { return r.json(); }).then(function (data) {
                            msg(data.success ? '{{friends.blockedLabel}}' : (data.message || '{{common.operationFailed}}'), data.success);
                            loadAll();
                        });
                    }, { confirmText: '{{friends.blockLabel}}' });
                }
                function request(action, id, method) {
                    var url = method === 'DELETE'
                        ? '/api/v2/friend-requests/' + encodeURIComponent(id)
                        : '/api/v2/friend-requests/' + encodeURIComponent(id) + '/' + action;
                    fetch(url, {
                        method: method || 'POST', headers: headers(), body: '{}'
                    }).then(function (r) { return r.json(); }).then(function (data) {
                        msg(data.success ? '{{common.success}}' : (data.message || '{{common.operationFailed}}'), data.success);
                        loadAll();
                    }).catch(function () { msg('{{common.requestFailed}}', false); });
                }
                document.addEventListener('click', function (event) {
                    var target = event.target;
                    var accept = target.getAttribute('data-request-accept');
                    if (accept) { request('accept', accept); return; }
                    var reject = target.getAttribute('data-request-reject');
                    if (reject) { request('reject', reject); return; }
                    var cancelRequest = target.getAttribute('data-request-cancel');
                    if (cancelRequest) { request('cancel', cancelRequest, 'DELETE'); return; }
                    var share = target.getAttribute('data-friend-share');
                    if (share) { shareTexture(share); return; }
                    var blockId = target.getAttribute('data-friend-block');
                    if (blockId) { blockUser(blockId); return; }
                    var remove = target.getAttribute('data-friend-remove');
                    if (remove) { removeFriend(target, remove); return; }
                });
                loadAll();
            })();
            </script>
            """;
        String navbar = PageRenderer.renderNavbar(im.xz.cn.config.SystemConfig.getInstance().getSiteName(), "user", false);
        String sidebar = Shared.buildUserSidebar("friends");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            """.formatted(sidebar, content);
        return PageRenderer.renderPage("{{friends.title}}", Shared.csrfInject(csrfToken) + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }
    
    public static String renderSharedV2Page(String csrfToken) {
        String content = pageHeader(I18n.t("user.shared.title"), I18n.t("user.shared.desc"))
                + """
            <section class="shared-section">
                <h3 class="shared-section-title"><i class="fas fa-star"></i> {{shared.myFavorites}}</h3>
                <div class="texture-grid" id="sharedFavorites"><p class="empty-hint" style="grid-column:1/-1">{{common.loadingShort}}...</p></div>
            </section>
            <section class="shared-section">
                <h3 class="shared-section-title"><i class="fas fa-inbox"></i> {{shared.receivedTitle}}</h3>
                <div class="texture-grid" id="sharedIncoming"><p class="empty-hint" style="grid-column:1/-1">{{common.loadingShort}}...</p></div>
            </section>
            <section class="shared-section">
                <h3 class="shared-section-title"><i class="fas fa-paper-plane"></i> {{shared.myShared}}</h3>
                <div class="texture-grid" id="sharedOutgoing"><p class="empty-hint" style="grid-column:1/-1">{{common.loadingShort}}...</p></div>
            </section>
            <div id="sharedV2Msg" class="msg-area"></div>
            <script>
            (function () {
                function headers() {
                    var h = { 'Content-Type': 'application/json' };
                    if (window.CSRF_TOKEN) { h['X-CSRF-Token'] = window.CSRF_TOKEN; }
                    return h;
                }
                function esc(value) {
                    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
                    });
                }
                function msg(text, ok) {
                    if (text && typeof showToast === 'function') showToast(text, ok ? 'success' : 'error');
                    var el = document.getElementById('sharedV2Msg');
                    if (el) { el.textContent = ''; el.className = 'msg-area'; }
                }
                function applyTexture(assetId, slot) {
                    fetch('/api/profiles', { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var profiles = data.profiles || [];
                            if (!profiles.length) { msg('{{msg.createProfileFirst}}', false); return; }
                            var names = profiles.map(function (p) { return p.name; }).join('、');
                            showPromptDialog({
                                title: '{{texture.applyToProfile}}', message: '{{texture.applyPrompt}}',
                                detail: '{{texture.selectProfileDetail}}' + names, value: profiles[0].name
                            }, function (name) {
                                if (!name) return false;
                                var profile = profiles.filter(function (p) { return p.name === name; })[0];
                                if (!profile) { msg('{{msg.profileNotExist}}', false); return false; }
                                fetch('/api/profiles/' + encodeURIComponent(profile.id) + '/textures/' + slot, {
                                    method: 'PUT', headers: headers(),
                                    body: JSON.stringify({ assetId: assetId, skinModel: profile.skinModel || 'DEFAULT' })
                                }).then(function (r) { return r.json(); }).then(function (d) {
                                    msg(d.success ? '{{texture.applied}}' : ('{{texture.applyFailedDetail}}' + (d.message || '')), d.success);
                                });
                                return true;
                            });
                        });
                }
                function renameTexture(assetId) {
                    showPromptDialog({ title: '{{common.rename}}', value: '' }, function (alias) {
                        if (!alias || !alias.trim()) { showToast('{{common.nameRequired}}', 'error'); return false; }
                        fetch('/api/v2/textures/' + encodeURIComponent(assetId) + '/alias', {
                            method: 'POST', headers: headers(), body: JSON.stringify({ alias: alias })
                        }).then(function (r) { return r.json(); }).then(function (d) {
                            msg(d.success ? '{{common.rename}}成功' : ('{{common.rename}}失败：' + (d.message || '')), d.success);
                            loadAll();
                        });
                        return true;
                    });
                }
                function card(item, direction) {
                    var name = esc(item.alias || item.displayName
                        || ((item.type === 'CAPE' ? '{{texture.cape}}' : '{{texture.skin}}') + ' · ' + esc(item.width) + 'x' + esc(item.height)));
                    var slot = item.type === 'CAPE' ? 'CAPE' : 'SKIN';
                    var counterpart = esc(item.counterpartName || '{{friends.title}}');
                    var badge = direction === 'incoming' ? ('{{common.from}} ' + counterpart)
                        : (direction === 'favorite' ? '{{shared.myFavorites}}' : ('{{common.shareTo}} ' + counterpart));
                    var actions = '';
                    if (direction !== 'outgoing') {
                        actions += '<button class="btn btn-sm btn-secondary" data-apply="' + esc(item.assetId || item.id)
                            + '" data-slot="' + slot + '">{{texture.applyToProfile}}</button>'
                            + '<button class="btn btn-sm btn-secondary" data-alias="' + esc(item.assetId || item.id) + '">{{common.rename}}</button>';
                    }
                    if (direction === 'favorite') {
                        actions += '<button class="btn btn-sm btn-danger" data-unfavorite="'
                            + esc(item.publicationId) + '">{{shared.unfavorite}}</button>';
                    } else if (direction === 'incoming') {
                        actions += '<button class="btn btn-sm btn-danger" data-share-revoke="' + esc(item.id) + '">{{shared.revoke}}</button>';
                    } else {
                        actions += '<button class="btn btn-sm btn-danger" data-share-revoke="' + esc(item.id) + '">{{shared.revoke}}</button>';
                    }
                    var cardEl = document.createElement('div');
                    cardEl.className = 'texture-item card-animate';
                    cardEl.innerHTML = '<div class="texture-thumb"><img src="' + esc(item.url) + '" alt="shared texture"></div>'
                        + '<div class="texture-item-body">'
                        + '<div class="texture-name">' + name + '</div>'
                        + '<div class="texture-meta">' + (item.available === false ? '{{common.unavailable}}' : '{{common.available}}')
                        + ' · ' + esc(item.width) + 'x' + esc(item.height) + '</div>'
                        + '</div>'
                        + '<span class="shared-badge shared-badge-' + direction + '">' + badge + '</span>'
                        + '<div class="shared-card-actions">' + actions + '</div>';
                    return cardEl;
                }
                function loadDirection(direction, targetId, emptyText) {
                    fetch('/api/v2/shares?direction=' + direction, { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var list = document.getElementById(targetId);
                            list.innerHTML = '';
                            (data.items || []).forEach(function (item) {
                                list.appendChild(card(item, direction === 'received' ? 'incoming' : 'outgoing'));
                            });
                            if (!list.children.length) {
                                list.innerHTML = '<p class="empty-hint" style="grid-column:1/-1">' + emptyText + '</p>';
                            }
                        })
                        .catch(function () {
                            document.getElementById(targetId).innerHTML = '<p class="empty-hint" style="grid-column:1/-1;color:#c62828">{{common.loadFailedShort}}</p>';
                        });
                }
                function loadFavorites() {
                    fetch('/api/v2/textures/my', { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var list = document.getElementById('sharedFavorites');
                            list.innerHTML = '';
                            (data.favoriteSkins || []).concat(data.favoriteCapes || []).forEach(function (item) {
                                list.appendChild(card(item, 'favorite'));
                            });
                            if (!list.children.length) {
                                list.innerHTML = '<p class="empty-hint" style="grid-column:1/-1">{{shared.emptyFavorites}}</p>';
                            }
                        })
                        .catch(function () {
                            document.getElementById('sharedFavorites').innerHTML =
                                '<p class="empty-hint" style="grid-column:1/-1;color:#c62828">{{common.loadFailedShort}}</p>';
                        });
                }
                function loadAll() {
                    loadDirection('received', 'sharedIncoming', '{{shared.emptyIncomingShort}}');
                    loadDirection('sent', 'sharedOutgoing', '{{shared.emptyOutgoingShort}}');
                    loadFavorites();
                }
                document.addEventListener('click', function (event) {
                    var apply = event.target.getAttribute('data-apply');
                    if (apply) { applyTexture(Number(apply), event.target.getAttribute('data-slot') || 'SKIN'); return; }
                    var alias = event.target.getAttribute('data-alias');
                    if (alias) { renameTexture(Number(alias)); return; }
                    var unfavorite = event.target.getAttribute('data-unfavorite');
                    if (unfavorite) {
                        fetch('/api/v2/publications/' + encodeURIComponent(unfavorite) + '/favorite', {
                            method: 'DELETE', headers: headers()
                        }).then(function (r) { return r.json(); }).then(function (d) {
                            msg(d.success ? '{{shared.favRemoved}}' : ('{{common.operationFailedDetail}}' + (d.message || '')), d.success);
                            loadFavorites();
                        });
                        return;
                    }
                    var id = event.target.getAttribute('data-share-revoke');
                    if (!id) return;
                    showPopconfirm(event.target, '{{shared.revokeConfirm}}', function () {
                        fetch('/api/v2/shares/' + encodeURIComponent(id), { method: 'DELETE', headers: headers() })
                            .then(function (r) { return r.json(); }).then(function (data) {
                                msg(data.success ? '{{shared.revoked}}' : ('{{shared.revokeFailed}}' + (data.message || '')), data.success);
                                loadAll();
                            }).catch(function () { msg('{{common.requestFailed}}', false); });
                    }, { confirmText: '{{shared.revoke}}' });
                });
                loadAll();
            })();
            </script>
            """;
        String navbar = PageRenderer.renderNavbar(im.xz.cn.config.SystemConfig.getInstance().getSiteName(), "user", false);
        String sidebar = Shared.buildUserSidebar("shared");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            """.formatted(sidebar, content);
        return PageRenderer.renderPage(I18n.t("user.shared.title"), Shared.csrfInject(csrfToken) + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }
    
    
    public static String renderWorldV2Page(String csrfToken) {
        String content = pageHeader("{{world.title}}", "{{world.browseHint}}")
                + """
            <div class="world-toolbar">
                <div class="world-tabs" role="tablist">
                    <button type="button" class="btn btn-secondary active" data-world-type="ALL">{{common.all}}</button>
                    <button type="button" class="btn btn-secondary" data-world-type="SKIN">{{texture.skin}}</button>
                    <button type="button" class="btn btn-secondary" data-world-type="CAPE">{{texture.cape}}</button>
                    <button type="button" class="btn btn-secondary" data-world-lowend>{{world.lowEnd}}</button>
                </div>
                <div class="world-sort">
                    <button type="button" class="btn btn-secondary active" data-world-sort="hot">{{common.hot}}</button>
                    <button type="button" class="btn btn-secondary" data-world-sort="new">{{common.latest}}</button>
                </div>
                <span class="world-total" id="worldTotal"></span>
            </div>
            <div class="texture-grid" id="worldV2List"></div>
            <div class="world-more"><button type="button" class="btn btn-secondary" id="worldMore">{{common.loadMore}}</button></div>
            <script src="/js/skinview3d.bundle.js"></script>
            <script src="/js/texture-3d.js"></script>
            <script>
            (function () {
                var state = { type: 'ALL', sort: 'hot', offset: 0, limit: 20, done: false, loading: false };
                function esc(value) {
                    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
                    });
                }
                function detailUrl(item) {
                    return '/userhome/' + encodeURIComponent(item.publisherCode || '')
                        + '/public/' + encodeURIComponent(item.hash || '')
                        + '?type=' + encodeURIComponent(item.type || 'SKIN');
                }
                function load(reset) {
                    if (state.loading) return;
                    if (reset) { state.offset = 0; state.done = false; }
                    if (state.done) return;
                    state.loading = true;
                    var query = '?limit=' + state.limit + '&offset=' + state.offset
                        + '&sort=' + encodeURIComponent(state.sort)
                        + (state.type === 'ALL' ? '' : '&type=' + encodeURIComponent(state.type));
                    fetch('/api/v2/publications' + query)
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var list = document.getElementById('worldV2List');
                            if (reset) { if (window.Texture3D) window.Texture3D.dispose(list); list.innerHTML = ''; }
                            var items = data.items || [];
                            items.forEach(function (item) {
                                var name = item.displayName || (item.type === 'CAPE' ? '{{texture.cape}}' : '{{texture.skin}}');
                                var card = document.createElement('a');
                                card.className = 'texture-item card-animate texture-card-clickable';
                                card.href = detailUrl(item);
                                card.innerHTML = '<div class="texture-thumb">'
                                    + '<canvas data-texture-3d data-type="' + esc(item.type)
                                    + '" data-url="' + esc(item.url) + '"></canvas>'
                                    + '</div>'
                                    + '<div class="texture-item-body">'
                                    + '<div class="texture-name">' + esc(name) + '</div>'
                                    + '<div class="texture-meta">' + esc(item.type === 'CAPE' ? '{{texture.cape}}' : '{{texture.skin}}')
                                    + ' · ' + esc(item.width) + '×' + esc(item.height) + '</div>'
                                    + '<div class="texture-meta">♥ ' + esc(item.likes || 0)
                                    + ' · ★ ' + esc(item.favorites || 0) + '</div>'
                                    + '</div>';
                                list.appendChild(card);
                            });
                            if (window.Texture3D) window.Texture3D.hydrate(list);
                            document.getElementById('worldTotal').textContent =
                                '{{world.total}}'.replace('{0}', esc(data.total == null ? items.length : data.total));
                            state.offset += items.length;
                            state.done = items.length < state.limit;
                            document.getElementById('worldMore').style.display = state.done ? 'none' : '';
                            if (!list.children.length) {
                                list.innerHTML = '<p class="empty-hint" style="grid-column:1/-1">{{world.emptyShort}}</p>';
                            }
                        })
                        .catch(function () {
                            var list = document.getElementById('worldV2List');
                            if (list) list.innerHTML = '<p class="empty-hint" style="grid-column:1/-1;color:#c62828">{{common.loadFailedShort}}</p>';
                        })
                        .finally(function () { state.loading = false; });
                }
                document.querySelectorAll('[data-world-type]').forEach(function (button) {
                    button.addEventListener('click', function () {
                        document.querySelectorAll('[data-world-type]').forEach(function (b) { b.classList.remove('active'); });
                        button.classList.add('active');
                        state.type = button.getAttribute('data-world-type');
                        load(true);
                    });
                });
                if (window.Texture3D) {
                    window.Texture3D.bindToggle(document.querySelector('[data-world-lowend]'), function () {
                        load(true);
                    });
                }
                document.querySelectorAll('[data-world-sort]').forEach(function (button) {
                    button.addEventListener('click', function () {
                        document.querySelectorAll('[data-world-sort]').forEach(function (b) { b.classList.remove('active'); });
                        button.classList.add('active');
                        state.sort = button.getAttribute('data-world-sort');
                        load(true);
                    });
                });
                document.getElementById('worldMore').addEventListener('click', function () { load(false); });
                load(true);
            })();
            </script>
            """;
        String navbar = PageRenderer.renderNavbar(
                im.xz.cn.config.SystemConfig.getInstance().getSiteName(), "world", false);
        String body = navbar + "<div class=\"public-page\"><div class=\"public-library-page\">" + content + "</div></div>";
        return PageRenderer.renderPage("{{world.title}}", Shared.csrfInject(csrfToken) + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }


    
    public static String renderTexturesV2Page(String csrfToken, String type) {
        boolean cape = "CAPE".equals(type);
        String pageTitle = cape ? I18n.t("user.capes.title") : I18n.t("user.skins.title");
        String pageDesc = cape ? I18n.t("user.capes.desc") : I18n.t("user.skins.desc");
        String uploadText = cape ? I18n.t("texture.uploadCape") : I18n.t("texture.uploadSkin");
        String uploadTile = """
            <input type="file" id="v2TextureFile" accept="image/png" style="display:none">
            <div class="texture-grid" id="v2TextureGrid">
                <div class="upload-tile card-animate" data-action="v2Upload" role="button" tabindex="0" data-tooltip="{{texture.uploadTooltip}}">
                    <div class="upload-tile-icon">
                        <svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                    </div>
                    <div class="upload-tile-text">%s</div>
                    <div class="upload-tile-hint">%s</div>
                </div>
            </div>
            <div id="v2TextureMsg" class="msg-area"></div>
            """.formatted(uploadText, I18n.t("texture.pngHint"));
        String content = pageHeader(pageTitle, pageDesc) + uploadTile
                + """
            <script>
            (function () {
                var TYPE = '__TYPE__';
                function headers(extra) {
                    var h = { 'Content-Type': 'application/json' };
                    if (window.CSRF_TOKEN) { h['X-CSRF-Token'] = window.CSRF_TOKEN; }
                    if (extra) { for (var k in extra) { h[k] = extra[k]; } }
                    return h;
                }
                function msg(text, ok) {
                    if (text && typeof showToast === 'function') showToast(text, ok ? 'success' : 'error');
                    var el = document.getElementById('v2TextureMsg');
                    if (el) { el.textContent = ''; el.className = 'msg-area'; }
                }
                function esc(value) {
                    return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
                    });
                }
                function load() {
                    fetch('/api/v2/textures', { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var grid = document.getElementById('v2TextureGrid');
                            var uploadTile = grid.querySelector('.upload-tile');
                            grid.querySelectorAll('.texture-item, .empty-hint').forEach(function (el) { el.remove(); });
                            var items = (data.items || []).filter(function (item) { return item.type === TYPE; });
                            items.forEach(function (item) {
                                var name = item.alias || item.displayName || item.originalName || item.hash;
                                var card = document.createElement('div');
                                card.className = 'texture-item card-animate texture-card-clickable';
                                card.innerHTML = '<div class="texture-thumb"><canvas></canvas></div>'
                                    + '<div class="texture-item-body">'
                                    + '<div class="texture-name">\u273F ' + esc(name) + '</div>'
                                    + '<div class="texture-meta">' + esc(formatSize(item.size)) + '</div>'
                                    + '</div>'
                                    + '<span style="position:absolute;top:8px;right:8px;font-size:10px;padding:1px 6px;border-radius:8px;border:1px solid '
                                    + (item.isPublic ? '#0bda51' : '#5897fb') + ';color:' + (item.isPublic ? '#0bda51' : '#5897fb') + '">'
                                    + (item.isPublic ? '{{texture.publicLabel}}' : '{{texture.private}}') + '</span>';
                                card.addEventListener('click', function () { showTextureDetail(item); });
                                grid.appendChild(card);
                                if (TYPE === 'SKIN') drawFaceCanvas(card.querySelector('canvas'), item.url);
                                else drawCapeThumb(card.querySelector('canvas'), item.url);
                            });

                            if (uploadTile) grid.appendChild(uploadTile);
                        })
                        .catch(function () { msg('{{common.loadFailedShort}}', false); });
                }
                function showTextureDetail(item) {
                    var old = document.getElementById('v2TextureDetail');
                    if (old) old.remove();
                    var overlay = document.createElement('div');
                    overlay.className = 'modal-overlay';
                    overlay.id = 'v2TextureDetail';
                    var box = document.createElement('div');
                    box.className = 'modal-box texture-detail-box';
                    var name = item.alias || item.displayName || item.originalName || item.hash;
                    var sizeText = typeof formatSize === 'function' ? formatSize(item.size) : '';
                    box.innerHTML = '<button class="modal-close-btn" id="v2DetailClose">&times;</button>'
                        + '<div class="detail-alias">\u273F ' + esc(name) + '</div>'
                        + '<div class="detail-meta">' + esc(item.originalName || item.hash)
                        + (sizeText ? ' &middot; ' + esc(sizeText) : '') + '</div>'
                        + '<div class="detail-preview"><canvas id="v2Detail3d"></canvas></div>'
                        + '<div class="detail-actions" id="v2DetailActions"></div>';
                    overlay.appendChild(box);
                    document.body.appendChild(overlay);

                    var viewer = null;
                    if (typeof skinview3d !== 'undefined') {
                        try {
                            var canvas = box.querySelector('#v2Detail3d');
                            var preview = canvas.parentElement;
                            var config = {
                                canvas: canvas,
                                width: preview.clientWidth || 300,
                                height: 320,
                                model: item.skinModel || 'default'
                            };
                            if (item.type === 'CAPE') config.cape = item.url;
                            else config.skin = item.url;
                            viewer = new skinview3d.SkinViewer(config);
                            viewer.autoRotate = true;
                            viewer.animation = new skinview3d.WalkingAnimation();
                        } catch (error) {
                            viewer = null;
                        }
                    }
                    if (!viewer) {
                        var previewBox = box.querySelector('.detail-preview');
                        previewBox.innerHTML = '<img src="' + esc(item.url) + '" alt="texture">';
                    }

                    var actions = box.querySelector('#v2DetailActions');
                    var download = document.createElement('button');
                    download.className = 'btn btn-secondary';
                    download.textContent = '{{texture.download}}';
                    download.addEventListener('click', function () { window.open(item.url, '_blank', 'noopener'); });
                    actions.appendChild(download);

                    var apply = document.createElement('button');
                    apply.className = 'btn btn-primary';
                    apply.textContent = '{{texture.applyToProfile}}';
                    apply.addEventListener('click', function () { close(); applyTexture(item.assetId); });
                    actions.appendChild(apply);

                    var aliasButton = document.createElement('button');
                    aliasButton.className = 'btn btn-secondary';
                    aliasButton.textContent = '{{common.rename}}';
                    aliasButton.addEventListener('click', function () { close(); alias(item.assetId); });
                    actions.appendChild(aliasButton);

                    var deleteButton = document.createElement('button');
                    deleteButton.className = 'btn btn-danger';
                    deleteButton.textContent = '{{common.delete}}';
                    deleteButton.addEventListener('click', function () {
                        showPopconfirm(deleteButton, '{{texture.deleteShortConfirm}}', function () {
                            fetch('/api/v2/textures/' + encodeURIComponent(item.assetId), { method: 'DELETE', headers: headers() })
                                .then(function (r) { return r.json(); }).then(function (data) {
                                    msg(data.success ? '{{common.deleted}}' : (data.message || '{{common.deleteFailed}}'), data.success);
                                    close();
                                    load();
                                }).catch(function () { msg('{{texture.deleteRequestFailed}}', false); });
                        }, { confirmText: '{{texture.deleteTexture}}' });
                    });
                    actions.appendChild(deleteButton);

                    var segment = document.createElement('div');
                    segment.className = 'visibility-segment';
                    var privateButton = document.createElement('button');
                    privateButton.type = 'button';
                    privateButton.className = 'vis-option' + (item.isPublic ? '' : ' active');
                    privateButton.textContent = '{{texture.private}}';
                    var publicButton = document.createElement('button');
                    publicButton.type = 'button';
                    publicButton.className = 'vis-option' + (item.isPublic ? ' active' : '');
                    publicButton.textContent = '{{texture.publicLabel}}';
                    function setVisibility(isPublic) {
                        fetch('/api/v2/textures/' + encodeURIComponent(item.assetId) + '/visibility', {
                            method: 'POST', headers: headers(), body: JSON.stringify({ isPublic: isPublic })
                        }).then(function (r) { return r.json(); }).then(function (data) {
                            if (!data.success) { msg(data.message || '{{texture.visibilityUpdateFailed}}', false); return; }
                            item.isPublic = isPublic;
                            privateButton.className = 'vis-option' + (isPublic ? '' : ' active');
                            publicButton.className = 'vis-option' + (isPublic ? ' active' : '');
                            msg('{{texture.visibilityUpdated}}', true);
                            load();
                        }).catch(function () { msg('可见性{{texture.updateRequestFailed}}', false); });
                    }
                    privateButton.addEventListener('click', function () { setVisibility(false); });
                    publicButton.addEventListener('click', function () { setVisibility(true); });
                    segment.appendChild(privateButton);
                    segment.appendChild(publicButton);
                    actions.appendChild(segment);

                    function close() {
                        if (viewer && typeof viewer.dispose === 'function') viewer.dispose();
                        overlay.remove();
                    }
                    box.querySelector('#v2DetailClose').addEventListener('click', close);
                    overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
                }
                var pendingFile = null;
                function openUploadPicker() {
                    var input = document.getElementById('v2TextureFile');
                    if (input) input.click();
                }
                function showToastSafe(text, ok) {
                    if (typeof showToast === 'function') showToast(text, ok ? 'success' : 'error');
                    else msg(text, ok);
                }
                function closeUploadModal() {
                    var modal = document.getElementById('v2UploadModal');
                    if (modal) modal.remove();
                    pendingFile = null;
                    var input = document.getElementById('v2TextureFile');
                    if (input) input.value = '';
                }
                function showUploadModal(file) {
                    if (!file) return;
                    if (file.type !== 'image/png' && !/\\.png$/i.test(file.name || '')) { showAlertDialog({ type: 'warning', title: '{{texture.invalidFormat}}', message: '{{texture.pngOnlyMessage}}' }); return; }
                    closeUploadModal();
                    pendingFile = file;
                    var overlay = document.createElement('div');
                    overlay.className = 'modal-overlay';
                    overlay.id = 'v2UploadModal';
                    var box = document.createElement('div');
                    box.className = 'modal-box';
                    box.innerHTML = '<button class="modal-close-btn" id="v2UploadClose">&times;</button>'
                        + '<h3>' + (TYPE === 'CAPE' ? '{{texture.uploadCape}}' : '{{texture.uploadSkin}}') + '</h3>'
                        + '<div class="upload-modal-body">'
                        + '<div class="upload-modal-preview"><img class="upload-modal-thumb" alt="preview">'
                        + '<div class="upload-modal-info"><div class="upload-modal-filename">' + esc(file.name) + '</div>'
                        + '<div class="upload-modal-filesize">' + Math.max(1, Math.round(file.size / 1024)) + ' KiB</div></div></div>'
                        + '<div class="form-group" style="text-align:left"><label class="form-label">{{texture.aliasOptionalLabel}}</label>'
                        + '<input type="text" class="form-input" id="v2UploadAlias" maxlength="64" placeholder="{{texture.aliasPlaceholderShort}}"></div>'
                        + '<div class="form-group" style="text-align:left"><label style="display:flex;gap:8px;align-items:center;cursor:pointer">'
                        + '<input type="checkbox" id="v2UploadPublic"> {{texture.publicToLibrary}}</label></div>'
                        + '<div id="v2UploadMsg" class="msg-area"></div>'
                        + '<div class="modal-actions"><button class="btn btn-secondary" id="v2UploadCancel">{{common.cancel}}</button>'
                        + '<button class="btn btn-primary" id="v2UploadConfirm">{{texture.upload}}</button></div></div>';
                    var preview = new FileReader();
                    preview.onload = function (event) {
                        var image = box.querySelector('.upload-modal-thumb');
                        if (image) image.src = event.target.result;
                    };
                    preview.readAsDataURL(file);
                    overlay.appendChild(box);
                    document.body.appendChild(overlay);
                    box.querySelector('#v2UploadClose').addEventListener('click', closeUploadModal);
                    box.querySelector('#v2UploadCancel').addEventListener('click', closeUploadModal);
                    overlay.addEventListener('click', function (event) { if (event.target === overlay) closeUploadModal(); });
                    box.querySelector('#v2UploadConfirm').addEventListener('click', function () {
                        uploadFile(box, function () { load(); });
                    });
                    box.querySelector('#v2UploadAlias').focus();
                }
                async function uploadFile(box, done) {
                    if (!pendingFile) return;
                    var file = pendingFile;
                    var button = box.querySelector('#v2UploadConfirm');
                    var message = box.querySelector('#v2UploadMsg');
                    button.disabled = true;
                    button.textContent = '{{common.uploading}}...';
                    try {
                        var bytes = await file.arrayBuffer();
                        var result = await fetch('/api/v2/textures?type=' + encodeURIComponent(TYPE)
                            + '&name=' + encodeURIComponent(file.name || 'texture.png'), {
                            method: 'POST',
                            headers: headers({ 'Content-Type': 'image/png' }),
                            body: bytes
                        }).then(function (r) { return r.json(); });
                        if (!result.success) throw new Error(result.message || '{{texture.uploadFailed}}');
                        var warnings = [];
                        var alias = box.querySelector('#v2UploadAlias').value.trim();
                        if (alias) {
                            var aliasResult = await fetch('/api/v2/textures/' + encodeURIComponent(result.assetId) + '/alias', {
                                method: 'POST', headers: headers(), body: JSON.stringify({ alias: alias })
                            }).then(function (r) { return r.json(); });
                            if (!aliasResult.success) warnings.push('{{texture.aliasNotSaved}}' + (aliasResult.message || ''));
                        }
                        if (box.querySelector('#v2UploadPublic').checked) {
                            var publicResult = await fetch('/api/v2/textures/' + encodeURIComponent(result.assetId) + '/visibility', {
                                method: 'POST', headers: headers(), body: JSON.stringify({ isPublic: true })
                            }).then(function (r) { return r.json(); });
                            if (!publicResult.success) warnings.push('{{texture.publicFailed}}' + (publicResult.message || ''));
                        }
                        closeUploadModal();
                        showToastSafe(warnings.length ? ('{{texture.uploadSuccessPrefix}}' + warnings.join('；')) : '{{texture.uploadSuccess}}',
                            warnings.length === 0);
                        if (typeof done === 'function') done();
                    } catch (error) {
                        if (window.showToast) showToast(error.message || '{{texture.uploadFailed}}', 'error');
                        if (message) message.textContent = '';
                        button.disabled = false;
                        button.textContent = '{{texture.upload}}';
                    }
                }
                function applyTexture(assetId) {
                    fetch('/api/profiles', { headers: headers() })
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var profiles = data.profiles || [];
                            if (!profiles.length) { showToastSafe('{{msg.createProfileFirst}}', false); return; }
                            var old = document.getElementById('v2ApplyModal');
                            if (old) old.remove();
                            var overlay = document.createElement('div');
                            overlay.className = 'modal-overlay';
                            overlay.id = 'v2ApplyModal';
                            var box = document.createElement('div');
                            box.className = 'modal-box';
                            var opts = profiles.map(function (p) { return '<option value="' + esc(p.id) + '">' + esc(p.name) + ' (' + esc(p.id) + ')</option>'; }).join('');
                            box.innerHTML = '<button class="modal-close-btn" id="v2ApplyClose">&times;</button><h3>{{texture.applyToProfile}}</h3>'
                                + '<div class="form-group"><label class="form-label">{{texture.selectProfile}}</label><select class="form-input" id="v2ApplyProfile">' + opts + '</select></div>'
                                + '<div class="modal-actions"><button class="btn btn-secondary" id="v2ApplyCancel">{{common.cancel}}</button>'
                                + '<button class="btn btn-primary" id="v2ApplyConfirm">{{texture.apply}}</button></div>';
                            overlay.appendChild(box);
                            document.body.appendChild(overlay);
                            function close() { overlay.remove(); }
                            box.querySelector('#v2ApplyClose').addEventListener('click', close);
                            box.querySelector('#v2ApplyCancel').addEventListener('click', close);
                            overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
                            box.querySelector('#v2ApplyConfirm').addEventListener('click', function () {
                                var profileId = box.querySelector('#v2ApplyProfile').value;
                                fetch('/api/profiles/' + encodeURIComponent(profileId) + '/textures/' + TYPE, {
                                    method: 'PUT', headers: headers(),
                                    body: JSON.stringify({ assetId: assetId })
                                }).then(function (r) { return r.json(); }).then(function (res) {
                                    if (res.success) { close(); showToastSafe('{{texture.applied}}', true); }
                                    else { showToastSafe(res.message || '{{texture.applyFailed}}', false); }
                                }).catch(function () { showToastSafe('{{texture.applyRequestFailed}}', false); });
                            });
                        }).catch(function () { showToastSafe('{{msg.profileLoadFailed}}', false); });
                }
                function alias(id) {
                    showPromptDialog({ title: '{{common.rename}}' }, function (value) {
                        if (!value || !value.trim()) { showToast('{{common.nameRequired}}', 'error'); return false; }
                        fetch('/api/v2/textures/' + encodeURIComponent(id) + '/alias', {
                            method: 'POST', headers: headers(), body: JSON.stringify({ alias: value })
                        }).then(function (r) { return r.json(); }).then(function (data) {
                            msg(data.success ? '{{common.rename}}成功' : ('{{common.rename}}失败：' + (data.message || '')), data.success); load();
                        }).catch(function () { msg('{{texture.updateRequestFailed}}', false); });
                        return true;
                    });
                }
                function visibility(id, isPublic) {
                    fetch('/api/v2/textures/' + encodeURIComponent(id) + '/visibility', {
                        method: 'POST', headers: headers(), body: JSON.stringify({ isPublic: isPublic })
                    }).then(function (r) { return r.json(); }).then(function (data) {
                        msg(data.success ? '{{texture.visibilityUpdated}}' : ('{{common.updateFailedDetail}}' + (data.message || '')), data.success); load();
                    }).catch(function () { msg('{{texture.updateRequestFailed}}', false); });
                }
                function remove(id, anchor) {
                    showPopconfirm(anchor, '{{texture.deleteShortConfirm}}', function () {
                        fetch('/api/v2/textures/' + encodeURIComponent(id), { method: 'DELETE', headers: headers() })
                            .then(function (r) { return r.json(); }).then(function (data) {
                                msg(data.success ? '{{common.deleted}}' : ('{{common.deleteFailedDetail}}' + (data.message || '')), data.success); load();
                            }).catch(function () { msg('{{texture.deleteRequestFailed}}', false); });
                    }, { confirmText: '{{texture.deleteTexture}}' });
                }
                document.addEventListener('click', function (event) {
                    var target = event.target;
                    var actionEl = target.closest ? target.closest('[data-action]') : null;
                    if (actionEl && actionEl.getAttribute('data-action') === 'v2Upload') { openUploadPicker(); }
                    var applyId = target.getAttribute('data-v2-apply');
                    if (applyId) { applyTexture(applyId); }
                    var aliasId = target.getAttribute('data-v2-alias');
                    if (aliasId) { alias(aliasId); }
                    var visibilityId = target.getAttribute('data-v2-visibility');
                    if (visibilityId) { visibility(visibilityId, target.getAttribute('data-public') === '1'); }
                    var deleteId = target.getAttribute('data-v2-delete');
                    if (deleteId) { remove(deleteId, target); }
                });
                var fileInput = document.getElementById('v2TextureFile');
                if (fileInput) {
                    fileInput.addEventListener('change', function () {
                        if (this.files && this.files[0]) showUploadModal(this.files[0]);
                    });
                }
                load();
            })();
            </script>
            """.replace("__TYPE__", type);
        String navbar = PageRenderer.renderNavbar(im.xz.cn.config.SystemConfig.getInstance().getSiteName(), "user", false);
        String sidebar = Shared.buildUserSidebar(cape ? "capes" : "skins");
        String body = navbar + """
            <div class="user-layout">
                %s
                <div class="user-content">%s</div>
            </div>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage(pageTitle, csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }



    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KiB", bytes / 1024.0);
        return String.format("%.2f MiB", bytes / (1024.0 * 1024.0));
    }

}

