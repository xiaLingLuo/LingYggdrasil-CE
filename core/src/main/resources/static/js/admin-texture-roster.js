(function () {
    var root = document.getElementById('textureRosterRoot');
    var TYPE = root ? (root.getAttribute('data-type') || 'SKIN') : 'SKIN';
    var state = { page: 1, pageSize: 100, total: 0, search: '', loading: false };
    var inflight = {};
    var previewViewer = null;

    function $(id) { return document.getElementById(id); }
    function headers() { return { 'Content-Type': 'application/json', 'X-CSRF-Token': window.CSRF_TOKEN || '' }; }

    function request(url, options) {
        return fetch(url, options).then(function (res) {
            return res.json().then(function (body) {
                if (!res.ok || body.success === false) {
                    var error = new Error(body.message || ('HTTP ' + res.status));
                    error.status = res.status;
                    error.body = body;
                    throw error;
                }
                return body;
            });
        });
    }

    function message(text, ok) {
        if (text && typeof showToast === 'function') {
            showToast(text, ok === false ? 'error' : 'success');
        }
    }

    function getAsset(id) {
        return fetch('/admin/api/texture-assets/' + encodeURIComponent(id), { headers: headers() })
            .then(function (res) {
                return res.json().then(function (body) {
                    if (res.status === 404) return null;
                    if (!res.ok || body.success === false) throw new Error(body.message || ('HTTP ' + res.status));
                    return body.item || null;
                });
            });
    }

    function load() {
        if (state.loading) return;
        state.loading = true;
        var url = '/admin/api/texture-assets?type=' + encodeURIComponent(TYPE)
            + '&limit=' + state.pageSize
            + '&offset=' + ((state.page - 1) * state.pageSize)
            + (state.search ? '&search=' + encodeURIComponent(state.search) : '');
        request(url, { headers: headers() }).then(function (data) {
            state.total = Number(data.total) || 0;
            render(data.items || []);
            renderAdminPagination('textureRosterPagination', state.total, state.page, state.pageSize,
                function (page) { state.page = page; load(); },
                function (size) { state.pageSize = size; state.page = 1; load(); });
        }).catch(function (error) {
            message('' + t('common.loadFailedShort') + '：' + error.message, false);
        }).finally(function () { state.loading = false; });
    }

    function render(items) {
        var body = $('textureRosterBody');
        if (!body) return;
        if (!items.length) {
            body.innerHTML = '<tr><td colspan="' + (TYPE === 'SKIN' ? 8 : 7)
                + '" class="text-center">' + t('common.noData') + '</td></tr>';
            return;
        }
        body.innerHTML = items.map(function (item) {
            var name = item.adminFileName || item.displayName || '-';
            var author = item.authorDisplayName || item.author || '' + t('common.anonymous') + '';
            var slim = item.skinModel === 'SLIM';
            var modelCell = TYPE === 'SKIN'
                ? '<td><button class="verified-badge" type="button" title="' + t('admin.textureRoster.toggleModel') + '" data-skin-model="'
                    + item.id + '" data-model="' + (slim ? 'SLIM' : 'DEFAULT') + '">'
                    + (slim ? '' + t('texture.slimModel') + '' : '' + t('texture.defaultModel') + '') + '</button></td>'
                : '';
            return '<tr>'
                + '<td style="font-family:Consolas,monospace;font-size:12px">' + esc((item.hash || '').substring(0, 6))
                + ' <button class="pencil-btn" type="button" title="HASH" data-act="hash" data-id="' + item.id
                + '" data-hash="' + esc(item.hash || '') + '"><i class="fas fa-magnifying-glass"></i></button></td>'
                + '<td>' + esc(name) + ' <button class="pencil-btn" type="button" title="' + t('admin.textureRoster.editName') + '" data-act="name" data-id="'
                + item.id + '" data-value="' + esc(name) + '"><i class="fas fa-pencil"></i></button></td>'
                + '<td>' + esc(author) + ' <button class="pencil-btn" type="button" title="' + t('admin.textureRoster.editAuthor') + '" data-act="author" data-id="'
                + item.id + '" data-value="' + esc(item.authorName || '') + '"><i class="fas fa-pencil"></i></button></td>'
                + modelCell
                + '<td>' + esc(formatSize(item.size)) + '</td>'
                + '<td>' + esc(item.referenceCount == null ? 0 : item.referenceCount) + '</td>'
                + '<td>' + esc(item.createdAt ? formatDate(item.createdAt) : '-') + '</td>'
                + '<td><div class="action-btns">'
                + '<button class="btn-action btn-edit" type="button" data-act="preview" data-id="' + item.id
                + '" data-name="' + esc(name) + '" data-model="' + esc(item.skinModel || 'DEFAULT') + '">' + t('common.preview') + '</button>'
                + '<a class="btn-action btn-edit" href="/admin/api/texture-assets/' + item.id + '/file" download>' + t('texture.download') + '</a>'
                + '<button class="btn-action btn-delete" type="button" title="' + t('admin.textureRoster.deleteHint') + '" data-act="delete" data-id="' + item.id
                + '" data-name="' + esc(name) + '">' + t('common.delete') + '</button>'
                + '</div></td></tr>';
        }).join('');
    }

    function editName(id, current) {
        showPromptDialog({ title: '' + t('admin.textureRoster.editName') + '', value: current, maxlength: 128 }, function (value) {
            return request('/admin/api/texture-assets/' + id + '/name', {
                method: 'POST', headers: headers(), body: JSON.stringify({ name: value })
            }).then(function () { message('' + t('admin.textureRoster.nameUpdated') + '', true); load(); return true; })
              .catch(function (error) {
                  return getAsset(id).then(function (item) {
                      var actual = item ? (item.adminFileName || item.name || '') : null;
                      if (actual !== null && String(actual) === String(value || '').trim()) {
                          message('' + t('admin.textureRoster.nameUpdated') + '', true); load(); return true;
                      }
                      message('' + t('common.updateFailedDetail') + '' + error.message, false); return false;
                  }).catch(function () { message('' + t('common.updateFailedDetail') + '' + error.message, false); return false; });
              });
        });
    }

    function editAuthor(id, current) {
        showPromptDialog({ title: '' + t('admin.textureRoster.editAuthor') + '', value: current, maxlength: 128 }, function (value) {
            return request('/admin/api/texture-assets/' + id + '/author', {
                method: 'POST', headers: headers(), body: JSON.stringify({ author: value })
            }).then(function () { message('' + t('admin.textureRoster.authorUpdated') + '', true); load(); return true; })
              .catch(function (error) {
                  return getAsset(id).then(function (item) {
                      var actual = item && item.authorName ? item.authorName : '';
                      if (String(actual) === String(value || '').trim()) {
                          message('' + t('admin.textureRoster.authorUpdated') + '', true); load(); return true;
                      }
                      message('' + t('common.updateFailedDetail') + '' + error.message, false); return false;
                  }).catch(function () { message('' + t('common.updateFailedDetail') + '' + error.message, false); return false; });
              });
        });
    }

    function preview(id, name, model) {
        disposePreview();
        var stage = document.createElement('div');
        stage.className = 'texture-preview-stage';
        var canvas = document.createElement('canvas');
        stage.appendChild(canvas);
        var dialog = openUiDialog({
            id: 'textureAssetPreview', title: name || '' + t('common.preview') + '', body: stage, wide: true,
            onClose: disposePreview
        });
        if (typeof skinview3d !== 'undefined') {
            try {
                var config = {
                    canvas: canvas,
                    width: stage.clientWidth || 340,
                    height: 340,
                    model: model === 'SLIM' ? 'slim' : 'default'
                };
                var url = '/admin/api/texture-assets/' + id + '/file';
                if (TYPE === 'CAPE') config.cape = url;
                else config.skin = url;
                previewViewer = new skinview3d.SkinViewer(config);
                previewViewer.autoRotate = true;
                previewViewer.animation = new skinview3d.WalkingAnimation();
                return;
            } catch (error) {
                previewViewer = null;
            }
        }
        var img = document.createElement('img');
        img.src = '/admin/api/texture-assets/' + id + '/file';
        img.style.maxWidth = '100%';
        stage.innerHTML = '';
        stage.appendChild(img);
    }

    function disposePreview() {
        if (previewViewer && typeof previewViewer.dispose === 'function') previewViewer.dispose();
        previewViewer = null;
    }

    function deleteAsset(id, name) {
        showConfirmDialog(t('admin.textureRoster.deleteConfirm').replace('{0}', name || id), function () {
            if (inflight['delete:' + id]) return;
            inflight['delete:' + id] = true;
            request('/admin/api/texture-assets/' + id + '/delete', {
                method: 'POST', headers: headers(), body: '{}'
            }).then(function () {
                message('' + t('common.deleted') + '', true);
                load();
            }).catch(function (error) {
                getAsset(id).then(function (item) {
                    if (!item) {
                        message('' + t('common.deleted') + '', true);
                        load();
                    } else {
                        message('' + t('common.deleteFailedDetail') + '' + error.message, false);
                    }
                }).catch(function () {
                    message('' + t('common.deleteFailedDetail') + '' + error.message, false);
                });
            }).finally(function () {
                delete inflight['delete:' + id];
            });
        }, { confirmText: '' + t('common.delete') + '' });
    }

    function applyModelButton(button, value) {
        button.setAttribute('data-model', value);
        button.classList.toggle('verified-yes', value === 'SLIM');
        button.classList.toggle('verified-no', value !== 'SLIM');
        button.textContent = value === 'SLIM' ? '' + t('texture.slimModel') + '' : '' + t('texture.defaultModel') + '';
    }

    function updateModel(button, id, value) {
        var key = 'model:' + id;
        if (inflight[key]) return;
        inflight[key] = true;
        button.disabled = true;
        request('/admin/api/texture-assets/' + id + '/skin-model', {
            method: 'POST', headers: headers(), body: JSON.stringify({ model: value })
        }).then(function () {
            applyModelButton(button, value);
            message('' + t('admin.textureRoster.modelUpdated') + '', true);
        }).catch(function (error) {
            getAsset(id).then(function (item) {
                if (item && item.skinModel === value) {
                    applyModelButton(button, value);
                    message('' + t('admin.textureRoster.modelUpdated') + '', true);
                    return;
                }
                message('' + t('admin.textureRoster.modelFailed') + '' + error.message, false);
            }).catch(function () {
                message('' + t('admin.textureRoster.modelFailed') + '' + error.message, false);
            });
        }).finally(function () {
            button.disabled = false;
            delete inflight[key];
        });
    }

    document.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-act]');
        if (!button) return;
        var act = button.getAttribute('data-act');
        var id = button.getAttribute('data-id');
        if (act === 'hash') {
            var hash = button.getAttribute('data-hash') || '';
            var detail = hash.length > 32 ? hash.slice(0, 32) + '\n' + hash.slice(32) : hash;
            showAlertDialog({ title: 'HASH', detail: detail });
        } else if (act === 'name') {
            editName(id, button.getAttribute('data-value') || '');
        } else if (act === 'author') {
            editAuthor(id, button.getAttribute('data-value') || '');
        } else if (act === 'preview') {
            preview(id, button.getAttribute('data-name') || '', button.getAttribute('data-model') || 'DEFAULT');
        } else if (act === 'delete') {
            deleteAsset(id, button.getAttribute('data-name') || '');
        }
    });
    document.addEventListener('click', function (event) {
        var toggle = event.target.closest('button[data-skin-model]');
        if (!toggle) return;
        var id = toggle.getAttribute('data-skin-model');
        var value = toggle.getAttribute('data-model') === 'SLIM' ? 'DEFAULT' : 'SLIM';
        updateModel(toggle, id, value);
    });
    var search = $('textureRosterSearch');
    if (search) {
        var timer = null;
        search.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(function () { state.search = search.value.trim(); state.page = 1; load(); }, 250);
        });
    }
    document.addEventListener('click', function (event) {
        if (event.target.getAttribute && event.target.getAttribute('data-action') === 'textureRosterSearch') {
            var input = $('textureRosterSearch');
            state.search = input ? input.value.trim() : '';
            state.page = 1;
            load();
        }
    });
    load();
})();
