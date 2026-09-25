/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

(function (global) {
    'use strict';

    function escapeHtml(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function escAttr(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/\\/g, '\\\\')
            .replace(/'/g, "\\'")
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function ensureToast() {
        var toast = document.getElementById('toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'toast';
            toast.className = 'toast';
            toast.style.display = 'none';
            document.body.appendChild(toast);
        }
        toast.setAttribute('role', 'status');
        toast.setAttribute('aria-live', 'polite');
        return toast;
    }

    var toastTimer = null;
    var toastHideTimer = null;
    function showToast(message, type) {
        var toast = ensureToast();
        toast.textContent = message === null || message === undefined ? '' : String(message);
        toast.className = 'toast toast-' + (type || 'info');
        toast.style.display = 'block';
        if (toastTimer) clearTimeout(toastTimer);
        if (toastHideTimer) clearTimeout(toastHideTimer);
        toastTimer = setTimeout(function () {
            toast.classList.add('toast-hide');
            toastHideTimer = setTimeout(function () {
                toast.style.display = 'none';
                toast.classList.remove('toast-hide');
            }, 300);
        }, 3000);
    }

    var previousFocus = null;
    var activeConfirm = null;

    function closeConfirmDialog(confirmed) {
        var overlay = document.getElementById('confirmDialog');
        if (!overlay) return;
        overlay.remove();
        document.removeEventListener('keydown', onConfirmKeydown);
        if (previousFocus && typeof previousFocus.focus === 'function') {
            previousFocus.focus();
        }
        previousFocus = null;
        var callback = activeConfirm;
        activeConfirm = null;
        if (confirmed && typeof callback === 'function') {
            callback();
        }
    }

    function onConfirmKeydown(event) {
        if (event.key === 'Escape') {
            closeConfirmDialog(false);
        }
    }

    function showConfirmDialog(message, onConfirm) {
        var existing = document.getElementById('confirmDialog');
        if (existing) existing.remove();

        previousFocus = document.activeElement;
        activeConfirm = onConfirm;

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'confirmDialog';
        overlay.style.zIndex = '1100';
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');

        var box = document.createElement('div');
        box.className = 'modal-box';

        var title = document.createElement('h3');
        title.textContent = t('common.confirmAction');

        var text = document.createElement('p');
        text.textContent = message === null || message === undefined ? '' : String(message);

        var actions = document.createElement('div');
        actions.className = 'modal-actions';
        actions.id = 'confirmDialogActions';

        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = t('common.cancel');
        cancelBtn.addEventListener('click', function () { closeConfirmDialog(false); });

        var confirmBtn = document.createElement('button');
        confirmBtn.type = 'button';
        confirmBtn.className = 'btn btn-danger';
        confirmBtn.textContent = t('common.confirm');
        confirmBtn.addEventListener('click', function () { closeConfirmDialog(true); });

        actions.appendChild(cancelBtn);
        actions.appendChild(confirmBtn);
        box.appendChild(title);
        box.appendChild(text);
        box.appendChild(actions);
        overlay.appendChild(box);
        document.body.appendChild(overlay);

        overlay.addEventListener('click', function (event) {
            if (event.target === overlay) closeConfirmDialog(false);
        });
        document.addEventListener('keydown', onConfirmKeydown);
        confirmBtn.focus();
    }

    function formatSize(bytes) {
        if (bytes === null || bytes === undefined || isNaN(bytes)) return '-';
        bytes = Number(bytes);
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KiB';
        return (bytes / (1024 * 1024)).toFixed(2) + ' MiB';
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            var d = new Date(dateStr);
            if (isNaN(d.getTime())) return dateStr;
            var locale = global.__LOCALE__ || 'zh-CN';
            return d.toLocaleDateString(locale) + ' ' +
                d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            return dateStr;
        }
    }

    function isFormData(body) {
        return typeof FormData !== 'undefined' && body instanceof FormData;
    }

    function redirectToLogin() {
        var path = global.location.pathname || '';
        global.location.href = path.indexOf('/admin') === 0 ? '/admin/login' : '/login';
    }

    async function apiFetch(url, options) {
        options = options || {};
        var opts = {
            method: options.method || 'GET',
            headers: Object.assign({}, options.headers),
            credentials: options.credentials || 'same-origin'
        };
        if (options.body !== undefined && options.body !== null) {
            if (typeof options.body === 'string' || isFormData(options.body)) {
                opts.body = options.body;
            } else {
                opts.headers['Content-Type'] = opts.headers['Content-Type'] || 'application/json';
                opts.body = JSON.stringify(options.body);
            }
        }
        if (opts.method.toUpperCase() !== 'GET') {
            opts.headers['X-CSRF-Token'] = opts.headers['X-CSRF-Token'] || (global.CSRF_TOKEN || '');
        }

        var response;
        try {
            response = await fetch(url, opts);
        } catch (e) {
            return { ok: false, status: 0, data: null, response: null, networkError: true };
        }

        var data = null;
        var contentType = response.headers.get('content-type') || '';
        if (contentType.indexOf('application/json') !== -1) {
            try { data = await response.json(); } catch (e) { data = null; }
        }

        return { ok: response.ok, status: response.status, data: data, response: response, networkError: false };
    }

    async function apiGet(url, options) {
        return apiFetch(url, Object.assign({ method: 'GET' }, options || {}));
    }

    async function apiPost(url, body, options) {
        return apiFetch(url, Object.assign({ method: 'POST', body: body }, options || {}));
    }

    function showLoading(container, text) {
        if (!container) return;
        container.innerHTML = '<div class="world-loading"><span class="spinner"></span> ' +
            escapeHtml(text || t('common.loading')) + '</div>';
    }

    function showError(container, message, onRetry) {
        if (!container) return;
        container.innerHTML = '';
        var box = document.createElement('div');
        box.className = 'empty-hint';
        box.style.color = '#C62828';
        box.textContent = message || t('common.loadFailed');
        container.appendChild(box);
        if (typeof onRetry === 'function') {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-secondary';
            btn.style.marginTop = '12px';
            btn.textContent = t('common.retry');
            btn.addEventListener('click', onRetry);
            container.appendChild(btn);
        }
    }

    function drawFaceCanvas(canvas, url) {
        canvas.width = 64;
        canvas.height = 64;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FFF0F5';
        ctx.fillRect(0, 0, 64, 64);
        var img = new Image();
        img.onload = function () {
            ctx.imageSmoothingEnabled = false;
            ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 64, 64);
            if (!(img.width === 64 && img.height === 32)) {
                ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 64, 64);
            }
        };
        img.src = url;
    }

    async function applyTextureToProfile(type, hash) {
        var existing = document.getElementById('profilePickerModal');
        if (existing) existing.remove();

        var res = await apiGet('/api/profiles');
        if (!res || !res.ok || !res.data || !res.data.success) {
            showToast(t('common.loadFailedShort'), 'error');
            return;
        }
        var profiles = res.data.profiles || [];

        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'profilePickerModal';

        var box = document.createElement('div');
        box.className = 'modal-box';
        box.style.maxWidth = '560px';

        var title = document.createElement('h3');
        title.textContent = t('texture.applyToProfile');
        box.appendChild(title);

        if (profiles.length === 0) {
            var empty = document.createElement('p');
            empty.className = 'empty-hint';
            empty.textContent = t('texture.noProfiles');
            box.appendChild(empty);
        } else {
            var grid = document.createElement('div');
            grid.className = 'profile-picker-grid';
            profiles.forEach(function (p) {
                var card = document.createElement('div');
                card.className = 'profile-picker-card';

                var cv = document.createElement('canvas');
                card.appendChild(cv);

                var nm = document.createElement('div');
                nm.className = 'profile-picker-name';
                nm.textContent = p.name;
                card.appendChild(nm);

                card.addEventListener('click', async function () {
                    var payload = { id: p.id };
                    if (type === 'CAPE') { payload.capeHash = hash; } else { payload.skinHash = hash; }
                    var r = await apiPost('/api/profiles/update', payload);
                    if (r && r.data && r.data.success) {
                        showToast(r.data.message || t('texture.applied'), 'success');
                        overlay.remove();
                    } else {
                        showToast((r && r.data && r.data.message) || t('texture.operationFailed'), 'error');
                    }
                });

                grid.appendChild(card);
                drawFaceCanvas(cv, p.skinHash ? ('/api/publicTexture/skin/' + p.skinHash) : '/img/juststeve.png');
            });
            box.appendChild(grid);
        }

        var closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'btn btn-secondary';
        closeBtn.style.marginTop = '16px';
        closeBtn.textContent = t('common.close');
        closeBtn.addEventListener('click', function () { overlay.remove(); });
        box.appendChild(closeBtn);

        overlay.appendChild(box);
        overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
        document.body.appendChild(overlay);
    }

    function widgetKey(el) {
        return el.getAttribute('data-widget') || el.id || null;
    }

    function persistOrder(container, storageKey) {
        var order = Array.prototype.slice.call(container.children)
            .map(widgetKey)
            .filter(Boolean);
        try { localStorage.setItem(storageKey, JSON.stringify(order)); } catch (e) {  }
    }

    function getDragAfterElement(container, x, y) {
        var candidates = Array.prototype.slice.call(container.children)
            .filter(function (el) { return el.classList.contains('sortable-item') && !el.classList.contains('dragging'); });
        var closest = null;
        var closestDistance = Infinity;
        candidates.forEach(function (el) {
            var rect = el.getBoundingClientRect();
            var dx = x - (rect.left + rect.width / 2);
            var dy = y - (rect.top + rect.height / 2);
            var distance = dx * dx + dy * dy;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = el;
            }
        });
        return closest;
    }

    function initSortableGrid(container, storageKey) {
        if (!container) return;
        var items = Array.prototype.slice.call(container.children);
        if (items.length < 2) return;

        try {
            var saved = JSON.parse(localStorage.getItem(storageKey) || '[]');
            if (saved && saved.length) {
                var map = {};
                items.forEach(function (el) { var k = widgetKey(el); if (k) map[k] = el; });
                saved.forEach(function (k) { if (map[k]) container.appendChild(map[k]); });
            }
        } catch (e) {  }

        items.forEach(function (el) {
            el.classList.add('sortable-item');
            el.setAttribute('draggable', 'false');
            el.addEventListener('dragstart', function (event) {
                if (!container._sortableEnabled) {
                    event.preventDefault();
                    return;
                }
                el.classList.add('dragging');
                try { event.dataTransfer.setData('text/plain', widgetKey(el) || ''); } catch (e) {  }
            });
            el.addEventListener('dragend', function () {
                el.classList.remove('dragging');
            });
        });

        container._setSortableEnabled = function (enabled) {
            container._sortableEnabled = enabled;
            if (enabled) {
                container.classList.add('sortable-enabled');
            } else {
                container.classList.remove('sortable-enabled');
            }
            items.forEach(function (el) {
                el.setAttribute('draggable', enabled ? 'true' : 'false');
            });
        };
        container._setSortableEnabled(false);

        container.addEventListener('dragover', function (event) {
            var dragging = container.querySelector('.dragging');
            if (!dragging) return;
            event.preventDefault();
            var after = getDragAfterElement(container, event.clientX, event.clientY);
            if (after == null || after === dragging) {
                container.appendChild(dragging);
            } else {
                container.insertBefore(dragging, after);
            }
        });
    }

    function initSortables() {
        document.querySelectorAll('[data-sortable]').forEach(function (el) {
            var name = el.getAttribute('data-sortable');
            var storageKey = 'ling-sort-' + name;
            initSortableGrid(el, storageKey);
            if (el.children.length < 2) return;
            var prev = el.previousElementSibling;
            if (prev && prev.classList && prev.classList.contains('layout-reset-bar')) return;

            var hasSaved = false;
            try { hasSaved = !!localStorage.getItem(storageKey); } catch (e) {  }

            var bar = document.createElement('div');
            bar.className = 'layout-reset-bar';

            var toggleBtn = document.createElement('button');
            toggleBtn.type = 'button';
            toggleBtn.className = 'layout-reset-btn layout-toggle-btn';
            toggleBtn.innerHTML = '<i class="fas fa-arrows-up-down-left-right"></i> ' + t('common.customizeLayout');
            toggleBtn.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                var enabled = !el._sortableEnabled;
                el._setSortableEnabled(enabled);
                toggleBtn.innerHTML = '<i class="fas ' + (enabled ? 'fa-floppy-disk' : 'fa-arrows-up-down-left-right') + '"></i> ' +
                    t(enabled ? 'common.finishLayout' : 'common.customizeLayout');
                if (!enabled) persistOrder(el, storageKey);
            });
            bar.appendChild(toggleBtn);

            if (hasSaved) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'layout-reset-btn';
                btn.innerHTML = '<i class="fas fa-rotate-left"></i> ' + t('common.resetLayout');
                btn.addEventListener('click', function () { global.resetSortableLayout(name); });
                bar.appendChild(btn);
            }
            el.parentNode.insertBefore(bar, el);
        });
    }

    function settingsBase() {
        return global.__SETTINGS_BASE__ || '';
    }

    function applyTheme(theme) {
        var dark = theme === 'dark';
        document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
        document.querySelectorAll('.theme-toggle i, #adminThemeToggle i').forEach(function (icon) {
            icon.className = dark ? 'fas fa-sun' : 'fas fa-moon';
        });
    }

    function initTheme() {
        var stored = null;
        try { stored = localStorage.getItem('ling-theme'); } catch (e) {  }
        if (stored) {
            applyTheme(stored);
        } else if (global.matchMedia && global.matchMedia('(prefers-color-scheme: dark)').matches) {
            applyTheme('dark');
        }

        apiGet(settingsBase() + '/api/settings/theme').then(function (res) {
            if (res && res.ok && res.data && res.data.theme) {
                applyTheme(res.data.theme);
                try { localStorage.setItem('ling-theme', res.data.theme); } catch (e) {  }
            }
        }).catch(function () {  });
    }

    function toggleTheme() {
        var current = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        applyTheme(next);
        try { localStorage.setItem('ling-theme', next); } catch (e) {  }
        apiPost(settingsBase() + '/api/settings/theme', { theme: next }).catch(function () {  });
    }

    function t(key) {
        var node = global.__I18N__;
        if (!node) return key;
        var parts = String(key).split('.');
        for (var i = 0; i < parts.length; i++) {
            if (node === null || typeof node !== 'object') return key;
            node = node[parts[i]];
        }
        if (typeof node !== 'string') return key;
        var list = Array.prototype.slice.call(arguments, 1);
        if (list.length === 1 && Array.isArray(list[0])) list = list[0];
        for (var j = 0; j < list.length; j++) {
            node = node.replace('{' + j + '}', list[j] === null || list[j] === undefined ? '' : String(list[j]));
        }
        return node;
    }

    function setLanguage(code) {
        if (!code) return;
        if (code === global.__LOCALE__) {
            closeLangMenus();
            return;
        }
        try { localStorage.setItem('ling-lang', code); } catch (e) {  }
        var cookieName = global.__LANG_COOKIE__ || 'ling-lang';
        document.cookie = cookieName + '=' + code + ';path=/;max-age=31536000;SameSite=Lax';
        var reload = function () { global.location.reload(); };
        var req = apiPost(settingsBase() + '/api/settings/language', { language: code });
        if (req && typeof req.then === 'function') {
            req.then(reload, reload);
        } else {
            reload();
        }
    }

    function closeLangMenus(event) {
        if (event && event.target && event.target.closest && event.target.closest('.lang-dropdown')) {
            return;
        }
        document.querySelectorAll('.lang-dropdown.open').forEach(function (d) {
            d.classList.remove('open');
        });
    }

    function toggleLangMenu(event) {
        if (event) event.stopPropagation();
        var src = event && event.target ? event.target : null;
        var dd = src && src.closest ? src.closest('.lang-dropdown') : null;
        var wasOpen = dd && dd.classList.contains('open');
        closeLangMenus();
        if (dd && !wasOpen) dd.classList.add('open');
    }

    function ensureBackdrop() {
        var backdrop = document.querySelector('.sidebar-backdrop');
        if (!backdrop) {
            backdrop = document.createElement('div');
            backdrop.className = 'sidebar-backdrop';
            backdrop.addEventListener('click', closeSidebar);
            document.body.appendChild(backdrop);
        }
        return backdrop;
    }

    function toggleSidebar() {
        ensureBackdrop();
        document.body.classList.toggle('sidebar-open');
    }

    function closeSidebar() {
        document.body.classList.remove('sidebar-open');
    }

    function showPermRisk(key) {
        var existing = document.getElementById('permRiskModal');
        if (existing) existing.remove();
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'permRiskModal';
        overlay.innerHTML =
            '<div class="modal-box" style="max-width:460px">' +
            '<h3><i class="fas fa-triangle-exclamation" style="color:#DC2626"></i> ' +
            escapeHtml(t('admin.permRisk.title')) + '</h3>' +
            '<p style="margin:12px 0;line-height:1.7;color:var(--color-text-muted)">' +
            escapeHtml(t('admin.permRisk.body')) + '</p>' +
            '<p style="margin:0;font-family:Consolas,monospace;font-size:12px;color:var(--color-text-faint)">' +
            escapeHtml(key) + '</p>' +
            '<div class="modal-actions"><button class="btn btn-secondary" id="permRiskCloseBtn">' +
            escapeHtml(t('common.close')) + '</button></div></div>';
        document.body.appendChild(overlay);
        document.getElementById('permRiskCloseBtn').addEventListener('click', function () { overlay.remove(); });
        overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
    }

    function showInfoDialog(title, message) {
        var existing = document.getElementById('infoDialogModal');
        if (existing) existing.remove();
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.id = 'infoDialogModal';
        overlay.innerHTML =
            '<div class="modal-box" style="max-width:460px">' +
            '<h3>' + escapeHtml(title || '') + '</h3>' +
            '<p style="margin:12px 0;line-height:1.7;color:var(--color-text-muted);white-space:pre-wrap">' +
            escapeHtml(message || '') + '</p>' +
            '<div class="modal-actions"><button class="btn btn-secondary" id="infoDialogCloseBtn">' +
            escapeHtml(t('common.close')) + '</button></div></div>';
        document.body.appendChild(overlay);
        document.getElementById('infoDialogCloseBtn').addEventListener('click', function () { overlay.remove(); });
        overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
    }

    function resolveAction(name) {
        if (!name) return null;
        if (typeof global[name] === 'function') return { fn: global[name], receiver: global };
        if (name.indexOf('.') > -1) {
            var parts = name.split('.');
            var receiver = global;
            for (var i = 0; i < parts.length - 1 && receiver; i++) receiver = receiver[parts[i]];
            if (receiver && typeof receiver[parts[parts.length - 1]] === 'function') {
                return { fn: receiver[parts[parts.length - 1]], receiver: receiver };
            }
        }
        return null;
    }

    function startBtnLoading(el) {
        if (!el) return null;
        if (el.dataset && el.dataset.loading === '1') return null;
        if (el.__btnStatus && el.__btnStatus.parentNode) {
            el.__btnStatus.parentNode.removeChild(el.__btnStatus);
        }
        el.__btnStatus = null;
        var state = { el: el, status: null, prevDisabled: false };
        if (el.dataset) el.dataset.loading = '1';
        if ('disabled' in el) { state.prevDisabled = el.disabled; el.disabled = true; }
        el.classList.add('is-loading');
        var status = document.createElement('span');
        status.className = 'btn-loading-status';
        status.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        state.status = status;
        el.__btnStatus = status;
        if (el.parentNode) el.parentNode.insertBefore(status, el.nextSibling);
        return state;
    }

    function finishBtnLoading(state, success) {
        if (!state || !state.el) return;
        var el = state.el;
        if (el.dataset) delete el.dataset.loading;
        if ('disabled' in el) el.disabled = state.prevDisabled;
        el.classList.remove('is-loading');
        var status = state.status;
        if (!status) return;
        if (success === false) {
            status.innerHTML = '<i class="fas fa-times"></i>';
            status.classList.add('is-failed');
        } else {
            status.innerHTML = '<i class="fas fa-check"></i>';
            status.classList.add('is-done');
        }
        setTimeout(function () {
            status.classList.add('fade-out');
            setTimeout(function () {
                if (status.parentNode) status.parentNode.removeChild(status);
                if (el.__btnStatus === status) el.__btnStatus = null;
            }, 700);
        }, 400);
    }

    function dispatchAction(e) {
        if (!e || !e.target || !e.target.closest) return;
        var el = e.target.closest('[data-action]');
        if (!el) return;
        var action = resolveAction(el.getAttribute('data-action'));
        if (!action) return;
        var args = [];
        if (el.hasAttribute('data-args')) {
            try {
                args = JSON.parse(el.getAttribute('data-args'));
                if (!Array.isArray(args)) args = [];
            } catch (err) {
                args = [];
            }
        }
        if (el.hasAttribute('data-this')) args.push(el);
        if (el.hasAttribute('data-event')) args.push(e);
        if (el.hasAttribute('data-prevent')) e.preventDefault();
        if (el.hasAttribute('data-stop')) e.stopPropagation();
        var name = el.getAttribute('data-action') || '';
        var state = /^(save|submit)/.test(name) ? startBtnLoading(el) : null;
        var result = action.fn.apply(action.receiver, args);
        if (state) {
            if (result && typeof result.then === 'function') {
                result.then(function (ok) { finishBtnLoading(state, ok !== false); },
                            function () { finishBtnLoading(state, false); });
            } else {
                finishBtnLoading(state, result !== false);
            }
        }
    }

    function init() {
        initTheme();
        initSortables();
        document.querySelectorAll('.sidebar a, .admin-sidebar a').forEach(function (a) {
            a.addEventListener('click', closeSidebar);
        });
        document.addEventListener('click', closeLangMenus);
        document.addEventListener('click', dispatchAction);
        document.addEventListener('change', dispatchAction);
        document.addEventListener('input', dispatchAction);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    global.toggleTheme = toggleTheme;
    global.t = t;
    global.setLanguage = setLanguage;
    global.toggleLangMenu = toggleLangMenu;
    global.toggleSidebar = toggleSidebar;
    global.showInfoDialog = showInfoDialog;
    global.closeSidebar = closeSidebar;
    global.showPermRisk = showPermRisk;
    global.startBtnLoading = startBtnLoading;
    global.finishBtnLoading = finishBtnLoading;
    global.initSortableGrid = initSortableGrid;
    global.resetSortableLayout = function (name) {
        try { localStorage.removeItem('ling-sort-' + name); } catch (e) {  }
        location.reload();
    };
    global.escapeHtml = escapeHtml;
    global.esc = escapeHtml;
    global.escAttr = escAttr;
    global.showToast = showToast;
    global.showConfirmDialog = showConfirmDialog;
    global.closeConfirmDialog = closeConfirmDialog;
    global.formatSize = formatSize;
    global.formatDate = formatDate;
    global.apiFetch = apiFetch;
    global.apiGet = apiGet;
    global.apiPost = apiPost;
    global.redirectToLogin = redirectToLogin;
    global.showLoading = showLoading;
    global.showError = showError;
    global.applyTextureToProfile = applyTextureToProfile;
    global.drawFaceCanvas = drawFaceCanvas;
})(window);
