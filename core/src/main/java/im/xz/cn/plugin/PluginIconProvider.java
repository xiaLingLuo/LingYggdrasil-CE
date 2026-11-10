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
package im.xz.cn.plugin;

import java.io.InputStream;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PluginIconProvider {
    private static final PluginSystemLog log = PluginSystemLog.get();
    private static final String DEFAULT_ICON = "/icons/plugin-default.svg";
    private static final int MAX_ICON_BYTES = 2 * 1024 * 1024;

    private PluginIconProvider() {}

    public record IconData(byte[] data, String contentType) {}

    public static IconData load(PluginDescriptor descriptor) {
        String iconPath = descriptor.iconPath();
        if (iconPath == null || iconPath.isBlank()) return null;
        String normalized = iconPath.startsWith("/") ? iconPath.substring(1) : iconPath;
        try (JarFile jar = new JarFile(descriptor.jarPath().toFile())) {
            JarEntry entry = jar.getJarEntry(normalized);
            if (entry == null) return null;
            byte[] data;
            try (InputStream is = jar.getInputStream(entry)) {
                data = is.readNBytes(MAX_ICON_BYTES + 1);
            }
            if (data.length > MAX_ICON_BYTES) {
                log.warn("[Plugin] Icon of {} exceeds {} bytes, ignored.", descriptor.name(), MAX_ICON_BYTES);
                return null;
            }
            String contentType = detectContentType(normalized, data);
            if (contentType == null) return null;
            return new IconData(data, contentType);
        } catch (Exception e) {
            log.warn("[Plugin] Failed to read icon '{}' of {}: {}", iconPath, descriptor.name(), e.getMessage());
            return null;
        }
    }

    public static IconData defaultIcon() {
        try (InputStream is = PluginIconProvider.class.getResourceAsStream(DEFAULT_ICON)) {
            if (is == null) return null;
            return new IconData(is.readAllBytes(), "image/svg+xml");
        } catch (Exception e) {
            return null;
        }
    }

    private static String detectContentType(String path, byte[] data) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") && isPng(data)) return "image/png";
        if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg")) && isJpeg(data)) return "image/jpeg";
        if (lower.endsWith(".svg") && isSvg(data)) return "image/svg+xml";
        if (lower.endsWith(".webp") && isWebp(data)) return "image/webp";
        return null;
    }

    private static boolean isPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 0x50 && d[2] == 0x4E && d[3] == 0x47;
    }

    private static boolean isJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    private static boolean isWebp(byte[] d) {
        return d.length > 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
    }

    private static boolean isSvg(byte[] d) {
        int len = Math.min(d.length, 1024);
        String head = new String(d, 0, len, java.nio.charset.StandardCharsets.UTF_8).trim();
        if (head.startsWith("<?xml")) {
            int decl = head.indexOf("?>");
            head = decl >= 0 ? head.substring(decl + 2).trim() : head;
        }
        return head.startsWith("<svg");
    }
}
