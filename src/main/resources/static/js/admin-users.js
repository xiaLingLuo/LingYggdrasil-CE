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

let allUsers = [];

(async function loadUsers() {
    try {
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
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">暂无用户数据</td></tr>';
        return;
    }
    tbody.innerHTML = users.map(u => `
        <tr>
            <td><strong>${esc(u.username)}</strong></td>
            <td><code>${esc(u.id)}</code></td>
            <td>${esc(u.email)}</td>
            <td>${esc(u.nickname || '-')}</td>
            <td><span class="role-badge ${u.role === 'BANNED' ? 'role-badge-banned' : 'role-badge-default'}">${u.role}</span></td>
            <td><span class="verified-badge ${u.emailVerified ? 'verified-yes' : 'verified-no'}">${u.emailVerified ? '已验证' : '未验证'}</span></td>
            <td>${formatDate(u.createdAt)}</td>
            <td>
                <div class="action-btns">
                    ${u.role === 'BANNED'
                        ? `<button class="btn-action btn-unban act-unban" data-id="${esc(u.id)}">解封</button>`
                        : `<button class="btn-action btn-ban act-ban" data-id="${esc(u.id)}">封禁</button>`
                    }
                    <button class="btn-action btn-delete act-delete" data-id="${esc(u.id)}" data-name="${esc(u.username)}">删除</button>
                    <button class="btn-action btn-edit act-username" data-id="${esc(u.id)}" data-name="${esc(u.username)}">改用户名</button>
                    <button class="btn-action btn-edit act-email" data-id="${esc(u.id)}" data-email="${esc(u.email)}">改邮箱</button>
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

async function banUser(id) {
    showConfirmDialog('确定要封禁该用户吗？', async function() {
        await apiPost('/admin/api/users/ban', { id });
        reloadUsers();
    });
}

async function unbanUser(id) {
    showConfirmDialog('确定要解封该用户吗？', async function() {
        await apiPost('/admin/api/users/unban', { id });
        reloadUsers();
    });
}

async function deleteUser(id, username) {
    showConfirmDialog(`确定要删除用户 "${username}" 吗？此操作不可撤销。`, async function() {
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

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

async function submitUsername() {
    const id = document.getElementById('editUserId').value;
    const username = document.getElementById('newUsername').value.trim();
    if (!username) { showToast('请输入用户名', 'error'); return; }
    const result = await apiPost('/admin/api/users/username', { id, username });
    if (result && result.success) {
        closeModal('usernameModal');
        reloadUsers();
    }
}

async function submitEmail() {
    const id = document.getElementById('editEmailUserId').value;
    const email = document.getElementById('newEmail').value.trim();
    if (!email) { showToast('请输入邮箱', 'error'); return; }
    const result = await apiPost('/admin/api/users/email', { id, email });
    if (result && result.success) {
        closeModal('emailModal');
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
    var tbody = document.getElementById('userTableBody');
    if (tbody) {
        tbody.addEventListener('click', function(e) {
            var btn = e.target.closest('button');
            if (!btn) return;
            if (btn.classList.contains('act-ban')) banUser(btn.dataset.id);
            else if (btn.classList.contains('act-unban')) unbanUser(btn.dataset.id);
            else if (btn.classList.contains('act-delete')) deleteUser(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-username')) openUsernameModal(btn.dataset.id, btn.dataset.name);
            else if (btn.classList.contains('act-email')) openEmailModal(btn.dataset.id, btn.dataset.email);
        });
    }
})();
