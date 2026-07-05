const InstallWizard = {
    currentStep: 1,
    totalSteps: 4,
    dbType: 'sqlite',
    installing: false,

    init() {
        const emailToggle = document.getElementById('emailEnabled');
        if (emailToggle) {
            emailToggle.addEventListener('change', () => {
                const fields = document.getElementById('email-fields');
                if (fields) {
                    fields.style.display = emailToggle.checked ? 'block' : 'none';
                }
            });
        }
        this.updateButtons();
    },

    selectDbType(type) {
        this.dbType = type;
        document.getElementById('dbType').value = type;

        document.querySelectorAll('.db-type-card').forEach(c => {
            c.classList.toggle('active', c.dataset.type === type);
        });

        const sqliteFields  = document.getElementById('db-sqlite-fields');
        const serverFields  = document.getElementById('db-server-fields');

        if (type === 'sqlite') {
            if (sqliteFields) sqliteFields.style.display = 'block';
            if (serverFields) serverFields.style.display = 'none';
        } else {
            if (sqliteFields) sqliteFields.style.display = 'none';
            if (serverFields) serverFields.style.display = 'block';
            const portInput = document.getElementById('dbPort');
            if (portInput) {
                portInput.value = type === 'mysql' ? '3306' : '5432';
            }
        }
    },

    nextStep() {
        if (this.installing) return;

        if (!this.validateStep(this.currentStep)) return;

        if (this.currentStep === this.totalSteps) {
            this.submit();
            return;
        }

        this.currentStep++;
        if (this.currentStep === this.totalSteps) {
            this.buildSummary();
        }
        this.showStep(this.currentStep);
    },

    prevStep() {
        if (this.installing) return;
        if (this.currentStep <= 1) return;
        this.currentStep--;
        this.showStep(this.currentStep);
    },

    showStep(step) {
        document.querySelectorAll('.step-content').forEach(el => {
            el.classList.remove('active');
        });
        const current = document.getElementById('step-' + step);
        if (current) {
            current.classList.add('active');
            current.style.animation = 'none';
            current.offsetHeight;
            current.style.animation = '';
        }
        this.updateStepIndicator();
        this.updateButtons();
    },

    updateStepIndicator() {
        document.querySelectorAll('.step-dot').forEach(dot => {
            const s = parseInt(dot.dataset.step);
            dot.classList.remove('active', 'done');
            if (s === this.currentStep) dot.classList.add('active');
            else if (s < this.currentStep) dot.classList.add('done');
        });
        document.querySelectorAll('.step-line').forEach((line, i) => {
            line.classList.toggle('done', (i + 1) < this.currentStep);
        });
        document.querySelectorAll('.step-label').forEach(lbl => {
            const s = parseInt(lbl.dataset.step);
            lbl.classList.remove('active', 'done');
            if (s === this.currentStep) lbl.classList.add('active');
            else if (s < this.currentStep) lbl.classList.add('done');
        });
    },

    updateButtons() {
        const prevBtn = document.getElementById('btn-prev');
        const nextBtn = document.getElementById('btn-next');
        if (prevBtn) prevBtn.style.visibility = this.currentStep > 1 ? 'visible' : 'hidden';
        if (nextBtn) {
            if (this.currentStep === this.totalSteps) {
                nextBtn.innerHTML = '<i class="fas fa-rocket"></i> 开始安装';
            } else {
                nextBtn.textContent = '下一步 →';
            }
        }
    },

    validateStep(step) {
        this.clearErrors();
        let valid = true;

        if (step === 1) {
            const username = this.val('rootUsername');
            const password = this.val('rootPassword');
            const confirm  = this.val('rootPasswordConfirm');
            const email    = this.val('rootEmail');

            if (!username) {
                this.markError('rootUsername', '用户名不能为空');
                valid = false;
            }
            const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            if (!password || !PASSWORD_REGEX.test(password)) {
                this.markError('rootPassword', '密码至少 12 位，需包含大写字母、小写字母、数字，以及特殊字符（仅允许 @ $ ! % * ? &）');
                valid = false;
            } else if (password !== confirm) {
                this.markError('rootPasswordConfirm', '两次输入的密码不一致');
                valid = false;
            }
            if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                this.markError('rootEmail', '请输入有效的邮箱地址');
                valid = false;
            }
        }

        if (step === 2) {
            const enabled = document.getElementById('emailEnabled').checked;
            if (enabled) {
                const host = this.val('emailHost');
                const port = this.val('emailPort');
                if (!host) { this.markError('emailHost', '请输入 SMTP 主机'); valid = false; }
                if (!port || parseInt(port) <= 0) { this.markError('emailPort', '请输入有效端口'); valid = false; }
            }
        }

        if (step === 3) {
            const type = this.dbType;
            if (type !== 'sqlite') {
                const host = this.val('dbHost');
                const port = this.val('dbPort');
                const name = this.val('dbName');
                if (!host) { this.markError('dbHost', '请输入数据库主机'); valid = false; }
                if (!port || parseInt(port) <= 0) { this.markError('dbPort', '请输入有效端口'); valid = false; }
                if (!name) { this.markError('dbName', '请输入数据库名'); valid = false; }
            }
        }

        return valid;
    },

    buildSummary() {
        const container = document.getElementById('summary-content');
        if (!container) return;

        const emailEnabled = document.getElementById('emailEnabled').checked;
        const type = this.dbType;
        const typeLabel = { sqlite: 'SQLite', mysql: 'MySQL', pgsql: 'PostgreSQL' }[type] || type;

        let dbRows = '';
        if (type === 'sqlite') {
            dbRows = this.summaryRow('文件路径', this.val('sqlitePath') || './data.db');
        } else {
            dbRows = this.summaryRow('主机', this.val('dbHost'))
                   + this.summaryRow('端口', this.val('dbPort'))
                   + this.summaryRow('数据库名', this.val('dbName'))
                   + this.summaryRow('用户名', this.val('dbUsername') || '(空)');
        }

        let mailSection = '';
        if (emailEnabled) {
            mailSection = `
                <div class="summary-section">
                    <div class="summary-section-title"><i class="fas fa-envelope"></i> 邮箱配置</div>
                    ${this.summaryRow('SMTP 主机', this.val('emailHost'))}
                    ${this.summaryRow('端口', this.val('emailPort'))}
                    ${this.summaryRow('用户名', this.val('emailUsername') || '(空)')}
                    ${this.summaryRow('发件人', this.val('emailFrom') || '(空)')}
                </div>
            `;
        }

        container.innerHTML = `
            <div class="summary-section">
                <div class="summary-section-title"><i class="fas fa-user"></i> 管理员账户</div>
                ${this.summaryRow('用户名', this.val('rootUsername'))}
                ${this.summaryRow('邮箱', this.val('rootEmail'))}
                ${this.summaryRow('密码', '••••••')}
            </div>
            ${mailSection}
            <div class="summary-section">
                <div class="summary-section-title"><i class="fas fa-database"></i> 数据库配置</div>
                ${this.summaryRow('类型', typeLabel)}
                ${dbRows}
            </div>
        `;
    },

    summaryRow(key, value) {
        return `<div class="summary-row">
            <span class="summary-key">${key}</span>
            <span class="summary-value">${value || '—'}</span>
        </div>`;
    },

    async submit() {
        if (this.installing) return;
        this.installing = true;

        document.querySelectorAll('.step-content').forEach(el => el.classList.remove('active'));
        const installingEl = document.getElementById('step-installing');
        const footerEl = document.getElementById('install-footer');
        if (installingEl) installingEl.style.display = 'block';
        if (footerEl) footerEl.style.display = 'none';

        this.setStatus('正在连接服务器...');

        const payload = this.buildPayload();

        try {
            const token = document.getElementById('install-token')?.value || '';
            const resp = await fetch('/api/install', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'X-Install-Token': token },
                body: JSON.stringify(payload)
            });

            const data = await resp.json();

            if (data.success) {
                this.showResult(true, data.message || '安装成功！');
            } else {
                this.showResult(false, data.message || '安装失败，请检查配置后重试。');
            }
        } catch (err) {
            this.showResult(false, '网络请求失败: ' + err.message);
        }
    },

    buildPayload() {
        const emailEnabled = document.getElementById('emailEnabled').checked;
        const type = this.dbType;
        const payload = {
            rootUsername: this.val('rootUsername'),
            rootPassword: this.val('rootPassword'),
            rootEmail:    this.val('rootEmail'),
            emailEnabled: emailEnabled,
            dbType:       type
        };

        if (emailEnabled) {
            payload.emailHost     = this.val('emailHost');
            payload.emailPort     = parseInt(this.val('emailPort')) || 587;
            payload.emailUsername = this.val('emailUsername');
            payload.emailPassword = this.val('emailPassword');
            payload.emailFrom     = this.val('emailFrom');
        }

        if (type === 'sqlite') {
            payload.sqlitePath = this.val('sqlitePath') || './data.db';
        } else {
            payload.dbHost     = this.val('dbHost') || 'localhost';
            payload.dbPort     = parseInt(this.val('dbPort')) || (type === 'mysql' ? 3306 : 5432);
            payload.dbName     = this.val('dbName') || 'yggdrasil';
            payload.dbUsername = this.val('dbUsername');
            payload.dbPassword = this.val('dbPassword');
        }

        return payload;
    },

    setStatus(msg) {
        const el = document.getElementById('installing-status');
        if (el) el.textContent = msg;
    },

    showResult(success, message) {
        this.installing = false;
        document.querySelectorAll('.step-content').forEach(el => {
            el.classList.remove('active');
            el.style.display = 'none';
        });

        const resultEl   = document.getElementById('step-result');
        const resultView = document.getElementById('result-view');
        const footerEl   = document.getElementById('install-footer');

        if (resultView) {
            if (success) {
                resultView.innerHTML = `
                    <div class="result-icon success"><i class="fas fa-circle-check"></i></div>
                    <h2 class="result-title success">安装完成</h2>
                    <p class="result-message">${message}</p>
                `;
            } else {
                resultView.innerHTML = `
                    <div class="result-icon error"><i class="fas fa-circle-xmark"></i></div>
                    <h2 class="result-title error">安装失败</h2>
                    <p class="result-message">${message}</p>
                    <button class="btn btn-primary" id="backToFormBtn">返回修改</button>
                `;
                var backBtn = document.getElementById('backToFormBtn');
                if (backBtn) {
                    backBtn.addEventListener('click', function() { InstallWizard.backToForm(); });
                }
            }
        }
        if (resultEl) {
            resultEl.style.display = 'block';
            resultEl.classList.add('active');
        }
        if (footerEl) footerEl.style.display = success ? 'none' : 'flex';
        if (success && footerEl) footerEl.style.display = 'none';
    },

    backToForm() {
        this.installing = false;
        document.querySelectorAll('.step-content').forEach(el => {
            el.style.display = '';
            el.classList.remove('active');
        });
        const footerEl = document.getElementById('install-footer');
        if (footerEl) footerEl.style.display = 'flex';
        this.showStep(this.currentStep);
    },

    val(id) {
        const el = document.getElementById(id);
        return el ? el.value.trim() : '';
    },

    markError(id, msg) {
        const el = document.getElementById(id);
        if (!el) return;
        el.classList.add('input-error');
        const err = document.createElement('span');
        err.className = 'form-input-error';
        err.textContent = msg;
        el.parentNode.appendChild(err);
    },

    clearErrors() {
        document.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
        document.querySelectorAll('.form-input-error').forEach(el => el.remove());
        const alertEl = document.getElementById('install-alert');
        if (alertEl) alertEl.style.display = 'none';
    }
};

document.addEventListener('DOMContentLoaded', () => {
    InstallWizard.init();
});
