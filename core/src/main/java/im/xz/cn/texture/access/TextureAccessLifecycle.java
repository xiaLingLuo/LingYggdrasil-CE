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
package im.xz.cn.texture.access;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.profile.ProfileTextureBindingRepository;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.sharing.ShareGrantRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;


public final class TextureAccessLifecycle {

    private final DatabaseManager database;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;
    private final TextureUploadRepository uploads;
    private final PublicationRepository publications;
    private final ShareGrantRepository grants;
    private final ProfileTextureBindingRepository bindings;

    public TextureAccessLifecycle(DatabaseManager database) {
        this.database = database;
        this.assets = new TextureAssetRepository(database);
        this.blobs = new TextureBlobRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.publications = new PublicationRepository(database);
        this.grants = new ShareGrantRepository(database);
        this.bindings = new ProfileTextureBindingRepository(database);
    }

    
    public boolean deliverable(long assetId) {
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null || asset.state() != TextureAsset.AssetState.READY) {
            return false;
        }
        TextureBlob blob = blobs.findById(asset.blobId()).orElse(null);
        return blob != null && blob.state() == im.xz.cn.texture.asset.BlobState.READY;
    }

    
    public boolean qualified(String userId, long assetId) {
        if (userId == null || !deliverable(assetId)) {
            return false;
        }
        return uploads.hasActiveUpload(userId, assetId)
                || grants.hasActiveForRecipient(userId, assetId)
                || publications.hasAvailableForAsset(assetId);
    }

    
    public int reconcileAsset(long assetId, String reason) {
        int removed = 0;
        for (ProfileTextureBindingRepository.Binding binding : bindings.findByAsset(assetId)) {
            String owner = ownerOf(binding.profileId());
            if (owner == null || !qualified(owner, assetId)) {
                bindings.remove(binding.profileId(), binding.slot());
                clearLegacyMirror(binding.profileId(), binding.slot());
                record(owner, binding.profileId(), assetId, reason);
                removed++;
            }
        }
        return removed;
    }

    public void record(String userId, String profileId, long assetId, String reason) {
        Jdbc.insert(database,
                "INSERT INTO texture_access_events (user_id, profile_id, asset_id, reason, occurred_at)"
                        + " VALUES (?, ?, ?, ?, ?)",
                userId, profileId, assetId, reason == null ? "ACCESS_ENDED" : reason,
                Jdbc.writeInstant(Instant.now()));
    }

    
    public void deleteAsset(long assetId) {
        TextureAsset asset = assets.findById(assetId).orElse(null);
        if (asset == null) {
            return;
        }
        TextureBlob blob = blobs.findById(asset.blobId()).orElse(null);
        String storageKey = blob == null ? null : blob.storageKey();
        new im.xz.cn.infrastructure.persistence.UnitOfWork(database).execute(connection -> {
            for (ProfileTextureBindingRepository.Binding binding : bindings.findByAsset(assetId)) {
                clearLegacyMirror(binding.profileId(), binding.slot());
            }
            if (blob != null) {
                String column = asset.type() == TextureType.CAPE ? "cape_hash" : "skin_hash";
                Jdbc.update(database, "UPDATE player_profiles SET " + column + " = NULL WHERE " + column + " = ?",
                        blob.sha256());
            }
            Jdbc.update(database, "DELETE FROM publication_likes WHERE publication_id IN"
                    + " (SELECT id FROM texture_publications WHERE asset_id = ?)", assetId);
            Jdbc.update(database, "DELETE FROM publication_favorites WHERE publication_id IN"
                    + " (SELECT id FROM texture_publications WHERE asset_id = ?)", assetId);
            Jdbc.update(database, "DELETE FROM texture_publications WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM texture_share_grants WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM profile_texture_bindings WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM texture_access_events WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM library_entries WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM texture_uploads WHERE asset_id = ?", assetId);
            Jdbc.update(database, "DELETE FROM texture_assets WHERE id = ?", assetId);
            if (blob != null) {
                blobs.deleteIfUnreferenced(blob.id(), Instant.now());
            }
            return null;
        });
        if (blob != null && storageKey != null && blobs.findById(blob.id()).isEmpty()) {
            long legacyRefs = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM textures WHERE hash = ?", blob.sha256());
            if (legacyRefs == 0) {
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(storageKey));
                } catch (java.io.IOException ignored) {
                    
                }
            }
        }
    }

    
    public int withdrawAll(long assetId, String reason) {
        int affected = 0;
        for (var publication : publications.findAvailableByAsset(assetId)) {
            publications.withdraw(publication.id(), Instant.now());
            affected++;
        }
        return affected;
    }

    
    public int revokeGrantsForAsset(String ownerId, long assetId, String reason) {
        return grants.revokeAllForAsset(ownerId, assetId, reason, Instant.now());
    }

    public Set<String> qualifiedUsers(long assetId) {
        Set<String> users = new LinkedHashSet<>();
        for (var upload : Jdbc.queryMany(database, rs -> rs.getString("user_id"),
                "SELECT user_id FROM texture_uploads WHERE asset_id = ? AND state = 'ACTIVE'", assetId)) {
            users.add(upload);
        }
        for (var grant : Jdbc.queryMany(database, rs -> rs.getString("recipient_id"),
                "SELECT recipient_id FROM texture_share_grants WHERE asset_id = ? AND state = 'ACTIVE'", assetId)) {
            users.add(grant);
        }
        return users;
    }

    private void clearLegacyMirror(String profileId, String slot) {
        String column = "CAPE".equalsIgnoreCase(slot) ? "cape_hash" : "skin_hash";
        Jdbc.update(database, "UPDATE player_profiles SET " + column + " = NULL WHERE id = ?",
                profileId);
    }

    private String ownerOf(String profileId) {
        return Jdbc.queryOne(database, rs -> rs.getString("owner_user_id"),
                "SELECT owner_user_id FROM player_profiles WHERE id = ?", profileId);
    }
}
