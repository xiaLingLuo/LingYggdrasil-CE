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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class I18n {
    private static final logApi log = logApi.getLogger(I18n.class);

    public record LocaleOption(String code, String label) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Map<String, Object>> BUILTIN = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> EXTERNAL = new ConcurrentHashMap<>();
    private static final Map<String, String> HOME_TEMPLATES = new ConcurrentHashMap<>();

    public static final String HOME_TEMPLATE_DIR = "index-page";
    private static final String RESOURCE_PREFIX = "/i18n/user/";

    private static volatile List<LocaleOption> resolvedLocales;
    private static volatile String resolvedDefault;

    private I18n() {
    }

    public static File externalDir() {
        return new File(System.getProperty("user.dir"), "i18n/user");
    }

    public static void releaseBundles() {
        try {
            File root = new File(System.getProperty("user.dir"), "i18n");
            if (!root.exists() && !root.mkdirs()) {
                throw new IOException("Failed to create " + root.getAbsolutePath());
            }
            File languageFile = new File(root, "lang.json");
            if (!languageFile.exists()) {
                try (InputStream input = I18n.class.getResourceAsStream("/i18n/lang.json")) {
                    if (input == null) throw new IOException("Missing bundled /i18n/lang.json");
                    Files.copy(input, languageFile.toPath());
                }
            }
            try {
                LanguageRegistry.load(languageFile);
            } catch (IllegalStateException e) {
                log.warn("[i18n] Invalid external lang.json ({}), falling back to bundled language list", e.getMessage());
                LanguageRegistry.loadBundled();
            }
            File userDir = externalDir();
            File adminDir = new File(root, "admin");
            resolvedLocales = null;
            resolvedDefault = null;
            List<LocaleOption> locales = supportedLocales();
            migrateLegacyBundles(root, userDir, adminDir, locales);
            releaseBundlesTo(userDir, true, locales);
            releaseBundlesTo(adminDir, false, locales);
            migrateUpdatedDefaultText(userDir, adminDir, locales);
        } catch (Exception e) {
            log.error("[i18n] Cannot initialize language configuration: {}", e.getMessage(), e);
            throw new IllegalStateException("无法初始化语言配置，请检查运行目录下的 i18n/lang.json：" + e.getMessage(), e);
        }
    }

    private static void migrateLegacyBundles(File root, File userDir, File adminDir,
                                             List<LocaleOption> locales) throws IOException {
        if (!userDir.exists() && !userDir.mkdirs()) {
            throw new IOException("Failed to create " + userDir.getAbsolutePath());
        }
        if (!adminDir.exists() && !adminDir.mkdirs()) {
            throw new IOException("Failed to create " + adminDir.getAbsolutePath());
        }
        File legacyPages = new File(root, HOME_TEMPLATE_DIR);
        File userPages = new File(userDir, HOME_TEMPLATE_DIR);
        if (legacyPages.isDirectory() && (!userPages.exists() && !userPages.mkdirs())) {
            throw new IOException("Failed to create " + userPages.getAbsolutePath());
        }
        for (LocaleOption opt : locales) {
            File legacy = new File(root, opt.code() + ".json");
            if (legacy.isFile()) {
                copyIfMissing(legacy, new File(userDir, opt.code() + ".json"));
                copyIfMissing(legacy, new File(adminDir, opt.code() + ".json"));
            }
            File legacyPage = new File(legacyPages, opt.code() + ".html");
            if (legacyPage.isFile()) {
                copyIfMissing(legacyPage, new File(userPages, opt.code() + ".html"));
            }
        }
    }

    
    private static void migrateUpdatedDefaultText(File userDir, File adminDir,
                                                  List<LocaleOption> locales) {
        String[][] replacements = {
                {"注销仅停用登录并对外匿名展示，账户、角色、纹理、好友与授权数据会保留；本版本不提供自助恢复。",
                        "这将会使此账户被永久注销，但{SiteName}仍然有权保留你的账户数据"},
                {"按分区修改系统配置；保存后立即应用，敏感值不会回显。",
                        "修改 LingYggdrasil 的系统设置"}
        };
        for (File dir : new File[]{userDir, adminDir}) {
            for (LocaleOption locale : locales) {
                File file = new File(dir, locale.code() + ".json");
                if (!file.isFile()) {
                    continue;
                }
                try {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    String updated = content;
                    for (String[] pair : replacements) {
                        updated = updated.replace(pair[0], pair[1]);
                    }
                    if (!updated.equals(content)) {
                        Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
                    }
                } catch (Exception error) {
                    log.warn("[i18n] Cannot update default text in {}: {}", file.getName(), error.getMessage());
                }
            }
        }
    }

    private static void releaseBundlesTo(File dir, boolean includeHomeTemplates, List<LocaleOption> locales) throws IOException {
        String resourcePrefix = includeHomeTemplates ? "/i18n/user/" : "/i18n/admin/";
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create " + dir.getAbsolutePath());
        }
        File pageDir = new File(dir, HOME_TEMPLATE_DIR);
        if (includeHomeTemplates && !pageDir.exists() && !pageDir.mkdirs()) {
            throw new IOException("Failed to create " + pageDir.getAbsolutePath());
        }
        for (LocaleOption opt : locales) {
            File target = new File(dir, opt.code() + ".json");
            if (!target.exists()) {
                try (InputStream is = I18n.class.getResourceAsStream(resourcePrefix + opt.code() + ".json")) {
                    if (is != null) Files.copy(is, target.toPath());
                }
            }
            if (includeHomeTemplates) {
                File pageTarget = new File(pageDir, opt.code() + ".html");
                if (!pageTarget.exists()) {
                    try (InputStream is = I18n.class.getResourceAsStream(RESOURCE_PREFIX + HOME_TEMPLATE_DIR + "/" + opt.code() + ".html")) {
                        if (is != null) Files.copy(is, pageTarget.toPath());
                    }
                }
            }
        }
    }

    private static void copyIfMissing(File source, File target) throws IOException {
        if (!target.exists()) {
            Files.copy(source.toPath(), target.toPath());
        }
    }

    public static List<LocaleOption> supportedLocales() {
        resolveLocales();
        return resolvedLocales;
    }

    public static String defaultLocale() {
        resolveLocales();
        return resolvedDefault;
    }

    private static void resolveLocales() {
        if (resolvedLocales != null) return;
        synchronized (I18n.class) {
            if (resolvedLocales != null) return;
            List<LocaleOption> available = new ArrayList<>();
            for (LanguageRegistry.Language language : LanguageRegistry.languages()) {
                if (hasBundle(language.code())) {
                    available.add(new LocaleOption(language.code(), language.name()));
                }
            }
            if (available.isEmpty()) {
                for (LanguageRegistry.Language language : LanguageRegistry.languages()) {
                    available.add(new LocaleOption(language.code(), language.name()));
                }
            }
            String defaultCode = LanguageRegistry.defaultLocale();
            boolean defaultAvailable = available.stream().anyMatch(option -> option.code().equals(defaultCode));
            resolvedDefault = defaultAvailable ? defaultCode : available.get(0).code();
            resolvedLocales = List.copyOf(available);
        }
    }

    private static boolean hasBundle(String code) {
        if (I18n.class.getResource(RESOURCE_PREFIX + code + ".json") != null) return true;
        return new File(externalDir(), code + ".json").isFile();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> builtinBundle(String locale) {
        locale = normalizeLocale(locale);
        final String resolvedLocale = locale;
        return BUILTIN.computeIfAbsent(locale, loc -> {
            try (InputStream is = I18n.class.getResourceAsStream(RESOURCE_PREFIX + resolvedLocale + ".json")) {
                if (is == null) return Map.of();
                return MAPPER.readValue(is, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> externalBundle(String locale) {
        locale = normalizeLocale(locale);
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
        if (value == null) value = resolve(defaultLocale(), key);
        if (value == null) return key;
        return interpolate(value, args);
    }

    public static String t(String key, Object... args) {
        return translate(LocaleContext.get(), key, args);
    }

    public static String tOrNull(String key) {
        String value = resolve(LocaleContext.get(), key);
        if (value == null) value = resolve(defaultLocale(), key);
        return value;
    }

    public static String rawJson(String locale) {
        try {
            Map<String, Object> merged = bundle(locale);
            if (merged.isEmpty()) merged = bundle(defaultLocale());
            return escapeForScript(MAPPER.writeValueAsString(merged));
        } catch (Exception e) {
            return "{}";
        }
    }

    public static String homeTemplate(String locale) {
        String template = loadHomeTemplate(locale);
        if (template == null) template = loadHomeTemplate(defaultLocale());
        return template == null ? "" : template;
    }

    private static String loadHomeTemplate(String locale) {
        if (locale == null || locale.isBlank()) return null;
        locale = normalizeLocale(locale);
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
            try (InputStream is = I18n.class.getResourceAsStream(RESOURCE_PREFIX + HOME_TEMPLATE_DIR + "/" + locale + ".html")) {
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

    private static String normalizeLocale(String locale) {
        return LanguageRegistry.normalize(locale);
    }

    private static String escapeForScript(String json) {
        return json.replace("<", "\\u003C")
                .replace(">", "\\u003E")
                .replace("&", "\\u0026")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
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
