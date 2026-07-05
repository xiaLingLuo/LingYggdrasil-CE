package im.xz.cn.server.handler;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.something.web.AdminPage;

import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminAppInfoHandler {

    public AdminAppInfoHandler() {}

    public void appInfoPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAppInfoPage(adminUsername, adminRole, csrfToken));
    }

    public void getAppInfo(Context ctx) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("appName", AppConfig.APP_NAME);
        info.put("appVersion", AppConfig.APP_VERSION);
        info.put("appRepo", AppConfig.APP_REPO);
        info.put("repoUrl", "https://git.im.xz.cn/lingluo/LingYggdrasil");
        info.put("installedAt", sysConfig.getInstalledAt() != null ? sysConfig.getInstalledAt() : "未知");
        ctx.json(info);
    }
}
