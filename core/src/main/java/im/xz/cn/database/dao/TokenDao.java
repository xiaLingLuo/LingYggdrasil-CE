package im.xz.cn.database.dao;

import im.xz.cn.common.TimeUtil;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.logging.logApi;
import im.xz.cn.model.AuthToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;


public class TokenDao {
    private static final logApi log = logApi.getLogger(TokenDao.class);
    private final DatabaseManager db;

    public TokenDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(AuthToken token) {
        db.executeUpdate(
                "INSERT INTO access_sessions (token_digest, client_token, user_id, profile_id,"
                        + " auth_source, credential_version, issued_at, expires_at, server_id)"
                        + " VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)",
                digest(token.getAccessToken()), token.getClientToken(), token.getUserId(),
                token.getProfileId(), token.getAuthSource() == null ? "WORLD_TREE" : token.getAuthSource(),
                token.getCreatedAt(), token.getExpiresAt(), token.getServerId());
    }

    public AuthToken findByAccessToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }
        String digest = digest(accessToken);
        AuthToken token = queryByDigest(
                "SELECT * FROM access_sessions WHERE token_digest = ?", digest, accessToken);
        if (token == null && accessToken.matches("[0-9a-fA-F]{64}")) {
            
            token = queryByDigest("SELECT * FROM access_sessions WHERE token_digest = ?",
                    accessToken.toLowerCase(java.util.Locale.ROOT), accessToken);
        }
        return token;
    }

    public List<AuthToken> findByUserId(String userId) {
        List<AuthToken> tokens = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM access_sessions WHERE user_id = ? AND revoked_at IS NULL"
                             + " ORDER BY issued_at DESC")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(map(rs, rs.getString("token_digest")));
                }
            }
        } catch (SQLException e) {
            log.error("TokenDao.findByUserId failed: {}", e.getMessage(), e);
            throw new RuntimeException("TokenDao.findByUserId failed", e);
        }
        return tokens;
    }

    public void deleteByAccessToken(String accessToken) {
        if (accessToken == null) {
            return;
        }
        int changed = db.executeUpdate("DELETE FROM access_sessions WHERE token_digest = ?",
                digest(accessToken));
        if (changed == 0 && accessToken.matches("[0-9a-fA-F]{64}")) {
            db.executeUpdate("DELETE FROM access_sessions WHERE token_digest = ?",
                    accessToken.toLowerCase(java.util.Locale.ROOT));
        }
    }

    public void deleteByUserId(String userId) {
        db.executeUpdate("DELETE FROM access_sessions WHERE user_id = ?", userId);
    }

    public void updateServerId(String accessToken, String serverId) {
        db.executeUpdate("UPDATE access_sessions SET server_id = ? WHERE token_digest = ?",
                serverId, digest(accessToken));
    }

    public void updateProfileId(String accessToken, String profileId) {
        db.executeUpdate("UPDATE access_sessions SET profile_id = ? WHERE token_digest = ?",
                profileId, digest(accessToken));
    }

    
    public record JoinTicketRow(String accessToken, String userId, String profileId,
                                String expiresAt, String ip) {
    }

    public JoinTicketRow findJoinTicket(String serverId, String profileId) {
        java.util.Map<String, Object> row = db.executeQuerySingle(
                "SELECT token_digest, user_id, profile_id, join_expires_at, join_ip FROM access_sessions"
                        + " WHERE server_id = ? AND profile_id = ?", serverId, profileId);
        if (row == null) {
            return null;
        }
        return new JoinTicketRow(text(row, "token_digest"), text(row, "user_id"),
                text(row, "profile_id"), text(row, "join_expires_at"), text(row, "join_ip"));
    }

    public void updateJoinTicket(String accessToken, String serverId, String profileId,
                                 String expiresAt, String ip) {
        db.executeUpdate("UPDATE access_sessions SET server_id = ?, profile_id = ?,"
                        + " join_expires_at = ?, join_ip = ? WHERE token_digest = ?",
                serverId, profileId, expiresAt, ip, digest(accessToken));
    }

    private static String text(java.util.Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : String.valueOf(value);
    }

    public void cleanExpired() {
        db.executeUpdate("DELETE FROM access_sessions WHERE expires_at < ?",
                Jdbc.writeInstant(Instant.now()));
    }

    public List<AuthToken> findSessionsInScope(String userId, String profileId) {
        String sql = profileId == null
                ? "SELECT * FROM access_sessions WHERE user_id = ?"
                        + " AND (profile_id IS NULL OR profile_id = '') AND revoked_at IS NULL"
                        + " ORDER BY issued_at DESC, id DESC"
                : "SELECT * FROM access_sessions WHERE user_id = ? AND profile_id = ?"
                        + " AND revoked_at IS NULL ORDER BY issued_at DESC, id DESC";
        List<AuthToken> tokens = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            if (profileId != null) {
                ps.setString(2, profileId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(map(rs, rs.getString("token_digest")));
                }
            }
        } catch (SQLException e) {
            log.error("TokenDao.findSessionsInScope failed: {}", e.getMessage(), e);
            throw new RuntimeException("TokenDao.findSessionsInScope failed", e);
        }
        return tokens;
    }

    public int countActive() {
        var result = db.executeQuerySingle(
                "SELECT COUNT(*) AS cnt FROM access_sessions WHERE expires_at >= ?"
                        + " AND revoked_at IS NULL", Jdbc.writeInstant(Instant.now()));
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    private AuthToken queryByDigest(String sql, String digest, String accessToken) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, digest);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, accessToken);
                }
            }
        } catch (SQLException e) {
            log.error("TokenDao query failed: {}", e.getMessage(), e);
            throw new RuntimeException("TokenDao query failed", e);
        }
        return null;
    }

    private static AuthToken map(ResultSet rs, String accessToken) throws SQLException {
        return new AuthToken(
                accessToken,
                rs.getString("client_token"),
                rs.getString("user_id"),
                rs.getString("profile_id"),
                rs.getString("server_id"),
                rs.getString("issued_at"),
                rs.getString("expires_at"),
                rs.getString("auth_source"));
    }

    private static String digest(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}

