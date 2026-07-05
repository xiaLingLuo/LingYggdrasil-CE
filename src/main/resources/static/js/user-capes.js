function showConfirmDialog(message, onConfirm) {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'confirmDialog';
    overlay.style.zIndex = '1100';
    overlay.innerHTML = '<div class="modal-box">' +
        '<h3>\u786E\u8BA4\u64CD\u4F5C</h3>' +
        '<p>' + (window.escapeHtml ? window.escapeHtml(message) : message) + '</p>' +
        '<div class="modal-actions" id="confirmDialogActions"></div></div>';
    document.body.appendChild(overlay);
    window._confirmCallback = onConfirm;
    var actionsDiv = overlay.querySelector('#confirmDialogActions');
    var cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.textContent = '\u53D6\u6D88';
    cancelBtn.addEventListener('click', function() { closeConfirmDialog(false); });
    actionsDiv.appendChild(cancelBtn);
    var confirmBtn = document.createElement('button');
    confirmBtn.className = 'btn btn-danger';
    confirmBtn.textContent = '\u786E\u8BA4';
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

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (toast) {
        toast.textContent = message;
        toast.className = 'toast toast-' + (type || 'info');
        toast.style.display = 'block';
        setTimeout(function() { toast.style.display = 'none'; }, 3000);
    }
}

(function() {
    async function loadCapes() {
        var container = document.getElementById('capeList');
        if (!container) return;

        try {
            var resp = await fetch('/api/capes');
            if (resp.status === 401) { window.location.href = '/login'; return; }
            var data = await resp.json();

            if (data.success && data.capes && data.capes.length > 0) {
                var html = '<div class="upload-tile card-animate" id="capeUploadTile">' +
                    '<div class="upload-tile-icon">' +
                    '<svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>' +
                    '</div>' +
                    '<div class="upload-tile-text">上传披风</div>' +
                    '<div class="upload-tile-hint">PNG 格式</div>' +
                    '</div>';
                data.capes.forEach(function(c) {
                    var displayName = c.alias || c.originalName || c.hash;
                    var previewUrl = '/api/capes/download?id=' + encodeURIComponent(c.id);
                    html += '<div class="texture-item card-animate texture-card-clickable" id="cape-' + escapeHtml(c.id) + '"' +
                        ' data-id="' + escAttr(c.id) + '"' +
                        ' data-alias="' + escAttr(c.alias || '') + '"' +
                        ' data-original-name="' + escAttr(c.originalName || '') + '"' +
                        ' data-hash="' + escAttr(c.hash) + '"' +
                        ' data-size="' + escAttr(String(c.size)) + '">' +
                        '<div class="texture-thumb"><img src="' + previewUrl + '" alt="cape preview"></div>' +
                        '<div class="texture-name">\u273F ' + escapeHtml(displayName) + '</div>' +
                        '<div class="texture-meta">' + formatSize(c.size) + '</div>' +
                        '</div>';
                });
                html += '';
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
            } else {
                container.innerHTML = '<div class="upload-tile card-animate" id="capeUploadTile">' +
                    '<div class="upload-tile-icon">' +
                    '<svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>' +
                    '</div>' +
                    '<div class="upload-tile-text">上传披风</div>' +
                    '<div class="upload-tile-hint">PNG 格式</div>' +
                    '</div>' +
                    '<p class="empty-hint">暂无披风，快上传一个吧~ <i class="fas fa-leaf"></i></p>';
            }
        } catch (err) {
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">加载失败，请刷新重试</p>';
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
                } else { showToast('仅支持 PNG 格式的图片', 'error'); }
            }
        });
    }
    
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files && this.files[0]) {
                if (this.files[0].type !== 'image/png') { showToast('仅支持 PNG 格式的图片', 'error'); return; }
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
            '<button class="modal-close-btn" title="关闭" id="detailCloseBtn">&times;</button>' +
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
            } catch(e) { /* 无3D的情况，不显示直接 */ }
        }

        var actionsDiv = modalBox.querySelector('#detailActions');

        var dlBtn = document.createElement('button');
        dlBtn.className = 'btn btn-secondary';
        dlBtn.textContent = '\u4E0B\u8F7D';
        dlBtn.addEventListener('click', function() { downloadCape(id); });
        actionsDiv.appendChild(dlBtn);

        var aliasBtn = document.createElement('button');
        aliasBtn.className = 'btn btn-secondary';
        aliasBtn.textContent = '\u4FEE\u6539\u522B\u540D';
        aliasBtn.addEventListener('click', function() { editAlias(id, alias); });
        actionsDiv.appendChild(aliasBtn);

        var delBtn = document.createElement('button');
        delBtn.className = 'btn btn-danger';
        delBtn.textContent = '\u5220\u9664';
        delBtn.addEventListener('click', function() { deleteCape(id, displayName); });
        actionsDiv.appendChild(delBtn);

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
            '<h3>\u4FEE\u6539\u522B\u540D</h3>' +
            '<div class="form-group">' +
            '<label class="form-label">\u65B0\u522B\u540D</label>' +
            '<input type="text" class="form-input" id="newAlias" value="' + escapeHtml(currentAlias) + '">' +
            '</div>' +
            '<div id="aliasMsg" class="msg-area"></div>' +
            '<div class="modal-actions" id="aliasModalActions"></div></div>';

        document.body.appendChild(overlay);

        var actionsDiv = overlay.querySelector('#aliasModalActions');
        var cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = '\u53D6\u6D88';
        cancelBtn.addEventListener('click', function() { closeAliasModal(); });
        actionsDiv.appendChild(cancelBtn);
        var saveBtn = document.createElement('button');
        saveBtn.className = 'btn btn-primary';
        saveBtn.textContent = '\u4FDD\u5B58';
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
            showMsg(msgDiv, '\u7F51\u7EDC\u9519\u8BEF', false);
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
        showConfirmDialog('\u786E\u5B9A\u8981\u5220\u9664\u62AB\u98CE "' + name + '" \u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u6062\u590D\u3002', function() {
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
                      showToast(data.message || '\u5220\u9664\u5931\u8D25', 'error');
                  }
              }).catch(function() {
                  showToast('\u7F51\u7EDC\u9519\u8BEF', 'error');
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
            '<button class="modal-close-btn" title="关闭" id="uploadModalCloseBtn">&times;</button>' +
            '<h3>上传披风</h3>' +
            '<div class="upload-modal-body">' +
            '<div class="upload-modal-preview">' +
            '<img class="upload-modal-thumb" id="uploadModalThumb" src="" alt="preview">' +
            '<div class="upload-modal-info">' +
            '<div class="upload-modal-filename">' + escapeHtml(file.name) + '</div>' +
            '<div class="upload-modal-filesize">' + formatSize(file.size) + '</div>' +
            '</div></div>' +
            '<div class="form-group" style="text-align:left">' +
            '<label class="form-label">别名 (可选)</label>' +
            '<input type="text" class="form-input" id="uploadAlias" placeholder="给披风起个名字吧">' +
            '</div>' +
            '<div id="uploadModalMsg" class="msg-area"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary" id="uploadCancelBtn">取消</button>' +
            '<button class="btn btn-primary" id="uploadConfirmBtn">上传</button>' +
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

        if (btn) { btn.disabled = true; btn.textContent = '上传中...'; }

        try {
            var resp = await fetch('/api/capes/upload', {
                method: 'POST',
                headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: formData
            });
            var data = await resp.json();
            if (data.success) {
                showToast(data.message || '上传成功', 'success');
                closeUploadModal();
                loadCapes();
            } else {
                showMsg(msgDiv, data.message, false);
                if (btn) { btn.disabled = false; btn.textContent = '上传'; }
            }
        } catch (err) {
            showMsg(msgDiv, '网络错误', false);
            if (btn) { btn.disabled = false; btn.textContent = '上传'; }
        }
    }

    function showMsg(div, msg, success) {
        if (!div) return;
        div.textContent = (success ? '\u2713 ' : '\u2717 ') + msg;
        div.className = 'msg-area ' + (success ? 'success' : 'error');
    }

    function formatSize(bytes) {
        if (bytes === null || bytes === undefined) return '-';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KiB';
        return (bytes / (1024 * 1024)).toFixed(2) + ' MiB';
    }

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

    window.escapeHtml = escapeHtml;
    window.escAttr = escAttr;
})();
