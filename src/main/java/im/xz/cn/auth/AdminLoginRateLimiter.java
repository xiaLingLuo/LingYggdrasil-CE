package im.xz.cn.auth;

import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.i18n.AdminI18n;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AdminLoginRateLimiter {
    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final int MAX_ATTEMPTS_PER_ACCOUNT = 5;
    private static final int LOCKOUT_DURATION = 900;
    private static final int RATE_WINDOW = 60;
    private final CacheDao cacheDao;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public AdminLoginRateLimiter(CacheDao cacheDao) { this.cacheDao = cacheDao; }

    public String checkRateLimit(String username, String clientIp) {
        String ipLockKey = "admin_login_lockout_ip:" + clientIp;
        if (cacheDao.get(ipLockKey) != null) return AdminI18n.t("msg.loginTooFrequent");
        String accountLockKey = "admin_login_lockout_acct:" + username.toLowerCase();
        if (cacheDao.get(accountLockKey) != null) return AdminI18n.t("msg.accountTempLocked");
        if (getAndIncrement("admin_login_rate_ip:" + clientIp) > MAX_ATTEMPTS_PER_IP) {
            cacheDao.put(ipLockKey, "locked", "rate_limit", LOCKOUT_DURATION);
            return AdminI18n.t("msg.loginTooFrequent");
        }
        if (getAndIncrement("admin_login_rate_acct:" + username.toLowerCase()) > MAX_ATTEMPTS_PER_ACCOUNT) {
            cacheDao.put(accountLockKey, "locked", "rate_limit", LOCKOUT_DURATION);
            return AdminI18n.t("msg.accountLockedTooMany");
        }
        return null;
    }

    public void recordSuccess(String username) {
        String key = username.toLowerCase();
        cacheDao.delete("admin_login_rate_acct:" + key);
        cacheDao.delete("admin_login_lockout_acct:" + key);
    }

    private int getAndIncrement(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            int count = cacheDao.get(key) == null ? 0 : Integer.parseInt(cacheDao.get(key));
            count++;
            cacheDao.put(key, String.valueOf(count), "rate_counter", RATE_WINDOW);
            return count;
        } finally {
            lock.unlock();
        }
    }
}
