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
let loadedYggdrasil = {};

(async function loadSettings() {
    try {
        const res = await fetch('/admin/api/yggdrasil/settings');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        currentSettings = await res.json();

        document.getElementById('uuidVersion').value = currentSettings.uuidVersion || 'v4';
        document.getElementById('tokenTempExpiry').value = currentSettings.tokenTempExpiry || 4320;
        document.getElementById('tokenPermanentExpiry').value = currentSettings.tokenPermanentExpiry || 10080;
        document.getElementById('maxTokensPerProfile').value = currentSettings.maxTokensPerProfile || 12;
        document.getElementById('worldTreeKeyLength').value = currentSettings.worldTreeKeyLength || 32;
        document.getElementById('authRateLimit').value = currentSettings.authRateLimit || 1000;
        document.getElementById('batchQueryMaxCount').value = currentSettings.batchQueryMaxCount || 6;
        initialMode = currentSettings.signatureMode || 'ed448';
        document.getElementById('signatureMode').value = initialMode;
        document.getElementById('yggdrasilPublicKey').value = currentSettings.yggdrasilPublicKey || '';
        document.getElementById('yggdrasilPrivateKey').value = currentSettings.yggdrasilPrivateKey || '';
        setApiMode(currentSettings.apiMode || 'default');
        captureLoadedYggdrasil();
        if (window.initAdminSaveCards) { window.initAdminSaveCards(); window.refreshAdminSaveCards(); }
    } catch (err) {
        console.error('Failed to load settings:', err);
        showToast(t('admin.yggdrasil.loadFailed'), 'error');
    }
})();

function setApiMode(mode) {
    var toggle = document.getElementById('apiModeToggle');
    if (!toggle) return;
    var modes = ['default', 'standalone', 'aggregate'];
    var index = modes.indexOf(mode);
    if (index < 0) { mode = 'default'; index = 0; }
    toggle.dataset.value = mode;
    var hidden = document.getElementById('apiModeValue');
    if (hidden && hidden.value !== mode) {
        hidden.value = mode;
        hidden.dispatchEvent(new Event('change', { bubbles: true }));
    }
    toggle.querySelectorAll('.api-mode-btn').forEach(function (btn) {
        btn.classList.toggle('active', btn.dataset.mode === mode);
    });
    var thumb = document.getElementById('apiModeThumb');
    if (thumb) thumb.style.transform = 'translateX(' + (index * 100) + '%)';
}

function selectApiMode(mode) {
    setApiMode(mode);
}

var apiModeValue = document.getElementById('apiModeValue');
if (apiModeValue) {
    apiModeValue.addEventListener('change', function () {
        setApiMode(this.value);
    });
}

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
                loadedYggdrasil['yggdrasil_public_key'] = data.publicKey;
                loadedYggdrasil['yggdrasil_private_key'] = data.privateKey;
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

function yggdrasilValue(id) {
    var el = document.getElementById(id);
    return el ? el.value : '';
}

function captureLoadedYggdrasil() {
    loadedYggdrasil = {
        uuid_version: yggdrasilValue('uuidVersion'),
        api_mode: yggdrasilValue('apiModeValue'),
        token_temp_expiry: yggdrasilValue('tokenTempExpiry'),
        token_permanent_expiry: yggdrasilValue('tokenPermanentExpiry'),
        max_tokens_per_profile: yggdrasilValue('maxTokensPerProfile'),
        world_tree_key_length: yggdrasilValue('worldTreeKeyLength'),
        auth_rate_limit: yggdrasilValue('authRateLimit'),
        batch_query_max_count: yggdrasilValue('batchQueryMaxCount'),
        yggdrasil_public_key: yggdrasilValue('yggdrasilPublicKey'),
        yggdrasil_private_key: yggdrasilValue('yggdrasilPrivateKey')
    };
}

async function saveYggdrasilSection(btn) {
    var scope = btn.closest('.card');
    if (!scope) return false;
    var pairs = [];
    scope.querySelectorAll('[data-ygg-key]').forEach(function(el) {
        pairs.push({ key: el.dataset.yggKey, value: el.value });
    });
    var changed = pairs.filter(function(p) { return p.value !== loadedYggdrasil[p.key]; });
    if (changed.length === 0) { showToast('\u6CA1\u6709\u66F4\u6539', 'info'); return true; }
    for (var i = 0; i < changed.length; i++) {
        try {
            var res = await fetch('/admin/api/yggdrasil/settings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify(changed[i])
            });
            var data = await res.json();
            if (!data.success) {
                showToast(data.message || t('admin.yggdrasil.saveFailed'), 'error');
                return false;
            }
            loadedYggdrasil[changed[i].key] = changed[i].value;
        } catch (err) {
            showToast(t('common.networkError'), 'error');
            return false;
        }
    }
    showToast(t('common.saveSuccess'), 'success');
    return true;
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
                loadedYggdrasil['yggdrasil_public_key'] = data.publicKey;
                loadedYggdrasil['yggdrasil_private_key'] = data.privateKey;
                showToast(data.message, 'success');
            } else {
                showToast(data.message || t('admin.yggdrasil.generateFailed'), 'error');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    });
}


