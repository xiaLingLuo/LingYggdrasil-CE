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
package im.xz.cn.logging;

import im.xz.cn.config.SystemConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class UserActionLogger {
    private static final String DIR = System.getProperty("user.dir")
            + File.separator + "logs" + File.separator + "user-actions";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm:ss");

    private UserActionLogger() {}

    public static void log(String userId, String ip, String actionKey) {
        if (userId == null || userId.isBlank() || actionKey == null) return;
        SystemConfig sc = SystemConfig.getInstance();
        if (!sc.isUserActionLogEnabled()) return;
        if (!sc.isUserActionLoggable(actionKey)) return;
        try {
            File dir = new File(DIR);
            if (!dir.exists() && !dir.mkdirs()) return;
            File file = new File(dir, safeName(userId) + ".log");
            List<String> lines = readLines(file);
            String label = im.xz.cn.i18n.I18n.t("userlog." + actionKey);
            String safeIp = (ip == null || ip.isBlank()) ? "-" : ip.replaceAll("[\\r\\n\\t]", "").trim();
            String newLine = "[" + LocalDateTime.now().format(TS) + "]" + label + " 于 " + safeIp;
            lines.add(newLine);

            int days = sc.getUserActionLogRetentionDays();
            if (days > 0) {
                LocalDate expiry = LocalDate.now().minusDays(days);
                boolean triggered = false;
                for (String l : lines) {
                    LocalDate d = dateOf(l);
                    if (d != null && d.isBefore(expiry)) { triggered = true; break; }
                }
                if (triggered) {
                    if (days <= 1) {
                        lines.clear();
                    } else {
                        int keepDays = Math.max(1, (int) Math.floor(days * 0.1));
                        LocalDate keepCutoff = LocalDate.now().minusDays(keepDays);
                        lines.removeIf(l -> {
                            LocalDate d = dateOf(l);
                            return d != null && d.isBefore(keepCutoff);
                        });
                    }
                }
            }
            if (lines.isEmpty()) lines.add(newLine);

            long maxBytes = Math.max(1, sc.getUserActionLogMaxKib()) * 1024L;
            if (totalBytes(lines) >= maxBytes && lines.size() > 1) {
                int removeCount = (int) Math.floor(lines.size() * 0.95);
                if (removeCount >= lines.size()) removeCount = lines.size() - 1;
                lines = new ArrayList<>(lines.subList(removeCount, lines.size()));
            }

            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static long sizeBytes(String userId) {
        if (userId == null || userId.isBlank()) return 0;
        File file = new File(DIR, safeName(userId) + ".log");
        return file.exists() ? file.length() : 0;
    }

    public static void clear(String userId) {
        if (userId == null || userId.isBlank()) return;
        try {
            File file = new File(DIR, safeName(userId) + ".log");
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        }
    }

    public static String read(String userId) {
        if (userId == null || userId.isBlank()) return "";
        try {
            File file = new File(DIR, safeName(userId) + ".log");
            if (!file.exists()) return "";
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (int i = lines.size() - 1; i >= 0; i--) {
                sb.append(lines.get(i)).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> readLines(File file) {
        if (!file.exists()) return new ArrayList<>();
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static long totalBytes(List<String> lines) {
        long total = 0;
        for (String l : lines) total += lineBytes(l);
        return total;
    }

    private static long lineBytes(String line) {
        return line.getBytes(StandardCharsets.UTF_8).length + 1;
    }

    private static LocalDate dateOf(String line) {
        try {
            if (line.length() >= 11 && line.charAt(0) == '[') {
                return LocalDate.parse(line.substring(1, 11));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String safeName(String userId) {
        return userId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
