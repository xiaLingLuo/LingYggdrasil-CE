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
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.plugin.spi.PluginStorage;

import java.util.Optional;


public final class PluginSettingsStorage implements PluginStorage {

    private final DatabaseManager database;
    private final String groupName;

    public PluginSettingsStorage(DatabaseManager database, String pluginId) {
        this.database = database;
        this.groupName = "plugin:" + pluginId;
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String value = Jdbc.queryOne(database, resultSet -> resultSet.getString(1),
                "SELECT value_json FROM settings WHERE group_name = ? AND setting_key = ?",
                groupName, key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("storage key is required");
        }
        long existing = Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM settings WHERE group_name = ? AND setting_key = ?",
                groupName, key);
        if (existing > 0) {
            Jdbc.update(database,
                    "UPDATE settings SET value_json = ? WHERE group_name = ? AND setting_key = ?",
                    value, groupName, key);
        } else {
            Jdbc.update(database,
                    "INSERT INTO settings (group_name, setting_key, value_json, revision)"
                            + " VALUES (?, ?, ?, 1)", groupName, key, value);
        }
    }

    @Override
    public void remove(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Jdbc.update(database,
                "DELETE FROM settings WHERE group_name = ? AND setting_key = ?", groupName, key);
    }
}
