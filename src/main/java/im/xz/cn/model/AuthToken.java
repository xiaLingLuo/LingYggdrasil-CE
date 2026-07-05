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
