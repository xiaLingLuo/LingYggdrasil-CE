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

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.logging.AuditLogger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class SettingsAdminService {

    public interface AuditSink {
        void record(String actor, String action, String ip);
    }

    public record GroupView(String group, long revision, Map<String, String> values) {
        public GroupView {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }

    private final DatabaseManager database;
    private final SettingsService settings;
    private final AuditSink audit;

    public SettingsAdminService(DatabaseManager database, SettingsService settings, AuditSink audit) {
        this.database = database;
        this.settings = settings;
        this.audit = audit;
    }

    public static SettingsAdminService create(DatabaseManager database) {
        im.xz.cn.audit.AuditEventRepository auditEvents = new im.xz.cn.audit.AuditEventRepository(database);
        return new SettingsAdminService(database, SettingsService.create(database),
                (actor, action, ip) -> {
                    AuditLogger.logSensitiveOperation(actor, action, ip);
                    auditEvents.append("ADMIN", actor, action, "SETTINGS", action, "SUCCESS",
                            ip, null, Instant.now());
                });
    }

    public List<GroupView> listGroups() {
        List<String> groups = Jdbc.queryMany(database, resultSet -> resultSet.getString(1),
                "SELECT group_name FROM settings_revision ORDER BY group_name");
        List<GroupView> views = new ArrayList<>();
        for (String group : groups) {
            views.add(get(group));
        }
        return List.copyOf(views);
    }

    public GroupView get(String group) {
        SettingsService.GroupSnapshot snapshot = settings.snapshot(group);
        return new GroupView(group, snapshot.revision(), snapshot.values());
    }

    
    public GroupView commit(String group, Map<String, String> values, long expectedRevision,
                            String actor, String ip) {
        SettingsService.GroupSnapshot snapshot = settings.commit(group, values, expectedRevision);
        audit.record(actor, "SETTINGS_UPDATE:" + group, ip);
        return new GroupView(group, snapshot.revision(), snapshot.values());
    }
}
