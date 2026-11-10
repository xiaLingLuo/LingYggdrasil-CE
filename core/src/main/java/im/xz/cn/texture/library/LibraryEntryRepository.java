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
package im.xz.cn.texture.library;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class LibraryEntryRepository {

    private static final Jdbc.RowMapper<LibraryEntry> MAPPER = resultSet -> new LibraryEntry(
            resultSet.getLong("id"),
            resultSet.getString("user_id"),
            resultSet.getLong("asset_id"),
            resultSet.getString("alias"),
            resultSet.getBoolean("hidden"),
            Jdbc.readInstant(resultSet, "created_at")
    );

    private final DatabaseManager database;

    public LibraryEntryRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<LibraryEntry> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM library_entries WHERE id = ?", id));
    }

    public Optional<LibraryEntry> findByUserAndAsset(String userId, long assetId) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM library_entries WHERE user_id = ? AND asset_id = ?", userId, assetId));
    }

    public List<LibraryEntry> findByUser(String userId) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM library_entries WHERE user_id = ? ORDER BY id DESC", userId);
    }

    
    public LibraryEntry upsert(String userId, long assetId, String alias, boolean hidden, Instant now) {
        Optional<LibraryEntry> existing = findByUserAndAsset(userId, assetId);
        if (existing.isPresent()) {
            LibraryEntry entry = existing.get();
            Jdbc.update(database, "UPDATE library_entries SET alias = ?, hidden = ? WHERE id = ?",
                    alias, hidden, entry.id());
            return findById(entry.id()).orElseThrow();
        }
        long id = Jdbc.insert(database,
                "INSERT INTO library_entries (user_id, asset_id, alias, hidden, created_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                userId, assetId, alias, hidden, Jdbc.writeInstant(now));
        return findById(id).orElseThrow();
    }

    public int updateAlias(long id, String alias) {
        return Jdbc.update(database, "UPDATE library_entries SET alias = ? WHERE id = ?", alias, id);
    }

    
    public LibraryEntry ensure(String userId, long assetId, Instant now) {
        Optional<LibraryEntry> existing = findByUserAndAsset(userId, assetId);
        if (existing.isPresent()) {
            return existing.get();
        }
        long id = Jdbc.insert(database,
                "INSERT INTO library_entries (user_id, asset_id, alias, hidden, created_at)"
                        + " VALUES (?, ?, NULL, ?, ?)",
                userId, assetId, false, Jdbc.writeInstant(now));
        return findById(id).orElseThrow();
    }

    public int delete(long id) {
        return Jdbc.update(database, "DELETE FROM library_entries WHERE id = ?", id);
    }
}
