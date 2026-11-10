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

import im.xz.cn.logging.logApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


public final class PluginDirectoryLoader {

    private static final logApi log = logApi.getLogger(PluginDirectoryLoader.class);

    public record ScanResult(List<PluginJarLoader.LoadedPlugin> loaded, List<String> rejected) {

        public ScanResult {
            loaded = List.copyOf(loaded);
            rejected = List.copyOf(rejected);
        }
    }

    private final PluginJarLoader loader = new PluginJarLoader();

    public ScanResult scan(Path pluginsDirectory) {
        List<PluginJarLoader.LoadedPlugin> loaded = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (pluginsDirectory == null || !Files.isDirectory(pluginsDirectory)) {
            return new ScanResult(loaded, rejected);
        }
        List<Path> jars = new ArrayList<>();
        try (Stream<Path> files = Files.list(pluginsDirectory)) {
            files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(jars::add);
        } catch (IOException error) {
            throw new IllegalStateException("cannot scan plugins directory: " + pluginsDirectory, error);
        }
        for (Path jar : jars) {
            try {
                loaded.add(loader.load(jar));
            } catch (RuntimeException error) {
                rejected.add(jar.getFileName() + ": " + error.getMessage());
                log.warn("[Plugin] rejected {}: {}", jar.getFileName(), error.getMessage());
            }
        }
        return new ScanResult(loaded, rejected);
    }

    
    public ScanResult registerAll(Path pluginsDirectory, PluginHost host) {
        ScanResult result = scan(pluginsDirectory);
        for (PluginJarLoader.LoadedPlugin plugin : result.loaded()) {
            host.register(plugin.descriptor(), plugin.plugin());
        }
        return result;
    }

    
    public void close(ScanResult result) {
        for (PluginJarLoader.LoadedPlugin plugin : result.loaded()) {
            try {
                plugin.classLoader().close();
            } catch (IOException error) {
                log.warn("[Plugin] failed to close classloader for {}: {}",
                        plugin.descriptor().id(), error.getMessage());
            }
        }
    }
}
