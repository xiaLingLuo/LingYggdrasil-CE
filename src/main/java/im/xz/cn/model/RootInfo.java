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

public class RootInfo {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String createdAt;
    private String lastLogin;
    private String theme = "light";
    private String language = "zh-CN";

    public RootInfo() {}

    public RootInfo(String id, String username, String email, String passwordHash, String createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static RootInfo fromResultSet(ResultSet rs) throws SQLException {
        RootInfo r = new RootInfo(
                rs.getString("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("created_at"));
        try { r.setLastLogin(rs.getString("last_login")); } catch (SQLException ignored) {}
        try {
            String theme = rs.getString("theme");
            if (theme != null && !theme.isBlank()) r.setTheme(theme);
        } catch (SQLException ignored) {}
        try {
            String language = rs.getString("language");
            if (language != null && !language.isBlank()) r.setLanguage(language);
        } catch (SQLException ignored) {}
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
