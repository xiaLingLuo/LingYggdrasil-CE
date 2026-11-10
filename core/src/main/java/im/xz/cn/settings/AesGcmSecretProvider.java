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

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public final class AesGcmSecretProvider implements SecretProvider {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final Map<Integer, SecretKeySpec> keys = new HashMap<>();
    private final int currentVersion;
    private final SecureRandom random = new SecureRandom();

    
    public AesGcmSecretProvider(Map<Integer, byte[]> masterKeys, int currentVersion) {
        if (masterKeys == null || masterKeys.isEmpty()) {
            throw new IllegalArgumentException("masterKeys must not be empty");
        }
        for (Map.Entry<Integer, byte[]> entry : masterKeys.entrySet()) {
            byte[] key = entry.getValue();
            if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
                throw new IllegalArgumentException("invalid master key length for version " + entry.getKey());
            }
            keys.put(entry.getKey(), new SecretKeySpec(key, KEY_ALGORITHM));
        }
        if (!keys.containsKey(currentVersion)) {
            throw new IllegalArgumentException("current key version not present: " + currentVersion);
        }
        this.currentVersion = currentVersion;
    }

    
    public static AesGcmSecretProvider singleKey(byte[] key) {
        return new AesGcmSecretProvider(Map.of(1, key), 1);
    }

    @Override
    public SealedSecret seal(String plaintext, String context) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(currentVersion), new GCMParameterSpec(TAG_BITS, iv));
            if (context != null) {
                cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            }
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return new SealedSecret(ALGORITHM, currentVersion,
                    Base64.getEncoder().encodeToString(combined));
        } catch (GeneralSecurityException error) {
            throw new SecretAccessException("seal failed", error);
        }
    }

    @Override
    public String open(SealedSecret secret, String context) {
        if (secret == null) {
            throw new IllegalArgumentException("secret must not be null");
        }
        SecretKeySpec key = keys.get(secret.keyVersion());
        if (key == null) {
            throw new SecretAccessException("unknown key version: " + secret.keyVersion(), null);
        }
        try {
            byte[] combined = Base64.getDecoder().decode(secret.ciphertext());
            if (combined.length <= IV_LENGTH) {
                throw new SecretAccessException("ciphertext too short", null);
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            if (context != null) {
                cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            }
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new SecretAccessException("open failed", error);
        }
    }

    @Override
    public int currentKeyVersion() {
        return currentVersion;
    }

    
    public static final class SecretAccessException extends RuntimeException {
        public SecretAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
