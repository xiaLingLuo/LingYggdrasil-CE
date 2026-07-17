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
    var DISMISS_KEY = 'announcement_dismissed';
    // Simple hash for content comparison
    function hash(str) {
        var h = 0;
        for (var i = 0; i < str.length; i++) {
            h = ((h << 5) - h) + str.charCodeAt(i);
            h |= 0;
        }
        return '' + h;
    }
    function parseScope(sc) { return (sc || 'user').split('|').filter(Boolean); }
    function getPageScope() {
        var p = window.location.pathname;
        if (p === '/meow' || p === '/meow/') return 'home';
        if (p.startsWith('/admin/')) return 'admin';
        return 'user';
    }
    function renderMarkdown(text) {
        if (!text) return '';
        var h = (text || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        // Headings
        h = h.replace(/^### (.+)$/gm, '<h3>$1</h3>');
        h = h.replace(/^## (.+)$/gm, '<h2>$1</h2>');
        h = h.replace(/^# (.+)$/gm, '<h3>$1</h3>');
        // Bold, italic, code, link
        h = h.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        h = h.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        h = h.replace(/`([^`]+)`/g, '<code>$1</code>');
        h = h.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
        // Paragraphs (double newline)
        h = h.replace(/\n\n/g, '</p><p>');
        h = '<p>' + h + '</p>';
        h = h.replace(/<p>\s*<\/p>/g, '');
        return h;
    }

    fetch('/api/announcement').then(function(r) { return r.json(); }).then(function(data) {
        if (!data.mode || data.mode === 'off') return;
        if (!data.content || !data.content.trim()) return;
        if (data.mode !== 'top_force' && !data.loggedIn) return;
        var scopes = parseScope(data.scope);
        if (!scopes.includes(getPageScope())) return;
        var contentHash = hash(data.content);
        if (data.mode !== 'top_force') {
            var dismissed = localStorage.getItem(DISMISS_KEY);
            if (dismissed === contentHash) return;
        }

        var contentHtml = renderMarkdown(data.content);

        if (data.mode === 'modal') {
            showModal(contentHtml, contentHash, data.mode);
        } else if (data.mode === 'toast') {
            showToast(contentHtml, contentHash);
        } else if (data.mode === 'top' || data.mode === 'top_force') {
            showTop(contentHtml, contentHash, data.mode);
        }
    }).catch(function() {});

    function dismiss(contentHash) {
        if (contentHash) localStorage.setItem(DISMISS_KEY, contentHash);
    }

    function showModal(contentHtml, contentHash, mode) {
        var overlay = document.createElement('div');
        overlay.className = 'announcement-overlay';
        overlay.innerHTML = '<div class="announcement-modal">' +
            '<button class="ann-close modal-close-btn" style="position:absolute;top:12px;right:14px;">&times;</button>' +
            '<h3>\u7CFB\u7EDF\u516C\u544A</h3>' +
            '<div class="ann-content">' + contentHtml + '</div></div>';
        document.body.appendChild(overlay);
        overlay.querySelector('.ann-close').addEventListener('click', function() {
            overlay.remove();
            dismiss(contentHash);
        });
        overlay.addEventListener('click', function(e) { if (e.target === overlay) { overlay.remove(); dismiss(contentHash); } });
    }

    function showTop(contentHtml, contentHash, mode) {
        var bar = document.createElement('div');
        bar.className = 'announcement-anchor top';
        if (mode === 'top_force') {
            bar.style.position = 'relative';
        }
        bar.innerHTML = '<div class="ann-content" style="text-align:center">' + contentHtml + '</div>';
        if (mode === 'top') {
            bar.innerHTML += '<button class="ann-close">&times;</button>';
        }
        document.body.insertBefore(bar, document.body.firstChild);
        if (mode === 'top') {
            bar.querySelector('.ann-close').addEventListener('click', function() {
                bar.remove();
                dismiss(contentHash);
            });
        }
    }

    function showToast(contentHtml, contentHash) {
        var toast = document.createElement('div');
        toast.className = 'announcement-toast';
        toast.innerHTML = '<button class="ann-close">&times;</button>' +
            '<div class="ann-content">' + contentHtml + '</div>' +
            '<div class="ann-progress" id="annProgress"></div>';
        document.body.appendChild(toast);

        var progress = toast.querySelector('#annProgress');
        var duration = 13000;
        var start = Date.now();
        var timer = setInterval(function() {
            var elapsed = Date.now() - start;
            var pct = Math.max(0, 100 - (elapsed / duration * 100));
            progress.style.width = pct + '%';
            if (elapsed >= duration) {
                clearInterval(timer);
                toast.remove();
                dismiss(contentHash);
            }
        }, 100);

        toast.querySelector('.ann-close').addEventListener('click', function() {
            clearInterval(timer);
            toast.remove();
            dismiss(contentHash);
        });
    }
})();
