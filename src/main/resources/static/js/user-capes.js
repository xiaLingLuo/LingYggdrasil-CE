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
    async function loadCapes() {
        var container = document.getElementById('capeList');
        if (!container) return;

        try {
            var resp = await fetch('/api/capes');
            if (resp.status === 401) { window.location.href = '/login'; return; }
            var data = await resp.json();

            if (data.success && data.capes && data.capes.length > 0) {
                var html = '';
                data.capes.forEach(function(c) {
                    var displayName = c.alias || c.originalName || c.hash;
                    var previewUrl = '/api/capes/download?id=' + encodeURIComponent(c.id);
                    var visColor = c.isPublic ? '#0bda51' : '#5897fb';
                    var visLabel = c.isPublic ? t('texture.publicLabel') : t('texture.privateLabel');
                    html += '<div class="texture-item card-animate texture-card-clickable" id="cape-' + escapeHtml(c.id) + '"' +
                        ' data-id="' + escAttr(c.id) + '"' +
                        ' data-alias="' + escAttr(c.alias || '') + '"' +
                        ' data-original-name="' + escAttr(c.originalName || '') + '"' +
                        ' data-hash="' + escAttr(c.hash) + '"' +
                        ' data-size="' + escAttr(String(c.size)) + '"' +
                        ' data-vis="' + (c.isPublic ? '1' : '0') + '">' +
                        '<div class="texture-thumb"><canvas></canvas></div>' +
                        '<div class="texture-item-body">' +
                        '<div class="texture-name">\u273F ' + escapeHtml(displayName) + '</div>' +
                        '<div class="texture-meta">' + formatSize(c.size) + '</div>' +
                        '</div>' +
                        '<span style="position:absolute;top:8px;right:8px;font-size:10px;padding:1px 6px;border-radius:8px;border:1px solid ' + visColor + ';color:' + visColor + '">' + visLabel + '</span>' +
                        '</div>';
                });
                html += '<div class="upload-tile card-animate" id="capeUploadTile">' +
                    '<div class="upload-tile-icon">' +
                    '<svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>' +
                    '</div>' +
                    '<div class="upload-tile-text">' + t('texture.uploadCape') + '</div>' +
                    '<div class="upload-tile-hint">' + t('texture.pngHint') + '</div>' +
                    '</div>';
                container.innerHTML = html;

                container.querySelectorAll('.texture-card-clickable').forEach(function(card) {
                    card.addEventListener('click', function() {
                        showCapeDetail(
                            this.dataset.id,
                            this.dataset.alias,
                            this.dataset.originalName,
                            this.dataset.hash,
                            this.dataset.size
                        );
                    });
                });

                container.querySelectorAll('.texture-thumb canvas').forEach(function(c) {
                    var card = c.closest('.texture-card-clickable');
                    var url = '/api/capes/download?id=' + encodeURIComponent(card.dataset.id);
                    drawCapeThumb(c, url);
                });
            } else {
                container.innerHTML = '<p class="empty-hint" style="grid-column:1/-1">' + t('texture.emptyCapes') + ' <i class="fas fa-leaf"></i></p>' +
                    '<div class="upload-tile card-animate" id="capeUploadTile">' +
                    '<div class="upload-tile-icon">' +
                    '<svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>' +
                    '</div>' +
                    '<div class="upload-tile-text">' + t('texture.uploadCape') + '</div>' +
                    '<div class="upload-tile-hint">' + t('texture.pngHint') + '</div>' +
                    '</div>';
            }
        } catch (err) {
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">' + t('texture.loadFailed') + '</p>';
        }
    }

    var fileInput = document.getElementById('capeFile');
    var gridContainer = document.getElementById('capeList');

    if (gridContainer) {
        gridContainer.addEventListener('click', function(e) {
            if (e.target.closest('#capeUploadTile')) fileInput.click();
        });
        ['dragenter', 'dragover'].forEach(function(evt) {
            gridContainer.addEventListener(evt, function(e) {
                e.preventDefault(); e.stopPropagation();
                var tile = document.getElementById('capeUploadTile');
                if (tile) tile.style.borderColor = '#FF69B4';
            });
        });
        ['dragleave', 'drop'].forEach(function(evt) {
            gridContainer.addEventListener(evt, function(e) {
                e.preventDefault(); e.stopPropagation();
                var tile = document.getElementById('capeUploadTile');
                if (tile) tile.style.borderColor = '';
            });
        });
        gridContainer.addEventListener('drop', function(e) {
            e.preventDefault();
            var files = e.dataTransfer.files;
            if (files && files.length > 0) {
                if (files[0].type === 'image/png') {
                    var dt = new DataTransfer(); dt.items.add(files[0]); fileInput.files = dt.files;
                    showUploadModal(files[0]);
                } else { showToast(t('texture.pngOnly'), 'error'); }
            }
        });
    }
    
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files && this.files[0]) {
                if (this.files[0].type !== 'image/png') { showToast(t('texture.pngOnly'), 'error'); return; }
                showUploadModal(this.files[0]);
            }
        });
    }

    loadCapes();

    window.showCapeDetail = function(id, alias, originalName, hash, size) {
        var existing = document.getElementById('detailModal');
        if (existing) existing.remove();

        var displayName = alias || originalName || hash;
        var previewUrl = '/api/capes/download?id=' + encodeURIComponent(id);

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'detailModal';

        var modalBox = document.createElement('div');
        modalBox.className = 'modal-box texture-detail-box';
        modalBox.innerHTML =
            '<button class="modal-close-btn" title="' + t('common.close') + '" id="detailCloseBtn">&times;</button>' +
            '<div class="detail-alias">\u273F ' + escapeHtml(displayName) + '</div>' +
            '<div class="detail-meta">' + escapeHtml(originalName || hash) + ' &middot; ' + formatSize(parseInt(size) || 0) + '</div>' +
            '<div class="detail-preview"><canvas id="cape3dCanvas"></canvas></div>' +
            '<div class="detail-actions" id="detailActions"></div>';

        overlay.appendChild(modalBox);
        document.body.appendChild(overlay);

        window._capeViewer = null;
        if (typeof skinview3d !== 'undefined') {
            try {
                var canvas = document.getElementById('cape3dCanvas');
                var previewDiv = canvas.parentElement;
                window._capeViewer = new skinview3d.SkinViewer({
                    canvas: canvas,
                    width: previewDiv.clientWidth || 300,
                    height: 320,
                    cape: previewUrl,
                    model: 'slim'
                });
                window._capeViewer.autoRotate = true;
                window._capeViewer.animation = new skinview3d.WalkingAnimation();
            } catch(e) {  }
        }

        var actionsDiv = modalBox.querySelector('#detailActions');

        var dlBtn = document.createElement('button');
        dlBtn.className = 'btn btn-secondary';
        dlBtn.textContent = t('texture.download');
        dlBtn.addEventListener('click', function() { downloadCape(id); });
        actionsDiv.appendChild(dlBtn);

        var applyBtn = document.createElement('button');
        applyBtn.className = 'btn btn-primary';
        applyBtn.textContent = t('texture.applyToProfile');
        applyBtn.addEventListener('click', function() { applyTextureToProfile('CAPE', hash); });
        actionsDiv.appendChild(applyBtn);

        var aliasBtn = document.createElement('button');
        aliasBtn.className = 'btn btn-secondary';
        aliasBtn.textContent = t('texture.editAlias');
        aliasBtn.addEventListener('click', function() { editAlias(id, alias); });
        actionsDiv.appendChild(aliasBtn);

        var delBtn = document.createElement('button');
        delBtn.className = 'btn btn-danger';
        delBtn.textContent = t('common.delete');
        delBtn.addEventListener('click', function() { deleteCape(id, displayName); });
        actionsDiv.appendChild(delBtn);

        var visWrap = document.createElement('div');
        visWrap.className = 'visibility-segment';
        visWrap.id = 'visSeg-' + id;

        var privBtn = document.createElement('button');
        privBtn.type = 'button';
        privBtn.className = 'vis-option';
        privBtn.textContent = t('texture.private');
        privBtn.addEventListener('click', function() { setVisibility(id, false, visWrap); });

        var pubBtn = document.createElement('button');
        pubBtn.type = 'button';
        pubBtn.className = 'vis-option';
        pubBtn.textContent = t('texture.public');
        pubBtn.addEventListener('click', function() { setVisibility(id, true, visWrap); });

        visWrap.appendChild(privBtn);
        visWrap.appendChild(pubBtn);
        actionsDiv.appendChild(visWrap);

        fetchVisibilityInto(id, visWrap);

        modalBox.querySelector('#detailCloseBtn').addEventListener('click', function() { closeDetailModal(); });
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) closeDetailModal();
        });
    };

    window.closeDetailModal = function() {
        if (window._capeViewer) {
            window._capeViewer.dispose();
            window._capeViewer = null;
        }
        var modal = document.getElementById('detailModal');
        if (modal) modal.remove();
    };

    window.editAlias = function(id, currentAlias) {
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'aliasModal';
        overlay.style.zIndex = '1050';

        overlay.innerHTML = '<div class="modal-box">' +
            '<h3>' + t('texture.editAlias') + '</h3>' +
            '<div class="form-group">' +
            '<label class="form-label">' + t('texture.newAlias') + '</label>' +
            '<input type="text" class="form-input" id="newAlias" value="' + escapeHtml(currentAlias) + '">' +
            '</div>' +
            '<div id="aliasMsg" class="msg-area"></div>' +
            '<div class="modal-actions" id="aliasModalActions"></div></div>';

        document.body.appendChild(overlay);

        var actionsDiv = overlay.querySelector('#aliasModalActions');
        var cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = t('common.cancel');
        cancelBtn.addEventListener('click', function() { closeAliasModal(); });
        actionsDiv.appendChild(cancelBtn);
        var saveBtn = document.createElement('button');
        saveBtn.className = 'btn btn-primary';
        saveBtn.textContent = t('common.save');
        saveBtn.addEventListener('click', function() { saveAlias(id); });
        actionsDiv.appendChild(saveBtn);

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) closeAliasModal();
        });
    };

    window.saveAlias = async function(id) {
        var alias = document.getElementById('newAlias').value.trim();
        var msgDiv = document.getElementById('aliasMsg');

        try {
            var resp = await fetch('/api/capes/alias', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ id: id, alias: alias })
            });
            var data = await resp.json();
            showMsg(msgDiv, data.message, data.success);
            if (data.success) {
                setTimeout(function() { closeAliasModal(); closeDetailModal(); loadCapes(); }, 800);
            }
        } catch (err) {
            showMsg(msgDiv, t('common.networkError'), false);
        }
    };

    window.closeAliasModal = function() {
        var modal = document.getElementById('aliasModal');
        if (modal) modal.remove();
    };

    window.downloadCape = function(id) {
        window.open('/api/capes/download?id=' + encodeURIComponent(id), '_blank');
    };

    window.deleteCape = function(id, name) {
        showConfirmDialog(t('texture.deleteConfirm', t('texture.cape'), name), function() {
            fetch('/api/capes/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ id: id })
            }).then(function(resp) { return resp.json(); })
              .then(function(data) {
                  if (data.success) {
                      closeDetailModal();
                      loadCapes();
                  } else {
                      showToast(data.message || t('texture.deleteFailed'), 'error');
                  }
              }).catch(function() {
                  showToast(t('common.networkError'), 'error');
              });
        });
    };

    function showUploadModal(file) {
        var existing = document.getElementById('uploadModal');
        if (existing) existing.remove();

        var reader = new FileReader();
        reader.onload = function(e) {
            var thumbEl = document.getElementById('uploadModalThumb');
            if (thumbEl) thumbEl.src = e.target.result;
        };
        reader.readAsDataURL(file);

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'uploadModal';

        var box = document.createElement('div');
        box.className = 'modal-box';
        box.innerHTML =
            '<button class="modal-close-btn" title="' + t('common.close') + '" id="uploadModalCloseBtn">&times;</button>' +
            '<h3>' + t('texture.uploadCape') + '</h3>' +
            '<div class="upload-modal-body">' +
            '<div class="upload-modal-preview">' +
            '<img class="upload-modal-thumb" id="uploadModalThumb" src="" alt="preview">' +
            '<div class="upload-modal-info">' +
            '<div class="upload-modal-filename">' + escapeHtml(file.name) + '</div>' +
            '<div class="upload-modal-filesize">' + formatSize(file.size) + '</div>' +
            '</div></div>' +
            '<div class="form-group" style="text-align:left">' +
            '<label class="form-label">' + t('texture.aliasOptional') + '</label>' +
            '<input type="text" class="form-input" id="uploadAlias" placeholder="' + t('texture.aliasPlaceholder') + '">' +
            '</div>' +
            '<div id="uploadModalMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="uploadCancelBtn">' + t('common.cancel') + '</button>' +
            '<button class="btn btn-primary" id="uploadConfirmBtn">' + t('texture.upload') + '</button>' +
            '</div></div>';

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        box.querySelector('#uploadModalCloseBtn').addEventListener('click', closeUploadModal);
        box.querySelector('#uploadCancelBtn').addEventListener('click', closeUploadModal);
        box.querySelector('#uploadConfirmBtn').addEventListener('click', function() { submitUpload(file); });
        overlay.addEventListener('click', function(e) { if (e.target === overlay) closeUploadModal(); });
        box.querySelector('#uploadAlias').focus();
    }

    function closeUploadModal() {
        var modal = document.getElementById('uploadModal');
        if (modal) modal.remove();
        fileInput.value = '';
    }

    async function submitUpload(file) {
        var aliasInput = document.getElementById('uploadAlias');
        var msgDiv = document.getElementById('uploadModalMsg');
        var btn = document.getElementById('uploadConfirmBtn');

        var formData = new FormData();
        formData.append('file', file);
        var alias = aliasInput ? aliasInput.value.trim() : '';
        if (alias) formData.append('alias', alias);

        if (btn) { btn.disabled = true; btn.textContent = t('texture.uploading'); }

        try {
            var resp = await fetch('/api/capes/upload', {
                method: 'POST',
                headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: formData
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || t('texture.uploadSuccess'), 'success');
                closeUploadModal();
                loadCapes();
            } else {
                showMsg(msgDiv, data.message, false);
                if (btn) { btn.disabled = false; btn.textContent = t('texture.upload'); }
            }
        } catch (err) {
            showMsg(msgDiv, t('common.networkError'), false);
            if (btn) { btn.disabled = false; btn.textContent = t('texture.upload'); }
        }
    }

    function showMsg(div, msg, success) {
        if (!div) return;
        div.textContent = (success ? '\u2713 ' : '\u2717 ') + msg;
        div.className = 'msg-area ' + (success ? 'success' : 'error');
    }

    window.toggleVisibility = async function(id, isPublic) {
        try {
            var resp = await fetch('/api/textures/visibility', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ textureId: id, isPublic: isPublic })
            });
            var data = await resp.json();
            if (!data.success) {
                showToast(data.message || t('texture.operationFailed'), 'error');
                var cb = document.getElementById('visCheckbox-' + id);
                if (cb) cb.checked = !isPublic;
            }
        } catch (err) {
            showToast(t('common.networkError'), 'error');
            var cb = document.getElementById('visCheckbox-' + id);
            if (cb) cb.checked = !isPublic;
        }
    };

    window.fetchVisibility = async function(id, checkbox) {
        try {
            var resp = await fetch('/api/textures/visibility?id=' + encodeURIComponent(id));
            if (resp.status === 401) return;
            var data = await resp.json();
            if (data.success && checkbox) {
                checkbox.checked = data.isPublic === true;
            }
        } catch (err) {}
    };

    function applyVisibilitySeg(seg, isPublic) {
        if (!seg) return;
        var opts = seg.querySelectorAll('.vis-option');
        if (opts.length >= 2) {
            opts[0].classList.toggle('active', !isPublic);
            opts[1].classList.toggle('active', isPublic);
        }
    }

    function setVisibility(id, isPublic, seg) {
        fetch('/api/textures/visibility', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify({ textureId: id, isPublic: isPublic })
        }).then(function(r) { return r.json(); }).then(function(data) {
            if (data && data.success) {
                applyVisibilitySeg(seg, isPublic);
                showToast(isPublic ? t('texture.publicSuccess') : t('texture.privateSuccess'), 'success');
            } else {
                showToast((data && data.message) || t('texture.operationFailed'), 'error');
            }
        }).catch(function() { showToast(t('common.networkError'), 'error'); });
    }

    function fetchVisibilityInto(id, seg) {
        fetch('/api/textures/visibility?id=' + encodeURIComponent(id))
            .then(function(r) { if (r.status === 401) return null; return r.json(); })
            .then(function(data) { if (data && data.success) applyVisibilitySeg(seg, data.isPublic === true); })
            .catch(function() {});
    }

    window.showToast = showToast;

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
})();
