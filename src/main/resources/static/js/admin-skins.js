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
    overlay.innerHTML = '<div class="modal-box"><h3>\u786E\u8BA4\u64CD\u4F5C</h3><p>' + esc(message) + '</p>' +
        '<div class="modal-actions" id="confirmDialogActions"></div></div>';
    document.body.appendChild(overlay);
    var a = overlay.querySelector('#confirmDialogActions');
    var cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.textContent = '\u53D6\u6D88';
    cancelBtn.addEventListener('click', function() { overlay.remove(); });
    a.appendChild(cancelBtn);
    var confirmBtn = document.createElement('button');
    confirmBtn.className = 'btn btn-danger';
    confirmBtn.textContent = '\u786E\u8BA4';
    confirmBtn.addEventListener('click', function() { overlay.remove(); if (onConfirm) onConfirm(); });
    a.appendChild(confirmBtn);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
}

function esc(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

var allSkins = [];

(async function loadSkins() {
    var res = await fetch('/admin/api/skins');
    if (res.status === 401) { window.location.href = '/admin/login'; return; }
    var data = await res.json();
    allSkins = (data.success && data.textures) ? data.textures : [];
    renderSkins(allSkins);
})();

function renderSkins(skins) {
    var tbody = document.getElementById('skinTableBody');
    if (!skins || skins.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">\u6682\u65E0\u76AE\u80A4\u6570\u636E</td></tr>';
        return;
    }
    tbody.innerHTML = skins.map(function(s) {
        var displayName = s.adminAlias || s.originalName || '-';
        var hashShort = s.hash ? s.hash.substring(0, 12) + '...' : '-';
        return '<tr>' +
            '<td style="font-family:Consolas,monospace;font-size:12px" title="' + esc(s.hash) + '">' + esc(hashShort) + '</td>' +
            '<td>' + esc(displayName) + '</td>' +
            '<td>' + formatSize(s.size) + '</td>' +
            '<td>' + esc(String(s.refCount)) + '</td>' +
            '<td>' + formatDate(s.createdAt) + '</td>' +
            '<td><div class="action-btns">' +
            '<button class="btn-action btn-edit act-download" data-hash="' + esc(s.hash) + '">\u4E0B\u8F7D</button>' +
            '<button class="btn-action btn-edit act-alias" data-hash="' + esc(s.hash) + '" data-alias="' + esc(s.adminAlias || '') + '">\u4FEE\u6539\u522B\u540D</button>' +
            '<button class="btn-action btn-delete act-delete" data-hash="' + esc(s.hash) + '">\u5220\u9664</button>' +
            '</div></td></tr>';
    }).join('');
}

document.getElementById('skinTableBody').addEventListener('click', function(e) {
    var btn = e.target.closest('button');
    if (!btn) return;
    var hash = btn.dataset.hash;
    if (btn.classList.contains('act-download')) {
        window.open('/admin/api/skins/download?hash=' + encodeURIComponent(hash), '_blank');
    } else if (btn.classList.contains('act-alias')) {
        showAliasModal(hash, btn.dataset.alias || '');
    } else if (btn.classList.contains('act-delete')) {
        deleteSkin(hash);
    }
});

function showAliasModal(hash, currentAlias) {
    var existing = document.getElementById('aliasModal');
    if (existing) existing.remove();
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'aliasModal';
    overlay.innerHTML =
        '<div class="modal-box">' +
        '<button class="modal-close-btn" id="aliasCloseBtn">&times;</button>' +
        '<h3>\u4FEE\u6539\u522B\u540D</h3>' +
        '<div class="form-group"><label class="form-label">\u65B0\u522B\u540D</label>' +
        '<input type="text" class="form-input" id="newAlias" value="' + esc(currentAlias) + '"></div>' +
        '<div id="aliasMsg" class="msg-area"></div>' +
        '<div class="modal-actions">' +
        '<button class="btn btn-secondary" id="aliasCancelBtn">\u53D6\u6D88</button>' +
        '<button class="btn btn-primary" id="aliasSaveBtn">\u4FDD\u5B58</button></div></div>';
    document.body.appendChild(overlay);
    document.getElementById('aliasCloseBtn').addEventListener('click', function() { overlay.remove(); });
    document.getElementById('aliasCancelBtn').addEventListener('click', function() { overlay.remove(); });
    document.getElementById('aliasSaveBtn').addEventListener('click', function() { submitAlias(hash); });
    overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
}

async function submitAlias(hash) {
    var alias = document.getElementById('newAlias').value.trim();
    var msgDiv = document.getElementById('aliasMsg');
    var result = await apiPost('/admin/api/skins/alias', { hash: hash, alias: alias });
    if (result && result.success) {
        document.getElementById('aliasModal').remove();
        reloadSkins();
    } else if (msgDiv) {
        msgDiv.textContent = (result ? result.message : '\u7F51\u7EDC\u9519\u8BEF');
        msgDiv.className = 'msg-area error';
    }
}

async function deleteSkin(hash) {
    showConfirmDialog('\u786E\u5B9A\u8981\u5220\u9664\u8BE5\u76AE\u80A4\u7684\u6240\u6709\u5F15\u7528\u8BB0\u5F55\u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u64A4\u9500\u3002', async function() {
        var result = await apiPost('/admin/api/skins/delete', { hash: hash });
        if (result && result.success) {
            showToast(result.message || '\u5DF2\u5220\u9664', 'success');
            reloadSkins();
        }
    });
}

async function reloadSkins() {
    var res = await fetch('/admin/api/skins');
    if (res.status === 401) { window.location.href = '/admin/login'; return; }
    var data = await res.json();
    allSkins = (data.success && data.textures) ? data.textures : [];
    renderSkins(allSkins);
}

async function apiPost(url, body) {
    var res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
        body: JSON.stringify(body)
    });
    if (res.headers.get('content-type') && res.headers.get('content-type').indexOf('application/json') !== -1) {
        return res.json();
    }
    return null;
}

function formatSize(bytes) {
    if (bytes == null) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(2) + ' KiB';
    return (bytes / 1048576).toFixed(2) + ' MiB';
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    var d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate() + ' ' +
        String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
}

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';
    setTimeout(function() { toast.style.display = 'none'; }, 3000);
}
