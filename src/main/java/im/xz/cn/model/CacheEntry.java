package im.xz.cn.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CacheEntry {
    private String cacheKey;
    private String cacheValue;
    private String cacheType;
    private String createdAt;
    private String expiresAt;

    public CacheEntry(String cacheKey, String cacheValue, String cacheType, String createdAt, String expiresAt) {
        this.cacheKey = cacheKey;
        this.cacheValue = cacheValue;
        this.cacheType = cacheType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static CacheEntry fromResultSet(ResultSet rs) throws SQLException {
        String cacheKey = rs.getString("cache_key");
        String cacheValue = rs.getString("cache_value");
        String cacheType = rs.getString("cache_type");
        String createdAt = rs.getString("created_at");
        String expiresAt = rs.getString("expires_at");
        return new CacheEntry(cacheKey, cacheValue, cacheType, createdAt, expiresAt);
    }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public String getCacheValue() { return cacheValue; }
    public void setCacheValue(String cacheValue) { this.cacheValue = cacheValue; }

    public String getCacheType() { return cacheType; }
    public void setCacheType(String cacheType) { this.cacheType = cacheType; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
