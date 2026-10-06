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
var currentSkinsPage = 1;
var skinsPageSize = 100;
var skinsTotal = 0;
var currentSkinsQuery = '';
var skinsSearchTimer = null;
var skinsRequestSequence = 0;

(async function loadSkins() {
    await fetchSkinsPage(1, skinsPageSize, '');
})();

function renderSkins(skins) {
    var tbody = document.getElementById('skinTableBody');
    if (!skins || skins.length === 0) {
        var emptyMessage = currentSkinsQuery ? t('admin.common.searchNoResults') : t('admin.skins.empty');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">' + emptyMessage + '</td></tr>';
    } else {
        tbody.innerHTML = skins.map(function(s) {
            var displayName = s.fileName || '-';
            return '<tr>' +
                '<td style="font-family:Consolas,monospace;font-size:12px">' + esc(s.hash ? s.hash.substring(0, 6) : '-') + ' <button class="pencil-btn act-hash" type="button" title="HASH" aria-label="HASH" data-hash="' + esc(s.hash) + '"><i class="fas fa-magnifying-glass"></i></button></td>' +
                '<td>' + esc(displayName) + ' <button class="pencil-btn act-alias" type="button" title="' + esc(t('admin.skins.editFileNameTitle')) + '" aria-label="' + esc(t('admin.skins.editFileNameTitle')) + '" data-hash="' + esc(s.hash) + '" data-alias="' + esc(s.fileName || '') + '"><i class="fas fa-pencil"></i></button></td>' +
                '<td>' + formatSize(s.size) + '</td>' +
                '<td>' + esc(String(s.refCount)) + '</td>' +
                '<td>' + formatDate(s.createdAt) + '</td>' +
                '<td><div class="action-btns">' +
                '<button class="btn-action btn-edit act-preview" data-hash="' + esc(s.hash) + '" data-name="' + esc(displayName) + '">' + t('admin.common.preview') + '</button>' +
                '<button class="btn-action btn-edit act-download" data-hash="' + esc(s.hash) + '">' + t('texture.download') + '</button>' +
                '<button class="btn-action btn-delete act-delete" data-hash="' + esc(s.hash) + '">' + t('common.delete') + '</button>' +
                '</div></td></tr>';
        }).join('');
    }
    renderAdminPagination('skinPagination', skinsTotal, currentSkinsPage, skinsPageSize,
        function(page) { fetchSkinsPage(page, skinsPageSize, currentSkinsQuery); },
        function(size) { fetchSkinsPage(1, size, currentSkinsQuery); });
}

function filterSkins() {
    var input = document.getElementById('skinSearchInput');
    if (!input) return;
    var query = input.value.trim();
    if (skinsSearchTimer) clearTimeout(skinsSearchTimer);
    skinsSearchTimer = setTimeout(function() { fetchSkinsPage(1, skinsPageSize, query); }, 250);
}

async function fetchSkinsPage(page, pageSize, search) {
    var sequence = ++skinsRequestSequence;
    var params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) });
    if (search) params.set('q', search);
    try {
        var res = await fetch('/admin/api/skins?' + params.toString());
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        var data = await res.json();
        if (sequence !== skinsRequestSequence) return;
        var skins = (data.success && data.textures) ? data.textures : [];
        skinsTotal = Number(data.total) || 0;
        currentSkinsPage = Number(data.page) || 1;
        skinsPageSize = Number(data.pageSize) || pageSize;
        currentSkinsQuery = search || '';
        renderSkins(skins);
    } catch (err) {
        if (sequence === skinsRequestSequence) console.error('加载皮肤列表失败:', err);
    }
}

document.getElementById('skinTableBody').addEventListener('click', function(e) {
    var btn = e.target.closest('button');
    if (!btn) return;
    var hash = btn.dataset.hash;
    if (btn.classList.contains('act-hash')) {
        showHashModal(hash);
    } else if (btn.classList.contains('act-preview')) {
        showPreview(hash, btn.dataset.name || '');
    } else if (btn.classList.contains('act-download')) {
        window.open('/admin/api/skins/download?hash=' + encodeURIComponent(hash), '_blank');
    } else if (btn.classList.contains('act-alias')) {
        showAliasModal(hash, btn.dataset.alias || '');
    } else if (btn.classList.contains('act-delete')) {
        deleteSkin(hash);
    }
});

function createSkinModal(id, title, bodyHtml, footerHtml, maxWidth, onClose) {
    var existing = document.getElementById(id);
    if (existing) existing.remove();
    var modal = document.createElement('div');
    modal.className = 'modal';
    modal.id = id;
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');
    var backdrop = document.createElement('div');
    backdrop.className = 'modal-overlay';
    var card = document.createElement('div');
    card.className = 'modal-card';
    if (maxWidth) card.style.maxWidth = maxWidth;
    card.innerHTML =
        '<div class="modal-header"><h3>' + esc(title) + '</h3>' +
        '<button type="button" class="modal-close" aria-label="' + esc(t('common.close')) + '">&times;</button></div>' +
        '<div class="modal-body">' + bodyHtml + '</div>' +
        (footerHtml ? '<div class="modal-footer">' + footerHtml + '</div>' : '');
    modal.appendChild(backdrop);
    modal.appendChild(card);
    document.body.appendChild(modal);
    var close = function() {
        if (onClose) onClose();
        modal.remove();
    };
    card.querySelector('.modal-close').addEventListener('click', close);
    backdrop.addEventListener('click', close);
    return { modal: modal, close: close };
}

function showHashModal(hash) {
    createSkinModal('hashModal', 'HASH', '<code id="fullHash" style="display:block;word-break:break-all"></code>');
    document.getElementById('fullHash').textContent = hash || '';
}

function showAliasModal(hash, currentAlias) {
    var footer = '<button type="button" class="btn btn-secondary" id="aliasCancelBtn">' + esc(t('common.cancel')) + '</button>' +
        '<button type="button" class="btn btn-primary" id="aliasSaveBtn">' + esc(t('common.save')) + '</button>';
    var body = '<div class="form-group"><label class="form-label">' + esc(t('admin.skins.fileName')) + '</label>' +
        '<input type="text" class="form-input" id="newAlias" value="' + esc(currentAlias) + '" placeholder="' + esc(t('admin.skins.fileNamePlaceholder')) + '" maxlength="255"></div>' +
        '<div id="aliasMsg" class="msg-area"></div>';
    var dialog = createSkinModal('aliasModal', t('admin.skins.editFileNameTitle'), body, footer);
    document.getElementById('aliasCancelBtn').addEventListener('click', dialog.close);
    document.getElementById('aliasSaveBtn').addEventListener('click', function() { submitAlias(hash); });
}

async function submitAlias(hash) {
    var fileName = document.getElementById('newAlias').value.trim();
    var msgDiv = document.getElementById('aliasMsg');
    var result = await apiPost('/admin/api/skins/alias', { hash: hash, fileName: fileName });
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
    await fetchSkinsPage(currentSkinsPage, skinsPageSize, currentSkinsQuery);
}

function showPreview(hash, name) {
    closePreview();
    var modal = createSkinModal('previewModal', name || hash,
        '<div class="texture-preview-stage"><canvas id="adminPreviewCanvas"></canvas></div>', '', '420px', disposePreviewViewer).modal;
    modal.querySelector('.modal-header h3').style.wordBreak = 'break-all';
    var stage = modal.querySelector('.texture-preview-stage');
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

function disposePreviewViewer() {
    if (window._adminPreviewViewer) {
        try { window._adminPreviewViewer.dispose(); } catch (e) {  }
        window._adminPreviewViewer = null;
    }
}

function closePreview() {
    disposePreviewViewer();
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
