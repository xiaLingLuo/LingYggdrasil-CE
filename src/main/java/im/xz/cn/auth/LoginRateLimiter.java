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

import im.xz.cn.database.CacheDao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final int MAX_ATTEMPTS_PER_ACCOUNT = 5;
    private static final int LOCKOUT_DURATION = 900;
    private static final int RATE_WINDOW = 60;

    private final CacheDao cacheDao;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public LoginRateLimiter(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }

    public String checkRateLimit(String username, String clientIp) {
        String ipLockKey = "login_lockout_ip:" + clientIp;
        if (cacheDao.get(ipLockKey) != null) {
            return "登录尝试过于频繁，请稍后再试";
        }
        String accountLockKey = "login_lockout_acct:" + username.toLowerCase();
        if (cacheDao.get(accountLockKey) != null) {
            return "该账户已临时锁定，请稍后再试";
        }
        String ipRateKey = "login_rate_ip:" + clientIp;
        int ipCount = getAndIncrement(ipRateKey);
        if (ipCount > MAX_ATTEMPTS_PER_IP) {
            cacheDao.put(ipLockKey, "locked", "rate_limit", LOCKOUT_DURATION);
            return "登录尝试过于频繁，请稍后再试";
        }
        String accountRateKey = "login_rate_acct:" + username.toLowerCase();
        int accountCount = getAndIncrement(accountRateKey);
        if (accountCount > MAX_ATTEMPTS_PER_ACCOUNT) {
            cacheDao.put(accountLockKey, "locked", "rate_limit", LOCKOUT_DURATION);
            return "该账户登录失败次数过多，已临时锁定";
        }
        return null;
    }

    public void recordSuccess(String username) {
        String accountRateKey = "login_rate_acct:" + username.toLowerCase();
        String accountLockKey = "login_lockout_acct:" + username.toLowerCase();
        cacheDao.delete(accountRateKey);
        cacheDao.delete(accountLockKey);
    }

    private int getAndIncrement(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            String val = cacheDao.get(key);
            int count = (val != null) ? Integer.parseInt(val) : 0;
            count++;
            cacheDao.put(key, String.valueOf(count), "rate_counter", LoginRateLimiter.RATE_WINDOW);
            return count;
        } finally {
            lock.unlock();
        }
    }
}
