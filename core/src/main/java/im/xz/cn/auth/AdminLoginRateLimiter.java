/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.auth;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.i18n.AdminI18n;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AdminLoginRateLimiter {
    private final CacheDao cacheDao;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public AdminLoginRateLimiter(CacheDao cacheDao) { this.cacheDao = cacheDao; }

    public String checkRateLimit(String username, String clientIp) {
        String ipLockKey = "admin_login_lockout_ip:" + clientIp;
        if (cacheDao.get(ipLockKey) != null) return AdminI18n.t("msg.loginTooFrequent");
        String accountLockKey = "admin_login_lockout_acct:" + username.toLowerCase() + "|" + clientIp;
        if (cacheDao.get(accountLockKey) != null) return AdminI18n.t("msg.accountTempLocked");
        return null;
    }

    
    public void recordFailure(String username, String clientIp) {
        SystemConfig cfg = SystemConfig.getInstance();
        int maxAttemptsPerIp = cfg.getLoginMaxAttemptsPerIp();
        int maxAttemptsPerAccount = cfg.getLoginMaxAttemptsPerAccount();
        int lockoutDuration = cfg.getLoginLockoutSeconds();
        int rateWindow = cfg.getLoginRateWindowSeconds();
        String ipLockKey = "admin_login_lockout_ip:" + clientIp;
        if (getAndIncrement("admin_login_rate_ip:" + clientIp, rateWindow) > maxAttemptsPerIp) {
            cacheDao.put(ipLockKey, "locked", "rate_limit", lockoutDuration);
        }
        String accountLockKey = "admin_login_lockout_acct:" + username.toLowerCase() + "|" + clientIp;
        if (getAndIncrement("admin_login_rate_acct:" + username.toLowerCase() + "|" + clientIp,
                rateWindow) > maxAttemptsPerAccount) {
            cacheDao.put(accountLockKey, "locked", "rate_limit", lockoutDuration);
        }
    }

    public void recordSuccess(String username, String clientIp) {
        String key = username.toLowerCase() + "|" + clientIp;
        cacheDao.delete("admin_login_rate_acct:" + key);
        cacheDao.delete("admin_login_lockout_acct:" + key);
    }

    private int getAndIncrement(String key, int rateWindow) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        try {
            int count = cacheDao.get(key) == null ? 0 : Integer.parseInt(cacheDao.get(key));
            count++;
            cacheDao.put(key, String.valueOf(count), "rate_counter", rateWindow);
            return count;
        } finally {
            lock.unlock();
        }
    }
}
