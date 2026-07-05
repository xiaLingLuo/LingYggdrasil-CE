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

let allCapes = [];

(async function loadCapes() {
    try {
        const res = await fetch('/admin/api/capes');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();
        allCapes = (data.success && data.textures) ? data.textures : [];
        renderCapes(allCapes);
    } catch (err) {
        console.error('加载披风列表失败:', err);
        renderCapes([]);
    }
})();

function renderCapes(capes) {
    const tbody = document.getElementById('capeTableBody');
    if (!tbody) return;
    if (!capes || capes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">暂无披风数据</td></tr>';
        return;
    }
    tbody.innerHTML = capes.map(c => `
        <tr>
            <td>${esc(c.alias || '-')}</td>
            <td>${esc(c.originalName)}</td>
            <td>${formatSize(c.size)}</td>
            <td>${esc(c.username || '-')}</td>
            <td>${formatDate(c.createdAt)}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-action btn-edit act-download" data-id="${esc(c.id)}">下载</button>
                    <button class="btn-action btn-edit act-alias" data-id="${esc(c.id)}" data-alias="${esc(c.alias || '')}">修改别名</button>
                    <button class="btn-action btn-delete act-delete" data-id="${esc(c.id)}" data-name="${esc(c.alias || c.originalName)}">删除</button>
                </div>
            </td>
        </tr>
    `).join('');
}

const uploadForm = document.getElementById('uploadForm');
if (uploadForm) {
    uploadForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        const fileInput = document.getElementById('capeFile');
        const aliasInput = document.getElementById('capeAlias');
        const msgDiv = document.getElementById('uploadMsg');

        if (!fileInput.files || !fileInput.files[0]) {
            showMsg(msgDiv, '请选择文件', false);
            return;
        }
        const file = fileInput.files[0];
        if (file.type !== 'image/png') {
            showMsg(msgDiv, '仅支持 PNG 格式的图片', false);
            return;
        }

        const formData = new FormData();
        formData.append('file', file);
        const alias = aliasInput.value.trim();
        if (alias) formData.append('alias', alias);

        try {
            const res = await fetch('/admin/api/capes/upload', {
                method: 'POST',
                headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: formData
            });
            const data = await res.json();
            showMsg(msgDiv, data.message, data.success);
            if (data.success) {
                fileInput.value = '';
                aliasInput.value = '';
                reloadCapes();
            }
        } catch (err) {
            showMsg(msgDiv, '网络错误', false);
        }
    });
}

function openAliasModal(id, currentAlias) {
    document.getElementById('editCapeId').value = id;
    document.getElementById('newAlias').value = currentAlias || '';
    document.getElementById('aliasModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitAlias() {
    const id = document.getElementById('editCapeId').value;
    const alias = document.getElementById('newAlias').value.trim();
    const result = await apiPost('/admin/api/capes/alias', { id, alias });
    if (result && result.success) {
        closeModal('aliasModal');
        reloadCapes();
    }
}

function downloadCape(id) {
    window.open('/admin/api/capes/download?id=' + encodeURIComponent(id), '_blank');
}

async function deleteCape(id, name) {
    showConfirmDialog(`确定要删除披风 "${name}" 吗？此操作不可撤销。`, async function() {
        const result = await apiPost('/admin/api/capes/delete', { id });
        if (result && result.success) {
            reloadCapes();
        }
    });
}

async function reloadCapes() {
    try {
        const res = await fetch('/admin/api/capes');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();
        allCapes = (data.success && data.textures) ? data.textures : [];
        renderCapes(allCapes);
    } catch (err) {
        console.error('刷新披风列表失败:', err);
    }
}

async function apiPost(url, body) {
    try {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        showToast(data.message || (data.success ? '操作成功' : '操作失败'), data.success ? 'success' : 'error');
        return data;
    } catch (err) {
        showToast('网络错误', 'error');
        return null;
    }
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, 3000);
}

function showMsg(div, msg, success) {
    if (!div) return;
    div.innerHTML = (success ? '<i class="fas fa-circle-check"></i> ' : '<i class="fas fa-circle-xmark"></i> ') + msg;
    div.className = 'msg-area ' + (success ? 'success' : 'error');
}

function formatSize(bytes) {
    if (bytes === null || bytes === undefined) return '-';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KiB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MiB';
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } catch { return dateStr; }
}

function esc(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

(function() {
    var tbody = document.getElementById('capeTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-download')) downloadCape(btn.dataset.id);
            else if (btn.classList.contains('act-alias')) openAliasModal(btn.dataset.id, btn.dataset.alias);
            else if (btn.classList.contains('act-delete')) deleteCape(btn.dataset.id, btn.dataset.name);
        });
    }
})();
