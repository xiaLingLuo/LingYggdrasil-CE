package im.xz.cn.rate;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;

import java.util.Map;

public class RateLimiter {
    private final CacheDao cacheDao;

    public RateLimiter(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }

    public boolean check(String key, int intervalMs) {
        if (intervalMs <= 0) return true;
        int ttl = (intervalMs / 1000) + 1;
        return cacheDao.putIfAbsent(key, "1", "ratelimit", ttl);
    }

    public boolean checkRate(String key, int maxRequests, int windowMs) {
        if (maxRequests <= 0) return true;
        int ttl = (windowMs / 1000) + 1;
        int count = cacheDao.incrementAndGet(key, "ratelimit", ttl);
        return count <= maxRequests;
    }

    private static String normalize(String path) {
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static Integer getInterval(String method, String path) {
        if (!method.equals("POST")) return null;
        return lookup(SystemConfig.getInstance().getRequestIntervalMap(), normalize(path));
    }

    public static Integer getRateLimit(String path) {
        return lookup(SystemConfig.getInstance().getRequestRateMap(), normalize(path));
    }

    private static Integer lookup(Map<String, Integer> map, String path) {
        if (map == null || map.isEmpty() || path == null) return null;
        Integer exact = map.get(path);
        if (exact != null) return exact;
        Integer bestPrefix = null;
        int bestPrefixLength = -1;
        Integer bestSuffix = null;
        int bestSuffixLength = -1;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("*") && key.length() > 1) {
                String prefix = key.substring(0, key.length() - 1);
                if (path.startsWith(prefix) && prefix.length() > bestPrefixLength) {
                    bestPrefixLength = prefix.length();
                    bestPrefix = entry.getValue();
                }
            } else if (key.startsWith("*") && key.length() > 1) {
                String suffix = key.substring(1);
                if (path.endsWith(suffix) && suffix.length() > bestSuffixLength) {
                    bestSuffixLength = suffix.length();
                    bestSuffix = entry.getValue();
                }
            }
        }
        return bestPrefix != null ? bestPrefix : bestSuffix;
    }
}
