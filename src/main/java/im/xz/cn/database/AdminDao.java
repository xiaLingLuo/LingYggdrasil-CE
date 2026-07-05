package im.xz.cn.database;

import im.xz.cn.model.Admin;
import im.xz.cn.model.enums.AdminRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDao {
    private final DatabaseManager db;

    public AdminDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(Admin admin) {
        db.executeUpdate(
            "INSERT INTO admins (id, username, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            admin.getId(), admin.getUsername(), admin.getEmail(), admin.getPasswordHash(),
            admin.getRole().toDbValue(), admin.getCreatedAt()
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
            System.err.println("AdminDao.findAll failed: " + e.getMessage());
        }
        return admins;
    }

    public void update(Admin admin) {
        db.executeUpdate(
            "UPDATE admins SET username = ?, email = ?, password_hash = ?, role = ? WHERE id = ?",
            admin.getUsername(), admin.getEmail(), admin.getPasswordHash(),
            admin.getRole().toDbValue(), admin.getId()
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
            System.err.println("AdminDao query failed: " + e.getMessage());
        }
        return null;
    }
}
