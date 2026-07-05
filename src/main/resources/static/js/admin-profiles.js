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

let allProfiles = [];

(async function loadProfiles() {
    try {
        const res = await fetch('/admin/api/profiles');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        allProfiles = await res.json();
        renderProfiles(allProfiles);
    } catch (err) {
        console.error('加载角色列表失败:', err);
    }
})();

function renderProfiles(profiles) {
    const tbody = document.getElementById('profileTableBody');
    if (!profiles || profiles.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">暂无角色数据</td></tr>';
        return;
    }
    tbody.innerHTML = profiles.map(p => `
        <tr>
            <td><strong>${esc(p.name)}</strong></td>
            <td>${esc(p.username)}</td>
            <td><code>${esc(p.id)}</code></td>
            <td>${esc(p.skinModel)}</td>
            <td>${formatDate(p.createdAt)}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-action btn-edit act-update" data-id="${esc(p.id)}" data-name="${esc(p.name)}">修改名称</button>
                    <button class="btn-action btn-edit act-transfer" data-id="${esc(p.id)}">转移所有权</button>
                    <button class="btn-action btn-delete act-clear" data-id="${esc(p.id)}" data-name="${esc(p.name)}">清除皮肤/披风</button>
                    <button class="btn-action btn-delete act-delete" data-id="${esc(p.id)}" data-name="${esc(p.name)}">删除</button>
                </div>
            </td>
        </tr>
    `).join('');
}

function openCreateModal() {
    document.getElementById('createUserId').value = '';
    document.getElementById('createName').value = '';
    document.getElementById('createModal').style.display = 'flex';
}

function openUpdateModal(id, currentName) {
    document.getElementById('editProfileId').value = id;
    document.getElementById('newName').value = currentName;
    document.getElementById('updateModal').style.display = 'flex';
}

function openTransferModal(id) {
    document.getElementById('transferProfileId').value = id;
    document.getElementById('transferUserId').value = '';
    document.getElementById('transferModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitCreate() {
    const userId = document.getElementById('createUserId').value.trim();
    const name = document.getElementById('createName').value.trim();
    if (!userId) { showToast('请输入用户ID、用户名或邮箱', 'error'); return; }
    if (!name) { showToast('请输入角色名称', 'error'); return; }
    if (name.length > 24) { showToast('角色名称不能超过24个字符', 'error'); return; }
    if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showToast('角色名称只能包含字母、数字、下划线、中文和连字符', 'error'); return; }
    const result = await apiPost('/admin/api/profiles/create', { userId, name });
    if (result && result.success) {
        closeModal('createModal');
        reloadProfiles();
    }
}

async function submitUpdate() {
    const id = document.getElementById('editProfileId').value;
    const name = document.getElementById('newName').value.trim();
    if (!name) { showToast('请输入角色名称', 'error'); return; }
    if (name.length > 24) { showToast('角色名称不能超过24个字符', 'error'); return; }
    if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showToast('角色名称只能包含字母、数字、下划线、中文和连字符', 'error'); return; }
    const result = await apiPost('/admin/api/profiles/update', { id, name });
    if (result && result.success) {
        closeModal('updateModal');
        reloadProfiles();
    }
}

async function submitTransfer() {
    const id = document.getElementById('transferProfileId').value;
    const userId = document.getElementById('transferUserId').value.trim();
    if (!userId) { showToast('请输入用户ID、用户名或邮箱', 'error'); return; }
    const result = await apiPost('/admin/api/profiles/transfer', { id, userId });
    if (result && result.success) {
        closeModal('transferModal');
        reloadProfiles();
    }
}

async function deleteProfile(id, name) {
    showConfirmDialog(`确定要删除角色 "${name}" 吗？此操作不可撤销。`, async function() {
        await apiPost('/admin/api/profiles/delete', { id });
        reloadProfiles();
    });
}

async function clearProfileTextures(id, name) {
    showConfirmDialog(`确定要清除角色 "${name}" 的皮肤和披风吗？这将恢复到默认状态。`, async function() {
        await apiPost('/admin/api/profiles/clear-textures', { id });
        reloadProfiles();
    });
}

async function reloadProfiles() {
    try {
        const res = await fetch('/admin/api/profiles');
        allProfiles = await res.json();
        renderProfiles(allProfiles);
    } catch (err) {
        console.error('刷新角色列表失败:', err);
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
    var tbody = document.getElementById('profileTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-update')) openUpdateModal(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-transfer')) openTransferModal(btn.dataset.id);
            else if (btn.classList.contains('act-clear')) clearProfileTextures(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-delete')) deleteProfile(btn.dataset.id, btn.dataset.name);
        });
    }
})();
