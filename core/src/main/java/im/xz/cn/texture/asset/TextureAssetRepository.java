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


public final class TextureAssetRepository {

    private static final Jdbc.RowMapper<TextureAsset> MAPPER = resultSet -> new TextureAsset(
            resultSet.getLong("id"),
            resultSet.getLong("blob_id"),
            TextureType.parse(resultSet.getString("type")),
            resultSet.getInt("width"),
            resultSet.getInt("height"),
            resultSet.getString("admin_file_name"),
            resultSet.getString("author_name"),
            TextureAsset.AssetState.parse(resultSet.getString("state")),
            Jdbc.readInstant(resultSet, "created_at")
    );

    private final DatabaseManager database;

    public TextureAssetRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<TextureAsset> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_assets WHERE id = ?", id));
    }

    public Optional<TextureAsset> findByTypeAndBlob(TextureType type, long blobId) {
        return Optional.ofNullable(Jdbc.queryOne(database, MAPPER,
                "SELECT * FROM texture_assets WHERE type = ? AND blob_id = ?", type.name(), blobId));
    }

    public long insert(TextureAsset asset) {
        return Jdbc.insert(database,
                "INSERT INTO texture_assets (blob_id, type, width, height, admin_file_name,"
                        + " author_name, state, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                asset.blobId(), asset.type().name(), asset.width(), asset.height(),
                asset.adminFileName(), asset.authorName(), asset.state().name(),
                Jdbc.writeInstant(asset.createdAt()));
    }

    public int updateAuthor(long id, String authorName) {
        return Jdbc.update(database, "UPDATE texture_assets SET author_name = ? WHERE id = ?",
                authorName, id);
    }

    public int updateAdminFileName(long id, String fileName) {
        return Jdbc.update(database, "UPDATE texture_assets SET admin_file_name = ? WHERE id = ?",
                fileName, id);
    }

    public int updateState(long id, TextureAsset.AssetState state) {
        return Jdbc.update(database, "UPDATE texture_assets SET state = ? WHERE id = ?",
                state.name(), id);
    }

    public long countByState(TextureAsset.AssetState state) {
        return Jdbc.queryCount(database, "SELECT COUNT(*) FROM texture_assets WHERE state = ?", state.name());
    }

    
    public long createReady(long blobId, TextureType type, int width, int height,
                            String adminFileName, String authorName, Instant now) {
        return insert(new TextureAsset(0L, blobId, type, width, height, adminFileName, authorName,
                TextureAsset.AssetState.READY, now));
    }
}
