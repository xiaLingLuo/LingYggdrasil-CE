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

import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.plugin.api.PluginApiHandler;
import im.xz.cn.plugin.api.PluginContext;
import im.xz.cn.plugin.api.PluginLogger;
import im.xz.cn.plugin.api.PluginMenu;

import java.io.File;
import java.util.Locale;

final class PluginContextImpl implements PluginContext {

    private final PluginHolder holder;
    private final PluginManager manager;
    private final PluginLoggerImpl logger;
    private final File dataFolder;

    PluginContextImpl(PluginHolder holder, PluginManager manager) {
        this.holder = holder;
        this.manager = manager;
        this.dataFolder = new File(manager.getPluginsDir(), holder.name());
        this.logger = new PluginLoggerImpl(holder.name(), dataFolder);
    }

    @Override
    public String getName() {
        return holder.descriptor.name();
    }

    @Override
    public String getFriendlyName() {
        return holder.descriptor.displayName();
    }

    @Override
    public String getVersion() {
        return holder.descriptor.version();
    }

    @Override
    public File getDataFolder() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warn("Failed to create plugin data folder: {}", dataFolder.getAbsolutePath());
        }
        return dataFolder;
    }

    @Override
    public PluginLogger getLogger() {
        return logger;
    }

    PluginLoggerImpl loggerImpl() {
        return logger;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return manager.getDatabaseManager();
    }

    @Override
    public SystemConfig getSystemConfig() {
        return SystemConfig.getInstance();
    }

    @Override
    public File getServerDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    @Override
    public void registerMenu(PluginMenu menu) {
        if (menu == null) return;
        if (holder.menu != null) {
            logger.warn("A secondary menu is already registered; ignoring '{}'.", menu.id());
            return;
        }
        holder.menu = menu;
    }

    @Override
    public void registerApiRoute(String path, PluginApiHandler handler) {
        if (path == null || handler == null) return;
        String normalized = normalize(path);
        if (normalized == null || normalized.isEmpty()) {
            logger.warn("Ignored API route with empty path.");
            return;
        }
        if (holder.routes.putIfAbsent(normalized, handler) != null) {
            logger.warn("API route '{}' is already registered; the earlier handler is kept.", normalized);
        }
    }

    @Override
    public void registerUserRoute(String method, String path, PluginApiHandler handler) {
        manager.registerUserRoute(holder.name(), method, path, handler);
    }

    @Override
    public void registerYggdrasilRoute(String method, String path, PluginApiHandler handler) {
        manager.registerYggdrasilRoute(holder.name(), method, path, handler);
    }

    private static String normalize(String path) {
        String p = path.trim();
        while (p.startsWith("/")) p = p.substring(1);
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (p.isEmpty()) return null;
        return p.toLowerCase(Locale.ROOT);
    }
}
