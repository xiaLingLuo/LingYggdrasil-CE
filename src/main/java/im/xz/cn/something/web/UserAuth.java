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

public class UserAuth {

    public static String renderLoginPage(String siteName) {
        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <span class="auth-icon">✿</span>
                        <h2>登录 %s</h2>
                        <p class="text-muted">欢迎回来，请登录您的账户</p>
                    </div>
                    <form id="loginForm">
                        <div class="form-group">
                            <label class="form-label">用户名 / 邮箱</label>
                            <input type="text" class="form-input" id="username" placeholder="请输入用户名或邮箱" autocomplete="username">
                        </div>
                        <div class="form-group">
                            <label class="form-label">密码</label>
                            <input type="password" class="form-input" id="password" placeholder="请输入密码" autocomplete="current-password">
                        </div>
                        <div id="loginError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">登录</button>
                    </form>
                    <div class="auth-footer">
                        <span>还没有账号？</span>
                        <a href="/register">立即注册</a>
                    </div>
                </div>
            </div>
            <script src="/js/user-login.js"></script>
            """.formatted(esc(siteName));
        return PageRenderer.renderPage("登录", body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderRegisterPage(String siteName, boolean registrationEnabled, boolean emailVerificationEnabled) {
        if (!registrationEnabled) {
            String body = """
                <div class="auth-container">
                    <div class="auth-card">
                        <div class="auth-header">
                            <span class="auth-icon">✿</span>
                            <h2>注册已关闭</h2>
                            <p class="text-muted">管理员已关闭注册功能，请稍后再试。</p>
                        </div>
                        <div class="auth-footer">
                            <a href="/login">返回登录</a>
                        </div>
                    </div>
                </div>
                """;
            return PageRenderer.renderPage("注册", body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
        }

        String verifyCodeSection = emailVerificationEnabled ? """
            <div class="form-group form-row">
                <input type="text" class="form-input" id="verifyCode" placeholder="邮箱验证码" style="flex:1" maxlength="8">
                <button type="button" class="btn btn-secondary" id="sendCodeBtn" onclick="sendRegCode()">发送验证码</button>
            </div>
            """ : "";

        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <span class="auth-icon">✿</span>
                        <h2>注册 %s</h2>
                        <p class="text-muted">创建您的账户，开始旅程</p>
                    </div>
                    <form id="registerForm">
                        <div class="form-group">
                            <label class="form-label">用户名 <span class="required">*</span></label>
                            <input type="text" class="form-input" id="username" placeholder="3-16个字符" maxlength="16" autocomplete="username">
                        </div>
                        <div class="form-group">
                            <label class="form-label">邮箱 <span class="required">*</span></label>
                            <input type="email" class="form-input" id="email" placeholder="请输入邮箱" autocomplete="email">
                        </div>
                        <div class="form-group">
                            <label class="form-label">密码 <span class="required">*</span></label>
                            <input type="password" class="form-input" id="password" placeholder="至少12位，含大小写字母、数字及特殊字符" autocomplete="new-password">
                            <div class="form-hint">密码要求：至少 12 位，需同时包含大写字母、小写字母、数字，以及至少一个特殊字符。允许的特殊字符：<span>@</span><span>$</span><span>!</span><span>%%</span><span>*</span><span>?</span><span>&amp;</span></div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">昵称 <span class="text-muted">(可选)</span></label>
                            <input type="text" class="form-input" id="nickname" placeholder="给自己取个名字吧" maxlength="32">
                        </div>
                        %s
                        <div id="registerError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">注册</button>
                    </form>
                    <div class="auth-footer">
                        <span>已有账号？</span>
                        <a href="/login">去登录</a>
                    </div>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/user-register.js"></script>
            """.formatted(esc(siteName), verifyCodeSection);
        return PageRenderer.renderPage("注册", body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderVerifyEmailPage() {
        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <span class="auth-icon">✿</span>
                        <h2>邮箱验证</h2>
                        <p class="text-muted">请输入发送到您邮箱的验证码</p>
                    </div>
                    <form id="verifyForm">
                        <div class="form-group">
                            <label class="form-label">邮箱地址</label>
                            <input type="email" class="form-input" id="email" placeholder="请输入邮箱">
                        </div>
                        <div class="form-group">
                            <label class="form-label">验证码</label>
                            <input type="text" class="form-input" id="code" placeholder="请输入8位验证码" maxlength="8">
                        </div>
                        <input type="hidden" id="verifyType" value="registration">
                        <div id="verifyError" class="alert alert-error" style="display:none"></div>
                        <button type="submit" class="btn btn-primary btn-block">验证</button>
                    </form>
                    <div class="auth-footer">
                        <a href="#" onclick="resendCode(); return false;">重新发送验证码</a>
                        <span> | </span>
                        <a href="/register">返回注册</a>
                    </div>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/user-register.js"></script>
            """;
        return PageRenderer.renderPage("邮箱验证", body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }

    public static String renderEmailRequiredPage(String userEmail) {
        String body = """
            <div class="auth-container">
                <div class="auth-card">
                    <div class="auth-header">
                        <span class="auth-icon"><i class="fas fa-envelope"></i></span>
                        <h2>邮箱验证</h2>
                        <p class="text-muted">需要完成邮箱验证后才能使用所有功能</p>
                    </div>
                    <div style="text-align:center; padding:12px 0; color:#666; font-size:14px; line-height:1.8;">
                        <p>您的邮箱 <strong>%s</strong> 尚未完成验证。</p>
                        <p>管理员已开启邮箱验证要求，请完成验证后继续使用。</p>
                    </div>
                    <div id="toast" class="toast" style="display:none;"></div>
                    <div class="form-group form-row" style="margin-top:12px;">
                        <input type="text" class="form-input" id="verifyCode" placeholder="请输入验证码" maxlength="8" style="flex:1;">
                        <button type="button" class="btn btn-secondary" id="sendCodeBtn" onclick="sendEmailCode()">发送验证码</button>
                    </div>
                    <button type="button" class="btn btn-primary btn-block" onclick="verifyEmail()">确认验证</button>
                    <div class="auth-footer" style="margin-top:16px;">
                        <a href="/logout">退出登录</a>
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
                btn.textContent = '发送中...';
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
                                btn.textContent = '重新发送';
                            } else {
                                btn.textContent = _countdown + 's';
                            }
                        }, 1000);
                    } else {
                        btn.disabled = false;
                        btn.textContent = '发送验证码';
                    }
                } catch(e) {
                    showToast('网络错误，请稍后重试', 'error');
                    btn.disabled = false;
                    btn.textContent = '发送验证码';
                }
            }
            async function verifyEmail() {
                var code = document.getElementById('verifyCode').value.trim();
                if (!code) { showToast('请输入验证码', 'error'); return; }
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
                    showToast('网络错误，请稍后重试', 'error');
                }
            }
            function showToast(message, type) {
                var toast = document.getElementById('toast');
                if (toast) {
                    toast.textContent = message;
                    toast.className = 'toast toast-' + (type || 'info');
                    toast.style.display = 'block';
                    setTimeout(function() { toast.style.display = 'none'; }, 3000);
                }
            }
            document.getElementById('verifyCode').addEventListener('keydown', function(e) {
                if (e.key === 'Enter') verifyEmail();
            });
            </script>
            """.formatted(esc(userEmail));
        return PageRenderer.renderPage("邮箱验证", body, "user", Css.getUserCssLink(), Css.getAuthPageCss());
    }
}
