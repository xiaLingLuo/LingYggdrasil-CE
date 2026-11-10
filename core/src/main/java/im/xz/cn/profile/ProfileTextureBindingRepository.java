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
package im.xz.cn.profile;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class ProfileTextureBindingRepository {

    public record Binding(String profileId, String slot, long assetId, String skinModel, Instant updatedAt) {
    }

    private final DatabaseManager database;

    public ProfileTextureBindingRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<Binding> find(String profileId, String slot) {
        return Optional.ofNullable(Jdbc.queryOne(database,
                resultSet -> new Binding(
                        resultSet.getString("profile_id"),
                        resultSet.getString("slot"),
                        resultSet.getLong("asset_id"),
                        resultSet.getString("skin_model"),
                        Jdbc.readInstant(resultSet, "updated_at")),
                "SELECT profile_id, slot, asset_id, skin_model, updated_at"
                        + " FROM profile_texture_bindings WHERE profile_id = ? AND slot = ?",
                profileId, slot));
    }

    public void upsert(String profileId, String slot, long assetId, String skinModel, Instant now) {
        Jdbc.update(database,
                "DELETE FROM profile_texture_bindings WHERE profile_id = ? AND slot = ?",
                profileId, slot);
        Jdbc.update(database,
                "INSERT INTO profile_texture_bindings (profile_id, slot, asset_id, skin_model, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                profileId, slot, assetId, skinModel, Jdbc.writeInstant(now));
    }

    public int remove(String profileId, String slot) {
        return Jdbc.update(database,
                "DELETE FROM profile_texture_bindings WHERE profile_id = ? AND slot = ?",
                profileId, slot);
    }

    public int removeByProfile(String profileId) {
        return Jdbc.update(database,
                "DELETE FROM profile_texture_bindings WHERE profile_id = ?", profileId);
    }

    public int removeByAsset(long assetId) {
        return Jdbc.update(database,
                "DELETE FROM profile_texture_bindings WHERE asset_id = ?", assetId);
    }

    public List<Binding> findByAsset(long assetId) {
        return Jdbc.queryMany(database,
                resultSet -> new Binding(
                        resultSet.getString("profile_id"),
                        resultSet.getString("slot"),
                        resultSet.getLong("asset_id"),
                        resultSet.getString("skin_model"),
                        Jdbc.readInstant(resultSet, "updated_at")),
                "SELECT profile_id, slot, asset_id, skin_model, updated_at"
                        + " FROM profile_texture_bindings WHERE asset_id = ? ORDER BY profile_id, slot",
                assetId);
    }
}
