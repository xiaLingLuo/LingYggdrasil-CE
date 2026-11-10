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
    let codeTimers = {};

    const nicknameForm = document.getElementById('nicknameForm');
    if (nicknameForm) {
        nicknameForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const nickname = document.getElementById('newNickname').value.trim();
            const msgDiv = document.getElementById('nicknameMsg');

            if (!nickname) { showMsg(msgDiv, t('settings.enterNickname'), false); return; }

            var nicknameLoading = window.startBtnLoading(nicknameForm.querySelector('button[type="submit"]'));
            try {
                const resp = await fetch('/api/settings/nickname', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ nickname })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    document.getElementById('newNickname').value = '';
                    setTimeout(() => location.reload(), 1000);
                }
            } catch (err) {
                showMsg(msgDiv, t('common.networkError'), false);
            } finally {
                window.finishBtnLoading(nicknameLoading);
            }
        });
    }

    const emailForm = document.getElementById('emailForm');
    if (emailForm) {
        emailForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const newEmail = document.getElementById('newEmail').value.trim();
            const password = document.getElementById('emailPassword').value;
            const verifyCode = document.getElementById('emailVerifyCode').value.trim();
            const msgDiv = document.getElementById('emailMsg');

            if (!newEmail || !newEmail.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
                showMsg(msgDiv, t('settings.enterEmail'), false); return;
            }
            if (!password) { showMsg(msgDiv, t('settings.enterCurrentPassword'), false); return; }
            if (!verifyCode) { showMsg(msgDiv, t('settings.enterCode'), false); return; }

            var emailLoading = window.startBtnLoading(emailForm.querySelector('button[type="submit"]'));
            try {
                const resp = await fetch('/api/settings/email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ newEmail, password, verifyCode })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    setTimeout(() => location.reload(), 1000);
                }
            } catch (err) {
                showMsg(msgDiv, t('common.networkError'), false);
            } finally {
                window.finishBtnLoading(emailLoading);
            }
        });
    }

    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const verifyCode = document.getElementById('passVerifyCode').value.trim();
            const msgDiv = document.getElementById('passwordMsg');

            if (!currentPassword) { showMsg(msgDiv, t('settings.enterCurrentPassword'), false); return; }
            const PASSWORD_REGEX = /^[A-Za-z\d@$!%*?&]{6,}$/;
            if (!newPassword || !PASSWORD_REGEX.test(newPassword)) { showMsg(msgDiv, t('settings.newPasswordRule'), false); return; }
            if (newPassword !== confirmPassword) { showMsg(msgDiv, t('settings.passwordMismatch'), false); return; }
            if (!verifyCode) { showMsg(msgDiv, t('settings.enterCode'), false); return; }

            var passwordLoading = window.startBtnLoading(passwordForm.querySelector('button[type="submit"]'));
            try {
                const resp = await fetch('/api/settings/password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ currentPassword, newPassword, verifyCode })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    document.getElementById('currentPassword').value = '';
                    document.getElementById('newPassword').value = '';
                    document.getElementById('confirmPassword').value = '';
                }
            } catch (err) {
                showMsg(msgDiv, t('common.networkError'), false);
            } finally {
                window.finishBtnLoading(passwordLoading);
            }
        });
    }

    window.sendSettingsCode = async function(type) {
        const btnId = type === 'email_change' ? 'sendEmailCodeBtn' : 'sendPassCodeBtn';
        const btn = document.getElementById(btnId);
        if (!btn || btn.disabled) return;

        btn.disabled = true;
        btn.textContent = t('common.sending');

        try {
            const resp = await fetch('/api/settings/send-verify-code', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ type })
            });
            const data = await resp.json();

            if (data.success) {
                btn.textContent = t('common.sent');
                startCountdown(btnId, 300);
            } else {
                showToast(data.message || t('settings.sendFailed'), 'error');
                btn.disabled = false;
                btn.textContent = t('common.sendCode');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
            btn.disabled = false;
            btn.textContent = t('common.sendCode');
        }
    };

    function startCountdown(btnId, seconds) {
        const btn = document.getElementById(btnId);
        let remaining = seconds;
        if (codeTimers[btnId]) clearInterval(codeTimers[btnId]);
        codeTimers[btnId] = setInterval(() => {
            remaining--;
            btn.textContent = remaining + 's';
            if (remaining <= 0) {
                clearInterval(codeTimers[btnId]);
                btn.textContent = t('common.sendCode');
                btn.disabled = false;
            }
        }, 1000);
    }

    function showMsg(div, msg, success) {
        if (typeof window.showToast === 'function') window.showToast(msg, success ? 'success' : 'error');
        if (div) {
            div.textContent = '';
            div.className = 'msg-area';
        }
    }

    var blockedCountEl = document.getElementById('blockedCountText');
    var clearBlockedBtn = document.getElementById('clearBlockedBtn');
    var manageBlockedBtn = document.getElementById('manageBlockedBtn');

    function showBlockedConfirm(message, onConfirm) {
        showConfirmDialog(message, onConfirm, { confirmText: t('common.confirm') });
    }

    function loadBlockedCount() {
        fetch('/api/v2/blocks').then(function(r) { return r.json(); }).then(function(d) {
            if (d.success && blockedCountEl) {
                blockedCountEl.textContent = t('settings.blockedCount', (d.users || []).length);
            }
        }).catch(function() {});
    }

    if (clearBlockedBtn) {
        clearBlockedBtn.addEventListener('click', function() {
            showBlockedConfirm(t('settings.clearBlockedConfirm'), function() {
            fetch('/api/v2/blocks/clear', {
                method: 'POST',
                headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
            }).then(function(r) { return r.json(); }).then(function(d) {
                showToast(d.message || (d.success ? t('settings.cleared') : t('common.failed')), d.success ? 'success' : 'error');
                if (d.success) loadBlockedCount();
            }).catch(function() { showToast(t('common.networkError'), 'error'); });
            });
        });
    }

    if (manageBlockedBtn) {
        manageBlockedBtn.addEventListener('click', function() {
            var existing = document.getElementById('blockedManageModal');
            if (existing) existing.remove();
            var overlay = document.createElement('div');
            overlay.className = 'modal-overlay';
            overlay.id = 'blockedManageModal';
            var box = document.createElement('div');
            box.className = 'modal-box';
            box.style.maxWidth = '440px';
            box.innerHTML = '<button class="modal-close-btn" id="blockedCloseBtn">&times;</button>' +
                '<h3>' + t('settings.manage') + '</h3>' +
                '<div id="blockedList" style="max-height:400px;overflow-y:auto"><p class="text-muted">' + t('common.loading') + '</p></div>' +
                '<div id="blockedMsg" class="msg-area"></div>';
            overlay.appendChild(box);
            document.body.appendChild(overlay);
            document.getElementById('blockedCloseBtn').addEventListener('click', function() { overlay.remove(); });
            overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });

            fetch('/api/v2/blocks').then(function(r) { return r.json(); }).then(function(d) {
                var container = document.getElementById('blockedList');
                if (!d.success || !d.users || d.users.length === 0) {
                    container.innerHTML = '<p class="empty-hint">' + t('settings.blockedEmpty') + '</p>';
                    return;
                }
                var html = '';
                d.users.forEach(function(b) {
                    html += '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px;border-bottom:1px solid #FFD6E8;">' +
                        '<span style="font-family:Consolas,monospace;font-size:14px;color:#FF69B4;letter-spacing:0.5px">' + escapeHtml(b.code || b.name || b.userId || '----') + '</span>' +
                        '<button class="btn btn-secondary" style="padding:4px 12px;font-size:12px" data-user-id="' + escAttr(b.userId) + '">' + t('settings.remove') + '</button></div>';
                });
                container.innerHTML = html;
                container.querySelectorAll('button').forEach(function(btn) {
                    btn.addEventListener('click', function() {
                        var uid = this.dataset.userId;
                        fetch('/api/v2/blocks/' + encodeURIComponent(uid), {
                            method: 'DELETE',
                            headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
                        }).then(function(r) { return r.json(); }).then(function(d) {
                            showToast(d.message || (d.success ? t('settings.removed') : t('common.failed')), d.success ? 'success' : 'error');
                            if (d.success) { btn.closest('div').remove(); loadBlockedCount(); }
                        }).catch(function() { showToast(t('common.networkError'), 'error'); });
                    });
                });
            }).catch(function() {
                var container = document.getElementById('blockedList');
                if (container) container.innerHTML = '<p class="empty-hint" style="color:#C62828">' + t('common.loadFailedShort') + '</p>';
            });
        });
    }

    const accountPasswordToggle = document.getElementById('accountPasswordLoginToggle');
    if (accountPasswordToggle) {
        const externalMsg = document.getElementById('externalLoginMsg');
        fetch('/api/settings/external-login').then(function(r) { return r.json(); }).then(function(d) {
            if (d && d.success) accountPasswordToggle.checked = !!d.allowAccountPasswordLogin;
        }).catch(function() {});
        accountPasswordToggle.addEventListener('change', function() {
            fetch('/api/settings/external-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ allowAccountPasswordLogin: accountPasswordToggle.checked })
            }).then(function(r) { return r.json(); }).then(function(d) {
                showMsg(externalMsg, d.success ? t('settings.externalLoginSaved')
                    : (d.message || t('common.failed')), d.success);
            }).catch(function() {
                showMsg(externalMsg, t('common.networkError'), false);
            });
        });
    }

    const cancelAccountBtn = document.getElementById('cancelAccountBtn');
    if (cancelAccountBtn) {
        cancelAccountBtn.addEventListener('click', function() {
            const msg = document.getElementById('cancelAccountMsg');
            const pwdInput = document.getElementById('cancelAccountPassword');
            const password = pwdInput ? pwdInput.value : '';
            if (!password) { showMsg(msg, t('settings.enterCurrentPassword'), false); return; }
            const run = function() {
                fetch('/api/settings/cancel-account', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ password: password })
                }).then(function(r) { return r.json(); }).then(function(d) {
                    showMsg(msg, d.message || (d.success ? t('msg.accountCancelled') : t('common.failed')), d.success);
                    if (d.success) { setTimeout(function() { window.location.href = '/logout'; }, 1500); }
                }).catch(function() { showMsg(msg, t('common.networkError'), false); });
            };
            showConfirmDialog(t('settings.cancelAccountConfirm'), run);
        });
    }

    if (blockedCountEl) loadBlockedCount();
})();
