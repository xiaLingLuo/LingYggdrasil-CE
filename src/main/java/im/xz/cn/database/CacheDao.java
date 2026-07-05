package im.xz.cn.database;

import im.xz.cn.util.TimeUtil;

public class CacheDao {
    private final DatabaseManager db;

    public CacheDao(DatabaseManager db) {
        this.db = db;
    }

    public void put(String key, String value, String type, int ttlSeconds) {
        String now = TimeUtil.now();
        String expiresAt = TimeUtil.plusSeconds(ttlSeconds);

        String dbType = db.getDbType();
        if ("mysql".equalsIgnoreCase(dbType)) {
            db.executeUpdate(
                "INSERT INTO cache_store (cache_key, cache_value, cache_type, created_at, expires_at) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE cache_value = VALUES(cache_value), cache_type = VALUES(cache_type), created_at = VALUES(created_at), expires_at = VALUES(expires_at)",
                key, value, type, now, expiresAt
            );
        } else if ("pgsql".equalsIgnoreCase(dbType)) {
            db.executeUpdate(
                "INSERT INTO cache_store (cache_key, cache_value, cache_type, created_at, expires_at) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (cache_key) DO UPDATE SET cache_value = EXCLUDED.cache_value, cache_type = EXCLUDED.cache_type, created_at = EXCLUDED.created_at, expires_at = EXCLUDED.expires_at",
                key, value, type, now, expiresAt
            );
        } else {
            db.executeUpdate(
                "INSERT OR REPLACE INTO cache_store (cache_key, cache_value, cache_type, created_at, expires_at) VALUES (?, ?, ?, ?, ?)",
                key, value, type, now, expiresAt
            );
        }
    }

    public String get(String key) {
        String now = TimeUtil.now();
        var result = db.executeQuerySingle(
            "SELECT cache_value FROM cache_store WHERE cache_key = ? AND expires_at > ?",
            key, now
        );
        if (result != null && result.get("cache_value") != null) {
            return String.valueOf(result.get("cache_value"));
        }
        return null;
    }

    public void delete(String key) {
        db.executeUpdate("DELETE FROM cache_store WHERE cache_key = ?", key);
    }

    public void deleteByType(String type) {
        db.executeUpdate("DELETE FROM cache_store WHERE cache_type = ?", type);
    }

    public void cleanExpired() {
        String now = TimeUtil.now();
        db.executeUpdate("DELETE FROM cache_store WHERE expires_at < ?", now);
    }

    public boolean exists(String key) {
        String now = TimeUtil.now();
        var result = db.executeQuerySingle(
            "SELECT COUNT(*) AS cnt FROM cache_store WHERE cache_key = ? AND expires_at > ?",
            key, now
        );
        if (result != null && result.get("cnt") != null) {
            return ((Number) result.get("cnt")).intValue() > 0;
        }
        return false;
    }
}
