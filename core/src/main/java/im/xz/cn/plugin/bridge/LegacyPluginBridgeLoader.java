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
package im.xz.cn.plugin.bridge;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.logApi;
import im.xz.cn.plugin.api.LingPlugin;
import im.xz.cn.plugin.runtime.PluginHost;
import im.xz.cn.plugin.spi.PluginDescriptor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;


public final class LegacyPluginBridgeLoader {

    private static final logApi log = logApi.getLogger(LegacyPluginBridgeLoader.class);

    public record Loaded(String id, LingPlugin plugin, URLClassLoader classLoader) {
    }

    public record ScanResult(List<Loaded> loaded, List<String> rejected) {

        public ScanResult {
            loaded = List.copyOf(loaded);
            rejected = List.copyOf(rejected);
        }
    }

    public ScanResult registerAll(Path legacyDirectory, PluginHost host, DatabaseManager database) {
        List<Loaded> loaded = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (legacyDirectory == null || !Files.isDirectory(legacyDirectory)) {
            return new ScanResult(loaded, rejected);
        }
        List<Path> jars = new ArrayList<>();
        try (Stream<Path> files = Files.list(legacyDirectory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(jars::add);
        } catch (IOException error) {
            throw new IllegalStateException("cannot scan legacy plugins directory: " + legacyDirectory, error);
        }
        String serverDir = System.getProperty("user.dir");
        for (Path jar : jars) {
            URLClassLoader classLoader = null;
            try {
                classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()},
                        LegacyPluginBridgeLoader.class.getClassLoader());
                Class<?> mainClass = findMainClass(jar, classLoader);
                if (mainClass == null) {
                    rejected.add(jar.getFileName() + ": no LingPlugin subclass");
                    closeQuietly(classLoader);
                    continue;
                }
                LingPlugin instance = (LingPlugin) mainClass.getDeclaredConstructor().newInstance();
                String id = pluginId(jar);
                PluginDescriptor descriptor = new PluginDescriptor(
                        id, id, "1.0.0", mainClass.getName(), "1",
                        "legacy", "legacy plugin loaded through ADR-0018 bridge",
                        List.of(), List.of());
                host.register(descriptor, new LegacyPluginAdapter(
                        instance, database, SystemConfig.getInstance(), new File(serverDir)));
                loaded.add(new Loaded(id, instance, classLoader));
                log.info("[Plugin] legacy plugin bridged: {} ({})", id, jar.getFileName());
            } catch (ReflectiveOperationException | RuntimeException | IOException error) {
                rejected.add(jar.getFileName() + ": " + error.getMessage());
                log.warn("[Plugin] legacy plugin rejected {}: {}", jar.getFileName(), error.getMessage());
                closeQuietly(classLoader);
            }
        }
        return new ScanResult(loaded, rejected);
    }

    private static Class<?> findMainClass(Path jar, ClassLoader classLoader)
            throws IOException, ClassNotFoundException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.contains("$") || name.equals("module-info.class")) {
                    continue;
                }
                String className = name.substring(0, name.length() - ".class".length())
                        .replace('/', '.');
                Class<?> candidate;
                try {
                    candidate = Class.forName(className, false, classLoader);
                } catch (LinkageError | ClassNotFoundException error) {
                    continue;
                }
                if (LingPlugin.class.isAssignableFrom(candidate)
                        && !candidate.isInterface()
                        && !Modifier.isAbstract(candidate.getModifiers())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String pluginId(Path jar) {
        String raw = jar.getFileName().toString();
        if (raw.endsWith(".jar")) {
            raw = raw.substring(0, raw.length() - 4);
        }
        String id = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        if (id.isEmpty() || !Character.isLetter(id.charAt(0))) {
            id = "legacy-" + id;
        }
        if (id.length() < 2) {
            id = id + "p";
        }
        if (id.length() > 64) {
            id = id.substring(0, 64);
        }
        return id;
    }

    private static void closeQuietly(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException error) {
            log.warn("[Plugin] failed to close legacy classloader: {}", error.getMessage());
        }
    }

    public void close(ScanResult result) {
        for (Loaded plugin : result.loaded()) {
            closeQuietly(plugin.classLoader());
        }
    }
}
