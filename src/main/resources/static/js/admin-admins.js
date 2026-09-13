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
const IS_ROOT = window.IS_ROOT || false;
const PERMS = window.__ADMIN_PERMS__ || [];
let allAdmins = [];
let allGroups = [];

function can(key) {
    return PERMS.indexOf('*') !== -1 || PERMS.indexOf(key) !== -1;
}

async function loadGroups() {
    try {
        const res = await fetch('/admin/api/perm-groups');
        if (!res.ok) return;
        const data = await res.json();
        allGroups = (data && data.groups) ? data.groups : [];
        ['createPermGroup', 'editPermGroup'].forEach(function(id) {
            const sel = document.getElementById(id);
            if (!sel) return;
            sel.innerHTML = allGroups.map(function(g) {
                return '<option value="' + esc(g.name) + '">' + esc(g.name) + '</option>';
            }).join('');
        });
    } catch (err) {
        console.error('Failed to load permission groups:', err);
    }
}

(async function loadAdmins() {
    try {
        await loadGroups();
        const res = await fetch('/admin/api/admins');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        allAdmins = await res.json();
        renderAdmins(allAdmins);
    } catch (err) {
        console.error('Failed to load admin list:', err);
    }
})();

function renderAdmins(admins) {
    const tbody = document.getElementById('adminTableBody');
    if (!admins || admins.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">' + t('admin.admins.empty') + '</td></tr>';
        return;
    }
    tbody.innerHTML = admins.map(a => {
        const editBtn = can('admin.admins.edit')
            ? `<button class="btn-action btn-edit act-edit" data-id="${esc(a.id)}" data-username="${esc(a.username)}" data-email="${esc(a.email)}" data-group="${esc(a.permGroup || 'op')}">${t('common.edit')}</button>` : '';
        const delBtn = can('admin.admins.delete')
            ? `<button class="btn-action btn-delete act-delete" data-id="${esc(a.id)}" data-username="${esc(a.username)}">${t('common.delete')}</button>` : '';
        const actions = (editBtn || delBtn)
            ? '<div class="action-btns">' + editBtn + delBtn + '</div>'
            : '<span class="btn-action btn-disabled">' + t('admin.admins.noPermission') + '</span>';

        return `
            <tr>
                <td><strong>${esc(a.username)}</strong></td>
                <td>${esc(a.email)}</td>
                <td>${esc(a.permGroup || 'op')}</td>
                <td>${formatDate(a.createdAt)}</td>
                <td>${actions}</td>
            </tr>
        `;
    }).join('');
}

function openCreateModal() {
    if (!can('admins.create')) return;
    document.getElementById('createUsername').value = '';
    document.getElementById('createEmail').value = '';
    document.getElementById('createPassword').value = '';
    const g = document.getElementById('createPermGroup');
    if (g) g.value = 'op';
    document.getElementById('createModal').style.display = 'flex';
}

function openEditModal(id, username, email, group) {
    if (!can('admins.edit')) return;
    document.getElementById('editAdminId').value = id;
    document.getElementById('editUsername').value = username;
    document.getElementById('editEmail').value = email;
    document.getElementById('editPassword').value = '';
    const g = document.getElementById('editPermGroup');
    if (g) g.value = group || 'op';
    document.getElementById('editModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitCreate() {
    if (!can('admins.create')) { showToast(t('admin.admins.noPermission'), 'error'); return; }

    const username = document.getElementById('createUsername').value.trim();
    const email = document.getElementById('createEmail').value.trim();
    const password = document.getElementById('createPassword').value;
    const permGroup = document.getElementById('createPermGroup').value;

    if (!username || !email || !password) {
        showToast(t('admin.admins.fillAll'), 'error');
        return;
    }

    const result = await apiPost('/admin/api/admins/create', { username, email, password, permGroup });
    if (result && result.success) {
        closeModal('createModal');
        reloadAdmins();
    }
}

async function submitEdit() {
    if (!can('admins.edit')) { showToast(t('admin.admins.noPermission'), 'error'); return; }

    const id = document.getElementById('editAdminId').value;
    const username = document.getElementById('editUsername').value.trim();
    const email = document.getElementById('editEmail').value.trim();
    const password = document.getElementById('editPassword').value;
    const permGroup = document.getElementById('editPermGroup').value;

    const body = { id };
    if (username) body.username = username;
    if (email) body.email = email;
    if (password) body.password = password;
    if (permGroup) body.permGroup = permGroup;

    const result = await apiPost('/admin/api/admins/update', body);
    if (result && result.success) {
        closeModal('editModal');
        reloadAdmins();
    }
}

async function deleteAdmin(id, username) {
    if (!can('admins.delete')) { showToast(t('admin.admins.noPermission'), 'error'); return; }
    showConfirmDialog(t('admin.admins.deleteConfirm', username), async function() {
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
        showToast(data.message || (data.success ? t('common.success') : t('common.failed')), data.success ? 'success' : 'error');
        return data;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return null;
    }
}

(function() {
    var tbody = document.getElementById('adminTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-edit')) openEditModal(btn.dataset.id, btn.dataset.username, btn.dataset.email, btn.dataset.group);
            else if (btn.classList.contains('act-delete')) deleteAdmin(btn.dataset.id, btn.dataset.username);
        });
    }
})();
