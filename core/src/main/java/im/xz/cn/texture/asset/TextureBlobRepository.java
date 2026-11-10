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
package im.xz.cn.texture.asset;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.Optional;


public final class TextureBlobRepository {

    private static final Jdbc.RowMapper<TextureBlob> MAPPER = resultSet -> new TextureBlob(
            resultSet.getLong("id"),
            resultSet.getString("sha256"),
            resultSet.getLong("size_bytes"),
            resultSet.getString("content_type"),
            resultSet.getString("storage_key"),
            BlobState.parse(resultSet.getString("state")),
            Jdbc.readInstant(resultSet, "created_at")
    );

    private final DatabaseManager database;

    public TextureBlobRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<TextureBlob> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_blobs WHERE id = ?", id));
    }

    public Optional<TextureBlob> findBySha256(String sha256) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_blobs WHERE sha256 = ?", sha256));
    }

    public long insert(TextureBlob blob) {
        long id = Jdbc.insert(database,
                "INSERT INTO texture_blobs (sha256, size_bytes, content_type, storage_key, state, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                blob.sha256(), blob.sizeBytes(), blob.contentType(), blob.storageKey(),
                blob.state().name(), Jdbc.writeInstant(blob.createdAt()));
        return id;
    }

    public int updateState(long id, BlobState state) {
        return Jdbc.update(database, "UPDATE texture_blobs SET state = ? WHERE id = ?",
                state.name(), id);
    }

    
    public long totalSizeBytes() {
        return Jdbc.queryCount(database, "SELECT COALESCE(SUM(size_bytes), 0) FROM texture_blobs"
                + " WHERE state <> 'DELETED'");
    }

    public long countReady() {
        return Jdbc.queryCount(database, "SELECT COUNT(*) FROM texture_blobs WHERE state = 'READY'");
    }

    
    public int deleteIfUnreferenced(long id, Instant now) {
        long references = Jdbc.queryCount(database, "SELECT COUNT(*) FROM texture_assets WHERE blob_id = ?", id);
        if (references > 0) {
            return 0;
        }
        return Jdbc.update(database, "DELETE FROM texture_blobs WHERE id = ?", id);
    }
}
