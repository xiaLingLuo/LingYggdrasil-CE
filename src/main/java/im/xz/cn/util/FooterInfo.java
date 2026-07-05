package im.xz.cn.util;

import im.xz.cn.config.SystemConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                cachedYear = response.body().trim();
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
            if (hasIcp) footer.append(escapeHtml(icp));
            if (hasIcp && hasPsr) footer.append(" | ");
            if (hasPsr) footer.append(escapeHtml(psr));
            return html.replace(FOOTER_PLACEHOLDER, footer.toString());
        } else {
            return html.replace(FOOTER_PLACEHOLDER, "");
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    public static void shutdown() {
        scheduler.shutdown();
    }
}
