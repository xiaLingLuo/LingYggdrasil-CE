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


public final class TextureUploadRepository {

    private static final Jdbc.RowMapper<TextureUpload> MAPPER = resultSet -> new TextureUpload(
            resultSet.getLong("id"),
            resultSet.getString("user_id"),
            resultSet.getLong("asset_id"),
            resultSet.getString("original_name"),
            Jdbc.readInstant(resultSet, "created_at"),
            UploadState.parse(resultSet.getString("state"))
    );

    private final DatabaseManager database;

    public TextureUploadRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<TextureUpload> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_uploads WHERE id = ?", id));
    }

    public Optional<TextureUpload> findByUserAndAsset(String userId, long assetId) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_uploads WHERE user_id = ? AND asset_id = ?", userId, assetId));
    }

    public List<TextureUpload> findActiveByUser(String userId) {
        return Jdbc.queryMany(database, MAPPER,
                "SELECT * FROM texture_uploads WHERE user_id = ? AND state = 'ACTIVE' ORDER BY id DESC", userId);
    }

    public boolean hasActiveUpload(String userId, long assetId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_uploads WHERE user_id = ? AND asset_id = ? AND state = 'ACTIVE'",
                userId, assetId) > 0;
    }

    
    public TextureUpload grant(String userId, long assetId, String originalName, Instant now) {
        Optional<TextureUpload> existing = findByUserAndAsset(userId, assetId);
        if (existing.isPresent()) {
            TextureUpload upload = existing.get();
            if (upload.state() == UploadState.ACTIVE) {
                return upload;
            }
            Jdbc.update(database, "UPDATE texture_uploads SET state = 'ACTIVE', original_name = ? WHERE id = ?",
                    originalName, upload.id());
            return findById(upload.id()).orElseThrow();
        }
        long id = Jdbc.insert(database,
                "INSERT INTO texture_uploads (user_id, asset_id, original_name, created_at, state)"
                        + " VALUES (?, ?, ?, ?, 'ACTIVE')",
                userId, assetId, originalName, Jdbc.writeInstant(now));
        return findById(id).orElseThrow();
    }

    public int revoke(long id) {
        return Jdbc.update(database, "UPDATE texture_uploads SET state = 'REVOKED' WHERE id = ?", id);
    }

    public long countActiveByUser(String userId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_uploads WHERE user_id = ? AND state = 'ACTIVE'", userId);
    }
}
