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

    function ensureToastStack() {
        var stack = document.getElementById('uiToastStack');
        if (!stack) {
            stack = document.createElement('div');
            stack.id = 'uiToastStack';
            stack.className = 'ui-toast-stack';
            stack.setAttribute('aria-live', 'polite');
            document.body.appendChild(stack);
        }
        return stack;
    }

    var TOAST_ICONS = {
        success: 'fa-circle-check',
        error: 'fa-circle-xmark',
        warning: 'fa-triangle-exclamation',
        info: 'fa-circle-info'
    };
    var TOAST_TITLES = {
        success: '' + t('common.success') + '',
        error: '' + t('common.operationFailed') + '',
        warning: '' + t('common.warningTitle') + '',
        info: '' + t('common.infoTitle') + ''
    };

    function showToast(message, type, options) {
        options = options || {};
        type = TOAST_ICONS[type] ? type : 'info';
        var stack = ensureToastStack();
        var toast = document.createElement('div');
        toast.className = 'ui-toast ui-toast-' + type;
        toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
        toast.setAttribute('aria-live', type === 'error' ? 'assertive' : 'polite');

        var icon = document.createElement('span');
        icon.className = 'ui-toast-icon';
        icon.innerHTML = '<i class="fas ' + TOAST_ICONS[type] + '"></i>';

        var body = document.createElement('div');
        body.className = 'ui-toast-body';
        var titleText = options.title || TOAST_TITLES[type];
        if (titleText) {
            var title = document.createElement('div');
            title.className = 'ui-toast-title';
            title.textContent = titleText;
            body.appendChild(title);
        }
        var text = document.createElement('div');
        text.className = 'ui-toast-message';
        text.textContent = message === null || message === undefined ? '' : String(message);
        body.appendChild(text);

        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'ui-toast-close';
        close.setAttribute('aria-label', t('common.close'));
        close.innerHTML = '<i class="fas fa-xmark"></i>';

        var progress = document.createElement('span');
        progress.className = 'ui-toast-progress';

        toast.appendChild(icon);
        toast.appendChild(body);
        toast.appendChild(close);
        toast.appendChild(progress);
        stack.appendChild(toast);

        while (stack.children.length > 4) {
            stack.removeChild(stack.firstElementChild);
        }

        var duration = options.duration || (type === 'error' ? 4500 : 3200);
        progress.style.animationDuration = duration + 'ms';
        var timer = setTimeout(dismiss, duration);
        function dismiss() {
            clearTimeout(timer);
            if (!toast.parentNode) return;
            toast.classList.add('ui-toast-leaving');
            setTimeout(function () { if (toast.parentNode) toast.remove(); }, 210);
        }
        close.addEventListener('click', dismiss);
        toast.addEventListener('mouseenter', function () { clearTimeout(timer); });
        toast.addEventListener('mouseleave', function () {
            timer = setTimeout(dismiss, Math.min(duration, 1800));
        });
        return { close: dismiss, element: toast };
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

    function createAdminModal(id, title, bodyContent, footerContent, onClose, maxWidth) {
        var existing = document.getElementById(id);
        if (existing) existing.remove();

        var modal = document.createElement('div');
        modal.className = 'modal';
        modal.id = id;
        modal.style.zIndex = '1100';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');

        var backdrop = document.createElement('div');
        backdrop.className = 'modal-overlay';

        var card = document.createElement('div');
        card.className = 'modal-card';
        if (maxWidth) card.style.maxWidth = maxWidth;

        var header = document.createElement('div');
        header.className = 'modal-header';
        var heading = title && title.nodeType === 1 ? title : document.createElement('h3');
        if (!(title && title.nodeType === 1)) heading.textContent = title === null || title === undefined ? '' : String(title);

        var closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'modal-close';
        closeBtn.textContent = '×';
        closeBtn.title = t('common.close');
        closeBtn.setAttribute('aria-label', closeBtn.title);
        header.appendChild(heading);
        header.appendChild(closeBtn);

        var body = document.createElement('div');
        body.className = 'modal-body';
        if (bodyContent) body.appendChild(bodyContent);
        card.appendChild(header);
        card.appendChild(body);
        if (footerContent) {
            var footer = document.createElement('div');
            footer.className = 'modal-footer';
            footer.appendChild(footerContent);
            card.appendChild(footer);
        }

        modal.appendChild(backdrop);
        modal.appendChild(card);
        document.body.appendChild(modal);

        var close = function() {
            if (onClose) onClose();
            if (modal.parentNode) modal.remove();
        };
        closeBtn.addEventListener('click', close);
        backdrop.addEventListener('click', close);
        return { modal: modal, close: close };
    }

    function openUiDialog(options) {
        options = options || {};
        var overlay = document.createElement('div');
        overlay.className = 'ui-dialog-overlay';
        if (options.id) overlay.id = options.id;
        overlay.setAttribute('role', 'dialog');
        overlay.setAttribute('aria-modal', 'true');

        var dialog = document.createElement('div');
        dialog.className = 'ui-dialog' + (options.wide ? ' ui-dialog-wide' : '');
        if (options.type) {
            var icon = document.createElement('div');
            icon.className = 'ui-dialog-icon ui-dialog-icon-' + options.type;
            var icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', warning: 'fa-triangle-exclamation', info: 'fa-circle-info', danger: 'fa-triangle-exclamation' };
            icon.innerHTML = '<i class="fas ' + (icons[options.type] || icons.info) + '"></i>';
            dialog.appendChild(icon);
        }

        if (options.title) {
            var heading = document.createElement('h3');
            heading.textContent = options.title;
            dialog.appendChild(heading);
        }
        if (options.message) {
            var message = document.createElement('p');
            message.className = 'ui-dialog-message';
            message.textContent = options.message;
            dialog.appendChild(message);
        }
        if (options.detail) {
            var detail = document.createElement('div');
            detail.className = 'ui-dialog-detail';
            detail.textContent = options.detail;
            dialog.appendChild(detail);
        }
        if (options.body) dialog.appendChild(options.body);

        var actions = document.createElement('div');
        actions.className = 'ui-dialog-actions';
        if (options.actions) {
            options.actions.forEach(function (item) {
                if (item.nodeType) actions.appendChild(item);
                else {
                    var btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'btn ' + (item.className || 'btn-secondary');
                    btn.textContent = item.label;
                    if (item.onClick) btn.addEventListener('click', item.onClick);
                    actions.appendChild(btn);
                }
            });
        }
        dialog.appendChild(actions);

        if (options.closeButton !== false) {
            var closeBtn = document.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'ui-dialog-close';
            closeBtn.setAttribute('aria-label', t('common.close'));
            closeBtn.innerHTML = '&times;';
            closeBtn.addEventListener('click', function () { close(); });
            dialog.appendChild(closeBtn);
        }

        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
        document.body.classList.add('ui-modal-open');
        var previous = document.activeElement;
        var closed = false;
        function close() {
            if (closed) return;
            closed = true;
            overlay.remove();
            document.body.classList.remove('ui-modal-open');
            document.removeEventListener('keydown', onKey);
            if (previous && typeof previous.focus === 'function') previous.focus();
            if (options.onClose) options.onClose();
        }
        function onKey(event) {
            if (event.key === 'Escape') { event.preventDefault(); close(); }
        }
        overlay.addEventListener('click', function (event) { if (event.target === overlay) close(); });
        document.addEventListener('keydown', onKey);
        var focusTarget = actions.querySelector('.btn-primary, .btn-danger, .btn-secondary');
        if (focusTarget) setTimeout(function () { focusTarget.focus(); }, 0);
        return { overlay: overlay, dialog: dialog, close: close };
    }

    function showAlertDialog(optionsOrMessage, type) {
        var options = typeof optionsOrMessage === 'string' ? { message: optionsOrMessage, type: type } : (optionsOrMessage || {});
        options.type = options.type || 'info';
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-primary';
        button.textContent = options.confirmText || t('common.confirm');
        var dialog = openUiDialog({
            id: options.id || 'uiAlertDialog',
            title: options.title || '' + t('common.infoTitle') + '',
            message: options.message || '',
            detail: options.detail,
            type: options.type,
            actions: [button],
            onClose: options.onClose
        });
        button.addEventListener('click', dialog.close);
        return dialog;
    }

    function showPromptDialog(options, onConfirm) {
        options = options || {};
        var body = document.createElement('div');
        body.className = 'form-group';
        var input = document.createElement('input');
        input.type = 'text';
        input.className = 'form-input';
        input.value = options.value == null ? '' : String(options.value);
        input.placeholder = options.placeholder || '';
        if (options.maxlength) input.maxLength = options.maxlength;
        body.appendChild(input);
        var cancel = document.createElement('button');
        cancel.type = 'button';
        cancel.className = 'btn btn-secondary';
        cancel.textContent = options.cancelText || t('common.cancel');
        var confirm = document.createElement('button');
        confirm.type = 'button';
        confirm.className = 'btn btn-primary';
        confirm.textContent = options.confirmText || t('common.save');
        var dialog = openUiDialog({
            id: options.id || 'uiPromptDialog',
            title: options.title || '' + t('common.edit') + '',
            message: options.message || '',
            body: body,
            actions: [cancel, confirm],
            onClose: options.onClose
        });
        cancel.addEventListener('click', dialog.close);
        confirm.addEventListener('click', function () {
            var result;
            try {
                result = onConfirm ? onConfirm(input.value) : true;
            } catch (error) {
                return;
            }
            if (result && typeof result.then === 'function') {
                result.then(function (ok) { if (ok !== false) dialog.close(); });
            } else if (result !== false) {
                dialog.close();
            }
        });
        setTimeout(function () { input.focus(); input.select(); }, 0);
        return dialog;
    }

    function showInlineAlert(container, optionsOrMessage, type) {
        if (!container) return null;
        if (container.classList && container.classList.contains('ui-inline-alert')) {
            container = container.parentNode;
        }
        var options = typeof optionsOrMessage === 'string'
            ? { message: optionsOrMessage, type: type || 'info' }
            : (optionsOrMessage || {});
        var kind = options.type || 'info';
        var icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', warning: 'fa-triangle-exclamation', info: 'fa-circle-info' };
        container.innerHTML = '';
        var alert = document.createElement('div');
        alert.className = 'ui-inline-alert ui-inline-alert-' + (icons[kind] ? kind : 'info');
        alert.setAttribute('role', kind === 'error' ? 'alert' : 'status');
        var icon = document.createElement('i');
        icon.className = 'fas ' + (icons[kind] || icons.info);
        alert.appendChild(icon);
        var text = document.createElement('span');
        text.textContent = options.message || '';
        alert.appendChild(text);
        if (options.closable !== false && kind === 'error') {
            var close = document.createElement('button');
            close.type = 'button';
            close.className = 'ui-inline-alert-close';
            close.setAttribute('aria-label', t('common.close'));
            close.innerHTML = '&times;';
            close.addEventListener('click', function () { alert.remove(); });
            alert.appendChild(close);
        }
        container.appendChild(alert);
        return alert;
    }

    function showPopconfirm(anchor, message, onConfirm, options) {
        options = options || {};
        var old = document.getElementById('uiPopconfirm');
        if (old) old.remove();
        if (!anchor || !anchor.getBoundingClientRect) return null;
        var pop = document.createElement('div');
        pop.id = 'uiPopconfirm';
        pop.className = 'ui-popconfirm' + (options.danger !== false ? ' ui-popconfirm-danger' : '');
        pop.setAttribute('role', 'dialog');
        var text = document.createElement('div');
        text.className = 'ui-popconfirm-message';
        text.textContent = message || '';
        var actions = document.createElement('div');
        actions.className = 'ui-popconfirm-actions';
        var cancel = document.createElement('button');
        cancel.type = 'button';
        cancel.className = 'btn btn-secondary';
        cancel.textContent = options.cancelText || t('common.cancel');
        var confirm = document.createElement('button');
        confirm.type = 'button';
        confirm.className = 'btn ' + (options.danger !== false ? 'btn-danger' : 'btn-primary');
        confirm.textContent = options.confirmText || t('common.confirm');
        actions.appendChild(cancel);
        actions.appendChild(confirm);
        pop.appendChild(text);
        pop.appendChild(actions);
        document.body.appendChild(pop);

        function close() {
            pop.remove();
            document.removeEventListener('mousedown', onOutside, true);
            document.removeEventListener('keydown', onKey);
            if (anchor && typeof anchor.focus === 'function') anchor.focus();
        }
        function onOutside(event) { if (!pop.contains(event.target) && event.target !== anchor) close(); }
        function onKey(event) { if (event.key === 'Escape') close(); }
        cancel.addEventListener('click', close);
        confirm.addEventListener('click', function () { close(); if (onConfirm) onConfirm(); });
        document.addEventListener('mousedown', onOutside, true);
        document.addEventListener('keydown', onKey);

        var rect = anchor.getBoundingClientRect();
        var width = pop.offsetWidth || 280;
        var left = Math.max(8, Math.min(window.innerWidth - width - 8, rect.left + rect.width / 2 - width / 2));
        var top = rect.top - (pop.offsetHeight || 100) - 10;
        var below = top < 8;
        if (below) top = rect.bottom + 10;
        pop.style.left = left + 'px';
        pop.style.top = top + 'px';
        pop.style.setProperty('--ui-arrow-left', Math.max(12, Math.min(width - 20, rect.left + rect.width / 2 - left)) + 'px');
        if (below) pop.classList.add('ui-popconfirm-below');
        confirm.focus();
        return { close: close, element: pop };
    }

    function initTooltips() {
        var tip = null;
        function hide() {
            if (tip) { tip.remove(); tip = null; }
        }
        function show(target) {
            var text = target.getAttribute('data-tooltip');
            if (!text) return;
            hide();
            tip = document.createElement('div');
            tip.className = 'ui-tooltip';
            tip.textContent = text;
            document.body.appendChild(tip);
            var rect = target.getBoundingClientRect();
            var width = tip.offsetWidth;
            var height = tip.offsetHeight;
            var left = Math.max(8, Math.min(window.innerWidth - width - 8, rect.left + rect.width / 2 - width / 2));
            var top = rect.top - height - 8;
            var below = top < 8;
            if (below) top = rect.bottom + 8;
            tip.style.left = left + 'px';
            tip.style.top = top + 'px';
            tip.style.setProperty('--ui-arrow-left', Math.max(10, Math.min(width - 18, rect.left + rect.width / 2 - left)) + 'px');
            if (below) tip.classList.add('ui-tooltip-below');
        }
        document.addEventListener('mouseover', function (event) {
            var target = event.target.closest ? event.target.closest('[data-tooltip]') : null;
            if (target) show(target);
        });
        document.addEventListener('mouseout', function (event) {
            var target = event.target.closest ? event.target.closest('[data-tooltip]') : null;
            if (target && !target.contains(event.relatedTarget)) hide();
        });
        document.addEventListener('focusin', function (event) {
            var target = event.target.closest ? event.target.closest('[data-tooltip]') : null;
            if (target) show(target);
        });
        document.addEventListener('focusout', hide);
        window.addEventListener('scroll', hide, true);
        window.addEventListener('resize', hide);
    }

    function showConfirmDialog(message, onConfirm, options) {
        options = options || {};
        activeConfirm = onConfirm;
        previousFocus = document.activeElement;
        var cancelBtn = document.createElement('button');
        cancelBtn.type = 'button';
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = options.cancelText || t('common.cancel');
        var confirmBtn = document.createElement('button');
        confirmBtn.type = 'button';
        confirmBtn.className = 'btn ' + (options.danger === false ? 'btn-primary' : 'btn-danger');
        confirmBtn.textContent = options.confirmText || t('common.confirm');
        var dialog = openUiDialog({
            id: 'confirmDialog',
            title: options.title || t('common.confirmAction'),
            message: message,
            detail: options.detail,
            type: options.type || 'warning',
            actions: [cancelBtn, confirmBtn],
            onClose: function () {
                var callback = activeConfirm;
                activeConfirm = null;
                document.removeEventListener('keydown', onConfirmKeydown);
                if (previousFocus && typeof previousFocus.focus === 'function') previousFocus.focus();
                previousFocus = null;
                if (callback && dialog.__confirmed) callback();
            }
        });
        cancelBtn.addEventListener('click', function () { dialog.close(); });
        confirmBtn.addEventListener('click', function () {
            dialog.__confirmed = true;
            dialog.close();
        });
        document.addEventListener('keydown', onConfirmKeydown);
        return dialog;
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

    function drawCapeThumb(canvas, url) {
        canvas.width = 64;
        canvas.height = 64;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#FFF0F5';
        ctx.fillRect(0, 0, 64, 64);
        var img = new Image();
        img.onload = function () {
            ctx.imageSmoothingEnabled = false;
            var padX = (64 - img.width) / 2;
            var padY = (64 - img.height) / 2;
            ctx.drawImage(img, Math.max(0, padX), Math.max(0, padY), img.width, img.height);
        };
        img.src = url;
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

    function renderAdminPagination(containerId, total, currentPage, pageSize, onPageChange, onPageSizeChange) {
        var container = document.getElementById(containerId);
        if (!container) return;

        var pageCount = Math.max(1, Math.ceil(total / pageSize));
        currentPage = Math.min(Math.max(1, currentPage), pageCount);
        var start = total ? (currentPage - 1) * pageSize + 1 : 0;
        var end = Math.min(total, currentPage * pageSize);
        var summary = document.createElement('div');
        summary.className = 'pagination-summary';
        summary.textContent = t('admin.common.paginationSummary', start, end, total);

        var controls = document.createElement('div');
        controls.className = 'pagination-controls';

        var sizeLabel = document.createElement('label');
        sizeLabel.className = 'pagination-size-label';
        sizeLabel.appendChild(document.createTextNode(t('admin.common.itemsPerPage')));

        var sizeSelect = document.createElement('select');
        sizeSelect.className = 'form-input pagination-size-select';
        [50, 100, 200, 500, 1000].forEach(function(size) {
            var option = document.createElement('option');
            option.value = String(size);
            option.textContent = String(size);
            sizeSelect.appendChild(option);
        });
        sizeSelect.value = String(pageSize);
        sizeSelect.addEventListener('change', function() {
            onPageSizeChange(Number(sizeSelect.value));
        });
        sizeLabel.appendChild(sizeSelect);

        var previous = document.createElement('button');
        previous.type = 'button';
        previous.className = 'pagination-btn';
        previous.textContent = '‹';
        previous.title = t('admin.common.previousPage');
        previous.setAttribute('aria-label', previous.title);
        previous.disabled = currentPage <= 1;
        previous.addEventListener('click', function() { onPageChange(currentPage - 1); });

        var pageLabel = document.createElement('span');
        pageLabel.className = 'pagination-page';
        pageLabel.textContent = t('admin.common.pageSummary', currentPage, pageCount);

        var next = document.createElement('button');
        next.type = 'button';
        next.className = 'pagination-btn';
        next.textContent = '›';
        next.title = t('admin.common.nextPage');
        next.setAttribute('aria-label', next.title);
        next.disabled = currentPage >= pageCount;
        next.addEventListener('click', function() { onPageChange(currentPage + 1); });

        controls.appendChild(sizeLabel);
        controls.appendChild(previous);
        controls.appendChild(pageLabel);
        controls.appendChild(next);
        container.replaceChildren(summary, controls);
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
        var title = document.createElement('h3');
        var icon = document.createElement('i');
        icon.className = 'fas fa-triangle-exclamation admin-dialog-warning-icon';
        title.appendChild(icon);
        title.appendChild(document.createTextNode(' ' + t('admin.permRisk.title')));

        var body = document.createElement('div');
        var description = document.createElement('p');
        description.className = 'admin-dialog-message';
        description.textContent = t('admin.permRisk.body');
        var keyText = document.createElement('p');
        keyText.className = 'admin-dialog-key';
        keyText.textContent = key;
        body.appendChild(description);
        body.appendChild(keyText);

        var footerButton = document.createElement('button');
        footerButton.type = 'button';
        footerButton.className = 'btn btn-secondary';
        footerButton.textContent = t('common.close');
        var footer = document.createElement('div');
        footer.appendChild(footerButton);

        var dialog = createAdminModal('permRiskModal', title, body, footer, null, '460px');
        footerButton.addEventListener('click', dialog.close);
    }

    function showInfoDialog(title, message) {
        var body = document.createElement('p');
        body.className = 'admin-dialog-message admin-dialog-prewrap';
        body.textContent = message || '';
        var footerButton = document.createElement('button');
        footerButton.type = 'button';
        footerButton.className = 'btn btn-secondary';
        footerButton.textContent = t('common.close');
        var footer = document.createElement('div');
        footer.appendChild(footerButton);
        var dialog = createAdminModal('infoDialogModal', title || '', body, footer, null, '460px');
        footerButton.addEventListener('click', dialog.close);
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
        initTooltips();
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
    global.renderAdminPagination = renderAdminPagination;
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
    global.showAlertDialog = showAlertDialog;
    global.showAlert = showAlertDialog;
    global.showInlineAlert = showInlineAlert;
    global.openUiDialog = openUiDialog;
    global.showPromptDialog = showPromptDialog;
    global.showConfirmDialog = showConfirmDialog;
    global.showPopconfirm = showPopconfirm;
    global.closeConfirmDialog = closeConfirmDialog;
    global.formatSize = formatSize;
    global.formatDate = formatDate;
    global.apiFetch = apiFetch;
    global.apiGet = apiGet;
    global.apiPost = apiPost;
    global.redirectToLogin = redirectToLogin;
    global.showLoading = showLoading;
    global.showError = showError;
    global.drawFaceCanvas = drawFaceCanvas;
    global.drawCapeThumb = drawCapeThumb;
})(window);
