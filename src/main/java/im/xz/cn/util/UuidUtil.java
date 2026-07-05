package im.xz.cn.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class UuidUtil {

    // MD5 ver
    public static String generateV3(String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] raw = md.digest(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            raw[6] = (byte) ((raw[6] & 0x0F) | 0x30);
            raw[8] = (byte) ((raw[8] & 0x3F) | 0x80);
            return toHex(raw);
        } catch (Exception e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    public static String generateV4() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateV5(String namespace, String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] namespaceBytes = UUID.nameUUIDFromBytes(namespace.getBytes(StandardCharsets.UTF_8)).toString().getBytes(StandardCharsets.UTF_8);
            md.update(namespaceBytes);
            md.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] raw = md.digest();
            byte[] uuid = new byte[16];
            System.arraycopy(raw, 0, uuid, 0, 16);
            uuid[6] = (byte) ((uuid[6] & 0x0F) | 0x50);
            uuid[8] = (byte) ((uuid[8] & 0x3F) | 0x80);
            return toHex(uuid);
        } catch (Exception e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    public static String generateProfileUuid(String name, String version) {
        return switch (version) {
            case "v3" -> generateV3(name);
            case "v5" -> generateV5("OfflinePlayer", name);
            default -> generateV4();
        };
    }

    public static String generateUserUuid() {
        return UUID.randomUUID().toString();
    }

    public static String generateAdminUuid() {
        return UUID.randomUUID().toString();
    }

    public static String toHex(String uuid) {
        return uuid.replace("-", "");
    }

    public static String fromHex(String hex) {
        if (hex.contains("-")) return hex;
        return hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
