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
package im.xz.cn.texture.asset;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public final class TextureAssetManagementService {

    private static final String BASE_SELECT = "SELECT a.id, a.blob_id, b.sha256, a.type, a.width,"
            + " a.height, a.admin_file_name, a.author_name, a.skin_model, a.state, a.created_at,"
            + " (SELECT u.original_name FROM texture_uploads u WHERE u.asset_id = a.id"
            + "  ORDER BY u.id LIMIT 1) AS first_upload_name,"
            + " (SELECT COUNT(*) FROM library_entries le WHERE le.asset_id = a.id) AS reference_count"
            + " FROM texture_assets a JOIN texture_blobs b ON b.id = a.blob_id";

    private final DatabaseManager database;
    private final im.xz.cn.texture.access.TextureAccessLifecycle lifecycle;
    private final Jdbc.RowMapper<AssetRow> mapper = resultSet -> new AssetRow(
            resultSet.getLong("id"),
            resultSet.getLong("blob_id"),
            resultSet.getString("sha256"),
            resultSet.getString("type"),
            resultSet.getInt("width"),
            resultSet.getInt("height"),
            resultSet.getString("admin_file_name"),
            resultSet.getString("author_name"),
            resultSet.getString("skin_model"),
            resultSet.getString("state"),
            Jdbc.readInstant(resultSet, "created_at"),
            resultSet.getString("first_upload_name"),
            resultSet.getLong("reference_count")
    );

    public TextureAssetManagementService(DatabaseManager database) {
        this.database = database;
        this.lifecycle = new im.xz.cn.texture.access.TextureAccessLifecycle(database);
    }

    public record AssetRow(long id, long blobId, String hash, String type, int width, int height,
                           String adminFileName, String authorName, String skinModel, String state,
                           Instant createdAt, String firstUploadName, long referenceCount) {

        
        public String authorDisplayName() {
            return authorName == null || authorName.isBlank() ? "佚名" : authorName;
        }
    }

    public List<AssetRow> page(String search, int limit, long offset) {
        return page(search, null, limit, offset);
    }

    public List<AssetRow> page(String search, String type, int limit, long offset) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        long safeOffset = Math.max(0, offset);
        Filter filter = filter(search, type);
        List<Object> params = new ArrayList<>(filter.params());
        params.add(safeLimit);
        params.add(safeOffset);
        return Jdbc.queryMany(database, mapper,
                BASE_SELECT + filter.sql() + " ORDER BY a.id DESC LIMIT ? OFFSET ?",
                params.toArray());
    }

    public long count(String search) {
        return count(search, null);
    }

    public long count(String search, String type) {
        Filter filter = filter(search, type);
        Long count = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                "SELECT COUNT(*) FROM texture_assets a JOIN texture_blobs b ON b.id = a.blob_id"
                        + filter.sql(), filter.params().toArray());
        return count == null ? 0L : count;
    }

    public String typeOf(long assetId) {
        return Jdbc.queryOne(database, resultSet -> resultSet.getString("type"),
                "SELECT type FROM texture_assets WHERE id = ?", assetId);
    }

    public AssetRow find(long assetId) {
        return Jdbc.queryOne(database, mapper, BASE_SELECT + " WHERE a.id = ?", assetId);
    }

    public void updateAuthor(long assetId, String author) {
        String normalized = author == null ? null : author.trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }
        if (normalized != null && normalized.length() > 128) {
            throw new IllegalArgumentException("author name too long");
        }
        Jdbc.update(database, "UPDATE texture_assets SET author_name = ? WHERE id = ?",
                normalized, assetId);
    }

    
    public void updateDisplayName(long assetId, String displayName) {
        String normalized = displayName == null ? null : displayName.trim();
        if (normalized != null && normalized.isEmpty()) {
            normalized = null;
        }
        if (normalized != null && normalized.length() > 128) {
            throw new IllegalArgumentException("display name too long");
        }
        Jdbc.update(database, "UPDATE texture_assets SET admin_file_name = ? WHERE id = ?",
                normalized, assetId);
    }

    public void updateSkinModel(long assetId, String model) {
        String normalized = model == null ? "DEFAULT" : model.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"DEFAULT".equals(normalized) && !"SLIM".equals(normalized)) {
            throw new IllegalArgumentException("invalid skin model");
        }
        Jdbc.update(database, "UPDATE texture_assets SET skin_model = ? WHERE id = ?", normalized, assetId);
    }

    public void delete(long assetId) {
        lifecycle.deleteAsset(assetId);
    }

    private Filter filter(String search, String type) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        String normalizedType = type == null ? null : type.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedType != null && !normalizedType.isBlank()) {
            sql.append(" WHERE a.type = ?");
            params.add(normalizedType);
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            sql.append(params.isEmpty() ? " WHERE (" : " AND (");
            sql.append("LOWER(COALESCE(a.admin_file_name, '')) LIKE ? ESCAPE '!'"
                    + " OR LOWER(COALESCE(a.author_name, '')) LIKE ? ESCAPE '!'"
                    + " OR LOWER(b.sha256) LIKE ? ESCAPE '!'"
                    + " OR LOWER(a.type) LIKE ? ESCAPE '!')");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return new Filter(sql.toString(), params);
    }

    
    private static String escapeLike(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private record Filter(String sql, List<Object> params) {
    }
}
