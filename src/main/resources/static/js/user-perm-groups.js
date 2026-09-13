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

var userPermCatalogue = [];

function userPermsCan(key) {
    var perms = window.__ADMIN_PERMS__ || [];
    return perms.indexOf('*') !== -1 || perms.indexOf(key) !== -1;
}

async function loadUserPermGroups() {
    var list = document.getElementById('userPermGroupList');
    if (!list) return;
    try {
        var catRes = await fetch('/admin/api/user-perm-groups/catalogue');
        var grpRes = await fetch('/admin/api/user-perm-groups');
        var catData = catRes.ok ? await catRes.json() : { permissions: [] };
        var grpData = grpRes.ok ? await grpRes.json() : { groups: [] };
        userPermCatalogue = catData.permissions || [];
        renderUserPermGroups(grpData.groups || []);
    } catch (err) {
        list.innerHTML = '<p class="text-muted">' + t('common.loadFailed') + '</p>';
    }
}

function renderUserPermGroups(groups) {
    var list = document.getElementById('userPermGroupList');
    if (!groups.length) {
        list.innerHTML = '<p class="text-muted">' + t('admin.admins.noPerms') + '</p>';
        return;
    }
    var canEdit = userPermsCan('admin.usergroups.edit');
    var html = '';
    groups.forEach(function (g) {
        var isAll = (g.permissions || '').trim() === '*';
        var perms = isAll ? null : (g.permissions || '').split(',').map(function (s) { return s.trim(); });
        var editable = canEdit && !g.immutable;

        var categories = {};
        var order = [];
        userPermCatalogue.forEach(function (p) {
            if (!categories[p.category]) { categories[p.category] = []; order.push(p.category); }
            categories[p.category].push(p);
        });

        var body = '';
        order.forEach(function (cat) {
            var catToggle = editable
                ? '<button type="button" class="perm-toggle-btn" data-action="toggleUserPermCat" data-this></button>'
                : '';
            body += '<div class="perm-cat"><div class="perm-cat-head"><div class="perm-cat-title">' +
                t('admin.permCat.' + cat) + '</div>' + catToggle + '</div><div class="perm-grid">';
            categories[cat].forEach(function (p) {
                var checked = isAll || perms.indexOf(p.key) !== -1;
                var riskIcon = p.highRisk
                    ? ' <i class="fas fa-triangle-exclamation perm-risk-icon" title="' + esc(t('admin.permRisk.title')) +
                      '" data-action="showPermRisk" data-args="' + esc(JSON.stringify([p.key])) + '" data-prevent data-stop></i>'
                    : '';
                body += '<label class="perm-item"><input type="checkbox" data-perm="' + esc(p.key) + '"' +
                    (checked ? ' checked' : '') + (editable ? '' : ' disabled') + '> <span>' + esc(p.key) + '</span>' +
                    riskIcon + '</label>';
            });
            body += '</div></div>';
        });

        var groupToggle = editable
            ? '<button type="button" class="perm-toggle-btn" style="margin-left:auto" data-action="toggleUserPermAll" data-this></button>'
            : '';
        var actions = '';
        if (editable) {
            actions += '<button class="btn btn-primary" data-action="saveUserGroupPerms" data-args="' + esc(JSON.stringify([g.id])) + '">' + t('admin.admins.savePerms') + '</button>';
            if (!g.builtin) {
                actions += ' <button class="btn btn-danger" data-action="deleteUserPermGroup" data-args="' + esc(JSON.stringify([g.id, g.alias || g.name])) + '">' + t('admin.admins.deleteGroup') + '</button>';
            }
        } else if (canEdit && g.immutable) {
            actions = '<button class="btn btn-secondary" data-action="showToast" data-args="' + esc(JSON.stringify([t('msg.builtinGroupLocked'), 'error'])) + '">' + t('admin.admins.savePerms') + '</button>';
        }

        html += '<div class="perm-group-card" data-group-id="' + esc(g.id) + '">' +
            '<div class="perm-group-head">' +
            '<strong>' + esc(g.alias || g.name) + '</strong>' +
            ' <span class="text-muted" style="font-size:12px">(' + esc(g.name) + ')</span>' +
            (g.builtin ? ' <span class="perm-builtin">' + t('admin.admins.builtin') + '</span>' : '') +
            ' <span class="text-muted" style="font-size:12px">' + t('admin.users.userCount', g.userCount) + '</span>' +
            groupToggle +
            '</div>' +
            '<div class="perm-group-body">' + body + '</div>' +
            (actions ? '<div class="perm-group-actions">' + actions + '</div>' : '') +
            '</div>';
    });
    list.innerHTML = html;
    list.querySelectorAll('.perm-group-card').forEach(refreshUserPermToggleLabels);
}

function refreshUserPermToggleLabels(card) {
    card.querySelectorAll('.perm-toggle-btn').forEach(function (btn) {
        var target = btn.closest('.perm-cat') || card;
        var boxes = target.querySelectorAll('input[data-perm]');
        var allChecked = boxes.length > 0
            && Array.prototype.every.call(boxes, function (cb) { return cb.checked; });
        btn.textContent = allChecked ? t('admin.admins.deselectAll') : t('admin.admins.selectAll');
    });
}

function toggleUserPermAll(btn) {
    var card = btn.closest('.perm-group-card');
    if (!card) return;
    var boxes = card.querySelectorAll('input[data-perm]');
    var allChecked = Array.prototype.every.call(boxes, function (cb) { return cb.checked; });
    Array.prototype.forEach.call(boxes, function (cb) { cb.checked = !allChecked; });
    refreshUserPermToggleLabels(card);
}

function toggleUserPermCat(btn) {
    var card = btn.closest('.perm-group-card');
    var cat = btn.closest('.perm-cat');
    if (!cat) return;
    var boxes = cat.querySelectorAll('input[data-perm]');
    var allChecked = Array.prototype.every.call(boxes, function (cb) { return cb.checked; });
    Array.prototype.forEach.call(boxes, function (cb) { cb.checked = !allChecked; });
    if (card) refreshUserPermToggleLabels(card);
}

async function saveUserGroupPerms(id) {
    var card = document.querySelector('.perm-group-card[data-group-id="' + id + '"]');
    if (!card) return false;
    var permissions = [];
    card.querySelectorAll('input[data-perm]').forEach(function (cb) {
        if (cb.checked) permissions.push(cb.dataset.perm);
    });
    var result = await userGroupPost('/admin/api/user-perm-groups/update', { id: id, permissions: permissions });
    if (result && result.success) { loadUserPermGroups(); return true; }
    return false;
}

function openUserGroupCreateModal() {
    var input = document.getElementById('userGroupNameInput');
    if (input) input.value = '';
    document.getElementById('userGroupModal').style.display = 'flex';
}

async function submitCreateUserGroup() {
    var name = document.getElementById('userGroupNameInput').value.trim();
    if (!name) { showToast(t('admin.admins.groupNamePlaceholder'), 'error'); return; }
    var result = await userGroupPost('/admin/api/user-perm-groups/create', { name: name, permissions: [] });
    if (result && result.success) {
        closeModal('userGroupModal');
        loadUserPermGroups();
    }
}

async function deleteUserPermGroup(id, name) {
    showConfirmDialog(t('admin.admins.groupDeleteConfirm', name), async function () {
        var result = await userGroupPost('/admin/api/user-perm-groups/delete', { id: id });
        if (result && result.success) loadUserPermGroups();
    });
}

async function userGroupPost(url, body) {
    try {
        var res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify(body)
        });
        var data = await res.json();
        showToast(data.message || (data.success ? t('common.success') : t('common.failed')), data.success ? 'success' : 'error');
        return data;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return null;
    }
}

(function () {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadUserPermGroups);
    } else {
        loadUserPermGroups();
    }
})();
