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
package im.xz.cn.web;

import java.security.SecureRandom;
import java.util.Base64;

public final class Csp {

    private static final ThreadLocal<String> NONCE = new ThreadLocal<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csp() {}

    public static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String nonce = Base64.getEncoder().encodeToString(bytes);
        NONCE.set(nonce);
        return nonce;
    }

    public static String current() {
        return NONCE.get();
    }

    public static void clear() {
        NONCE.remove();
    }
}
