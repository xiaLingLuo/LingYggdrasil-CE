package im.xz.cn.model;

import im.xz.cn.model.enums.AdminRole;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Admin {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private AdminRole role;
    private String createdAt;

    public Admin(String id, String username, String email, String passwordHash, AdminRole role, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static Admin fromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String roleStr = rs.getString("role");
        AdminRole role = AdminRole.fromDbValue(roleStr);
        String createdAt = rs.getString("created_at");
        return new Admin(id, username, email, passwordHash, role, createdAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public AdminRole getRole() { return role; }
    public void setRole(AdminRole role) { this.role = role; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
