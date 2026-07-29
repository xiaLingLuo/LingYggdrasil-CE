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
    /*
     * reference_type 仅代表材质的引用来源，与权限管理无关。
     *   self   — 来自自己上传
     *   friend — 来自好友共享
     *   public — 来自公共材质库收藏
     *
     * 无论何种引用类型，使用者只能操作自己这个引用的"副本"：
     *   - 可以修改自己的别名
     *   - 拥有对该材质文件的访问权
     *   - 不能直接对原始材质文件进行任何变更
     */
    private String referenceType;
    private String refOwnerId;
    private String refCreatedAt;

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
        this.referenceType = "self";
    }

    public static Texture fromResultSet(ResultSet rs) throws SQLException {
        Texture t = new Texture(
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
        try { t.setReferenceType(rs.getString("reference_type")); } catch (SQLException ignored) {}
        try { t.setRefOwnerId(rs.getString("ref_owner_id")); } catch (SQLException ignored) {}
        try { t.setRefCreatedAt(rs.getString("ref_created_at")); } catch (SQLException ignored) {}
        return t;
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

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public String getRefOwnerId() { return refOwnerId; }
    public void setRefOwnerId(String refOwnerId) { this.refOwnerId = refOwnerId; }

    public String getRefCreatedAt() { return refCreatedAt; }
    public void setRefCreatedAt(String refCreatedAt) { this.refCreatedAt = refCreatedAt; }
}
