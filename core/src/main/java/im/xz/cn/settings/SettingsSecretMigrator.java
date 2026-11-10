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
import im.xz.cn.logging.logApi;

import java.util.Map;


public final class SettingsSecretMigrator {

    private static final logApi log = logApi.getLogger(SettingsSecretMigrator.class);

    private SettingsSecretMigrator() {
    }

    public static void migrate(DatabaseManager database) {
        SecretStore store = SecretStore.create(database);
        migrate(database, store, "yggdrasil_private_key", "yggdrasil.private_key", "settings.yggdrasil");
        migrate(database, store, "mail_password", "mail.smtp_password", "settings.mail");
    }

    private static void migrate(DatabaseManager database, SecretStore store, String settingKey,
                                String secretName, String context) {
        try {
            for (Map.Entry<String, String> row : Jdbc.queryMany(database,
                    resultSet -> Map.entry(resultSet.getString("group_name"),
                            resultSet.getString("value_json")),
                    "SELECT group_name, value_json FROM settings WHERE setting_key = ?", settingKey)) {
                String value = row.getValue();
                if (value == null || value.isBlank()) continue;
                store.put(secretName, value, context);
                Jdbc.update(database,
                        "DELETE FROM settings WHERE group_name = ? AND setting_key = ?",
                        row.getKey(), settingKey);
                log.info("[Config] Migrated legacy secret {} from settings to secret_store", secretName);
            }
        } catch (RuntimeException error) {
            log.warn("[Config] Legacy secret migration skipped for {}: {}", secretName, error.getMessage());
        }
    }
}
