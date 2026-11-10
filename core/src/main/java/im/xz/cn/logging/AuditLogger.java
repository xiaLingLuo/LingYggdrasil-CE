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

public class AuditLogger {

    private AuditLogger() {
    }

    public static void logLogin(String user, String ip, boolean success) {
        if (success) {
            write("INFO", "LOGIN_SUCCESS user=" + user + " ip=" + ip);
        } else {
            write("WARN", "LOGIN_FAILURE user=" + user + " ip=" + ip);
        }
    }

    public static void logLogout(String user, String ip) {
        write("INFO", "LOGOUT user=" + user + " ip=" + ip);
    }

    public static void logPasswordChange(String user, String ip) {
        write("INFO", "PASSWORD_CHANGE user=" + user + " ip=" + ip);
    }

    public static void logPermissionChange(String admin, String target, String action, String ip) {
        write("INFO", "PERMISSION_CHANGE admin=" + admin + " target=" + target + " action=" + action + " ip=" + ip);
    }

    public static void logSensitiveOperation(String user, String action, String ip) {
        write("INFO", "SENSITIVE_OPERATION user=" + user + " action=" + action + " ip=" + ip);
    }

    private static void write(String level, String message) {
        ServiceLog.write(ServiceLog.AUDIT, level, "AuditLogger", message);
    }
}
