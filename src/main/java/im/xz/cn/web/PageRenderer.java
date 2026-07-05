package im.xz.cn.web;

import im.xz.cn.something.web.Css;
import im.xz.cn.util.FooterInfo;

public class PageRenderer {

    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    private static String getBaseCss() {
        return Css.getBaseCss();
    }

    public static String renderPage(String title, String bodyContent, String pageType, String... extraCss) {
        StringBuilder linkTags = new StringBuilder();
        StringBuilder inlineCss = new StringBuilder();
        for (String c : extraCss) {
            String trimmed = c.trim();
            if (trimmed.startsWith("<link")) {
                linkTags.append(trimmed).append("\n");
            } else if (trimmed.startsWith("@import")) {
                inlineCss.append(trimmed).append("\n");
            } else {
                inlineCss.append(c).append("\n");
            }
        }

        String inlineStyleBlock = !inlineCss.isEmpty()
                ? "<style>\n" + inlineCss.toString() + "</style>\n"
                : "";

        String html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - 泠 Yggdrasil</title>
                <link rel="stylesheet" href="/css/all.min.css">
                %s
                <style>
                    %s
                </style>
                %s
            </head>
            <body>
                %s
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
            </body>
            </html>
            """.formatted(escapeHtml(title), linkTags.toString(), getBaseCss(), inlineStyleBlock, bodyContent, FooterInfo.getYear(), FooterInfo.FOOTER_PLACEHOLDER);

        return FooterInfo.injectFooterRecords(html);
    }

    public static String renderNavbar(String siteName, String currentPage, boolean isAdmin) {
        String adminLink = isAdmin ?
            """
            <a href="/admin" class="nav-link %s">管理后台</a>
            """.formatted("admin".equals(currentPage) ? "active" : "") : "";

        return """
            <nav class="navbar">
                <div class="navbar-inner">
                    <div class="navbar-brand">
                        <span class="brand-icon">✿</span>
                        <span class="brand-text">%s</span>
                    </div>
                    <div class="navbar-links">
                        <a href="/" class="nav-link %s">首页</a>
                        %s
                    </div>
                </div>
            </nav>
            """.formatted(escapeHtml(siteName),
                "home".equals(currentPage) ? "active" : "",
                adminLink);
    }

    public static String renderSidebar(String currentPage, boolean isAdmin, boolean isRoot) {
        StringBuilder items = new StringBuilder();

        items.append(sidebarItem("/admin", "dashboard", "仪表盘", currentPage));
        items.append(sidebarItem("/admin/users", "users", "用户管理", currentPage));
        items.append(sidebarItem("/admin/profiles", "profiles", "角色管理", currentPage));
        items.append(sidebarItem("/admin/settings", "settings", "系统设置", currentPage));

        if (isRoot) {
            items.append(sidebarItem("/admin/admins", "admins", "管理员管理", currentPage));
            items.append(sidebarItem("/admin/logs", "logs", "系统日志", currentPage));
        }

        return """
            <aside class="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-icon"><i class="fas fa-bars"></i></span>
                    <span>管理菜单</span>
                </div>
                <div class="sidebar-menu">
                    %s
                </div>
            </aside>
            """.formatted(items.toString());
    }

    public static String renderCard(String title, String content) {
        return """
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">%s</h3>
                </div>
                <div class="card-body">
                    %s
                </div>
            </div>
            """.formatted(escapeHtml(title), content);
    }

    public static String renderAlert(String message, String type) {
        String icon = switch (type) {
            case "success" -> "<i class=\"fas fa-circle-check\"></i>";
            case "error" -> "<i class=\"fas fa-circle-xmark\"></i>";
            case "warning" -> "<i class=\"fas fa-triangle-exclamation\"></i>";
            default -> "<i class=\"fas fa-circle-info\"></i>";
        };
        return """
            <div class="alert alert-%s">
                <span class="alert-icon">%s</span>
                <span class="alert-message">%s</span>
            </div>
            """.formatted(type, icon, escapeHtml(message));
    }

    private static String sidebarItem(String href, String page, String label, String currentPage) {
        String active = page.equals(currentPage) ? "active" : "";
        return """
            <a href="%s" class="sidebar-item %s">
                <span>%s</span>
            </a>
            """.formatted(href, active, label);
    }
}
