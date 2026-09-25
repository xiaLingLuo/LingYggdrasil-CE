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
            if (!myData.success) { showToast(window.t('friends.loadFailed'), 'error'); return; }
            myInfoCache = myData.info;

            var html = '<div class="friend-card friend-my-card card-animate" id="friendMyCard">' +
                '<div class="friend-card-avatar"><canvas></canvas></div>' +
                '<div class="friend-card-name">' + escapeHtml(myInfoCache.displayProfileName || window.t('friends.me')) + '</div>' +
                '<div class="friend-card-code">' + escapeHtml(myInfoCache.friendCodeFormatted || '----') + '</div></div>';

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
                        (isPending ? '<div class="friend-card-label">' + window.t('friends.pending') + '</div>' : '') +
                        '</div>';
                });
            } else {
                html += '<p class="empty-hint" style="grid-column:1/-1">' + window.t('friends.empty') + ' <i class="fas fa-leaf"></i></p>';
            }
            html += '<div class="friend-add-tile card-animate" id="friendAddTile">' +
                '<div class="friend-add-tile-icon"><span class="friend-add-tile-plus">+</span></div>' +
                '<div class="friend-add-tile-text">' + window.t('friends.add') + '</div>' +
                '<div class="friend-add-tile-hint">' + window.t('friends.addHint') + '</div></div>';
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
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">' + window.t('friends.loadFailedRetry') + '</p>';
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
            '<h3>' + window.t('friends.requestTitle') + '</h3>' +
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
            cancelBtn.textContent = window.t('friends.cancelRequest');
            cancelBtn.addEventListener('click', function() {
                cancelRequest(dataset.requestId, 'sent');
            });
            actionsDiv.appendChild(cancelBtn);
        } else if (dataset.type === 'pending_received') {
            var acceptBtn = document.createElement('button');
            acceptBtn.className = 'btn btn-primary';
            acceptBtn.textContent = window.t('friends.acceptRequest');
            acceptBtn.addEventListener('click', function() {
                acceptRequest(dataset.requestId);
            });
            actionsDiv.appendChild(acceptBtn);
            var rejectBtn = document.createElement('button');
            rejectBtn.className = 'btn btn-secondary';
            rejectBtn.textContent = window.t('friends.rejectRequest');
            rejectBtn.addEventListener('click', function() {
                cancelRequest(dataset.requestId, 'received');
            });
            actionsDiv.appendChild(rejectBtn);
            var blockBtn = document.createElement('button');
            blockBtn.className = 'btn btn-danger';
            blockBtn.textContent = window.t('friends.block');
            blockBtn.addEventListener('click', function() {
                blockUser(dataset.friendId);
            });
            actionsDiv.appendChild(blockBtn);
        }

        document.getElementById('friendDetailCloseBtn').addEventListener('click', closeAllModals);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeAllModals(); });
        document.getElementById('friendCodeCopyBtn').addEventListener('click', function() {
            navigator.clipboard.writeText(dataset.friendCodeRaw || '').then(function() {
                showToast(window.t('friends.codeCopied'), 'success');
            }).catch(function() { showToast(window.t('friends.copyFailed'), 'error'); });
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
            '<div class="form-group" style="margin-top:8px">' +
            '<label class="form-label" style="margin-bottom:4px"><i class="fas fa-share-nodes"></i> ' + window.t('friends.shareTextures') + '</label>' +
            '<div class="friend-share-actions">' +
            '<button type="button" class="btn btn-secondary btn-small" id="friendShareSkinBtn"><i class="fas fa-shirt"></i> ' + window.t('friends.shareSkin') + '</button>' +
            '<button type="button" class="btn btn-secondary btn-small" id="friendShareCapeBtn"><i class="fas fa-scroll"></i> ' + window.t('friends.shareCape') + '</button>' +
            '</div></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-danger" id="friendDetailDeleteBtn">' + window.t('friends.deleteFriend') + '</button>' +
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
                showToast(window.t('friends.codeCopied'), 'success');
            }).catch(function() { showToast(window.t('friends.copyFailed'), 'error'); });
        });
        document.getElementById('friendDetailDeleteBtn').addEventListener('click', function() {
            deleteFriend(dataset.friendId);
        });

        bindShareButtons(dataset.friendId);
    }

    function bindShareButtons(friendId) {
        var skinBtn = document.getElementById('friendShareSkinBtn');
        var capeBtn = document.getElementById('friendShareCapeBtn');
        if (skinBtn) skinBtn.addEventListener('click', function() { openSharePicker(friendId, 'SKIN'); });
        if (capeBtn) capeBtn.addEventListener('click', function() { openSharePicker(friendId, 'CAPE'); });
    }

    async function openSharePicker(friendId, type) {
        var existing = document.getElementById('sharePickerModal');
        if (existing) existing.remove();
        var isSkin = type === 'SKIN';
        try {
            var [listResp, sharedResp] = await Promise.all([
                fetch(isSkin ? '/api/skins' : '/api/capes'),
                fetch('/api/friends/' + encodeURIComponent(friendId) + '/my-shared')
            ]);
            if (listResp.status === 401) { window.location.href = '/login'; return; }
            var listData = await listResp.json();
            var sharedData = await sharedResp.json();
            var mine = isSkin ? (listData.skins || []) : (listData.capes || []);
            var sharedIds = {};
            if (sharedData.success && sharedData.textures) {
                sharedData.textures.forEach(function(t) { sharedIds[t.id] = true; });
            }

            var rows = '';
            mine.forEach(function(t) {
                if (sharedIds[t.id]) return;
                var displayName = t.alias || t.originalName || t.hash;
                rows += '<label class="share-picker-row">' +
                    '<input type="checkbox" class="share-picker-check" value="' + escapeHtml(t.id) + '">' +
                    '<span class="share-picker-name">' + escapeHtml(displayName) + '</span></label>';
            });
            if (!rows) {
                rows = '<p class="text-muted share-picker-empty">' + window.t('friends.noShareable') + '</p>';
            }

            var overlay = document.createElement('div');
            overlay.className = 'modal-overlay';
            overlay.id = 'sharePickerModal';
            overlay.style.zIndex = '1080';
            overlay.innerHTML = '<div class="modal-box">' +
                '<h3>' + window.t(isSkin ? 'friends.shareSkin' : 'friends.shareCape') + '</h3>' +
                '<div class="share-picker-list">' + rows + '</div>' +
                '<div class="modal-actions">' +
                '<button type="button" class="btn btn-secondary" id="sharePickerCancel">' + window.t('common.cancel') + '</button>' +
                '<button type="button" class="btn btn-primary" id="sharePickerConfirm">' + window.t('friends.initiateShare') + '</button>' +
                '</div></div>';
            document.body.appendChild(overlay);

            document.getElementById('sharePickerCancel').addEventListener('click', function() { overlay.remove(); });
            overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
            document.getElementById('sharePickerConfirm').addEventListener('click', async function() {
                var checks = overlay.querySelectorAll('.share-picker-check:checked');
                if (checks.length === 0) return;
                var ok = 0, fail = 0;
                for (var i = 0; i < checks.length; i++) {
                    var data = await sendShare(friendId, checks[i].value);
                    if (data && data.success) ok++; else fail++;
                }
                if (ok > 0) showToast(window.t('friends.shareSuccess', ok), 'success');
                if (fail > 0) showToast(window.t('friends.sharePartial'), 'error');
                overlay.remove();
            });
        } catch (err) {
            showToast(window.t('common.networkError'), 'error');
        }
    }

    async function sendShare(friendId, textureId) {
        try {
            var resp = await fetch('/api/friends/share-texture', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendId: friendId, textureId: textureId })
            });
            return await resp.json();
        } catch (err) {
            return { success: false };
        }
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
        var profileOpts = '<option value="">' + window.t('friends.notDisplay') + '</option>';
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
            '<h3>' + window.t('friends.myCard') + '</h3>' +
            '<div class="friend-profile-body">' +
            '<div class="friend-profile-preview"><canvas id="profileSkinCanvas"></canvas></div>' +
            '<div class="friend-profile-details">' +
            '<div class="form-group"><label class="form-label">' + window.t('friends.code') + '</label>' +
            '<div class="friend-code-display">' +
            '<span id="profileFriendCode">' + escapeHtml(myInfoCache.friendCodeFormatted || '----') + '</span>' +
            '<button class="btn btn-secondary btn-small" id="copyFriendCodeBtn"><i class="fas fa-copy"></i></button>' +
            '</div></div>' +
            '<div class="form-group"><label class="form-label">' + window.t('friends.displayProfile') + '</label>' +
            '<select class="form-input" id="displayProfileSelect">' + profileOpts + '</select></div>' +
            '<div id="profileMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="profileCancelBtn">' + window.t('common.close') + '</button>' +
            '<button class="btn btn-primary" id="profileSaveBtn">' + window.t('common.save') + '</button>' +
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
                showToast(window.t('friends.codeCopied'), 'success');
            }).catch(function() { showToast(window.t('friends.copyFailed'), 'error'); });
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
                showToast(data.message || window.t('common.saveSuccess'), 'success');
                closeAllModals();
                loadFriends();
            } else {
                if (msgDiv) { msgDiv.textContent = '\u2717 ' + (data.message || window.t('common.failed')); msgDiv.className = 'msg-area error'; }
            }
        } catch (err) {
            if (msgDiv) { msgDiv.textContent = '\u2717 ' + window.t('common.networkError'); msgDiv.className = 'msg-area error'; }
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
            '<h3>' + window.t('friends.addTitle') + '</h3>' +
            '<div class="form-group"><label class="form-label">' + window.t('friends.code') + '</label>' +
            '<input type="text" class="form-input" id="friendCodeInput" placeholder="1002-3004-5006-7008" maxlength="19"></div>' +
            '<div id="addFriendMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="addFriendCancelBtn">' + window.t('common.cancel') + '</button>' +
            '<button class="btn btn-primary" id="addFriendConfirmBtn">' + window.t('friends.addBtn') + '</button></div>';
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
            if (msgDiv) { msgDiv.textContent = '\u2717 ' + window.t('friends.enterCode'); msgDiv.className = 'msg-area error'; }
            return;
        }
        if (btn) { btn.disabled = true; btn.textContent = window.t('friends.adding'); }
        try {
            var resp = await fetch('/api/friends/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendCode: code })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || window.t('friends.requestSent'), 'success');
                closeModal('addFriendModal');
                loadFriends();
            } else {
                if (msgDiv) { msgDiv.textContent = '\u2717 ' + (data.message || window.t('common.failed')); msgDiv.className = 'msg-area error'; }
                if (btn) { btn.disabled = false; btn.textContent = window.t('friends.addBtn'); }
            }
        } catch (err) {
            if (msgDiv) { msgDiv.textContent = '\u2717 ' + window.t('common.networkError'); msgDiv.className = 'msg-area error'; }
            if (btn) { btn.disabled = false; btn.textContent = window.t('friends.addBtn'); }
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
            showToast(data.message || (data.success ? window.t('friends.added') : window.t('texture.operationFailed')), data.success ? 'success' : 'error');
            if (data.success) { closeAllModals(); loadFriends(); }
        } catch (err) { showToast(window.t('common.networkError'), 'error'); }
    }

    async function cancelRequest(requestId, type) {
        var msg = type === 'sent' ? window.t('friends.cancelRequestConfirm') : window.t('friends.rejectConfirm');
        showConfirmDialog(msg, async function() {
            try {
                var resp = await fetch('/api/friends/request/cancel', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ requestId: requestId })
                });
                var data = await resp.json();
                showToast(data.message || (data.success ? window.t('friends.cancelled') : window.t('texture.operationFailed')), data.success ? 'success' : 'error');
                if (data.success) { closeAllModals(); loadFriends(); }
            } catch (err) { showToast(window.t('common.networkError'), 'error'); }
        });
    }

    async function blockUser(userId) {
        showConfirmDialog(window.t('friends.blockConfirm'), async function() {
            try {
                var resp = await fetch('/api/friends/block', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ userId: userId })
                });
                var data = await resp.json();
                showToast(data.message || (data.success ? window.t('friends.blocked') : window.t('texture.operationFailed')), data.success ? 'success' : 'error');
                if (data.success) { closeAllModals(); loadFriends(); }
            } catch (err) { showToast(window.t('common.networkError'), 'error'); }
        });
    }

    function deleteFriend(friendId) {
        showConfirmDialog(window.t('friends.deleteConfirm'), function() {
            fetch('/api/friends/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendId: friendId })
            }).then(function(r) { return r.json(); })
              .then(function(data) {
                  if (data.success) { closeAllModals(); showToast(data.message || window.t('friends.deleted'), 'success'); loadFriends(); }
                  else showToast(data.message || window.t('friends.deleteFailed'), 'error');
              }).catch(function() { showToast(window.t('common.networkError'), 'error'); });
        });
    }

    loadFriends();
})();
