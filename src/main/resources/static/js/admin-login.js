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
