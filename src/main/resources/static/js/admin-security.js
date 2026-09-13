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
        var data = await res.json();
        currentLevel = data.encryptionLevel || 1;
        var hiddenInput = document.getElementById('encryptionLevel');
        if (hiddenInput) hiddenInput.value = currentLevel;
        renderLevelCards();
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
