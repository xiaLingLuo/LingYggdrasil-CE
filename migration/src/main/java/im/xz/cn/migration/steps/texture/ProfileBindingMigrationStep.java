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
package im.xz.cn.migration.steps.texture;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.MigrationReport;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.sharing.ShareGrantRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


public final class ProfileBindingMigrationStep {

    public static final String STEP_ID = "v2_3_16.profile.001";

    private final DatabaseManager database;
    private final TextureBlobRepository blobs;
    private final TextureAssetRepository assets;
    private final TextureUploadRepository uploads;
    private final PublicationRepository publications;
    private final ShareGrantRepository grants;

    public ProfileBindingMigrationStep(DatabaseManager database) {
        this.database = database;
        this.blobs = new TextureBlobRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.publications = new PublicationRepository(database);
        this.grants = new ShareGrantRepository(database);
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        for (ProfileRow row : readProfiles()) {
            report.add("profilesSeen", 1);
            bind(row, TextureType.SKIN, row.skinRef(), report);
            bind(row, TextureType.CAPE, row.capeRef(), report);
        }
        return report;
    }

    private void bind(ProfileRow row, TextureType type, String reference, MigrationReport report) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        report.add("referencesSeen", 1);
        String hash = extractHash(reference);
        if (hash == null) {
            report.add("unresolvedReferences", 1);
            report.warn("cannot resolve texture hash from reference: profileId=" + row.id());
            return;
        }
        Optional<TextureBlob> blob = blobs.findBySha256(hash);
        if (blob.isEmpty()) {
            report.add("missingBlobs", 1);
            report.warn("texture blob missing for profile binding: profileId=" + row.id() + " hash=" + hash);
            return;
        }
        Optional<TextureAsset> asset = assets.findByTypeAndBlob(type, blob.get().id());
        if (asset.isEmpty()) {
            report.add("missingAssets", 1);
            report.warn("texture asset missing for profile binding: profileId=" + row.id()
                    + " type=" + type + " hash=" + hash);
            return;
        }
        long assetId = asset.get().id();
        if (!hasAccess(row.userId(), assetId)) {
            report.add("noAccessReferences", 1);
            report.warn("profile texture has no valid source; binding skipped: profileId=" + row.id());
            return;
        }
        long existing = Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM profile_texture_bindings WHERE profile_id = ? AND slot = ?",
                row.id(), type.name());
        if (existing > 0) {
            return;
        }
        Jdbc.update(database,
                "INSERT INTO profile_texture_bindings (profile_id, slot, asset_id, skin_model, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                row.id(), type.name(), assetId, normalizeSkinModel(row.skinModel()),
                Jdbc.writeInstant(Instant.now()));
        report.add("bindingsCreated", 1);
    }

    private boolean hasAccess(String userId, long assetId) {
        return uploads.hasActiveUpload(userId, assetId)
                || publications.hasAvailableForAsset(assetId)
                || grants.hasActiveForRecipient(userId, assetId);
    }

    private List<ProfileRow> readProfiles() {
        try {
            return Jdbc.queryMany(database, resultSet -> new ProfileRow(
                            resultSet.getString("id"),
                            resultSet.getString("user_id"),
                            resultSet.getString("skin_url"),
                            resultSet.getString("cape_url"),
                            resultSet.getString("skin_model")),
                    "SELECT id, user_id, skin_url, cape_url, skin_model FROM player_profiles"
                            + " ORDER BY created_at, id");
        } catch (RuntimeException legacyMissing) {
            
            return Jdbc.queryMany(database, resultSet -> new ProfileRow(
                            resultSet.getString("id"),
                            resultSet.getString("user_id"),
                            resultSet.getString("skin_hash"),
                            resultSet.getString("cape_hash"),
                            resultSet.getString("skin_model")),
                    "SELECT id, user_id, skin_hash, cape_hash, skin_model FROM player_profiles"
                            + " ORDER BY created_at, id");
        }
    }

    
    static String extractHash(String reference) {
        if (reference == null) {
            return null;
        }
        String value = reference.trim();
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        value = value.toLowerCase(Locale.ROOT);
        if (value.endsWith(".png")) {
            value = value.substring(0, value.length() - 4);
        }
        return value.length() == 64 && value.matches("[0-9a-f]{64}") ? value : null;
    }

    private static String normalizeSkinModel(String legacy) {
        if (legacy != null && legacy.trim().equalsIgnoreCase("slim")) {
            return "SLIM";
        }
        return "DEFAULT";
    }

    private record ProfileRow(String id, String userId, String skinRef, String capeRef, String skinModel) {
    }
}
