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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class LanguageRegistry {
    public record Language(String code, String name) {}

    private record RegistryState(String defaultLocale, List<Language> languages, Map<String, String> names) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern LANGUAGE_CODE = Pattern.compile("^[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)*$");
    private static volatile RegistryState state;

    private LanguageRegistry() {}

    public static synchronized void load(File file) {
        try (InputStream input = new FileInputStream(file)) {
            state = parse(MAPPER.readTree(input));
        } catch (Exception e) {
            throw new IllegalStateException("无法读取 i18n/lang.json：" + e.getMessage(), e);
        }
    }

    public static synchronized void loadBundled() {
        try (InputStream input = LanguageRegistry.class.getResourceAsStream("/i18n/lang.json")) {
            if (input == null) throw new IllegalStateException("缺少打包的 i18n/lang.json 语言清单");
            state = parse(MAPPER.readTree(input));
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) throw illegalStateException;
            throw new IllegalStateException("无法读取内置 i18n/lang.json：" + e.getMessage(), e);
        }
    }

    public static List<Language> languages() {
        return current().languages();
    }

    public static String defaultLocale() {
        return current().defaultLocale();
    }

    public static boolean isSupported(String locale) {
        if (locale == null || locale.isBlank()) return false;
        String normalized = locale.trim().replace('_', '-');
        return current().names().keySet().stream().anyMatch(code -> code.equalsIgnoreCase(normalized));
    }

    public static String normalize(String locale) {
        String match = match(locale);
        return match == null ? defaultLocale() : match;
    }

    public static String match(String locale) {
        if (locale == null || locale.isBlank()) return null;
        RegistryState current = current();
        String normalized = locale.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        for (Language language : current.languages()) {
            if (language.code().toLowerCase(Locale.ROOT).equals(normalized)) return language.code();
        }
        String primary = normalized.split("-")[0];
        for (Language language : current.languages()) {
            String code = language.code().toLowerCase(Locale.ROOT);
            if (code.equals(primary) || code.startsWith(primary + "-")) return language.code();
        }
        return null;
    }

    private static RegistryState current() {
        RegistryState loaded = state;
        if (loaded != null) return loaded;
        synchronized (LanguageRegistry.class) {
            if (state != null) return state;
            try (InputStream input = LanguageRegistry.class.getResourceAsStream("/i18n/lang.json")) {
                if (input == null) throw new IllegalStateException("缺少打包的 i18n/lang.json 语言清单");
                state = parse(MAPPER.readTree(input));
                return state;
            } catch (Exception e) {
                if (e instanceof IllegalStateException illegalStateException) throw illegalStateException;
                throw new IllegalStateException("无法加载 i18n/lang.json：" + e.getMessage(), e);
            }
        }
    }

    private static RegistryState parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalStateException("i18n/lang.json 顶层必须是 JSON 对象");
        }
        JsonNode defaultNode = root.get("default");
        if (defaultNode == null || !defaultNode.isTextual() || defaultNode.asText().isBlank()) {
            throw new IllegalStateException("i18n/lang.json 必须设置非空的 default 语言代码");
        }
        JsonNode languagesNode = root.get("languages");
        if (languagesNode == null || !languagesNode.isObject() || languagesNode.isEmpty()) {
            throw new IllegalStateException("i18n/lang.json 的 languages 至少需要配置一种语言");
        }

        List<Language> languages = new ArrayList<>();
        Map<String, String> names = new LinkedHashMap<>();
        var fields = languagesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String code = entry.getKey();
            JsonNode nameNode = entry.getValue();
            if (code.length() > 64 || !LANGUAGE_CODE.matcher(code).matches()) {
                throw new IllegalStateException("i18n/lang.json 包含不安全或无效的语言代码：" + code);
            }
            if (nameNode == null || !nameNode.isTextual()) {
                throw new IllegalStateException("i18n/lang.json 中语言 " + code + " 的名称必须是字符串");
            }
            String name = nameNode.asText();
            languages.add(new Language(code, name));
            names.put(code, name);
        }

        String defaultLocale = defaultNode.asText();
        if (!names.containsKey(defaultLocale)) {
            throw new IllegalStateException("i18n/lang.json 的 default 语言必须存在于 languages 中：" + defaultLocale);
        }
        return new RegistryState(defaultLocale, List.copyOf(languages), Collections.unmodifiableMap(names));
    }
}
