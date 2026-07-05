package im.xz.cn.server.handler;

import im.xz.cn.something.web.AdminPage;

public class AdminLayoutHelper {

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content) {
        return AdminPage.renderAdminLayout(currentPage, adminUsername, adminRole, content);
    }

    public static String renderAdminLayout(String currentPage, String adminUsername, String adminRole, String content, String csrfToken) {
        return AdminPage.renderAdminLayout(currentPage, adminUsername, adminRole, content, csrfToken);
    }
}
