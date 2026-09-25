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
const ENCRYPTION_LEVELS = [
    { level: 1, nameKey: 'admin.security.level1', color: '#3B82F6', bg: null, bold: false, memory: 65536, iterations: 3, parallelism: 1, hash: 32, salt: 32 },
    { level: 2, nameKey: 'admin.security.level2', color: '#EF4444', bg: null, bold: false, memory: 131072, iterations: 3, parallelism: 2, hash: 32, salt: 32 },
    { level: 3, nameKey: 'admin.security.level3', color: '#EAB308', bg: null, bold: false, memory: 262144, iterations: 3, parallelism: 3, hash: 32, salt: 32 },
    { level: 4, nameKey: 'admin.security.level4', color: '#A855F7', bg: null, bold: false, memory: 524288, iterations: 3, parallelism: 4, hash: 32, salt: 32 },
    { level: 5, nameKey: 'admin.security.level5', color: '#22C55E', bg: null, bold: false, memory: 1048576, iterations: 3, parallelism: 6, hash: 32, salt: 32 },
    { level: 6, nameKey: 'admin.security.level6', color: '#FFFFFF', bg: '#66CCFF', bold: true, memory: 2097152, iterations: 3, parallelism: 8, hash: 48, salt: 48 }
];

let currentLevel = 1;
var originalRootUsername = null;

function renderLevelCards() {
    var container = document.getElementById('encryptionLevelList');
    if (!container) return;

    var descriptions = {
        1: t('admin.security.desc1'),
        2: t('admin.security.desc2'),
        3: t('admin.security.desc3'),
        4: t('admin.security.desc4'),
        5: t('admin.security.desc5'),
        6: t('admin.security.desc6')
    };

    var html = '';
    for (var i = 0; i < ENCRYPTION_LEVELS.length; i++) {
        var info = ENCRYPTION_LEVELS[i];
        var isSelected = (info.level === currentLevel);
        var accent = info.bg || info.color;
        var levelName = t(info.nameKey);
        var nameHtml = info.bg
            ? '<span class="level-name-chip" style="background:' + info.bg + ';color:' + info.color + '">' + levelName + '</span>'
            : '<span class="level-name" style="color:' + info.color + '">' + levelName + '</span>';
        var warningHtml = info.level >= 5
            ? '<div class="level-warning"><i class="fas fa-triangle-exclamation"></i> ' + t('admin.security.perfWarning') + '</div>'
            : '';

        html += '<div class="level-card' + (isSelected ? ' active' : '') + '" data-level="' + info.level + '" style="--level-color:' + accent + '">' +
            '<div class="level-card-head">' +
            '<span class="level-radio"></span>' +
            '<div class="level-card-main">' +
            nameHtml +
            '<div class="level-desc">' + descriptions[info.level] + '</div>' +
            warningHtml +
            '</div></div></div>';
    }
    container.innerHTML = html;

    container.querySelectorAll('.level-card').forEach(function(card) {
        card.addEventListener('click', function() {
            selectLevel(parseInt(this.dataset.level, 10));
        });
    });
}

function selectLevel(level) {
    currentLevel = level;
    var hiddenInput = document.getElementById('encryptionLevel');
    if (hiddenInput) hiddenInput.value = level;
    renderLevelCards();
}

(async function loadSettings() {
    try {
        var res = await fetch('/admin/api/security/settings');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        if (!res.ok) throw new Error('settings request failed');
        var data = await res.json();
        currentLevel = data.encryptionLevel || 1;
        var hiddenInput = document.getElementById('encryptionLevel');
        if (hiddenInput) hiddenInput.value = currentLevel;
        document.getElementById('pngValidationEnabled').checked = data.pngValidationEnabled !== false;
        document.getElementById('pngMaxWidth').value = data.pngMaxWidth || 4096;
        document.getElementById('pngMaxHeight').value = data.pngMaxHeight || 4096;
        document.getElementById('pngMaxPixels').value = data.pngMaxPixels || 1048576;
        document.getElementById('pngMaxChunkSizeKib').value = data.pngMaxChunkSizeKib || 1024;
        document.getElementById('pngStrictChunkMode').checked = data.pngStrictChunkMode !== false;
        document.getElementById('pngMaxConcurrent').value = data.pngMaxConcurrent || 4;
        setValue('userSessionTimeoutSeconds', data.userSessionTimeoutSeconds);
        setValue('adminSessionTimeoutSeconds', data.adminSessionTimeoutSeconds);
        setValue('loginMaxAttemptsPerIp', data.loginMaxAttemptsPerIp);
        setValue('loginMaxAttemptsPerAccount', data.loginMaxAttemptsPerAccount);
        setValue('loginLockoutSeconds', data.loginLockoutSeconds);
        setValue('loginRateWindowSeconds', data.loginRateWindowSeconds);
        setValue('requestIntervals', data.requestIntervals);
        setValue('requestRates', data.requestRates);
        setValue('corsOrigins', data.corsOrigins);
        setValue('headerCsp', data.headerCsp);
        setValue('headerHsts', data.headerHsts);
        setValue('headerContentTypeOptions', data.headerContentTypeOptions);
        setValue('headerFrameOptions', data.headerFrameOptions);
        setValue('headerXssProtection', data.headerXssProtection);
        setValue('headerReferrerPolicy', data.headerReferrerPolicy);
        setValue('headerPermissionsPolicy', data.headerPermissionsPolicy);
        setValue('headerCacheControl', data.headerCacheControl);
        renderLevelCards();
        var rootUsernameInput = document.getElementById('rootNewUsername');
        if (rootUsernameInput) {
            var rootRes = await fetch('/admin/api/security/root');
            if (rootRes.status === 401) { window.location.href = '/admin/login'; return; }
            if (!rootRes.ok) throw new Error('root settings request failed');
            var rootData = await rootRes.json();
            rootUsernameInput.value = rootData.username || '';
            originalRootUsername = rootUsernameInput.value;
        }
    } catch (err) {
        console.error('Failed to load settings:', err);
        showToast(t('admin.security.loadFailed'), 'error');
    }
})();

async function saveSettings() {
    var level = document.getElementById('encryptionLevel').value;
    try {
        var res = await fetch('/admin/api/security/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify({ key: 'encryption_level', value: level })
        });
        var data = await res.json();
        if (!data.success) {
            showToast(data.message || t('admin.security.saveFailed'), 'error');
            return false;
        }
        showToast(t('admin.security.saved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}

async function savePngSettings() {
    var form = document.getElementById('pngUploadSettings');
    var inputs = form.querySelectorAll('input[type="number"]');
    for (var i = 0; i < inputs.length; i++) {
        if (!inputs[i].reportValidity()) return false;
    }

    var settings = {
        key: 'png_upload_settings',
        pngValidationEnabled: document.getElementById('pngValidationEnabled').checked,
        pngMaxWidth: parseInt(document.getElementById('pngMaxWidth').value, 10),
        pngMaxHeight: parseInt(document.getElementById('pngMaxHeight').value, 10),
        pngMaxPixels: parseInt(document.getElementById('pngMaxPixels').value, 10),
        pngMaxChunkSizeKib: parseInt(document.getElementById('pngMaxChunkSizeKib').value, 10),
        pngMaxConcurrent: parseInt(document.getElementById('pngMaxConcurrent').value, 10),
        pngStrictChunkMode: document.getElementById('pngStrictChunkMode').checked
    };

    try {
        var res = await fetch('/admin/api/security/settings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify(settings)
        });
        var data = await res.json();
        if (!data.success) {
            showToast(data.message || t('admin.security.saveFailed'), 'error');
            return false;
        }
        showToast(t('admin.security.saved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}

function setValue(id, value) {
    var el = document.getElementById(id);
    if (el) el.value = value === null || value === undefined ? '' : value;
}

function numValue(id) {
    return parseInt(document.getElementById(id).value, 10);
}

function textValue(id) {
    var el = document.getElementById(id);
    return el ? el.value : '';
}

async function postSecuritySettings(payload) {
    var res = await fetch('/admin/api/security/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
        body: JSON.stringify(payload)
    });
    return res.json();
}

async function saveFrequencySettings() {
    var form = document.getElementById('frequencySettings');
    var inputs = form.querySelectorAll('input[type="number"]');
    for (var i = 0; i < inputs.length; i++) {
        if (!inputs[i].reportValidity()) return false;
    }
    var settings = {
        key: 'frequency_settings',
        userSessionTimeoutSeconds: numValue('userSessionTimeoutSeconds'),
        adminSessionTimeoutSeconds: numValue('adminSessionTimeoutSeconds'),
        loginMaxAttemptsPerIp: numValue('loginMaxAttemptsPerIp'),
        loginMaxAttemptsPerAccount: numValue('loginMaxAttemptsPerAccount'),
        loginLockoutSeconds: numValue('loginLockoutSeconds'),
        loginRateWindowSeconds: numValue('loginRateWindowSeconds'),
        requestIntervals: textValue('requestIntervals'),
        requestRates: textValue('requestRates')
    };
    try {
        var data = await postSecuritySettings(settings);
        if (!data.success) {
            showToast(data.message || t('admin.security.saveFailed'), 'error');
            return false;
        }
        showToast(t('admin.security.saved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}

async function saveCorsSettings() {
    var settings = {
        key: 'cors_security_settings',
        corsOrigins: textValue('corsOrigins'),
        headerCsp: textValue('headerCsp'),
        headerHsts: textValue('headerHsts'),
        headerContentTypeOptions: textValue('headerContentTypeOptions'),
        headerFrameOptions: textValue('headerFrameOptions'),
        headerXssProtection: textValue('headerXssProtection'),
        headerReferrerPolicy: textValue('headerReferrerPolicy'),
        headerPermissionsPolicy: textValue('headerPermissionsPolicy'),
        headerCacheControl: textValue('headerCacheControl')
    };
    try {
        var data = await postSecuritySettings(settings);
        if (!data.success) {
            showToast(data.message || t('admin.security.saveFailed'), 'error');
            return false;
        }
        showToast(t('admin.security.saved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}

function confirmCorsSettings(btn) {
    showConfirmDialog(t('admin.security.corsConfirm'), function () {
        var state = startBtnLoading(btn);
        saveCorsSettings().then(function (ok) {
            finishBtnLoading(state, ok !== false);
        }, function () {
            finishBtnLoading(state, false);
        });
    });
}

async function saveRootSettings() {
    var usernameInput = document.getElementById('rootNewUsername');
    var currentPasswordInput = document.getElementById('rootCurrentPassword');
    if (!usernameInput.reportValidity() || !currentPasswordInput.reportValidity()) return false;
    if (usernameInput.value !== originalRootUsername) {
        if (usernameInput.value.length < 3 || usernameInput.value.length > 32) {
            showToast(t('msg.usernameLength'), 'error');
            return false;
        }
        if (!/^[A-Za-z0-9_]+$/.test(usernameInput.value)) {
            showToast(t('msg.usernameCharset'), 'error');
            return false;
        }
    }

    var settings = {
        newUsername: usernameInput.value,
        currentPassword: currentPasswordInput.value,
        newPassword: document.getElementById('rootNewPassword').value,
        confirmPassword: document.getElementById('rootConfirmPassword').value
    };
    try {
        var res = await fetch('/admin/api/security/root', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify(settings)
        });
        if (res.status === 401) { window.location.href = '/admin/login'; return false; }
        var data = await res.json();
        if (!data.success) {
            showToast(data.message || t('admin.security.saveFailed'), 'error');
            return false;
        }
        usernameInput.value = data.username || settings.newUsername;
        originalRootUsername = usernameInput.value;
        var sidebarUsername = document.querySelector('.sidebar-username');
        if (sidebarUsername) sidebarUsername.textContent = usernameInput.value;
        currentPasswordInput.value = '';
        document.getElementById('rootNewPassword').value = '';
        document.getElementById('rootConfirmPassword').value = '';
        showToast(t('admin.security.saved'), 'success');
        return true;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return false;
    }
}
