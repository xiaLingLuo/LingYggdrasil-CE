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

function downloadLog() {
    fetch('/logs/download').then(function (res) {
        if (res.ok) {
            return res.blob().then(function (blob) {
                var url = URL.createObjectURL(blob);
                var a = document.createElement('a');
                a.href = url;
                a.download = 'user-log.txt';
                document.body.appendChild(a);
                a.click();
                a.remove();
                URL.revokeObjectURL(url);
            });
        }
        return res.json().then(function (data) {
            showToast((data && data.message) || t('common.failed'), 'error');
        }).catch(function () {
            showToast(t('common.failed'), 'error');
        });
    }).catch(function () {
        showToast(t('common.networkError'), 'error');
    });
}

function clearLog() {
    showConfirmDialog(t('user.logs.clearConfirm'), async function () {
        try {
            var res = await fetch('/logs/clear', {
                method: 'POST',
                headers: { 'X-CSRF-Token': (window.CSRF_TOKEN || '') }
            });
            var data = await res.json();
            showToast(data.message || (data.success ? t('user.logs.cleared') : t('common.failed')),
                data.success ? 'success' : 'error');
            if (data.success) location.reload();
        } catch (err) {
            showToast(t('common.networkError'), 'error');
        }
    });
}
