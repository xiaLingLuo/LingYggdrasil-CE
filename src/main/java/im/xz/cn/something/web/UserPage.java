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

package im.xz.cn.something.web;

import static im.xz.cn.something.web.Shared.esc;
import im.xz.cn.web.PageRenderer;

public class UserPage {

    public static String welcomeSection(String displayName, String username, String email, boolean emailVerified, String createdAt) {
        String emailBadge = emailVerified
                ? "<span class='badge badge-success'>已验证</span>"
                : "<span class='badge badge-warning'>未验证</span>";
        String date = createdAt != null ? createdAt.substring(0, Math.min(10, createdAt.length())) : "未知";
        return """
            <div class="welcome-section">
                <h2>欢迎回来，%s <i class="fas fa-wand-magic-sparkles"></i></h2>
                <p class="text-muted">账户: %s | 邮箱: %s %s</p>
                <p class="text-muted">注册时间: %s</p>
            </div>
            """.formatted(esc(displayName), esc(username), esc(email), emailBadge, date);
    }

    public static String yggdrasilGuide(String apiDomain) {
        String displayUrl = (apiDomain != null && !apiDomain.isBlank())
                ? esc(apiDomain)
                : "<span style='color:#999'>管理员尚未配置 API 外网域名，请联系管理员</span>";
        return String.format("""
            <div class="yggdrasil-guide">
                <div class="guide-step">
                    <div class="guide-step-num">1</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">创建角色</div>
                        <div class="guide-step-desc">前往 <a href="/profiles">角色管理</a> 创建你的游戏角色，每个角色拥有独立的 Yggdrasil Token。</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">2</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">获取 Yggdrasil Token</div>
                        <div class="guide-step-desc">在角色管理中查看并复制角色的 <strong>Yggdrasil Token</strong>，它将作为 Minecraft 登录密码使用。</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">3</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">配置 Minecraft 客户端</div>
                        <div class="guide-step-desc">在你的 Minecraft 启动器中添加自定义 Yggdrasil 认证服务器，填入以下地址：</div>
                        <div class="guide-code" id="yggdrasilUrl">%s</div>
                    </div>
                </div>
                <div class="guide-step">
                    <div class="guide-step-num">4</div>
                    <div class="guide-step-body">
                        <div class="guide-step-title">登录游戏</div>
                        <div class="guide-step-desc">在启动器中使用 <strong>角色名称</strong> 作为用户名，<strong>Yggdrasil Token</strong> 作为密码进行登录。</div>
                    </div>
                </div>
                <div class="guide-tips">
                    <div class="guide-tips-title"><i class="fas fa-lightbulb"></i> 小贴士</div>
                    <ul>
                        <li>不同启动器的自定义认证服务器设置位置可能不同，请参考对应启动器的文档。</li>
                        <li>Token 泄露可能导致他人冒用你的角色，请妥善保管。</li>
                        <li>如 Token 泄露，可在角色管理中重新生成。</li>
                    </ul>
                </div>
            </div>
            """, displayUrl);
    }

    public static String accountInfoContent(String username, String email, int profileCount) {
        return "<p><strong>用户名:</strong> %s</p><p><strong>邮箱:</strong> %s</p><p><strong>角色数:</strong> %d</p>"
                .formatted(esc(username), esc(email), profileCount);
    }

    public static String nicknameForm(String currentNickname) {
        return """
            <form id="nicknameForm">
                <div class="form-group">
                    <label class="form-label">当前昵称</label>
                    <input type="text" class="form-input" value="%s" disabled>
                </div>
                <div class="form-group">
                    <label class="form-label">新昵称</label>
                    <input type="text" class="form-input" id="newNickname" placeholder="输入新昵称" maxlength="32">
                </div>
                <button type="submit" class="btn btn-primary">保存昵称</button>
            </form>
            <div id="nicknameMsg" class="msg-area"></div>
            """.formatted(esc(currentNickname));
    }

    public static String emailForm(String currentEmail) {
        return """
            <form id="emailForm">
                <div class="form-group">
                    <label class="form-label">当前邮箱</label>
                    <input type="text" class="form-input" value="%s" disabled>
                </div>
                <div class="form-group">
                    <label class="form-label">新邮箱</label>
                    <input type="email" class="form-input" id="newEmail" placeholder="输入新邮箱">
                </div>
                <div class="form-group">
                    <label class="form-label">当前密码</label>
                    <input type="password" class="form-input" id="emailPassword" placeholder="输入当前密码">
                </div>
                <div class="form-group form-row">
                    <input type="text" class="form-input" id="emailVerifyCode" placeholder="邮箱验证码" style="flex:1">
                    <button type="button" class="btn btn-secondary" id="sendEmailCodeBtn" onclick="sendSettingsCode('email_change')">发送验证码</button>
                </div>
                <button type="submit" class="btn btn-primary">修改邮箱</button>
            </form>
            <div id="emailMsg" class="msg-area"></div>
            """.formatted(esc(currentEmail));
    }

    public static String passwordForm() {
        return """
            <form id="passwordForm">
                <div class="form-group">
                    <label class="form-label">当前密码</label>
                    <input type="password" class="form-input" id="currentPassword" placeholder="输入当前密码">
                </div>
                <div class="form-group">
                    <label class="form-label">新密码</label>
                    <input type="password" class="form-input" id="newPassword" placeholder="至少12位，含大小写字母、数字及特殊字符">
                    <div class="form-hint">密码要求：至少 12 位，需同时包含大写字母、小写字母、数字，以及至少一个特殊字符。允许的特殊字符：<span>@</span><span>$</span><span>!</span><span>%%</span><span>*</span><span>?</span><span>&amp;</span></div>
                </div>
                <div class="form-group">
                    <label class="form-label">确认新密码</label>
                    <input type="password" class="form-input" id="confirmPassword" placeholder="再次输入新密码">
                </div>
                <div class="form-group form-row">
                    <input type="text" class="form-input" id="passVerifyCode" placeholder="邮箱验证码" style="flex:1">
                    <button type="button" class="btn btn-secondary" id="sendPassCodeBtn" onclick="sendSettingsCode('password_change')">发送验证码</button>
                </div>
                <button type="submit" class="btn btn-primary">修改密码</button>
            </form>
            <div id="passwordMsg" class="msg-area"></div>
            """;
    }

    public static String createProfileForm() {
        return """
            <form id="createProfileForm" class="inline-form">
                <input type="text" class="form-input" id="newProfileName" placeholder="角色名称" maxlength="16" style="flex:1">
                <button type="submit" class="btn btn-primary">创建角色</button>
            </form>
            <div id="createMsg" class="msg-area"></div>
            """;
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
            "<div class='upload-tile-text'>上传皮肤</div>" +
            "<div class='upload-tile-hint'>PNG 格式</div>" +
            "</div>" +
            "<p class='text-muted' style='grid-column:1/-1'>加载中...</p>" +
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
            "<div class='upload-tile-text'>上传披风</div>" +
            "<div class='upload-tile-hint'>PNG 格式</div>" +
            "</div>" +
            "<p class='text-muted' style='grid-column:1/-1'>加载中...</p>" +
            "</div>";
    }

    public static String bannedPage() {
        return """
            <!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><title>账户已封禁</title>
            <link rel="stylesheet" href="/css/all.min.css">
            <style>body{font-family:'Segoe UI','Microsoft YaHei',sans-serif;background:linear-gradient(135deg,#FFF0F5,#F5F0FF);
            display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;color:#333}
            .box{background:#fff;border-radius:16px;padding:60px 48px;box-shadow:0 8px 32px rgba(255,105,180,0.12);text-align:center;max-width:420px}
            h1{color:#FF69B4;margin-bottom:16px}p{color:#666;line-height:1.6}</style></head>
            <body><div class="box"><h1><i class="fas fa-circle-xmark"></i> 账户已被封禁</h1><p>您的账户已被管理员封禁，无法访问系统。</p>
            <p>如有疑问，请联系管理员。</p></div></body></html>
            """;
    }

    public static String renderDashboardPage(String csrfToken, String siteName,
                                              String displayName, String username, String email,
                                              boolean emailVerified, String createdAt,
                                              String apiDomain, int profileCount) {
        String welcome = welcomeSection(displayName, username, email, emailVerified, createdAt);
        String guide = yggdrasilGuide(apiDomain);
        String guideSection = PageRenderer.renderCard("Yggdrasil Minecraft登录 快速使用指南", guide);
        String accountSection = PageRenderer.renderCard("账户信息", accountInfoContent(username, email, profileCount));
        String body = Shared.buildUserLayout(siteName, "dashboard", welcome + guideSection + "<br>" + accountSection, "dashboard");
        String csrf = Shared.csrfInject(csrfToken);
        return PageRenderer.renderPage("仪表盘", csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderSettingsPage(String csrfToken, String siteName, String nickname, String email) {
        String content = PageRenderer.renderCard("修改昵称", nicknameForm(nickname))
                + "<br>" + PageRenderer.renderCard("修改邮箱", emailForm(email))
                + "<br>" + PageRenderer.renderCard("修改密码", passwordForm())
                + "<br>" + PageRenderer.renderCard("黑名单管理",
                    "<div style='text-align:center'>"
                    + "<p id='blockedCountText' style='color:#999;margin-bottom:12px'>加载中...</p>"
                    + "<div style='display:flex;gap:10px;justify-content:center'>"
                    + "<button class='btn btn-danger' id='clearBlockedBtn'>清空黑名单</button>"
                    + "<button class='btn btn-secondary' id='manageBlockedBtn'>细致管理</button>"
                    + "</div></div>");
        String body = Shared.buildUserLayout(siteName, "settings", content, "settings");
        String csrf = Shared.csrfInject(csrfToken);
        return PageRenderer.renderPage("设置", csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderProfilesPage(String csrfToken, String siteName, String apiDomain) {
        String content = PageRenderer.renderCard("创建角色", createProfileForm())
                + "<br><div id='profileList'><p class='text-muted'>加载中...</p></div>"
                + "<div id='toast' class='toast' style='display:none;'></div>";
        String body = Shared.buildUserLayout(siteName, "profiles", content, "profiles");
        String apiDomainInject = "<script>window.YGGDRASIL_API_DOMAIN='" + esc(apiDomain != null ? apiDomain : "") + "';</script>";
        String csrf = Shared.csrfInject(csrfToken)
                + apiDomainInject
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage("角色管理", csrf + body, "user",
                Css.getUserCssLink(), Css.getUserDashboardCss());
    }

    public static String renderSkinsPage(String csrfToken) {
        String content = skinsContent();
        String navbar = PageRenderer.renderNavbar("用户中心", "user", false);
        String sidebar = Shared.buildUserSidebar("skins");
        String body = navbar + """
            <div class="admin-layout">
                %s
                <div class="admin-content">%s</div>
            </div>
            <script src="/js/user-skins.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage("我的皮肤", csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }

    public static String renderCapesPage(String csrfToken) {
        String content = capesContent();
        String navbar = PageRenderer.renderNavbar("用户中心", "user", false);
        String sidebar = Shared.buildUserSidebar("capes");
        String body = navbar + """
            <div class="admin-layout">
                %s
                <div class="admin-content">%s</div>
            </div>
            <script src="/js/user-capes.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage("我的披风", csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }

    public static String friendsContent() {
        return """
            <div class="friend-grid" id="friendList">
            <div class="friend-card friend-my-card card-animate" id="friendMyCard">
            <div class="friend-card-skin" id="mySkinPreview"></div>
            <div class="friend-card-info">
            <div class="friend-card-name" id="myName">我</div>
            <div class="friend-card-code" id="myCode">----</div>
            </div>
            </div>
            <div class="friend-add-tile card-animate" id="friendAddTile">
            <div class="friend-add-tile-icon">
            <i class="fas fa-user-plus"></i>
            </div>
            <div class="friend-add-tile-text">添加好友</div>
            <div class="friend-add-tile-hint">输入好友代码</div>
            </div>
            <p class="text-muted" style="grid-column:1/-1">加载中...</p>
            </div>
            <div id="toast" class="toast" style="display:none"></div>
            """;
    }

    public static String renderFriendsPage(String csrfToken) {
        String content = friendsContent();
        String navbar = PageRenderer.renderNavbar("用户中心", "user", false);
        String sidebar = Shared.buildUserSidebar("friends");
        String body = navbar + """
            <div class="admin-layout">
                %s
                <div class="admin-content">%s</div>
            </div>
            <script src="/js/user-friends.js"></script>
            """.formatted(sidebar, content);
        String csrf = Shared.csrfInject(csrfToken)
                + "<script src=\"/js/skinview3d.bundle.js\"></script>";
        return PageRenderer.renderPage("好友", csrf + body, "user",
                Css.getUserCssLink(), Css.getTextureCss());
    }
}
