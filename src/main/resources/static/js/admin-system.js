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
function showConfirmDialog(message, onConfirm) {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'confirmDialog';
    overlay.innerHTML = '<div class="modal-box"><h3>\u786E\u8BA4\u64CD\u4F5C</h3><p>' + escapeHtml(message) + '</p>' +
        '<div class="modal-actions" id="confirmDialogActions"></div></div>';
    document.body.appendChild(overlay);
    var actionsDiv = overlay.querySelector('#confirmDialogActions');
    var cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.textContent = '\u53D6\u6D88';
    cancelBtn.addEventListener('click', function() { closeDialog(false); });
    actionsDiv.appendChild(cancelBtn);
    var confirmBtn = document.createElement('button');
    confirmBtn.className = 'btn btn-danger';
    confirmBtn.textContent = '\u786E\u8BA4';
    confirmBtn.addEventListener('click', function() { closeDialog(true); });
    actionsDiv.appendChild(confirmBtn);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) closeDialog(false); });
    function closeDialog(confirmed) { overlay.remove(); if (confirmed && onConfirm) onConfirm(); }
}

function escapeHtml(str) {
    if (!str) return '';
    var d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

var initialValues = {};

function getElValue(el) {
    if (!el) return '';
    if (el.type === 'checkbox') return String(el.checked);
    return el.value;
}

function setElValue(el, value) {
    if (!el) return;
    if (el.type === 'checkbox') { el.checked = (value === 'true' || value === true); }
    else el.value = value == null ? '' : String(value);
}

(function() {
    fetch('/admin/api/system/settings').then(function(r) { return r.json(); }).then(function(s) {
        setElValue(document.getElementById('siteName'), s.siteName);
        setElValue(document.getElementById('siteDescription'), s.siteDescription);
        setElValue(document.getElementById('registrationEnabled'), s.registrationEnabled);
        setElValue(document.getElementById('emailVerificationEnabled'), s.emailVerificationEnabled);
        setElValue(document.getElementById('userDomain'), s.userDomain);
        setElValue(document.getElementById('adminDomain'), s.adminDomain);
        setElValue(document.getElementById('apiDomain'), s.apiDomain);
        setElValue(document.getElementById('commonDomain'), s.commonDomain);
        setElValue(document.getElementById('usernameBlacklist'), s.usernameBlacklist);
        setElValue(document.getElementById('usernameBlacklistCaseSensitive'), s.usernameBlacklistCaseSensitive);
        setElValue(document.getElementById('emailDomainList'), s.emailDomainList);
        setElValue(document.getElementById('emailDomainMode'), s.emailDomainMode);
        setElValue(document.getElementById('mailEnabled'), s.mailEnabled);
        setElValue(document.getElementById('mailHost'), s.mailHost);
        setElValue(document.getElementById('mailPort'), s.mailPort);
        setElValue(document.getElementById('mailUsername'), s.mailUsername);
        setElValue(document.getElementById('mailPassword'), '');
        setElValue(document.getElementById('mailFrom'), s.mailFrom);
        setElValue(document.getElementById('mailTls'), s.mailTls);
        setElValue(document.getElementById('icpRecord'), s.icpRecord);
        setElValue(document.getElementById('publicSecurityRecord'), s.publicSecurityRecord);
        setElValue(document.getElementById('skinMaxSize'), s.skinMaxSize);
        setElValue(document.getElementById('skinMaxCount'), s.skinMaxCount);
        setElValue(document.getElementById('skinMaxTotalSize'), s.skinMaxTotalSize);
        setElValue(document.getElementById('skinRateLimit'), s.skinRateLimit);
        setElValue(document.getElementById('skinStoragePath'), s.skinStoragePath);
        setElValue(document.getElementById('capeMaxSize'), s.capeMaxSize);
        setElValue(document.getElementById('capeMaxCount'), s.capeMaxCount);
        setElValue(document.getElementById('capeMaxTotalSize'), s.capeMaxTotalSize);
        setElValue(document.getElementById('capeRateLimit'), s.capeRateLimit);
        setElValue(document.getElementById('capeStoragePath'), s.capeStoragePath);
        setElValue(document.getElementById('allowDownloadSkin'), s.allowDownloadSkin);
        setElValue(document.getElementById('allowDownloadCape'), s.allowDownloadCape);
        setElValue(document.getElementById('maxProfilesPerUser'), s.maxProfilesPerUser);
        setElValue(document.getElementById('maxAccountsPerIp'), s.maxAccountsPerIp);
        setElValue(document.getElementById('maxBlockedUsers'), s.maxBlockedUsers);
        setElValue(document.getElementById('announcementMode'), s.announcementMode || 'off');
        setElValue(document.getElementById('announcementScope'), s.announcementScope || 'user');
        setElValue(document.getElementById('announcementContent'), s.announcementContent || '');

        // Sync announcement scope checkboxes
        var scopeVal = s.announcementScope || 'user';
        document.querySelectorAll('.announcement-scope-check').forEach(function(cb) {
            cb.checked = scopeVal.indexOf(cb.value) >= 0;
            cb.addEventListener('change', function() {
                var parts = [];
                document.querySelectorAll('.announcement-scope-check').forEach(function(b) {
                    if (b.checked) parts.push(b.value);
                });
                document.getElementById('announcementScope').value = parts.join('|');
                document.getElementById('announcementScope').dispatchEvent(new Event('input'));
            });
        });
        document.getElementById('announcementScope').value = scopeVal;

        document.querySelectorAll('[data-setting-key]').forEach(function(el) {
            initialValues[el.dataset.settingKey] = getElValue(el);
        });
    }).catch(function() { showToast('\u52A0\u8F7D\u8BBE\u7F6E\u5931\u8D25', 'error'); });
})();

async function saveSection(btn) {
    var card = btn.closest('.card');
    if (!card) return;
    var inputs = card.querySelectorAll('[data-setting-key]');
    var changes = [];
    inputs.forEach(function(el) {
        var key = el.dataset.settingKey;
        var val = getElValue(el);
        if (val !== initialValues[key]) {
            changes.push({ key: key, value: val });
        }
    });
    if (changes.length === 0) { showToast('\u6CA1\u6709\u66F4\u6539', 'info'); return; }

    var isMail = card.dataset.section === 'mail';
    if (isMail) {
        var mailData = {};
        changes.forEach(function(c) { mailData[c.key] = c.value; });
        var mailPwd = document.getElementById('mailPassword').value;
        if (mailPwd) mailData.password = mailPwd;
        try {
            var res = await fetch('/admin/api/system/mail', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify(mailData)
            });
            var d = await res.json();
            if (!d.success) { showToast(d.message || '\u4FDD\u5B58\u5931\u8D25', 'error'); return; }
        } catch (err) { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); return; }
    } else {
        for (var i = 0; i < changes.length; i++) {
            try {
                var res = await fetch('/admin/api/system/settings', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify(changes[i])
                });
                var d = await res.json();
                if (!d.success) { showToast(d.message || '\u4FDD\u5B58\u5931\u8D25', 'error'); return; }
            } catch (err) { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); return; }
        }
    }

    changes.forEach(function(c) { initialValues[c.key] = c.value; });
    showToast('\u4FDD\u5B58\u6210\u529F', 'success');
}

async function clearCache() {
    showConfirmDialog('\u786E\u5B9A\u8981\u6E05\u9664\u6240\u6709\u7F13\u5B58\u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u64A4\u9500\u3002', async function() {
        try {
            var res = await fetch('/admin/api/system/cache/clear', { method: 'POST', headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') } });
            var data = await res.json();
            showToast(data.message || '\u7F13\u5B58\u5DF2\u6E05\u9664', data.success ? 'success' : 'error');
        } catch (err) { showToast('\u6E05\u9664\u7F13\u5B58\u5931\u8D25', 'error'); }
    });
}

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';
    setTimeout(function() { toast.style.display = 'none'; }, 3000);
}
