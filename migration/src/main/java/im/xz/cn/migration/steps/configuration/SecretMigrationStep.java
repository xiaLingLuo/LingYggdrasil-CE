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
import im.xz.cn.migration.legacy.v2_3_16.LegacyConfigCipher;
import im.xz.cn.settings.AesGcmSecretProvider;
import im.xz.cn.settings.SecretProvider;
import im.xz.cn.settings.SecretStore;


public final class SecretMigrationStep {

    public static final String STEP_ID = "v2_3_16.secrets.001";

    private final DatabaseManager database;
    private final SecretStore secrets;
    private final byte[] legacyKey;
    private final MigrationMappingStore mappings;

    public SecretMigrationStep(DatabaseManager database, byte[] legacyKey) {
        this.database = database;
        this.legacyKey = legacyKey;
        this.secrets = new SecretStore(database, AesGcmSecretProvider.singleKey(legacyKey));
        this.mappings = new MigrationMappingStore(database);
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        Jdbc.queryMany(database, resultSet -> new SettingRow(
                        resultSet.getString("setting_key"),
                        resultSet.getString("setting_value")),
                "SELECT setting_key, setting_value FROM system_settings ORDER BY setting_key")
                .forEach(row -> migrate(row, report));
        report.add("mappingSecrets", mappings.count("secret"));
        return report;
    }

    private void migrate(SettingRow row, MigrationReport report) {
        if (!LegacyConfigCipher.isEncrypted(row.value())) {
            return;
        }
        report.add("encryptedSeen", 1);
        if (mappings.find("secret", row.key()).isPresent()) {
            report.add("skippedAlreadyMigrated", 1);
            return;
        }
        String name = "legacy:" + row.key();
        try {
            String plaintext = LegacyConfigCipher.decrypt(row.value(), legacyKey);
            secrets.put(name, plaintext, "legacy.config:" + row.key());
            mappings.record("secret", row.key(), name, "legacy");
            report.add("secretsMigrated", 1);
        } catch (RuntimeException error) {
            report.add("secretsFailed", 1);
            report.warn("cannot decrypt legacy secret for key " + row.key() + ": " + error.getMessage());
        }
    }

    private record SettingRow(String key, String value) {
    }
}
