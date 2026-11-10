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

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.logApi;
import im.xz.cn.plugin.spi.Plugin;
import im.xz.cn.plugin.spi.PluginContext;
import im.xz.cn.plugin.spi.PluginDescriptor;
import im.xz.cn.plugin.spi.PluginLifecycle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class PluginHost implements AutoCloseable {

    private static final logApi log = logApi.getLogger(PluginHost.class);

    public record PluginStatus(String id, PluginLifecycle state, String failure) {
    }

    private static final class Entry {
        final PluginDescriptor descriptor;
        final Plugin plugin;
        final PluginTaskScheduler scheduler;
        PluginContext context;
        PluginLifecycle state = PluginLifecycle.DISCOVERED;
        String failure;

        Entry(PluginDescriptor descriptor, Plugin plugin) {
            this.descriptor = descriptor;
            this.plugin = plugin;
            this.scheduler = new PluginTaskScheduler(descriptor.id());
        }
    }

    private final DatabaseManager database;
    private final Path pluginsDirectory;
    private final Map<String, Entry> plugins = new LinkedHashMap<>();

    public PluginHost(DatabaseManager database, Path pluginsDirectory) {
        this.database = database;
        this.pluginsDirectory = pluginsDirectory;
    }

    public synchronized void register(PluginDescriptor descriptor, Plugin plugin) {
        if (descriptor == null || plugin == null) {
            throw new IllegalArgumentException("descriptor and plugin are required");
        }
        if (plugins.containsKey(descriptor.id())) {
            throw new IllegalStateException("duplicate plugin id: " + descriptor.id());
        }
        plugins.put(descriptor.id(), new Entry(descriptor, plugin));
    }

    public synchronized void loadAll() {
        for (Entry entry : plugins.values()) {
            if (entry.state != PluginLifecycle.DISCOVERED) {
                continue;
            }
            try {
                Path dataDirectory = pluginsDirectory.resolve(entry.descriptor.id());
                Files.createDirectories(dataDirectory);
                entry.context = new PluginContextImpl(
                        entry.descriptor.id(),
                        dataDirectory,
                        new PluginLogAdapter(entry.descriptor.id()),
                        new PluginSettingsStorage(database, entry.descriptor.id()),
                        entry.scheduler,
                        im.xz.cn.plugin.PluginManager.getInstance());
                entry.plugin.onLoad(entry.context);
                entry.state = PluginLifecycle.LOADED;
            } catch (Throwable error) {
                fail(entry, "onLoad", error);
            }
        }
    }

    public synchronized void enableAll() {
        for (Entry entry : plugins.values()) {
            if (entry.state != PluginLifecycle.LOADED) {
                continue;
            }
            try {
                entry.plugin.onEnable();
                entry.state = PluginLifecycle.ENABLED;
            } catch (Throwable error) {
                fail(entry, "onEnable", error);
            }
        }
    }

    public synchronized void disableAll() {
        List<Entry> entries = new ArrayList<>(plugins.values());
        java.util.Collections.reverse(entries);
        for (Entry entry : entries) {
            if (entry.state != PluginLifecycle.ENABLED) {
                continue;
            }
            entry.state = PluginLifecycle.DRAINING;
            try {
                entry.plugin.onDisable();
            } catch (Throwable error) {
                entry.failure = describe("onDisable", error);
                log.error("[Plugin] {} onDisable failed: {}", entry.descriptor.id(), error.getMessage(), error);
            } finally {
                entry.state = PluginLifecycle.DISABLED;
            }
        }
    }

    public synchronized List<PluginStatus> statuses() {
        List<PluginStatus> result = new ArrayList<>();
        for (Entry entry : plugins.values()) {
            result.add(new PluginStatus(entry.descriptor.id(), entry.state, entry.failure));
        }
        return List.copyOf(result);
    }

    public synchronized Optional<PluginLifecycle> stateOf(String pluginId) {
        Entry entry = plugins.get(pluginId);
        return entry == null ? Optional.empty() : Optional.of(entry.state);
    }

    public synchronized Optional<String> failureOf(String pluginId) {
        Entry entry = plugins.get(pluginId);
        return entry == null || entry.failure == null ? Optional.empty() : Optional.of(entry.failure);
    }

    private void fail(Entry entry, String phase, Throwable error) {
        entry.state = PluginLifecycle.FAILED;
        entry.failure = describe(phase, error);
        log.error("[Plugin] {} {} failed: {}", entry.descriptor.id(), phase,
                error.getMessage(), error);
    }

    private static String describe(String phase, Throwable error) {
        String message = error == null ? "unknown" : error.getMessage();
        return phase + " failed: " + error.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    @Override
    public synchronized void close() {
        disableAll();
        for (Entry entry : plugins.values()) {
            entry.scheduler.close();
            im.xz.cn.permission.PermissionRegistry.getInstance()
                    .unregisterSource(entry.descriptor.id());
            entry.state = PluginLifecycle.CLOSED;
        }
    }
}
