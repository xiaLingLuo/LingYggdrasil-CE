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
(async function loadAppInfo() {
    try {
        const res = await fetch('/admin/api/appinfo');
        if (res.status === 401) { window.location.href = '/admin/login'; return; }
        const data = await res.json();

        setText('appName', data.appName);
        setText('appVersion', data.appVersion);
        setLink('appRepo', data.repoUrl);
        setText('installedAt', formatDate(data.installedAt));
    } catch (err) {
        console.error('加载应用信息失败:', err);
    }
})();

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text || '-';
}

function setLink(id, url) {
    const el = document.getElementById(id);
    if (!el) return;
    if (!url) { el.textContent = '-'; return; }
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener';
    a.textContent = url;
    el.replaceChildren(a);
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    const locale = window.__LOCALE__ || 'zh-CN';
    return d.toLocaleDateString(locale, { year: 'numeric', month: 'long', day: 'numeric' }) +
        ' ' + d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });
}

const UPDATE_CACHE_KEY = 'ling-admin-update-check';
const UPDATE_CACHE_TTL = 30 * 60 * 1000;

function readUpdateCache() {
    try {
        const raw = localStorage.getItem(UPDATE_CACHE_KEY);
        if (!raw) return null;
        const data = JSON.parse(raw);
        if (!data || typeof data.ts !== 'number' || (Date.now() - data.ts) > UPDATE_CACHE_TTL) {
            localStorage.removeItem(UPDATE_CACHE_KEY);
            return null;
        }
        return data;
    } catch (err) {
        return null;
    }
}

function writeUpdateCache(data) {
    try {
        localStorage.setItem(UPDATE_CACHE_KEY, JSON.stringify({
            source: data.source,
            current: data.current,
            latest: data.latest,
            status: data.status,
            ts: Date.now()
        }));
    } catch (err) {
    }
}

function openUpdateModal() {
    const result = document.getElementById('updateResult');
    if (result) {
        const cached = readUpdateCache();
        if (cached) {
            result.replaceChildren(buildUpdateResult(cached));
        } else {
            result.textContent = t('admin.appinfo.updateNotChecked');
        }
    }
    document.getElementById('updateModal').style.display = 'flex';
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

async function checkUpdate(source) {
    const result = document.getElementById('updateResult');
    const btns = document.querySelectorAll('.update-source-btns button');
    const loading = document.getElementById('updateLoading');
    btns.forEach(function (btn) { btn.style.display = 'none'; });
    if (loading) loading.style.display = 'inline-flex';
    try {
        const res = await fetch('/admin/api/appinfo/check-update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify({ source: source })
        });
        const data = await res.json();
        if (res.status === 429) {
            showToast((data && data.message) || t('msg.tooFrequent'), 'error');
            return;
        }
        if (!result) return;
        if (data && data.success) {
            writeUpdateCache(data);
            result.replaceChildren(buildUpdateResult(data));
        } else {
            result.textContent = t('admin.appinfo.updateFailed', (data && data.message) || '');
        }
    } catch (err) {
        if (result) result.textContent = t('admin.appinfo.updateFailed', t('common.networkError'));
    } finally {
        if (loading) loading.style.display = 'none';
        btns.forEach(function (btn) { btn.style.display = ''; });
    }
}

function buildUpdateResult(data) {
    let message;
    if (data.status === 'outdated') {
        message = t('admin.appinfo.updateOutdated', [data.latest, data.current]);
    } else if (data.status === 'ahead') {
        message = t('admin.appinfo.updateAhead', [data.current, data.latest]);
    } else {
        message = t('admin.appinfo.updateUpToDate', [data.latest]);
    }

    const row = document.createElement('div');
    row.className = 'update-result-row';

    const text = document.createElement('div');
    text.className = 'update-result-text';
    text.textContent = message;
    row.appendChild(text);

    if (data.status === 'outdated') {
        const link = document.createElement('a');
        link.className = 'btn btn-primary';
        link.href = 'https://github.com/xiaLingLuo/LingYggdrasil-CE/releases/latest';
        link.target = '_blank';
        link.rel = 'noopener';
        link.textContent = t('admin.appinfo.updateGoDownload');
        row.appendChild(link);
    }

    const fragment = document.createDocumentFragment();
    fragment.appendChild(row);
    return fragment;
}
