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
package im.xz.cn.settings;

import java.util.List;
import java.util.Locale;


public final class SettingsGroups {

    public static final String LEGACY = "legacy";
    public static final List<String> MIGRATED_GROUPS =
            List.of("site", "yggdrasil", "texture", "identity", "logging", "mail", LEGACY);

    private SettingsGroups() {
    }

    public static String classify(String key) {
        if (key == null) {
            return LEGACY;
        }
        String value = key.toLowerCase(Locale.ROOT);
        if (value.startsWith("mail") || value.startsWith("smtp")
                || value.contains("email_template") || value.contains("emailtemplate")) {
            return "mail";
        }
        if (value.startsWith("log") || value.startsWith("audit")
                || value.startsWith("user_action_log")) {
            return "logging";
        }
        if (value.startsWith("site") || value.startsWith("icp")
                || value.startsWith("public_security") || value.startsWith("announcement")
                || value.startsWith("footer")) {
            return "site";
        }
        if (value.startsWith("token") || value.startsWith("max_tokens")
                || value.startsWith("signature") || value.startsWith("yggdrasil")
                || value.startsWith("api_mode") || value.startsWith("uuid_version")
                || value.startsWith("auth_rate_limit") || value.startsWith("batch_query")) {
            return "yggdrasil";
        }
        if (value.startsWith("allow_download") || value.startsWith("skin")
                || value.startsWith("cape") || value.startsWith("texture")
                || value.startsWith("max_favorites")) {
            return "texture";
        }
        if (value.startsWith("registration") || value.startsWith("email_verification")
                || value.startsWith("username") || value.startsWith("profile_name")
                || value.startsWith("email_domain") || value.startsWith("max_profiles")
                || value.startsWith("max_accounts") || value.startsWith("max_blocked")
                || value.startsWith("request_")) {
            return "identity";
        }
        return LEGACY;
    }
}
