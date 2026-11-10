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

import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceLog {
    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String API = "api";
    public static final String AUDIT = "audit";
    public static final String PLUGIN = "plugin";
    public static final String APP = "app";

    private static final String[] ALL = {USER, ADMIN, API, AUDIT, PLUGIN, APP};

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();
    private static final Map<String, DailyFileWriter> WRITERS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> RETENTION = new ConcurrentHashMap<>();

    private static volatile int levelThreshold = levelValue("INFO");
    private static volatile boolean auditEnabled = true;
    private static volatile boolean pluginEnabled = true;

    private ServiceLog() {}

    public static void init() {
        for (String service : ALL) {
            File dir = new File(System.getProperty("user.dir"), "logs" + File.separator + service);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    public static void configure(String level, boolean audit, boolean plugin,
                                 Map<String, Integer> retentionByService) {
        levelThreshold = levelValue(level);
        auditEnabled = audit;
        pluginEnabled = plugin;
        RETENTION.clear();
        if (retentionByService != null) {
            RETENTION.putAll(retentionByService);
        }
        resetWriters();
        applyLogbackLevel(level);
    }

    private static void applyLogbackLevel(String level) {
        try {
            org.slf4j.Logger root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            if (root instanceof ch.qos.logback.classic.Logger logbackRoot) {
                logbackRoot.setLevel(ch.qos.logback.classic.Level.toLevel(
                        level == null ? "INFO" : level.trim().toUpperCase(),
                        ch.qos.logback.classic.Level.INFO));
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setService(String service) {
        if (service == null || service.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(service);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static String currentService() {
        String service = CURRENT.get();
        return (service == null || service.isBlank()) ? APP : service;
    }

    public static void writeCurrent(String level, String loggerName, String message) {
        write(currentService(), level, loggerName, message);
    }

    public static void write(String service, String level, String loggerName, String message) {
        String name = (service == null || service.isBlank()) ? APP : service;
        if (AUDIT.equals(name) && !auditEnabled) return;
        if (PLUGIN.equals(name) && !pluginEnabled) return;
        if (!AUDIT.equals(name) && levelValue(level) < levelThreshold) return;
        DailyFileWriter writer = WRITERS.computeIfAbsent(name, ServiceLog::createWriter);
        if (writer != null) {
            writer.write(level, loggerName, message);
        }
    }

    private static DailyFileWriter createWriter(String service) {
        File dir = new File(System.getProperty("user.dir"), "logs" + File.separator + service);
        return new DailyFileWriter(dir, service, retentionDays(service));
    }

    public static int retentionDays(String service) {
        return RETENTION.getOrDefault(service, 30);
    }

    public static int pluginRetentionDays() {
        return retentionDays(PLUGIN);
    }

    public static int levelValue(String level) {
        if (level == null) return 2;
        return switch (level.trim().toUpperCase()) {
            case "TRACE" -> 0;
            case "DEBUG" -> 1;
            case "INFO" -> 2;
            case "WARN" -> 3;
            case "ERROR" -> 4;
            default -> 2;
        };
    }

    private static void resetWriters() {
        for (DailyFileWriter writer : WRITERS.values()) {
            writer.close();
        }
        WRITERS.clear();
    }

    public static void shutdown() {
        resetWriters();
    }
}
