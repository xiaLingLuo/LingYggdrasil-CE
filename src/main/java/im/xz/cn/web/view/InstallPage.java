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

package im.xz.cn.web.view;

import im.xz.cn.i18n.I18n;
import im.xz.cn.web.PageRenderer;

public class InstallPage {

    public static String generateInstallPageContent(String token) {
        return PageRenderer.tr("""
            <input type="hidden" id="install-token" value="%s">
            <button type="button" class="theme-toggle install-theme-toggle" data-action="toggleTheme" aria-label="{{nav.theme}}" title="{{nav.theme}}"><i class="fas fa-moon"></i></button>
            <div class="install-wrapper">
                <div class="install-header">
                    <div class="install-logo"><img src="/icons/app.ico" class="logo-icon" alt="LingYggdrasil"></div>
                    <h1 class="install-title">{{install.title}}</h1>
                    <p class="install-subtitle">{{install.subtitle}}</p>
                </div>

                <div class="step-indicator">
                    <div class="step-dot active" data-step="1"><span>1</span></div>
                    <div class="step-line"></div>
                    <div class="step-dot" data-step="2"><span>2</span></div>
                    <div class="step-line"></div>
                    <div class="step-dot" data-step="3"><span>3</span></div>
                    <div class="step-line"></div>
                    <div class="step-dot" data-step="4"><span>4</span></div>
                </div>
                <div class="step-labels">
                    <span class="step-label active" data-step="1">{{install.stepAdmin}}</span>
                    <span class="step-label" data-step="2">{{install.stepEmail}}</span>
                    <span class="step-label" data-step="3">{{install.stepDb}}</span>
                    <span class="step-label" data-step="4">{{install.stepConfirm}}</span>
                </div>

                <div class="install-card">
                    <div id="install-alert" class="install-alert" style="display:none;"></div>

                    <div class="step-content active" id="step-1">
                        <h2 class="step-title">{{install.adminTitle}}</h2>
                        <p class="step-desc">{{install.adminDesc}}</p>
                        <div class="form-group">
                            <label class="form-label">{{install.username}} <span class="req">*</span></label>
                            <input type="text" id="rootUsername" class="form-input" placeholder="{{install.usernamePlaceholder}}" autocomplete="off">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{install.password}} <span class="req">*</span></label>
                            <input type="password" id="rootPassword" class="form-input" placeholder="{{install.passwordPlaceholder}}" autocomplete="new-password">
                            <div class="form-hint">{{install.passwordHint}}<span>@</span><span>$</span><span>!</span><span>%%</span><span>*</span><span>?</span><span>&amp;</span></div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{install.confirmPassword}} <span class="req">*</span></label>
                            <input type="password" id="rootPasswordConfirm" class="form-input" placeholder="{{install.confirmPasswordPlaceholder}}" autocomplete="new-password">
                        </div>
                        <div class="form-group">
                            <label class="form-label">{{install.email}} <span class="req">*</span></label>
                            <input type="email" id="rootEmail" class="form-input" placeholder="admin@example.com" autocomplete="off">
                        </div>
                    </div>

                    <div class="step-content" id="step-2">
                        <h2 class="step-title">{{install.emailTitle}}</h2>
                        <p class="step-desc">{{install.emailDesc}}</p>
                        <div class="form-group toggle-group">
                            <label class="form-label">{{install.enableEmail}}</label>
                            <label class="toggle-switch">
                                <input type="checkbox" id="emailEnabled">
                                <span class="toggle-slider"></span>
                            </label>
                        </div>
                        <div id="email-fields" class="email-fields" style="display:none;">
                            <div class="form-row">
                                <div class="form-group flex-2">
                                    <label class="form-label">{{install.smtpHost}}</label>
                                    <input type="text" id="emailHost" class="form-input" placeholder="smtp.example.com">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">{{install.port}}</label>
                                    <input type="number" id="emailPort" class="form-input" placeholder="587" value="587">
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">{{install.smtpUsername}}</label>
                                <input type="text" id="emailUsername" class="form-input" placeholder="{{install.smtpUsernamePlaceholder}}">
                            </div>
                            <div class="form-group">
                                <label class="form-label">{{install.smtpPassword}}</label>
                                <input type="password" id="emailPassword" class="form-input" placeholder="{{install.smtpPasswordPlaceholder}}">
                            </div>
                            <div class="form-group">
                                <label class="form-label">{{install.from}}</label>
                                <input type="email" id="emailFrom" class="form-input" placeholder="noreply@example.com">
                            </div>
                        </div>
                    </div>

                    <div class="step-content" id="step-3">
                        <h2 class="step-title">{{install.dbTitle}}</h2>
                        <p class="step-desc">{{install.dbDesc}}</p>
                        <div class="db-type-selector">
                            <div class="db-type-card active" data-type="sqlite" data-action="InstallWizard.selectDbType" data-args='["sqlite"]'>
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">SQLite</div>
                                <div class="db-desc">{{install.sqliteDesc}}</div>
                            </div>
                            <div class="db-type-card" data-type="mysql" data-action="InstallWizard.selectDbType" data-args='["mysql"]'>
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">MySQL</div>
                                <div class="db-desc">{{install.mysqlDesc}}</div>
                            </div>
                            <div class="db-type-card db-type-disabled" data-type="pgsql" data-disabled="true" data-action="InstallWizard.selectDbType" data-args='["pgsql"]'>
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">PostgreSQL <span class="db-badge">{{install.pgsqlUnavailableBadge}}</span></div>
                                <div class="db-desc">{{install.pgsqlUnavailable}}</div>
                            </div>
                        </div>
                        <input type="hidden" id="dbType" value="sqlite">
                        <div id="db-sqlite-fields" class="db-fields">
                            <div class="form-group">
                                <label class="form-label">{{install.sqlitePath}}</label>
                                <input type="text" id="sqlitePath" class="form-input" value="./data.db" placeholder="./data.db">
                            </div>
                        </div>
                        <div id="db-server-fields" class="db-fields" style="display:none;">
                            <div class="form-row">
                                <div class="form-group flex-2">
                                    <label class="form-label">{{install.host}}</label>
                                    <input type="text" id="dbHost" class="form-input" value="localhost" placeholder="localhost">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">{{install.port}}</label>
                                    <input type="number" id="dbPort" class="form-input" value="3306" placeholder="3306">
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">{{install.dbName}}</label>
                                <input type="text" id="dbName" class="form-input" value="yggdrasil" placeholder="yggdrasil">
                            </div>
                            <div class="form-row">
                                <div class="form-group flex-1">
                                    <label class="form-label">{{install.dbUsername}}</label>
                                    <input type="text" id="dbUsername" class="form-input" placeholder="root">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">{{install.dbPassword}}</label>
                                    <input type="password" id="dbPassword" class="form-input" placeholder="">
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="step-content" id="step-4">
                        <h2 class="step-title">{{install.confirmTitle}}</h2>
                        <p class="step-desc">{{install.confirmDesc}}</p>
                        <div id="summary-content" class="summary-card">
                        </div>
                    </div>

                    <div class="step-content" id="step-installing" style="display:none;">
                        <div class="installing-view">
                            <div class="installing-spinner"></div>
                            <h2 class="installing-title">{{install.installing}}</h2>
                            <p class="installing-desc" id="installing-status">{{install.initializing}}</p>
                        </div>
                    </div>

                    <div class="step-content" id="step-result" style="display:none;">
                        <div class="result-view" id="result-view">
                        </div>
                    </div>

                    <div class="install-footer" id="install-footer">
                        <button class="btn btn-secondary" id="btn-prev" data-action="InstallWizard.prevStep" style="visibility:hidden;">
                            {{install.prev}}
                        </button>
                        <button class="btn btn-primary" id="btn-next" data-action="InstallWizard.nextStep">
                            {{install.next}}
                        </button>
                    </div>
                </div>
            </div>
            <script src="/js/install.js"></script>
            """.formatted(token));
    }
}
