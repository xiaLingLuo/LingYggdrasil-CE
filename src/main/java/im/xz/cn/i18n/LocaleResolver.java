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

import java.util.Locale;

public final class LocaleResolver {
    public static final String[] SUPPORTED = I18n.supportedLocales().stream()
            .map(I18n.LocaleOption::code)
            .toArray(String[]::new);

    private LocaleResolver() {
    }

    public static String normalize(String tag) {
        if (tag == null || tag.isBlank()) return I18n.DEFAULT_LOCALE;
        String normalized = tag.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        for (String supported : SUPPORTED) {
            if (supported.toLowerCase(Locale.ROOT).equals(normalized)) return supported;
        }
        String primary = normalized.split("-")[0];
        for (String supported : SUPPORTED) {
            String lower = supported.toLowerCase(Locale.ROOT);
            if (lower.equals(primary) || lower.startsWith(primary + "-")) return supported;
        }
        return I18n.DEFAULT_LOCALE;
    }

    public static String fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) return null;
        for (String part : header.split(",")) {
            String tag = part.split(";")[0].trim();
            if (tag.isEmpty()) continue;
            String primary = tag.replace('_', '-').toLowerCase(Locale.ROOT).split("-")[0];
            for (String supported : SUPPORTED) {
                String lower = supported.toLowerCase(Locale.ROOT);
                if (lower.equals(primary) || lower.startsWith(primary + "-")) return supported;
            }
        }
        return null;
    }

    public static boolean isSupported(String locale) {
        for (String supported : SUPPORTED) {
            if (supported.equals(locale)) return true;
        }
        return false;
    }
}
