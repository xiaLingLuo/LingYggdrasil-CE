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

import java.security.SecureRandom;

public class VerificationCode {
    private static final int CODE_LENGTH = 8;
    private static final int EXPIRY_SECONDS = 300;

    public static String generate() {
        SecureRandom random = new SecureRandom();
        return String.format("%08d", random.nextInt(100000000));
    }

    public static int getExpirySeconds() {
        return EXPIRY_SECONDS;
    }
}
