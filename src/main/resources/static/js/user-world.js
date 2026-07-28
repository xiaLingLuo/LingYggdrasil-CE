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
var world = (function() {
    var currentType = '';
    var after = null;
    var loading = false;
    var hasMore = true;

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

    function init() {
        var tabs = document.querySelectorAll('#worldTabs .world-tab');
        tabs.forEach(function(tab) {
            tab.addEventListener('click', function() {
                tabs.forEach(function(t) { t.classList.remove('active'); });
                this.classList.add('active');
                currentType = this.dataset.type;
                resetAndLoad();
            });
        });
        resetAndLoad();
        setupInfiniteScroll();
    }

    function resetAndLoad() {
        after = null;
        hasMore = true;
        document.getElementById('worldTextureGrid').innerHTML = '';
        document.getElementById('worldEnd').style.display = 'none';
        loadTextures();
    }

    function setupInfiniteScroll() {
        var observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting && !loading && hasMore) {
                    loadTextures();
                }
            });
        }, { rootMargin: '200px' });
        var sentinel = document.getElementById('worldEnd');
        observer.observe(sentinel);
        document.getElementById('worldLoading').style.display = 'none';
    }

    async function loadTextures() {
        if (loading || !hasMore) return;
        loading = true;
        document.getElementById('worldLoading').style.display = 'flex';

        var params = 'limit=20';
        if (currentType) params += '&type=' + encodeURIComponent(currentType);
        if (after) params += '&after=' + encodeURIComponent(after);

        try {
            var resp = await fetch('/api/world/textures?' + params);
            if (resp.status === 401) { window.location.href = '/login'; return; }
            var data = await resp.json();
            if (!data.success) { showToast(data.message || '加载失败', 'error'); return; }

            var textures = data.textures || [];
            after = data.nextAfter;
            hasMore = data.hasMore;

            if (textures.length === 0 && after === null) {
                var grid = document.getElementById('worldTextureGrid');
                grid.innerHTML = '<p class="text-muted" style="grid-column:1/-1;text-align:center;padding:40px">暂无公开材质 <i class="fas fa-leaf"></i></p>';
                document.getElementById('worldEnd').style.display = 'none';
                loading = false;
                document.getElementById('worldLoading').style.display = 'none';
                return;
            }

            renderTextures(textures, 'worldTextureGrid');

            if (data.loginRequired) {
                document.getElementById('worldEnd').innerHTML = '<a href="/login" style="color:#FF69B4;text-decoration:none;font-weight:600">登录以查看更多哦 <i class="fas fa-arrow-right"></i></a>';
                document.getElementById('worldEnd').style.display = 'block';
                hasMore = false;
            } else if (!hasMore) {
                document.getElementById('worldEnd').style.display = 'block';
            }
        } catch (err) {
            showToast('网络错误', 'error');
        } finally {
            loading = false;
            document.getElementById('worldLoading').style.display = 'none';
        }
    }

    function renderTextures(textures, gridId) {
        var grid = document.getElementById(gridId);
        if (!grid) return;
        textures.forEach(function(t) {
            var displayName = t.alias || t.originalName || t.hash;
            var card = document.createElement('div');
            card.className = 'texture-item card-animate texture-card-clickable';
            card.setAttribute('data-id', t.id);
            card.setAttribute('data-type', t.type);
            card.setAttribute('data-hash', t.hash);
            card.setAttribute('data-alias', t.alias || '');
            card.setAttribute('data-original-name', t.originalName || '');
            card.setAttribute('data-size', t.size || 0);
            card.innerHTML =
                '<div class="texture-thumb"><canvas></canvas></div>' +
                '<div class="texture-name">' + escapeHtml(displayName) + '</div>' +
                '<div class="texture-meta">' + escapeHtml(t.ownerName || '') + ' &middot; ' + formatSize(t.size) + '</div>' +
                '<div class="texture-card-footer">' +
                '<span class="texture-card-owner">' + (t.type === 'CAPE' ? '披风' : '皮肤') + '</span>' +
                '<button class="like-btn' + (t.liked ? ' liked' : '') + '" data-tid="' + escapeHtml(t.id) + '">' +
                '<span class="like-heart">' + (t.liked ? '❤️' : '🤍') + '</span> ' +
                '<span class="like-count">' + (t.likeCount || 0) + '</span></button>' +
                '<button class="fav-btn' + (t.favorited ? ' favorited' : '') + '" data-tid="' + escapeHtml(t.id) + '">' +
                '<span>' + (t.favorited ? '⭐' : '☆') + '</span></button></div>';

            card.addEventListener('click', function(e) {
                if (e.target.closest('.like-btn') || e.target.closest('.fav-btn')) return;
                showTextureDetail(t);
            });

            card.querySelector('.like-btn').addEventListener('click', function(e) {
                e.stopPropagation();
                toggleLike(t.id, this);
            });

            card.querySelector('.fav-btn').addEventListener('click', function(e) {
                e.stopPropagation();
                toggleFavorite(t.id, this);
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

    async function callFavoriteApi(textureId) {
        try {
            var resp = await fetch('/api/world/favorite', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId })
            });
            if (resp.redirected) {
                showToast('请先登录，正在跳转到注册页...', 'info');
                setTimeout(function(){window.location.href='/register'}, 3000);
                return { success: false };
            }
            var data = await resp.json();
            if (data.success) {
                showToast(data.favorited ? '收藏成功啦~快去你的共享材质库看看吧~' : '已取消收藏', 'success');
            } else {
                showToast(data.message || '操作失败', 'error');
            }
            return data;
        } catch (err) {
            showToast('网络错误', 'error');
            return { success: false };
        }
    }

    async function toggleLike(textureId, btn) {
        try {
            var resp = await fetch('/api/world/like', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: textureId })
            });
            if (resp.redirected) {
                showToast('请先登录，正在跳转到注册页...', 'info');
                setTimeout(function(){window.location.href='/register'}, 3000);
                return;
            }
            var data = await resp.json();
            if (!data.success) { showToast(data.message || '操作失败', 'error'); return; }
            var liked = data.liked;
            btn.classList.toggle('liked', liked);
            var heart = btn.querySelector('.like-heart');
            if (heart) heart.textContent = liked ? '❤️' : '🤍';
            var countEl = btn.querySelector('.like-count');
            if (countEl) {
                var current = parseInt(countEl.textContent) || 0;
                countEl.textContent = liked ? current + 1 : Math.max(0, current - 1);
            }
            if (liked) showToast('感谢大大支持！这个材质会被更多人看到！', 'success');
        } catch (err) {
            showToast('网络错误', 'error');
        }
    }

    async function toggleFavorite(textureId, btn) {
        var data = await callFavoriteApi(textureId);
        if (!data.success) return;
        var favd = data.favorited;
        btn.classList.toggle('favorited', favd);
        btn.querySelector('span').textContent = favd ? '⭐' : '☆';
    }

    function showTextureDetail(t) {
        var existing = document.getElementById('detailModal');
        if (existing) existing.remove();

        var displayName = t.alias || t.originalName || t.hash;
        var previewUrl = t.thumbnailUrl;

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'detailModal';

        var box = document.createElement('div');
        box.className = 'modal-box texture-detail-box';
        box.innerHTML =
            '<button class="modal-close-btn">&times;</button>' +
            '<div class="detail-alias">' + escapeHtml(displayName) + '</div>' +
            '<div class="detail-meta">' + escapeHtml(t.ownerName || '') + ' &middot; ' + formatSize(t.size) + '</div>' +
            '<div class="detail-preview"><canvas id="world3dCanvas"></canvas></div>' +
            '<div class="detail-actions" id="worldDetailActions"></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        if (typeof skinview3d !== 'undefined') {
            try {
                var canvas = document.getElementById('world3dCanvas');
                var previewDiv = canvas.parentElement;
                var opts = {
                    canvas: canvas,
                    width: previewDiv.clientWidth || 300,
                    height: 320,
                    skin: previewUrl,
                    model: 'slim'
                };
                if (t.type === 'CAPE') {
                    opts.skin = '/img/juststeve.png';
                    opts.cape = previewUrl;
                }
                window._worldViewer = new skinview3d.SkinViewer(opts);
                window._worldViewer.autoRotate = true;
                window._worldViewer.animation = new skinview3d.WalkingAnimation();
            } catch(e) {}
        }

        var actionsDiv = box.querySelector('#worldDetailActions');

        var dlBtn = document.createElement('button');
        dlBtn.className = 'btn btn-secondary';
        dlBtn.textContent = '下载';
        dlBtn.addEventListener('click', function() {
            if (!window.CSRF_TOKEN) { showToast('请先登录，正在跳转到注册页...', 'info'); setTimeout(function(){window.location.href='/register'}, 3000); return; }
            window.open('/api/skins/download?id=' + encodeURIComponent(t.id), '_blank');
        });
        actionsDiv.appendChild(dlBtn);

        var favBtn = document.createElement('button');
        favBtn.className = 'btn btn-secondary';
        favBtn.textContent = t.favorited ? '已收藏' : '收藏';
        favBtn.addEventListener('click', async function() {
            var result = await callFavoriteApi(t.id);
            if (result.success) { closeDetailModal(); resetAndLoad(); }
        });
        actionsDiv.appendChild(favBtn);

        box.querySelector('.modal-close-btn').addEventListener('click', closeDetailModal);
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeDetailModal(); });
    }

    function closeDetailModal() {
        if (window._worldViewer) { window._worldViewer.dispose(); window._worldViewer = null; }
        var modal = document.getElementById('detailModal');
        if (modal) modal.remove();
    }

    return { init: init };
})();

document.addEventListener('DOMContentLoaded', function() { world.init(); });
