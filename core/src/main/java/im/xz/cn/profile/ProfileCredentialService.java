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
import im.xz.cn.security.ConfigCipher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;


public final class ProfileCredentialService {

    private final ProfileCredentialRepository credentials;

    public ProfileCredentialService(DatabaseManager database) {
        this.credentials = new ProfileCredentialRepository(database);
    }

    
    public String reveal(String profileId, String legacyToken) {
        Optional<ProfileCredentialRepository.Credential> credential = credentials.find(profileId);
        if (credential.isPresent()) {
            try {
                return ConfigCipher.decrypt(credential.get().keyCipher());
            } catch (RuntimeException error) {
                return "";
            }
        }
        if (legacyToken != null && !legacyToken.isEmpty()) {
            store(profileId, legacyToken);
            return legacyToken;
        }
        return "";
    }

    
    public void rotate(String profileId, String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("world tree key must not be blank");
        }
        store(profileId, plaintext);
    }

    public Optional<ProfileCredentialRepository.Credential> find(String profileId) {
        return credentials.find(profileId);
    }

    public void delete(String profileId) {
        credentials.delete(profileId);
    }

    private void store(String profileId, String plaintext) {
        String cipher = ConfigCipher.encrypt(plaintext);
        String digest = sha256Hex(plaintext);
        credentials.upsert(profileId, cipher, digest, 1);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    public static String displayMask(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        return plaintext.substring(0, Math.min(4, plaintext.length())) + "****";
    }

    public static String normalizeKey(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
