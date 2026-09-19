(function () {
    'use strict';

    var DICT = window.__INSTALL_I18N__ || {};
    function lookup(key) {
        var node = DICT;
        var parts = key.split('.');
        for (var i = 0; i < parts.length; i++) {
            if (node == null || typeof node !== 'object') return null;
            node = node[parts[i]];
        }
        return typeof node === 'string' ? node : null;
    }
    function t(key) {
        var value = lookup(key);
        if (value == null) return key;
        for (var i = 1; i < arguments.length; i++) {
            value = value.split('{' + (i - 1) + '}').join(String(arguments[i]));
        }
        return value;
    }
    window.t = t;

    var SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">';
    var ICONS = {
        tree: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2 6.5 9.5h3L5 15.5h5V22h4v-6.5h5l-4.5-6h3L12 2Z"/></svg>',
        moon: SVG + '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"/></svg>',
        sun: SVG + '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>',
        database: SVG + '<ellipse cx="12" cy="5" rx="8" ry="3"/><path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5"/><path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/></svg>',
        envelope: SVG + '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/></svg>',
        user: SVG + '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/></svg>',
        rocket: SVG + '<path d="M12 2c3 2 5 6 5 10l-5 3-5-3c0-4 2-8 5-10Z"/><path d="M7 15l-2 5 5-2M17 15l2 5-5-2"/></svg>',
        check: SVG + '<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/></svg>',
        xmark: SVG + '<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6M9 9l6 6"/></svg>'
    };
    function icon(name) { return ICONS[name] || ''; }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme === 'dark' ? 'dark' : 'light');
        var btn = document.getElementById('themeToggle');
        if (btn) btn.innerHTML = icon(theme === 'dark' ? 'sun' : 'moon');
    }
    function toggleTheme() {
        var current = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        try { localStorage.setItem('ling-theme', next); } catch (e) {}
        applyTheme(next);
    }
    window.toggleTheme = toggleTheme;

    var toastTimer = null;
    function showToast(message, type) {
        var existing = document.getElementById('install-toast');
        if (existing) existing.remove();
        if (toastTimer) { clearTimeout(toastTimer); toastTimer = null; }
        var el = document.createElement('div');
        el.id = 'install-toast';
        el.className = 'toast ' + (type === 'error' ? 'toast-error' : 'toast-success');
        el.textContent = message;
        document.body.appendChild(el);
        toastTimer = setTimeout(function () { el.remove(); }, 3500);
    }
    window.showToast = showToast;

    function resolveAction(name) {
        if (!name) return null;
        if (typeof window[name] === 'function') return { fn: window[name], receiver: window };
        if (name.indexOf('.') > -1) {
            var parts = name.split('.');
            var receiver = window;
            for (var i = 0; i < parts.length - 1 && receiver; i++) receiver = receiver[parts[i]];
            if (receiver && typeof receiver[parts[parts.length - 1]] === 'function') {
                return { fn: receiver[parts[parts.length - 1]], receiver: receiver };
            }
        }
        return null;
    }
    function dispatchAction(e) {
        if (!e || !e.target || !e.target.closest) return;
        var el = e.target.closest('[data-action]');
        if (!el) return;
        var action = resolveAction(el.getAttribute('data-action'));
        if (!action) return;
        var args = [];
        if (el.hasAttribute('data-args')) {
            try { args = JSON.parse(el.getAttribute('data-args')); if (!Array.isArray(args)) args = []; }
            catch (err) { args = []; }
        }
        if (el.hasAttribute('data-this')) args.push(el);
        if (el.hasAttribute('data-event')) args.push(e);
        action.fn.apply(action.receiver, args);
    }

    window.InstallWizard = {
        currentStep: 1,
        totalSteps: 4,
        dbType: 'sqlite',
        installing: false,

        init() {
            var emailToggle = document.getElementById('emailEnabled');
            if (emailToggle) {
                emailToggle.addEventListener('change', function () {
                    var fields = document.getElementById('email-fields');
                    if (fields) fields.style.display = emailToggle.checked ? 'block' : 'none';
                });
            }
            this.updateButtons();
        },

        selectDbType(type) {
            if (type === 'pgsql') {
                showToast(t('install.pgsqlUnavailable'), 'error');
                return;
            }
            this.dbType = type;
            var typeInput = document.getElementById('dbType');
            if (typeInput) typeInput.value = type;

            document.querySelectorAll('.db-type-card').forEach(function (c) {
                c.classList.toggle('active', c.dataset.type === type);
            });

            var sqliteFields = document.getElementById('db-sqlite-fields');
            var serverFields = document.getElementById('db-server-fields');
            if (type === 'sqlite') {
                if (sqliteFields) sqliteFields.style.display = 'block';
                if (serverFields) serverFields.style.display = 'none';
            } else {
                if (sqliteFields) sqliteFields.style.display = 'none';
                if (serverFields) serverFields.style.display = 'block';
                var portInput = document.getElementById('dbPort');
                if (portInput) portInput.value = '3306';
            }
        },

        nextStep() {
            if (this.installing) return;
            if (!this.validateStep(this.currentStep)) return;
            if (this.currentStep === this.totalSteps) { this.submit(); return; }
            this.currentStep++;
            if (this.currentStep === this.totalSteps) this.buildSummary();
            this.showStep(this.currentStep);
        },

        prevStep() {
            if (this.installing) return;
            if (this.currentStep <= 1) return;
            this.currentStep--;
            this.showStep(this.currentStep);
        },

        showStep(step) {
            document.querySelectorAll('.step-content').forEach(function (el) { el.classList.remove('active'); });
            var current = document.getElementById('step-' + step);
            if (current) {
                current.classList.add('active');
                current.style.animation = 'none';
                void current.offsetHeight;
                current.style.animation = '';
            }
            this.updateStepIndicator();
            this.updateButtons();
        },

        updateStepIndicator() {
            var step = this.currentStep;
            document.querySelectorAll('.step-dot').forEach(function (dot) {
                var s = parseInt(dot.dataset.step, 10);
                dot.classList.remove('active', 'done');
                if (s === step) dot.classList.add('active');
                else if (s < step) dot.classList.add('done');
            });
            document.querySelectorAll('.step-line').forEach(function (line, i) {
                line.classList.toggle('done', (i + 1) < step);
            });
            document.querySelectorAll('.step-label').forEach(function (lbl) {
                var s = parseInt(lbl.dataset.step, 10);
                lbl.classList.remove('active', 'done');
                if (s === step) lbl.classList.add('active');
                else if (s < step) lbl.classList.add('done');
            });
        },

        updateButtons() {
            var prevBtn = document.getElementById('btn-prev');
            var nextBtn = document.getElementById('btn-next');
            if (prevBtn) prevBtn.style.visibility = this.currentStep > 1 ? 'visible' : 'hidden';
            if (nextBtn) {
                if (this.currentStep === this.totalSteps) {
                    nextBtn.innerHTML = icon('rocket') + ' ' + t('install.startInstall');
                } else {
                    nextBtn.textContent = t('install.next');
                }
            }
        },

        validateStep(step) {
            this.clearErrors();
            var valid = true;

            if (step === 1) {
                var username = this.val('rootUsername');
                var password = this.val('rootPassword');
                var confirm = this.val('rootPasswordConfirm');
                var email = this.val('rootEmail');
                if (!username) { this.markError('rootUsername', t('install.usernameRequired')); valid = false; }
                var PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{12,}$/;
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
                var enabled = document.getElementById('emailEnabled').checked;
                if (enabled) {
                    var host = this.val('emailHost');
                    var port = this.val('emailPort');
                    if (!host) { this.markError('emailHost', t('install.smtpHostRequired')); valid = false; }
                    if (!port || parseInt(port, 10) <= 0) { this.markError('emailPort', t('install.portInvalid')); valid = false; }
                }
            }

            if (step === 3 && this.dbType !== 'sqlite') {
                var dbHost = this.val('dbHost');
                var dbPort = this.val('dbPort');
                var dbName = this.val('dbName');
                if (!dbHost) { this.markError('dbHost', t('install.dbHostRequired')); valid = false; }
                if (!dbPort || parseInt(dbPort, 10) <= 0) { this.markError('dbPort', t('install.portInvalid')); valid = false; }
                if (!dbName) { this.markError('dbName', t('install.dbNameRequired')); valid = false; }
            }

            return valid;
        },

        buildSummary() {
            var container = document.getElementById('summary-content');
            if (!container) return;

            var emailEnabled = document.getElementById('emailEnabled').checked;
            var type = this.dbType;
            var typeLabel = { sqlite: 'SQLite', mysql: 'MySQL' }[type] || type;

            var dbRows;
            if (type === 'sqlite') {
                dbRows = this.summaryRow(t('install.summaryFilePath'), this.val('sqlitePath') || './data.db');
            } else {
                dbRows = this.summaryRow(t('install.summaryHost'), this.val('dbHost'))
                    + this.summaryRow(t('install.summaryPort'), this.val('dbPort'))
                    + this.summaryRow(t('install.summaryDbName'), this.val('dbName'))
                    + this.summaryRow(t('install.summaryUsername'), this.val('dbUsername') || t('install.summaryEmpty'));
            }

            var mailSection = '';
            if (emailEnabled) {
                mailSection = '<div class="summary-section">'
                    + '<div class="summary-section-title">' + icon('envelope') + ' ' + t('install.summaryEmailSection') + '</div>'
                    + this.summaryRow(t('install.smtpHost'), this.val('emailHost'))
                    + this.summaryRow(t('install.summaryPort'), this.val('emailPort'))
                    + this.summaryRow(t('install.summaryUsername'), this.val('emailUsername') || t('install.summaryEmpty'))
                    + this.summaryRow(t('install.summaryFrom'), this.val('emailFrom') || t('install.summaryEmpty'))
                    + '</div>';
            }

            container.innerHTML =
                '<div class="summary-section">'
                + '<div class="summary-section-title">' + icon('user') + ' ' + t('install.summaryAdminSection') + '</div>'
                + this.summaryRow(t('install.summaryUsername'), this.val('rootUsername'))
                + this.summaryRow(t('install.summaryEmail'), this.val('rootEmail'))
                + this.summaryRow(t('install.summaryPassword'), '\u2022\u2022\u2022\u2022\u2022\u2022')
                + '</div>'
                + mailSection
                + '<div class="summary-section">'
                + '<div class="summary-section-title">' + icon('database') + ' ' + t('install.summaryDbSection') + '</div>'
                + this.summaryRow(t('install.summaryType'), typeLabel)
                + dbRows
                + '</div>';
        },

        escapeHtml(str) {
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(str == null ? '' : String(str)));
            return div.innerHTML;
        },

        summaryRow(key, value) {
            return '<div class="summary-row">'
                + '<span class="summary-key">' + this.escapeHtml(key) + '</span>'
                + '<span class="summary-value">' + this.escapeHtml(value || '\u2014') + '</span>'
                + '</div>';
        },

        async submit() {
            if (this.installing) return;
            this.installing = true;

            document.querySelectorAll('.step-content').forEach(function (el) { el.classList.remove('active'); });
            var installingEl = document.getElementById('step-installing');
            var footerEl = document.getElementById('install-footer');
            if (installingEl) installingEl.style.display = 'block';
            if (footerEl) footerEl.style.display = 'none';

            this.setStatus(t('install.connecting'));

            try {
                var tokenEl = document.getElementById('install-token');
                var token = tokenEl ? tokenEl.value : '';
                var resp = await fetch('/api/install', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'X-Install-Token': token },
                    body: JSON.stringify(this.buildPayload())
                });
                var data = await resp.json();
                if (data.success) this.showResult(true, data.message || t('install.installSuccess'));
                else this.showResult(false, data.message || t('install.installFailed'));
            } catch (err) {
                this.showResult(false, t('install.networkError') + err.message);
            }
        },

        buildPayload() {
            var emailEnabled = document.getElementById('emailEnabled').checked;
            var type = this.dbType;
            var payload = {
                rootUsername: this.val('rootUsername'),
                rootPassword: this.val('rootPassword'),
                rootEmail: this.val('rootEmail'),
                emailEnabled: emailEnabled,
                dbType: type
            };
            if (emailEnabled) {
                payload.emailHost = this.val('emailHost');
                payload.emailPort = parseInt(this.val('emailPort'), 10) || 587;
                payload.emailUsername = this.val('emailUsername');
                payload.emailPassword = this.val('emailPassword');
                payload.emailFrom = this.val('emailFrom');
            }
            if (type === 'sqlite') {
                payload.sqlitePath = this.val('sqlitePath') || './data.db';
            } else {
                payload.dbHost = this.val('dbHost') || 'localhost';
                payload.dbPort = parseInt(this.val('dbPort'), 10) || 3306;
                payload.dbName = this.val('dbName') || 'yggdrasil';
                payload.dbUsername = this.val('dbUsername');
                payload.dbPassword = this.val('dbPassword');
            }
            return payload;
        },

        setStatus(msg) {
            var el = document.getElementById('installing-status');
            if (el) el.textContent = msg;
        },

        showResult(success, message) {
            this.installing = false;
            document.querySelectorAll('.step-content').forEach(function (el) {
                el.classList.remove('active');
                el.style.display = 'none';
            });

            var resultEl = document.getElementById('step-result');
            var resultView = document.getElementById('result-view');
            var footerEl = document.getElementById('install-footer');

            if (resultView) {
                if (success) {
                    resultView.innerHTML = '<div class="result-icon success">' + icon('check') + '</div>'
                        + '<h2 class="result-title success">' + t('install.resultSuccessTitle') + '</h2>'
                        + '<p class="result-message">' + this.escapeHtml(message) + '</p>';
                } else {
                    resultView.innerHTML = '<div class="result-icon error">' + icon('xmark') + '</div>'
                        + '<h2 class="result-title error">' + t('install.resultErrorTitle') + '</h2>'
                        + '<p class="result-message">' + this.escapeHtml(message) + '</p>'
                        + '<button class="btn btn-primary" id="backToFormBtn">' + t('install.backToForm') + '</button>';
                    var backBtn = document.getElementById('backToFormBtn');
                    if (backBtn) backBtn.addEventListener('click', function () { window.InstallWizard.backToForm(); });
                }
            }
            if (resultEl) {
                resultEl.style.display = 'block';
                resultEl.classList.add('active');
            }
            if (footerEl) footerEl.style.display = success ? 'none' : 'flex';
        },

        backToForm() {
            this.installing = false;
            document.querySelectorAll('.step-content').forEach(function (el) {
                el.style.display = '';
                el.classList.remove('active');
            });
            var footerEl = document.getElementById('install-footer');
            if (footerEl) footerEl.style.display = 'flex';
            this.showStep(this.currentStep);
        },

        val(id) {
            var el = document.getElementById(id);
            return el ? el.value.trim() : '';
        },

        markError(id, msg) {
            var el = document.getElementById(id);
            if (!el) return;
            el.classList.add('input-error');
            var err = document.createElement('span');
            err.className = 'form-input-error';
            err.textContent = msg;
            el.parentNode.appendChild(err);
        },

        clearErrors() {
            document.querySelectorAll('.input-error').forEach(function (el) { el.classList.remove('input-error'); });
            document.querySelectorAll('.form-input-error').forEach(function (el) { el.remove(); });
            var alertEl = document.getElementById('install-alert');
            if (alertEl) alertEl.style.display = 'none';
        }
    };

    function boot() {
        var stored = null;
        try { stored = localStorage.getItem('ling-theme'); } catch (e) {}
        var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyTheme(stored || (prefersDark ? 'dark' : 'light'));

        document.addEventListener('click', dispatchAction);
        document.addEventListener('change', dispatchAction);
        document.addEventListener('input', dispatchAction);

        window.InstallWizard.init();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
