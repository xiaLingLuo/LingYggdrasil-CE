package im.xz.cn.something.web;

public class InstallPage {

    public static String generateInstallPageContent(String token) {
        return """
            <input type="hidden" id="install-token" value="%s">
            <div class="install-wrapper">
                <div class="install-header">
                    <div class="install-logo">✿</div>
                    <h1 class="install-title">泠 Yggdrasil 安装向导</h1>
                    <p class="install-subtitle">首次使用配置 · First Time Setup</p>
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
                    <span class="step-label active" data-step="1">管理员账户</span>
                    <span class="step-label" data-step="2">邮箱配置</span>
                    <span class="step-label" data-step="3">数据库配置</span>
                    <span class="step-label" data-step="4">确认安装</span>
                </div>

                <div class="install-card">
                    <div id="install-alert" class="install-alert" style="display:none;"></div>

                    <div class="step-content active" id="step-1">
                        <h2 class="step-title">创建 Root 管理员账户</h2>
                        <p class="step-desc">此账户拥有最高权限，请妥善保管密码。</p>
                        <div class="form-group">
                            <label class="form-label">用户名 <span class="req">*</span></label>
                            <input type="text" id="rootUsername" class="form-input" placeholder="请输入管理员用户名" autocomplete="off">
                        </div>
                        <div class="form-group">
                            <label class="form-label">密码 <span class="req">*</span></label>
                            <input type="password" id="rootPassword" class="form-input" placeholder="至少12位，含大小写字母、数字及特殊字符" autocomplete="new-password">
                            <div class="form-hint">密码要求：至少 12 位，需同时包含大写字母、小写字母、数字，以及至少一个特殊字符。允许的特殊字符：<span>@</span><span>$</span><span>!</span><span>%%</span><span>*</span><span>?</span><span>&amp;</span></div>
                        </div>
                        <div class="form-group">
                            <label class="form-label">确认密码 <span class="req">*</span></label>
                            <input type="password" id="rootPasswordConfirm" class="form-input" placeholder="再次输入密码" autocomplete="new-password">
                        </div>
                        <div class="form-group">
                            <label class="form-label">邮箱 <span class="req">*</span></label>
                            <input type="email" id="rootEmail" class="form-input" placeholder="admin@example.com" autocomplete="off">
                        </div>
                    </div>

                    <div class="step-content" id="step-2">
                        <h2 class="step-title">邮箱验证配置</h2>
                        <p class="step-desc">配置 SMTP 邮件服务，用以发送验证码和通知（可选）。</p>
                        <div class="form-group toggle-group">
                            <label class="form-label">启用邮箱验证</label>
                            <label class="toggle-switch">
                                <input type="checkbox" id="emailEnabled">
                                <span class="toggle-slider"></span>
                            </label>
                        </div>
                        <div id="email-fields" class="email-fields" style="display:none;">
                            <div class="form-row">
                                <div class="form-group flex-2">
                                    <label class="form-label">SMTP 主机</label>
                                    <input type="text" id="emailHost" class="form-input" placeholder="smtp.example.com">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">端口</label>
                                    <input type="number" id="emailPort" class="form-input" placeholder="587" value="587">
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">SMTP 用户名</label>
                                <input type="text" id="emailUsername" class="form-input" placeholder="SMTP 登录用户名">
                            </div>
                            <div class="form-group">
                                <label class="form-label">SMTP 密码</label>
                                <input type="password" id="emailPassword" class="form-input" placeholder="SMTP 登录密码">
                            </div>
                            <div class="form-group">
                                <label class="form-label">发件人地址</label>
                                <input type="email" id="emailFrom" class="form-input" placeholder="noreply@example.com">
                            </div>
                        </div>
                    </div>

                    <div class="step-content" id="step-3">
                        <h2 class="step-title">数据库配置</h2>
                        <p class="step-desc">选择并配置数据存储方案。</p>
                        <div class="db-type-selector">
                            <div class="db-type-card active" data-type="sqlite" onclick="InstallWizard.selectDbType('sqlite')">
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">SQLite</div>
                                <div class="db-desc">轻量级，适合单机部署</div>
                            </div>
                            <div class="db-type-card" data-type="mysql" onclick="InstallWizard.selectDbType('mysql')">
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">MySQL</div>
                                <div class="db-desc">流行关系型数据库</div>
                            </div>
                            <div class="db-type-card" data-type="pgsql" onclick="InstallWizard.selectDbType('pgsql')">
                                <div class="db-icon"><i class="fas fa-database"></i></div>
                                <div class="db-name">PostgreSQL</div>
                                <div class="db-desc">高级开源数据库</div>
                            </div>
                        </div>
                        <input type="hidden" id="dbType" value="sqlite">
                        <div id="db-sqlite-fields" class="db-fields">
                            <div class="form-group">
                                <label class="form-label">数据库文件路径</label>
                                <input type="text" id="sqlitePath" class="form-input" value="./data.db" placeholder="./data.db">
                            </div>
                        </div>
                        <div id="db-server-fields" class="db-fields" style="display:none;">
                            <div class="form-row">
                                <div class="form-group flex-2">
                                    <label class="form-label">主机地址</label>
                                    <input type="text" id="dbHost" class="form-input" value="localhost" placeholder="localhost">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">端口</label>
                                    <input type="number" id="dbPort" class="form-input" value="3306" placeholder="3306">
                                </div>
                            </div>
                            <div class="form-group">
                                <label class="form-label">数据库名</label>
                                <input type="text" id="dbName" class="form-input" value="yggdrasil" placeholder="yggdrasil">
                            </div>
                            <div class="form-row">
                                <div class="form-group flex-1">
                                    <label class="form-label">用户名</label>
                                    <input type="text" id="dbUsername" class="form-input" placeholder="root">
                                </div>
                                <div class="form-group flex-1">
                                    <label class="form-label">密码</label>
                                    <input type="password" id="dbPassword" class="form-input" placeholder="">
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="step-content" id="step-4">
                        <h2 class="step-title">确认安装配置</h2>
                        <p class="step-desc">请核对以下配置信息，确认无误后点击"开始安装"。</p>
                        <div id="summary-content" class="summary-card">
                        </div>
                    </div>

                    <div class="step-content" id="step-installing" style="display:none;">
                        <div class="installing-view">
                            <div class="installing-spinner"></div>
                            <h2 class="installing-title">正在安装...</h2>
                            <p class="installing-desc" id="installing-status">初始化中</p>
                        </div>
                    </div>

                    <div class="step-content" id="step-result" style="display:none;">
                        <div class="result-view" id="result-view">
                        </div>
                    </div>

                    <div class="install-footer" id="install-footer">
                        <button class="btn btn-secondary" id="btn-prev" onclick="InstallWizard.prevStep()" style="visibility:hidden;">
                            ← 上一步
                        </button>
                        <button class="btn btn-primary" id="btn-next" onclick="InstallWizard.nextStep()">
                            下一步 →
                        </button>
                    </div>
                </div>
            </div>
            <script src="/js/install.js"></script>
            """.formatted(token);
    }
}
