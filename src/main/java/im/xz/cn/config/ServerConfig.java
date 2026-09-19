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
package im.xz.cn.config;

import im.xz.cn.logging.logApi;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerConfig {
    private static final logApi log = logApi.getLogger(ServerConfig.class);
    private static final ServerConfig INSTANCE = new ServerConfig();

    public static final String CONFIG_FILE = "config.yml";

    private static final String DEFAULT_YAML = """
            services:
              user:
                enabled: true
                ip: 0.0.0.0
                port: 35565
                logRetentionDays: 30
              yggdrasil:
                enabled: true
                ip: 0.0.0.0
                port: 35577
                logRetentionDays: 30
              admin:
                enabled: true
                ip: 0.0.0.0
                port: 35599
                logRetentionDays: 30
            logging:
              level: INFO
              audit:
                enabled: true
                retentionDays: 90
              pluginSystem:
                enabled: true
                retentionDays: 30
            """;

    private boolean userEnabled = true;
    private String userIp = "0.0.0.0";
    private int userPort = 35565;
    private int userLogRetentionDays = 30;

    private boolean yggdrasilEnabled = true;
    private String yggdrasilIp = "0.0.0.0";
    private int yggdrasilPort = 35577;
    private int yggdrasilLogRetentionDays = 30;

    private boolean adminEnabled = true;
    private String adminIp = "0.0.0.0";
    private int adminPort = 35599;
    private int adminLogRetentionDays = 30;

    private String logLevel = "INFO";
    private boolean auditLogEnabled = true;
    private int auditLogRetentionDays = 90;
    private boolean pluginSystemLogEnabled = true;
    private int pluginSystemLogRetentionDays = 30;

    private boolean loaded;

    private ServerConfig() {}

    public static ServerConfig getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        if (loaded) return;
        loaded = true;

        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            writeDefault();
        }
        try (InputStream is = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> root = yaml.load(is);
            if (root != null) apply(root);
        } catch (Exception e) {
            log.error("Failed to load {}: {}", CONFIG_FILE, e.getMessage(), e);
        }
        normalizeRetention();
    }

    @SuppressWarnings("unchecked")
    private void apply(Map<String, Object> root) {
        Map<String, Object> services = asMap(root.get("services"));
        if (services != null) {
            Map<String, Object> user = asMap(services.get("user"));
            if (user != null) {
                userEnabled = bool(user.get("enabled"), userEnabled);
                userIp = str(user.get("ip"), userIp);
                userPort = integer(user.get("port"), userPort);
                userLogRetentionDays = integer(user.get("logRetentionDays"), userLogRetentionDays);
            }
            Map<String, Object> yggdrasil = asMap(services.get("yggdrasil"));
            if (yggdrasil != null) {
                yggdrasilEnabled = bool(yggdrasil.get("enabled"), yggdrasilEnabled);
                yggdrasilIp = str(yggdrasil.get("ip"), yggdrasilIp);
                yggdrasilPort = integer(yggdrasil.get("port"), yggdrasilPort);
                yggdrasilLogRetentionDays = integer(yggdrasil.get("logRetentionDays"), yggdrasilLogRetentionDays);
            }
            Map<String, Object> admin = asMap(services.get("admin"));
            if (admin != null) {
                adminEnabled = bool(admin.get("enabled"), adminEnabled);
                adminIp = str(admin.get("ip"), adminIp);
                adminPort = integer(admin.get("port"), adminPort);
                adminLogRetentionDays = integer(admin.get("logRetentionDays"), adminLogRetentionDays);
            }
        }

        Map<String, Object> logging = asMap(root.get("logging"));
        if (logging != null) {
            logLevel = str(logging.get("level"), logLevel);
            Map<String, Object> audit = asMap(logging.get("audit"));
            if (audit != null) {
                auditLogEnabled = bool(audit.get("enabled"), auditLogEnabled);
                auditLogRetentionDays = integer(audit.get("retentionDays"), auditLogRetentionDays);
            }
            Map<String, Object> plugin = asMap(logging.get("pluginSystem"));
            if (plugin != null) {
                pluginSystemLogEnabled = bool(plugin.get("enabled"), pluginSystemLogEnabled);
                pluginSystemLogRetentionDays = integer(plugin.get("retentionDays"), pluginSystemLogRetentionDays);
            }
        }
    }

    private void normalizeRetention() {
        userLogRetentionDays = normalize("services.user.logRetentionDays", userLogRetentionDays);
        yggdrasilLogRetentionDays = normalize("services.yggdrasil.logRetentionDays", yggdrasilLogRetentionDays);
        adminLogRetentionDays = normalize("services.admin.logRetentionDays", adminLogRetentionDays);
        auditLogRetentionDays = normalize("logging.audit.retentionDays", auditLogRetentionDays);
        pluginSystemLogRetentionDays = normalize("logging.pluginSystem.retentionDays", pluginSystemLogRetentionDays);
    }

    private int normalize(String key, int value) {
        if (value < 1) {
            log.warn("{}: {} = {} is invalid (must be >= 1); using 1 instead.", CONFIG_FILE, key, value);
            return 1;
        }
        return value;
    }

    private void writeDefault() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            writer.write(DEFAULT_YAML);
        } catch (Exception e) {
            log.error("Failed to write default {}: {}", CONFIG_FILE, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    private static String str(Object value, String def) {
        if (value == null) return def;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? def : s;
    }

    private static boolean bool(Object value, boolean def) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) return true;
            if ("false".equalsIgnoreCase(s.trim())) return false;
        }
        return def;
    }

    private static int integer(Object value, int def) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public Map<String, Integer> logRetention() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(im.xz.cn.logging.ServiceLog.USER, userLogRetentionDays);
        map.put(im.xz.cn.logging.ServiceLog.ADMIN, adminLogRetentionDays);
        map.put(im.xz.cn.logging.ServiceLog.API, yggdrasilLogRetentionDays);
        map.put(im.xz.cn.logging.ServiceLog.AUDIT, auditLogRetentionDays);
        map.put(im.xz.cn.logging.ServiceLog.PLUGIN, pluginSystemLogRetentionDays);
        return map;
    }

    public boolean isUserEnabled() { return userEnabled; }
    public String getUserIp() { return userIp; }
    public int getUserPort() { return userPort; }
    public int getUserLogRetentionDays() { return userLogRetentionDays; }

    public boolean isYggdrasilEnabled() { return yggdrasilEnabled; }
    public String getYggdrasilIp() { return yggdrasilIp; }
    public int getYggdrasilPort() { return yggdrasilPort; }
    public int getYggdrasilLogRetentionDays() { return yggdrasilLogRetentionDays; }

    public boolean isAdminEnabled() { return adminEnabled; }
    public String getAdminIp() { return adminIp; }
    public int getAdminPort() { return adminPort; }
    public int getAdminLogRetentionDays() { return adminLogRetentionDays; }

    public String getLogLevel() { return logLevel; }
    public boolean isAuditLogEnabled() { return auditLogEnabled; }
    public int getAuditLogRetentionDays() { return auditLogRetentionDays; }
    public boolean isPluginSystemLogEnabled() { return pluginSystemLogEnabled; }
    public int getPluginSystemLogRetentionDays() { return pluginSystemLogRetentionDays; }
}
