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
import im.xz.cn.model.RootInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RootInfoDao {
    private static final logApi log = logApi.getLogger(RootInfoDao.class);
    private final DatabaseManager db;

    public RootInfoDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(RootInfo r) {
        db.executeUpdate(
            "INSERT INTO root_info (id, username, email, password_hash, created_at, last_login, theme, language) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            r.getId(), r.getUsername(), r.getEmail(), r.getPasswordHash(), r.getCreatedAt(),
            r.getLastLogin(), r.getTheme(), r.getLanguage()
        );
    }

    public RootInfo findById(String id) {
        return querySingle("SELECT * FROM root_info WHERE id = ?", id);
    }

    public RootInfo findByUsername(String username) {
        return querySingle("SELECT * FROM root_info WHERE username = ?", username);
    }

    public int count() {
        var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM root_info");
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    public void updateTheme(String id, String theme) {
        db.executeUpdate("UPDATE root_info SET theme = ? WHERE id = ?", theme, id);
    }

    public void updateLanguage(String id, String language) {
        db.executeUpdate("UPDATE root_info SET language = ? WHERE id = ?", language, id);
    }

    public void updateLastLogin(String id, String lastLogin) {
        db.executeUpdate("UPDATE root_info SET last_login = ? WHERE id = ?", lastLogin, id);
    }

    public void updatePassword(String id, String passwordHash) {
        db.executeUpdate("UPDATE root_info SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public void updateUsername(String id, String username) {
        db.executeUpdate("UPDATE root_info SET username = ? WHERE id = ?", username, id);
    }

    public void updateEmail(String id, String email) {
        db.executeUpdate("UPDATE root_info SET email = ? WHERE id = ?", email, id);
    }

    private RootInfo querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return RootInfo.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.error("RootInfoDao query failed: {}", e.getMessage(), e);
        }
        return null;
    }
}
