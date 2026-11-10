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
(function () {
    function elements(card) {
        return Array.prototype.slice.call(card.querySelectorAll('input, textarea, select'))
            .filter(function (el) {
                if (el.disabled || el.getAttribute('data-save-ignore') !== null) return false;
                return el.type !== 'button' && el.type !== 'submit' && el.type !== 'file';
            });
    }

    function valueOf(el) {
        if (el.type === 'checkbox') return el.checked ? 'true' : 'false';
        return el.value == null ? '' : String(el.value);
    }

    function setValue(el, value) {
        if (el.type === 'checkbox') {
            el.checked = value === true || value === 'true';
        } else {
            el.value = value == null ? '' : String(value);
        }
    }

    function init(toolbar) {
        var card = toolbar.closest('.card');
        if (!card) return;
        if (toolbar.__saveCard) return;
        var status = toolbar.querySelector('[data-save-status]');
        var reset = toolbar.querySelector('[data-save-reset]');
        var save = toolbar.querySelector('[data-save-submit]');
        var initial = [];
        var saving = false;

        function capture() {
            initial = elements(card).map(function (el) {
                return { el: el, value: valueOf(el) };
            });
            if (status) {
                status.textContent = '未修改';
                status.className = 'settings-status settings-status-clean';
            }
            update();
        }

        function dirty() {
            return initial.some(function (item) { return valueOf(item.el) !== item.value; });
        }

        function update() {
            var changed = dirty();
            if (!saving) {
                if (reset) reset.disabled = !changed;
                if (save) save.disabled = !changed;
                if (status) {
                    status.textContent = changed ? '有未保存更改' : '未修改';
                    status.className = 'settings-status settings-status-' + (changed ? 'dirty' : 'clean');
                }
            }
        }

        function finish(ok, message) {
            saving = false;
            if (ok) {
                capture();
                if (status) {
                    status.textContent = '已保存';
                    status.className = 'settings-status settings-status-saved';
                }
            } else {
                update();
                if (status) {
                    status.textContent = message || '保存失败';
                    status.className = 'settings-status settings-status-failed';
                }
            }
        }

        card.addEventListener('input', update);
        card.addEventListener('change', update);
        if (reset) {
            reset.addEventListener('click', function () {
                initial.forEach(function (item) {
                    setValue(item.el, item.value);
                    item.el.dispatchEvent(new Event('change', { bubbles: true }));
                });
                update();
            });
        }
        if (save) {
            save.addEventListener('click', function () {
                var action = toolbar.getAttribute('data-save-action');
                if (action === 'confirmCorsSettings') {
                    var cors = window.confirmCorsSettings;
                    if (typeof cors === 'function') cors(save);
                    return;
                }
                var fn = window[action];
                if (typeof fn !== 'function') {
                    finish(false, '保存操作不可用');
                    return;
                }
                saving = true;
                save.disabled = true;
                reset.disabled = true;
                if (status) {
                    status.textContent = '正在保存...';
                    status.className = 'settings-status settings-status-saving';
                }
                Promise.resolve(fn(save)).then(function (ok) {
                    finish(ok !== false);
                }).catch(function (error) {
                    finish(false, error && error.message ? error.message : '保存失败');
                });
            });
        }
        toolbar.__saveCard = { capture: capture };
        capture();
    }

    window.initAdminSaveCards = function () {
        document.querySelectorAll('[data-save-card]').forEach(init);
    };

    window.refreshAdminSaveCards = function () {
        document.querySelectorAll('[data-save-card]').forEach(function (toolbar) {
            if (toolbar.__saveCard) toolbar.__saveCard.capture();
        });
    };

    window.refreshAdminSaveCard = function (element) {
        var toolbar = element && element.closest ? element.closest('[data-save-card]') : null;
        if (toolbar && toolbar.__saveCard) toolbar.__saveCard.capture();
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', window.initAdminSaveCards);
    } else {
        window.initAdminSaveCards();
    }
})();
