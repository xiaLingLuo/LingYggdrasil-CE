package im.xz.cn.util;

import io.javalin.http.Context;

/* 反向代理 */
public final class IpUtil {

    private IpUtil() {}

    public static String getClientIp(Context ctx) {
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String ip = forwarded.split(",")[0].trim();
            if (!ip.isEmpty()) return ip;
        }

        String realIp = ctx.header("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return ctx.req().getRemoteAddr();
    }
}
