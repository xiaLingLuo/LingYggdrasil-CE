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

            if (!newEmail || !newEmail.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
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
        div.innerHTML = (success ? '<i class="fas fa-circle-check"></i> ' : '<i class="fas fa-circle-xmark"></i> ') + msg;
        div.className = 'msg-area ' + (success ? 'success' : 'error');
    }

    var blockedCountEl = document.getElementById('blockedCountText');
    var clearBlockedBtn = document.getElementById('clearBlockedBtn');
    var manageBlockedBtn = document.getElementById('manageBlockedBtn');

    function showBlockedConfirm(message, onConfirm) {
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'blockedConfirm';
        overlay.style.zIndex = '1100';
        overlay.innerHTML = '<div class="modal-box"><h3>' + t('common.confirmAction') + '</h3><p>' + escapeHtml(message) + '</p>' +
            '<div class="modal-actions" id="blockedConfirmActions"></div></div>';
        document.body.appendChild(overlay);
        var a = overlay.querySelector('#blockedConfirmActions');
        var cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = t('common.cancel');
        cancelBtn.addEventListener('click', function() { overlay.remove(); });
        a.appendChild(cancelBtn);
        var confirmBtn = document.createElement('button');
        confirmBtn.className = 'btn btn-danger';
        confirmBtn.textContent = t('common.confirm');
        confirmBtn.addEventListener('click', function() { overlay.remove(); if (onConfirm) onConfirm(); });
        a.appendChild(confirmBtn);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
    }

    function loadBlockedCount() {
        fetch('/api/friends/blocked/count').then(function(r) { return r.json(); }).then(function(d) {
            if (d.success && blockedCountEl) {
                blockedCountEl.textContent = t('settings.blockedCount', d.count);
            }
        }).catch(function() {});
    }

    if (clearBlockedBtn) {
        clearBlockedBtn.addEventListener('click', function() {
            showBlockedConfirm(t('settings.clearBlockedConfirm'), function() {
            fetch('/api/friends/blocked/clear', {
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

            fetch('/api/friends/blocked').then(function(r) { return r.json(); }).then(function(d) {
                var container = document.getElementById('blockedList');
                if (!d.success || !d.blocked || d.blocked.length === 0) {
                    container.innerHTML = '<p class="empty-hint">' + t('settings.blockedEmpty') + '</p>';
                    return;
                }
                var html = '';
                d.blocked.forEach(function(b) {
                    html += '<div style="display:flex;align-items:center;justify-content:space-between;padding:10px;border-bottom:1px solid #FFD6E8;">' +
                        '<span style="font-family:Consolas,monospace;font-size:14px;color:#FF69B4;letter-spacing:0.5px">' + escapeHtml(b.friendCodeFormatted || '----') + '</span>' +
                        '<button class="btn btn-secondary" style="padding:4px 12px;font-size:12px" data-user-id="' + escAttr(b.userId) + '">' + t('settings.remove') + '</button></div>';
                });
                container.innerHTML = html;
                container.querySelectorAll('button').forEach(function(btn) {
                    btn.addEventListener('click', function() {
                        var uid = this.dataset.userId;
                        fetch('/api/friends/unblock', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                            body: JSON.stringify({ userId: uid })
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

    if (blockedCountEl) loadBlockedCount();
})();
