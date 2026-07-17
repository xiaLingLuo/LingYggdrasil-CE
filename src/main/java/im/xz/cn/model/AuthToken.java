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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuthToken {
    private String accessToken;
    private String clientToken;
    private String userId;
    private String profileId;
    private String serverId;
    private String createdAt;
    private String expiresAt;

    public AuthToken(String accessToken, String clientToken, String userId, String profileId,
                     String serverId, String createdAt, String expiresAt) {
        this.accessToken = accessToken;
        this.clientToken = clientToken;
        this.userId = userId;
        this.profileId = profileId;
        this.serverId = serverId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static AuthToken fromResultSet(ResultSet rs) throws SQLException {
        String accessToken = rs.getString("access_token");
        String clientToken = rs.getString("client_token");
        String userId = rs.getString("user_id");
        String profileId = rs.getString("profile_id");
        String serverId = rs.getString("server_id");
        String createdAt = rs.getString("created_at");
        String expiresAt = rs.getString("expires_at");
        return new AuthToken(accessToken, clientToken, userId, profileId, serverId, createdAt, expiresAt);
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean isExpired() {
        if (expiresAt == null || expiresAt.isEmpty()) return true;
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiresAt, FORMATTER);
            return LocalDateTime.now().isAfter(expiry);
        } catch (Exception e) {
            return true;
        }
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getClientToken() { return clientToken; }
    public void setClientToken(String clientToken) { this.clientToken = clientToken; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
