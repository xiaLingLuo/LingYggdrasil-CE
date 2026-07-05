package im.xz.cn.something.web;

import static im.xz.cn.something.web.Shared.esc;
import im.xz.cn.util.FooterInfo;
import im.xz.cn.web.PageRenderer;

public class AdminPage {

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content) {
        return renderAdminLayout(currentPage, adminUsername, adminRole, content, null);
    }

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content, String csrfToken) {
        String roleDisplay = "ROOT".equalsIgnoreCase(adminRole) ? "超级管理员" : "运维管理员";
        String safeUsername = esc(adminUsername);
        String safeInitial = adminUsername != null && !adminUsername.isEmpty() ? esc(adminUsername.substring(0, 1).toUpperCase()) : "A";
        String csrfInject = Shared.csrfInject(csrfToken);
        String layout = csrfInject + """
            <div class="admin-layout-wrap">
                <aside class="admin-sidebar">
                    <div class="sidebar-brand">
                        <span class="sidebar-logo">✿</span>
                        <span class="sidebar-title">泠 Yggdrasil</span>
                    </div>
                    <nav class="sidebar-nav">
                        <a href="/admin/dashboard" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-gauge-high"></i></span>
                            <span>仪表盘</span>
                        </a>
                        <a href="/admin/system" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-gear"></i></span>
                            <span>系统管理</span>
                        </a>
                        <a href="/admin/yggdrasil" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-tree"></i></span>
                            <span>世界树管理</span>
                        </a>
                        <a href="/admin/users" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-users"></i></span>
                            <span>用户管理</span>
                        </a>
                        <a href="/admin/profiles" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-gamepad"></i></span>
                            <span>角色管理</span>
                        </a>
                        <a href="/admin/skins" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-palette"></i></span>
                            <span>皮肤管理</span>
                        </a>
                        <a href="/admin/capes" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-mask"></i></span>
                            <span>披风管理</span>
                        </a>
                        <a href="/admin/security" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-lock"></i></span>
                            <span>安全设置</span>
                        </a>
                        <a href="/admin/admins" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-user-shield"></i></span>
                            <span>管理员管理</span>
                        </a>
                        <a href="/admin/appinfo" class="sidebar-link %s">
                            <span class="sidebar-link-icon"><i class="fas fa-circle-info"></i></span>
                            <span>应用信息</span>
                        </a>
                    </nav>
                    <div class="sidebar-footer">
                        <div class="sidebar-user">
                            <div class="sidebar-avatar">%s</div>
                            <div class="sidebar-user-info">
                                <div class="sidebar-username">%s</div>
                                <div class="sidebar-role">%s</div>
                            </div>
                        </div>
                        <a href="/admin/logout" class="sidebar-link sidebar-logout">
                            <span class="sidebar-link-icon"><i class="fas fa-right-from-bracket"></i></span>
                            <span>登出</span>
                        </a>
                    </div>
                </aside>
                <main class="admin-main">
                    <div class="admin-content">
                        %s
                    </div>
                    <footer class="page-footer">
                        <p>&copy; <span class="footer-year">%s</span> LingYggdrasil. All rights reserved.</p>
                        <p class="footer-records" style="margin-top:4px;font-size:12px;color:#999;">%s</p>
                    </footer>
                    <script>
                    fetch('/api/footer-info').then(function(r){return r.json()}).then(function(d){
                        document.querySelectorAll('.footer-year').forEach(function(el){el.textContent=d.year});
                    }).catch(function(){
                        document.querySelectorAll('.footer-year').forEach(function(el){el.textContent=new Date().getFullYear()});
                    });
                    </script>
                </main>
            </div>
            """.formatted(
                "dashboard".equals(currentPage) ? "active" : "",
                "system".equals(currentPage) ? "active" : "",
                "yggdrasil".equals(currentPage) ? "active" : "",
                "users".equals(currentPage) ? "active" : "",
                "profiles".equals(currentPage) ? "active" : "",
                "skins".equals(currentPage) ? "active" : "",
                "capes".equals(currentPage) ? "active" : "",
                "security".equals(currentPage) ? "active" : "",
                "admins".equals(currentPage) ? "active" : "",
                "appinfo".equals(currentPage) ? "active" : "",
                adminUsername != null && !adminUsername.isEmpty() ? safeInitial : "A",
                adminUsername != null ? safeUsername : "Admin",
                roleDisplay,
                content,
                FooterInfo.getYear(),
                FooterInfo.FOOTER_PLACEHOLDER
            );

        return FooterInfo.injectFooterRecords(layout);
    }

    public static String renderLoginPage() {
        String body = """
            <div class="admin-login-container">
                <div class="admin-login-card">
                    <div class="login-header">
                        <div class="login-logo">✿</div>
                        <h1>泠 Yggdrasil 管理后台</h1>
                        <p class="login-subtitle">Administration Console</p>
                    </div>
                    <form id="loginForm" class="login-form">
                        <div class="form-group">
                            <label class="form-label">用户名</label>
                            <input type="text" id="username" class="form-input" placeholder="请输入管理员用户名" required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">密码</label>
                            <input type="password" id="password" class="form-input" placeholder="请输入密码" required>
                        </div>
                        <div id="errorMsg" class="error-msg" style="display:none;"></div>
                        <button type="submit" class="btn btn-primary btn-login">登 录</button>
                    </form>
                </div>
            </div>
            <script src="/js/admin-login.js"></script>
            """;

        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("管理员登录", body, "admin-login", css);
    }

    public static String dashboardContent() {
        return """
            <div class="page-header">
                <h2>仪表盘</h2>
                <p class="page-desc">系统概览与统计数据</p>
            </div>
            <div class="stats-grid">
                <div class="stat-card" id="stat-users">
                    <div class="stat-icon"><i class="fas fa-users"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="userCount">--</div>
                        <div class="stat-label">用户总数</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-profiles">
                    <div class="stat-icon"><i class="fas fa-gamepad"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="profileCount">--</div>
                        <div class="stat-label">角色总数</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-tokens">
                    <div class="stat-icon"><i class="fas fa-key"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="activeTokenCount">--</div>
                        <div class="stat-label">活跃令牌</div>
                    </div>
                </div>
                <div class="stat-card" id="stat-admins">
                    <div class="stat-icon"><i class="fas fa-user-shield"></i></div>
                    <div class="stat-info">
                        <div class="stat-number" id="adminCount">--</div>
                        <div class="stat-label">管理员数</div>
                    </div>
                </div>
            </div>
            <script src="/js/admin-dashboard.js"></script>
            """;
    }

    public static String renderDashboardPage(String adminUsername, String adminRole, String csrfToken) {
        String body = renderAdminLayout("dashboard", adminUsername, adminRole, dashboardContent(), csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("仪表盘", body, "admin", css);
    }

    public static String renderSkinsPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>皮肤管理</h2>
                <p class="page-desc">管理所有用户上传的皮肤</p>
            </div>
            <div class="card">
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>用户</th>
                                <th>别名</th>
                                <th>Hash</th>
                                <th>大小</th>
                                <th>上传时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="skinTableBody">
                            <tr><td colspan="7" class="text-center">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <div id="aliasModal" class="modal-overlay" style="display:none;">
                <div class="modal-box">
                    <h3>修改别名</h3>
                    <div class="form-group">
                        <label class="form-label">新别名</label>
                        <input type="text" class="form-input" id="newAlias" placeholder="输入新别名">
                    </div>
                    <input type="hidden" id="editSkinId">
                    <div id="aliasMsg" class="msg-area"></div>
                    <div class="modal-actions">
                        <button class="btn btn-secondary" onclick="closeModal('aliasModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitAlias()">保存</button>
                    </div>
                </div>
            </div>
            <script src="/js/admin-skins.js"></script>
            """;
        String body = renderAdminLayout("skins", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("皮肤管理", body, "admin", css);
    }

    public static String renderCapesPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>披风管理</h2>
                <p class="page-desc">管理所有用户上传的披风</p>
            </div>
            <div class="card">
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>用户</th>
                                <th>别名</th>
                                <th>Hash</th>
                                <th>大小</th>
                                <th>上传时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="capeTableBody">
                            <tr><td colspan="7" class="text-center">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <div id="aliasModal" class="modal-overlay" style="display:none;">
                <div class="modal-box">
                    <h3>修改别名</h3>
                    <div class="form-group">
                        <label class="form-label">新别名</label>
                        <input type="text" class="form-input" id="newAlias" placeholder="输入新别名">
                    </div>
                    <input type="hidden" id="editSkinId">
                    <div id="aliasMsg" class="msg-area"></div>
                    <div class="modal-actions">
                        <button class="btn btn-secondary" onclick="closeModal('aliasModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitAlias()">保存</button>
                    </div>
                </div>
            </div>
            <script src="/js/admin-capes.js"></script>
            """;
        String body = renderAdminLayout("capes", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("披风管理", body, "admin", css);
    }

    public static String renderAppInfoPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>应用信息</h2>
                <p class="page-desc">系统版本与基本信息</p>
            </div>
            <div class="appinfo-card card">
                <div class="card-header"><h3 class="card-title">系统信息</h3></div>
                <div class="card-body">
                    <div class="info-grid">
                        <div class="info-item">
                            <div class="info-label">程序名称</div>
                            <div class="info-value" id="appName">加载中...</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">版本号</div>
                            <div class="info-value" id="appVersion">加载中...</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">代码仓库</div>
                            <div class="info-value" id="appRepo">加载中...</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">安装时间</div>
                            <div class="info-value" id="installedAt">加载中...</div>
                        </div>
                    </div>
                </div>
            </div>
            <script src="/js/admin-appinfo.js"></script>
            """;
        String body = renderAdminLayout("appinfo", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("应用信息", body, "admin", css);
    }

    public static String renderSecurityPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>安全设置</h2>
                <p class="page-desc">安全策略管理</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">加密等级</h3></div>
                <div class="card-body">
                    <p style="margin-bottom:16px;color:#666;font-size:14px;">泠 Yggdrasil 提供了丰富的安全性自订能力，解决了拥有好机器却跑不满性能，担心不够安全的问题：既然拥有更强的服务器，那么为何不使用更强大的加密系统呢？</p>
                    <p style="margin-bottom:16px;color:#666;font-size:14px;">注意：提升此参数可能导致性能不够而服务器崩溃，重则无法完成账户鉴权从而无法登录管理后台，建议在非生产环境充分测试后再使用</p>
                    <div id="encryptionLevelList" class="encryption-level-list">
                    </div>
                    <input type="hidden" id="encryptionLevel" value="1">
                </div>
            </div>
            <div class="settings-actions">
                <button class="btn btn-primary" onclick="saveSettings()">保存设置</button>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-security.js"></script>
            """;
        String body = renderAdminLayout("security", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("安全设置", body, "admin", css);
    }

    public static String renderUsersPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>用户管理</h2>
                <p class="page-desc">查看和管理所有注册用户</p>
            </div>
            <div class="toolbar">
                <input type="text" id="searchInput" class="form-input search-input" placeholder="搜索用户名、邮箱或昵称..." oninput="filterUsers()">
            </div>
            <div class="card">
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>用户名</th>
                                <th>UUID</th>
                                <th>邮箱</th>
                                <th>昵称</th>
                                <th>角色</th>
                                <th>邮箱验证</th>
                                <th>注册时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="userTableBody">
                            <tr><td colspan="8" class="text-center">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div id="usernameModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('usernameModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>修改用户名</h3>
                        <button class="modal-close" onclick="closeModal('usernameModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editUserId">
                        <div class="form-group">
                            <label class="form-label">新用户名</label>
                            <input type="text" id="newUsername" class="form-input" placeholder="请输入新用户名">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('usernameModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitUsername()">确认修改</button>
                    </div>
                </div>
            </div>

            <div id="emailModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('emailModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>修改邮箱</h3>
                        <button class="modal-close" onclick="closeModal('emailModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editEmailUserId">
                        <div class="form-group">
                            <label class="form-label">新邮箱</label>
                            <input type="email" id="newEmail" class="form-input" placeholder="请输入新邮箱">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('emailModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitEmail()">确认修改</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-users.js"></script>
            """;
        String body = renderAdminLayout("users", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("用户管理", body, "admin", css);
    }

    public static String renderAdminsPage(String adminUsername, String adminRole, boolean isRoot, String csrfToken) {
        String createBtn = isRoot ? "<button class=\"btn btn-primary\" onclick=\"openCreateModal()\">+ 创建管理员</button>" : "";
        String content = """
            <div class="page-header">
                <h2>管理员管理</h2>
                <p class="page-desc">管理后台管理员账号</p>
            </div>
            <div class="toolbar">
                %s
            </div>
            <div class="card">
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>用户名</th>
                                <th>邮箱</th>
                                <th>角色</th>
                                <th>创建时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="adminTableBody">
                            <tr><td colspan="5" class="text-center">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div id="createModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('createModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>创建管理员</h3>
                        <button class="modal-close" onclick="closeModal('createModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">用户名</label>
                            <input type="text" id="createUsername" class="form-input" placeholder="管理员用户名">
                        </div>
                        <div class="form-group">
                            <label class="form-label">邮箱</label>
                            <input type="email" id="createEmail" class="form-input" placeholder="管理员邮箱">
                        </div>
                        <div class="form-group">
                            <label class="form-label">密码</label>
                            <input type="password" id="createPassword" class="form-input" placeholder="管理员密码">
                        </div>
                        <div class="form-group">
                            <label class="form-label">角色</label>
                            <select id="createRole" class="form-input">
                                <option value="OP">运维管理员 (OP)</option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('createModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitCreate()">创建</button>
                    </div>
                </div>
            </div>

            <div id="editModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('editModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>修改管理员</h3>
                        <button class="modal-close" onclick="closeModal('editModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editAdminId">
                        <div class="form-group">
                            <label class="form-label">用户名</label>
                            <input type="text" id="editUsername" class="form-input" placeholder="用户名（留空不修改）">
                        </div>
                        <div class="form-group">
                            <label class="form-label">邮箱</label>
                            <input type="email" id="editEmail" class="form-input" placeholder="邮箱（留空不修改）">
                        </div>
                        <div class="form-group">
                            <label class="form-label">新密码</label>
                            <input type="password" id="editPassword" class="form-input" placeholder="密码（留空不修改）">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('editModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitEdit()">保存修改</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script>window.IS_ROOT = %s;</script>
            <script src="/js/admin-admins.js"></script>
            """.formatted(createBtn, Boolean.toString(isRoot));
        String body = renderAdminLayout("admins", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("管理员管理", body, "admin", css);
    }

    public static String renderAdminProfilesPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>角色管理</h2>
                <p class="page-desc">管理所有 Yggdrasil 玩家角色</p>
            </div>
            <div class="toolbar">
                <button class="btn btn-primary" onclick="openCreateModal()">+ 创建角色</button>
            </div>
            <div class="card">
                <div class="card-body" style="padding:0;">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th>角色名</th>
                                <th>所属用户</th>
                                <th>UUID</th>
                                <th>形态</th>
                                <th>创建时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="profileTableBody">
                            <tr><td colspan="6" class="text-center">加载中...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div id="createModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('createModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>创建角色</h3>
                        <button class="modal-close" onclick="closeModal('createModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label class="form-label">用户UUID、用户名或邮箱</label>
                            <input type="text" id="createUserId" class="form-input" placeholder="请输入用户UUID、用户名或邮箱">
                        </div>
                        <div class="form-group">
                            <label class="form-label">角色名称</label>
                            <input type="text" id="createName" class="form-input" placeholder="请输入角色名称" maxlength="24">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('createModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitCreate()">创建</button>
                    </div>
                </div>
            </div>

            <div id="updateModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('updateModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>修改角色名称</h3>
                        <button class="modal-close" onclick="closeModal('updateModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="editProfileId">
                        <div class="form-group">
                            <label class="form-label">新名称</label>
                            <input type="text" id="newName" class="form-input" placeholder="请输入新名称" maxlength="24">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('updateModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitUpdate()">确认修改</button>
                    </div>
                </div>
            </div>

            <div id="transferModal" class="modal" style="display:none;">
                <div class="modal-overlay" onclick="closeModal('transferModal')"></div>
                <div class="modal-card">
                    <div class="modal-header">
                        <h3>转移所有权</h3>
                        <button class="modal-close" onclick="closeModal('transferModal')">&times;</button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="transferProfileId">
                        <div class="form-group">
                            <label class="form-label">用户ID、用户名或邮箱</label>
                            <input type="text" id="transferUserId" class="form-input" placeholder="请输入用户ID、用户名或邮箱">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" onclick="closeModal('transferModal')">取消</button>
                        <button class="btn btn-primary" onclick="submitTransfer()">确认转移</button>
                    </div>
                </div>
            </div>

            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-profiles.js"></script>
            """;
        String body = renderAdminLayout("profiles", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("角色管理", body, "admin", css);
    }

    public static String renderYggdrasilPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>世界树设置</h2>
                <p class="page-desc">配置 Yggdrasil 认证服务参数</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">UUID 设置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">UUID 版本</div>
                            <div class="setting-desc">玩家档案使用的 UUID 版本</div>
                        </div>
                        <select id="uuidVersion" class="form-input setting-select">
                            <option value="v3">v3 (MD5)</option>
                            <option value="v4">v4 (Random)</option>
                            <option value="v5">v5 (SHA-1)</option>
                        </select>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">Token 设置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">Token 临时过期时间（分钟）</div>
                            <div class="setting-desc">临时 Token 的有效期，默认 4320 分钟（3天）</div>
                        </div>
                        <input type="number" id="tokenTempExpiry" class="form-input setting-input" placeholder="4320">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">Token 永久过期时间（分钟）</div>
                            <div class="setting-desc">永久 Token 的有效期，默认 10080 分钟（7天）</div>
                        </div>
                        <input type="number" id="tokenPermanentExpiry" class="form-input setting-input" placeholder="10080">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">每角色最大 Token 数量</div>
                            <div class="setting-desc">每个角色可同时存在的有效 Token 上限</div>
                        </div>
                        <input type="number" id="maxTokensPerProfile" class="form-input setting-input" placeholder="12">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">认证限制</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">登入/登出频率限制（毫秒）</div>
                            <div class="setting-desc">两次认证请求之间的最小间隔时间</div>
                        </div>
                        <input type="number" id="authRateLimit" class="form-input setting-input" placeholder="1000">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">批量查询角色最大数量</div>
                            <div class="setting-desc">单次批量查询接口允许查询的最大角色数</div>
                        </div>
                        <input type="number" id="batchQueryMaxCount" class="form-input setting-input" placeholder="6">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">签名证书</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">证书模式</div>
                            <div class="setting-desc">现代模式使用 Ed448；兼容模式使用 RSA-SHA512</div>
                        </div>
                        <select id="signatureMode" class="form-input setting-select">
                            <option value="ed448">现代</option>
                            <option value="rsa-sha512">兼容</option>
                        </select>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">RSA/EdDSA 公钥</div>
                            <div class="setting-desc">Yggdrasil API 签名验证使用的公钥</div>
                        </div>
                        <textarea id="yggdrasilPublicKey" class="form-input setting-textarea" rows="6" readonly placeholder="点击上方按钮生成密钥对"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">RSA/EdDSA 私钥</div>
                            <div class="setting-desc">服务端签名使用的私钥，请妥善保管</div>
                        </div>
                        <textarea id="yggdrasilPrivateKey" class="form-input setting-textarea" rows="6" placeholder="点击上方按钮生成密钥对"></textarea>
                    </div>
                    <div style="display:flex;gap:10px;margin-top:12px;">
                        <button class="btn btn-primary" onclick="regenerateKeys()">重新生成密钥对</button>
                        <button class="btn btn-danger" onclick="switchMode()">切换证书模式</button>
                    </div>
                </div>
            </div>
            <div class="settings-actions">
                <button class="btn btn-primary" onclick="saveSettings()">保存设置</button>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-yggdrasil.js"></script>
            """;
        String body = renderAdminLayout("yggdrasil", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("世界树设置", body, "admin", css);
    }

    public static String renderSystemPage(String adminUsername, String adminRole, String csrfToken) {
        String content = """
            <div class="page-header">
                <h2>系统管理</h2>
                <p class="page-desc">管理系统配置与设置</p>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">基本设置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">站点名称</div>
                            <div class="setting-desc">显示在页面标题和导航栏</div>
                        </div>
                        <input type="text" id="siteName" class="form-input setting-input" placeholder="站点名称">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">站点介绍</div>
                            <div class="setting-desc">站点的简短描述</div>
                        </div>
                        <textarea id="siteDescription" class="form-input setting-textarea" rows="3" placeholder="站点介绍"></textarea>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">功能开关</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">注册功能</div>
                            <div class="setting-desc">允许新用户注册账号</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="registrationEnabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">邮箱验证要求</div>
                            <div class="setting-desc">用户注册时需要验证邮箱</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="emailVerificationEnabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">项目外网域名</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">用户页面（homepage）外网域名</div>
                            <div class="setting-desc">用户端页面的外部访问 URL</div>
                        </div>
                        <input type="text" id="userDomain" class="form-input setting-input" placeholder="例如: https://example.com">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">管理页面外网域名</div>
                            <div class="setting-desc">管理后台的外部访问 URL</div>
                        </div>
                        <input type="text" id="adminDomain" class="form-input setting-input" placeholder="例如: https://admin.example.com">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">API 外网域名</div>
                            <div class="setting-desc">Yggdrasil API 的外部访问 URL</div>
                        </div>
                        <input type="text" id="apiDomain" class="form-input setting-input" placeholder="例如: https://api.example.com">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">通用外网域名</div>
                            <div class="setting-desc">当上述三项无数据时的通用回退域名</div>
                        </div>
                        <input type="text" id="commonDomain" class="form-input setting-input" placeholder="例如: https://example.com">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">邮箱服务器配置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">邮箱验证开关</div>
                            <div class="setting-desc">启用后用户注册时需要验证邮箱</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="mailEnabled">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">SMTP 主机</div>
                            <div class="setting-desc">邮件服务器地址</div>
                        </div>
                        <input type="text" id="mailHost" class="form-input setting-input" placeholder="例如: smtp.example.com">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">SMTP 端口</div>
                            <div class="setting-desc">邮件服务器端口，常见: 465 (SSL), 587 (TLS)</div>
                        </div>
                        <input type="number" id="mailPort" class="form-input setting-input" placeholder="465">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">SMTP 用户名</div>
                            <div class="setting-desc">登录邮件服务器的用户名</div>
                        </div>
                        <input type="text" id="mailUsername" class="form-input setting-input" placeholder="用户名或邮箱地址">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">SMTP 密码</div>
                            <div class="setting-desc">登录邮件服务器的密码或授权码</div>
                        </div>
                        <input type="password" id="mailPassword" class="form-input setting-input" placeholder="密码">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">发件人地址</div>
                            <div class="setting-desc">发送邮件时显示的发件人地址</div>
                        </div>
                        <input type="email" id="mailFrom" class="form-input setting-input" placeholder="noreply@example.com">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">TLS 加密</div>
                            <div class="setting-desc">启用后使用 TLS/SSL 加密连接邮件服务器</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="mailTls">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">用户名黑名单控制</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">用户名黑名单</div>
                            <div class="setting-desc">每行一个用户名，支持通配符 *（如 *admin* 表示包含 admin 即禁）</div>
                        </div>
                        <textarea id="usernameBlacklist" class="form-input setting-textarea" rows="5" placeholder="admin&#10;root&#10;system"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">大小写严格模式</div>
                            <div class="setting-desc">启用后黑名单将区分大小写</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="usernameBlacklistCaseSensitive">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">邮箱域名控制</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">域名列表</div>
                            <div class="setting-desc">每行一个域名，支持通配符 *（如 *.evil.com）</div>
                        </div>
                        <textarea id="emailDomainList" class="form-input setting-textarea" rows="5" placeholder="example.com&#10;test.com"></textarea>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">域名过滤模式</div>
                            <div class="setting-desc">黑名单模式下不允许列表中的域名注册；白名单模式下只允许列表中的域名注册</div>
                        </div>
                        <select id="emailDomainMode" class="form-input setting-select">
                            <option value="blacklist">黑名单模式</option>
                            <option value="whitelist">白名单模式</option>
                        </select>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">备案信息</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">ICP 备案号</div>
                            <div class="setting-desc">显示在页面底部 footer 中，为空时不显示</div>
                        </div>
                        <input type="text" id="icpRecord" class="form-input setting-input" placeholder="例如：京ICP备12345678号">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label"> 公网安备号</div>
                            <div class="setting-desc">显示在页面底部 footer 中，为空时不显示</div>
                        </div>
                        <input type="text" id="publicSecurityRecord" class="form-input setting-input" placeholder="例如：京公网安备11010502030143号">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">角色设置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">每用户最大角色数量</div>
                            <div class="setting-desc">每个用户最多可创建的角色数量</div>
                        </div>
                        <input type="number" id="maxProfilesPerUser" class="form-input setting-input" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">同 IP 最大可注册账号数量</div>
                            <div class="setting-desc">同一 IP 地址最多可注册的账号数量</div>
                        </div>
                        <input type="number" id="maxAccountsPerIp" class="form-input setting-input" placeholder="3">
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">皮肤与披风设置</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">皮肤最大大小（KiB）</div>
                            <div class="setting-desc">单个皮肤文件的最大大小</div>
                        </div>
                        <input type="number" id="skinMaxSize" class="form-input setting-input" placeholder="64">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">皮肤最大数量</div>
                            <div class="setting-desc">每个用户最多可上传的皮肤数量</div>
                        </div>
                        <input type="number" id="skinMaxCount" class="form-input setting-input" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">皮肤总大小限制（KiB）</div>
                            <div class="setting-desc">每个用户皮肤总大小上限</div>
                        </div>
                        <input type="number" id="skinMaxTotalSize" class="form-input setting-input" placeholder="640">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">皮肤上传频率限制（每天，-1=无限制）</div>
                            <div class="setting-desc">每个用户每天最多上传皮肤次数</div>
                        </div>
                        <input type="number" id="skinRateLimit" class="form-input setting-input" placeholder="24">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">皮肤存储路径</div>
                            <div class="setting-desc">皮肤文件存储目录</div>
                        </div>
                        <input type="text" id="skinStoragePath" class="form-input setting-input" placeholder="skins">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">披风最大大小（KiB）</div>
                            <div class="setting-desc">单个披风文件的最大大小</div>
                        </div>
                        <input type="number" id="capeMaxSize" class="form-input setting-input" placeholder="64">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">披风最大数量</div>
                            <div class="setting-desc">每个用户最多可上传的披风数量</div>
                        </div>
                        <input type="number" id="capeMaxCount" class="form-input setting-input" placeholder="10">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">披风总大小限制（KiB）</div>
                            <div class="setting-desc">每个用户披风总大小上限</div>
                        </div>
                        <input type="number" id="capeMaxTotalSize" class="form-input setting-input" placeholder="640">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">披风上传频率限制（每天，-1=无限制）</div>
                            <div class="setting-desc">每个用户每天最多上传披风次数</div>
                        </div>
                        <input type="number" id="capeRateLimit" class="form-input setting-input" placeholder="24">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">披风存储路径</div>
                            <div class="setting-desc">披风文件存储目录</div>
                        </div>
                        <input type="text" id="capeStoragePath" class="form-input setting-input" placeholder="capes">
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">允许下载皮肤</div>
                            <div class="setting-desc">是否允许通过 Yggdrasil API 下载皮肤</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="allowDownloadSkin">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">允许下载披风</div>
                            <div class="setting-desc">是否允许通过 Yggdrasil API 下载披风</div>
                        </div>
                        <label class="toggle-switch">
                            <input type="checkbox" id="allowDownloadCape">
                            <span class="toggle-slider"></span>
                        </label>
                    </div>
                </div>
            </div>
            <div class="settings-card card">
                <div class="card-header"><h3 class="card-title">缓存管理</h3></div>
                <div class="card-body">
                    <div class="setting-item">
                        <div class="setting-info">
                            <div class="setting-label">清除缓存</div>
                            <div class="setting-desc">清除所有系统缓存和过期令牌</div>
                        </div>
                        <button class="btn btn-danger" onclick="clearCache()">清除缓存</button>
                    </div>
                </div>
            </div>
            <div class="settings-actions">
                <button class="btn btn-primary" onclick="saveSettings()">保存设置</button>
            </div>
            <div id="toast" class="toast" style="display:none;"></div>
            <script src="/js/admin-system.js"></script>
            """;
        String body = renderAdminLayout("system", adminUsername, adminRole, content, csrfToken);
        String css = Css.getAdminCssLink();
        return PageRenderer.renderPage("系统管理", body, "admin", css);
    }
}
