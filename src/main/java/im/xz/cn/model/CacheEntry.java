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
