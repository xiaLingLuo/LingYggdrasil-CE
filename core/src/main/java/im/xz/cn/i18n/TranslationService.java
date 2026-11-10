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

import java.util.List;
import java.util.Locale;
import java.util.Objects;


public final class TranslationService {

    
    public interface Catalog {
        
        String translate(String locale, String key, Object... args);

        boolean supports(String locale);
    }

    private final Catalog catalog;
    private final List<String> supportedLocales;
    private final String defaultLocale;

    public TranslationService(Catalog catalog, List<String> supportedLocales, String defaultLocale) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.supportedLocales = supportedLocales == null ? List.of() : List.copyOf(supportedLocales);
        this.defaultLocale = normalize(defaultLocale);
    }

    public String translate(String requestedLocale, String key, Object... args) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String locale = resolve(requestedLocale);
        String value = catalog.translate(locale, key, args);
        if (usable(value, key)) {
            return value;
        }
        if (defaultLocale != null && !defaultLocale.equals(locale)) {
            String fallback = catalog.translate(defaultLocale, key, args);
            if (usable(fallback, key)) {
                return fallback;
            }
        }
        return key;
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public List<String> supportedLocales() {
        return supportedLocales;
    }

    
    String resolve(String requested) {
        String normalized = normalize(requested);
        if (normalized != null) {
            for (String candidate : supportedLocales) {
                if (normalized.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        return defaultLocale;
    }

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().replace('_', '-');
        String[] parts = value.split("-");
        if (parts.length == 0 || parts[0].isBlank()) {
            return null;
        }
        StringBuilder result = new StringBuilder(parts[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isBlank()) {
                continue;
            }
            result.append('-');
            result.append(formatSubtag(parts[i]));
        }
        return result.toString();
    }

    
    private static String formatSubtag(String part) {
        if (part.length() == 2) {
            return part.toUpperCase(Locale.ROOT);
        }
        if (part.length() == 4) {
            return part.substring(0, 1).toUpperCase(Locale.ROOT)
                    + part.substring(1).toLowerCase(Locale.ROOT);
        }
        return part.toLowerCase(Locale.ROOT);
    }

    private static boolean usable(String value, String key) {
        return value != null && !value.isBlank() && !value.equals(key);
    }
}
