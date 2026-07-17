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
(function() {
    var myInfoCache = null;

    function escapeHtml(str) {
        if (!str) return '';
        var d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    function escAttr(str) {
        if (!str) return '';
        return String(str).replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function showToast(message, type) {
        var toast = document.getElementById('toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'toast';
            toast.className = 'toast';
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.className = 'toast toast-' + (type || 'info');
        toast.style.display = 'block';
        setTimeout(function() { toast.style.display = 'none'; }, 3000);
    }

    function showConfirmDialog(message, onConfirm) {
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'confirmDialog';
        overlay.style.zIndex = '1100';
        overlay.innerHTML = '<div class="modal-box"><h3>\u786E\u8BA4\u64CD\u4F5C</h3><p>' + escapeHtml(message) + '</p>' +
            '<div class="modal-actions" id="confirmDialogActions"></div></div>';
        document.body.appendChild(overlay);
        var actionsDiv = overlay.querySelector('#confirmDialogActions');
        var cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = '\u53D6\u6D88';
        cancelBtn.addEventListener('click', function() { closeDialog(false); });
        actionsDiv.appendChild(cancelBtn);
        var confirmBtn = document.createElement('button');
        confirmBtn.className = 'btn btn-danger';
        confirmBtn.textContent = '\u786E\u8BA4';
        confirmBtn.addEventListener('click', function() { closeDialog(true); });
        actionsDiv.appendChild(confirmBtn);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeDialog(false); });
        function closeDialog(confirmed) { overlay.remove(); if (confirmed && onConfirm) onConfirm(); }
    }

    function initSkinViewer(canvas, skinUrl, skinModel) {
        if (typeof skinview3d === 'undefined' || !skinUrl) return null;
        try {
            var previewDiv = canvas.parentElement;
            var viewer = new skinview3d.SkinViewer({
                canvas: canvas, width: previewDiv.clientWidth || 300, height: 320,
                skin: skinUrl, model: skinModel || 'slim'
            });
            viewer.autoRotate = true;
            viewer.animation = new skinview3d.WalkingAnimation();
            return viewer;
        } catch(e) { return null; }
    }

    function loadFace(skinUrl, canvas) {
        canvas.width = 64; canvas.height = 64;
        var url = skinUrl || '/img/juststeve.png';
        var img = new Image();
        img.onload = function() {
            var w = img.width, isOld = w === 64 && img.height === 32;
            var ctx = canvas.getContext('2d'); ctx.imageSmoothingEnabled = false;
            ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 64, 64);
            if (!isOld) ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 64, 64);
        };
        img.onerror = function() {
            var ctx = canvas.getContext('2d');
            ctx.fillStyle = '#E8D5F5'; ctx.fillRect(0, 0, 64, 64);
        };
        img.src = url;
    }

    async function loadFriends() {
        var container = document.getElementById('friendList');
        if (!container) return;
        try {
            var [myResp, friendsResp] = await Promise.all([
                fetch('/api/friends/my-info'), fetch('/api/friends')
            ]);
            if (myResp.status === 401) { window.location.href = '/login'; return; }
            var myData = await myResp.json();
            var friendsData = await friendsResp.json();
            if (!myData.success) { showToast('\u52A0\u8F7D\u5931\u8D25', 'error'); return; }
            myInfoCache = myData.info;

            var html = '<div class="friend-card friend-my-card card-animate" id="friendMyCard">' +
                '<div class="friend-card-avatar"><canvas></canvas></div>' +
                '<div class="friend-card-name">' + escapeHtml(myInfoCache.displayProfileName || '\u6211') + '</div>' +
                '<div class="friend-card-code">' + escapeHtml(myInfoCache.friendCodeFormatted || '----') + '</div></div>';

            html += '<div class="friend-add-tile card-animate" id="friendAddTile">' +
                '<div class="friend-add-tile-icon"><span class="friend-add-tile-plus">+</span></div>' +
                '<div class="friend-add-tile-text">\u6DFB\u52A0\u597D\u53CB</div>' +
                '<div class="friend-add-tile-hint">1002-3004-5006-7008</div></div>';

            if (friendsData.success && friendsData.friends && friendsData.friends.length > 0) {
                friendsData.friends.forEach(function(f) {
                    var isPending = f.type === 'pending_sent' || f.type === 'pending_received';
                    html += '<div class="friend-card card-animate friend-card-clickable"' +
                        ' data-type="' + escAttr(f.type) + '"' +
                        ' data-request-id="' + escAttr(f.requestId || '') + '"' +
                        ' data-friend-id="' + escAttr(f.userId) + '"' +
                        ' data-display-name="' + escAttr(f.displayName) + '"' +
                        ' data-username="' + escAttr(f.username || '') + '"' +
                        ' data-friend-code="' + escAttr(f.friendCodeFormatted || '') + '"' +
                        ' data-friend-code-raw="' + escAttr(f.friendCode || '') + '"' +
                        ' data-skin-url="' + escAttr(f.skinUrl || '') + '"' +
                        ' data-skin-model="' + escAttr(f.skinModel || 'default') + '">' +
                        '<div class="friend-card-avatar"><canvas></canvas></div>' +
                        '<div class="friend-card-name">' + escapeHtml(f.displayName) + '</div>' +
                        (isPending ? '<div class="friend-card-label">\u597D\u53CB\u8BF7\u6C42</div>' : '') +
                        '</div>';
                });
            } else {
                html += '<p class="empty-hint" style="grid-column:1/-1">\u6682\u65E0\u597D\u53CB\uFF0C\u5FEB\u901A\u8FC7\u597D\u53CB\u4EE3\u7801\u6DFB\u52A0\u4E00\u4E2A\u5427~ <i class="fas fa-leaf"></i></p>';
            }
            container.innerHTML = html;

            loadFace(myInfoCache.skinUrl || '', document.querySelector('#friendMyCard .friend-card-avatar canvas'));
            container.querySelectorAll('.friend-card-clickable .friend-card-avatar canvas').forEach(function(c) {
                var card = c.closest('.friend-card-clickable');
                var skinUrl = card && card.dataset.skinUrl;
                loadFace(skinUrl || '', c);
            });

            container.querySelectorAll('.friend-card-clickable').forEach(function(card) {
                card.addEventListener('click', function() {
                    var ds = this.dataset;
                    if (ds.type === 'confirmed') showFriendDetail(ds);
                    else showPendingDetail(ds);
                });
            });
            document.getElementById('friendMyCard').addEventListener('click', function() { showMyProfileModal(); });
            document.getElementById('friendAddTile').addEventListener('click', function() { showAddFriendModal(); });
        } catch (err) {
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u5237\u65B0\u91CD\u8BD5</p>';
        }
    }

    function showPendingDetail(dataset) {
        var title = dataset.displayName || '';
        if (dataset.username && dataset.username !== dataset.displayName) {
            title = dataset.displayName + ' (' + dataset.username + ')';
        }

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'friendDetailModal';

        var box = document.createElement('div');
        box.className = 'modal-box friend-detail-modal';
        box.innerHTML =
            '<button class="modal-close-btn" id="friendDetailCloseBtn">&times;</button>' +
            '<h3>\u597D\u53CB\u8BF7\u6C42</h3>' +
            '<p style="font-size:15px;font-weight:600;margin-bottom:12px;">' + escapeHtml(title) + '</p>' +
            '<div class="friend-code-display">' +
            '<span>' + escapeHtml(dataset.friendCode || '----') + '</span>' +
            '<button class="btn btn-secondary btn-small" id="friendCodeCopyBtn"><i class="fas fa-copy"></i></button>' +
            '</div>' +
            '<div class="modal-actions" id="pendingActions"></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        var actionsDiv = box.querySelector('#pendingActions');
        if (dataset.type === 'pending_sent') {
            var cancelBtn = document.createElement('button');
            cancelBtn.className = 'btn btn-danger';
            cancelBtn.textContent = '\u53D6\u6D88\u8BF7\u6C42';
            cancelBtn.addEventListener('click', function() {
                cancelRequest(dataset.requestId, 'sent');
            });
            actionsDiv.appendChild(cancelBtn);
        } else if (dataset.type === 'pending_received') {
            var acceptBtn = document.createElement('button');
            acceptBtn.className = 'btn btn-primary';
            acceptBtn.textContent = '\u63A5\u53D7\u8BF7\u6C42';
            acceptBtn.addEventListener('click', function() {
                acceptRequest(dataset.requestId);
            });
            actionsDiv.appendChild(acceptBtn);
            var rejectBtn = document.createElement('button');
            rejectBtn.className = 'btn btn-secondary';
            rejectBtn.textContent = '\u62D2\u7EDD\u6DFB\u52A0';
            rejectBtn.addEventListener('click', function() {
                cancelRequest(dataset.requestId, 'received');
            });
            actionsDiv.appendChild(rejectBtn);
            var blockBtn = document.createElement('button');
            blockBtn.className = 'btn btn-danger';
            blockBtn.textContent = '\u62C9\u9ED1';
            blockBtn.addEventListener('click', function() {
                blockUser(dataset.friendId);
            });
            actionsDiv.appendChild(blockBtn);
        }

        document.getElementById('friendDetailCloseBtn').addEventListener('click', closeAllModals);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeAllModals(); });
        document.getElementById('friendCodeCopyBtn').addEventListener('click', function() {
            navigator.clipboard.writeText(dataset.friendCodeRaw || '').then(function() {
                showToast('\u597D\u53CB\u4EE3\u7801\u5DF2\u590D\u5236', 'success');
            }).catch(function() { showToast('\u590D\u5236\u5931\u8D25', 'error'); });
        });
    }

    function showFriendDetail(dataset) {
        closeAllModals();
        var title = dataset.displayName || '';
        if (dataset.username && dataset.username !== dataset.displayName) {
            title = dataset.displayName + ' (' + dataset.username + ')';
        }
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'friendDetailModal';

        var box = document.createElement('div');
        box.className = 'modal-box friend-detail-modal';
        box.innerHTML =
            '<button class="modal-close-btn" id="friendDetailCloseBtn">&times;</button>' +
            '<h3>' + escapeHtml(title) + '</h3>' +
            '<div class="friend-detail-body">' +
            '<div class="friend-detail-preview"><canvas id="friendDetailCanvas"></canvas></div>' +
            '<div class="friend-detail-info">' +
            '<div class="friend-code-display">' +
            '<span>' + escapeHtml(dataset.friendCode || '----') + '</span>' +
            '<button class="btn btn-secondary btn-small" id="friendCodeCopyBtn"><i class="fas fa-copy"></i></button>' +
            '</div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-danger" id="friendDetailDeleteBtn">\u5220\u9664\u597D\u53CB</button>' +
            '</div></div></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        requestAnimationFrame(function() {
            var skinUrl = dataset.skinUrl || '/img/juststeve.png';
            var canvas = document.getElementById('friendDetailCanvas');
            if (canvas) window._friendDetailViewer = initSkinViewer(canvas, skinUrl, dataset.skinModel || 'slim');
        });

        document.getElementById('friendDetailCloseBtn').addEventListener('click', closeAllModals);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeAllModals(); });
        document.getElementById('friendCodeCopyBtn').addEventListener('click', function() {
            navigator.clipboard.writeText(dataset.friendCodeRaw || '').then(function() {
                showToast('\u597D\u53CB\u4EE3\u7801\u5DF2\u590D\u5236', 'success');
            }).catch(function() { showToast('\u590D\u5236\u5931\u8D25', 'error'); });
        });
        document.getElementById('friendDetailDeleteBtn').addEventListener('click', function() {
            deleteFriend(dataset.friendId);
        });
    }

    function closeAllModals() {
        if (window._friendDetailViewer) { window._friendDetailViewer.dispose(); window._friendDetailViewer = null; }
        if (window._profileViewer) { window._profileViewer.dispose(); window._profileViewer = null; }
        var m = document.getElementById('friendDetailModal');
        if (m) m.remove();
        m = document.getElementById('myProfileModal');
        if (m) m.remove();
    }

    function showMyProfileModal() {
        if (!myInfoCache) return;
        closeAllModals();
        var profiles = myInfoCache.profiles || [];
        var profileOpts = '<option value="">\u4E0D\u5C55\u793A</option>';
        profiles.forEach(function(p) {
            var sel = myInfoCache.displayProfileId === p.id ? ' selected' : '';
            profileOpts += '<option value="' + escAttr(p.id) + '"' + sel + '>' + escapeHtml(p.name) + '</option>';
        });

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'myProfileModal';
        var box = document.createElement('div');
        box.className = 'modal-box friend-profile-modal';
        box.innerHTML =
            '<button class="modal-close-btn" id="profileCloseBtn">&times;</button>' +
            '<h3>\u6211\u7684\u8D44\u6599\u5361</h3>' +
            '<div class="friend-profile-body">' +
            '<div class="friend-profile-preview"><canvas id="profileSkinCanvas"></canvas></div>' +
            '<div class="friend-profile-details">' +
            '<div class="form-group"><label class="form-label">\u597D\u53CB\u4EE3\u7801</label>' +
            '<div class="friend-code-display">' +
            '<span id="profileFriendCode">' + escapeHtml(myInfoCache.friendCodeFormatted || '----') + '</span>' +
            '<button class="btn btn-secondary btn-small" id="copyFriendCodeBtn"><i class="fas fa-copy"></i></button>' +
            '</div></div>' +
            '<div class="form-group"><label class="form-label">\u5C55\u793A\u7528\u89D2\u8272</label>' +
            '<select class="form-input" id="displayProfileSelect">' + profileOpts + '</select></div>' +
            '<div id="profileMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="profileCancelBtn">\u5173\u95ED</button>' +
            '<button class="btn btn-primary" id="profileSaveBtn">\u4FDD\u5B58</button>' +
            '</div></div></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        requestAnimationFrame(function() {
            var skinUrl = myInfoCache.skinUrl || '/img/juststeve.png';
            var canvas = document.getElementById('profileSkinCanvas');
            if (canvas) window._profileViewer = initSkinViewer(canvas, skinUrl, myInfoCache.skinModel || 'slim');
        });

        document.getElementById('displayProfileSelect').addEventListener('change', function() {
            if (window._profileViewer) { window._profileViewer.dispose(); window._profileViewer = null; }
            var pid = this.value;
            if (pid) {
                var prof = profiles.find(function(p) { return p.id === pid; });
                if (prof) {
                    requestAnimationFrame(function() {
                        var canvas = document.getElementById('profileSkinCanvas');
                        if (canvas) window._profileViewer = initSkinViewer(canvas, prof.skinUrl || '/img/juststeve.png', prof.skinModel || 'slim');
                    });
                }
            } else {
                requestAnimationFrame(function() {
                    var canvas = document.getElementById('profileSkinCanvas');
                    if (canvas) window._profileViewer = initSkinViewer(canvas, '/img/juststeve.png', 'slim');
                });
            }
        });
        document.getElementById('copyFriendCodeBtn').addEventListener('click', function() {
            navigator.clipboard.writeText(myInfoCache.friendCodeFormatted || '').then(function() {
                showToast('\u597D\u53CB\u4EE3\u7801\u5DF2\u590D\u5236', 'success');
            }).catch(function() { showToast('\u590D\u5236\u5931\u8D25', 'error'); });
        });
        document.getElementById('profileSaveBtn').addEventListener('click', saveDisplayProfile);
        document.getElementById('profileCancelBtn').addEventListener('click', closeAllModals);
        document.getElementById('profileCloseBtn').addEventListener('click', closeAllModals);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeAllModals(); });
    }

    async function saveDisplayProfile() {
        var select = document.getElementById('displayProfileSelect');
        var msgDiv = document.getElementById('profileMsg');
        var pid = select ? select.value || '' : '';
        try {
            var resp = await fetch('/api/friends/display-profile', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ profileId: pid })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || '\u4FDD\u5B58\u6210\u529F', 'success');
                closeAllModals();
                loadFriends();
            } else {
                if (msgDiv) { msgDiv.textContent = '\u2717 ' + (data.message || '\u5931\u8D25'); msgDiv.className = 'msg-area error'; }
            }
        } catch (err) {
            if (msgDiv) { msgDiv.textContent = '\u2717 \u7F51\u7EDC\u9519\u8BEF'; msgDiv.className = 'msg-area error'; }
        }
    }

    function showAddFriendModal() {
        var existing = document.getElementById('addFriendModal');
        if (existing) existing.remove();
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'addFriendModal';
        var box = document.createElement('div');
        box.className = 'modal-box';
        box.innerHTML =
            '<button class="modal-close-btn" id="addFriendCloseBtn">&times;</button>' +
            '<h3>\u6DFB\u52A0\u597D\u53CB</h3>' +
            '<div class="form-group"><label class="form-label">\u597D\u53CB\u4EE3\u7801</label>' +
            '<input type="text" class="form-input" id="friendCodeInput" placeholder="1002-3004-5006-7008" maxlength="19"></div>' +
            '<div id="addFriendMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="addFriendCancelBtn">\u53D6\u6D88</button>' +
            '<button class="btn btn-primary" id="addFriendConfirmBtn">\u6DFB\u52A0</button></div>';
        overlay.appendChild(box);
        document.body.appendChild(overlay);
        document.getElementById('addFriendCloseBtn').addEventListener('click', function() { closeModal('addFriendModal'); });
        document.getElementById('addFriendCancelBtn').addEventListener('click', function() { closeModal('addFriendModal'); });
        document.getElementById('addFriendConfirmBtn').addEventListener('click', submitAddFriend);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeModal('addFriendModal'); });
        var input = document.getElementById('friendCodeInput');
        if (input) input.focus();
    }

    function closeModal(id) { var m = document.getElementById(id); if (m) m.remove(); }

    async function submitAddFriend() {
        var input = document.getElementById('friendCodeInput');
        var msgDiv = document.getElementById('addFriendMsg');
        var btn = document.getElementById('addFriendConfirmBtn');
        var code = input ? input.value.trim().replace(/-/g, '') : '';
        if (!code) {
            if (msgDiv) { msgDiv.textContent = '\u2717 \u8BF7\u8F93\u5165\u597D\u53CB\u4EE3\u7801'; msgDiv.className = 'msg-area error'; }
            return;
        }
        if (btn) { btn.disabled = true; btn.textContent = '\u6DFB\u52A0\u4E2D...'; }
        try {
            var resp = await fetch('/api/friends/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendCode: code })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || '\u8BF7\u6C42\u5DF2\u53D1\u9001', 'success');
                closeModal('addFriendModal');
                loadFriends();
            } else {
                if (msgDiv) { msgDiv.textContent = '\u2717 ' + (data.message || '\u5931\u8D25'); msgDiv.className = 'msg-area error'; }
                if (btn) { btn.disabled = false; btn.textContent = '\u6DFB\u52A0'; }
            }
        } catch (err) {
            if (msgDiv) { msgDiv.textContent = '\u2717 \u7F51\u7EDC\u9519\u8BEF'; msgDiv.className = 'msg-area error'; }
            if (btn) { btn.disabled = false; btn.textContent = '\u6DFB\u52A0'; }
        }
    }

    async function acceptRequest(requestId) {
        try {
            var resp = await fetch('/api/friends/request/accept', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ requestId: requestId })
            });
            var data = await resp.json();
            showToast(data.message || (data.success ? '\u5DF2\u6DFB\u52A0' : '\u64CD\u4F5C\u5931\u8D25'), data.success ? 'success' : 'error');
            if (data.success) { closeAllModals(); loadFriends(); }
        } catch (err) { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); }
    }

    async function cancelRequest(requestId, type) {
        var msg = type === 'sent' ? '\u786E\u5B9A\u8981\u53D6\u6D88\u8BE5\u597D\u53CB\u8BF7\u6C42\u5417\uFF1F' : '\u786E\u5B9A\u8981\u62D2\u7EDD\u8BE5\u597D\u53CB\u8BF7\u6C42\u5417\uFF1F';
        showConfirmDialog(msg, async function() {
            try {
                var resp = await fetch('/api/friends/request/cancel', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ requestId: requestId })
                });
                var data = await resp.json();
                showToast(data.message || (data.success ? '\u5DF2\u53D6\u6D88' : '\u64CD\u4F5C\u5931\u8D25'), data.success ? 'success' : 'error');
                if (data.success) { closeAllModals(); loadFriends(); }
            } catch (err) { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); }
        });
    }

    async function blockUser(userId) {
        showConfirmDialog('\u786E\u5B9A\u8981\u62C9\u9ED1\u8BE5\u7528\u6237\u5417\uFF1F\u62C9\u9ED1\u540E\u5C06\u65E0\u6CD5\u518D\u6536\u5230\u5176\u597D\u53CB\u8BF7\u6C42\u3002', async function() {
            try {
                var resp = await fetch('/api/friends/block', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ userId: userId })
                });
                var data = await resp.json();
                showToast(data.message || (data.success ? '\u5DF2\u62C9\u9ED1' : '\u64CD\u4F5C\u5931\u8D25'), data.success ? 'success' : 'error');
                if (data.success) { closeAllModals(); loadFriends(); }
            } catch (err) { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); }
        });
    }

    function deleteFriend(friendId) {
        showConfirmDialog('\u786E\u5B9A\u8981\u5220\u9664\u8BE5\u597D\u53CB\u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u6062\u590D\u3002', function() {
            fetch('/api/friends/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendId: friendId })
            }).then(function(r) { return r.json(); })
              .then(function(data) {
                  if (data.success) { closeAllModals(); showToast(data.message || '\u5DF2\u5220\u9664', 'success'); loadFriends(); }
                  else showToast(data.message || '\u5220\u9664\u5931\u8D25', 'error');
              }).catch(function() { showToast('\u7F51\u7EDC\u9519\u8BEF', 'error'); });
        });
    }

    loadFriends();
})();
