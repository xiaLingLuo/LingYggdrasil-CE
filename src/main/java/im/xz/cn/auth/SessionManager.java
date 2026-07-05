package im.xz.cn.auth;

import im.xz.cn.util.IpUtil;
import io.javalin.http.Context;

public class SessionManager {

    public static void setUserId(Context ctx, String userId) {
        ctx.sessionAttribute("userId", userId);
    }

    public static String getUserId(Context ctx) {
        return ctx.sessionAttribute("userId");
    }

    public static void setAdminId(Context ctx, String adminId) {
        ctx.sessionAttribute("adminId", adminId);
    }

    public static String getAdminId(Context ctx) {
        return ctx.sessionAttribute("adminId");
    }

    public static void setAdminRole(Context ctx, String role) {
        ctx.sessionAttribute("adminRole", role);
    }

    public static String getAdminRole(Context ctx) {
        return ctx.sessionAttribute("adminRole");
    }

    public static void invalidate(Context ctx) {
        ctx.sessionAttribute("userId", null);
        ctx.sessionAttribute("clientFingerprint", null);
    }

    public static void invalidateAdmin(Context ctx) {
        ctx.sessionAttribute("adminId", null);
        ctx.sessionAttribute("adminRole", null);
        ctx.sessionAttribute("adminUsername", null);
        ctx.sessionAttribute("clientFingerprint", null);
    }

    public static boolean isLoggedIn(Context ctx) {
        return getUserId(ctx) != null;
    }

    public static boolean isAdminLoggedIn(Context ctx) {
        return getAdminId(ctx) != null;
    }

    public static String getOrCreateCsrfToken(Context ctx) {
        String token = ctx.sessionAttribute("csrfToken");
        if (token == null) {
            token = java.util.UUID.randomUUID().toString().replace("-", "");
            ctx.sessionAttribute("csrfToken", token);
        }
        return token;
    }

    public static boolean validateCsrfToken(Context ctx) {
        String sessionToken = ctx.sessionAttribute("csrfToken");
        if (sessionToken == null) return false;
        String requestToken = ctx.header("X-CSRF-Token");
        if (requestToken == null) {
            requestToken = ctx.formParam("_csrf");
        }
        return sessionToken.equals(requestToken);
    }

    public static void invalidateAndRenewUser(Context ctx) {
        var oldSession = ctx.req().getSession(false);
        if (oldSession != null) {
            String adminId = (String) oldSession.getAttribute("adminId");
            String adminRole = (String) oldSession.getAttribute("adminRole");
            String adminUsername = (String) oldSession.getAttribute("adminUsername");
            String csrfToken = (String) oldSession.getAttribute("csrfToken");
            String clientFingerprint = (String) oldSession.getAttribute("clientFingerprint");

            oldSession.invalidate();

            var newSession = ctx.req().getSession(true);
            if (adminId != null) newSession.setAttribute("adminId", adminId);
            if (adminRole != null) newSession.setAttribute("adminRole", adminRole);
            if (adminUsername != null) newSession.setAttribute("adminUsername", adminUsername);
            if (csrfToken != null) newSession.setAttribute("csrfToken", csrfToken);
            if (clientFingerprint != null) newSession.setAttribute("clientFingerprint", clientFingerprint);
        }
    }

    public static void invalidateAndRenewAdmin(Context ctx) {
        var oldSession = ctx.req().getSession(false);
        if (oldSession != null) {
            String userId = (String) oldSession.getAttribute("userId");
            String csrfToken = (String) oldSession.getAttribute("csrfToken");
            String clientFingerprint = (String) oldSession.getAttribute("clientFingerprint");

            oldSession.invalidate();

            var newSession = ctx.req().getSession(true);
            if (userId != null) newSession.setAttribute("userId", userId);
            if (csrfToken != null) newSession.setAttribute("csrfToken", csrfToken);
            if (clientFingerprint != null) newSession.setAttribute("clientFingerprint", clientFingerprint);
        }
    }

    public static void bindClientFingerprint(Context ctx) {
        String ip = IpUtil.getClientIp(ctx);
        String ua = ctx.header("User-Agent");
        if (ua == null) ua = "";
        String fingerprint = sha256(ip + "|" + ua);
        ctx.sessionAttribute("clientFingerprint", fingerprint);
    }

    public static boolean validateClientFingerprint(Context ctx) {
        String stored = ctx.sessionAttribute("clientFingerprint");
        if (stored == null) return true;
        String ip = IpUtil.getClientIp(ctx);
        String ua = ctx.header("User-Agent");
        if (ua == null) ua = "";
        String current = sha256(ip + "|" + ua);
        return stored.equals(current);
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
