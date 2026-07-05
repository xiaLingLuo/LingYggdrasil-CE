package im.xz.cn.server.handler;

import im.xz.cn.auth.AuthService;
import im.xz.cn.auth.LoginRateLimiter;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.CacheDao;
import im.xz.cn.model.Admin;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.web.PageRenderer;

import io.javalin.http.Context;

import java.util.Map;

public class AdminAuthHandler {
    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    public AdminAuthHandler(AuthService authService, CacheDao cacheDao) {
        this.authService = authService;
        this.rateLimiter = new LoginRateLimiter(cacheDao);
    }

    public void loginPage(Context ctx) {
        ctx.html(AdminPage.renderLoginPage());
    }

    public void login(Context ctx) {
        var body = ctx.bodyAsClass(Map.class);
        String username = (String) body.get("username");
        String password = (String) body.get("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            ctx.json(Map.of("success", false, "message", "用户名和密码不能为空"));
            return;
        }

        String clientIp = IpUtil.getClientIp(ctx);
        String rateLimitMsg = rateLimiter.checkRateLimit("admin:" + username, clientIp);
        if (rateLimitMsg != null) {
            ctx.json(Map.of("success", false, "message", rateLimitMsg));
            return;
        }

        Admin admin = authService.authenticateAdmin(username, password);
        if (admin == null) {
            AuditLogger.logLogin("admin:" + username, clientIp, false);
            ctx.json(Map.of("success", false, "message", "用户名或密码错误"));
            return;
        }

        rateLimiter.recordSuccess("admin:" + username);
        AuditLogger.logLogin("admin:" + username, clientIp, true);

        SessionManager.invalidateAndRenewAdmin(ctx);
        SessionManager.setAdminId(ctx, admin.getId());
        SessionManager.setAdminRole(ctx, admin.getRole().name());
        ctx.sessionAttribute("adminUsername", admin.getUsername());
        SessionManager.bindClientFingerprint(ctx);
        ctx.json(Map.of("success", true, "redirect", "/admin/dashboard"));
    }

    public void logout(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String ip = IpUtil.getClientIp(ctx);
        AuditLogger.logLogout("admin:" + (adminUsername != null ? adminUsername : "unknown"), ip);
        SessionManager.invalidateAdmin(ctx);
        ctx.redirect("/admin/login");
    }
}
