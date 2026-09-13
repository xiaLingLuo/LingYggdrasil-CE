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
(function() {
    let codeTimer = null;

    const regForm = document.getElementById('registerForm');
    if (regForm) {
        regForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const username = document.getElementById('username').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const nicknameEl = document.getElementById('nickname');
            const nickname = nicknameEl ? nicknameEl.value.trim() : '';
            const verifyCodeEl = document.getElementById('verifyCode');
            const verifyCode = verifyCodeEl ? verifyCodeEl.value.trim() : '';
            const errorDiv = document.getElementById('registerError');
            if (errorDiv) errorDiv.style.display = 'none';

            if (username.length < 3 || username.length > 16) {
                showError(errorDiv, t('auth.register.usernameRule')); return;
            }
            if (!email.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
                showError(errorDiv, t('auth.register.emailInvalid')); return;
            }
            const PASSWORD_REGEX = /^[A-Za-z\d@$!%*?&]{6,}$/;
            if (!PASSWORD_REGEX.test(password)) {
                showError(errorDiv, t('auth.register.passwordRule')); return;
            }

            const submitBtn = regForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = t('auth.register.registering');

            try {
                const body = { username, email, password, nickname };
                if (verifyCode) body.verifyCode = verifyCode;

                const resp = await fetch('/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify(body)
                });
                const data = await resp.json();

                if (data.success) {
                    showToast(t('auth.register.success'), 'success');
                    setTimeout(() => { window.location.href = data.redirect || '/login'; }, 1000);
                } else if (data.needVerify) {
                    showToast(data.message || t('auth.register.codeSent'), 'success');
                    if (verifyCodeEl) verifyCodeEl.focus();
                    submitBtn.disabled = false;
                    submitBtn.textContent = t('auth.register.submit');
                } else {
                    showToast(data.message || t('auth.register.failed'), 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = t('auth.register.submit');
                }
            } catch (err) {
                showToast(t('common.networkError'), 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = t('auth.register.submit');
            }
        });
    }

    const verifyForm = document.getElementById('verifyForm');
    if (verifyForm) {
        const params = new URLSearchParams(window.location.search);
        const emailParam = params.get('email');
        if (emailParam) {
            document.getElementById('email').value = emailParam;
        }

        verifyForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const email = document.getElementById('email').value.trim();
            const code = document.getElementById('code').value.trim();
            const type = document.getElementById('verifyType').value;
            const errorDiv = document.getElementById('verifyError');
            if (errorDiv) errorDiv.style.display = 'none';

            if (!email || !code) {
                showError(errorDiv, t('auth.verify.fillAll')); return;
            }

            const submitBtn = verifyForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = t('auth.verify.verifying');

            try {
                const resp = await fetch('/api/verify-email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ email, code, type })
                });
                const data = await resp.json();

                if (data.success) {
                    showToast(t('auth.verify.success'), 'success');
                    setTimeout(() => { window.location.href = data.redirect || '/login'; }, 1000);
                } else {
                    showToast(data.message || t('auth.verify.failed'), 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = t('auth.verify.submit');
                }
            } catch (err) {
                showToast(t('common.networkError'), 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = t('auth.verify.submit');
            }
        });
    }

    window.sendRegCode = async function() {
        const email = document.getElementById('email').value.trim();
        const btn = document.getElementById('sendCodeBtn');

        if (!email || !email.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
            showToast(t('auth.register.emailFirst'), 'error');
            return;
        }

        btn.disabled = true;
        try {
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const nicknameEl = document.getElementById('nickname');

            if (!username || username.length < 3) {
                showToast(t('auth.register.usernameFirst'), 'error');
                btn.disabled = false; return;
            }
            const PASSWORD_REGEX = /^[A-Za-z\d@$!%*?&]{6,}$/;
            if (!password || !PASSWORD_REGEX.test(password)) {
                showToast(t('auth.register.passwordFirst'), 'error');
                btn.disabled = false; return;
            }

            const body = { username, email, password, nickname: nicknameEl ? nicknameEl.value.trim() : '' };
            const resp = await fetch('/api/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify(body)
            });
            const data = await resp.json();

            if (data.needVerify) {
                showToast(data.message || t('auth.register.codeSentShort'), 'success');
                startCountdown(btn, 300);
            } else if (data.success) {
                window.location.href = data.redirect || '/login';
            } else {
                showToast(data.message || t('auth.register.sendFailed'), 'error');
                btn.disabled = false;
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
            btn.disabled = false;
        }
    };

    window.resendCode = async function() {
        const email = document.getElementById('email').value.trim();
        if (!email) { showToast(t('auth.register.emailRequired'), 'error'); return; }

        try {
            const resp = await fetch('/api/resend-code', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ email, type: 'registration' })
            });
            const data = await resp.json();
            showToast(data.message || (data.success ? t('auth.register.codeResent') : t('auth.register.sendFailed')), data.success ? 'success' : 'error');
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    };

    function startCountdown(btn, seconds) {
        let remaining = seconds;
        btn.textContent = remaining + 's';
        if (codeTimer) clearInterval(codeTimer);
        codeTimer = setInterval(() => {
            remaining--;
            btn.textContent = remaining + 's';
            if (remaining <= 0) {
                clearInterval(codeTimer);
                btn.textContent = t('auth.register.sendCode');
                btn.disabled = false;
            }
        }, 1000);
    }

    function showError(div, msg) {
        div.innerHTML = '<i class="fas fa-circle-xmark"></i> ' + msg;
        div.style.display = 'flex';
    }

})();
