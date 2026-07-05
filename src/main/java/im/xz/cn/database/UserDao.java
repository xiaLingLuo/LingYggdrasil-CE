package im.xz.cn.database;

import im.xz.cn.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final DatabaseManager db;

    public UserDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(User user) {
        db.executeUpdate(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, email_verified, created_at, last_login, registered_ip, last_login_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash(),
            user.getNickname(), user.getRole().toDbValue(), user.isEmailVerified() ? 1 : 0,
            user.getCreatedAt(), user.getLastLogin(), user.getRegisteredIp(), user.getLastLoginIp()
        );
    }

    public User findById(String id) {
        return querySingle("SELECT * FROM users WHERE id = ?", id);
    }

    public User findByUsername(String username) {
        return querySingle("SELECT * FROM users WHERE username = ?", username);
    }

    public User findByEmail(String email) {
        return querySingle("SELECT * FROM users WHERE email = ?", email);
    }

    public User findByUsernameOrEmail(String login) {
        return querySingle("SELECT * FROM users WHERE username = ? OR email = ?", login, login);
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users ORDER BY created_at ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(User.fromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDao.findAll failed: " + e.getMessage());
        }
        return users;
    }

    public void update(User user) {
        db.executeUpdate(
            "UPDATE users SET username = ?, email = ?, password_hash = ?, nickname = ?, role = ?, email_verified = ?, last_login = ?, registered_ip = ?, last_login_ip = ? WHERE id = ?",
            user.getUsername(), user.getEmail(), user.getPasswordHash(),
            user.getNickname(), user.getRole().toDbValue(), user.isEmailVerified() ? 1 : 0,
            user.getLastLogin(), user.getRegisteredIp(), user.getLastLoginIp(), user.getId()
        );
    }

    public void delete(String id) {
        db.executeUpdate("DELETE FROM users WHERE id = ?", id);
    }

    public void updateRole(String id, String role) {
        db.executeUpdate("UPDATE users SET role = ? WHERE id = ?", role, id);
    }

    public void updateUsername(String id, String newUsername) {
        db.executeUpdate("UPDATE users SET username = ? WHERE id = ?", newUsername, id);
    }

    public void updateEmail(String id, String newEmail) {
        db.executeUpdate("UPDATE users SET email = ? WHERE id = ?", newEmail, id);
    }

    public void updatePassword(String id, String newPasswordHash) {
        db.executeUpdate("UPDATE users SET password_hash = ? WHERE id = ?", newPasswordHash, id);
    }

    public void updateNickname(String id, String nickname) {
        db.executeUpdate("UPDATE users SET nickname = ? WHERE id = ?", nickname, id);
    }

    public void updateLastLogin(String id, String timestamp) {
        db.executeUpdate("UPDATE users SET last_login = ? WHERE id = ?", timestamp, id);
    }

    public void updateLastLoginWithIp(String id, String timestamp, String ip) {
        db.executeUpdate("UPDATE users SET last_login = ?, last_login_ip = ? WHERE id = ?", timestamp, ip, id);
    }

    public void updateRegisteredIp(String id, String ip) {
        db.executeUpdate("UPDATE users SET registered_ip = ? WHERE id = ?", ip, id);
    }

    public void setEmailVerified(String id, boolean verified) {
        db.executeUpdate("UPDATE users SET email_verified = ? WHERE id = ?", verified ? 1 : 0, id);
    }

    public int count() {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM users");
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public int countByIp(String ip) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM users WHERE registered_ip = ?", ip);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    private User querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return User.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDao query failed: " + e.getMessage());
        }
        return null;
    }
}
