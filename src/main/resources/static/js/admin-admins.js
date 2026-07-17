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

const IS_ROOT = window.IS_ROOT || false;
let allAdmins = [];

(async function loadAdmins() {
    try {
        const res = await fetch('/admin/api/admins');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        allAdmins = await res.json();
        renderAdmins(allAdmins);
    } catch (err) {
        console.error('加载管理员列表失败:', err);
    }
})();

function renderAdmins(admins) {
    const tbody = document.getElementById('adminTableBody');
    if (!admins || admins.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">暂无管理员数据</td></tr>';
        return;
    }
    tbody.innerHTML = admins.map(a => {
        const isRootAdmin = a.role === 'ROOT';
        const actions = IS_ROOT && !isRootAdmin ? `
            <div class="action-btns">
                <button class="btn-action btn-edit act-edit" data-id="${esc(a.id)}" data-username="${esc(a.username)}" data-email="${esc(a.email)}">修改</button>
                <button class="btn-action btn-delete act-delete" data-id="${esc(a.id)}" data-username="${esc(a.username)}">删除</button>
            </div>
        ` : (isRootAdmin ? '<span class="role-badge role-badge-root">受保护</span>' : '<span class="btn-action btn-disabled">无权限</span>');

        return `
            <tr>
                <td><strong>${esc(a.username)}</strong></td>
                <td>${esc(a.email)}</td>
                <td><span class="role-badge ${isRootAdmin ? 'role-badge-root' : 'role-badge-op'}">${a.role}</span></td>
                <td>${formatDate(a.createdAt)}</td>
                <td>${actions}</td>
            </tr>
        `;
    }).join('');
}

function openCreateModal() {
    if (!IS_ROOT) return;
    document.getElementById('createUsername').value = '';
    document.getElementById('createEmail').value = '';
    document.getElementById('createPassword').value = '';
    document.getElementById('createModal').style.display = 'flex';
}

function openEditModal(id, username, email) {
    if (!IS_ROOT) return;
    document.getElementById('editAdminId').value = id;
    document.getElementById('editUsername').value = username;
    document.getElementById('editEmail').value = email;
    document.getElementById('editPassword').value = '';
    document.getElementById('editModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitCreate() {
    if (!IS_ROOT) { showToast('仅 root 可创建管理员', 'error'); return; }

    const username = document.getElementById('createUsername').value.trim();
    const email = document.getElementById('createEmail').value.trim();
    const password = document.getElementById('createPassword').value;

    if (!username || !email || !password) {
        showToast('请填写所有字段', 'error');
        return;
    }

    const result = await apiPost('/admin/api/admins/create', { username, email, password, role: 'OP' });
    if (result && result.success) {
        closeModal('createModal');
        reloadAdmins();
    }
}

async function submitEdit() {
    if (!IS_ROOT) { showToast('仅 root 可修改管理员', 'error'); return; }

    const id = document.getElementById('editAdminId').value;
    const username = document.getElementById('editUsername').value.trim();
    const email = document.getElementById('editEmail').value.trim();
    const password = document.getElementById('editPassword').value;

    const body = { id };
    if (username) body.username = username;
    if (email) body.email = email;
    if (password) body.password = password;

    const result = await apiPost('/admin/api/admins/update', body);
    if (result && result.success) {
        closeModal('editModal');
        reloadAdmins();
    }
}

async function deleteAdmin(id, username) {
    if (!IS_ROOT) { showToast('仅 root 可删除管理员', 'error'); return; }
    showConfirmDialog(`确定要删除管理员 "${username}" 吗？此操作不可撤销。`, async function() {
        const result = await apiPost('/admin/api/admins/delete', { id });
        if (result && result.success) {
            reloadAdmins();
        }
    });
}

async function reloadAdmins() {
    try {
        const res = await fetch('/admin/api/admins');
        allAdmins = await res.json();
        renderAdmins(allAdmins);
    } catch (err) {
        console.error('刷新管理员列表失败:', err);
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
    var tbody = document.getElementById('adminTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-edit')) openEditModal(btn.dataset.id, btn.dataset.username, btn.dataset.email);
            else if (btn.classList.contains('act-delete')) deleteAdmin(btn.dataset.id, btn.dataset.username);
        });
    }
})();
