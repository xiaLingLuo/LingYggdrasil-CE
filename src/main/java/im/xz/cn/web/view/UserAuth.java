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

public class UserAuth {

    public static String renderLoginPage(String siteName) {
        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <img src="/icons/app.ico" class="logo-icon auth-icon" alt="LingYggdrasil">
                        <h2>%s</h2>
                        <p class="text-muted">%s</p>
                    </div>
                    <form id="loginForm">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" class="form-input" id="username" placeholder="%s" autocomplete="username">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="password" class="form-input" id="password" placeholder="%s" autocomplete="current-password">
                        </div>
                        <div id="loginError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">%s</button>
                    </form>
                    <div class="auth-footer">
                        <span>%s</span>
                        <a href="/register">%s</a>
                    </div>
                </div>
            </div>
            <script src="/js/user-login.js"></script>
            """.formatted(
                I18n.t("auth.login.title", esc(siteName)),
                I18n.t("auth.login.subtitle"),
                I18n.t("auth.login.identifier"),
                I18n.t("auth.login.identifierPlaceholder"),
                I18n.t("auth.login.password"),
                I18n.t("auth.login.passwordPlaceholder"),
                I18n.t("auth.login.submit"),
                I18n.t("auth.login.noAccount"),
                I18n.t("auth.login.registerNow"));
        return PageRenderer.renderPage(I18n.t("auth.login.pageTitle"), body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderRegisterPage(String siteName, boolean registrationEnabled, boolean emailVerificationEnabled) {
        if (!registrationEnabled) {
            String body = """
                <div class="auth-container">
                    <div class="auth-card">
                        <div class="auth-header">
                            <img src="/icons/app.ico" class="logo-icon auth-icon" alt="LingYggdrasil">
                            <h2>%s</h2>
                            <p class="text-muted">%s</p>
                        </div>
                        <div class="auth-footer">
                            <a href="/login">%s</a>
                        </div>
                    </div>
                </div>
                """.formatted(
                    I18n.t("auth.register.closedTitle"),
                    I18n.t("auth.register.closedDesc"),
                    I18n.t("auth.register.backLogin"));
            return PageRenderer.renderPage(I18n.t("auth.register.pageTitle"), body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
        }

        String verifyCodeSection = emailVerificationEnabled ? """
            <div class="form-group form-row">
                <input type="text" class="form-input" id="verifyCode" placeholder="%s" style="flex:1" maxlength="8">
                <button type="button" class="btn btn-secondary" id="sendCodeBtn" data-action="sendRegCode">%s</button>
            </div>
            """.formatted(I18n.t("auth.register.verifyCodePlaceholder"), I18n.t("auth.register.sendCode")) : "";

        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <img src="/icons/app.ico" class="logo-icon auth-icon" alt="LingYggdrasil">
                        <h2>%s</h2>
                        <p class="text-muted">%s</p>
                    </div>
                    <form id="registerForm">
                        <div class="form-group">
                            <label class="form-label">%s <span class="required">*</span></label>
                            <input type="text" class="form-input" id="username" placeholder="%s" maxlength="16" autocomplete="username">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s <span class="required">*</span></label>
                            <input type="email" class="form-input" id="email" placeholder="%s" autocomplete="email">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s <span class="required">*</span></label>
                            <input type="password" class="form-input" id="password" placeholder="%s" autocomplete="new-password">
                            <div class="form-hint">%s<span>@</span><span>$</span><span>!</span><span>%%</span><span>*</span><span>?</span><span>&amp;</span></div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s <span class="text-muted">%s</span></label>
                            <input type="text" class="form-input" id="nickname" placeholder="%s" maxlength="32">
                        </div>
                        %s
                        <div id="registerError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">%s</button>
                    </form>
                    <div class="auth-footer">
                        <span>%s</span>
                        <a href="/login">%s</a>
                    </div>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/user-register.js"></script>
            """.formatted(
                I18n.t("auth.register.title", esc(siteName)),
                I18n.t("auth.register.subtitle"),
                I18n.t("auth.register.username"),
                I18n.t("auth.register.usernamePlaceholder"),
                I18n.t("auth.register.email"),
                I18n.t("auth.register.emailPlaceholder"),
                I18n.t("auth.register.password"),
                I18n.t("auth.register.passwordPlaceholder"),
                I18n.t("auth.register.passwordHint"),
                I18n.t("auth.register.nickname"),
                I18n.t("auth.register.nicknameOptional"),
                I18n.t("auth.register.nicknamePlaceholder"),
                verifyCodeSection,
                I18n.t("auth.register.submit"),
                I18n.t("auth.register.haveAccount"),
                I18n.t("auth.register.goLogin"));
        return PageRenderer.renderPage(I18n.t("auth.register.pageTitle"), body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderVerifyEmailPage() {
        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <img src="/icons/app.ico" class="logo-icon auth-icon" alt="LingYggdrasil">
                        <h2>%s</h2>
                        <p class="text-muted">%s</p>
                    </div>
                    <form id="verifyForm">
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="email" class="form-input" id="email" placeholder="%s">
                        </div>
                        <div class="form-group">
                            <label class="form-label">%s</label>
                            <input type="text" class="form-input" id="code" placeholder="%s" maxlength="8">
                        </div>
                        <input type="hidden" id="verifyType" value="registration">
                        <div id="verifyError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">%s</button>
                    </form>
                    <div class="auth-footer">
                        <a href="#" data-action="resendCode" data-prevent>%s</a>
                        <span> | </span>
                        <a href="/register">%s</a>
                    </div>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/user-register.js"></script>
            """.formatted(
                I18n.t("auth.verify.title"),
                I18n.t("auth.verify.subtitle"),
                I18n.t("auth.verify.email"),
                I18n.t("auth.verify.emailPlaceholder"),
                I18n.t("auth.verify.code"),
                I18n.t("auth.verify.codePlaceholder"),
                I18n.t("auth.verify.submit"),
                I18n.t("auth.verify.resend"),
                I18n.t("auth.verify.backRegister"));
        return PageRenderer.renderPage(I18n.t("auth.verify.pageTitle"), body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderEmailRequiredPage(String userEmail, String csrfInject) {
        String body = (csrfInject == null ? "" : csrfInject) + """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <span class="auth-icon"><i class="fas fa-envelope"></i></span>
                        <h2>%s</h2>
                        <p class="text-muted">%s</p>
                    </div>
                    <div style="text-align:center; padding:12px 0; color:#666; font-size:14px; line-height:1.8;">
                        <p>%s</p>
                        <p>%s</p>
                    </div>
                    <div id="toast" class="toast" style="display:none;"></div>
                    <div class="form-group form-row" style="margin-top:12px;">
                        <input type="text" class="form-input" id="verifyCode" placeholder="%s" maxlength="8" style="flex:1;">
                        <button type="button" class="btn btn-secondary" id="sendCodeBtn" data-action="sendEmailCode">%s</button>
                    </div>
                    <button type="button" class="btn btn-primary btn-block" data-action="verifyEmail">%s</button>
                    <div class="auth-footer" style="margin-top:16px;">
                        <a href="/logout">%s</a>
                    </div>
                </div>
            </div>
            <script>
            var _countdown = 0;
            var _timer = null;
            async function sendEmailCode() {
                var btn = document.getElementById('sendCodeBtn');
                if (_countdown > 0) return;
                btn.disabled = true;
                btn.textContent = t('auth.emailRequired.sending');
                try {
                    var resp = await fetch('/api/send-email-verify', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
                    });
                    var data = await resp.json();
                    showToast(data.message, data.success ? 'success' : 'error');
                    if (data.success) {
                        _countdown = 300;
                        _timer = setInterval(function() {
                            _countdown--;
                            if (_countdown <= 0) {
                                clearInterval(_timer);
                                btn.disabled = false;
                                btn.textContent = t('auth.emailRequired.resend');
                            } else {
                                btn.textContent = _countdown + 's';
                            }
                        }, 1000);
                    } else {
                        btn.disabled = false;
                        btn.textContent = t('auth.emailRequired.sendCode');
                    }
                } catch(e) {
                    showToast(t('common.networkError'), 'error');
                    btn.disabled = false;
                    btn.textContent = t('auth.emailRequired.sendCode');
                }
            }
            async function verifyEmail() {
                var code = document.getElementById('verifyCode').value.trim();
                if (!code) { showToast(t('auth.emailRequired.codePlaceholder'), 'error'); return; }
                try {
                    var resp = await fetch('/api/verify-my-email', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                        body: JSON.stringify({ code: code })
                    });
                    var data = await resp.json();
                    showToast(data.message, data.success ? 'success' : 'error');
                    if (data.success && data.redirect) {
                        setTimeout(function() { window.location.href = data.redirect; }, 800);
                    }
                } catch(e) {
                    showToast(t('common.networkError'), 'error');
                }
            }
            document.getElementById('verifyCode').addEventListener('keydown', function(e) {
                if (e.key === 'Enter') verifyEmail();
            });
            </script>
            """.formatted(
                I18n.t("auth.emailRequired.title"),
                I18n.t("auth.emailRequired.subtitle"),
                I18n.t("auth.emailRequired.notVerified", esc(userEmail)),
                I18n.t("auth.emailRequired.requirement"),
                I18n.t("auth.emailRequired.codePlaceholder"),
                I18n.t("auth.emailRequired.sendCode"),
                I18n.t("auth.emailRequired.confirm"),
                I18n.t("auth.emailRequired.logout"));
        return PageRenderer.renderPage(I18n.t("auth.emailRequired.pageTitle"), body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }
}
