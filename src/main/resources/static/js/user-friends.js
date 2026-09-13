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
            '<div id="friendSharedList" style="max-height:160px;overflow-y:auto;border:1px solid #FFD6E8;border-radius:8px;padding:4px">' +
            '<p class="text-muted" style="padding:8px;font-size:12px">' + window.t('friends.loading') + '</p></div>' +
            '</div>' +
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

        loadFriendShared(dataset.friendId);
    }

    async function loadFriendShared(friendId) {
        var container = document.getElementById('friendSharedList');
        if (!container) return;
        try {
            var [skinsResp, capesResp, mySharedResp] = await Promise.all([
                fetch('/api/skins'),
                fetch('/api/capes'),
                fetch('/api/friends/' + encodeURIComponent(friendId) + '/my-shared')
            ]);
            if (skinsResp.status === 401) return;
            var skinsData = await skinsResp.json();
            var capesData = await capesResp.json();
            var mySharedData = await mySharedResp.json();

            var mySkins = (skinsData.success && skinsData.skins) ? skinsData.skins : [];
            var myCapes = (capesData.success && capesData.capes) ? capesData.capes : [];
            var sharedIds = {};
            if (mySharedData.success && mySharedData.textures) {
                mySharedData.textures.forEach(function(t) { sharedIds[t.id] = true; });
            }

            var allTextures = [];
            mySkins.forEach(function(s) { s.type = 'SKIN'; allTextures.push(s); });
            myCapes.forEach(function(c) { c.type = 'CAPE'; allTextures.push(c); });

            if (allTextures.length === 0) {
                container.innerHTML = '<p class="text-muted" style="padding:8px;font-size:12px">' + window.t('friends.noTextures') + '</p>';
                return;
            }

            var html = '';
            allTextures.forEach(function(t) {
                var displayName = t.alias || t.originalName || t.hash;
                var checked = sharedIds[t.id] ? ' checked' : '';
                var typeLabel = t.type === 'CAPE' ? window.t('texture.cape') : window.t('texture.skin');
                html += '<label style="display:flex;align-items:center;gap:6px;padding:4px 6px;cursor:pointer;font-size:12px;border-radius:4px;' +
                    '" onmouseover="this.style.background=\'#FFF8FC\'" onmouseout="this.style.background=\'\'">' +
                    '<input type="checkbox" class="friend-share-check"' + checked +
                    ' data-tid="' + escapeHtml(t.id) + '" data-fid="' + escapeHtml(friendId) + '">' +
                    '<span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + escapeHtml(displayName) + '</span>' +
                    '<span style="color:#999;font-size:10px">' + typeLabel + '</span></label>';
            });
            container.innerHTML = html;

            container.querySelectorAll('.friend-share-check').forEach(function(cb) {
                cb.addEventListener('change', function() {
                    toggleShareTextureToFriend(this.dataset.fid, this.dataset.tid, this.checked);
                });
            });
        } catch (err) {
            container.innerHTML = '<p class="text-muted" style="padding:8px;font-size:12px">' + window.t('friends.loadFailed') + '</p>';
        }
    }

    async function toggleShareTextureToFriend(friendId, textureId, share) {
        try {
            var url = share ? '/api/friends/share-texture' : '/api/friends/unshare-texture';
            var resp = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendId: friendId, textureId: textureId })
            });
            var data = await resp.json();
            if (!data.success) {
                showToast(data.message || window.t('texture.operationFailed'), 'error');
            }
        } catch (err) {
            showToast(window.t('common.networkError'), 'error');
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
