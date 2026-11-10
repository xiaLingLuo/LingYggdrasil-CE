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
    var KEY = 'ling-low-end-gpu';
    var DEFAULT_SKIN = '/img/juststeve.png';

    function lowEnd() {
        try {
            return localStorage.getItem(KEY) === '1';
        } catch (error) {
            return false;
        }
    }

    function setLowEnd(value) {
        try {
            localStorage.setItem(KEY, value ? '1' : '0');
        } catch (error) {
            
        }
    }

    function disposeIn(root) {
        (root || document).querySelectorAll('canvas[data-texture-3d]').forEach(function (canvas) {
            if (canvas.__textureObserver) {
                try { canvas.__textureObserver.disconnect(); } catch (error) { }
                canvas.__textureObserver = null;
            }
            if (canvas.__textureViewer) {
                try { canvas.__textureViewer.dispose(); } catch (error) { }
                canvas.__textureViewer = null;
            }
        });
    }

    function fallback(canvas, url) {
        var img = document.createElement('img');
        img.src = url;
        img.alt = '';
        img.loading = 'lazy';
        img.style.width = '100%';
        img.style.height = '100%';
        img.style.objectFit = 'contain';
        img.style.imageRendering = 'pixelated';
        canvas.replaceWith(img);
    }

    function mount(canvas, type, url, options) {
        options = options || {};
        if (!url) return false;
        if (!options.force && lowEnd()) {
            fallback(canvas, url);
            return false;
        }
        if (typeof skinview3d === 'undefined') {
            fallback(canvas, url);
            return false;
        }
        try {
            var box = canvas.parentElement;
            var width = Math.max(120, box ? box.clientWidth : 160);
            var height = Math.max(120, box ? box.clientHeight : 160);
            var viewer = new skinview3d.SkinViewer({
                canvas: canvas,
                width: width,
                height: height,
                model: 'default'
            });
            function fail() {
                try { viewer.dispose(); } catch (error) { }
                if (canvas.__textureViewer === viewer) canvas.__textureViewer = null;
                if (canvas.parentNode) fallback(canvas, url);
            }
            var pending;
            if (type === 'CAPE') {
                pending = Promise.all([viewer.loadSkin(DEFAULT_SKIN), viewer.loadCape(url)]);
            } else {
                pending = viewer.loadSkin(url);
            }
            if (pending && typeof pending.catch === 'function') pending.catch(fail);
            viewer.autoRotate = !!options.rotate;
            canvas.__textureViewer = viewer;
            return true;
        } catch (error) {
            fallback(canvas, url);
            return false;
        }
    }

    function hydrate(root) {
        root = root || document;
        disposeIn(root);
        var canvases = Array.prototype.slice.call(root.querySelectorAll('canvas[data-texture-3d]'));
        if (lowEnd()) {
            canvases.forEach(function (canvas) {
                fallback(canvas, canvas.getAttribute('data-url'));
            });
            return;
        }
        if (typeof IntersectionObserver === 'undefined') {
            canvases.forEach(function (canvas) {
                mount(canvas, canvas.getAttribute('data-type'), canvas.getAttribute('data-url'));
            });
            return;
        }
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                var canvas = entry.target;
                if (entry.isIntersecting) {
                    if (!canvas.__textureViewer) {
                        mount(canvas, canvas.getAttribute('data-type'), canvas.getAttribute('data-url'));
                    }
                } else if (canvas.__textureViewer) {
                    try { canvas.__textureViewer.dispose(); } catch (error) { }
                    canvas.__textureViewer = null;
                }
            });
        }, { rootMargin: '200px' });
        canvases.forEach(function (canvas) {
            canvas.__textureObserver = observer;
            observer.observe(canvas);
        });
    }

    function bindToggle(button, onChange) {
        if (!button) return;
        button.classList.toggle('active', lowEnd());
        button.addEventListener('click', function () {
            setLowEnd(!lowEnd());
            button.classList.toggle('active', lowEnd());
            if (onChange) onChange(lowEnd());
        });
    }

    window.Texture3D = {
        isLowEnd: lowEnd,
        setLowEnd: setLowEnd,
        hydrate: hydrate,
        mount: mount,
        dispose: disposeIn,
        bindToggle: bindToggle
    };
})();
