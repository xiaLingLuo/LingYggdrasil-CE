function showConfirmDialog(message, onConfirm) {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'confirmDialog';
    overlay.style.zIndex = '1100';
    overlay.innerHTML = '<div class="modal-box">' +
        '<h3>确认操作</h3>' +
        '<p>' + (window.escapeHtml ? window.escapeHtml(message) : message) + '</p>' +
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

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (toast) {
        toast.textContent = message;
        toast.className = 'toast ' + (type || '');
        toast.style.display = 'block';
        setTimeout(function() { toast.style.display = 'none'; }, 3000);
    }
}

(function() {
    var DEFAULT_SKIN_URL = '/img/juststeve.png';

    async function loadProfiles() {
        var container = document.getElementById('profileList');
        if (!container) return;

        try {
            var resp = await fetch('/api/profiles');
            var data = await resp.json();

            if (data.success && data.profiles && data.profiles.length > 0) {
                var html = '<div class="profile-grid">';
                data.profiles.forEach(function(p) {
                    var skinLabel = p.skinUrl
                        ? '<span style="color:#16a34a">' + escapeHtml(p.skinName || '皮肤') + '</span>'
                        : '<span style="color:#f87171">未设置</span>';
                    var capeLabel = p.capeUrl
                        ? '<span style="color:#16a34a">' + escapeHtml(p.capeName || '披风') + '</span>'
                        : '<span style="color:#f87171">未设置</span>';
                    var isSlim = p.skinModel === 'slim';
                    var modelBadgeStyle = 'position:absolute;top:12px;right:12px;font-size:11px;padding:2px 8px;border-radius:10px;border:1px solid ' + (isSlim ? '#66CCFF;color:#66CCFF' : '#3B82F6;color:#3B82F6');
                    var modelBadge = '<span style="' + modelBadgeStyle + '">' + (isSlim ? '纤细' : '传统') + '</span>';
                    html += '<div class="profile-item card-animate profile-card-clickable" id="profile-' + escapeHtml(p.id) + '"' +
                        ' data-profile-id="' + escAttr(p.id) + '"' +
                        ' data-name="' + escAttr(p.name) + '"' +
                        ' data-model="' + escAttr(p.skinModel) + '"' +
                        ' data-skin="' + escAttr(p.skinUrl || '') + '"' +
                        ' data-cape="' + escAttr(p.capeUrl || '') + '"' +
                        ' data-token="' + escAttr(p.yggdrasilToken || '') + '">' +
                        modelBadge +
                        '<div class="profile-name"><i class="fas fa-user-astronaut"></i> ' + escapeHtml(p.name) + '</div>' +
                        '<div class="profile-uuid">' + escapeHtml(p.id) + '</div>' +
                        '<div class="text-muted" style="font-size:12px;margin-bottom:8px">' + skinLabel + ' 搭配 ' + capeLabel + '</div>' +
                        '</div>';
                });
                html += '</div>';
                container.innerHTML = html;

                container.querySelectorAll('.profile-card-clickable').forEach(function(card) {
                    card.addEventListener('click', function() {
                        editProfile(
                            this.dataset.profileId,
                            this.dataset.name,
                            this.dataset.model,
                            this.dataset.skin,
                            this.dataset.cape,
                            this.dataset.token
                        );
                    });
                });
            } else {
                container.innerHTML = '<p class="empty-hint">\u6682\u65E0\u89D2\u8272\uFF0C\u4F7F\u7528\u4E0A\u65B9\u8868\u5355\u521B\u5EFA\u4E00\u4E2A\u5427 \u273F</p>';
            }
        } catch (err) {
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u5237\u65B0\u91CD\u8BD5</p>';
        }
    }

    var createForm = document.getElementById('createProfileForm');
    if (createForm) {
        createForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            var name = document.getElementById('newProfileName').value.trim();
            var msgDiv = document.getElementById('createMsg');

            if (!name) { showMsg(msgDiv, '\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0', false); return; }
            if (name.length > 16) { showMsg(msgDiv, '\u89D2\u8272\u540D\u79F0\u6700\u957F16\u4E2A\u5B57\u7B26', false); return; }
            if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showMsg(msgDiv, '\u89D2\u8272\u540D\u79F0\u53EA\u80FD\u5305\u542B\u5B57\u6BCD\u3001\u6570\u5B57\u3001\u4E0B\u5212\u7EBF\u3001\u4E2D\u6587\u548C\u8FDE\u5B57\u7B26', false); return; }

            try {
                var resp = await fetch('/api/profiles/create', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ name: name })
                });
                var data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    document.getElementById('newProfileName').value = '';
                    loadProfiles();
                }
            } catch (err) {
                showMsg(msgDiv, '\u7F51\u7EDC\u9519\u8BEF', false);
            }
        });

        loadProfiles();
    }

    window.editProfile = function(id, currentName, currentModel, currentSkinUrl, currentCapeUrl, currentToken) {
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'editModal';

        var defaultSel = currentModel === 'slim' ? '' : ' selected';
        var slimSel = currentModel === 'slim' ? ' selected' : '';

        var currentSkinHash = '';
        var currentCapeHash = '';
        if (currentSkinUrl) {
            var skinMatch = currentSkinUrl.match(/\/textures\/SKIN\/(.+)$/);
            if (skinMatch) currentSkinHash = skinMatch[1];
        }
        if (currentCapeUrl) {
            var capeMatch = currentCapeUrl.match(/\/textures\/CAPE\/(.+)$/);
            if (capeMatch) currentCapeHash = capeMatch[1];
        }

        var has3d = typeof skinview3d !== 'undefined';
        var hasSkin = !!currentSkinUrl;
        var hasCape = !!currentCapeUrl;
        var show3d = has3d && (hasSkin || hasCape);

        var rightCol = '';
        if (show3d) {
            rightCol =
                '<div class="profile-3d-preview" id="profile3dPreview">' +
                '<canvas id="profile3dCanvas"></canvas>' +
                '</div>';
        }

        var modalBox = document.createElement('div');
        modalBox.className = 'modal-box profile-edit-modal';
        modalBox.innerHTML =
            '<button class="modal-close-btn" title="关闭" id="editModalCloseBtn">&times;</button>' +
            '<h3>编辑角色</h3>' +
            '<div class="profile-edit-layout">' +
            '<div class="profile-edit-left">' +
            '<div class="form-row-group">' +
            '<div class="form-group form-group-flex">' +
            '<label class="form-label">角色名称</label>' +
            '<input type="text" class="form-input" id="editName" value="' + escapeHtml(currentName) + '" maxlength="16">' +
            '</div>' +
            '<div class="form-group form-group-shrink">' +
            '<label class="form-label">形态</label>' +
            '<select class="form-input" id="editModel">' +
            '<option value="default"' + defaultSel + '>传统</option>' +
            '<option value="slim"' + slimSel + '>纤细</option>' +
            '</select></div></div>' +
            '<div class="form-row-group">' +
            '<div class="form-group form-group-flex">' +
            '<label class="form-label">皮肤</label>' +
            '<select class="form-input" id="editSkinHash">' +
            '<option value="">不设置</option>' +
            '</select></div>' +
            '<div class="form-group form-group-flex">' +
            '<label class="form-label">披风</label>' +
            '<select class="form-input" id="editCapeHash">' +
            '<option value="">不设置</option>' +
            '</select></div></div>' +
            '<div class="form-group">' +
            '<label class="form-label">Yggdrasil Token</label>' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:8px" id="tokenInputGroup"></div>' +
            '<div style="display:flex;gap:8px" id="tokenActionGroup"></div>' +
            '<div id="tokenMsg" class="msg-area" style="margin-top:4px"></div>' +
            '</div>' +
            '<div id="editMsg" class="msg-area"></div>' +
            '<div class="modal-actions modal-actions-split" id="editModalActions"></div>' +
            '</div>' +
            '<div class="profile-edit-right">' +
            rightCol +
            '<div class="profile-edit-save-area" id="profileEditSaveArea"></div>' +
            '</div>' +
            '</div>';

        overlay.appendChild(modalBox);

        var tokenInputGroup = modalBox.querySelector('#tokenInputGroup');

        var tokenInput = document.createElement('input');
        tokenInput.type = 'text';
        tokenInput.className = 'form-input';
        tokenInput.id = 'editToken';
        tokenInput.value = currentToken || '';
        tokenInput.readOnly = true;
        tokenInput.style.cssText = 'flex:1;font-family:monospace;font-size:13px;background:#f9f9f9;cursor:text;filter:blur(5px);transition:filter 0.2s;letter-spacing:1px';
        tokenInput.addEventListener('focus', function() { this.select(); });
        tokenInputGroup.appendChild(tokenInput);

        var eyeBtn = document.createElement('button');
        eyeBtn.type = 'button';
        eyeBtn.title = '显示/隐藏 Token';
        eyeBtn.style.cssText = 'background:none;border:none;cursor:pointer;padding:4px 6px;font-size:18px;color:#999;transition:color 0.2s;flex-shrink:0';
        eyeBtn.innerHTML = '&#128065;';
        var tokenVisible = false;
        eyeBtn.addEventListener('click', function() {
            tokenVisible = !tokenVisible;
            tokenInput.style.filter = tokenVisible ? 'none' : 'blur(5px)';
            eyeBtn.style.color = tokenVisible ? '#3B82F6' : '#999';
        });
        tokenInputGroup.appendChild(eyeBtn);

        var tokenActionGroup = modalBox.querySelector('#tokenActionGroup');

        var copyBtn = document.createElement('button');
        copyBtn.type = 'button';
        copyBtn.className = 'btn btn-secondary';
        copyBtn.style.cssText = 'flex:1;padding:8px 12px';
        copyBtn.textContent = '复制';
        copyBtn.addEventListener('click', function() { copyToken(); });
        tokenActionGroup.appendChild(copyBtn);

        var regenBtn = document.createElement('button');
        regenBtn.type = 'button';
        regenBtn.className = 'btn btn-secondary';
        regenBtn.style.cssText = 'flex:1;padding:8px 12px';
        regenBtn.textContent = '重新生成';
        regenBtn.addEventListener('click', function() {
            console.log('重新生成 Token 按钮被点击, profileId:', id);
            regenerateToken(id);
        });
        tokenActionGroup.appendChild(regenBtn);

        var actionsDiv = modalBox.querySelector('#editModalActions');

        var deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger modal-delete-btn';
        deleteBtn.textContent = '删除角色';
        deleteBtn.addEventListener('click', function() { deleteProfile(id, currentName); });
        actionsDiv.appendChild(deleteBtn);

        var saveArea = modalBox.querySelector('#profileEditSaveArea');
        var saveBtn = document.createElement('button');
        saveBtn.className = 'btn btn-primary profile-edit-save-btn';
        saveBtn.textContent = '保存更改';
        saveBtn.addEventListener('click', function() { saveProfile(id); });
        if (saveArea) {
            saveArea.appendChild(saveBtn);
        }

        document.body.appendChild(overlay);

        modalBox.querySelector('#editModalCloseBtn').addEventListener('click', function() { closeModal(); });

        loadTextureOptions(currentSkinHash, currentCapeHash).then(function() {
            initProfile3dPreview(currentSkinUrl, currentCapeUrl, currentModel);
        });

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) closeModal();
        });
    };

    async function loadTextureOptions(currentSkinHash, currentCapeHash) {
        try {
            var resp = await fetch('/api/textures/my');
            var data = await resp.json();
            if (data.success) {
                var skinSelect = document.getElementById('editSkinHash');
                var capeSelect = document.getElementById('editCapeHash');
                if (skinSelect && data.skins) {
                    data.skins.forEach(function(s) {
                        var option = document.createElement('option');
                        option.value = s.hash;
                        option.dataset.textureId = s.id;
                        option.textContent = escapeHtml(s.alias || s.hash);
                        if (s.hash === currentSkinHash) option.selected = true;
                        skinSelect.appendChild(option);
                    });
                }
                if (capeSelect && data.capes) {
                    data.capes.forEach(function(c) {
                        var option = document.createElement('option');
                        option.value = c.hash;
                        option.dataset.textureId = c.id;
                        option.textContent = escapeHtml(c.alias || c.hash);
                        if (c.hash === currentCapeHash) option.selected = true;
                        capeSelect.appendChild(option);
                    });
                }
            }
        } catch (err) {
            console.error('\u52A0\u8F7D\u7EB9\u7406\u5217\u8868\u5931\u8D25:', err);
        }
    }

    function findTextureId(hash, type) {
        var sel = type === 'SKIN' ? '#editSkinHash' : '#editCapeHash';
        var select = document.querySelector(sel);
        if (!select) return null;
        for (var i = 0; i < select.options.length; i++) {
            if (select.options[i].value === hash) {
                return select.options[i].dataset.textureId || null;
            }
        }
        return null;
    }

    function getDownloadUrl(type, id) {
        if (!id) return null;
        return type === 'SKIN'
            ? '/api/skins/download?id=' + encodeURIComponent(id)
            : '/api/capes/download?id=' + encodeURIComponent(id);
    }

    function initProfile3dPreview(skinUrl, capeUrl, model) {
        if (typeof skinview3d === 'undefined') return;
        var canvas = document.getElementById('profile3dCanvas');
        if (!canvas) return;
        var previewDiv = canvas.parentElement;
        var w = previewDiv.clientWidth || 260;
        var h = previewDiv.clientHeight || 260;

        var hasSkin = !!skinUrl;
        var hasCape = !!capeUrl;

        var skinHash = '';
        var capeHash = '';
        if (hasSkin) {
            var m = skinUrl.match(/\/textures\/SKIN\/([a-fA-F0-9]+)$/);
            if (m) skinHash = m[1];
        }
        if (hasCape) {
            var m2 = capeUrl.match(/\/textures\/CAPE\/([a-fA-F0-9]+)$/);
            if (m2) capeHash = m2[1];
        }
        var skinId = skinHash ? findTextureId(skinHash, 'SKIN') : null;
        var capeId = capeHash ? findTextureId(capeHash, 'CAPE') : null;
        var skinDownloadUrl = skinId ? getDownloadUrl('SKIN', skinId) : null;
        var capeDownloadUrl = capeId ? getDownloadUrl('CAPE', capeId) : null;

        var effectiveSkin = hasSkin ? skinDownloadUrl : (hasCape ? DEFAULT_SKIN_URL : null);
        if (!effectiveSkin) return;

        try {
            window._profileViewer = new skinview3d.SkinViewer({
                canvas: canvas,
                width: w,
                height: h,
                skin: effectiveSkin,
                cape: hasCape ? capeDownloadUrl : undefined,
                model: model === 'slim' ? 'slim' : 'default'
            });
            window._profileViewer.autoRotate = true;
            window._profileViewer.animation = new skinview3d.WalkingAnimation();

            var skinSelect = document.getElementById('editSkinHash');
            var capeSelect = document.getElementById('editCapeHash');
            var modelSelect = document.getElementById('editModel');

            if (skinSelect) {
                skinSelect.addEventListener('change', function() {
                    var hash = this.value;
                    var capeHash = document.getElementById('editCapeHash').value;
                    if (!hash && !capeHash) return;
                    var url;
                    if (hash) {
                        var id = findTextureId(hash, 'SKIN');
                        url = id ? getDownloadUrl('SKIN', id) : null;
                    } else {
                        url = DEFAULT_SKIN_URL;
                    }
                    if (url && window._profileViewer) {
                        var isSlim = document.getElementById('editModel').value === 'slim';
                        window._profileViewer.loadSkin(url, { model: isSlim ? 'slim' : 'default' });
                    }
                });
            }
            if (capeSelect) {
                capeSelect.addEventListener('change', function() {
                    var hash = this.value;
                    if (window._profileViewer) {
                        if (hash) {
                            var id = findTextureId(hash, 'CAPE');
                            window._profileViewer.loadCape(id ? getDownloadUrl('CAPE', id) : null);
                        } else {
                            window._profileViewer.loadCape(null);
                        }
                    }
                });
            }
            if (modelSelect) {
                modelSelect.addEventListener('change', function() {
                    var isSlim = this.value === 'slim';
                    var skinHash = document.getElementById('editSkinHash').value;
                    var capeHash = document.getElementById('editCapeHash').value;
                    var url;
                    if (skinHash) {
                        var id = findTextureId(skinHash, 'SKIN');
                        url = id ? getDownloadUrl('SKIN', id) : null;
                    } else if (capeHash) {
                        url = DEFAULT_SKIN_URL;
                    }
                    if (url && window._profileViewer) {
                        window._profileViewer.loadSkin(url, { model: isSlim ? 'slim' : 'default' });
                    }
                });
            }
        } catch(e) { console.error('3D预览初始化失败:', e); }
    }

    window.saveProfile = async function(id) {
        var name = document.getElementById('editName').value.trim();
        var skinModel = document.getElementById('editModel').value;
        var skinHash = document.getElementById('editSkinHash').value;
        var capeHash = document.getElementById('editCapeHash').value;
        var msgDiv = document.getElementById('editMsg');

        if (!name) { showMsg(msgDiv, '\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A', false); return; }
        if (name.length > 16) { showMsg(msgDiv, '\u89D2\u8272\u540D\u79F0\u6700\u957F16\u4E2A\u5B57\u7B26', false); return; }
        if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showMsg(msgDiv, '\u89D2\u8272\u540D\u79F0\u53EA\u80FD\u5305\u542B\u5B57\u6BCD\u3001\u6570\u5B57\u3001\u4E0B\u5212\u7EBF\u3001\u4E2D\u6587\u548C\u8FDE\u5B57\u7B26', false); return; }

        try {
            var resp = await fetch('/api/profiles/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ id: id, name: name, skinModel: skinModel, skinHash: skinHash, capeHash: capeHash })
            });
            var data = await resp.json();
            showMsg(msgDiv, data.message, data.success);
            if (data.success) {
                setTimeout(function() { closeModal(); loadProfiles(); }, 800);
            }
        } catch (err) {
            showMsg(msgDiv, '\u7F51\u7EDC\u9519\u8BEF', false);
        }
    };

    window.deleteProfile = function(id, name) {
        showConfirmDialog('\u786E\u5B9A\u8981\u5220\u9664\u89D2\u8272 "' + name + '" \u5417\uFF1F\u6B64\u64CD\u4F5C\u4E0D\u53EF\u6062\u590D\u3002', function() {
            fetch('/api/profiles/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ id: id })
            }).then(function(resp) { return resp.json(); })
              .then(function(data) {
                  if (data.success) {
                      loadProfiles();
                  } else {
                      showToast(data.message || '\u5220\u9664\u5931\u8D25', 'error');
                  }
              }).catch(function() {
                  showToast('\u7F51\u7EDC\u9519\u8BEF', 'error');
              });
        });
    };

    window.closeModal = function() {
        if (window._profileViewer) {
            window._profileViewer.dispose();
            window._profileViewer = null;
        }
        var modal = document.getElementById('editModal');
        if (modal) modal.remove();
    };

    window.copyToken = function() {
        var tokenInput = document.getElementById('editToken');
        if (!tokenInput) return;
        tokenInput.select();
        navigator.clipboard.writeText(tokenInput.value).then(function() {
            var msg = document.getElementById('tokenMsg');
            if (msg) { showMsg(msg, 'Token 已复制到剪贴板', true); }
        }).catch(function() {
            document.execCommand('copy');
            var msg = document.getElementById('tokenMsg');
            if (msg) { showMsg(msg, 'Token 已复制', true); }
        });
    };

    window.regenerateToken = function(id) {
        showConfirmDialog('确定要重新生成 Token 吗？旧 Token 将立即失效，使用旧 Token 的客户端需要更新配置。', async function() {
            var tokenInput = document.getElementById('editToken');
            var msg = document.getElementById('tokenMsg');
            if (msg) { showMsg(msg, '正在重新生成 Token...', true); }
            try {
                var resp = await fetch('/api/profiles/regenerate-token', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ id: id })
                });
                var data = await resp.json();
                if (data.success && data.token) {
                    if (tokenInput) tokenInput.value = data.token;
                    if (msg) { showMsg(msg, data.message, true); }
                    showToast(data.message || 'Token 已重新生成', 'success');
                } else {
                    var errMsg = data.message || '重新生成失败';
                    if (msg) { showMsg(msg, errMsg, false); }
                    showToast(errMsg, 'error');
                }
            } catch (err) {
                console.error('重新生成 Token 失败:', err);
                var errMsg = '网络错误，请稍后重试';
                if (msg) { showMsg(msg, errMsg, false); }
                showToast(errMsg, 'error');
            }
        });
    };

    function showMsg(div, msg, success) {
        if (!div) return;
        div.textContent = (success ? '\u2713 ' : '\u2717 ') + msg;
        div.className = 'msg-area ' + (success ? 'success' : 'error');
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
