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
package im.xz.cn.database.dao;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.logApi;
import im.xz.cn.model.Texture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class TextureDao {
    private static final logApi log = logApi.getLogger(TextureDao.class);
    private final DatabaseManager db;

    public record PublicTexture(Texture texture, long popularity) {}

    private record PageCursor(long popularity, String createdAt, String id) {}

    public TextureDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(Texture texture) {
        db.executeUpdate(
            "INSERT INTO textures (id, user_id, type, hash, alias, original_name, size, content_type, created_at, reference_type, ref_owner_id, ref_created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            texture.getId(), texture.getUserId(), texture.getType(), texture.getHash(),
            texture.getAlias(), texture.getOriginalName(), texture.getSize(),
            texture.getContentType(), texture.getCreatedAt(),
            texture.getReferenceType(), texture.getRefOwnerId(), texture.getRefCreatedAt()
        );
    }

    public Texture findById(String id) {
        return querySingle("SELECT * FROM textures WHERE id = ?", id);
    }

    public Texture findByHash(String type, String hash) {
        return querySingle("SELECT * FROM textures WHERE type = ? AND hash = ?", type, hash);
    }

    public List<Texture> findAllByHash(String type, String hash) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM textures WHERE type = ? AND hash = ?")) {
            ps.setString(1, type);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findAllByHash failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findAllByHash failed", e);
        }
        return list;
    }

    public Texture findByUserAndHash(String userId, String type, String hash) {
        return querySingle("SELECT * FROM textures WHERE user_id = ? AND type = ? AND hash = ?", userId, type, hash);
    }

    public boolean hasReference(String userId, String type, String hash) {
        var row = db.executeQuerySingle("SELECT 1 FROM textures WHERE user_id = ? AND type = ? AND hash = ?", userId, type, hash);
        return row != null;
    }

    public boolean holdsTexture(String userId, String type, String hash) {
        return hasReference(userId, type, hash);
    }

    public List<Texture> findRefsByOwner(String type, String hash, String refOwnerId) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM textures WHERE type = ? AND hash = ? AND ref_owner_id = ? "
                             + "AND (reference_type = 'public' OR reference_type LIKE 'friend:%')")) {
            ps.setString(1, type);
            ps.setString(2, hash);
            ps.setString(3, refOwnerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findRefsByOwner failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findRefsByOwner failed", e);
        }
        return list;
    }

    public List<Texture> findFriendRefsBetween(String userA, String userB) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM textures WHERE reference_type LIKE 'friend:%' AND "
                             + "((user_id = ? AND ref_owner_id = ?) OR (user_id = ? AND ref_owner_id = ?))")) {
            ps.setString(1, userA);
            ps.setString(2, userB);
            ps.setString(3, userB);
            ps.setString(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findFriendRefsBetween failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findFriendRefsBetween failed", e);
        }
        return list;
    }

    public void deleteRef(String userId, String type, String hash, String refType) {
        db.executeUpdate("DELETE FROM textures WHERE user_id = ? AND type = ? AND hash = ? AND reference_type = ?", userId, type, hash, refType);
    }

    public void deleteAnyRef(String userId, String type, String hash) {
        db.executeUpdate("DELETE FROM textures WHERE user_id = ? AND type = ? AND hash = ? AND reference_type != 'self'", userId, type, hash);
    }

    public void deleteRefByOwner(String userId, String type, String hash, String refOwnerId) {
        db.executeUpdate("DELETE FROM textures WHERE user_id = ? AND type = ? AND hash = ? AND reference_type LIKE 'friend:%' AND ref_owner_id = ?", userId, type, hash, refOwnerId);
    }

    public void deleteFriendRef(String userId, String type, String hash, String refOwnerId) {
        db.executeUpdate("DELETE FROM textures WHERE user_id = ? AND type = ? AND hash = ? AND reference_type LIKE 'friend:%' AND ref_owner_id = ?", userId, type, hash, refOwnerId);
    }

    public void deleteRefsByOwner(String type, String hash, String refOwnerId) {
        db.executeUpdate("DELETE FROM textures WHERE type = ? AND hash = ? AND ref_owner_id = ? AND (reference_type = 'public' OR reference_type LIKE 'friend:%')", type, hash, refOwnerId);
    }

    public void deleteAllRefsByHash(String type, String hash, String excludeUserId) {
        db.executeUpdate("DELETE FROM textures WHERE type = ? AND hash = ? AND (reference_type = 'public' OR reference_type LIKE 'friend:%') AND user_id != ?", type, hash, excludeUserId);
    }

    public int countPublicByHash(String type, String hash, String excludeUserId) {
        var row = db.executeQuerySingle(
            "SELECT COUNT(*) AS cnt FROM textures t JOIN texture_visibility tv ON t.id = tv.texture_id WHERE t.type = ? AND t.hash = ? AND t.user_id != ? AND tv.is_public = 1",
            type, hash, excludeUserId);
        if (row != null && row.get("cnt") != null) {
            return ((Number) row.get("cnt")).intValue();
        }
        return 0;
    }

    public void deleteFriendRefs(String user1Id, String user2Id) {
        db.executeUpdate("DELETE FROM textures WHERE reference_type LIKE 'friend:%' AND ((user_id = ? AND ref_owner_id = ?) OR (user_id = ? AND ref_owner_id = ?))",
            user1Id, user2Id, user2Id, user1Id);
    }

    public void deleteAllRefsBetweenUsers(String user1Id, String user2Id) {
        db.executeUpdate("DELETE FROM textures WHERE (reference_type = 'public' OR reference_type LIKE 'friend:%') AND ((user_id = ? AND ref_owner_id = ?) OR (user_id = ? AND ref_owner_id = ?))",
            user1Id, user2Id, user2Id, user1Id);
    }

    public List<Texture> findByUserId(String userId, String type) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM textures WHERE user_id = ? AND type = ? ORDER BY created_at DESC")) {
            ps.setString(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findByUserId failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findByUserId failed", e);
        }
        return list;
    }

    public List<Texture> findSelfUploaded(String userId, String type) {
        return executeSelfUploadedQuery(userId, type);
    }

    private List<Texture> executeSelfUploadedQuery(String userId, String type) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM textures WHERE user_id = ? AND type = ? AND (reference_type IS NULL OR reference_type = '' OR reference_type = 'self') ORDER BY created_at DESC, id DESC")) {
            ps.setString(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.executeSelfUploadedQuery failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.executeSelfUploadedQuery failed", e);
        }
        return list;
    }

    public List<PublicTexture> findPublicTextures(String type, String after, int limit) {
        List<PublicTexture> list = new ArrayList<>();
        PageCursor cursor = decodeCursor(after);
        String legacyAfter = after != null && !after.isEmpty() && cursor == null ? after : null;
        String popularityExpr = "COALESCE(l.like_count, 0) + COALESCE(f.favorite_count, 0) * 5";
        StringBuilder sql = new StringBuilder("SELECT t.*, ").append(popularityExpr).append(" AS popularity ")
                .append("FROM textures t ")
                .append("JOIN texture_visibility tv ON t.id = tv.texture_id ")
                .append("LEFT JOIN (SELECT texture_id, COUNT(*) AS like_count FROM texture_likes GROUP BY texture_id) l ON l.texture_id = t.id ")
                .append("LEFT JOIN (SELECT texture_id, COUNT(*) AS favorite_count FROM texture_favorites GROUP BY texture_id) f ON f.texture_id = t.id ")
                .append("WHERE tv.is_public = 1 ");
        if (type != null && !type.isEmpty()) sql.append("AND t.type = ? ");
        if (legacyAfter != null) {
            sql.append("AND t.created_at < ? ");
        } else if (cursor != null) {
            sql.append("AND (").append(popularityExpr).append(" < ? OR (")
                    .append(popularityExpr).append(" = ? AND (t.created_at < ? OR (t.created_at = ? AND t.id < ?)))) ");
        }
        sql.append("ORDER BY popularity DESC, t.created_at DESC, t.id DESC LIMIT ?");
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (type != null && !type.isEmpty()) {
                    ps.setString(idx++, type);
                }
                if (legacyAfter != null) {
                    ps.setString(idx++, legacyAfter);
                } else if (cursor != null) {
                    ps.setLong(idx++, cursor.popularity());
                    ps.setLong(idx++, cursor.popularity());
                    ps.setString(idx++, cursor.createdAt());
                    ps.setString(idx++, cursor.createdAt());
                    ps.setString(idx++, cursor.id());
                }
                ps.setInt(idx, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new PublicTexture(Texture.fromResultSet(rs), rs.getLong("popularity")));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findPublicTextures failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findPublicTextures failed", e);
        }
        return list;
    }

    public String encodeCursor(PublicTexture texture) {
        String raw = texture.popularity() + "\n" + texture.texture().getCreatedAt() + "\n" + texture.texture().getId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PageCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\n", 3);
            if (parts.length != 3) return null;
            long popularity = Long.parseLong(parts[0]);
            if (popularity < 0 || parts[1].isEmpty() || parts[2].isEmpty()) return null;
            return new PageCursor(popularity, parts[1], parts[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<Texture> findByUserId(String userId) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM textures WHERE user_id = ? ORDER BY created_at DESC")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findByUserId failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findByUserId failed", e);
        }
        return list;
    }

    public List<Texture> findAll(String type) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM textures WHERE type = ? ORDER BY created_at DESC")) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Texture.fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao.findAll failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao.findAll failed", e);
        }
        return list;
    }

    public void delete(String id) {
        db.executeUpdate("DELETE FROM textures WHERE id = ?", id);
    }

    public int countByUserId(String userId, String type) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM textures WHERE user_id = ? AND type = ?", userId, type);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public long sumSizeByUserId(String userId, String type) {
        var result = db.executeQuerySingle("SELECT COALESCE(SUM(size), 0) AS total FROM textures WHERE user_id = ? AND type = ?", userId, type);
        if (result != null && result.get("total") != null) {
            return ((Number) result.get("total")).longValue();
        }
        return 0;
    }

    public int countByType(String type) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM textures WHERE type = ?", type);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public int countByHash(String type, String hash) {
        var result = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM textures WHERE type = ? AND hash = ?", type, hash);
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue();
        }
        return 0;
    }

    public void updateAlias(String id, String alias) {
        db.executeUpdate("UPDATE textures SET alias = ? WHERE id = ?", alias, id);
    }

    public void updateRefAlias(String userId, String type, String hash, String alias) {
        db.executeUpdate("UPDATE textures SET alias = ? WHERE user_id = ? AND type = ? AND hash = ?", alias, userId, type, hash);
    }

    public List<Map<String, Object>> queryRaw(String sql, Object... params) {
        return db.executeQuery(sql, params);
    }

    private Texture querySingle(String sql, Object... params) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Texture.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.error("TextureDao query failed: {}", e.getMessage(), e);
            throw new RuntimeException("TextureDao query failed", e);
        }
        return null;
    }
}
