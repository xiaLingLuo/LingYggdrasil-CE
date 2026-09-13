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
package im.xz.cn.i18n;

public final class LocaleContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private LocaleContext() {
    }

    public static void set(String locale) {
        CURRENT.set(locale);
    }

    public static String get() {
        String value = CURRENT.get();
        return value == null ? I18n.DEFAULT_LOCALE : value;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
