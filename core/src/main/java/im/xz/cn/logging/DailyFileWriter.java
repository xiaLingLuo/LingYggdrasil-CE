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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DailyFileWriter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File dir;
    private final String prefix;
    private final int retentionDays;

    private BufferedWriter writer;
    private String currentDate;

    public DailyFileWriter(File dir, String prefix) {
        this(dir, prefix, 0);
    }

    public DailyFileWriter(File dir, String prefix, int retentionDays) {
        this.dir = dir;
        this.prefix = prefix;
        this.retentionDays = retentionDays;
    }

    public synchronized void write(String level, String loggerName, String message) {
        try {
            String date = LocalDate.now().format(DATE_FORMAT);
            if (!date.equals(currentDate)) {
                closeWriter();
                currentDate = date;
                cleanupOldFiles();
            }
            if (writer == null) {
                writer = open(date);
                if (writer == null) return;
            }
            String line = LocalDateTime.now().format(TIME_FORMAT)
                    + " [" + Thread.currentThread().getName() + "] "
                    + level + " " + loggerName + " - " + message;
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (Exception ignored) {
        }
    }

    private BufferedWriter open(String date) {
        try {
            if (!dir.exists() && !dir.mkdirs()) return null;
            File file = new File(dir, prefix + "-" + date + ".log");
            return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            return null;
        }
    }

    private void cleanupOldFiles() {
        if (retentionDays <= 0) return;
        try {
            LocalDate cutoff = LocalDate.now().minusDays(retentionDays - 1L);
            File[] files = dir.listFiles((d, n) -> n.startsWith(prefix + "-") && n.endsWith(".log"));
            if (files == null) return;
            for (File file : files) {
                LocalDate date = parseDate(file.getName());
                if (date != null && date.isBefore(cutoff)) {
                    Files.deleteIfExists(file.toPath());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private LocalDate parseDate(String name) {
        try {
            String stamp = name.substring(prefix.length() + 1, name.length() - 4);
            return LocalDate.parse(stamp, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized void close() {
        closeWriter();
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignored) {
            }
            writer = null;
        }
    }
}
