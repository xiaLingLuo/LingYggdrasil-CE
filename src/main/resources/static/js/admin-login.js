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
document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('errorMsg');
    const btn = this.querySelector('.btn-login');

    errorMsg.style.display = 'none';
    btn.disabled = true;
    btn.textContent = '登录中...';

    try {
        const res = await fetch('/admin/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();

        if (data.success) {
            window.location.href = data.redirect || '/admin/dashboard';
        } else {
            errorMsg.textContent = data.message || '登录失败';
            errorMsg.style.display = 'block';
        }
    } catch (err) {
        errorMsg.textContent = '网络错误，请稍后重试';
        errorMsg.style.display = 'block';
    } finally {
        btn.disabled = false;
        btn.textContent = '登 录';
    }
});
