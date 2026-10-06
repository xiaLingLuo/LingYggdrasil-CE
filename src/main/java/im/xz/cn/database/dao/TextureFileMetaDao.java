package im.xz.cn.database.dao;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.SqlLike;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class TextureFileMetaDao {
    private final DatabaseManager db;

    public TextureFileMetaDao(DatabaseManager db) {
        this.db = db;
    }

    public void insertFirstUpload(Connection conn, String type, String hash, String fileName,
                                  String createdAt, long size) throws SQLException {
        String sql = "mysql".equals(db.getDbType())
                ? "INSERT IGNORE INTO texture_file_meta (type, hash, first_upload_name, admin_file_name, first_uploaded_at, size) VALUES (?, ?, ?, NULL, ?, ?)"
                : "INSERT INTO texture_file_meta (type, hash, first_upload_name, admin_file_name, first_uploaded_at, size) VALUES (?, ?, ?, NULL, ?, ?) ON CONFLICT (type, hash) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, hash);
            ps.setString(3, fileName == null ? "" : fileName);
            if ("pgsql".equals(db.getDbType())) ps.setTimestamp(4, Timestamp.valueOf(createdAt));
            else ps.setString(4, createdAt);
            ps.setLong(5, size);
            ps.executeUpdate();
        }
    }

    public int updateAdminFileName(String type, String hash, String fileName) {
        int updated = db.executeUpdate(
                "UPDATE texture_file_meta SET admin_file_name = ? WHERE type = ? AND hash = ?",
                fileName, type, hash);
        if (updated > 0) return updated;
        return db.executeQuerySingle("SELECT 1 FROM texture_file_meta WHERE type = ? AND hash = ?", type, hash) == null
                ? 0 : 1;
    }

    public void delete(String type, String hash) {
        db.executeUpdate("DELETE FROM texture_file_meta WHERE type = ? AND hash = ?", type, hash);
    }

    public List<Map<String, Object>> findAdminPage(String type, String search, int limit, long offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.hash, m.first_upload_name AS original_name, " +
                "COALESCE(m.admin_file_name, m.first_upload_name) AS file_name, " +
                "m.size, m.first_uploaded_at AS created_at, COUNT(t.id) AS ref_count " +
                "FROM texture_file_meta m JOIN textures t ON t.type = m.type AND t.hash = m.hash " +
                "WHERE m.type = ? ");
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(COALESCE(m.admin_file_name, m.first_upload_name)) LIKE ? ESCAPE '!' " +
                    "OR LOWER(m.first_upload_name) LIKE ? ESCAPE '!' OR LOWER(m.hash) LIKE ? ESCAPE '!') ");
        }
        sql.append("GROUP BY m.type, m.hash, m.admin_file_name, m.first_upload_name, m.size, m.first_uploaded_at ")
                .append("ORDER BY m.first_uploaded_at DESC, m.hash ASC LIMIT ? OFFSET ?");
        if (search == null || search.isBlank()) {
            return db.executeQuery(sql.toString(), type, limit, offset);
        }
        String pattern = SqlLike.contains(search);
        return db.executeQuery(sql.toString(), type, pattern, pattern, pattern, limit, offset);
    }

    public int countAdminFiles(String type, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS cnt FROM texture_file_meta m WHERE m.type = ? " +
                "AND EXISTS (SELECT 1 FROM textures t WHERE t.type = m.type AND t.hash = m.hash) ");
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(COALESCE(m.admin_file_name, m.first_upload_name)) LIKE ? ESCAPE '!' " +
                    "OR LOWER(m.first_upload_name) LIKE ? ESCAPE '!' OR LOWER(m.hash) LIKE ? ESCAPE '!')");
        }
        Map<String, Object> result;
        if (search == null || search.isBlank()) {
            result = db.executeQuerySingle(sql.toString(), type);
        } else {
            String pattern = SqlLike.contains(search);
            result = db.executeQuerySingle(sql.toString(), type, pattern, pattern, pattern);
        }
        return result == null || result.get("cnt") == null ? 0 : ((Number) result.get("cnt")).intValue();
    }
}
