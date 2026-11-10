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
package im.xz.cn.migration;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.Optional;


public final class MigrationMappingStore {

    private final DatabaseManager database;

    public MigrationMappingStore(DatabaseManager database) {
        this.database = database;
    }

    public void record(String kind, String oldId, String newId, String note) {
        Optional<String> existing = find(kind, oldId);
        if (existing.isPresent()) {
            if (!existing.get().equals(newId)) {
                throw new IllegalStateException("mapping conflict: " + kind + "/" + oldId
                        + " -> " + existing.get() + " vs " + newId);
            }
            return;
        }
        Jdbc.update(database,
                "INSERT INTO migration_mappings (kind, old_id, new_id, note, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                kind, oldId, newId, note, Jdbc.writeInstant(Instant.now()));
    }

    public Optional<String> find(String kind, String oldId) {
        return Optional.ofNullable(Jdbc.queryOne(database, resultSet -> resultSet.getString("new_id"),
                "SELECT new_id FROM migration_mappings WHERE kind = ? AND old_id = ?", kind, oldId));
    }

    public long count(String kind) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM migration_mappings WHERE kind = ?", kind);
    }
}
