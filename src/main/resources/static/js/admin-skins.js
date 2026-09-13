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
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">' + t('admin.skins.empty') + '</td></tr>';
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
            '<button class="btn-action btn-edit act-preview" data-hash="' + esc(s.hash) + '" data-name="' + esc(displayName) + '">' + t('admin.common.preview') + '</button>' +
            '<button class="btn-action btn-edit act-download" data-hash="' + esc(s.hash) + '">' + t('texture.download') + '</button>' +
            '<button class="btn-action btn-edit act-alias" data-hash="' + esc(s.hash) + '" data-alias="' + esc(s.adminAlias || '') + '">' + t('texture.editAlias') + '</button>' +
            '<button class="btn-action btn-delete act-delete" data-hash="' + esc(s.hash) + '">' + t('common.delete') + '</button>' +
            '</div></td></tr>';
    }).join('');
}

document.getElementById('skinTableBody').addEventListener('click', function(e) {
    var btn = e.target.closest('button');
    if (!btn) return;
    var hash = btn.dataset.hash;
    if (btn.classList.contains('act-preview')) {
        showPreview(hash, btn.dataset.name || '');
    } else if (btn.classList.contains('act-download')) {
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
        '<h3>' + t('admin.skins.editAliasTitle') + '</h3>' +
        '<div class="form-group"><label class="form-label">' + t('admin.skins.newAlias') + '</label>' +
        '<input type="text" class="form-input" id="newAlias" value="' + esc(currentAlias) + '"></div>' +
        '<div id="aliasMsg" class="msg-area"></div>' +
        '<div class="modal-actions">' +
        '<button class="btn btn-secondary" id="aliasCancelBtn">' + t('common.cancel') + '</button>' +
        '<button class="btn btn-primary" id="aliasSaveBtn">' + t('common.save') + '</button></div></div>';
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
        msgDiv.textContent = (result ? result.message : t('common.networkError'));
        msgDiv.className = 'msg-area error';
    }
}

async function deleteSkin(hash) {
    showConfirmDialog(t('admin.skins.deleteConfirm'), async function() {
        var result = await apiPost('/admin/api/skins/delete', { hash: hash });
        if (result && result.success) {
            showToast(result.message || t('common.success'), 'success');
            reloadSkins();
        }
    });
}

async function deleteOrphanSkins() {
    showConfirmDialog(t('admin.skins.deleteOrphanConfirm'), async function() {
        var result = await apiPost('/admin/api/skins/delete-orphans', {});
        if (result && result.success) {
            showToast(result.message || t('common.success'), 'success');
            reloadSkins();
        } else if (result) {
            showToast(result.message || '\u64CD\u4F5C\u5931\u8D25', 'error');
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

function showPreview(hash, name) {
    closePreview();
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'previewModal';
    overlay.innerHTML =
        '<div class="modal-box" style="max-width:420px">' +
        '<button class="modal-close-btn" id="previewCloseBtn">&times;</button>' +
        '<h3 style="margin-bottom:12px;word-break:break-all">' + esc(name || hash) + '</h3>' +
        '<div class="texture-preview-stage"><canvas id="adminPreviewCanvas"></canvas></div>' +
        '</div>';
    document.body.appendChild(overlay);
    document.getElementById('previewCloseBtn').addEventListener('click', closePreview);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) closePreview(); });
    var stage = overlay.querySelector('.texture-preview-stage');
    if (typeof skinview3d !== 'undefined') {
        try {
            window._adminPreviewViewer = new skinview3d.SkinViewer({
                canvas: document.getElementById('adminPreviewCanvas'),
                width: stage.clientWidth || 340,
                height: 340,
                skin: '/admin/api/skins/download?hash=' + encodeURIComponent(hash)
            });
            window._adminPreviewViewer.autoRotate = true;
            window._adminPreviewViewer.animation = new skinview3d.WalkingAnimation();
        } catch (e) {  }
    }
}

function closePreview() {
    if (window._adminPreviewViewer) {
        try { window._adminPreviewViewer.dispose(); } catch (e) {  }
        window._adminPreviewViewer = null;
    }
    var m = document.getElementById('previewModal');
    if (m) m.remove();
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


