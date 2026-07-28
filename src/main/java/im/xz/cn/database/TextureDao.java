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
package im.xz.cn.database;

import im.xz.cn.model.Texture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextureDao {
    private final DatabaseManager db;

    public TextureDao(DatabaseManager db) {
        this.db = db;
    }

    public void insert(Texture texture) {
        db.executeUpdate(
            "INSERT INTO textures (id, user_id, type, hash, alias, original_name, size, content_type, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            texture.getId(), texture.getUserId(), texture.getType(), texture.getHash(),
            texture.getAlias(), texture.getOriginalName(), texture.getSize(),
            texture.getContentType(), texture.getCreatedAt()
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
            System.err.println("TextureDao.findAllByHash failed: " + e.getMessage());
        }
        return list;
    }

    public Texture findByUserAndHash(String userId, String type, String hash) {
        return querySingle("SELECT * FROM textures WHERE user_id = ? AND type = ? AND hash = ?", userId, type, hash);
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
            System.err.println("TextureDao.findByUserId failed: " + e.getMessage());
        }
        return list;
    }

    public List<Texture> findPublicTextures(String type, String afterCreatedAt, int limit) {
        List<Texture> list = new ArrayList<>();
        try (Connection conn = db.getConnection()) {
            String orderExpr = "(SELECT COUNT(*) FROM texture_likes tl WHERE tl.texture_id = t.id) + (SELECT COUNT(*) FROM texture_favorites tf WHERE tf.texture_id = t.id) * 5";
            String sql;
            if (type != null && !type.isEmpty()) {
                if (afterCreatedAt != null && !afterCreatedAt.isEmpty()) {
                    sql = "SELECT t.* FROM textures t JOIN texture_visibility tv ON t.id = tv.texture_id WHERE tv.is_public = 1 AND t.type = ? AND t.created_at < ? ORDER BY " + orderExpr + " DESC, t.created_at DESC LIMIT ?";
                } else {
                    sql = "SELECT t.* FROM textures t JOIN texture_visibility tv ON t.id = tv.texture_id WHERE tv.is_public = 1 AND t.type = ? ORDER BY " + orderExpr + " DESC, t.created_at DESC LIMIT ?";
                }
            } else {
                if (afterCreatedAt != null && !afterCreatedAt.isEmpty()) {
                    sql = "SELECT t.* FROM textures t JOIN texture_visibility tv ON t.id = tv.texture_id WHERE tv.is_public = 1 AND t.created_at < ? ORDER BY " + orderExpr + " DESC, t.created_at DESC LIMIT ?";
                } else {
                    sql = "SELECT t.* FROM textures t JOIN texture_visibility tv ON t.id = tv.texture_id WHERE tv.is_public = 1 ORDER BY " + orderExpr + " DESC, t.created_at DESC LIMIT ?";
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                if (type != null && !type.isEmpty()) {
                    ps.setString(idx++, type);
                }
                if (afterCreatedAt != null && !afterCreatedAt.isEmpty()) {
                    ps.setString(idx++, afterCreatedAt);
                }
                ps.setInt(idx, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(Texture.fromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("TextureDao.findPublicTextures failed: " + e.getMessage());
        }
        return list;
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
            System.err.println("TextureDao.findByUserId failed: " + e.getMessage());
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
            System.err.println("TextureDao.findAll failed: " + e.getMessage());
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
            System.err.println("TextureDao query failed: " + e.getMessage());
        }
        return null;
    }
}
