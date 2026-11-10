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
import im.xz.cn.plugin.api.LingPlugin;
import im.xz.cn.plugin.spi.Plugin;
import im.xz.cn.plugin.spi.PluginContext;

import java.io.File;


public final class LegacyPluginAdapter implements Plugin {

    private final LingPlugin legacy;
    private final DatabaseManager database;
    private final SystemConfig systemConfig;
    private final File serverDirectory;
    private volatile PluginContext context;

    public LegacyPluginAdapter(LingPlugin legacy,
                               DatabaseManager database,
                               SystemConfig systemConfig,
                               File serverDirectory) {
        if (legacy == null) {
            throw new IllegalArgumentException("legacy plugin is required");
        }
        this.legacy = legacy;
        this.database = database;
        this.systemConfig = systemConfig;
        this.serverDirectory = serverDirectory;
    }

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        LegacyPluginContextAdapter legacyContext =
                new LegacyPluginContextAdapter(context, database, systemConfig, serverDirectory);
        legacy.bind(legacyContext);
        legacy.onLoad(legacyContext);
    }

    @Override
    public void onEnable() {
        legacy.onEnable();
    }

    @Override
    public void onDisable() {
        legacy.onDisable();
    }

    public LingPlugin legacyPlugin() {
        return legacy;
    }
}
