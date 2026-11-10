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
package im.xz.cn.settings;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.infrastructure.persistence.UnitOfWork;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


public final class SettingsRepository {

    private final DatabaseManager database;
    private final UnitOfWork unitOfWork;

    public SettingsRepository(DatabaseManager database) {
        this.database = database;
        this.unitOfWork = new UnitOfWork(database);
    }

    public long revision(String group) {
        Long revision = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                "SELECT revision FROM settings_revision WHERE group_name = ?", group);
        return revision == null ? 0L : revision;
    }

    public Map<String, String> load(String group) {
        Map<String, String> values = new LinkedHashMap<>();
        Jdbc.queryMany(database, resultSet -> Map.entry(
                        resultSet.getString("setting_key"),
                        resultSet.getString("value_json")),
                "SELECT setting_key, value_json FROM settings WHERE group_name = ? ORDER BY setting_key",
                group).forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(values);
    }

    
    public Map<String, String> loadEffectiveOverrides() {
        String placeholders = String.join(",", java.util.Collections.nCopies(
                SettingsGroups.MIGRATED_GROUPS.size(), "?"));
        Map<String, String> values = new LinkedHashMap<>();
        Jdbc.queryMany(database, resultSet -> Map.entry(
                        resultSet.getString("setting_key"), resultSet.getString("value_json")),
                "SELECT setting_key, value_json FROM settings WHERE group_name IN (" + placeholders
                        + ") ORDER BY group_name, setting_key",
                SettingsGroups.MIGRATED_GROUPS.toArray()).forEach(entry ->
                values.put(entry.getKey(), entry.getValue()));
        return values;
    }

    
    public long save(String group, Map<String, String> values, long expectedRevision) {
        return unitOfWork.execute(connection -> {
            Long actual = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                    "SELECT revision FROM settings_revision WHERE group_name = ?", group);
            long current = actual == null ? 0L : actual;
            if (current != expectedRevision) {
                throw new RevisionConflictException(group, expectedRevision, current);
            }
            long next = current + 1;
            String now = Jdbc.writeInstant(Instant.now());
            for (Map.Entry<String, String> entry : values.entrySet()) {
                long existing = Jdbc.queryCount(database,
                        "SELECT COUNT(*) FROM settings WHERE group_name = ? AND setting_key = ?",
                        group, entry.getKey());
                if (existing > 0) {
                    Jdbc.update(database,
                            "UPDATE settings SET value_json = ?, revision = ? WHERE group_name = ? AND setting_key = ?",
                            entry.getValue(), next, group, entry.getKey());
                } else {
                    Jdbc.update(database,
                            "INSERT INTO settings (group_name, setting_key, value_json, revision)"
                                    + " VALUES (?, ?, ?, ?)", group, entry.getKey(), entry.getValue(), next);
                }
            }
            if (actual == null) {
                Jdbc.update(database,
                        "INSERT INTO settings_revision (group_name, revision, updated_at) VALUES (?, ?, ?)",
                        group, next, now);
            } else {
                Jdbc.update(database,
                        "UPDATE settings_revision SET revision = ?, updated_at = ? WHERE group_name = ?",
                        next, now, group);
            }
            return next;
        });
    }
}
