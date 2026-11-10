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
package im.xz.cn.identity;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;


public final class VerificationChallengeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TTL_SECONDS = 300;

    private final DatabaseManager database;

    public VerificationChallengeService(DatabaseManager database) {
        this.database = database;
    }

    public String issue(String purpose, String subjectId, String target) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant now = Instant.now();
        Jdbc.insert(database,
                "INSERT INTO verification_challenges (purpose, subject_id, target, digest,"
                        + " expires_at, remaining_attempts) VALUES (?, ?, ?, ?, ?, ?)",
                purpose, subjectId, target, digest(token),
                Jdbc.writeInstant(now.plusSeconds(TTL_SECONDS)), 1);
        return token;
    }

    
    public boolean consume(String purpose, String subjectId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String digest = digest(token.trim());
        int updated = Jdbc.update(database,
                "UPDATE verification_challenges SET consumed_at = ?"
                        + " WHERE purpose = ? AND subject_id = ? AND digest = ?"
                        + " AND consumed_at IS NULL AND expires_at > ?",
                Jdbc.writeInstant(Instant.now()), purpose, subjectId, digest,
                Jdbc.writeInstant(Instant.now()));
        return updated == 1;
    }

    private static String digest(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}
