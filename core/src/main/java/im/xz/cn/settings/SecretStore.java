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

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class SecretStore {

    private final DatabaseManager database;
    private final SecretProvider provider;

    public SecretStore(DatabaseManager database, SecretProvider provider) {
        this.database = database;
        this.provider = provider;
    }

    
    public static SecretStore create(DatabaseManager database) {
        return new SecretStore(database,
                AesGcmSecretProvider.singleKey(im.xz.cn.security.ConfigCipher.masterKeyBytes()));
    }

    public void put(String name, String plaintext, String context) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("secret name is required");
        }
        SecretProvider.SealedSecret sealed = provider.seal(plaintext, context);
        String now = Jdbc.writeInstant(Instant.now());
        long existing = Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM secret_store WHERE name = ?", name);
        if (existing > 0) {
            Jdbc.update(database,
                    "UPDATE secret_store SET algorithm = ?, key_version = ?, ciphertext = ?,"
                            + " updated_at = ? WHERE name = ?",
                    sealed.algorithm(), sealed.keyVersion(), sealed.ciphertext(), now, name);
        } else {
            Jdbc.update(database,
                    "INSERT INTO secret_store (name, algorithm, key_version, ciphertext, updated_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    name, sealed.algorithm(), sealed.keyVersion(), sealed.ciphertext(), now);
        }
    }

    public Optional<String> get(String name, String context) {
        SecretProvider.SealedSecret sealed = Jdbc.queryOne(database, resultSet ->
                        new SecretProvider.SealedSecret(
                                resultSet.getString("algorithm"),
                                resultSet.getInt("key_version"),
                                resultSet.getString("ciphertext")),
                "SELECT algorithm, key_version, ciphertext FROM secret_store WHERE name = ?", name);
        return sealed == null ? Optional.empty() : Optional.of(provider.open(sealed, context));
    }

    public boolean exists(String name) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM secret_store WHERE name = ?", name) > 0;
    }

    public void delete(String name) {
        Jdbc.update(database, "DELETE FROM secret_store WHERE name = ?", name);
    }

    public List<String> names() {
        return Jdbc.queryMany(database, resultSet -> resultSet.getString(1),
                "SELECT name FROM secret_store ORDER BY name");
    }
}
