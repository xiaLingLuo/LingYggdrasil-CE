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
package im.xz.cn.server.handler.admin;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.web.view.AdminPage;

import io.javalin.http.Context;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminAppInfoHandler {

    private static final String GITHUB_LATEST_URL =
            "https://api.github.com/repos/xiaLingLuo/LingYggdrasil-CE/releases/latest";
    private static final String CN_LATEST_URL = "https://api.multimc.cn/";
    private static final String UPDATE_CACHE_PREFIX = "appinfo:update:";
    private static final long UPDATE_INTERVAL_MS = 60000L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private final CacheDao cacheDao;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminAppInfoHandler(CacheDao cacheDao) {
        this.cacheDao = cacheDao;
    }

    public void appInfoPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderAppInfoPage(adminUsername, adminRole, csrfToken));
    }

    public void getAppInfo(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.appinfo.view")) return;
        SystemConfig sysConfig = SystemConfig.getInstance();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("appName", AppConfig.APP_NAME);
        info.put("appVersion", AppConfig.APP_VERSION);
        info.put("appRepo", AppConfig.APP_REPO);
        info.put("repoUrl", "https://github.com/xiaLingLuo/LingYggdrasil-CE");
        info.put("installedAt", sysConfig.getInstalledAt() != null ? sysConfig.getInstalledAt() : I18n.t("msg.unknown"));
        ctx.json(info);
    }

    @SuppressWarnings("unchecked")
    public void checkUpdate(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.appinfo.view")) return;
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String source = body == null ? null : body.get("source");
        if (!"github".equals(source) && !"cn".equals(source)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        String updateKey = UPDATE_CACHE_PREFIX + source;
        long remainingMs = updateCooldownRemainingMs(updateKey);
        if (remainingMs > 0) {
            int seconds = (int) Math.ceil(remainingMs / 1000.0);
            ctx.status(429).json(Map.of("success", false, "message", I18n.t("admin.appinfo.updateTooFrequent", seconds)));
            return;
        }
        cacheDao.put(updateKey, String.valueOf(System.currentTimeMillis() + UPDATE_INTERVAL_MS),
                "ratelimit", (int) (UPDATE_INTERVAL_MS / 1000) + 1);
        String bodyText;
        try {
            bodyText = httpGet("github".equals(source) ? GITHUB_LATEST_URL : CN_LATEST_URL);
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("admin.appinfo.updateErrNetwork")));
            return;
        }
        String latest = normalizeVersion(extractLatest(source, bodyText));
        String current = normalizeVersion(AppConfig.APP_VERSION);
        if (latest == null || current == null) {
            ctx.json(Map.of("success", false, "message", I18n.t("admin.appinfo.updateErrParse")));
            return;
        }
        int cmp = compareVersion(current, latest);
        String status = cmp < 0 ? "outdated" : (cmp > 0 ? "ahead" : "up_to_date");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("source", source);
        result.put("current", current);
        result.put("latest", latest);
        result.put("status", status);
        ctx.json(result);
    }

    private long updateCooldownRemainingMs(String key) {
        String value = cacheDao.get(key);
        if (value == null) return 0;
        try {
            long remaining = Long.parseLong(value) - System.currentTimeMillis();
            return remaining > 0 ? remaining : 0;
        } catch (NumberFormatException e) {
            return UPDATE_INTERVAL_MS;
        }
    }

    private String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "LingYggdrasil/" + AppConfig.APP_VERSION)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String extractLatest(String source, String bodyText) {
        try {
            JsonNode root = mapper.readTree(bodyText);
            JsonNode node = "github".equals(source)
                    ? root.path("tag_name")
                    : root.path("meta").path("implementationVersion");
            return node.isTextual() ? node.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeVersion(String raw) {
        if (raw == null) return null;
        String version = raw.trim();
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1).trim();
        }
        return VERSION_PATTERN.matcher(version).matches() ? version : null;
    }

    private static int compareVersion(String current, String latest) {
        int[] a = parseVersion(current);
        int[] b = parseVersion(latest);
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
