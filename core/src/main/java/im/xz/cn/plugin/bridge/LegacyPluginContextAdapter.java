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
import im.xz.cn.plugin.api.PluginApiHandler;
import im.xz.cn.plugin.api.PluginContext;
import im.xz.cn.plugin.api.PluginLogger;
import im.xz.cn.plugin.api.PluginMenu;

import java.io.File;


public final class LegacyPluginContextAdapter implements PluginContext {

    private final im.xz.cn.plugin.spi.PluginContext delegate;
    private final DatabaseManager database;
    private final SystemConfig systemConfig;
    private final File serverDirectory;
    private final PluginLogger logger;

    public LegacyPluginContextAdapter(im.xz.cn.plugin.spi.PluginContext delegate,
                                      DatabaseManager database,
                                      SystemConfig systemConfig,
                                      File serverDirectory) {
        this.delegate = delegate;
        this.database = database;
        this.systemConfig = systemConfig;
        this.serverDirectory = serverDirectory;
        this.logger = new LegacyPluginLoggerAdapter(delegate == null ? null : delegate.logger());
    }

    @Override
    public String getName() {
        return delegate == null ? "legacy-plugin" : delegate.pluginId();
    }

    @Override
    public String getFriendlyName() {
        return getName();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public File getDataFolder() {
        return delegate == null ? serverDirectory : delegate.dataDirectory().toFile();
    }

    @Override
    public PluginLogger getLogger() {
        return logger;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return database;
    }

    @Override
    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    @Override
    public File getServerDirectory() {
        return serverDirectory;
    }

    public im.xz.cn.plugin.spi.PluginPermissions permissions() {
        return delegate == null ? new im.xz.cn.plugin.spi.PluginPermissions() {
            @Override
            public void registerUser(String key, String category, String description) {
            }

            @Override
            public void registerAdmin(String key, String category, String description) {
            }
        } : delegate.permissions();
    }

    @Override
    public void registerMenu(PluginMenu menu) {
        if (delegate == null || menu == null) return;
        im.xz.cn.plugin.PluginManager.getInstance()
                .registerMenuForPlugin(delegate.pluginId(), menu);
    }

    @Override
    public void registerApiRoute(String path, PluginApiHandler handler) {
        if (delegate == null || path == null || handler == null) return;
        
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (!normalized.startsWith("/api/plugin/")) {
            normalized = "/api/plugin" + normalized;
        }
        im.xz.cn.plugin.PluginManager.getInstance()
                .registerUserRouteForPlugin(delegate.pluginId(), "GET", normalized, handler);
    }

    @Override
    public void registerUserRoute(String method, String path, PluginApiHandler handler) {
        if (delegate == null) return;
        im.xz.cn.plugin.PluginManager.getInstance()
                .registerUserRouteForPlugin(delegate.pluginId(), method, path, handler);
    }

    @Override
    public void registerYggdrasilRoute(String method, String path, PluginApiHandler handler) {
        if (delegate == null) return;
        im.xz.cn.plugin.PluginManager.getInstance()
                .registerYggdrasilRouteForPlugin(delegate.pluginId(), method, path, handler);
    }
}
