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
package im.xz.cn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private AuditLogger() {
    }

    public static void logLogin(String user, String ip, boolean success) {
        if (success) {
            logger.info("[AUDIT] LOGIN_SUCCESS user={} ip={}", user, ip);
        } else {
            logger.warn("[AUDIT] LOGIN_FAILURE user={} ip={}", user, ip);
        }
    }

    public static void logLogout(String user, String ip) {
        logger.info("[AUDIT] LOGOUT user={} ip={}", user, ip);
    }

    public static void logPasswordChange(String user, String ip) {
        logger.info("[AUDIT] PASSWORD_CHANGE user={} ip={}", user, ip);
    }

    public static void logPermissionChange(String admin, String target, String action, String ip) {
        logger.info("[AUDIT] PERMISSION_CHANGE admin={} target={} action={} ip={}", admin, target, action, ip);
    }

    public static void logSensitiveOperation(String user, String action, String ip) {
        logger.info("[AUDIT] SENSITIVE_OPERATION user={} action={} ip={}", user, action, ip);
    }
}
