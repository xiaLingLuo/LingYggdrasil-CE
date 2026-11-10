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
package im.xz.cn.database.dao;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.logApi;
import im.xz.cn.model.PermGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserPermGroupDao {
    private static final logApi log = logApi.getLogger(UserPermGroupDao.class);
    private final DatabaseManager db;

    public UserPermGroupDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(PermGroup g) {
        db.executeUpdate(
            "INSERT INTO user_perm_group (id, name, permissions, created_at) VALUES (?, ?, ?, ?)",
            g.getId(), g.getName(), g.getPermissions(), g.getCreatedAt());
        mirrorToModern(g);
    }

    public PermGroup findById(String id) {
        return querySingle("SELECT * FROM user_perm_group WHERE id = ?", id);
    }

    public PermGroup findByName(String name) {
        PermGroup modern = findModern(name);
        if (modern != null) {
            return modern;
        }
        PermGroup legacy = querySingle("SELECT * FROM user_perm_group WHERE name = ?", name);
        if (legacy != null) {
            mirrorToModern(legacy);
        }
        return legacy;
    }

    
    private PermGroup findModern(String code) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT g.id, g.code, g.display_name FROM permission_groups g"
                             + " WHERE g.code = ? AND g.scope = 'USER'")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String groupId = rs.getString("id");
                StringBuilder permissions = new StringBuilder();
                try (PreparedStatement nodes = conn.prepareStatement(
                        "SELECT node FROM group_permission_nodes WHERE group_id = ? ORDER BY node")) {
                    nodes.setLong(1, Long.parseLong(groupId));
                    try (ResultSet nodeRows = nodes.executeQuery()) {
                        while (nodeRows.next()) {
                            if (permissions.length() > 0) permissions.append(',');
                            permissions.append(nodeRows.getString(1));
                        }
                    }
                }
                return new PermGroup(groupId, rs.getString("code"),
                        permissions.toString(), null);
            }
        } catch (Exception error) {
            return null;
        }
    }

    private void mirrorToModern(PermGroup legacy) {
        try (Connection conn = db.getConnection()) {
            Long groupId = null;
            try (PreparedStatement find = conn.prepareStatement(
                    "SELECT id FROM permission_groups WHERE code = ? AND scope = 'USER'")) {
                find.setString(1, legacy.getName());
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) groupId = rs.getLong(1);
                }
            }
            if (groupId == null) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO permission_groups (code, display_name, scope, builtin, created_at)"
                                + " VALUES (?, ?, 'USER', ?, ?)")) {
                    insert.setString(1, legacy.getName());
                    insert.setString(2, legacy.getName());
                    insert.setInt(3, 0);
                    insert.setString(4, legacy.getCreatedAt());
                    insert.executeUpdate();
                }
                try (PreparedStatement find = conn.prepareStatement(
                        "SELECT id FROM permission_groups WHERE code = ? AND scope = 'USER'")) {
                    find.setString(1, legacy.getName());
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next()) groupId = rs.getLong(1);
                    }
                }
            }
            if (groupId == null || legacy.getPermissions() == null) return;
            try (PreparedStatement clear = conn.prepareStatement(
                    "DELETE FROM group_permission_nodes WHERE group_id = ?")) {
                clear.setLong(1, groupId);
                clear.executeUpdate();
            }
            for (String node : legacy.getPermissions().split(",")) {
                String key = node.trim();
                if (key.isEmpty()) continue;
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO group_permission_nodes (group_id, node, kind) VALUES (?, ?, ?)")) {
                    insert.setLong(1, groupId);
                    insert.setString(2, key);
                    insert.setString(3, key.contains(".state.") ? "STATE" : "CAPABILITY");
                    insert.executeUpdate();
                }
            }
        } catch (Exception ignored) {
            
        }
    }

    public List<PermGroup> findAll() {
        List<PermGroup> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user_perm_group ORDER BY created_at ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(PermGroup.fromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("UserPermGroupDao.findAll failed: {}", e.getMessage(), e);
            throw new RuntimeException("UserPermGroupDao.findAll failed", e);
        }
        return list;
    }

    public void updatePermissions(String id, String permissions) {
        db.executeUpdate("UPDATE user_perm_group SET permissions = ? WHERE id = ?", permissions, id);
        PermGroup group = findById(id);
        if (group != null) mirrorToModern(group);
    }

    public void updateName(String id, String name) {
        db.executeUpdate("UPDATE user_perm_group SET name = ? WHERE id = ?", name, id);
    }

    public void delete(String id) {
        PermGroup group = findById(id);
        db.executeUpdate("DELETE FROM user_perm_group WHERE id = ?", id);
        if (group != null) {
            db.executeUpdate("DELETE FROM permission_groups WHERE code = ? AND scope = 'USER'", group.getName());
        }
    }

    public int count() {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM user_perm_group");
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    public int countUsersUsing(String groupName) {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM users WHERE perm_group = ?", groupName);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    private PermGroup querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PermGroup.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.error("UserPermGroupDao query failed: {}", e.getMessage(), e);
            throw new RuntimeException("UserPermGroupDao query failed", e);
        }
        return null;
    }
}


