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
package im.xz.cn.migration.verification;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class TextureReconciliation {

    private final DatabaseManager database;

    public TextureReconciliation(DatabaseManager database) {
        this.database = database;
    }

    public Result reconcile() {
        List<String> missingBlobs = new ArrayList<>();
        List<String> missingAssets = new ArrayList<>();
        List<String> missingUploads = new ArrayList<>();
        List<String> missingPublications = new ArrayList<>();
        List<String> missingFiles = new ArrayList<>();
        List<String> missingFavorites = new ArrayList<>();
        List<String> missingLikes = new ArrayList<>();
        List<String> missingGrants = new ArrayList<>();
        Map<String, String[]> textureToTarget = new HashMap<>();

        List<String[]> rows = Jdbc.queryMany(database, resultSet -> new String[]{
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("type"),
                resultSet.getString("hash"),
                resultSet.getString("reference_type"),
                resultSet.getString("ref_owner_id")
        }, "SELECT id, user_id, type, hash, reference_type, ref_owner_id FROM textures"
                + " WHERE hash IS NOT NULL AND LENGTH(hash) = 64 ORDER BY created_at, id");

        for (String[] row : rows) {
            String textureId = row[0];
            String userId = row[1];
            String type = row[2];
            String hash = row[3];
            boolean selfOwned = row[4] == null || row[4].isBlank() || "self".equals(row[4]);

            Long blobId = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                    "SELECT id FROM texture_blobs WHERE sha256 = ?", hash);
            if (blobId == null) {
                missingBlobs.add(textureId);
                continue;
            }
            String storageKey = Jdbc.queryOne(database, resultSet -> resultSet.getString(1),
                    "SELECT storage_key FROM texture_blobs WHERE id = ?", blobId);
            if (!fileExists(storageKey)) {
                missingFiles.add(textureId + " -> " + storageKey);
            }
            Long assetId = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                    "SELECT id FROM texture_assets WHERE type = ? AND blob_id = ?", type, blobId);
            if (assetId == null) {
                missingAssets.add(textureId);
                continue;
            }
            textureToTarget.put(textureId, new String[]{userId, type, hash,
                    selfOwned ? userId : row[5], Long.toString(assetId)});
            long uploads = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM texture_uploads WHERE user_id = ? AND asset_id = ?",
                    userId, assetId);
            if (selfOwned && uploads == 0) {
                missingUploads.add(textureId);
            }
            if (isPublicInLegacy(textureId, userId)) {
                long published = Jdbc.queryCount(database,
                        "SELECT COUNT(*) FROM texture_publications"
                                + " WHERE publisher_id = ? AND asset_id = ? AND state = 'PUBLIC'",
                        userId, assetId);
                if (published == 0) {
                    missingPublications.add(textureId);
                }
            }
        }

        for (var favorite : readInteractions("texture_favorites")) {
            String[] target = textureToTarget.get(favorite.textureId());
            if (target == null) {
                missingFavorites.add(favorite.textureId());
                continue;
            }
            Long publicationId = findPublication(target[3], Long.parseLong(target[4]));
            if (publicationId == null || Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM publication_favorites WHERE user_id = ? AND publication_id = ?",
                    favorite.userId(), publicationId) == 0) {
                missingFavorites.add(favorite.userId() + ":" + favorite.textureId());
            }
        }
        for (var like : readInteractions("texture_likes")) {
            String[] target = textureToTarget.get(like.textureId());
            if (target == null) {
                missingLikes.add(like.textureId());
                continue;
            }
            Long publicationId = findPublication(target[3], Long.parseLong(target[4]));
            if (publicationId == null || Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM publication_likes WHERE user_id = ? AND publication_id = ?",
                    like.userId(), publicationId) == 0) {
                missingLikes.add(like.userId() + ":" + like.textureId());
            }
        }
        List<String[]> shares = Jdbc.queryMany(database, resultSet -> new String[]{
                resultSet.getString("owner_id"),
                resultSet.getString("friend_id"),
                resultSet.getString("texture_id")
        }, "SELECT owner_id, friend_id, texture_id FROM friend_shared_textures ORDER BY id");
        for (String[] share : shares) {
            String[] target = textureToTarget.get(share[2]);
            if (target == null) {
                missingGrants.add(share[0] + ":" + share[1] + ":" + share[2]);
                continue;
            }
            long active = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM texture_share_grants WHERE owner_id = ?"
                            + " AND recipient_id = ? AND asset_id = ? AND state = 'ACTIVE'",
                    share[0], share[1], Long.parseLong(target[4]));
            if (active == 0) {
                missingGrants.add(share[0] + ":" + share[1] + ":" + share[2]);
            }
        }

        return new Result(rows.size(), missingBlobs, missingAssets, missingUploads,
                missingPublications, missingFiles, missingLikes, missingFavorites, missingGrants);
    }

    private List<LegacyInteraction> readInteractions(String table) {
        return Jdbc.queryMany(database, resultSet -> new LegacyInteraction(
                resultSet.getString("user_id"), resultSet.getString("texture_id")),
                "SELECT user_id, texture_id FROM " + table + " ORDER BY id");
    }

    private Long findPublication(String publisherId, long assetId) {
        if (publisherId == null || publisherId.isBlank()) {
            return null;
        }
        return Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                "SELECT id FROM texture_publications WHERE publisher_id = ? AND asset_id = ?"
                        + " ORDER BY id LIMIT 1", publisherId, assetId);
    }

    private static boolean fileExists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        Path path = Path.of(storageKey);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path);
        }
        return Files.isRegularFile(path);
    }

    private boolean isPublicInLegacy(String textureId, String userId) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM texture_visibility WHERE texture_id = ? AND user_id = ? AND is_public = 1",
                textureId, userId) > 0;
    }

    private record LegacyInteraction(String userId, String textureId) {
    }

    public record Result(long texturesChecked, List<String> missingBlobs, List<String> missingAssets,
                         List<String> missingUploads, List<String> missingPublications,
                         List<String> missingFiles, List<String> missingLikes,
                         List<String> missingFavorites, List<String> missingGrants) {

        public boolean ok() {
            return missingBlobs.isEmpty() && missingAssets.isEmpty()
                    && missingUploads.isEmpty() && missingPublications.isEmpty()
                    && missingFiles.isEmpty() && missingLikes.isEmpty()
                    && missingFavorites.isEmpty() && missingGrants.isEmpty();
        }

        public String toText() {
            StringBuilder builder = new StringBuilder();
            builder.append("checked=").append(texturesChecked)
                    .append(" ok=").append(ok()).append('\n');
            append(builder, "missingBlobs", missingBlobs);
            append(builder, "missingAssets", missingAssets);
            append(builder, "missingUploads", missingUploads);
            append(builder, "missingPublications", missingPublications);
            append(builder, "missingFiles", missingFiles);
            append(builder, "missingLikes", missingLikes);
            append(builder, "missingFavorites", missingFavorites);
            append(builder, "missingGrants", missingGrants);
            return builder.toString();
        }

        private static void append(StringBuilder builder, String label, List<String> ids) {
            if (ids.isEmpty()) {
                return;
            }
            builder.append("  ").append(label).append('=').append(ids.size()).append('\n');
            for (String id : ids) {
                builder.append("    - ").append(id).append('\n');
            }
        }
    }
}
