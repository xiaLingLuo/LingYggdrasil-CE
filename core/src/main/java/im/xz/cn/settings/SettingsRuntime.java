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

import im.xz.cn.common.Treasure;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.ServerConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;

import java.util.Map;


public final class SettingsRuntime {

    private SettingsRuntime() {
    }

    public static void apply(DatabaseManager database, Map<String, String> values) {
        apply(database, values, true);
    }

    
    public static void apply(DatabaseManager database, Map<String, String> values, boolean persistSecrets) {
        apply(database, values, persistSecrets, false);
    }

    public static void apply(DatabaseManager database, Map<String, String> values,
                             boolean persistSecrets, boolean allowBlankDomains) {
        SystemConfig system = SystemConfig.getInstance();
        ServerConfig server = ServerConfig.getInstance();
        MailConfig mail = AppConfig.getInstance().getMailConfig();
        SecretStore secrets = SecretStore.create(database);
        boolean mailChanged = false;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            if (key.endsWith("_domain")) {
                
                if (!value.isBlank() || allowBlankDomains) {
                    server.updateDomainSetting(key, value);
                }
            } else if ("mail_password".equals(key)) {
                if (!value.isEmpty()) {
                    if (persistSecrets) {
                        secrets.put("mail.smtp_password", value, "settings.mail");
                    }
                    mail.setPassword(value);
                    mailChanged = true;
                }
            } else if (key.startsWith("mail_template_") || "mail_test_content".equals(key)) {
                system.updateSetting(key, value);
            } else if (key.startsWith("mail_")) {
                applyMail(mail, key, value);
                mailChanged = true;
            } else if ("treasure_enabled".equals(key)) {
                boolean enabled = Boolean.parseBoolean(value);
                system.updateSetting(key, value);
                Treasure.setEnabled(database, enabled);
            } else if ("yggdrasil_private_key".equals(key)) {
                if (!value.isEmpty()) {
                    if (persistSecrets) {
                        secrets.put("yggdrasil.private_key", value, "settings.yggdrasil");
                    }
                    system.setYggdrasilPrivateKey(value);
                }
            } else {
                system.updateSetting(key, value);
            }
        }
        if (mailChanged) {
            AppConfig.getInstance().saveConfig();
        }
    }

    private static void applyMail(MailConfig mail, String key, String value) {
        switch (key) {
            case "mail_enabled" -> mail.setEnabled(Boolean.parseBoolean(value));
            case "mail_host" -> mail.setHost(value);
            case "mail_port" -> {
                try {
                    mail.setPort(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "mail_username" -> mail.setUsername(value);
            case "mail_from" -> mail.setFrom(value);
            case "mail_tls" -> mail.setTls(Boolean.parseBoolean(value));
            default -> {
            }
        }
    }
}
