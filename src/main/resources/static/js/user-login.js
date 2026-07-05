(function() {
    const form = document.getElementById('loginForm');
    if (!form) return;

    const savedUser = localStorage.getItem('ling_username');
    if (savedUser) {
        document.getElementById('username').value = savedUser;
    }

    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        const errorDiv = document.getElementById('loginError');

        if (!username || !password) {
            showError(errorDiv, '请填写用户名和密码');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        submitBtn.disabled = true;
        submitBtn.textContent = '登录中...';

        try {
            const resp = await fetch('/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ username, password })
            });
            const data = await resp.json();

            if (data.success) {
                localStorage.setItem('ling_username', username);
                window.location.href = data.redirect || '/dashboard';
            } else {
                showError(errorDiv, data.message || '登录失败');
                submitBtn.disabled = false;
                submitBtn.textContent = '登录';
            }
        } catch (err) {
            showError(errorDiv, '网络错误，请重试');
            submitBtn.disabled = false;
            submitBtn.textContent = '登录';
        }
    });

    function showError(div, msg) {
        div.innerHTML = '<i class="fas fa-circle-xmark"></i> ' + msg;
        div.style.display = 'flex';
        div.style.animation = 'none';
        void div.offsetWidth;
        div.style.animation = 'slideDown 0.3s ease';
    }
})();
