/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package im.xz.cn.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.logging.logApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminI18n {
    public record LocaleOption(String code, String label) {}

    private static final logApi log = logApi.getLogger(AdminI18n.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESOURCE_PREFIX = "/i18n/admin/";
    private static final Map<String, Map<String, Object>> BUILTIN = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> EXTERNAL = new ConcurrentHashMap<>();
    private static final List<LocaleOption> SUPPORTED_LOCALES = List.of(
            new LocaleOption("zh-CN", "简体中文"),
            new LocaleOption("zh-TW", "繁體中文"),
            new LocaleOption("zh-XIA", "中文（華夏）"),
            new LocaleOption("en-US", "English"),
            new LocaleOption("ru-RU", "Русский"),
            new LocaleOption("de-DE", "Deutsch"),
            new LocaleOption("fr-FR", "Français"),
            new LocaleOption("it-IT", "Italiano"),
            new LocaleOption("ja-JP", "日本語"),
            new LocaleOption("ko-KR", "한국어"));

    private AdminI18n() {}

    public static File externalDir() {
        return new File(new File(System.getProperty("user.dir"), "i18n"), "admin");
    }

    public static List<LocaleOption> supportedLocales() {
        return SUPPORTED_LOCALES;
    }

    public static String translate(String locale, String key, Object... args) {
        String value = resolve(locale, key);
        if (value == null) value = resolve(I18n.DEFAULT_LOCALE, key);
        return value == null ? key : interpolate(value, args);
    }

    public static String t(String key, Object... args) {
        return translate(LocaleContext.get(), key, args);
    }

    public static String tOrNull(String key) {
        String value = resolve(LocaleContext.get(), key);
        return value != null ? value : resolve(I18n.DEFAULT_LOCALE, key);
    }

    public static String rawJson(String locale) {
        try {
            return escapeForScript(MAPPER.writeValueAsString(bundle(locale)));
        } catch (Exception e) {
            log.warn("[admin-i18n] Failed to serialize locale {}: {}", locale, e.getMessage());
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> builtinBundle(String locale) {
        locale = normalizeLocale(locale);
        final String resolvedLocale = locale;
        return BUILTIN.computeIfAbsent(locale, loc -> {
            try (InputStream is = AdminI18n.class.getResourceAsStream(RESOURCE_PREFIX + resolvedLocale + ".json")) {
                return is == null ? Map.of() : MAPPER.readValue(is, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> externalBundle(String locale) {
        locale = normalizeLocale(locale);
        File file = new File(externalDir(), locale + ".json");
        if (!file.isFile()) return Map.of();
        return EXTERNAL.computeIfAbsent(locale, loc -> {
            try (InputStream is = new FileInputStream(file)) {
                return MAPPER.readValue(is, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    private static Map<String, Object> bundle(String locale) {
        Map<String, Object> base = builtinBundle(locale);
        Map<String, Object> external = externalBundle(locale);
        return external.isEmpty() ? base : deepMerge(base, external);
    }

    @SuppressWarnings("unchecked")
    private static String resolve(String locale, String key) {
        if (locale == null || locale.isBlank()) locale = I18n.DEFAULT_LOCALE;
        Object node = bundle(locale);
        for (String part : key.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
        }
        return node instanceof String ? (String) node : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>(base);
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            Object oldValue = result.get(entry.getKey());
            Object newValue = entry.getValue();
            result.put(entry.getKey(), oldValue instanceof Map && newValue instanceof Map
                    ? deepMerge((Map<String, Object>) oldValue, (Map<String, Object>) newValue)
                    : newValue);
        }
        return result;
    }

    private static String interpolate(String template, Object... args) {
        if (args == null) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "" : String.valueOf(args[i]));
        }
        return result;
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || !SUPPORTED_LOCALES.stream().anyMatch(option -> option.code().equals(locale))) {
            return I18n.DEFAULT_LOCALE;
        }
        return locale;
    }

    private static String escapeForScript(String json) {
        return json.replace("<", "\\u003C")
                .replace(">", "\\u003E")
                .replace("&", "\\u0026")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }
}
