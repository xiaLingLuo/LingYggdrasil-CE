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
let currentSettings = {};
let initialMode = '';

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
        initialMode = currentSettings.signatureMode || 'ed448';
        document.getElementById('signatureMode').value = initialMode;
        document.getElementById('yggdrasilPublicKey').value = currentSettings.yggdrasilPublicKey || '';
        document.getElementById('yggdrasilPrivateKey').value = currentSettings.yggdrasilPrivateKey || '';
    } catch (err) {
        console.error('Failed to load settings:', err);
        showToast(t('admin.yggdrasil.loadFailed'), 'error');
    }
})();

function onModeChange() {
    var current = document.getElementById('signatureMode').value;
    var btn = document.getElementById('switchModeBtn');
    if (current !== initialMode) {
        btn.style.display = 'inline-block';
    } else {
        btn.style.display = 'none';
    }
}

function confirmSwitchMode() {
    var newMode = document.getElementById('signatureMode').value;
    var modeNames = {
        'ed448': t('admin.yggdrasil.signModeEd448'),
        'rsa-sha512': t('admin.yggdrasil.signModeRsaSha512'),
        'rsa-sha1': t('admin.yggdrasil.signModeRsaSha1')
    };
    var modeName = modeNames[newMode] || newMode;

    showConfirmDialog(t('admin.yggdrasil.switchConfirm', modeName), async function() {
        try {
            const res = await fetch('/admin/api/yggdrasil/switch-mode', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ mode: newMode })
            });
            const data = await res.json();
            if (data.success) {
                initialMode = newMode;
                document.getElementById('signatureMode').value = newMode;
                document.getElementById('switchModeBtn').style.display = 'none';
                document.getElementById('yggdrasilPublicKey').value = data.publicKey;
                document.getElementById('yggdrasilPrivateKey').value = data.privateKey;
                showToast(data.message, 'success');
            } else {
                document.getElementById('signatureMode').value = initialMode;
                showToast(data.message || t('admin.yggdrasil.switchFailed'), 'error');
            }
        } catch (err) {
            document.getElementById('signatureMode').value = initialMode;
            showToast(t('common.networkError'), 'error');
        }
    });
}

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
                showToast(data.message || t('admin.yggdrasil.saveFailed'), 'error');
                return false;
            }
        }
        showToast(t('admin.yggdrasil.allSaved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}

async function regenerateKeys() {
    showConfirmDialog(t('admin.yggdrasil.regenerateConfirm'), async function() {
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
                showToast(data.message || t('admin.yggdrasil.generateFailed'), 'error');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    });
}


