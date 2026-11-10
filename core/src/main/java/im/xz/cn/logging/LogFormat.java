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

public final class LogFormat {

    private LogFormat() {}

    public static String format(String message, Object... args) {
        if (message == null) return "";
        if (args == null || args.length == 0) return message;
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int cursor = 0;
        while (cursor < message.length()) {
            int idx = message.indexOf("{}", cursor);
            if (idx < 0 || argIndex >= args.length) {
                sb.append(message, cursor, message.length());
                break;
            }
            sb.append(message, cursor, idx);
            sb.append(args[argIndex] == null ? "null" : String.valueOf(args[argIndex]));
            argIndex++;
            cursor = idx + 2;
        }
        return sb.toString();
    }
}
