(function() {
    let codeTimers = {};

    const nicknameForm = document.getElementById('nicknameForm');
    if (nicknameForm) {
        nicknameForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const nickname = document.getElementById('newNickname').value.trim();
            const msgDiv = document.getElementById('nicknameMsg');

            if (!nickname) { showMsg(msgDiv, '请输入昵称', false); return; }

            try {
                const resp = await fetch('/api/settings/nickname', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ nickname })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    document.getElementById('newNickname').value = '';
                    setTimeout(() => location.reload(), 1000);
                }
            } catch (err) {
                showMsg(msgDiv, '网络错误', false);
            }
        });
    }

    const emailForm = document.getElementById('emailForm');
    if (emailForm) {
        emailForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const newEmail = document.getElementById('newEmail').value.trim();
            const password = document.getElementById('emailPassword').value;
            const verifyCode = document.getElementById('emailVerifyCode').value.trim();
            const msgDiv = document.getElementById('emailMsg');

            if (!newEmail || !newEmail.match(/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/)) {
                showMsg(msgDiv, '请输入正确的邮箱', false); return;
            }
            if (!password) { showMsg(msgDiv, '请输入当前密码', false); return; }
            if (!verifyCode) { showMsg(msgDiv, '请输入验证码', false); return; }

            try {
                const resp = await fetch('/api/settings/email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ newEmail, password, verifyCode })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    setTimeout(() => location.reload(), 1000);
                }
            } catch (err) {
                showMsg(msgDiv, '网络错误', false);
            }
        });
    }

    const passwordForm = document.getElementById('passwordForm');
    if (passwordForm) {
        passwordForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const currentPassword = document.getElementById('currentPassword').value;
            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const verifyCode = document.getElementById('passVerifyCode').value.trim();
            const msgDiv = document.getElementById('passwordMsg');

            if (!currentPassword) { showMsg(msgDiv, '请输入当前密码', false); return; }
            const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            if (!newPassword || !PASSWORD_REGEX.test(newPassword)) { showMsg(msgDiv, '新密码至少12位，需包含大小写字母、数字和特殊字符(@$!%*?&)', false); return; }
            if (newPassword !== confirmPassword) { showMsg(msgDiv, '两次输入的密码不一致', false); return; }
            if (!verifyCode) { showMsg(msgDiv, '请输入验证码', false); return; }

            try {
                const resp = await fetch('/api/settings/password', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                    body: JSON.stringify({ currentPassword, newPassword, verifyCode })
                });
                const data = await resp.json();
                showMsg(msgDiv, data.message, data.success);
                if (data.success) {
                    document.getElementById('currentPassword').value = '';
                    document.getElementById('newPassword').value = '';
                    document.getElementById('confirmPassword').value = '';
                }
            } catch (err) {
                showMsg(msgDiv, '网络错误', false);
            }
        });
    }

    window.sendSettingsCode = async function(type) {
        const btnId = type === 'email_change' ? 'sendEmailCodeBtn' : 'sendPassCodeBtn';
        const btn = document.getElementById(btnId);
        if (!btn || btn.disabled) return;

        btn.disabled = true;
        btn.textContent = '发送中...';

        try {
            const resp = await fetch('/api/settings/send-verify-code', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': (window.CSRF_TOKEN || '') },
                body: JSON.stringify({ type })
            });
            const data = await resp.json();

            if (data.success) {
                btn.textContent = '已发送';
                startCountdown(btnId, 300);
            } else {
                showToast(data.message || '发送失败', 'error');
                btn.disabled = false;
                btn.textContent = '发送验证码';
            }
        } catch (err) {
            showToast('网络错误', 'error');
            btn.disabled = false;
            btn.textContent = '发送验证码';
        }
    };

    function startCountdown(btnId, seconds) {
        const btn = document.getElementById(btnId);
        let remaining = seconds;
        if (codeTimers[btnId]) clearInterval(codeTimers[btnId]);
        codeTimers[btnId] = setInterval(() => {
            remaining--;
            btn.textContent = remaining + 's';
            if (remaining <= 0) {
                clearInterval(codeTimers[btnId]);
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
    function showMsg(div, msg, success) {
        div.innerHTML = (success ? '<i class="fas fa-circle-check"></i> ' : '<i class="fas fa-circle-xmark"></i> ') + msg;
        div.className = 'msg-area ' + (success ? 'success' : 'error');
    }
})();
