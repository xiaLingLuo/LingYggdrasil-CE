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
import im.xz.cn.model.Admin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDao {
    private static final logApi log = logApi.getLogger(AdminDao.class);
    private final DatabaseManager db;

    public AdminDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(Admin admin) {
        db.executeUpdate(
            "INSERT INTO admins (id, username, email, password_hash, created_at, perm_group) VALUES (?, ?, ?, ?, ?, ?)",
            admin.getId(), admin.getUsername(), admin.getEmail(), admin.getPasswordHash(),
            admin.getCreatedAt(),
            admin.getPermGroup() == null ? "op" : admin.getPermGroup()
        );
    }

    public Admin findById(String id) {
        return querySingle("SELECT * FROM admins WHERE id = ?", id);
    }

    public Admin findByUsername(String username) {
        return querySingle("SELECT * FROM admins WHERE username = ?", username);
    }

    public List<Admin> findAll() {
        List<Admin> admins = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM admins ORDER BY created_at ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                admins.add(Admin.fromResultSet(rs));
            }
        } catch (SQLException e) {
            log.error("AdminDao.findAll failed: {}", e.getMessage(), e);
        }
        return admins;
    }

    public void update(Admin admin) {
        db.executeUpdate(
            "UPDATE admins SET username = ?, email = ?, password_hash = ? WHERE id = ?",
            admin.getUsername(), admin.getEmail(), admin.getPasswordHash(), admin.getId()
        );
    }

    public void delete(String id) {
        db.executeUpdate("DELETE FROM admins WHERE id = ?", id);
    }

    public void updateUsername(String id, String newUsername) {
        db.executeUpdate("UPDATE admins SET username = ? WHERE id = ?", newUsername, id);
    }

    public void updateEmail(String id, String newEmail) {
        db.executeUpdate("UPDATE admins SET email = ? WHERE id = ?", newEmail, id);
    }

    public void updatePassword(String id, String newPasswordHash) {
        db.executeUpdate("UPDATE admins SET password_hash = ? WHERE id = ?", newPasswordHash, id);
    }

    public void updateTheme(String id, String theme) {
        db.executeUpdate("UPDATE admins SET theme = ? WHERE id = ?", theme, id);
    }

    public void updateLanguage(String id, String language) {
        db.executeUpdate("UPDATE admins SET language = ? WHERE id = ?", language, id);
    }

    public void updateLastLogin(String id, String lastLogin) {
        db.executeUpdate("UPDATE admins SET last_login = ? WHERE id = ?", lastLogin, id);
    }

    public void updatePermGroup(String id, String permGroup) {
        db.executeUpdate("UPDATE admins SET perm_group = ? WHERE id = ?", permGroup, id);
    }

    public int count() {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM admins");
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    private Admin querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Admin.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.error("AdminDao query failed: {}", e.getMessage(), e);
        }
        return null;
    }
}
