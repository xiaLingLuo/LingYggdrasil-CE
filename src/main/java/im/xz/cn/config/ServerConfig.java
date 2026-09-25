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
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            writeDefault();
        }
        try (InputStream is = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> root = asMap(yaml.load(is), "root");
            if (root != null) apply(root);
            normalizeRetention();
            validate();
            loaded = true;
        } catch (Exception e) {
            resetDefaults();
            log.error("Failed to load {}: {}", CONFIG_FILE, e.getMessage(), e);
            throw new IllegalStateException("Invalid " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
    }

    private void resetDefaults() {
        userEnabled = true;
        userIp = "0.0.0.0";
        userPort = 35565;
        userLogRetentionDays = 30;
        yggdrasilEnabled = true;
        yggdrasilIp = "0.0.0.0";
        yggdrasilPort = 35577;
        yggdrasilLogRetentionDays = 30;
        adminEnabled = true;
        adminIp = "0.0.0.0";
        adminPort = 35599;
        adminLogRetentionDays = 30;
        logLevel = "INFO";
        auditLogEnabled = true;
        auditLogRetentionDays = 90;
        pluginSystemLogEnabled = true;
        pluginSystemLogRetentionDays = 30;
        loaded = false;
    }

    private void apply(Map<String, Object> root) {
        Map<String, Object> services = asMap(root.get("services"), "services");
        if (services != null) {
            Map<String, Object> user = asMap(services.get("user"), "services.user");
            if (user != null) {
                userEnabled = bool(user.get("enabled"), userEnabled, "services.user.enabled");
                userIp = str(user.get("ip"), userIp, "services.user.ip");
                userPort = integer(user.get("port"), userPort, "services.user.port");
                userLogRetentionDays = integer(user.get("logRetentionDays"), userLogRetentionDays, "services.user.logRetentionDays");
            }
            Map<String, Object> yggdrasil = asMap(services.get("yggdrasil"), "services.yggdrasil");
            if (yggdrasil != null) {
                yggdrasilEnabled = bool(yggdrasil.get("enabled"), yggdrasilEnabled, "services.yggdrasil.enabled");
                yggdrasilIp = str(yggdrasil.get("ip"), yggdrasilIp, "services.yggdrasil.ip");
                yggdrasilPort = integer(yggdrasil.get("port"), yggdrasilPort, "services.yggdrasil.port");
                yggdrasilLogRetentionDays = integer(yggdrasil.get("logRetentionDays"), yggdrasilLogRetentionDays, "services.yggdrasil.logRetentionDays");
            }
            Map<String, Object> admin = asMap(services.get("admin"), "services.admin");
            if (admin != null) {
                adminEnabled = bool(admin.get("enabled"), adminEnabled, "services.admin.enabled");
                adminIp = str(admin.get("ip"), adminIp, "services.admin.ip");
                adminPort = integer(admin.get("port"), adminPort, "services.admin.port");
                adminLogRetentionDays = integer(admin.get("logRetentionDays"), adminLogRetentionDays, "services.admin.logRetentionDays");
            }
        }

        Map<String, Object> logging = asMap(root.get("logging"), "logging");
        if (logging != null) {
            logLevel = str(logging.get("level"), logLevel, "logging.level");
            Map<String, Object> audit = asMap(logging.get("audit"), "logging.audit");
            if (audit != null) {
                auditLogEnabled = bool(audit.get("enabled"), auditLogEnabled, "logging.audit.enabled");
                auditLogRetentionDays = integer(audit.get("retentionDays"), auditLogRetentionDays, "logging.audit.retentionDays");
            }
            Map<String, Object> plugin = asMap(logging.get("pluginSystem"), "logging.pluginSystem");
            if (plugin != null) {
                pluginSystemLogEnabled = bool(plugin.get("enabled"), pluginSystemLogEnabled, "logging.pluginSystem.enabled");
                pluginSystemLogRetentionDays = integer(plugin.get("retentionDays"), pluginSystemLogRetentionDays, "logging.pluginSystem.retentionDays");
            }
        }
    }

    private void validate() {
        validateService("services.user", userIp, userPort);
        validateService("services.yggdrasil", yggdrasilIp, yggdrasilPort);
        validateService("services.admin", adminIp, adminPort);
        if (!Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR").contains(logLevel.toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("logging.level must be TRACE, DEBUG, INFO, WARN, or ERROR");
        }
        if (userEnabled && yggdrasilEnabled && sameEndpoint(userIp, userPort, yggdrasilIp, yggdrasilPort)) {
            throw new IllegalArgumentException("services.user and services.yggdrasil cannot bind to the same address and port");
        }
        if (userEnabled && adminEnabled && sameEndpoint(userIp, userPort, adminIp, adminPort)) {
            throw new IllegalArgumentException("services.user and services.admin cannot bind to the same address and port");
        }
        if (yggdrasilEnabled && adminEnabled && sameEndpoint(yggdrasilIp, yggdrasilPort, adminIp, adminPort)) {
            throw new IllegalArgumentException("services.yggdrasil and services.admin cannot bind to the same address and port");
        }
    }

    private static void validateService(String name, String ip, int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(name + ".port must be between 1 and 65535");
        }
        validateIp(name + ".ip", ip);
    }

    private static void validateIp(String key, String ip) {
        if (ip == null || ip.isBlank() || !ip.equals(ip.trim()) || !ip.matches("[0-9a-fA-F:.]+")) {
            throw new IllegalArgumentException(key + " must be an IPv4 or IPv6 address");
        }
        try {
            if (!ip.contains(":")) {
                String[] parts = ip.split("\\.", -1);
                if (parts.length != 4) throw new IllegalArgumentException(key + " must be an IPv4 or IPv6 address");
                for (String part : parts) {
                    if (part.length() > 1 && part.startsWith("0")) {
                        throw new IllegalArgumentException(key + " contains a non-canonical IPv4 octet");
                    }
                    int value = Integer.parseInt(part);
                    if (value < 0 || value > 255) throw new IllegalArgumentException(key + " contains an invalid IPv4 octet");
                }
            } else {
                InetAddress.getByName(ip);
            }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException argumentException) throw argumentException;
            throw new IllegalArgumentException(key + " must be a valid IP address", e);
        }
    }

    private static boolean sameEndpoint(String firstIp, int firstPort, String secondIp, int secondPort) {
        if (firstPort != secondPort) return false;
        try {
            InetAddress first = InetAddress.getByName(firstIp);
            InetAddress second = InetAddress.getByName(secondIp);
            return first.isAnyLocalAddress() || second.isAnyLocalAddress()
                    || Arrays.equals(first.getAddress(), second.getAddress());
        } catch (Exception e) {
            return firstIp.equalsIgnoreCase(secondIp);
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

    private static Map<String, Object> asMap(Object value, String key) {
        if (value == null) return null;
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(key + " must be a mapping");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String name)) {
                throw new IllegalArgumentException(key + " contains a non-string key");
            }
            result.put(name, entry.getValue());
        }
        return result;
    }

    private static String str(Object value, String def, String key) {
        if (value == null) return def;
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException(key + " must be a non-empty string");
        }
        return s.trim();
    }

    private static boolean bool(Object value, boolean def, String key) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) return true;
            if ("false".equalsIgnoreCase(s.trim())) return false;
        }
        if (value == null) return def;
        throw new IllegalArgumentException(key + " must be true or false");
    }

    private static int integer(Object value, int def, String key) {
        if (value instanceof Number n) {
            try {
                return new BigDecimal(n.toString()).intValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be an integer", e);
            }
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be an integer", e);
            }
        }
        if (value == null) return def;
        throw new IllegalArgumentException(key + " must be an integer");
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
