(function() {
    let codeTimer = null;

    const regForm = document.getElementById('registerForm');
    if (regForm) {
        regForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const username = document.getElementById('username').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const nicknameEl = document.getElementById('nickname');
            const nickname = nicknameEl ? nicknameEl.value.trim() : '';
            const verifyCodeEl = document.getElementById('verifyCode');
            const verifyCode = verifyCodeEl ? verifyCodeEl.value.trim() : '';
            const errorDiv = document.getElementById('registerError');
            if (errorDiv) errorDiv.style.display = 'none';

            if (username.length < 3 || username.length > 16) {
                showError(errorDiv, '用户名需要3-16个字符'); return;
            }
            if (!email.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
                showError(errorDiv, '邮箱格式不正确'); return;
            }
            const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            if (!PASSWORD_REGEX.test(password)) {
                showError(errorDiv, '密码至少12位，需包含大小写字母、数字和特殊字符(@$!%*?&)'); return;
            }

            const submitBtn = regForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = '注册中...';

            try {
                const body = { username, email, password, nickname };
                if (verifyCode) body.verifyCode = verifyCode;

                const resp = await fetch('/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify(body)
                });
                const data = await resp.json();

                if (data.success) {
                    showToast('注册成功！正在跳转...', 'success');
                    setTimeout(() => { window.location.href = data.redirect || '/login'; }, 1000);
                } else if (data.needVerify) {
                    showToast(data.message || '验证码已发送，请查收邮箱', 'success');
                    if (verifyCodeEl) verifyCodeEl.focus();
                    submitBtn.disabled = false;
                    submitBtn.textContent = '注册';
                } else {
                    showToast(data.message || '注册失败', 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = '注册';
                }
            } catch (err) {
                showToast('网络错误，请重试', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = '注册';
            }
        });
    }

    const verifyForm = document.getElementById('verifyForm');
    if (verifyForm) {
        const params = new URLSearchParams(window.location.search);
        const emailParam = params.get('email');
        if (emailParam) {
            document.getElementById('email').value = emailParam;
        }

        verifyForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const email = document.getElementById('email').value.trim();
            const code = document.getElementById('code').value.trim();
            const type = document.getElementById('verifyType').value;
            const errorDiv = document.getElementById('verifyError');
            if (errorDiv) errorDiv.style.display = 'none';

            if (!email || !code) {
                showError(errorDiv, '请填写邮箱和验证码'); return;
            }

            const submitBtn = verifyForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = '验证中...';

            try {
                const resp = await fetch('/api/verify-email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ email, code, type })
                });
                const data = await resp.json();

                if (data.success) {
                    showToast('验证成功！正在跳转...', 'success');
                    setTimeout(() => { window.location.href = data.redirect || '/login'; }, 1000);
                } else {
                    showToast(data.message || '验证失败', 'error');
                    submitBtn.disabled = false;
                    submitBtn.textContent = '验证';
                }
            } catch (err) {
                showToast('网络错误，请重试', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = '验证';
            }
        });
    }

    window.sendRegCode = async function() {
        const email = document.getElementById('email').value.trim();
        const btn = document.getElementById('sendCodeBtn');

        if (!email || !email.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
            showToast('请先输入正确的邮箱', 'error');
            return;
        }

        btn.disabled = true;
        try {
            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value;
            const nicknameEl = document.getElementById('nickname');

            if (!username || username.length < 3) {
                showToast('请先填写用户名', 'error');
                btn.disabled = false; return;
            }
            const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            if (!password || !PASSWORD_REGEX.test(password)) {
                showToast('请先填写密码（至少12位，含大小写字母、数字和特殊字符）', 'error');
                btn.disabled = false; return;
            }

            const body = { username, email, password, nickname: nicknameEl ? nicknameEl.value.trim() : '' };
            const resp = await fetch('/api/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify(body)
            });
            const data = await resp.json();

            if (data.needVerify) {
                showToast(data.message || '验证码发送成功', 'success');
                startCountdown(btn, 300);
            } else if (data.success) {
                window.location.href = data.redirect || '/login';
            } else {
                showToast(data.message || '发送失败', 'error');
                btn.disabled = false;
            }
        } catch (err) {
            showToast('网络错误', 'error');
            btn.disabled = false;
        }
    };

    window.resendCode = async function() {
        const email = document.getElementById('email').value.trim();
        if (!email) { showToast('请先输入邮箱地址', 'error'); return; }

        try {
            const resp = await fetch('/api/resend-code', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ email, type: 'registration' })
            });
            const data = await resp.json();
            showToast(data.message || (data.success ? '验证码已重新发送' : '发送失败'), data.success ? 'success' : 'error');
        } catch (err) {
            showToast('网络错误，请重试', 'error');
        }
    };

    function startCountdown(btn, seconds) {
        let remaining = seconds;
        btn.textContent = remaining + 's';
        if (codeTimer) clearInterval(codeTimer);
        codeTimer = setInterval(() => {
            remaining--;
            btn.textContent = remaining + 's';
            if (remaining <= 0) {
                clearInterval(codeTimer);
                btn.textContent = '发送验证码';
                btn.disabled = false;
            }
        }, 1000);
    }

    function showToast(message, type) {
        var toast = document.getElementById('toast');
        if (toast) {
            toast.textContent = message;
            toast.className = 'toast toast-' + (type || 'info');
            toast.style.display = 'block';
            setTimeout(function() { toast.style.display = 'none'; }, 3000);
        }
    }
    function showError(div, msg) {
        div.innerHTML = '<i class="fas fa-circle-xmark"></i> ' + msg;
        div.style.display = 'flex';
    }

})();
