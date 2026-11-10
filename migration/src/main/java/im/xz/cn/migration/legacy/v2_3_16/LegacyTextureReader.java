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
package im.xz.cn.migration.legacy.v2_3_16;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public final class LegacyTextureReader {

    private static final String TEXTURE_COLUMNS =
            "id, user_id, type, hash, alias, original_name, size, content_type, created_at,"
                    + " reference_type, ref_owner_id";

    private final DatabaseManager database;

    public LegacyTextureReader(DatabaseManager database) {
        this.database = database;
    }

    public List<LegacyTexture> readAllTextures() {
        return Jdbc.queryMany(database, LegacyTextureReader::mapTexture,
                "SELECT " + TEXTURE_COLUMNS + " FROM textures ORDER BY created_at, id");
    }

    public Optional<LegacyTexture> findById(String textureId) {
        return Optional.ofNullable(Jdbc.queryOne(database, LegacyTextureReader::mapTexture,
                "SELECT " + TEXTURE_COLUMNS + " FROM textures WHERE id = ?", textureId));
    }

    public List<LegacyVisibility> readPublicVisibility() {
        return Jdbc.queryMany(database, resultSet -> new LegacyVisibility(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("texture_id"),
                resultSet.getInt("is_public") == 1,
                resultSet.getString("created_at")
        ), "SELECT id, user_id, texture_id, is_public, created_at FROM texture_visibility"
                + " WHERE is_public = 1 ORDER BY created_at, id");
    }

    public List<LegacyInteraction> readLikes() {
        return Jdbc.queryMany(database, resultSet -> new LegacyInteraction(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("texture_id"),
                resultSet.getString("created_at")
        ), "SELECT id, user_id, texture_id, created_at FROM texture_likes ORDER BY created_at, id");
    }

    public List<LegacyInteraction> readFavorites() {
        return Jdbc.queryMany(database, resultSet -> new LegacyInteraction(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("texture_id"),
                resultSet.getString("created_at")
        ), "SELECT id, user_id, texture_id, created_at FROM texture_favorites ORDER BY created_at, id");
    }

    
    public List<LegacyFavoriteAlias> readFavoriteAliases() {
        return Jdbc.queryMany(database, resultSet -> new LegacyFavoriteAlias(
                resultSet.getString("user_id"),
                resultSet.getString("texture_id"),
                resultSet.getString("alias")
        ), "SELECT user_id, texture_id, alias FROM texture_favorites"
                + " WHERE alias IS NOT NULL AND alias <> '' ORDER BY created_at, id");
    }

    public List<LegacyFriendShare> readFriendShares() {
        return Jdbc.queryMany(database, resultSet -> new LegacyFriendShare(
                resultSet.getString("id"),
                resultSet.getString("owner_id"),
                resultSet.getString("friend_id"),
                resultSet.getString("texture_id"),
                resultSet.getString("created_at")
        ), "SELECT id, owner_id, friend_id, texture_id, created_at FROM friend_shared_textures"
                + " ORDER BY created_at, id");
    }

    private static LegacyTexture mapTexture(ResultSet resultSet) throws SQLException {
        Object size = resultSet.getObject("size");
        return new LegacyTexture(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("type"),
                resultSet.getString("hash"),
                resultSet.getString("alias"),
                resultSet.getString("original_name"),
                size == null ? null : ((Number) size).longValue(),
                resultSet.getString("content_type"),
                resultSet.getString("created_at"),
                resultSet.getString("reference_type"),
                resultSet.getString("ref_owner_id")
        );
    }

    
    public record LegacyVisibility(String id, String userId, String textureId, boolean isPublic,
                                   String createdAt) {
    }

    
    public record LegacyInteraction(String id, String userId, String textureId, String createdAt) {
    }

    public record LegacyFavoriteAlias(String userId, String textureId, String alias) {
    }

    
    public record LegacyFriendShare(String id, String ownerId, String friendId, String textureId,
                                    String createdAt) {
    }
}
