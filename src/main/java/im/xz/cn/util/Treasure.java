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
package im.xz.cn.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import im.xz.cn.config.AppConfig;
import im.xz.cn.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Treasure {
    private static final Logger logger = LoggerFactory.getLogger(Treasure.class);

    private static final String TREASURE_URL = "https://treasure.xzrui.cn/treasure/lingyggdrasil";
    private static final long INTERVAL_SECONDS = 60;
    private static final String SETTING_KEY = "server_uuid";

    private static Treasure instance;

    private static String cachedIp;
    private static long lastIpFetch;

    private final DatabaseManager db;
    private final String serverUUID;
    private ScheduledExecutorService scheduler;

    private Treasure(DatabaseManager db) {
        this.db = db;
        this.serverUUID = loadOrGenerateUUID();
    }

    public static synchronized Treasure init(DatabaseManager db) {
        if (instance == null) {
            instance = new Treasure(db);
            instance.start();
        }
        return instance;
    }

    public static void shutdown() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    private void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "treasure-reporter");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                post();
            } catch (Exception e) {
                logger.debug("[Treasure] Report failed: {}", e.getMessage());
            }
        }, 0, INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.info("[Treasure] Reporter started (interval: {}s)", INTERVAL_SECONDS);
    }

    private void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private String loadOrGenerateUUID() {
        try {
            List<Map<String, Object>> rows = db.executeQuery(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?", SETTING_KEY);
            if (!rows.isEmpty()) {
                String raw = String.valueOf(rows.get(0).get("setting_value"));
                if (raw != null && !raw.isEmpty() && !"null".equals(raw)) {
                    return raw;
                }
            }
        } catch (Exception e) {
            logger.debug("[Treasure] Failed to load UUID from DB: {}", e.getMessage());
        }

        String uuid = UUID.randomUUID().toString();
        try {
            String dbType = db.getDbType();
            if ("mysql".equalsIgnoreCase(dbType)) {
                db.executeUpdate(
                    "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)",
                    SETTING_KEY, uuid
                );
            } else if ("pgsql".equalsIgnoreCase(dbType)) {
                db.executeUpdate(
                    "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) " +
                    "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value",
                    SETTING_KEY, uuid
                );
            } else {
                db.executeUpdate(
                    "INSERT OR REPLACE INTO system_settings (setting_key, setting_value) VALUES (?, ?)",
                    SETTING_KEY, uuid
                );
            }
        } catch (Exception e) {
            logger.debug("[Treasure] Failed to save UUID: {}", e.getMessage());
        }
        return uuid;
    }

    private void post() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("uuid", serverUUID);

        safePut(root, "ver", AppConfig.APP_VERSION);
        safePut(root, "javaVer", System.getProperty("java.version"));
        safePut(root, "os", System.getProperty("os.name"));
        safePut(root, "osVer", System.getProperty("os.version"));
        safePut(root, "cpu", getCpuName());
        root.put("cpuN", safeInt(() -> Runtime.getRuntime().availableProcessors()));
        root.put("ram", safeLong(Treasure::getTotalMemoryMiB));
        safePut(root, "ip", getPublicIp());
        safePut(root, "db", safeGet(() -> db.getDbType()));
        root.put("user", safeInt(() -> countTable("users")));
        root.put("player", safeInt(() -> countTable("player_profiles")));
        root.put("texture", safeInt(() -> countTable("textures")));

        byte[] body;
        try {
            body = mapper.writeValueAsBytes(root);
        } catch (Exception e) {
            logger.debug("[Treasure] JSON serialize failed: {}", e.getMessage());
            return;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(TREASURE_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                logger.debug("[Treasure] HTTP response: {}", responseCode);
            }
        } catch (Exception e) {
            logger.debug("[Treasure] POST failed: {}", e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String getPublicIp() {
        if (cachedIp != null && System.currentTimeMillis() - lastIpFetch < 3600_000) {
            return cachedIp;
        }
        String ip = fetchIpFromPrimary();
        if (ip == null) {
            ip = fetchIpFromBackup();
        }
        if (ip != null) {
            cachedIp = ip;
            lastIpFetch = System.currentTimeMillis();
            return ip;
        }
        return cachedIp != null ? cachedIp : "-1";
    }

    private static String fetchIpFromPrimary() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://ip.im.xz.cn/api/"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                var json = new ObjectMapper().readTree(resp.body());
                var v4 = json.get("ipv4");
                if (v4 != null && !v4.isNull()) {
                    return v4.asText().trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String fetchIpFromBackup() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String ip = resp.body().trim();
                if (!ip.isEmpty()) return ip;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void safePut(ObjectNode node, String key, String value) {
        node.put(key, value != null && !value.isEmpty() ? value : "-1");
    }

    private static int safeInt(IntSupplier supplier) {
        try {
            return supplier.getAsInt();
        } catch (Exception e) {
            return -1;
        }
    }

    private static long safeLong(LongSupplier supplier) {
        try {
            return supplier.getAsLong();
        } catch (Exception e) {
            return -1;
        }
    }

    private static String safeGet(Supplier<String> supplier) {
        try {
            String v = supplier.get();
            return v != null && !v.isEmpty() ? v : "-1";
        } catch (Exception e) {
            return "-1";
        }
    }

    private int countTable(String table) {
        try {
            var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM " + table);
            if (result != null && result.get("cnt") != null) {
                return ((Number) result.get("cnt")).intValue();
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static String getCpuName() {
        try {
            String win = System.getenv("PROCESSOR_IDENTIFIER");
            if (win != null && !win.isEmpty()) return win;
        } catch (Exception ignored) {}
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux")) {
                Path path = Path.of("/proc/cpuinfo");
                if (Files.exists(path)) {
                    try (var lines = Files.lines(path)) {
                        return lines.filter(l -> l.startsWith("model name"))
                                .map(l -> l.substring(l.indexOf(':') + 1).trim())
                                .findFirst().orElse("-1");
                    }
                }
            }
            if (os.contains("mac") || os.contains("darwin")) {
                Process p = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    return r.readLine().trim();
                }
            }
        } catch (Exception ignored) {}
        return "-1";
    }

    private static long getTotalMemoryMiB() {
        try {
            OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean mx) {
                return mx.getTotalPhysicalMemorySize() / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return -1;
    }

}
