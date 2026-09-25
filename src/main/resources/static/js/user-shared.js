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

var shared = (function() {
    async function loadShared() {
        try {
            var results = await Promise.all([
                fetch('/api/shared/outgoing'),
                fetch('/api/shared/incoming'),
                fetch('/api/shared/my')
            ]);
            if (results.some(function(r) { return r.status === 401; })) {
                window.location.href = '/login';
                return;
            }
            var outgoing = await results[0].json();
            var incoming = await results[1].json();
            var mine = await results[2].json();

            renderGrid('outgoingGrid', outgoing.success ? (outgoing.textures || []) : [], 'outgoing',
                t('shared.emptyOutgoing'));
            renderGrid('incomingGrid', incoming.success ? (incoming.textures || []) : [], 'incoming',
                t('shared.emptyIncoming'));
            renderGrid('favoritesGrid', mine.success ? (mine.favorites || []) : [], 'favorite',
                t('shared.empty'));
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    function renderGrid(gridId, textures, mode, emptyText) {
        var grid = document.getElementById(gridId);
        if (!grid) return;
        if (!textures || textures.length === 0) {
            grid.innerHTML = '<p class="text-muted" style="grid-column:1/-1">' + escapeHtml(emptyText) + '</p>';
            return;
        }
        grid.innerHTML = '';
        textures.forEach(function(t) {
            var displayName = t.favoriteAlias || t.alias || t.originalName || t.hash;
            var typeLabel = t.type === 'CAPE' ? window.t('texture.cape') : window.t('texture.skin');
            var subtitle;
            if (mode === 'outgoing') {
                subtitle = window.t('shared.shareTo', t.friendName || '');
            } else if (mode === 'incoming') {
                subtitle = window.t('shared.fromFriend', t.ownerName || window.t('shared.friend'));
            } else {
                subtitle = t.ownerName ? window.t('shared.fromLibrary', t.ownerName) : typeLabel;
            }
            var card = document.createElement('div');
            card.className = 'texture-item card-animate texture-card-clickable';
            card.innerHTML =
                '<div class="texture-thumb"><canvas></canvas></div>' +
                '<div class="texture-item-body">' +
                '<div class="texture-name">' + escapeHtml(displayName) + '</div>' +
                '<div class="texture-meta">' + escapeHtml(subtitle) + ' &middot; ' + typeLabel + '</div>' +
                '</div>' +
                '<span class="shared-badge shared-badge-' + mode + '">' +
                escapeHtml(window.t(mode === 'outgoing' ? 'shared.badgeOutgoing'
                    : (mode === 'incoming' ? 'shared.badgeIncoming' : 'shared.favBadge'))) + '</span>';

            card.addEventListener('click', function() { showDetail(t, mode); });
            grid.appendChild(card);
            var canvas = card.querySelector('canvas');
            if (t.type === 'CAPE') {
                drawCapeThumb(canvas, t.thumbnailUrl);
            } else {
                drawFace(canvas, t.thumbnailUrl);
            }
        });
    }

    function showDetail(t, mode) {
        var existing = document.getElementById('detailModal');
        if (existing) existing.remove();

        var displayName = t.favoriteAlias || t.alias || t.originalName || t.hash;
        var typeLabel = t.type === 'CAPE' ? window.t('texture.cape') : window.t('texture.skin');
        var sourceLabel;
        if (mode === 'outgoing') {
            sourceLabel = window.t('shared.shareTo', t.friendName || '');
        } else if (mode === 'incoming') {
            sourceLabel = window.t('shared.fromFriend', t.ownerName || window.t('shared.friend'));
        } else {
            sourceLabel = t.ownerName ? window.t('shared.fromLibrary', t.ownerName) : typeLabel;
        }

        var aliasBlock = '';
        if (mode !== 'outgoing') {
            aliasBlock =
                '<div class="form-group" style="text-align:left"><label class="form-label">' + window.t('shared.alias') + '</label>' +
                '<input type="text" class="form-input" id="sharedAliasInput" value="' + escapeHtml(t.favoriteAlias || t.alias || '') + '">' +
                '</div>';
        }

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'detailModal';

        var box = document.createElement('div');
        box.className = 'modal-box texture-detail-box';
        box.innerHTML =
            '<button class="modal-close-btn">&times;</button>' +
            '<div class="detail-alias">' + escapeHtml(displayName) + '</div>' +
            '<div class="detail-meta">' + escapeHtml(sourceLabel) + ' &middot; ' + typeLabel + '</div>' +
            '<div class="detail-preview"><canvas id="shared3dCanvas"></canvas></div>' +
            aliasBlock +
            '<div id="aliasMsg" class="msg-area"></div>' +
            '<div class="detail-actions" id="sharedDetailActions"></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        if (typeof skinview3d !== 'undefined') {
            try {
                var canvas = document.getElementById('shared3dCanvas');
                var previewDiv = canvas.parentElement;
                var opts = {
                    canvas: canvas,
                    width: previewDiv.clientWidth || 300,
                    height: 320,
                    skin: t.thumbnailUrl,
                    model: 'slim'
                };
                if (t.type === 'CAPE') {
                    opts.skin = '/img/juststeve.png';
                    opts.cape = t.thumbnailUrl;
                }
                window._sharedViewer = new skinview3d.SkinViewer(opts);
                window._sharedViewer.autoRotate = true;
                window._sharedViewer.animation = new skinview3d.WalkingAnimation();
            } catch(e) {}
        }

        var actionsDiv = box.querySelector('#sharedDetailActions');

        if (mode === 'outgoing') {
            var revokeBtn = document.createElement('button');
            revokeBtn.className = 'btn btn-danger';
            revokeBtn.textContent = window.t('shared.revokeShare');
            revokeBtn.addEventListener('click', function() { revokeShare(t); });
            actionsDiv.appendChild(revokeBtn);
        } else if (mode === 'incoming') {
            var applyBtn = document.createElement('button');
            applyBtn.className = 'btn btn-primary';
            applyBtn.textContent = window.t('texture.applyToProfile');
            applyBtn.addEventListener('click', function() {
                window.applyTextureToProfile(t.type, t.hash);
            });
            actionsDiv.appendChild(applyBtn);

            var saveAliasBtn = document.createElement('button');
            saveAliasBtn.className = 'btn btn-secondary';
            saveAliasBtn.textContent = window.t('shared.saveAlias');
            saveAliasBtn.addEventListener('click', function() {
                saveReceivedAlias(t.textureId, document.getElementById('sharedAliasInput').value.trim());
            });
            actionsDiv.appendChild(saveAliasBtn);

            var delBtn = document.createElement('button');
            delBtn.className = 'btn btn-danger';
            delBtn.textContent = window.t('common.delete');
            delBtn.addEventListener('click', function() { deleteReceived(t); });
            actionsDiv.appendChild(delBtn);
        } else {
            var applyFavBtn = document.createElement('button');
            applyFavBtn.className = 'btn btn-primary';
            applyFavBtn.textContent = window.t('texture.applyToProfile');
            applyFavBtn.addEventListener('click', function() {
                window.applyTextureToProfile(t.type, t.hash);
            });
            actionsDiv.appendChild(applyFavBtn);

            var saveFavAliasBtn = document.createElement('button');
            saveFavAliasBtn.className = 'btn btn-secondary';
            saveFavAliasBtn.textContent = window.t('shared.saveAlias');
            saveFavAliasBtn.addEventListener('click', function() {
                saveFavoriteAlias(t.id, document.getElementById('sharedAliasInput').value.trim());
            });
            actionsDiv.appendChild(saveFavAliasBtn);

            var unfavBtn = document.createElement('button');
            unfavBtn.className = 'btn btn-danger';
            unfavBtn.textContent = window.t('shared.unfavorite');
            unfavBtn.addEventListener('click', function() { removeFavorite(t.id); });
            actionsDiv.appendChild(unfavBtn);
        }

        box.querySelector('.modal-close-btn').addEventListener('click', closeDetail);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeDetail(); });
    }

    async function saveFavoriteAlias(textureId, alias) {
        try {
            var resp = await fetch('/api/world/favorite/alias', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId, alias: alias })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(t('shared.aliasUpdated'), 'success');
                closeDetail();
                loadShared();
            } else {
                var msgDiv = document.getElementById('aliasMsg');
                if (msgDiv) { msgDiv.textContent = data.message || t('shared.failed'); msgDiv.className = 'msg-area error'; }
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    async function saveReceivedAlias(textureId, alias) {
        try {
            var resp = await fetch('/api/friends/received-texture/alias', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId, alias: alias })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(t('shared.aliasUpdated'), 'success');
                closeDetail();
                loadShared();
            } else {
                var msgDiv = document.getElementById('aliasMsg');
                if (msgDiv) { msgDiv.textContent = data.message || t('shared.failed'); msgDiv.className = 'msg-area error'; }
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    async function revokeShare(t) {
        try {
            var resp = await fetch('/api/friends/unshare-texture', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ friendId: t.friendId, textureId: t.textureId })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || t('shared.shareRevoked'), 'success');
                closeDetail();
                loadShared();
            } else {
                showToast(data.message || t('texture.operationFailed'), 'error');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    async function deleteReceived(t) {
        try {
            var resp = await fetch('/api/friends/received-texture/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: t.textureId })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || t('shared.receivedDeleted'), 'success');
                closeDetail();
                loadShared();
            } else {
                showToast(data.message || t('texture.operationFailed'), 'error');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    async function removeFavorite(textureId) {
        try {
            var resp = await fetch('/api/world/favorite', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId })
            });
            var data = await resp.json();
            if (data.success) {
                showToast(t('shared.favRemoved'), 'success');
                closeDetail();
                loadShared();
            } else {
                showToast(data.message || t('texture.operationFailed'), 'error');
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    }

    function closeDetail() {
        if (window._sharedViewer) { window._sharedViewer.dispose(); window._sharedViewer = null; }
        var modal = document.getElementById('detailModal');
        if (modal) modal.remove();
    }

    function drawFace(canvas, url) {
        canvas.width = 64; canvas.height = 64;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FFF0F5'; ctx.fillRect(0, 0, 64, 64);
        var img = new Image();
        img.onload = function() {
            ctx.imageSmoothingEnabled = false;
            ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 64, 64);
            if (!(img.width === 64 && img.height === 32)) {
                ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 64, 64);
            }
        };
        img.src = url;
    }

    function drawCapeThumb(canvas, url) {
        canvas.width = 64; canvas.height = 64;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FFF0F5'; ctx.fillRect(0, 0, 64, 64);
        var img = new Image();
        img.onload = function() {
            ctx.imageSmoothingEnabled = false;
            var dstW = 64, dstH = 64;
            var padX = (dstW - img.width) / 2;
            var padY = (dstH - img.height) / 2;
            ctx.drawImage(img, Math.max(0, padX), Math.max(0, padY), img.width, img.height);
        };
        img.src = url;
    }

    return { loadShared: loadShared };
})();

document.addEventListener('DOMContentLoaded', function() { shared.loadShared(); });
