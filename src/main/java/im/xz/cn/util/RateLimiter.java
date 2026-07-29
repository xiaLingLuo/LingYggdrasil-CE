package im.xz.cn.util;

import im.xz.cn.database.CacheDao;

public class RateLimiter {
    private final CacheDao cacheDao;

    public RateLimiter(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }

    public boolean check(String key, int intervalMs) {
        if (intervalMs <= 0) return true;
        int ttl = (intervalMs / 1000) + 1;
        String val = cacheDao.get(key);
        if (val != null) return false;
        cacheDao.put(key, "1", "ratelimit", ttl);
        return true;
    }

    public boolean checkRate(String key, int maxRequests, int windowMs) {
        if (maxRequests <= 0) return true;
        int ttl = (windowMs / 1000) + 1;
        int count = cacheDao.incrementAndGet(key, "ratelimit", ttl);
        return count <= maxRequests;
    }

    public static Integer getInterval(String method, String path) {
        if (!method.equals("POST")) return null;

        if (path.equals("/api/register") || path.equals("/api/resend-code")) return 300000;

        if (path.equals("/api/skins/upload") || path.equals("/api/capes/upload")
                || path.equals("/api/skins/delete") || path.equals("/api/capes/delete")) return 60000;

        if (path.equals("/api/settings/email") || path.equals("/api/settings/password")
                || path.equals("/api/send-email-verify") || path.equals("/api/settings/send-verify-code")
                || path.equals("/api/verify-my-email")
                || path.equals("/api/profiles/update") || path.equals("/api/profiles/create")
                || path.equals("/api/profiles/delete") || path.equals("/api/profiles/regenerate-token")) return 5000;

        if (path.equals("/api/friends/block") || path.equals("/api/friends/unblock")
                || path.equals("/api/friends/blocked/clear") || path.equals("/api/friends/add")
                || path.equals("/api/friends/delete") || path.equals("/api/friends/request/accept")
                || path.equals("/api/friends/request/cancel") || path.equals("/api/settings/nickname")) return 1000;

        if (path.equals("/api/world/like") || path.equals("/api/world/favorite")
                || path.equals("/api/world/favorite/alias")
                || path.equals("/api/textures/visibility")
                || path.equals("/api/friends/share-texture") || path.equals("/api/friends/unshare-texture")
                || path.equals("/api/friends/display-profile")
                || path.equals("/api/skins/alias") || path.equals("/api/capes/alias")) return 500;

        return null;
    }

    public static Integer getRateLimit(String path) {
        if (path.equals("/api/world/textures") || path.startsWith("/api/world/textures")) return 30;

        if (path.startsWith("/api/friends/") && (path.endsWith("/shared-textures") || path.endsWith("/my-shared")
                || path.equals("/api/friends") || path.equals("/api/friends/my-info")
                || path.equals("/api/friends/blocked"))) return 60;

        if (path.equals("/api/skins") || path.equals("/api/capes")
                || path.equals("/api/profiles") || path.equals("/api/textures/my")
                || path.equals("/api/shared/my")
                || path.equals("/api/skins/download") || path.equals("/api/capes/download")) return 30;

        if (path.equals("/api/textures/visibility")) return 60;

        if (path.equals("/api/announcement") || path.equals("/api/footer-info")) return 60;

        return null;
    }
}
