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

import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.logging.logApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class I18n {
    public static final String DEFAULT_LOCALE = "zh-CN";

    private static final logApi log = logApi.getLogger(I18n.class);

    public record LocaleOption(String code, String label) {
    }

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
            new LocaleOption("ko-KR", "한국어")
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Map<String, Object>> BUILTIN = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> EXTERNAL = new ConcurrentHashMap<>();
    private static final Map<String, String> HOME_TEMPLATES = new ConcurrentHashMap<>();

    public static final String HOME_TEMPLATE_DIR = "index-page";

    private I18n() {
    }

    public static File externalDir() {
        return new File(System.getProperty("user.dir"), "i18n");
    }

    public static void releaseBundles() {
        try {
            File dir = externalDir();
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("[i18n] Failed to create external i18n directory: {}", dir.getAbsolutePath());
                return;
            }
            File pageDir = new File(dir, HOME_TEMPLATE_DIR);
            if (!pageDir.exists() && !pageDir.mkdirs()) {
                log.warn("[i18n] Failed to create external home template directory: {}", pageDir.getAbsolutePath());
            }
            for (LocaleOption opt : SUPPORTED_LOCALES) {
                File target = new File(dir, opt.code() + ".json");
                if (!target.exists()) {
                    try (InputStream is = I18n.class.getResourceAsStream("/i18n/" + opt.code() + ".json")) {
                        if (is != null) {
                            Files.copy(is, target.toPath());
                            log.info("[i18n] Released bundle {}", target.getName());
                        }
                    }
                }
                File pageTarget = new File(pageDir, opt.code() + ".html");
                if (!pageTarget.exists()) {
                    try (InputStream is = I18n.class.getResourceAsStream("/i18n/" + HOME_TEMPLATE_DIR + "/" + opt.code() + ".html")) {
                        if (is != null) {
                            Files.copy(is, pageTarget.toPath());
                            log.info("[i18n] Released home template {}", pageTarget.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[i18n] Failed to release bundles: {}", e.getMessage());
        }
    }

    public static List<LocaleOption> supportedLocales() {
        return SUPPORTED_LOCALES;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> builtinBundle(String locale) {
        return BUILTIN.computeIfAbsent(locale, loc -> {
            try (InputStream is = I18n.class.getResourceAsStream("/i18n/" + loc + ".json")) {
                if (is == null) return Map.of();
                return MAPPER.readValue(is, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> externalBundle(String locale) {
        File ext = new File(externalDir(), locale + ".json");
        if (!ext.isFile()) return Map.of();
        return EXTERNAL.computeIfAbsent(locale, loc -> {
            try (InputStream is = new FileInputStream(ext)) {
                return MAPPER.readValue(is, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    private static Map<String, Object> bundle(String locale) {
        Map<String, Object> external = externalBundle(locale);
        if (external.isEmpty()) return builtinBundle(locale);
        return deepMerge(builtinBundle(locale), external);
    }

    public static String translate(String locale, String key, Object... args) {
        String value = resolve(locale, key);
        if (value == null) value = resolve(DEFAULT_LOCALE, key);
        if (value == null) return key;
        return interpolate(value, args);
    }

    public static String t(String key, Object... args) {
        return translate(LocaleContext.get(), key, args);
    }

    public static String tOrNull(String key) {
        String value = resolve(LocaleContext.get(), key);
        if (value == null) value = resolve(DEFAULT_LOCALE, key);
        return value;
    }

    public static String rawJson(String locale) {
        try {
            Map<String, Object> merged = bundle(locale);
            if (merged.isEmpty()) merged = bundle(DEFAULT_LOCALE);
            return MAPPER.writeValueAsString(merged);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String homeTemplate(String locale) {
        String template = loadHomeTemplate(locale);
        if (template == null) template = loadHomeTemplate(DEFAULT_LOCALE);
        return template == null ? "" : template;
    }

    private static String loadHomeTemplate(String locale) {
        if (locale == null || locale.isBlank()) return null;
        String cached = HOME_TEMPLATES.get(locale);
        if (cached != null) return cached.isEmpty() ? null : cached;
        String content = null;
        File ext = new File(new File(externalDir(), HOME_TEMPLATE_DIR), locale + ".html");
        if (ext.isFile()) {
            try {
                content = Files.readString(ext.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("[i18n] Failed to read home template {}: {}", ext.getAbsolutePath(), e.getMessage());
            }
        }
        if (content == null) {
            try (InputStream is = I18n.class.getResourceAsStream("/i18n/" + HOME_TEMPLATE_DIR + "/" + locale + ".html")) {
                if (is != null) content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("[i18n] Failed to read built-in home template {}: {}", locale, e.getMessage());
            }
        }
        HOME_TEMPLATES.put(locale, content == null ? "" : content);
        return content;
    }

    @SuppressWarnings("unchecked")
    private static String resolve(String locale, String key) {
        return resolveIn(bundle(locale), key);
    }

    @SuppressWarnings("unchecked")
    private static String resolveIn(Map<String, Object> bundle, String key) {
        Object node = bundle;
        for (String part : key.split("\\.")) {
            if (!(node instanceof Map)) return null;
            node = ((Map<String, Object>) node).get(part);
        }
        return node instanceof String ? (String) node : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> result = new LinkedHashMap<>(base);
        for (Map.Entry<String, Object> e : override.entrySet()) {
            Object baseVal = result.get(e.getKey());
            Object overVal = e.getValue();
            if (baseVal instanceof Map && overVal instanceof Map) {
                result.put(e.getKey(), deepMerge((Map<String, Object>) baseVal, (Map<String, Object>) overVal));
            } else {
                result.put(e.getKey(), overVal);
            }
        }
        return result;
    }

    private static String interpolate(String template, Object... args) {
        if (args == null || args.length == 0) return template;
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "" : String.valueOf(args[i]));
        }
        return result;
    }
}
