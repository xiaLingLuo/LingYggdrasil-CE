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


import im.xz.cn.i18n.I18n;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;

import java.util.Locale;

public class LoginRateLimiter {

    private final CacheDao cacheDao;

    public LoginRateLimiter(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }

    public String checkRateLimit(String username, String clientIp) {
        SystemConfig cfg = SystemConfig.getInstance();
        int maxAttemptsPerIp = cfg.getLoginMaxAttemptsPerIp();
        int maxAttemptsPerAccount = cfg.getLoginMaxAttemptsPerAccount();
        int lockoutDuration = cfg.getLoginLockoutSeconds();
        int rateWindow = cfg.getLoginRateWindowSeconds();
        String ipLockKey = "login_lockout_ip:" + clientIp;
        if (cacheDao.get(ipLockKey) != null) {
            return I18n.t("msg.loginTooFrequent");
        }
        String accountLockKey = "login_lockout_acct:" + username.toLowerCase(Locale.ROOT);
        if (cacheDao.get(accountLockKey) != null) {
            return I18n.t("msg.accountTempLocked");
        }
        String ipRateKey = "login_rate_ip:" + clientIp;
        int ipCount = cacheDao.incrementAndGet(ipRateKey, "rate_counter", rateWindow, true);
        if (ipCount > maxAttemptsPerIp) {
            cacheDao.put(ipLockKey, "locked", "rate_limit", lockoutDuration);
            return I18n.t("msg.loginTooFrequent");
        }
        String accountRateKey = "login_rate_acct:" + username.toLowerCase(Locale.ROOT);
        int accountCount = cacheDao.incrementAndGet(accountRateKey, "rate_counter", rateWindow, true);
        if (accountCount > maxAttemptsPerAccount) {
            cacheDao.put(accountLockKey, "locked", "rate_limit", lockoutDuration);
            return I18n.t("msg.accountLockedTooMany");
        }
        return null;
    }

    public void recordSuccess(String username) {
        String accountRateKey = "login_rate_acct:" + username.toLowerCase(Locale.ROOT);
        String accountLockKey = "login_lockout_acct:" + username.toLowerCase(Locale.ROOT);
        cacheDao.delete(accountRateKey);
        cacheDao.delete(accountLockKey);
    }
}
