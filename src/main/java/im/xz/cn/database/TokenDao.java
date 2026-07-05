package im.xz.cn.database;

import im.xz.cn.model.AuthToken;
import im.xz.cn.util.TimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TokenDao {
    private final DatabaseManager db;

    public TokenDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(AuthToken token) {
        db.executeUpdate(
            "INSERT INTO auth_tokens (access_token, client_token, user_id, profile_id, server_id, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            token.getAccessToken(), token.getClientToken(), token.getUserId(),
            token.getProfileId(), token.getServerId(), token.getCreatedAt(), token.getExpiresAt()
        );
    }

    public AuthToken findByAccessToken(String accessToken) {
        return querySingle("SELECT * FROM auth_tokens WHERE access_token = ?", accessToken);
    }

    public List<AuthToken> findByUserId(String userId) {
        List<AuthToken> tokens = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM auth_tokens WHERE user_id = ? ORDER BY created_at DESC")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(AuthToken.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("TokenDao.findByUserId failed: " + e.getMessage());
        }
        return tokens;
    }

    public void deleteByAccessToken(String accessToken) {
        db.executeUpdate("DELETE FROM auth_tokens WHERE access_token = ?", accessToken);
    }

    public void deleteByUserId(String userId) {
        db.executeUpdate("DELETE FROM auth_tokens WHERE user_id = ?", userId);
    }

    public void updateServerId(String accessToken, String serverId) {
        db.executeUpdate("UPDATE auth_tokens SET server_id = ? WHERE access_token = ?", serverId, accessToken);
    }

    public void updateProfileId(String accessToken, String profileId) {
        db.executeUpdate("UPDATE auth_tokens SET profile_id = ? WHERE access_token = ?", profileId, accessToken);
    }

    public void cleanExpired() {
        String now = TimeUtil.now();
        db.executeUpdate("DELETE FROM auth_tokens WHERE expires_at < ?", now);
    }

    public int countActive() {
        String now = TimeUtil.now();
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM auth_tokens WHERE expires_at >= ?", now);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    private AuthToken querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return AuthToken.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("TokenDao query failed: " + e.getMessage());
        }
        return null;
    }
}
