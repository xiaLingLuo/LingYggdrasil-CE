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
package im.xz.cn.common;

import io.javalin.http.Context;


public final class IpUtil {

    private IpUtil() {}

    public static String getClientIp(Context ctx) {
        String remote = ctx.req().getRemoteAddr();
        if (remote == null || remote.isBlank()) {
            remote = "unknown";
        }
        if (!isTrustedProxy(remote)) {
            return remote;
        }
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            
            String[] parts = forwarded.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = normalizeIp(parts[i]);
                if (candidate != null && !isTrustedProxy(candidate)) {
                    return candidate;
                }
            }
        }
        String realIp = normalizeIp(ctx.header("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return remote;
    }

    
    static boolean isTrustedProxy(String ip) {
        String value = normalizeIp(ip);
        if (value == null) {
            return false;
        }
        if (value.equals("::1")) {
            return true;
        }
        if (value.startsWith("127.")) {
            return true;
        }
        if (value.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            String[] octets = value.split("\\.");
            int a = Integer.parseInt(octets[0]);
            int b = Integer.parseInt(octets[1]);
            if (a == 10) return true;
            if (a == 172 && b >= 16 && b <= 31) return true;
            if (a == 192 && b == 168) return true;
            return false;
        }
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        
        return lower.startsWith("fc") || lower.startsWith("fd")
                || lower.startsWith("fe8") || lower.startsWith("fe9")
                || lower.startsWith("fea") || lower.startsWith("feb");
    }

    
    static String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 64) {
            return null;
        }
        
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end <= 0) return null;
            value = value.substring(1, end);
        } else if (value.matches("\\d{1,3}(\\.\\d{1,3}){3}:\\d+")) {
            value = value.substring(0, value.lastIndexOf(':'));
        }
        if (value.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            for (String octet : value.split("\\.")) {
                int number = Integer.parseInt(octet);
                if (number < 0 || number > 255) {
                    return null;
                }
            }
            return value;
        }
        if (value.contains(":") && value.matches("[0-9a-fA-F:.%]+")) {
            return value;
        }
        return null;
    }
}
