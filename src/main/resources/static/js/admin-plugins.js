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

var pluginData = {};

function pluginStateLabel(state) {
    var label = t('admin.plugins.status.' + state);
    return label === 'admin.plugins.status.' + state ? state : label;
}

async function loadPlugins() {
    var list = document.getElementById('pluginList');
    if (!list) return;
    try {
        var res = await fetch('/admin/api/plugins');
        var data = res.ok ? await res.json() : { plugins: [] };
        renderPlugins(data.plugins || []);
    } catch (err) {
        list.innerHTML = '<p class="text-muted">' + esc(t('common.loadFailed')) + '</p>';
    }
}

function renderPlugins(plugins) {
    var list = document.getElementById('pluginList');
    if (!plugins.length) {
        list.innerHTML = '<p class="text-muted">' + esc(t('admin.plugins.empty')) + '</p>';
        return;
    }
    pluginData = {};
    var html = '';
    plugins.forEach(function (p) {
        pluginData[p.name] = p;
        var authors = (p.authors && p.authors.length) ? p.authors.join(', ') : t('admin.plugins.unknownAuthor');
        var statusClass = 'status-' + p.state;
        var depBtn = (p.state === 'missingDependency')
            ? ' <button type="button" class="plugin-help-btn" title="' + esc(t('admin.plugins.dependencies')) +
              '" data-action="pluginShowDependencies" data-args="' + esc(JSON.stringify([p.name])) + '">?</button>'
            : '';
        var actions = '';
        if (p.state === 'enabled') {
            actions += '<button class="btn btn-secondary" data-action="pluginReload" data-args="' +
                esc(JSON.stringify([p.name])) + '">' + t('admin.plugins.action.reload') + '</button>';
            actions += '<button class="btn btn-danger" data-action="pluginDisable" data-args="' +
                esc(JSON.stringify([p.name])) + '">' + t('admin.plugins.action.stop') + '</button>';
        } else if (p.state === 'disabled' || p.state === 'loadFailed') {
            actions += '<button class="btn btn-primary" data-action="pluginEnable" data-args="' +
                esc(JSON.stringify([p.name])) + '">' + t('admin.plugins.action.start') + '</button>';
        }
        var safeSite = (p.website && /^https?:\/\//i.test(p.website)) ? p.website : '';
        var website = safeSite
            ? ' <a class="plugin-link" href="' + esc(safeSite) + '" target="_blank" rel="noopener">' + esc(safeSite) + '</a>'
            : '';
        html += '<div class="plugin-card">' +
            '<div class="plugin-card-top">' +
                '<img class="plugin-icon" src="/admin/api/plugins/' + encodeURIComponent(p.name) + '/icon" alt="">' +
                '<div class="plugin-title">' +
                    '<span class="plugin-name">' + esc(p.displayName) + '</span>' +
                    '<span class="plugin-internal">' + esc(p.name) + '</span>' +
                '</div>' +
                '<span class="plugin-version">v' + esc(p.version) + '</span>' +
            '</div>' +
            '<div class="plugin-meta">' + esc(t('admin.plugins.authors')) + ': ' + esc(authors) +
                ' · ' + esc(t('admin.plugins.apiVer')) + ': ' + esc(p.apiVer) + website + '</div>' +
            '<div class="plugin-desc">' + esc(p.description || '') + '</div>' +
            '<div class="plugin-status">' +
                '<span class="plugin-status-badge ' + statusClass + '">' + esc(pluginStateLabel(p.state)) + '</span>' +
                depBtn +
            '</div>' +
            (actions ? '<div class="plugin-actions">' + actions + '</div>' : '') +
            '</div>';
    });
    list.innerHTML = html;
}

function pluginHotReloadWarning() {
    showInfoDialog(t('admin.plugins.title'), t('admin.plugins.hotReloadWarning'));
}

async function pluginPost(url) {
    try {
        var res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
        });
        var text = await res.text();
        var data;
        try {
            data = JSON.parse(text);
        } catch (e) {
            data = { success: false, message: text || t('common.failed') };
        }
        showToast(data.message || (data.success ? t('common.success') : t('common.failed')), data.success ? 'success' : 'error');
        return data;
    } catch (err) {
        showToast(t('common.networkError'), 'error');
        return null;
    }
}

function pluginEnable(name) {
    var p = pluginData[name];
    if (p && !p.hotReloadable) { pluginHotReloadWarning(); return; }
    return pluginPost('/admin/api/plugins/' + encodeURIComponent(name) + '/enable').then(loadPlugins);
}

function pluginDisable(name) {
    var p = pluginData[name];
    if (p && !p.hotReloadable) { pluginHotReloadWarning(); return; }
    return pluginPost('/admin/api/plugins/' + encodeURIComponent(name) + '/disable').then(loadPlugins);
}

function pluginReload(name) {
    var p = pluginData[name];
    if (p && !p.hotReloadable) { pluginHotReloadWarning(); return; }
    return pluginPost('/admin/api/plugins/' + encodeURIComponent(name) + '/reload').then(loadPlugins);
}

function pluginShowDependencies(name) {
    var p = pluginData[name];
    var deps = (p && p.missingDependencies && p.missingDependencies.length)
        ? p.missingDependencies.join('\n')
        : t('admin.plugins.none');
    showInfoDialog(t('admin.plugins.dependencies'), deps);
}

(function () {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadPlugins);
    } else {
        loadPlugins();
    }
})();
