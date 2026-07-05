package im.xz.cn.server.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InstallRateLimiter {
    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 3600_000; // 一时辰

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> attempts = new ConcurrentHashMap<>();

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();
        CopyOnWriteArrayList<Long> list = attempts.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());
        
        list.removeIf(t -> now - t > WINDOW_MS);
        
        if (list.size() >= MAX_ATTEMPTS) {
            return false;
        }
        
        list.add(now);
        return true;
    }
}
