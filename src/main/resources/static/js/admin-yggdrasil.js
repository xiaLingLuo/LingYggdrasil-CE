function escapeHtml(str) {
    if (!str) return '';
    var d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

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

let currentSettings = {};

(async function loadSettings() {
    try {
        const res = await fetch('/admin/api/yggdrasil/settings');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        currentSettings = await res.json();

        document.getElementById('uuidVersion').value = currentSettings.uuidVersion || 'v4';
        document.getElementById('tokenTempExpiry').value = currentSettings.tokenTempExpiry || 4320;
        document.getElementById('tokenPermanentExpiry').value = currentSettings.tokenPermanentExpiry || 10080;
        document.getElementById('maxTokensPerProfile').value = currentSettings.maxTokensPerProfile || 12;
        document.getElementById('authRateLimit').value = currentSettings.authRateLimit || 1000;
        document.getElementById('batchQueryMaxCount').value = currentSettings.batchQueryMaxCount || 6;
        document.getElementById('signatureMode').value = currentSettings.signatureMode || 'ed448';
        document.getElementById('yggdrasilPublicKey').value = currentSettings.yggdrasilPublicKey || '';
        document.getElementById('yggdrasilPrivateKey').value = currentSettings.yggdrasilPrivateKey || '';
    } catch (err) {
        console.error('加载设置失败:', err);
        showToast('加载设置失败', 'error');
    }
})();

async function saveSettings() {
    const settings = [
        { key: 'uuid_version', value: document.getElementById('uuidVersion').value },
        { key: 'token_temp_expiry', value: document.getElementById('tokenTempExpiry').value },
        { key: 'token_permanent_expiry', value: document.getElementById('tokenPermanentExpiry').value },
        { key: 'max_tokens_per_profile', value: document.getElementById('maxTokensPerProfile').value },
        { key: 'auth_rate_limit', value: document.getElementById('authRateLimit').value },
        { key: 'batch_query_max_count', value: document.getElementById('batchQueryMaxCount').value },
        { key: 'yggdrasil_public_key', value: document.getElementById('yggdrasilPublicKey').value },
        { key: 'yggdrasil_private_key', value: document.getElementById('yggdrasilPrivateKey').value }
    ];

    try {
        for (const s of settings) {
            const res = await fetch('/admin/api/yggdrasil/settings', {
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
        showToast('所有设置已保存', 'success');
    } catch (err) {
        showToast('网络错误，请稍后重试', 'error');
    }
}

async function regenerateKeys() {
    showConfirmDialog('确定要重新生成密钥对吗？旧密钥将立即失效，所有客户端需要重新获取公钥。', async function() {
        try {
            const res = await fetch('/admin/api/yggdrasil/regenerate-keys', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
            });
            const data = await res.json();
            if (data.success) {
                document.getElementById('yggdrasilPublicKey').value = data.publicKey;
                document.getElementById('yggdrasilPrivateKey').value = data.privateKey;
                showToast(data.message, 'success');
            } else {
                showToast(data.message || '生成失败', 'error');
            }
        } catch (err) {
            showToast('网络错误', 'error');
        }
    });
}

async function switchMode() {
    var currentMode = document.getElementById('signatureMode').value;
    var newMode = currentMode === 'ed448' ? 'rsa-sha512' : 'ed448';
    var modeName = newMode === 'ed448' ? '现代模式 (Ed448)' : '兼容模式 (RSA-SHA512)';

    showConfirmDialog('确定要切换到 ' + modeName + ' 吗？切换后将立即重新生成密钥对，旧密钥将失效。', async function() {
        try {
            const res = await fetch('/admin/api/yggdrasil/switch-mode', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ mode: newMode })
            });
            const data = await res.json();
            if (data.success) {
                document.getElementById('signatureMode').value = newMode;
                document.getElementById('yggdrasilPublicKey').value = data.publicKey;
                document.getElementById('yggdrasilPrivateKey').value = data.privateKey;
                showToast(data.message, 'success');
            } else {
                showToast(data.message || '切换失败', 'error');
            }
        } catch (err) {
            showToast('网络错误', 'error');
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
