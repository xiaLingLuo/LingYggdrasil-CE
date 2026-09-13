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
    var DEFAULT_SKIN_URL = '/img/juststeve.png';
    var profilesById = {};
    var PROFILE_ADD_TILE = '<div class="profile-item profile-add-tile card-animate" id="profileAddTile">' +
        '<div class="profile-add-icon"><span>+</span></div>' +
        '<div class="profile-add-text">' + t('user.profiles.add') + '</div>' +
        '<div class="profile-add-hint">' + t('user.profiles.addHint') + '</div>' +
        '</div>';

    async function loadProfiles() {
        var container = document.getElementById('profileList');
        if (!container) return;

        try {
            var resp = await fetch('/api/profiles');
            var data = await resp.json();

            if (data.success && data.profiles && data.profiles.length > 0) {
                var html = '<div class="profile-grid">';
                profilesById = {};
                data.profiles.forEach(function(p) {
                    profilesById[p.id] = p;
                    var skinLabel = p.skinUrl
                        ? '<span style="color:#16a34a">' + escapeHtml(p.skinName || t('texture.skin')) + '</span>'
                        : '<span style="color:#f87171">' + t('user.profiles.notSet') + '</span>';
                    var capeLabel = p.capeUrl
                        ? '<span style="color:#16a34a">' + escapeHtml(p.capeName || t('texture.cape')) + '</span>'
                        : '<span style="color:#f87171">' + t('user.profiles.notSet') + '</span>';
                    var isSlim = p.skinModel === 'slim';
                    var modelBadgeStyle = 'position:absolute;top:12px;right:12px;font-size:11px;padding:2px 8px;border-radius:10px;border:1px solid ' + (isSlim ? '#66CCFF;color:#66CCFF' : '#3B82F6;color:#3B82F6');
                    var modelBadge = '<span style="' + modelBadgeStyle + '">' + (isSlim ? t('user.profiles.modelSlim') : t('user.profiles.modelDefault')) + '</span>';
                    html += '<div class="profile-item card-animate profile-card-clickable" id="profile-' + escapeHtml(p.id) + '"' +
                        ' data-profile-id="' + escAttr(p.id) + '"' +
                        ' data-name="' + escAttr(p.name) + '"' +
                        ' data-model="' + escAttr(p.skinModel) + '"' +
                        ' data-skin="' + escAttr(p.skinUrl || '') + '"' +
                        ' data-cape="' + escAttr(p.capeUrl || '') + '"' +
                        ' data-skin-hash="' + escAttr(p.skinHash || '') + '"' +
                        ' data-token="' + escAttr(p.yggdrasilToken || '') + '">' +
                        modelBadge +
                        '<div style="display:flex;align-items:center;gap:12px">' +
                        '<div style="width:56px;height:56px;border-radius:10px;overflow:hidden;background:#fff0f5;flex-shrink:0"><canvas></canvas></div>' +
                        '<div style="flex:1;min-width:0">' +
                        '<div class="profile-name" style="margin-bottom:2px">' + escapeHtml(p.name) + '</div>' +
                        '<div class="text-muted" style="font-size:12px">' + t('user.profiles.assetPair', [skinLabel, capeLabel]) + '</div>' +
                        '</div></div>' +
                        '<div class="profile-uuid" style="margin-top:10px">' + escapeHtml(p.id) + '</div>' +
                        '</div>';
                });
                html += PROFILE_ADD_TILE;
                html += '</div>';
                container.innerHTML = html;

                container.querySelectorAll('.profile-card-clickable').forEach(function(card) {
                    card.addEventListener('click', function() {
                        editProfile(this.dataset.profileId);
                    });
                    var canvas = card.querySelector('canvas');
                    var skinHash = card.dataset.skinHash;
                    if (skinHash) {
                        drawFace(canvas, '/api/publicTexture/skin/' + skinHash);
                    } else {
                        drawFace(canvas, '/img/juststeve.png');
                    }
                });
            } else {
                container.innerHTML = '<div class="profile-grid">' +
                    '<p class="empty-hint" style="grid-column:1/-1">' + t('user.profiles.empty') + ' \u273F</p>' +
                    PROFILE_ADD_TILE +
                    '</div>';
            }

            var addTile = document.getElementById('profileAddTile');
            if (addTile) addTile.addEventListener('click', showCreateProfileModal);
        } catch (err) {
            container.innerHTML = '<p class="empty-hint" style="color:#C62828">\u52A0\u8F7D\u5931\u8D25\uFF0C\u8BF7\u5237\u65B0\u91CD\u8BD5</p>';
        }
    }

    function showCreateProfileModal() {
        var existing = document.getElementById('createProfileModal');
        if (existing) existing.remove();

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'createProfileModal';
        overlay.innerHTML =
            '<div class="modal-box">' +
            '<h3>' + t('user.profiles.create') + '</h3>' +
            '<div class="form-group">' +
            '<label class="form-label">' + t('user.profiles.name') + '</label>' +
            '<input type="text" class="form-input" id="newProfileName" placeholder="' + t('user.profiles.namePlaceholder') + '" maxlength="16">' +
            '</div>' +
            '<div id="createMsg" class="msg-area"></div>' +
            '<div class="modal-actions" id="createProfileActions"></div>' +
            '</div>';
        document.body.appendChild(overlay);

        var actions = overlay.querySelector('#createProfileActions');
        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = t('common.cancel');
        cancelBtn.addEventListener('click', function() { overlay.remove(); });
        actions.appendChild(cancelBtn);

        var createBtn = document.createElement('button');
        createBtn.type = 'button';
        createBtn.className = 'btn btn-primary';
        createBtn.textContent = t('common.create');
        createBtn.addEventListener('click', submitCreateProfile);
        actions.appendChild(createBtn);

        overlay.addEventListener('click', function(e) { if (e.target === overlay) overlay.remove(); });
        var input = overlay.querySelector('#newProfileName');
        if (input) {
            input.focus();
            input.addEventListener('keydown', function(e) { if (e.key === 'Enter') submitCreateProfile(); });
        }
    }

    async function submitCreateProfile() {
        var input = document.getElementById('newProfileName');
        var msgDiv = document.getElementById('createMsg');
        var name = input ? input.value.trim() : '';

        if (!name) { showMsg(msgDiv, t('user.profiles.nameRequired'), false); return; }
        if (name.length > 16) { showMsg(msgDiv, t('user.profiles.nameTooLong'), false); return; }
        if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showMsg(msgDiv, t('user.profiles.nameInvalid'), false); return; }

        try {
            var resp = await fetch('/api/profiles/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ name: name })
            });
            var data = await resp.json();
            showMsg(msgDiv, data.message, data.success);
            if (data.success) {
                var modal = document.getElementById('createProfileModal');
                if (modal) modal.remove();
                loadProfiles();
            }
        } catch (err) {
            showMsg(msgDiv, '\u7F51\u7EDC\u9519\u8BEF', false);
        }
    }

    window.showCreateProfileModal = showCreateProfileModal;

    loadProfiles();

    window.editProfile = function(id) {
        var p = profilesById[id];
        if (!p) return;

        var currentName = p.name;
        var currentModel = p.skinModel;
        var currentSkinUrl = p.skinUrl;
        var currentCapeUrl = p.capeUrl;
        var currentToken = p.yggdrasilToken;

        var isSlim = currentModel === 'slim';
        var hasSkin = !!currentSkinUrl;
        var hasCape = !!currentCapeUrl;

        function sourceLabel(src) {
            if (!src || src === 'none') return t('user.profiles.sourceNone');
            if (src === 'friend') return t('user.profiles.sourceFriend');
            if (src === 'public') return t('user.profiles.sourceFavorite');
            return t('user.profiles.sourceMine');
        }
        function textureDesc(name, original, hash) {
            if (!hash) return t('user.profiles.notSet');
            return name || original || hash.substring(0, 12);
        }
        var skinAsset = '<div class="profile-asset">' +
            '<div class="profile-asset-head">' +
            '<div class="profile-asset-label">' + t('user.profiles.currentSkin') + '</div>' +
            '<button type="button" class="asset-reset-btn" data-type="SKIN" title="' + t('texture.reset') + '"><i class="fas fa-trash-can"></i></button>' +
            '</div>' +
            '<div class="profile-asset-value">' + escapeHtml(textureDesc(p.skinName, p.skinOriginal, p.skinHash)) + '</div>' +
            '<div class="profile-asset-meta">' + t('user.profiles.source') + ': ' + escapeHtml(sourceLabel(p.skinSource)) +
            (p.skinHash ? ' · ' + escapeHtml(p.skinHash.substring(0, 16)) : '') + '</div></div>';
        var capeAsset = '<div class="profile-asset">' +
            '<div class="profile-asset-head">' +
            '<div class="profile-asset-label">' + t('user.profiles.currentCape') + '</div>' +
            '<button type="button" class="asset-reset-btn" data-type="CAPE" title="' + t('texture.reset') + '"><i class="fas fa-trash-can"></i></button>' +
            '</div>' +
            '<div class="profile-asset-value">' + escapeHtml(textureDesc(p.capeName, p.capeOriginal, p.capeHash)) + '</div>' +
            '<div class="profile-asset-meta">' + t('user.profiles.source') + ': ' + escapeHtml(sourceLabel(p.capeSource)) +
            (p.capeHash ? ' · ' + escapeHtml(p.capeHash.substring(0, 16)) : '') + '</div></div>';

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'editModal';

        var show3d = typeof skinview3d !== 'undefined' && (hasSkin || hasCape);

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
            '<button class="modal-close-btn" title="' + t('common.close') + '" id="editModalCloseBtn">&times;</button>' +
            '<h3>' + t('user.profiles.editTitle') + '</h3>' +
            '<div class="profile-edit-layout">' +
            '<div class="profile-edit-left">' +
            '<div class="form-group">' +
            '<label class="form-label">' + t('user.profiles.name') + '</label>' +
            '<input type="text" class="form-input" id="editName" value="' + escapeHtml(currentName) + '" maxlength="16">' +
            '</div>' +
            '<div class="form-group">' +
            '<label class="form-label">' + t('user.profiles.model') + '</label>' +
            '<div class="model-toggle" id="modelToggle">' +
            '<span class="model-toggle-thumb" id="modelToggleThumb"></span>' +
            '<button type="button" class="model-toggle-btn' + (!isSlim ? ' active' : '') + '" data-model="default">' + t('user.profiles.modelDefault') + '</button>' +
            '<button type="button" class="model-toggle-btn' + (isSlim ? ' active' : '') + '" data-model="slim">' + t('user.profiles.modelSlim') + '</button>' +
            '</div>' +
            '<input type="hidden" id="editModel" value="' + (isSlim ? 'slim' : 'default') + '">' +
            '</div>' +
            '<div class="form-group">' +
            '<label class="form-label">' + t('user.profiles.uuid') + '</label>' +
            '<input type="text" class="form-input" id="editUuid" value="' + escapeHtml(id) + '" readonly>' +
            '</div>' +
            skinAsset +
            capeAsset +
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
        tokenInput.className = 'form-input token-field token-hidden';
        tokenInput.id = 'editToken';
        tokenInput.value = currentToken || '';
        tokenInput.readOnly = true;
        tokenInput.addEventListener('focus', function() { this.select(); });
        tokenInputGroup.appendChild(tokenInput);

        var eyeBtn = document.createElement('button');
        eyeBtn.type = 'button';
        eyeBtn.className = 'token-eye-btn';
        eyeBtn.title = t('user.profiles.toggleToken');
        eyeBtn.innerHTML = '&#128065;';
        var tokenVisible = false;
        eyeBtn.addEventListener('click', function() {
            tokenVisible = !tokenVisible;
            tokenInput.classList.toggle('token-hidden', !tokenVisible);
            eyeBtn.classList.toggle('active', tokenVisible);
        });
        tokenInputGroup.appendChild(eyeBtn);

        var tokenActionGroup = modalBox.querySelector('#tokenActionGroup');

        var copyBtn = document.createElement('button');
        copyBtn.type = 'button';
        copyBtn.className = 'btn btn-secondary';
        copyBtn.style.cssText = 'flex:1;padding:8px 12px';
        copyBtn.textContent = t('user.profiles.copy');
        copyBtn.addEventListener('click', function() { copyToken(); });
        tokenActionGroup.appendChild(copyBtn);

        var regenBtn = document.createElement('button');
        regenBtn.type = 'button';
        regenBtn.className = 'btn btn-secondary';
        regenBtn.style.cssText = 'flex:1;padding:8px 12px';
        regenBtn.textContent = t('user.profiles.regenerate');
        regenBtn.addEventListener('click', function() {
            regenerateToken(id);
        });
        tokenActionGroup.appendChild(regenBtn);

        var actionsDiv = modalBox.querySelector('#editModalActions');

        var deleteBtn = document.createElement('button');
        deleteBtn.className = 'btn btn-danger modal-delete-btn';
        deleteBtn.textContent = t('user.profiles.deleteProfile');
        deleteBtn.addEventListener('click', function() { deleteProfile(id, currentName); });
        actionsDiv.appendChild(deleteBtn);

        var saveArea = modalBox.querySelector('#profileEditSaveArea');
        var saveBtn = document.createElement('button');
        saveBtn.className = 'btn btn-primary profile-edit-save-btn';
        saveBtn.textContent = t('user.profiles.saveChanges');
        saveBtn.addEventListener('click', function() { saveProfile(id); });
        if (saveArea) {
            saveArea.appendChild(saveBtn);
        }

        document.body.appendChild(overlay);

        modalBox.querySelector('#editModalCloseBtn').addEventListener('click', function() { closeModal(); });

        initProfile3dPreview(currentSkinUrl, currentCapeUrl, currentModel);

        var modelToggle = modalBox.querySelector('#modelToggle');
        var modelThumb = modalBox.querySelector('#modelToggleThumb');
        function syncModelThumb() {
            var active = modelToggle ? modelToggle.querySelector('.model-toggle-btn.active') : null;
            if (modelThumb && active) {
                modelThumb.style.transform = (active.dataset.model === 'slim') ? 'translateX(100%)' : 'translateX(0)';
            }
        }
        if (modelToggle) {
            modelToggle.querySelectorAll('.model-toggle-btn').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    modelToggle.querySelectorAll('.model-toggle-btn').forEach(function(b) { b.classList.remove('active'); });
                    btn.classList.add('active');
                    var hidden = modalBox.querySelector('#editModel');
                    if (hidden) hidden.value = btn.dataset.model;
                    syncModelThumb();
                    if (window._profileViewer && window._profileSkinUrl) {
                        window._profileViewer.loadSkin(window._profileSkinUrl,
                            { model: btn.dataset.model === 'slim' ? 'slim' : 'default' });
                    }
                });
            });
            syncModelThumb();
        }

        modalBox.querySelectorAll('.asset-reset-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                resetProfileTexture(id, btn.dataset.type);
            });
        });

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) closeModal();
        });
    };

    function resetProfileTexture(profileId, type) {
        var typeName = type === 'CAPE' ? t('texture.cape') : t('texture.skin');
        showConfirmDialog(t('texture.resetConfirm', typeName), async function() {
            var payload = { id: profileId };
            if (type === 'CAPE') { payload.capeHash = ''; } else { payload.skinHash = ''; }
            var r = await apiPost('/api/profiles/update', payload);
            if (r && r.data && r.data.success) {
                closeModal();
                loadProfiles();
            }
        });
    }

    async function loadTextureOptions(currentSkinHash, currentCapeHash) {
        try {
            var resp = await fetch('/api/textures/my');
            var data = await resp.json();
            if (data.success) {
                var skinSelect = document.getElementById('editSkinHash');
                var capeSelect = document.getElementById('editCapeHash');

                function populateSelect(select, items, favItems, sharedItems) {
                    var mineGroup = document.createElement('optgroup');
                    mineGroup.label = t('user.profiles.sourceMine');
                    (items || []).forEach(function(s) {
                        var option = document.createElement('option');
                        option.value = s.hash;
                        option.dataset.textureId = s.id;
                        option.textContent = s.alias || s.hash;
                        mineGroup.appendChild(option);
                    });
                    if (mineGroup.children.length > 0) select.appendChild(mineGroup);

                    if (favItems && favItems.length > 0) {
                        var favGroup = document.createElement('optgroup');
                        favGroup.label = t('user.profiles.sourceFavorite');
                        favItems.forEach(function(s) {
                            var option = document.createElement('option');
                            option.value = s.hash;
                            option.dataset.textureId = s.id;
                            option.textContent = s.alias;
                            favGroup.appendChild(option);
                        });
                        select.appendChild(favGroup);
                    }

                    if (sharedItems && sharedItems.length > 0) {
                        var sharedGroup = document.createElement('optgroup');
                        sharedGroup.label = t('user.profiles.sourceFriend');
                        sharedItems.forEach(function(s) {
                            var option = document.createElement('option');
                            option.value = s.hash;
                            option.dataset.textureId = s.id;
                            option.textContent = s.alias + ' 🔗';
                            sharedGroup.appendChild(option);
                        });
                        select.appendChild(sharedGroup);
                    }
                }

                if (skinSelect) {
                    populateSelect(skinSelect, data.skins || [], data.favoriteSkins || [], data.sharedSkins || []);
                    for (var i = 0; i < skinSelect.options.length; i++) {
                        if (skinSelect.options[i].value === currentSkinHash) {
                            skinSelect.options[i].selected = true;
                            break;
                        }
                    }
                }
                if (capeSelect) {
                    populateSelect(capeSelect, data.capes || [], data.favoriteCapes || [], data.sharedCapes || []);
                    for (var i = 0; i < capeSelect.options.length; i++) {
                        if (capeSelect.options[i].value === currentCapeHash) {
                            capeSelect.options[i].selected = true;
                            break;
                        }
                    }
                }
            }
        } catch (err) {
            console.error('加载纹理列表失败:', err);
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

    function getTextureUrl(type, hash) {
        if (!hash) return null;
        return '/api/publicTexture/' + type.toLowerCase() + '/' + encodeURIComponent(hash);
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
        var skinDownloadUrl = skinHash ? getTextureUrl('SKIN', skinHash) : null;
        var capeDownloadUrl = capeHash ? getTextureUrl('CAPE', capeHash) : null;

        var effectiveSkin = hasSkin ? skinDownloadUrl : (hasCape ? DEFAULT_SKIN_URL : null);
        if (!effectiveSkin) return;

        window._profileSkinUrl = effectiveSkin;
        window._profileCapeUrl = capeDownloadUrl;

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
        } catch(e) { console.error('3D预览初始化失败:', e); }
    }

    window.saveProfile = async function(id) {
        var name = document.getElementById('editName').value.trim();
        var skinModel = document.getElementById('editModel').value;
        var msgDiv = document.getElementById('editMsg');

        if (!name) { showMsg(msgDiv, t('user.profiles.nameRequired'), false); return; }
        if (name.length > 16) { showMsg(msgDiv, t('user.profiles.nameTooLong'), false); return; }
        if (!/^[a-zA-Z0-9_\u4e00-\u9fa5-]+$/.test(name)) { showMsg(msgDiv, t('user.profiles.nameInvalid'), false); return; }

        try {
            var resp = await fetch('/api/profiles/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ id: id, name: name, skinModel: skinModel })
            });
            var data = await resp.json();
            showMsg(msgDiv, data.message, data.success);
            if (data.success) {
                setTimeout(function() { closeModal(); loadProfiles(); }, 800);
            }
        } catch (err) {
            showMsg(msgDiv, t('common.networkError'), false);
        }
    };

    window.deleteProfile = function(id, name) {
        showConfirmDialog(t('user.profiles.deleteConfirm', name), function() {
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
            if (msg) { showMsg(msg, t('user.profiles.tokenCopied'), true); }
        }).catch(function() {
            document.execCommand('copy');
            var msg = document.getElementById('tokenMsg');
            if (msg) { showMsg(msg, t('user.profiles.tokenCopiedShort'), true); }
        });
    };

    window.regenerateToken = function(id) {
        showConfirmDialog(t('user.profiles.regenerateConfirm'), async function() {
            var tokenInput = document.getElementById('editToken');
            var msg = document.getElementById('tokenMsg');
            if (msg) { showMsg(msg, t('user.profiles.regenerating'), true); }
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
                    showToast(data.message || t('user.profiles.regenerateSuccess'), 'success');
                } else {
                    var errMsg = data.message || t('user.profiles.regenerateFailed');
                    if (msg) { showMsg(msg, errMsg, false); }
                    showToast(errMsg, 'error');
                }
            } catch (err) {
                console.error('重新生成 Token 失败:', err);
                var errMsg = t('common.networkError');
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

    function drawFace(canvas, url) {
        canvas.width = 56; canvas.height = 56;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FFF0F5'; ctx.fillRect(0, 0, 56, 56);
        var img = new Image();
        img.onload = function() {
            ctx.imageSmoothingEnabled = false;
            ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 56, 56);
            if (!(img.width === 64 && img.height === 32)) {
                ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 56, 56);
            }
        };
        img.src = url;
    }
})();
