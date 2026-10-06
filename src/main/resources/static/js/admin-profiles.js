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
let currentProfilesPage = 1;
let profilesPageSize = 100;
let profilesTotal = 0;
let currentProfilesQuery = '';
let profilesSearchTimer = null;
let profilesRequestSequence = 0;

(async function loadProfiles() {
    try {
        const search = document.getElementById('profileSearchInput').value.trim();
        await fetchProfilesPage(1, profilesPageSize, search);
    } catch (err) {
        console.error('加载角色列表失败:', err);
    }
})();

function renderProfiles(profiles) {
    const tbody = document.getElementById('profileTableBody');
    if (!profiles || profiles.length === 0) {
        const emptyMessage = currentProfilesQuery ? t('admin.common.searchNoResults') : t('admin.profiles.empty');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">' + emptyMessage + '</td></tr>';
    } else {
        tbody.innerHTML = profiles.map(p => {
            const modelLabel = p.skinModel === 'slim' ? t('admin.profiles.modelSlim') : t('admin.profiles.modelDefault');
            return `
            <tr>
                <td><strong>${esc(p.name)}</strong>
                    <button class="pencil-btn act-update" title="${t('admin.profiles.updateTitle')}" data-id="${esc(p.id)}" data-name="${esc(p.name)}"><i class="fas fa-pencil"></i></button>
                </td>
                <td>${esc(p.username)}
                    <button class="pencil-btn act-transfer" title="${t('admin.profiles.transferTitle')}" data-id="${esc(p.id)}"><i class="fas fa-pencil"></i></button>
                </td>
                <td><code>${esc(p.id)}</code></td>
                <td>
                    <button class="model-badge act-model ${p.skinModel === 'slim' ? 'model-slim' : 'model-default'}" title="${t('admin.profiles.toggleModelTitle')}" data-id="${esc(p.id)}" data-model="${p.skinModel === 'slim' ? 'slim' : 'default'}">${modelLabel}</button>
                </td>
                <td>${formatDate(p.createdAt)}</td>
                <td>
                    <div class="action-btns">
                        <button class="btn-action btn-edit act-clear" data-id="${esc(p.id)}" data-name="${esc(p.name)}">${t('admin.profiles.resetTextures')}</button>
                        <button class="btn-action btn-delete act-delete" data-id="${esc(p.id)}" data-name="${esc(p.name)}">${t('common.delete')}</button>
                    </div>
                </td>
            </tr>
        `;
        }).join('');
    }
    renderAdminPagination('profilePagination', profilesTotal, currentProfilesPage, profilesPageSize,
        function(page) { fetchProfilesPage(page, profilesPageSize, currentProfilesQuery); },
        function(size) { fetchProfilesPage(1, size, currentProfilesQuery); });
}

function filterProfiles() {
    const input = document.getElementById('profileSearchInput');
    if (!input) return;
    const query = input.value.trim();
    if (profilesSearchTimer) clearTimeout(profilesSearchTimer);
    profilesSearchTimer = setTimeout(function() { fetchProfilesPage(1, profilesPageSize, query); }, 250);
}

async function fetchProfilesPage(page, pageSize, search) {
    const sequence = ++profilesRequestSequence;
    const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) });
    if (search) params.set('q', search);
    try {
        const res = await fetch('/admin/api/profiles?' + params.toString());
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();
        if (sequence !== profilesRequestSequence) return;
        profilesTotal = Number(data.total) || 0;
        currentProfilesPage = Number(data.page) || 1;
        profilesPageSize = Number(data.pageSize) || pageSize;
        currentProfilesQuery = search || '';
        renderProfiles(data.items || []);
    } catch (err) {
        if (sequence === profilesRequestSequence) console.error('加载角色列表失败:', err);
    }
}

function openCreateModal() {
    document.getElementById('createName').value = '';
    document.getElementById('createUuid').value = '';
    document.getElementById('createModal').style.display = 'flex';
}

function generateUuid() {
    var uuid;
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        uuid = window.crypto.randomUUID().replace(/-/g, '');
    } else {
        uuid = '';
        var hex = '0123456789abcdef';
        for (var i = 0; i < 32; i++) uuid += hex.charAt(Math.floor(Math.random() * 16));
    }
    document.getElementById('createUuid').value = uuid;
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

async function toggleProfileModel(id, currentModel) {
    const next = currentModel === 'slim' ? 'default' : 'slim';
    const result = await apiPost('/admin/api/profiles/update', { id, model: next });
    if (result && result.success) reloadProfiles();
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitCreate() {
    const name = document.getElementById('createName').value.trim();
    const uuid = document.getElementById('createUuid').value.trim();
    if (!name) { showToast(t('admin.profiles.enterName'), 'error'); return; }
    if (name.length > 24) { showToast(t('admin.profiles.nameTooLong'), 'error'); return; }
    if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showToast(t('admin.profiles.nameInvalid'), 'error'); return; }
    const hex = uuid.replace(/-/g, '').toLowerCase();
    if (!/^[0-9a-f]{32}$/.test(hex)) { showToast(t('admin.profiles.uuidInvalid'), 'error'); return; }
    const result = await apiPost('/admin/api/profiles/create', { name, uuid: hex });
    if (result && result.success) {
        closeModal('createModal');
        reloadProfiles();
    }
}

async function submitUpdate() {
    const id = document.getElementById('editProfileId').value;
    const name = document.getElementById('newName').value.trim();
    if (!name) { showToast(t('admin.profiles.enterNewName'), 'error'); return; }
    if (name.length > 24) { showToast(t('admin.profiles.nameTooLong'), 'error'); return; }
    if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showToast(t('admin.profiles.nameInvalid'), 'error'); return; }
    const result = await apiPost('/admin/api/profiles/update', { id, name });
    if (result && result.success) {
        closeModal('updateModal');
        reloadProfiles();
    }
}

async function submitTransfer() {
    const id = document.getElementById('transferProfileId').value;
    const userId = document.getElementById('transferUserId').value.trim();
    if (!userId) { showToast(t('admin.profiles.transferFillAll'), 'error'); return; }
    const result = await apiPost('/admin/api/profiles/transfer', { id, userId });
    if (result && result.success) {
        closeModal('transferModal');
        reloadProfiles();
    }
}

async function deleteProfile(id, name) {
    showConfirmDialog(t('admin.profiles.deleteConfirm', name), async function() {
        await apiPost('/admin/api/profiles/delete', { id });
        reloadProfiles();
    });
}

async function clearProfileTextures(id, name) {
    showConfirmDialog(t('admin.profiles.resetConfirm', name), async function() {
        await apiPost('/admin/api/profiles/clear-textures', { id });
        reloadProfiles();
    });
}

async function reloadProfiles() {
    await fetchProfilesPage(currentProfilesPage, profilesPageSize, currentProfilesQuery);
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
    var tbody = document.getElementById('profileTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-update')) openUpdateModal(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-transfer')) openTransferModal(btn.dataset.id);
            else if (btn.classList.contains('act-model')) toggleProfileModel(btn.dataset.id, btn.dataset.model);
            else if (btn.classList.contains('act-clear')) clearProfileTextures(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-delete')) deleteProfile(btn.dataset.id, btn.dataset.name);
        });
    }
})();
