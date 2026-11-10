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
package im.xz.cn.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public final class LanguagePackValidator {

    public interface ResourceLoader {
        InputStream open(String path) throws IOException;
    }

    public record Problem(String locale, String resource, String reason) {
    }

    public record Report(String defaultLocale, List<String> locales, List<Problem> problems) {

        public Report {
            locales = List.copyOf(locales);
            problems = List.copyOf(problems);
        }

        public boolean ok() {
            return problems.isEmpty();
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LanguagePackValidator() {
    }

    
    public static Report validateBundled() {
        return validate(LanguagePackValidator.class::getResourceAsStream);
    }

    static Report validate(ResourceLoader loader) {
        List<Problem> problems = new ArrayList<>();
        String defaultLocale;
        List<String> locales = new ArrayList<>();
        try (InputStream input = loader.open("/i18n/lang.json")) {
            if (input == null) {
                problems.add(new Problem("", "/i18n/lang.json", "missing language index"));
                return new Report("", locales, problems);
            }
            JsonNode root = MAPPER.readTree(input);
            JsonNode languages = root.path("languages");
            defaultLocale = root.path("default").asText("");
            languages.fieldNames().forEachRemaining(locale -> {
                locales.add(locale);
                for (String resource : expectedResources(locale)) {
                    checkResource(loader, locale, resource, problems);
                }
            });
            if (!defaultLocale.isBlank() && !languages.has(defaultLocale)) {
                problems.add(new Problem(defaultLocale, "lang.json",
                        "default locale is not listed in languages"));
            }
        } catch (IOException error) {
            problems.add(new Problem("", "/i18n/lang.json", "invalid JSON: " + error.getMessage()));
            return new Report("", locales, problems);
        }
        if (locales.isEmpty()) {
            problems.add(new Problem("", "lang.json", "no languages declared"));
        }
        return new Report(defaultLocale, locales, problems);
    }

    private static List<String> expectedResources(String locale) {
        return List.of(
                "/i18n/user/" + locale + ".json",
                "/i18n/admin/" + locale + ".json",
                "/i18n/user/index-page/" + locale + ".html");
    }

    private static void checkResource(ResourceLoader loader, String locale, String resource,
                                      List<Problem> problems) {
        try (InputStream input = loader.open(resource)) {
            if (input == null) {
                problems.add(new Problem(locale, resource, "missing"));
                return;
            }
            if (resource.endsWith(".json")) {
                JsonNode node = MAPPER.readTree(input);
                if (node == null || !node.isObject()) {
                    problems.add(new Problem(locale, resource, "bundle must be a JSON object"));
                }
            }
        } catch (IOException error) {
            problems.add(new Problem(locale, resource, "invalid: " + error.getMessage()));
        }
    }
}
