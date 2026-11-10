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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.common.IpUtil;
import im.xz.cn.config.AppConfig;
import im.xz.cn.config.MailConfig;
import im.xz.cn.config.ServerConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.i18n.AdminI18n;
import im.xz.cn.mail.MailService;
import im.xz.cn.settings.RevisionConflictException;
import im.xz.cn.settings.SecretStore;
import im.xz.cn.settings.SettingsAdminService;
import im.xz.cn.settings.SettingsRuntime;
import im.xz.cn.settings.SettingsSchema;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class AdminSettingsHandler {

    private final SettingsAdminService service;
    private final DatabaseManager database;
    private final SecretStore secrets;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminSettingsHandler(DatabaseManager database) {
        this.database = database;
        this.service = SettingsAdminService.create(database);
        this.secrets = SecretStore.create(database);
    }

    public void schema(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.view")) return;
        List<Map<String, Object>> groups = new ArrayList<>();
        for (SettingsSchema.Group group : SettingsSchema.GROUPS) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("name", group.name());
            view.put("label", label(group.labelKey()));
            view.put("description", description(group.descriptionKey()));
            List<Map<String, Object>> fields = new ArrayList<>();
            for (SettingsSchema.Field field : group.fields()) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("key", field.key());
                f.put("type", field.type());
                f.put("label", label(field.labelKey()));
                f.put("description", description(field.descriptionKey()));
                f.put("placeholder", field.placeholderKey() == null ? "" : label(field.placeholderKey()));
                f.put("secret", field.secret());
                f.put("advanced", field.advanced());
                f.put("restartRequired", field.restartRequired());
                if (field.min() != null) f.put("min", field.min());
                if (field.max() != null) f.put("max", field.max());
                if (field.options() != null) {
                    List<Map<String, Object>> options = new ArrayList<>();
                    for (SettingsSchema.Option option : field.options()) {
                        options.add(Map.of("value", option.value(), "label", label(option.labelKey())));
                    }
                    f.put("options", options);
                }
                fields.add(f);
            }
            view.put("fields", fields);
            view.put("actions", group.actions());
            groups.add(view);
        }
        ctx.json(Map.of("success", true, "groups", groups));
    }

    public void list(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.view")) return;
        List<Map<String, Object>> groups = service.listGroups().stream()
                .filter(view -> im.xz.cn.settings.SettingsGroups.MIGRATED_GROUPS.contains(view.group()))
                .map(this::toMap)
                .toList();
        ctx.json(Map.of("success", true, "groups", groups));
    }

    public void get(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.view")) return;
        String group = ctx.pathParam("group");
        if (SettingsSchema.group(group) == null
                && !im.xz.cn.settings.SettingsGroups.MIGRATED_GROUPS.contains(group)) {
            ctx.status(404).json(Map.of("success", false, "code", "SETTINGS_GROUP_NOT_FOUND",
                    "message", "Settings group not found"));
            return;
        }
        ctx.json(Map.of("success", true, "group", toMap(service.get(group))));
    }

    public void update(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        String group = ctx.pathParam("group");
        SettingsSchema.Group schema = SettingsSchema.group(group);
        if (schema == null) {
            ctx.status(404).json(Map.of("success", false, "code", "SETTINGS_GROUP_NOT_FOUND",
                    "message", "Settings group not found"));
            return;
        }
        Map<String, Object> body;
        try {
            body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid JSON body"));
            return;
        }
        Object revisionValue = body.get("revision");
        if (!(revisionValue instanceof Number revisionNumber)) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "revision is required"));
            return;
        }
        Object valuesValue = body.get("values");
        if (!(valuesValue instanceof Map<?, ?> rawValues)) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "values object is required"));
            return;
        }
        Map<String, SettingsSchema.Field> definitions = new LinkedHashMap<>();
        for (SettingsSchema.Field field : schema.fields()) {
            definitions.put(field.key(), field);
        }
        Map<String, String> runtimeValues = new LinkedHashMap<>();
        Map<String, String> storedValues = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawValues.entrySet()) {
            String key = String.valueOf(entry.getKey());
            SettingsSchema.Field field = definitions.get(key);
            if (field == null) {
                
                if (service.get(group).values().containsKey(key)) {
                    continue;
                }
                ctx.status(400).json(Map.of("success", false, "code", "UNKNOWN_SETTING",
                        "message", "Unknown setting: " + key));
                return;
            }
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            String error = validate(field, value);
            if (error != null) {
                ctx.status(422).json(Map.of("success", false, "code", "VALIDATION_FAILED",
                        "field", key, "message", error));
                return;
            }
            runtimeValues.put(key, value);
            if (!"mail_password".equals(key)) {
                storedValues.put(key, value);
            }
        }
        String actor = ctx.sessionAttribute("adminUsername");
        try {
            
            SettingsAdminService.GroupView view = service.commit(
                    group, storedValues, revisionNumber.longValue(), actor, IpUtil.getClientIp(ctx));
            String mailPassword = runtimeValues.get("mail_password");
            if (mailPassword != null && !mailPassword.isEmpty()) {
                secrets.put("mail.smtp_password", mailPassword, "settings.mail");
            }
            SettingsRuntime.apply(database, runtimeValues, true, true);
            ctx.json(Map.of("success", true, "group", toMap(view)));
        } catch (RevisionConflictException conflict) {
            ctx.status(409).json(Map.of(
                    "success", false,
                    "code", "REVISION_CONFLICT",
                    "message", "Settings were modified concurrently; reload and retry",
                    "revision", conflict.actualRevision()));
        } catch (IllegalArgumentException | IllegalStateException error) {
            ctx.status(422).json(Map.of("success", false, "code", "VALIDATION_FAILED",
                    "message", error.getMessage() == null ? "Invalid settings" : error.getMessage()));
        }
    }

    public void testMail(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.edit")) return;
        Map<String, Object> body;
        try {
            body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid JSON body"));
            return;
        }
        Object toValue = body.get("to");
        String to = toValue == null ? "" : String.valueOf(toValue).trim();
        if (to.isEmpty()) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Target email is required"));
            return;
        }
        MailConfig mail = AppConfig.getInstance().getMailConfig();
        if (!mail.isEnabled()) {
            ctx.status(400).json(Map.of("success", false, "code", "MAIL_DISABLED",
                    "message", "Mail service is disabled"));
            return;
        }
        String content = body.get("content") == null ? "Test Email" : String.valueOf(body.get("content"));
        boolean sent = new MailService(mail).sendTestEmail(to, content);
        ctx.status(sent ? 200 : 500).json(Map.of("success", sent,
                "message", sent ? "Test mail sent" : "Test mail failed"));
    }

    private String validate(SettingsSchema.Field field, String value) {
        switch (field.type()) {
            case "boolean" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    return field.key() + " must be true or false";
                }
            }
            case "number" -> {
                try {
                    int parsed = Integer.parseInt(value);
                    if (field.min() != null && parsed < field.min()) return field.key() + " is too small";
                    if (field.max() != null && parsed > field.max()) return field.key() + " is too large";
                } catch (NumberFormatException error) {
                    return field.key() + " must be a number";
                }
            }
            case "url" -> {
                if (!value.isBlank() && !value.matches("^https?://[^\\s]+$")) {
                    return field.key() + " must start with http:// or https://";
                }
            }
            case "email" -> {
                if (!value.isBlank() && !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    return field.key() + " is not a valid email";
                }
            }
            case "select", "multiselect" -> {
                if (!value.isBlank() && field.options() != null) {
                    List<String> allowed = field.options().stream().map(SettingsSchema.Option::value).toList();
                    for (String part : value.split("[|,]")) {
                        if (!allowed.contains(part)) return field.key() + " has an invalid option";
                    }
                }
            }
            default -> {
            }
        }
        return null;
    }

    private Map<String, Object> toMap(SettingsAdminService.GroupView view) {
        Map<String, String> values = new LinkedHashMap<>(view.values());
        values.remove("yggdrasil_private_key");
        values.remove("mail_password");
        if ("site".equals(view.group())) {
            ServerConfig server = ServerConfig.getInstance();
            values.put("user_domain", server.getUserDomain());
            values.put("admin_domain", server.getAdminDomain());
            values.put("api_domain", server.getYggdrasilDomain());
            values.put("common_domain", server.getCommonDomain());
        } else if ("mail".equals(view.group())) {
            MailConfig mail = AppConfig.getInstance().getMailConfig();
            values.put("mail_enabled", String.valueOf(mail.isEnabled()));
            values.put("mail_host", mail.getHost() == null ? "" : mail.getHost());
            values.put("mail_port", String.valueOf(mail.getPort()));
            values.put("mail_username", mail.getUsername() == null ? "" : mail.getUsername());
            values.put("mail_from", mail.getFrom() == null ? "" : mail.getFrom());
            values.put("mail_tls", String.valueOf(mail.isTls()));
            if (secrets.exists("mail.smtp_password")) {
                values.put("mail_password", "");
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("group", view.group());
        map.put("revision", view.revision());
        map.put("values", values);
        return map;
    }

    private static String label(String key) {
        if (key == null) return "";
        if (!key.contains(".")) return key;
        String value = AdminI18n.t(key);
        return value == null || value.isBlank() ? key : value;
    }

    private static String description(String key) {
        if (key == null) return "";
        if (!key.contains(".")) return key;
        String value = AdminI18n.t(key);
        return value == null || value.isBlank() ? "" : value;
    }
}
