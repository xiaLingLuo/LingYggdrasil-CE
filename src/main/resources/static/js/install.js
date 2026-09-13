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
window.InstallWizard = {
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
                nextBtn.innerHTML = '<i class="fas fa-rocket"></i> ' + t('install.startInstall');
            } else {
                nextBtn.textContent = t('install.next');
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
                this.markError('rootUsername', t('install.usernameRequired'));
                valid = false;
            }
            const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
            if (!password || !PASSWORD_REGEX.test(password)) {
                this.markError('rootPassword', t('install.passwordWeak'));
                valid = false;
            } else if (password !== confirm) {
                this.markError('rootPasswordConfirm', t('install.passwordMismatch'));
                valid = false;
            }
            if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                this.markError('rootEmail', t('install.emailInvalid'));
                valid = false;
            }
        }

        if (step === 2) {
            const enabled = document.getElementById('emailEnabled').checked;
            if (enabled) {
                const host = this.val('emailHost');
                const port = this.val('emailPort');
                if (!host) { this.markError('emailHost', t('install.smtpHostRequired')); valid = false; }
                if (!port || parseInt(port) <= 0) { this.markError('emailPort', t('install.portInvalid')); valid = false; }
            }
        }

        if (step === 3) {
            const type = this.dbType;
            if (type !== 'sqlite') {
                const host = this.val('dbHost');
                const port = this.val('dbPort');
                const name = this.val('dbName');
                if (!host) { this.markError('dbHost', t('install.dbHostRequired')); valid = false; }
                if (!port || parseInt(port) <= 0) { this.markError('dbPort', t('install.portInvalid')); valid = false; }
                if (!name) { this.markError('dbName', t('install.dbNameRequired')); valid = false; }
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
            dbRows = this.summaryRow(t('install.summaryFilePath'), this.val('sqlitePath') || './data.db');
        } else {
            dbRows = this.summaryRow(t('install.summaryHost'), this.val('dbHost'))
                   + this.summaryRow(t('install.summaryPort'), this.val('dbPort'))
                   + this.summaryRow(t('install.summaryDbName'), this.val('dbName'))
                   + this.summaryRow(t('install.summaryUsername'), this.val('dbUsername') || t('install.summaryEmpty'));
        }

        let mailSection = '';
        if (emailEnabled) {
            mailSection = `
                <div class="summary-section">
                    <div class="summary-section-title"><i class="fas fa-envelope"></i> ${t('install.summaryEmailSection')}</div>
                    ${this.summaryRow(t('install.smtpHost'), this.val('emailHost'))}
                    ${this.summaryRow(t('install.summaryPort'), this.val('emailPort'))}
                    ${this.summaryRow(t('install.summaryUsername'), this.val('emailUsername') || t('install.summaryEmpty'))}
                    ${this.summaryRow(t('install.summaryFrom'), this.val('emailFrom') || t('install.summaryEmpty'))}
                </div>
            `;
        }

        container.innerHTML = `
            <div class="summary-section">
                <div class="summary-section-title"><i class="fas fa-user"></i> ${t('install.summaryAdminSection')}</div>
                ${this.summaryRow(t('install.summaryUsername'), this.val('rootUsername'))}
                ${this.summaryRow(t('install.summaryEmail'), this.val('rootEmail'))}
                ${this.summaryRow(t('install.summaryPassword'), '••••••')}
            </div>
            ${mailSection}
            <div class="summary-section">
                <div class="summary-section-title"><i class="fas fa-database"></i> ${t('install.summaryDbSection')}</div>
                ${this.summaryRow(t('install.summaryType'), typeLabel)}
                ${dbRows}
            </div>
        `;
    },

    escapeHtml(str) {
        const div = document.createElement('div');
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    },

    summaryRow(key, value) {
        return `<div class="summary-row">
            <span class="summary-key">${this.escapeHtml(key)}</span>
            <span class="summary-value">${this.escapeHtml(value || '—')}</span>
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

        this.setStatus(t('install.connecting'));

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
                this.showResult(true, data.message || t('install.installSuccess'));
            } else {
                this.showResult(false, data.message || t('install.installFailed'));
            }
        } catch (err) {
            this.showResult(false, t('install.networkError') + err.message);
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
                    <h2 class="result-title success">${t('install.resultSuccessTitle')}</h2>
                    <p class="result-message">${this.escapeHtml(message)}</p>
                `;
            } else {
                resultView.innerHTML = `
                    <div class="result-icon error"><i class="fas fa-circle-xmark"></i></div>
                    <h2 class="result-title error">${t('install.resultErrorTitle')}</h2>
                    <p class="result-message">${this.escapeHtml(message)}</p>
                    <button class="btn btn-primary" id="backToFormBtn">${t('install.backToForm')}</button>
                `;
                const backBtn = document.getElementById('backToFormBtn');
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
