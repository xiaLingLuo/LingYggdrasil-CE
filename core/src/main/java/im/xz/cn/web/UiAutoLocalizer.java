package im.xz.cn.web;

import im.xz.cn.i18n.I18n;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;


public final class UiAutoLocalizer {

    private static final List<Entry> ENTRIES = load();

    private UiAutoLocalizer() {
    }

    public static String localize(String html) {
        if (html == null || html.isEmpty() || ENTRIES.isEmpty()) {
            return html;
        }
        String result = html;
        for (Entry entry : ENTRIES) {
            if (result.indexOf(entry.text()) < 0) {
                continue;
            }
            String translated = I18n.t("uiAuto." + entry.key());
            if (translated == null || translated.isBlank()
                    || translated.equals("uiAuto." + entry.key())) {
                continue;
            }
            result = result.replace(entry.text(), translated);
        }
        return result;
    }

    private static List<Entry> load() {
        Properties properties = new Properties();
        try (InputStream input = UiAutoLocalizer.class.getResourceAsStream("/i18n/ui-auto.properties")) {
            if (input == null) {
                return List.of();
            }
            properties.load(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (Exception error) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            String text = properties.getProperty(key);
            if (text != null && !text.isBlank()) {
                entries.add(new Entry(key, text));
            }
        }
        entries.sort(Comparator.comparingInt((Entry entry) -> entry.text().length()).reversed());
        return List.copyOf(entries);
    }

    private record Entry(String key, String text) {
    }
}
