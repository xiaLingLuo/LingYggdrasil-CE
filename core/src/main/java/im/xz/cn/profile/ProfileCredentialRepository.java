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
import java.util.Optional;


public final class ProfileCredentialRepository {

    public record Credential(String profileId, String keyCipher, String keyDigest, int keyVersion,
                             Instant updatedAt) {
    }

    private final DatabaseManager database;

    public ProfileCredentialRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<Credential> find(String profileId) {
        return Optional.ofNullable(Jdbc.queryOne(database,
                resultSet -> new Credential(
                        resultSet.getString("profile_id"),
                        resultSet.getString("key_cipher"),
                        resultSet.getString("key_digest"),
                        resultSet.getInt("key_version"),
                        Jdbc.readInstant(resultSet, "updated_at")),
                "SELECT profile_id, key_cipher, key_digest, key_version, updated_at"
                        + " FROM profile_credentials WHERE profile_id = ?",
                profileId));
    }

    public void upsert(String profileId, String keyCipher, String keyDigest, int keyVersion) {
        Jdbc.update(database, "DELETE FROM profile_credentials WHERE profile_id = ?", profileId);
        Jdbc.update(database,
                "INSERT INTO profile_credentials (profile_id, key_cipher, key_digest, key_version, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                profileId, keyCipher, keyDigest, keyVersion, Jdbc.writeInstant(Instant.now()));
    }

    public int delete(String profileId) {
        return Jdbc.update(database, "DELETE FROM profile_credentials WHERE profile_id = ?", profileId);
    }
}
