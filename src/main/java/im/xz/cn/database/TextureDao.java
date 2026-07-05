package im.xz.cn.database;

import im.xz.cn.model.Texture;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public void updateAlias(String id, String alias) {
        db.executeUpdate("UPDATE textures SET alias = ? WHERE id = ?", alias, id);
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
