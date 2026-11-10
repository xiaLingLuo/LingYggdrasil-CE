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
    function initSubnav() {
        var content = document.querySelector('.admin-content');
        if (!content) return;
        if (content.querySelector(':scope > .subnav')) return;

        var cards = Array.prototype.slice.call(content.querySelectorAll(':scope > .settings-card'));
        if (cards.length < 2) return;

        var nav = document.createElement('div');
        nav.className = 'subnav';

        cards.forEach(function (card, index) {
            var titleEl = card.querySelector('.card-title');
            var label = titleEl ? titleEl.textContent.trim() : ('#' + (index + 1));
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'subnav-item' + (index === 0 ? ' active' : '');
            btn.textContent = label;
            btn.addEventListener('click', function () {
                cards.forEach(function (c) { c.style.display = 'none'; });
                nav.querySelectorAll('.subnav-item').forEach(function (b) { b.classList.remove('active'); });
                card.style.display = '';
                btn.classList.add('active');
            });
            nav.appendChild(btn);
        });

        cards.forEach(function (card, index) {
            if (index !== 0) card.style.display = 'none';
        });

        var header = content.querySelector(':scope > .page-header');
        if (header && header.nextSibling) {
            content.insertBefore(nav, header.nextSibling);
        } else if (header) {
            content.appendChild(nav);
        } else {
            content.insertBefore(nav, content.firstChild);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSubnav);
    } else {
        initSubnav();
    }
})();
