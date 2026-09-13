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

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String nickname;
    private String permGroup = "default";
    private boolean emailVerified;
    private String createdAt;
    private String lastLogin;
    private String registeredIp;
    private String lastLoginIp;
    private String displayProfileId;
    private String theme = "light";
    private String language = "zh-CN";

    public User(String id, String username, String email, String passwordHash, String nickname,
                boolean emailVerified, String createdAt, String lastLogin) {
        this(id, username, email, passwordHash, nickname, emailVerified, createdAt, lastLogin, null, null, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                boolean emailVerified, String createdAt, String lastLogin, String registeredIp) {
        this(id, username, email, passwordHash, nickname, emailVerified, createdAt, lastLogin, registeredIp, null, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                boolean emailVerified, String createdAt, String lastLogin,
                String registeredIp, String lastLoginIp) {
        this(id, username, email, passwordHash, nickname, emailVerified, createdAt, lastLogin, registeredIp, lastLoginIp, null);
    }

    public User(String id, String username, String email, String passwordHash, String nickname,
                boolean emailVerified, String createdAt, String lastLogin,
                String registeredIp, String lastLoginIp, String displayProfileId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
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
        int emailVerifiedInt = rs.getInt("email_verified");
        boolean emailVerified = emailVerifiedInt != 0;
        String createdAt = rs.getString("created_at");
        String lastLogin = rs.getString("last_login");
        String registeredIp = rs.getString("registered_ip");
        String lastLoginIp = rs.getString("last_login_ip");
        String displayProfileId = rs.getString("display_profile_id");
        User user = new User(id, username, email, passwordHash, nickname, emailVerified, createdAt, lastLogin, registeredIp, lastLoginIp, displayProfileId);
        try {
            String theme = rs.getString("theme");
            if (theme != null && !theme.isBlank()) user.setTheme(theme);
        } catch (SQLException ignored) {}
        try {
            String language = rs.getString("language");
            if (language != null && !language.isBlank()) user.setLanguage(language);
        } catch (SQLException ignored) {}
        try {
            String permGroup = rs.getString("perm_group");
            if (permGroup != null && !permGroup.isBlank()) user.setPermGroup(permGroup);
        } catch (SQLException ignored) {}
        return user;
    }

    public String getDisplayName() {
        return (nickname != null && !nickname.isBlank()) ? nickname : username;
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

    public String getPermGroup() { return permGroup; }
    public void setPermGroup(String permGroup) { this.permGroup = permGroup; }

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

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
