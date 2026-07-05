function showConfirmDialog(message, onConfirm) {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'confirmDialog';
    overlay.innerHTML = '<div class="modal-box">' +
        '<h3>确认操作</h3>' +
        '<p>' + escapeHtml(message) + '</p>' +
        '<div class="modal-actions" id="confirmDialogActions"></div></div>';
    document.body.appendChild(overlay);
    window._confirmCallback = onConfirm;
    var actionsDiv = overlay.querySelector('#confirmDialogActions');
    var cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.textContent = '取消';
    cancelBtn.addEventListener('click', function() { closeConfirmDialog(false); });
    actionsDiv.appendChild(cancelBtn);
    var confirmBtn = document.createElement('button');
    confirmBtn.className = 'btn btn-danger';
    confirmBtn.textContent = '确认';
    confirmBtn.addEventListener('click', function() { closeConfirmDialog(true); });
    actionsDiv.appendChild(confirmBtn);
    overlay.addEventListener('click', function(e) {
        if (e.target === overlay) closeConfirmDialog(false);
    });
}

function closeConfirmDialog(confirmed) {
    var dialog = document.getElementById('confirmDialog');
    if (dialog) dialog.remove();
    if (confirmed && window._confirmCallback) {
        window._confirmCallback();
        window._confirmCallback = null;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    var d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

let currentSettings = {};

(async function loadSettings() {
    try {
        const res = await fetch('/admin/api/system/settings');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        currentSettings = await res.json();

        document.getElementById('siteName').value = currentSettings.siteName || '';
        document.getElementById('siteDescription').value = currentSettings.siteDescription || '';
        document.getElementById('registrationEnabled').checked = !!currentSettings.registrationEnabled;
        document.getElementById('emailVerificationEnabled').checked = !!currentSettings.emailVerificationEnabled;

        document.getElementById('userDomain').value = currentSettings.userDomain || '';
        document.getElementById('adminDomain').value = currentSettings.adminDomain || '';
        document.getElementById('apiDomain').value = currentSettings.apiDomain || '';
        document.getElementById('commonDomain').value = currentSettings.commonDomain || '';

        document.getElementById('usernameBlacklist').value = currentSettings.usernameBlacklist || '';
        document.getElementById('usernameBlacklistCaseSensitive').checked = !!currentSettings.usernameBlacklistCaseSensitive;
        document.getElementById('emailDomainList').value = currentSettings.emailDomainList || '';
        document.getElementById('emailDomainMode').value = currentSettings.emailDomainMode || 'blacklist';

        document.getElementById('mailEnabled').checked = !!currentSettings.mailEnabled;
        document.getElementById('mailHost').value = currentSettings.mailHost || '';
        document.getElementById('mailPort').value = currentSettings.mailPort || '';
        document.getElementById('mailUsername').value = currentSettings.mailUsername || '';
        document.getElementById('mailPassword').value = '';
        document.getElementById('mailFrom').value = currentSettings.mailFrom || '';
        document.getElementById('mailTls').checked = !!currentSettings.mailTls;

        document.getElementById('icpRecord').value = currentSettings.icpRecord || '';
        document.getElementById('publicSecurityRecord').value = currentSettings.publicSecurityRecord || '';

        document.getElementById('skinMaxSize').value = currentSettings.skinMaxSize || '';
        document.getElementById('skinMaxCount').value = currentSettings.skinMaxCount || '';
        document.getElementById('skinMaxTotalSize').value = currentSettings.skinMaxTotalSize || '';
        document.getElementById('skinRateLimit').value = currentSettings.skinRateLimit || '';
        document.getElementById('skinStoragePath').value = currentSettings.skinStoragePath || '';
        document.getElementById('capeMaxSize').value = currentSettings.capeMaxSize || '';
        document.getElementById('capeMaxCount').value = currentSettings.capeMaxCount || '';
        document.getElementById('capeMaxTotalSize').value = currentSettings.capeMaxTotalSize || '';
        document.getElementById('capeRateLimit').value = currentSettings.capeRateLimit || '';
        document.getElementById('capeStoragePath').value = currentSettings.capeStoragePath || '';
        document.getElementById('allowDownloadSkin').checked = !!currentSettings.allowDownloadSkin;
        document.getElementById('allowDownloadCape').checked = !!currentSettings.allowDownloadCape;

        document.getElementById('maxProfilesPerUser').value = currentSettings.maxProfilesPerUser || 10;
        document.getElementById('maxAccountsPerIp').value = currentSettings.maxAccountsPerIp || 3;
    } catch (err) {
        console.error('加载设置失败:', err);
        showToast('加载设置失败', 'error');
    }
})();

async function saveSettings() {
    const settings = [
        { key: 'site_name', value: document.getElementById('siteName').value.trim() },
        { key: 'site_description', value: document.getElementById('siteDescription').value.trim() },
        { key: 'registration_enabled', value: String(document.getElementById('registrationEnabled').checked) },
        { key: 'email_verification_enabled', value: String(document.getElementById('emailVerificationEnabled').checked) },
        { key: 'user_domain', value: document.getElementById('userDomain').value.trim() },
        { key: 'admin_domain', value: document.getElementById('adminDomain').value.trim() },
        { key: 'api_domain', value: document.getElementById('apiDomain').value.trim() },
        { key: 'common_domain', value: document.getElementById('commonDomain').value.trim() },
        { key: 'username_blacklist', value: document.getElementById('usernameBlacklist').value },
        { key: 'username_blacklist_case_sensitive', value: String(document.getElementById('usernameBlacklistCaseSensitive').checked) },
        { key: 'email_domain_list', value: document.getElementById('emailDomainList').value },
        { key: 'email_domain_mode', value: document.getElementById('emailDomainMode').value },
        { key: 'icp_record', value: document.getElementById('icpRecord').value.trim() },
        { key: 'public_security_record', value: document.getElementById('publicSecurityRecord').value.trim() },
        { key: 'skin_max_size', value: String(document.getElementById('skinMaxSize').value.trim() || '64') },
        { key: 'skin_max_count', value: String(document.getElementById('skinMaxCount').value.trim() || '10') },
        { key: 'skin_max_total_size', value: String(document.getElementById('skinMaxTotalSize').value.trim() || '640') },
        { key: 'skin_rate_limit', value: String(document.getElementById('skinRateLimit').value.trim() || '24') },
        { key: 'skin_storage_path', value: document.getElementById('skinStoragePath').value.trim() || 'skins' },
        { key: 'cape_max_size', value: String(document.getElementById('capeMaxSize').value.trim() || '64') },
        { key: 'cape_max_count', value: String(document.getElementById('capeMaxCount').value.trim() || '10') },
        { key: 'cape_max_total_size', value: String(document.getElementById('capeMaxTotalSize').value.trim() || '640') },
        { key: 'cape_rate_limit', value: String(document.getElementById('capeRateLimit').value.trim() || '24') },
        { key: 'cape_storage_path', value: document.getElementById('capeStoragePath').value.trim() || 'capes' },
        { key: 'allow_download_skin', value: String(document.getElementById('allowDownloadSkin').checked) },
        { key: 'allow_download_cape', value: String(document.getElementById('allowDownloadCape').checked) },
        { key: 'max_profiles_per_user', value: String(document.getElementById('maxProfilesPerUser').value.trim() || '10') },
        { key: 'max_accounts_per_ip', value: String(document.getElementById('maxAccountsPerIp').value.trim() || '3') }
    ];

    try {
        for (const s of settings) {
            const res = await fetch('/admin/api/system/settings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify(s)
            });
            const data = await res.json();
            if (!data.success) {
                showToast(data.message || '保存失败', 'error');
                return;
            }
        }

        const mailData = {
            enabled: String(document.getElementById('mailEnabled').checked),
            host: document.getElementById('mailHost').value.trim(),
            port: document.getElementById('mailPort').value.trim() || '0',
            username: document.getElementById('mailUsername').value.trim(),
            from: document.getElementById('mailFrom').value.trim(),
            tls: String(document.getElementById('mailTls').checked)
        };
        const mailPwd = document.getElementById('mailPassword').value;
        if (mailPwd) mailData.password = mailPwd;

        const mailRes = await fetch('/admin/api/system/mail', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify(mailData)
        });
        const mailResult = await mailRes.json();
        if (!mailResult.success) {
            showToast(mailResult.message || '邮箱配置保存失败', 'error');
            return;
        }

        showToast('所有设置已保存', 'success');
    } catch (err) {
        showToast('网络错误，请稍后重试', 'error');
    }
}

async function clearCache() {
    showConfirmDialog('确定要清除所有缓存吗？此操作不可撤销。', async function() {
        try {
            const res = await fetch('/admin/api/system/cache/clear', { method: 'POST', headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') } });
            const data = await res.json();
            showToast(data.message || '缓存已清除', data.success ? 'success' : 'error');
        } catch (err) {
            showToast('清除缓存失败', 'error');
        }
    });
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, 3000);
}
