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
    function escapeHtml(str) {
        if (!str) return '';
        var d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    function formatSize(bytes) {
        if (!bytes) return '-';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KiB';
        return (bytes / (1024 * 1024)).toFixed(2) + ' MiB';
    }

    function showToast(message, type) {
        var toast = document.getElementById('toast');
        if (toast) {
            toast.textContent = message;
            toast.className = 'toast toast-' + (type || 'info');
            toast.style.display = 'block';
            setTimeout(function() { toast.style.display = 'none'; }, 3000);
        }
    }

    async function loadShared() {
        try {
            var resp = await fetch('/api/shared/my');
            if (resp.status === 401) { window.location.href = '/login'; return; }
            var data = await resp.json();
            if (!data.success) { showToast('加载失败', 'error'); return; }

            var friendTex = (data.friendShared || []).map(function(t) { t.source = 'friend'; return t; });
            var favTex = (data.favorites || []).map(function(t) { t.source = 'favorite'; return t; });
            var seen = {};
            var all = [];
            friendTex.concat(favTex).forEach(function(t) {
                if (!seen[t.id]) { seen[t.id] = true; all.push(t); }
            });
            renderAll(all);
        } catch (err) {
            showToast('网络错误', 'error');
        }
    }

    function renderAll(textures) {
        var grid = document.getElementById('sharedGrid');
        if (!grid) return;

        if (!textures || textures.length === 0) {
            grid.innerHTML = '<p class="text-muted" style="grid-column:1/-1">暂无共享材质</p>';
            return;
        }

        grid.innerHTML = '';
        textures.forEach(function(t) {
            var displayName = t.favoriteAlias || t.alias || t.originalName || t.hash;
            var isFriend = t.source === 'friend';
            var badgeColor = isFriend ? '#5897fb' : '#0bda51';
            var badgeLabel = isFriend ? '\u597D\u53CB' : '\u6536\u85CF';
            var card = document.createElement('div');
            card.className = 'texture-item card-animate texture-card-clickable';
            card.setAttribute('data-id', t.id);
            card.setAttribute('data-type', t.type);
            card.setAttribute('data-hash', t.hash);
            card.setAttribute('data-alias', t.favoriteAlias || t.alias || '');
            card.setAttribute('data-original-name', t.originalName || '');
            card.innerHTML =
                '<div class="texture-thumb"><canvas></canvas></div>' +
                '<div class="texture-name">' + escapeHtml(displayName) + '</div>' +
                '<div class="texture-meta">' + (t.ownerName ? escapeHtml(t.ownerName) + ' &middot; ' : '') + (t.type === 'CAPE' ? '\u62AB\u98CE' : '\u76AE\u80A4') + '</div>' +
                '<span style="position:absolute;top:8px;right:8px;font-size:10px;padding:1px 6px;border-radius:8px;border:1px solid ' + badgeColor + ';color:' + badgeColor + '">' + badgeLabel + '</span>';

            card.addEventListener('click', function() {
                showSharedDetail(t);
            });
            grid.appendChild(card);
            var canvas = card.querySelector('canvas');
            if (t.type === 'CAPE') {
                drawCapeThumb(canvas, t.thumbnailUrl);
            } else {
                drawFace(canvas, t.thumbnailUrl);
            }
        });
    }

    function showSharedDetail(t) {
        var existing = document.getElementById('detailModal');
        if (existing) existing.remove();

        var displayName = t.favoriteAlias || t.alias || t.originalName || t.hash;
        var isFriend = t.source === 'friend';
        var sourceLabel = isFriend ? ('\u6765\u81EA ' + (t.ownerName || '\u597D\u53CB')) : ('\u6765\u81EA ' + (t.ownerName || '\u6750\u8D28\u5E93'));

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'detailModal';

        var box = document.createElement('div');
        box.className = 'modal-box texture-detail-box';
        box.innerHTML =
            '<button class="modal-close-btn">&times;</button>' +
            '<div class="detail-alias">' + escapeHtml(displayName) + '</div>' +
            '<div class="detail-meta">' + escapeHtml(sourceLabel) + ' &middot; ' + (t.type === 'CAPE' ? '\u62AB\u98CE' : '\u76AE\u80A4') + '</div>' +
            '<div class="detail-preview"><canvas id="shared3dCanvas"></canvas></div>' +
            '<div class="form-group" style="text-align:left"><label class="form-label">\u522B\u540D</label>' +
            '<input type="text" class="form-input" id="sharedAliasInput" value="' + escapeHtml(t.favoriteAlias || t.alias || '') + '">' +
            '</div>' +
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

        if (!isFriend) {
            var saveAliasBtn = document.createElement('button');
            saveAliasBtn.className = 'btn btn-primary';
            saveAliasBtn.textContent = '\u4FDD\u5B58\u522B\u540D';
            saveAliasBtn.addEventListener('click', function() {
                saveAlias(t.id, document.getElementById('sharedAliasInput').value.trim());
            });
            actionsDiv.appendChild(saveAliasBtn);
        }

        if (isFriend) {
            var returnBtn = document.createElement('button');
            returnBtn.className = 'btn btn-danger';
            returnBtn.textContent = '\u8FD4\u8FD8';
            returnBtn.addEventListener('click', function() {
                returnShared(t.id);
            });
            actionsDiv.appendChild(returnBtn);
        } else {
            var unfavBtn = document.createElement('button');
            unfavBtn.className = 'btn btn-danger';
            unfavBtn.textContent = '\u53D6\u6D88\u6536\u85CF';
            unfavBtn.addEventListener('click', function() {
                removeFavorite(t.id);
            });
            actionsDiv.appendChild(unfavBtn);
        }

        box.querySelector('.modal-close-btn').addEventListener('click', closeDetail);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeDetail(); });
    }

    async function saveAlias(textureId, alias) {
        var msgDiv = document.getElementById('aliasMsg');
        try {
            var resp = await fetch('/api/world/favorite/alias', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId, alias: alias })
            });
            var data = await resp.json();
            if (data.success) {
                showToast('别名已更新', 'success');
                closeDetail();
                loadShared();
            } else {
                msgDiv.textContent = '✗ ' + (data.message || '失败');
                msgDiv.className = 'msg-area error';
            }
        } catch (err) {
            showToast('网络错误', 'error');
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
                showToast('已取消收藏', 'success');
                closeDetail();
                loadShared();
            } else {
                showToast(data.message || '操作失败', 'error');
            }
        } catch (err) {
            showToast('网络错误', 'error');
        }
    }

    async function returnShared(textureId) {
        try {
            var resp = await fetch('/api/friends/return-texture', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId })
            });
            var data = await resp.json();
            if (data.success) {
                showToast('已返还', 'success');
                closeDetail();
                loadShared();
            } else {
                showToast(data.message || '操作失败', 'error');
            }
        } catch (err) {
            showToast('网络错误', 'error');
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
