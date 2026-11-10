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
package im.xz.cn.plugin.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.plugin.spi.Plugin;
import im.xz.cn.plugin.spi.PluginApiVersion;
import im.xz.cn.plugin.spi.PluginDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public final class PluginJarLoader {

    static final String DESCRIPTOR_ENTRY = "plugin.json";

    public record LoadedPlugin(PluginDescriptor descriptor, Plugin plugin, PluginClassLoader classLoader) {
    }

    public static final class PluginLoadException extends RuntimeException {
        public PluginLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public LoadedPlugin load(Path jarPath) {
        if (jarPath == null || !java.nio.file.Files.isRegularFile(jarPath)) {
            throw new PluginLoadException("plugin jar not found: " + jarPath, null);
        }
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(DESCRIPTOR_ENTRY);
            if (entry == null) {
                throw new PluginLoadException("missing " + DESCRIPTOR_ENTRY + " in " + jarPath, null);
            }
            PluginDescriptor descriptor;
            try (InputStream input = jar.getInputStream(entry)) {
                descriptor = parseDescriptor(input);
            }
            if (!isApiCompatible(descriptor.apiVersion())) {
                throw new PluginLoadException("plugin '" + descriptor.id() + "' requires API "
                        + descriptor.apiVersion() + " but runtime provides " + PluginApiVersion.CURRENT, null);
            }
            URL jarUrl = jarPath.toUri().toURL();
            PluginClassLoader classLoader = new PluginClassLoader(jarUrl,
                    PluginJarLoader.class.getClassLoader());
            try {
                Class<?> main = Class.forName(descriptor.mainClass(), true, classLoader);
                if (!Plugin.class.isAssignableFrom(main)) {
                    throw new PluginLoadException("plugin main class does not implement Plugin: "
                            + descriptor.mainClass(), null);
                }
                Plugin plugin = (Plugin) main.getDeclaredConstructor().newInstance();
                return new LoadedPlugin(descriptor, plugin, classLoader);
            } catch (ReflectiveOperationException | RuntimeException error) {
                classLoader.close();
                throw error;
            }
        } catch (PluginLoadException error) {
            throw error;
        } catch (ReflectiveOperationException | IOException error) {
            throw new PluginLoadException("failed to load plugin jar " + jarPath + ": " + error.getMessage(), error);
        }
    }

    PluginDescriptor parseDescriptor(InputStream input) throws IOException {
        JsonNode node = mapper.readTree(input);
        if (node == null || !node.isObject()) {
            throw new PluginLoadException("plugin.json must be a JSON object", null);
        }
        return new PluginDescriptor(
                text(node, "id"),
                text(node, "name"),
                text(node, "version"),
                text(node, "mainClass"),
                text(node, "apiVersion"),
                text(node, "author"),
                text(node, "description"),
                list(node, "dependencies"),
                list(node, "permissions"));
    }

    
    public static boolean isApiCompatible(String pluginApiVersion) {
        int[] required = parse(pluginApiVersion);
        int[] current = parse(PluginApiVersion.CURRENT);
        if (required[0] != current[0]) {
            return false;
        }
        return required[1] < current[1]
                || (required[1] == current[1] && required[2] <= current[2]);
    }

    private static int[] parse(String version) {
        String[] parts = version.split("[.-]");
        int[] numbers = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static List<String> list(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> result.add(item.asText()));
        return List.copyOf(result);
    }
}
