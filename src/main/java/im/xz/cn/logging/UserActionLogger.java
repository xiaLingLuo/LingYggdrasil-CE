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
package im.xz.cn.logging;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.UserLogDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class UserActionLogger {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile UserLogDao dao;

    private UserActionLogger() {}

    public static void init(UserLogDao userLogDao) {
        dao = userLogDao;
    }

    public static void log(String userId, String ip, String actionKey) {
        UserLogDao store = dao;
        if (store == null || userId == null || userId.isBlank() || actionKey == null) return;
        SystemConfig sc = SystemConfig.getInstance();
        if (!sc.isUserActionLogEnabled()) return;
        if (!sc.isUserActionLoggable(actionKey)) return;
        try {
            String label = im.xz.cn.i18n.I18n.t("userlog." + actionKey);
            String safeIp = (ip == null || ip.isBlank()) ? "-" : ip.replaceAll("[\\r\\n\\t]", "").trim();
            String message = label + " 于 " + safeIp;
            store.insert(userId, actionKey, message, LocalDateTime.now().format(TS));

            int days = sc.getUserActionLogRetentionDays();
            if (days > 0) {
                store.deleteBefore(userId, LocalDate.now().minusDays(days).atStartOfDay().format(TS));
            }
            long maxBytes = Math.max(1, sc.getUserActionLogMaxKib()) * 1024L;
            store.deleteOldestToFit(userId, maxBytes);
        } catch (Exception ignored) {
        }
    }

    public static long sizeBytes(String userId) {
        UserLogDao store = dao;
        if (store == null || userId == null || userId.isBlank()) return 0;
        try {
            return store.totalBytes(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void clear(String userId) {
        UserLogDao store = dao;
        if (store == null || userId == null || userId.isBlank()) return;
        try {
            store.clear(userId);
        } catch (Exception ignored) {
        }
    }

    public static String read(String userId) {
        UserLogDao store = dao;
        if (store == null || userId == null || userId.isBlank()) return "";
        try {
            List<Map<String, Object>> rows = store.findByUserId(userId);
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : rows) {
                sb.append('[').append(row.get("created_at")).append(']').append(row.get("message")).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
