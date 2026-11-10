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
package im.xz.cn.migration.steps.identity;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.MigrationMappingStore;
import im.xz.cn.migration.MigrationReport;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public final class PermissionGroupMigrationStep {

    public static final String STEP_ID = "v2_3_16.identity.001";
    private static final Set<String> BUILTIN_GROUPS = Set.of("default", "banned", "cancelled");

    private final DatabaseManager database;
    private final MigrationMappingStore mappings;

    public PermissionGroupMigrationStep(DatabaseManager database) {
        this.database = database;
        this.mappings = new MigrationMappingStore(database);
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        List<GroupRow> groups = Jdbc.queryMany(database, resultSet -> new GroupRow(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("permissions"),
                resultSet.getString("created_at")
        ), "SELECT id, name, permissions, created_at FROM user_perm_group ORDER BY id");

        for (GroupRow group : groups) {
            report.add("groupsSeen", 1);
            String code = group.name() == null || group.name().isBlank() ? group.id() : group.name();
            Long groupId = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                    "SELECT id FROM permission_groups WHERE code = ? AND scope = 'USER'", code);
            if (groupId == null) {
                boolean builtin = BUILTIN_GROUPS.contains(group.id()) || BUILTIN_GROUPS.contains(code);
                groupId = Jdbc.insert(database,
                        "INSERT INTO permission_groups (code, display_name, scope, builtin, created_at)"
                                + " VALUES (?, ?, 'USER', ?, ?)",
                        code, group.name(), builtin ? 1 : 0,
                        group.createdAt() == null ? Jdbc.writeInstant(Instant.now()) : group.createdAt());
                report.add("groupsCreated", 1);
            }
            mappings.record("permission-group", group.id(), Long.toString(groupId), code);

            for (String node : splitNodes(group.permissions())) {
                report.add("nodesSeen", 1);
                long existing = Jdbc.queryCount(database,
                        "SELECT COUNT(*) FROM group_permission_nodes WHERE group_id = ? AND node = ?",
                        groupId, node);
                if (existing > 0) {
                    continue;
                }
                Jdbc.update(database,
                        "INSERT INTO group_permission_nodes (group_id, node, kind) VALUES (?, ?, ?)",
                        groupId, node, node.contains(".state.") ? "STATE" : "CAPABILITY");
                report.add("nodesCreated", 1);
                if ("*".equals(node)) {
                    report.add("wildcardGroups", 1);
                }
            }
        }
        report.add("mappingGroups", mappings.count("permission-group"));
        ensureCancelledGroup(report);
        return report;
    }

    
    private void ensureCancelledGroup(MigrationReport report) {
        Long existing = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                "SELECT id FROM permission_groups WHERE code = 'cancelled' AND scope = 'USER'");
        if (existing != null) {
            return;
        }
        long groupId = Jdbc.insert(database,
                "INSERT INTO permission_groups (code, display_name, scope, builtin, created_at)"
                        + " VALUES ('cancelled', 'cancelled', 'USER', 1, ?)",
                Jdbc.writeInstant(Instant.now()));
        Jdbc.update(database,
                "INSERT INTO group_permission_nodes (group_id, node, kind) VALUES (?, ?, 'STATE')",
                groupId, "user.state.cancelled");
        mappings.record("permission-group", "cancelled", Long.toString(groupId), "builtin");
        report.add("groupsCreated", 1);
        report.add("nodesCreated", 1);
        report.add("cancelledGroupCreated", 1);
    }

    private static Set<String> splitNodes(String raw) {
        Set<String> nodes = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return nodes;
        }
        for (String part : raw.split(",")) {
            String node = part.trim();
            if (!node.isEmpty()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private record GroupRow(String id, String name, String permissions, String createdAt) {
    }
}
