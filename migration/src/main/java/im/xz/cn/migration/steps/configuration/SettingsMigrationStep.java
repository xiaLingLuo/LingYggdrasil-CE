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
package im.xz.cn.migration.steps.configuration;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.MigrationMappingStore;
import im.xz.cn.migration.MigrationReport;
import im.xz.cn.settings.SettingsGroups;
import im.xz.cn.settings.SettingsRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class SettingsMigrationStep {

    public static final String STEP_ID = "v2_3_16.configuration.001";

    private final DatabaseManager database;
    private final SettingsRepository settings;
    private final MigrationMappingStore mappings;

    public SettingsMigrationStep(DatabaseManager database) {
        this.database = database;
        this.settings = new SettingsRepository(database);
        this.mappings = new MigrationMappingStore(database);
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        List<SettingRow> rows = Jdbc.queryMany(database, resultSet -> new SettingRow(
                        resultSet.getString("setting_key"),
                        resultSet.getString("setting_value")),
                "SELECT setting_key, setting_value FROM system_settings ORDER BY setting_key");

        Map<String, Map<String, String>> byGroup = new LinkedHashMap<>();
        for (SettingRow row : rows) {
            report.add("settingsSeen", 1);
            if (row.key() == null || row.key().isBlank()) {
                report.add("skippedInvalid", 1);
                continue;
            }
            if (mappings.find("config-key", row.key()).isPresent()) {
                continue;
            }
            byGroup.computeIfAbsent(groupFor(row.key()), key -> new LinkedHashMap<>())
                    .put(row.key(), row.value() == null ? "" : row.value());
        }

        for (Map.Entry<String, Map<String, String>> entry : byGroup.entrySet()) {
            String group = entry.getKey();
            Map<String, String> incoming = entry.getValue();
            long revision = settings.revision(group);
            Map<String, String> merged = new LinkedHashMap<>(settings.load(group));
            merged.putAll(incoming);
            long newRevision = settings.save(group, merged, revision);
            for (String key : incoming.keySet()) {
                mappings.record("config-key", key, group + "." + key, group);
                report.add("keysMapped", 1);
            }
            report.add("groupsWritten", 1);
            report.warn("group '" + group + "' committed at revision " + newRevision
                    + " (" + incoming.size() + " keys)");
        }

        report.add("mappingConfigKeys", mappings.count("config-key"));
        return report;
    }

    
    static String groupFor(String key) {
        return SettingsGroups.classify(key);
    }
    private record SettingRow(String key, String value) {
    }
}
