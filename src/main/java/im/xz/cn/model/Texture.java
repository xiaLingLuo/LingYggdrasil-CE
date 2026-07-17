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

public class Texture {
    private String id;
    private String userId;
    private String type;
    private String hash;
    private String alias;
    private String originalName;
    private long size;
    private String contentType;
    private String createdAt;

    public Texture() {}

    public Texture(String id, String userId, String type, String hash, String alias,
                   String originalName, long size, String contentType, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.hash = hash;
        this.alias = alias;
        this.originalName = originalName;
        this.size = size;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public static Texture fromResultSet(ResultSet rs) throws SQLException {
        return new Texture(
            rs.getString("id"),
            rs.getString("user_id"),
            rs.getString("type"),
            rs.getString("hash"),
            rs.getString("alias"),
            rs.getString("original_name"),
            rs.getLong("size"),
            rs.getString("content_type"),
            rs.getString("created_at")
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
