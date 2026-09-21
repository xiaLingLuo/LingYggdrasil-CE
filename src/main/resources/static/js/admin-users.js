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
let allUsers = [];
let userGroupAliases = {};

function can(key) {
    const perms = window.__ADMIN_PERMS__ || [];
    return perms.indexOf('*') !== -1 || perms.indexOf(key) !== -1;
}

async function loadUserGroupsForSelect() {
    try {
        const res = await fetch('/admin/api/user-perm-groups');
        if (!res.ok) return;
        const data = await res.json();
        const groups = (data && data.groups) ? data.groups : [];
        userGroupAliases = {};
        groups.forEach(g => { userGroupAliases[g.name] = g.alias || g.name; });
        const sel = document.getElementById('setGroupSelect');
        if (sel) {
            sel.innerHTML = groups.map(g => '<option value="' + esc(g.name) + '">' + esc(g.alias || g.name) + ' (' + esc(g.name) + ')</option>').join('');
        }
    } catch (err) {
        console.error('Failed to load user groups:', err);
    }
}

function groupLabel(name) {
    return userGroupAliases[name] || name || 'default';
}

(async function loadUsers() {
    try {
        await loadUserGroupsForSelect();
        const res = await fetch('/admin/api/users');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        allUsers = await res.json();
        renderUsers(allUsers);
    } catch (err) {
        console.error('加载用户列表失败:', err);
    }
})();

function renderUsers(users) {
    const tbody = document.getElementById('userTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">' + t('common.empty') + '</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr>
            <td><strong>${esc(u.username)}</strong>
                <button class="pencil-btn act-username" title="${t('admin.users.editUsernameTitle')}" data-id="${esc(u.id)}" data-name="${esc(u.username)}"><i class="fas fa-pencil"></i></button>
            </td>
            <td><code>${esc(u.id)}</code></td>
            <td>${esc(u.email)}
                <button class="pencil-btn act-email" title="${t('admin.users.editEmailTitle')}" data-id="${esc(u.id)}" data-email="${esc(u.email)}"><i class="fas fa-pencil"></i></button>
            </td>
            <td>${esc(u.nickname || '-')}
                <button class="pencil-btn act-nickname" title="${t('admin.users.editNicknameTitle')}" data-id="${esc(u.id)}" data-nickname="${esc(u.nickname || '')}"><i class="fas fa-pencil"></i></button>
            </td>
            <td><span class="role-badge role-badge-default">${esc(groupLabel(u.permGroup))}</span>
                ${can('admin.users.edit') ? `<button class="pencil-btn act-group" title="${t('admin.admins.permGroup')}" data-id="${esc(u.id)}" data-group="${esc(u.permGroup || 'default')}"><i class="fas fa-pencil"></i></button>` : ''}
            </td>
            <td>
                <button class="verified-badge act-verify ${u.emailVerified ? 'verified-yes' : 'verified-no'}" title="${t('admin.users.toggleVerifyTitle')}" data-id="${esc(u.id)}" data-verified="${u.emailVerified}">${u.emailVerified ? t('admin.users.verified') : t('admin.users.unverified')}</button>
            </td>
            <td>${formatDate(u.createdAt)}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-action btn-delete act-delete" data-id="${esc(u.id)}" data-name="${esc(u.username)}">${t('common.delete')}</button>
                </div>
            </td>
        </tr>
    `).join('');
}

function filterUsers() {
    const q = document.getElementById('searchInput').value.toLowerCase();
    if (!q) { renderUsers(allUsers); return; }
    const filtered = allUsers.filter(u =>
        (u.username && u.username.toLowerCase().includes(q)) ||
        (u.email && u.email.toLowerCase().includes(q)) ||
        (u.nickname && u.nickname.toLowerCase().includes(q))
    );
    renderUsers(filtered);
}

function openSetGroupModal(id, group) {
    document.getElementById('setGroupUserId').value = id;
    const sel = document.getElementById('setGroupSelect');
    if (sel) sel.value = group || 'default';
    document.getElementById('userSetGroupModal').style.display = 'flex';
}

async function submitSetUserGroup() {
    const id = document.getElementById('setGroupUserId').value;
    const permGroup = document.getElementById('setGroupSelect').value;
    const result = await apiPost('/admin/api/users/perm-group', { id, permGroup });
    if (result && result.success) {
        closeModal('userSetGroupModal');
        reloadUsers();
    }
}

async function toggleEmailVerified(id, current) {
    const result = await apiPost('/admin/api/users/verify-email', { id, verified: String(!current) });
    if (result && result.success) reloadUsers();
}

async function deleteUser(id, username) {
    showConfirmDialog(t('admin.users.deleteConfirm', username), async function() {
        await apiPost('/admin/api/users/delete', { id });
        reloadUsers();
    });
}

function openUsernameModal(id, currentUsername) {
    document.getElementById('editUserId').value = id;
    document.getElementById('newUsername').value = currentUsername;
    document.getElementById('usernameModal').style.display = 'flex';
}

function openEmailModal(id, currentEmail) {
    document.getElementById('editEmailUserId').value = id;
    document.getElementById('newEmail').value = currentEmail;
    document.getElementById('emailModal').style.display = 'flex';
}

function openNicknameModal(id, currentNickname) {
    document.getElementById('editNicknameUserId').value = id;
    document.getElementById('newNickname').value = currentNickname || '';
    document.getElementById('nicknameModal').style.display = 'flex';
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

function openCreateUserModal() {
    document.getElementById('createUsername').value = '';
    document.getElementById('createUserEmail').value = '';
    document.getElementById('createUserPassword').value = '';
    document.getElementById('createUserNickname').value = '';
    document.getElementById('createUserVerified').checked = true;
    document.getElementById('createUserModal').style.display = 'flex';
}

async function submitCreateUser() {
    const username = document.getElementById('createUsername').value.trim();
    const email = document.getElementById('createUserEmail').value.trim();
    const password = document.getElementById('createUserPassword').value;
    const nickname = document.getElementById('createUserNickname').value.trim();
    const emailVerified = document.getElementById('createUserVerified').checked;
    if (!username || !email || !password) { showToast(t('admin.users.createFillAll'), 'error'); return; }
    const result = await apiPost('/admin/api/users/create', {
        username, email, password, nickname, emailVerified: String(emailVerified)
    });
    if (result && result.success) {
        closeModal('createUserModal');
        reloadUsers();
    }
}

async function submitUsername() {
    const id = document.getElementById('editUserId').value;
    const username = document.getElementById('newUsername').value.trim();
    if (!username) { showToast(t('admin.users.enterUsername'), 'error'); return; }
    const result = await apiPost('/admin/api/users/username', { id, username });
    if (result && result.success) {
        closeModal('usernameModal');
        reloadUsers();
    }
}

async function submitEmail() {
    const id = document.getElementById('editEmailUserId').value;
    const email = document.getElementById('newEmail').value.trim();
    if (!email) { showToast(t('admin.users.enterEmail'), 'error'); return; }
    const result = await apiPost('/admin/api/users/email', { id, email });
    if (result && result.success) {
        closeModal('emailModal');
        reloadUsers();
    }
}

async function submitNickname() {
    const id = document.getElementById('editNicknameUserId').value;
    const nickname = document.getElementById('newNickname').value.trim();
    if (!nickname) { showToast(t('admin.users.enterNickname'), 'error'); return; }
    const result = await apiPost('/admin/api/users/nickname', { id, nickname });
    if (result && result.success) {
        closeModal('nicknameModal');
        reloadUsers();
    }
}

async function reloadUsers() {
    try {
        const res = await fetch('/admin/api/users');
        allUsers = await res.json();
        renderUsers(allUsers);
    } catch (err) {
        console.error('刷新用户列表失败:', err);
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
    var tbody = document.getElementById('userTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-group')) openSetGroupModal(btn.dataset.id, btn.dataset.group);
            else if (btn.classList.contains('act-delete')) deleteUser(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-username')) openUsernameModal(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-email')) openEmailModal(btn.dataset.id, btn.dataset.email);
            else if (btn.classList.contains('act-nickname')) openNicknameModal(btn.dataset.id, btn.dataset.nickname);
            else if (btn.classList.contains('act-verify')) toggleEmailVerified(btn.dataset.id, btn.dataset.verified === 'true');
        });
    }
})();
