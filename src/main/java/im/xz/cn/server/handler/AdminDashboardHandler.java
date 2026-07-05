package im.xz.cn.server.handler;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.AdminDao;
import im.xz.cn.database.ProfileDao;
import im.xz.cn.database.TokenDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.something.web.AdminPage;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminDashboardHandler {
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final TokenDao tokenDao;
    private final AdminDao adminDao;

    public AdminDashboardHandler(UserDao userDao, ProfileDao profileDao, TokenDao tokenDao, AdminDao adminDao) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.tokenDao = tokenDao;
        this.adminDao = adminDao;
    }

    public void dashboardPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);

        ctx.html(AdminPage.renderDashboardPage(adminUsername, adminRole, csrfToken));
    }

    public void getStats(Context ctx) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userCount", userDao.count());
        stats.put("profileCount", profileDao.count());
        stats.put("activeTokenCount", tokenDao.countActive());
        stats.put("adminCount", adminDao.count());
        ctx.json(stats);
    }


}
