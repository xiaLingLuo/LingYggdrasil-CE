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
    { level: 1, name: '电磁加密', color: '#3B82F6', bg: null, bold: false, memory: 65536, iterations: 3, parallelism: 1, hash: 32, salt: 32 },
    { level: 2, name: '能量加密', color: '#EF4444', bg: null, bold: false, memory: 131072, iterations: 3, parallelism: 2, hash: 32, salt: 32 },
    { level: 3, name: '结构加密', color: '#EAB308', bg: null, bold: false, memory: 262144, iterations: 3, parallelism: 3, hash: 32, salt: 32 },
    { level: 4, name: '信息加密', color: '#A855F7', bg: null, bold: false, memory: 524288, iterations: 3, parallelism: 4, hash: 32, salt: 32 },
    { level: 5, name: '引力加密', color: '#22C55E', bg: null, bold: false, memory: 1048576, iterations: 3, parallelism: 6, hash: 32, salt: 32 },
    { level: 6, name: '宇宙加密', color: '#FFFFFF', bg: '#66CCFF', bold: true, memory: 2097152, iterations: 3, parallelism: 8, hash: 48, salt: 48 }
];

let currentLevel = 1;

function renderLevelCards() {
    var container = document.getElementById('encryptionLevelList');
    if (!container) return;
    var html = '';
    for (var i = 0; i < ENCRYPTION_LEVELS.length; i++) {
        var info = ENCRYPTION_LEVELS[i];
        var isSelected = (info.level === currentLevel);
        var memDisplay = info.memory >= 1048576
            ? (info.memory / 1048576) + ' GB'
            : (info.memory / 1024) + ' MB';

        var cardBorder = isSelected ? '2px solid ' + (info.bg || info.color) : '2px solid #e5e7eb';
        var cardBg = isSelected ? (info.bg ? info.bg + '1A' : info.color + '0D') : '#fff';
        var cardStyle = 'border:' + cardBorder + ';background:' + cardBg + ';border-radius:10px;padding:16px;cursor:pointer;transition:all 0.2s;margin-bottom:10px;';

        var nameStyle = 'font-size:16px;font-weight:' + (info.bold ? 'bold' : '600') + ';color:' + info.color + ';';
        if (info.bg) {
            nameStyle += 'background:' + info.bg + ';padding:4px 12px;border-radius:6px;display:inline-block;';
        }

        var radioOuter = 'width:18px;height:18px;border-radius:50%;border:2px solid ' + (isSelected ? (info.bg || info.color) : '#ccc') + ';display:flex;align-items:center;justify-content:center;flex-shrink:0;';
        var radioInner = isSelected
            ? 'width:10px;height:10px;border-radius:50%;background:' + (info.bg || info.color) + ';'
            : '';

        var descriptions = {
            1: '基本的加密安全等级，兼具安全性和性能，对机器性能占用极低',
            2: '增强的加密安全等级，安全性更高，对机器性能占用相对电磁加密更高',
            3: '引入钛晶体，使安全系数进一步提升，但也需求更高的服务器性能',
            4: '引入了粒子宽带，控制粒子让其维持一定的规律性，但对服务器性能要求更高',
            5: '引入了奇异物质和量子计算，在某种程度上使量子场论和广义相对论达到了统一，安全性极高，需要极强的服务器性能。',
            6: '将' +
                '<span style="color: #3B82F6;font-weight: bold;">电磁</span>、' +
                '<span style="color: #EF4444;font-weight: bold;">能量</span>、' +
                '<span style="color: #EAB308;font-weight: bold;">结构</span>、' +
                '<span style="color: #A855F7;font-weight: bold;">信息</span>、' +
                '<span style="color: #22C55E;font-weight: bold;">引力</span>' +
                '的力量结合在一起，利用' +
                '<span style="color: #000000;font-weight: bold;">反物质</span>' +
                '催化，是万有理论的终极结晶，安全性几乎无可战胜，需要无比强悍的服务器。'
        };
        var paramsHtml = '<div style="margin-top:8px;font-size:13px;color:#666;">' + descriptions[info.level] + '</div>';

        var warningHtml = '';
        if (info.level >= 5) {
            warningHtml = '<div style="margin-top:6px;font-size:12px;color:#856404;"><i class="fas fa-triangle-exclamation"></i> 对服务器性能开销极大，配置要求极高！</div>';
        }

        html += '<div style="' + cardStyle + '" data-level="' + info.level + '" class="level-card">' +
            '<div style="display:flex;align-items:center;gap:12px;">' +
            '<div style="' + radioOuter + '"><div style="' + radioInner + '"></div></div>' +
            '<div style="flex:1;">' +
            '<div style="' + nameStyle + '">' + info.name + '</div>' +
            paramsHtml +
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
        console.error('加载设置失败:', err);
        showToast('加载设置失败', 'error');
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
            showToast(data.message || '保存失败', 'error');
            return;
        }
        showToast('设置已保存', 'success');
    } catch (err) {
        showToast('网络错误，请稍后重试', 'error');
    }
}

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';
    setTimeout(function() { toast.style.display = 'none'; }, 3000);
}
