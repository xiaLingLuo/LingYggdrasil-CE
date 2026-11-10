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
package im.xz.cn.plugin;

import im.xz.cn.permission.PermissionNode;
import im.xz.cn.permission.PermissionType;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PluginYaml {

    private static final Set<String> NAME_BLACKLIST = Set.of(
            "lingyggdrasil",
            "lingyggdrasil-main",
            "lingyggdrasil-api",
            "yggdrasil",
            "yggdrasil-main",
            "yggdrasil-api",
            "ling"
    );

    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_.-]+$");

    private static final java.util.regex.Pattern PERM_KEY_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_.-]+$");

    private PluginYaml() {}

    public record ParseResult(PluginDescriptor descriptor, String error) {
        public boolean ok() {
            return descriptor != null;
        }

        static ParseResult fail(String error) {
            return new ParseResult(null, error);
        }
    }

    @SuppressWarnings("unchecked")
    public static ParseResult parse(Path jarPath) {
        String fileName = jarPath.getFileName().toString();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) entry = jar.getJarEntry("plugin.yaml");
            if (entry == null) {
                return ParseResult.fail("plugin.yml not found");
            }
            String yamlText;
            try (InputStream is = jar.getInputStream(entry)) {
                yamlText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            Object loaded;
            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                loaded = yaml.load(yamlText);
            } catch (Exception e) {
                return ParseResult.fail("plugin.yml parse error: " + e.getMessage());
            }
            if (!(loaded instanceof Map)) {
                return ParseResult.fail("plugin.yml is empty or malformed");
            }
            Map<String, Object> root = (Map<String, Object>) loaded;

            String name = str(root.get("name"));
            if (name == null) return ParseResult.fail("missing required field: name");
            if (!NAME_PATTERN.matcher(name).matches()) {
                return ParseResult.fail("invalid plugin name: " + name);
            }
            if (NAME_BLACKLIST.contains(name.toLowerCase(Locale.ROOT))) {
                return ParseResult.fail("reserved plugin name: " + name);
            }

            String version = str(root.get("ver"));
            if (version == null) return ParseResult.fail("missing required field: ver");

            String main = str(root.get("main"));
            if (main == null) return ParseResult.fail("missing required field: main");

            String apiVer = str(root.get("apiVer"));
            if (apiVer == null) return ParseResult.fail("missing required field: apiVer");

            Boolean hotReloadable = asBoolean(root.get("hotReloadable"));
            if (hotReloadable == null) {
                return ParseResult.fail("missing required field: hotReloadable");
            }

            String friendlyName = str(root.get("friendlyName"));
            String website = str(root.get("website"));
            String description = str(root.get("description"));
            String iconPath = str(root.get("icon"));

            List<String> authors = stringList(root.get("authors"));
            List<String> depend = stringList(root.get("depend"));
            List<String> softdepend = stringList(root.get("softdepend"));

            String source = (friendlyName == null || friendlyName.isBlank()) ? name : friendlyName;
            List<PermissionNode> permissions = parsePermissions(root.get("perms"), source);

            PluginDescriptor descriptor = new PluginDescriptor(
                    name, version, main, apiVer, hotReloadable,
                    friendlyName, authors, website, description, iconPath,
                    depend, softdepend, permissions, jarPath, fileName);
            return new ParseResult(descriptor, null);
        } catch (Exception e) {
            return ParseResult.fail("failed to read jar: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<PermissionNode> parsePermissions(Object raw, String source) {
        List<PermissionNode> nodes = new ArrayList<>();
        if (!(raw instanceof Map)) return nodes;
        for (Map.Entry<String, Object> e : ((Map<String, Object>) raw).entrySet()) {
            String key = e.getKey() == null ? null : e.getKey().trim();
            if (key == null || key.isEmpty()) continue;
            if (!PERM_KEY_PATTERN.matcher(key).matches()) {
                PluginSystemLog.get().warn("[Plugin] Ignored invalid permission key '{}' in plugin.yml", key);
                continue;
            }
            String description = null;
            PermissionType type = PermissionType.ADMIN;
            if (e.getValue() instanceof Map) {
                Map<String, Object> meta = (Map<String, Object>) e.getValue();
                description = str(meta.get("description"));
                String typeRaw = str(meta.get("type"));
                if (typeRaw != null) {
                    type = "user".equalsIgnoreCase(typeRaw) ? PermissionType.USER : PermissionType.ADMIN;
                }
            }
            nodes.add(new PermissionNode(key, type, null, source, description, false));
        }
        return nodes;
    }

    private static String str(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(s.trim())) return Boolean.FALSE;
        }
        return null;
    }

    private static List<String> stringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) result.add(s);
                }
            }
        } else if (value instanceof String s && !s.isBlank()) {
            result.add(s.trim());
        }
        return result;
    }
}
