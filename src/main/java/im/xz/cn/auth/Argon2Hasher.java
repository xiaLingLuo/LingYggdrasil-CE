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
package im.xz.cn.auth;

import im.xz.cn.config.SystemConfig;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.security.SecureRandom;
import java.util.Base64;

public class Argon2Hasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int VERSION = Argon2Parameters.ARGON2_VERSION_13; // v=19

    private static final int DEFAULT_MEMORY = 65536;
    private static final int DEFAULT_ITERATIONS = 3;
    private static final int DEFAULT_PARALLELISM = 1;
    private static final int DEFAULT_HASH_LENGTH = 32;
    private static final int DEFAULT_SALT_LENGTH = 32;

    public enum EncryptionLevel {
        LEVEL_1(1, "电磁加密", 65536, 3, 1, 32, 32),
        LEVEL_2(2, "能量加密", 131072, 3, 2, 32, 32),
        LEVEL_3(3, "结构加密", 262144, 3, 3, 32, 32),
        LEVEL_4(4, "信息加密", 524288, 3, 4, 32, 32),
        LEVEL_5(5, "引力加密", 1048576, 3, 6, 32, 32),
        LEVEL_6(6, "宇宙加密", 2097152, 3, 8, 48, 48);

        private final int level;
        private final String name;
        private final int memory;
        private final int iterations;
        private final int parallelism;
        private final int hashLength;
        private final int saltLength;

        EncryptionLevel(int level, String name, int memory, int iterations, int parallelism, int hashLength, int saltLength) {
            this.level = level;
            this.name = name;
            this.memory = memory;
            this.iterations = iterations;
            this.parallelism = parallelism;
            this.hashLength = hashLength;
            this.saltLength = saltLength;
        }

        public int getLevel() { return level; }
        public String getName() { return name; }
        public int getMemory() { return memory; }
        public int getIterations() { return iterations; }
        public int getParallelism() { return parallelism; }
        public int getHashLength() { return hashLength; }
        public int getSaltLength() { return saltLength; }

        public static EncryptionLevel fromLevel(int level) {
            for (EncryptionLevel el : values()) {
                if (el.level == level) return el;
            }
            return LEVEL_1;
        }
    }

    public static String hash(String password, EncryptionLevel level) {
        byte[] salt = new byte[level.getSaltLength()];
        SECURE_RANDOM.nextBytes(salt);

        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(VERSION)
                .withMemoryAsKB(level.getMemory())
                .withIterations(level.getIterations())
                .withParallelism(level.getParallelism())
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[level.getHashLength()];
        generator.generateBytes(password.toCharArray(), hash);

        String saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt);
        String hashB64 = Base64.getEncoder().withoutPadding().encodeToString(hash);

        return "$argon2id$v=19$m=" + level.getMemory() + ",t=" + level.getIterations() + ",p=" + level.getParallelism() + "$" + saltB64 + "$" + hashB64;
    }

    public static String hash(String password) {
        EncryptionLevel level = getCurrentLevel();
        return hash(password, level);
    }

    public static boolean needsRehash(String encodedHash) {
        try {
            if (encodedHash == null || encodedHash.isEmpty()) return false;
            String[] parts = encodedHash.split("\\$");
            if (parts.length < 6) return false;

            String[] paramParts = parts[3].split(",");
            int hashMemory = Integer.parseInt(paramParts[0].substring(2));
            int hashIterations = Integer.parseInt(paramParts[1].substring(2));
            int hashParallelism = Integer.parseInt(paramParts[2].substring(2));

            EncryptionLevel currentLevel = getCurrentLevel();
            return hashMemory != currentLevel.getMemory()
                || hashIterations != currentLevel.getIterations()
                || hashParallelism != currentLevel.getParallelism();
        } catch (Exception e) {
            return false;
        }
    }

    private static EncryptionLevel getCurrentLevel() {
        try {
            int level = SystemConfig.getInstance().getEncryptionLevel();
            return EncryptionLevel.fromLevel(level);
        } catch (Exception e) {
            return EncryptionLevel.LEVEL_1;
        }
    }

    public static boolean verify(String password, String encodedHash) {
        try {
            String[] parts = encodedHash.split("\\$");
            // 格式: $argon2id$v=19$m,t,p$salt$hash
            if (parts.length < 6) return false;

            String[] paramParts = parts[3].split(",");
            int memory = Integer.parseInt(paramParts[0].substring(2));
            int iterations = Integer.parseInt(paramParts[1].substring(2));
            int parallelism = Integer.parseInt(paramParts[2].substring(2));

            byte[] salt = Base64.getDecoder().decode(padBase64(parts[4]));
            byte[] expectedHash = Base64.getDecoder().decode(padBase64(parts[5]));

            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(VERSION)
                    .withMemoryAsKB(memory)
                    .withIterations(iterations)
                    .withParallelism(parallelism)
                    .withSalt(salt)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);

            byte[] actualHash = new byte[expectedHash.length];
            generator.generateBytes(password.toCharArray(), actualHash);

            return java.security.MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            System.err.println("Argon2 verify failed: " + e.getMessage());
            return false;
        }
    }

    private static String padBase64(String base64) {
        int padding = (4 - base64.length() % 4) % 4;
        return base64 + "=".repeat(padding);
    }
}
