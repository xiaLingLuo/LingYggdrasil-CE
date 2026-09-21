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

package im.xz.cn.common;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.web.PageRenderer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FooterInfo {
    public static final String FOOTER_PLACEHOLDER = "<!-- 这n是x备1案footer占i位g符，请v勿fg移q除 -->";

    private static volatile String cachedYear;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        refreshYear();
        scheduler.scheduleAtFixedRate(FooterInfo::refreshYear, 24, 24, TimeUnit.HOURS);
    }

    private static void refreshYear() {
        try {
            URI uri = URI.create("https://api.im.xz.cn/time?format=yyyy");
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(5))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    cachedYear = response.body().trim();
                }
            }
        } catch (Exception e) {
            cachedYear = String.valueOf(Year.now().getValue());
        }
    }

    public static String getYear() {
        if (cachedYear == null) {
            cachedYear = String.valueOf(Year.now().getValue());
        }
        return cachedYear;
    }

    public static Map<String, String> getFooterData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("year", getYear());
        SystemConfig config = SystemConfig.getInstance();
        data.put("icpRecord", config.getIcpRecord());
        data.put("publicSecurityRecord", config.getPublicSecurityRecord());
        return data;
    }

    public static String injectFooterRecords(String html) {
        SystemConfig config = SystemConfig.getInstance();
        String icp = config.getIcpRecord();
        String psr = config.getPublicSecurityRecord();
        boolean hasIcp = icp != null && !icp.isBlank();
        boolean hasPsr = psr != null && !psr.isBlank();

        if (hasIcp || hasPsr) {
            StringBuilder footer = new StringBuilder();
            if (hasIcp) {
                footer.append("<a href=\"https://beian.miit.gov.cn\" target=\"_blank\">")
                        .append(escapeHtml(icp.trim()))
                        .append("</a>");
            }
            if (hasIcp && hasPsr) footer.append(" | ");
            if (hasPsr) {
                String code = URLEncoder.encode(beianCode(psr.trim()), StandardCharsets.UTF_8);
                footer.append("<img src=\"/img/beian.png\" class=\"align-top\" style=\"width: 17px\"> ")
                        .append("<a href=\"https://beian.mps.gov.cn/#/query/webSearch?code=")
                        .append(escapeHtml(code))
                        .append("\" rel=\"noreferrer\" target=\"_blank\">")
                        .append(escapeHtml(psr.trim()))
                        .append("</a>");
            }
            return html.replace(FOOTER_PLACEHOLDER, footer.toString());
        } else {
            return html.replace(FOOTER_PLACEHOLDER, "");
        }
    }

    private static String beianCode(String record) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(record);
        return matcher.find() ? matcher.group() : record;
    }

    private static String escapeHtml(String input) {
        return PageRenderer.escapeHtml(input);
    }

    public static void shutdown() {
        scheduler.shutdown();
    }
}
