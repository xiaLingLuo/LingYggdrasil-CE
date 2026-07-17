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
package im.xz.cn.model;

import im.xz.cn.model.enums.UserRole;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private UserRole role;
    private boolean emailVerified;
    private String createdAt;
    private String lastLogin;
    private String registeredIp;
    private String lastLoginIp;
    private String displayProfileId;

    public User(String id, String username, String email, String passwordHash, String nickname,
                UserRole role, boolean emailVerified, String createdAt, String lastLogin) {
        this(id, username, email, passwordHash, nickname, role, emailVerified, createdAt, lastLogin, null, null, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                UserRole role, boolean emailVerified, String createdAt, String lastLogin, String registeredIp) {
        this(id, username, email, passwordHash, nickname, role, emailVerified, createdAt, lastLogin, registeredIp, null, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                UserRole role, boolean emailVerified, String createdAt, String lastLogin,
                String registeredIp, String lastLoginIp) {
        this(id, username, email, passwordHash, nickname, role, emailVerified, createdAt, lastLogin, registeredIp, lastLoginIp, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                UserRole role, boolean emailVerified, String createdAt, String lastLogin,
                String registeredIp, String lastLoginIp, String displayProfileId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.registeredIp = registeredIp;
        this.lastLoginIp = lastLoginIp;
        this.displayProfileId = displayProfileId;
    }

    public static User fromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String nickname = rs.getString("nickname");
        String roleStr = rs.getString("role");
        UserRole role = UserRole.fromDbValue(roleStr);
        int emailVerifiedInt = rs.getInt("email_verified");
        boolean emailVerified = emailVerifiedInt != 0;
        String createdAt = rs.getString("created_at");
        String lastLogin = rs.getString("last_login");
        String registeredIp = rs.getString("registered_ip");
        String lastLoginIp = rs.getString("last_login_ip");
        String displayProfileId = rs.getString("display_profile_id");
        return new User(id, username, email, passwordHash, nickname, role, emailVerified, createdAt, lastLogin, registeredIp, lastLoginIp, displayProfileId);
    }

    public String getDisplayName() {
        return nickname != null ? nickname : username;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public String getRegisteredIp() { return registeredIp; }
    public void setRegisteredIp(String registeredIp) { this.registeredIp = registeredIp; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    public String getDisplayProfileId() { return displayProfileId; }
    public void setDisplayProfileId(String displayProfileId) { this.displayProfileId = displayProfileId; }
}
