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
import im.xz.cn.migration.MigrationMappingStore;
import im.xz.cn.migration.MigrationReport;
import im.xz.cn.migration.legacy.v2_3_16.LegacyTexture;
import im.xz.cn.migration.legacy.v2_3_16.LegacyTextureReader;
import im.xz.cn.texture.asset.BlobState;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.library.LibraryEntry;
import im.xz.cn.texture.library.LibraryEntryRepository;
import im.xz.cn.texture.library.TextureUpload;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.publication.TexturePublication;
import im.xz.cn.texture.sharing.ShareGrantRepository;
import im.xz.cn.texture.sharing.TextureShareGrant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public final class TextureMigrationStep {

    public static final String STEP_ID = "v2_3_16.texture.001";
    private static final DateTimeFormatter LEGACY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseManager database;
    private final LegacyTextureReader legacy;
    private final TextureBlobRepository blobs;
    private final TextureAssetRepository assets;
    private final TextureUploadRepository uploads;
    private final LibraryEntryRepository library;
    private final PublicationRepository publications;
    private final InteractionRepository interactions;
    private final ShareGrantRepository grants;
    private final MigrationMappingStore mappings;

    public TextureMigrationStep(DatabaseManager database) {
        this.database = database;
        this.legacy = new LegacyTextureReader(database);
        this.blobs = new TextureBlobRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.library = new LibraryEntryRepository(database);
        this.publications = new PublicationRepository(database);
        this.interactions = new InteractionRepository(database);
        this.grants = new ShareGrantRepository(database);
        this.mappings = new MigrationMappingStore(database);
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        Map<String, LegacyTexture> byId = new HashMap<>();
        Map<String, Long> assetByTextureId = new HashMap<>();

        List<LegacyTexture> rows = legacy.readAllTextures();
        for (LegacyTexture row : rows) {
            report.add("texturesSeen", 1);
            byId.put(row.id(), row);
            if (row.hash() == null || row.hash().length() != 64) {
                report.add("skippedInvalidHash", 1);
                report.warn("invalid hash skipped: textureId=" + row.id());
                continue;
            }
            try {
                long assetId = ensureAsset(row, report, row.selfOwned());
                assetByTextureId.put(row.id(), assetId);
                mappings.record("texture-asset", row.id(), Long.toString(assetId), row.type());
                if (!row.selfOwned()) {
                    report.add("referenceRowsMigrated", 1);
                }
            } catch (RuntimeException error) {
                report.add("failedTextures", 1);
                report.warn("texture migration failed: id=" + row.id() + " reason=" + error.getMessage());
            }
        }

        
        Set<String> interactedTextureIds = new HashSet<>();
        for (LegacyTextureReader.LegacyInteraction interaction : legacy.readFavorites()) {
            interactedTextureIds.add(interaction.textureId());
        }
        for (LegacyTextureReader.LegacyInteraction interaction : legacy.readLikes()) {
            interactedTextureIds.add(interaction.textureId());
        }
        for (String textureId : interactedTextureIds) {
            LegacyTexture row = byId.get(textureId);
            if (row == null) {
                continue;
            }
            Long assetId = assetByTextureId.get(textureId);
            if (assetId == null) {
                continue;
            }
            String publisherId = row.selfOwned() ? row.userId() : row.refOwnerId();
            if (publisherId == null || publisherId.isBlank()) {
                continue;
            }
            boolean isPublic = legacy.readPublicVisibility().stream()
                    .anyMatch(v -> v.textureId().equals(textureId) && v.isPublic());
            if (!isPublic) {
                publications.findByPublisherAndAsset(publisherId, assetId).orElseGet(() -> {
                    TexturePublication publication = publications.publish(
                            publisherId, assetId, parseLegacyTime(row.createdAt(), report));
                    publications.withdraw(publication.id(), parseLegacyTime(row.createdAt(), report));
                    report.add("historicalPrivatePublications", 1);
                    return publications.findById(publication.id()).orElse(publication);
                });
            }
        }

        for (LegacyTextureReader.LegacyVisibility visibility : legacy.readPublicVisibility()) {
            LegacyTexture row = byId.get(visibility.textureId());
            if (row == null || !row.selfOwned() || !assetByTextureId.containsKey(row.id())) {
                report.add("skippedVisibility", 1);
                report.warn("visibility without migratable texture: textureId=" + visibility.textureId());
                continue;
            }
            long assetId = assetByTextureId.get(row.id());
            Optional<TexturePublication> existing = publications.findByPublisherAndAsset(row.userId(), assetId);
            if (existing.isPresent() && existing.get().available()) {
                continue;
            }
            Instant when = parseLegacyTime(visibility.createdAt(), report);
            TexturePublication publication = publications.publish(row.userId(), assetId, when);
            mappings.record("texture-publication", row.id(), Long.toString(publication.id()), row.userId());
            report.add(existing.isEmpty() ? "publicationsCreated" : "publicationsRestored", 1);
        }

        for (LegacyTextureReader.LegacyInteraction like : legacy.readLikes()) {
            Optional<TexturePublication> publication = publicationOf(like.textureId(), byId, assetByTextureId);
            if (publication.isEmpty()) {
                report.add("skippedLikes", 1);
                report.warn("like target has no available publication: textureId=" + like.textureId());
                continue;
            }
            if (!interactions.hasLike(like.userId(), publication.get().id())) {
                interactions.like(like.userId(), publication.get().id(), parseLegacyTime(like.createdAt(), report));
                report.add("likesMigrated", 1);
            }
        }

        for (LegacyTextureReader.LegacyInteraction favorite : legacy.readFavorites()) {
            Optional<TexturePublication> publication = publicationOf(favorite.textureId(), byId, assetByTextureId);
            if (publication.isEmpty()) {
                report.add("skippedFavorites", 1);
                report.warn("favorite target has no available publication: textureId=" + favorite.textureId());
                continue;
            }
            if (!interactions.hasFavorite(favorite.userId(), publication.get().id())) {
                interactions.favorite(favorite.userId(), publication.get().id(),
                        parseLegacyTime(favorite.createdAt(), report));
                report.add("favoritesMigrated", 1);
            }
        }

        for (LegacyTextureReader.LegacyFavoriteAlias favoriteAlias : legacy.readFavoriteAliases()) {
            Long assetId = assetByTextureId.get(favoriteAlias.textureId());
            if (assetId == null) {
                report.add("skippedFavoriteAliases", 1);
                continue;
            }
            LibraryEntry entry = library.ensure(favoriteAlias.userId(), assetId, Instant.now());
            if (entry.alias() == null || entry.alias().isBlank()) {
                library.updateAlias(entry.id(), favoriteAlias.alias());
                report.add("favoriteAliasesMigrated", 1);
            }
        }

        for (LegacyTextureReader.LegacyFriendShare share : legacy.readFriendShares()) {
            LegacyTexture row = byId.get(share.textureId());
            if (row == null || !assetByTextureId.containsKey(row.id())) {
                report.add("skippedShares", 1);
                report.warn("friend share target missing: textureId=" + share.textureId());
                continue;
            }
            if (share.ownerId().equals(share.friendId())) {
                report.add("skippedShares", 1);
                report.warn("self friend share skipped: shareId=" + share.id());
                continue;
            }
            long assetId = assetByTextureId.get(row.id());
            Optional<TextureUpload> ownerUpload = uploads.findByUserAndAsset(share.ownerId(), assetId);
            if (ownerUpload.isEmpty() || !ownerUpload.get().active()) {
                report.add("skippedShares", 1);
                report.warn("friend share owner has no active upload: shareId=" + share.id());
                continue;
            }
            if (grants.findActive(share.ownerId(), share.friendId(), assetId).isPresent()) {
                continue;
            }
            TextureShareGrant grant = grants.create(ownerUpload.get().id(), share.ownerId(), share.friendId(),
                    assetId, parseLegacyTime(share.createdAt(), report));
            mappings.record("friend-share", share.id(), Long.toString(grant.id()), share.textureId());
            report.add("grantsMigrated", 1);
        }

        report.add("mappingTextureAssets", mappings.count("texture-asset"));
        report.add("mappingPublications", mappings.count("texture-publication"));
        report.add("mappingFriendShares", mappings.count("friend-share"));
        return report;
    }

    private long ensureAsset(LegacyTexture row, MigrationReport report, boolean selfOwned) {
        TextureType type = TextureType.parse(row.type());
        Path source = locateLegacyFile(type, row.hash());
        if (source == null) {
            report.add("skippedMissingFiles", 1);
            report.warn("legacy texture file not found: id=" + row.id() + " hash=" + row.hash()
                    + " type=" + row.type());
            throw new IllegalStateException("missing legacy texture file: " + row.hash());
        }
        long fileSize;
        int width;
        int height;
        try {
            fileSize = Files.size(source);
            int[] dims = readPngDimensions(Files.readAllBytes(source));
            width = dims[0];
            height = dims[1];
        } catch (Exception error) {
            report.add("failedFiles", 1);
            throw new IllegalStateException("cannot read legacy texture file: " + source, error);
        }
        String storageKey = source.toAbsolutePath().normalize().toString();
        String adminName = legacyAdminName(row.type(), row.hash());
        if (adminName == null || adminName.isBlank()) {
            adminName = row.originalName();
        }

        TextureBlob blob = blobs.findBySha256(row.hash()).orElse(null);
        if (blob == null) {
            long createdId = blobs.insert(new TextureBlob(0L, row.hash(), fileSize,
                    "image/png", storageKey, BlobState.READY,
                    parseLegacyTime(row.createdAt(), report)));
            report.add("blobsCreated", 1);
            blob = blobs.findById(createdId).orElseThrow();
        } else {
            repairBlobStorage(blob, storageKey, fileSize, report);
            blob = blobs.findById(blob.id()).orElseThrow();
        }

        long assetId;
        Optional<TextureAsset> existingAsset = assets.findByTypeAndBlob(type, blob.id());
        if (existingAsset.isPresent()) {
            assetId = existingAsset.get().id();
            if (adminName != null && !adminName.isBlank()) {
                assets.updateAdminFileName(assetId, adminName);
            }
        } else {
            assetId = assets.createReady(blob.id(), type, width, height,
                    adminName, null, parseLegacyTime(row.createdAt(), report));
            report.add("assetsCreated", 1);
        }

        if (selfOwned) {
            Optional<TextureUpload> existingUpload = uploads.findByUserAndAsset(row.userId(), assetId);
            uploads.grant(row.userId(), assetId, row.originalName(),
                    parseLegacyTime(row.createdAt(), report));
            if (existingUpload.isEmpty()) {
                report.add("uploadsCreated", 1);
            }
        }

        LibraryEntry entry = library.ensure(row.userId(), assetId, parseLegacyTime(row.createdAt(), report));
        if (row.alias() != null && !row.alias().isBlank() && entry.alias() == null) {
            library.updateAlias(entry.id(), row.alias());
            report.add("aliasesMigrated", 1);
        }
        return assetId;
    }

    private Optional<TexturePublication> publicationOf(String textureId,
                                                       Map<String, LegacyTexture> byId,
                                                       Map<String, Long> assetByTextureId) {
        LegacyTexture row = byId.get(textureId);
        if (row == null) {
            return Optional.empty();
        }
        Long assetId = assetByTextureId.get(textureId);
        if (assetId == null) {
            return Optional.empty();
        }
        String publisherId = row.selfOwned() ? row.userId() : row.refOwnerId();
        if (publisherId == null || publisherId.isBlank()) {
            return Optional.empty();
        }
        return publications.findByPublisherAndAsset(publisherId, assetId);
    }

    
    private Path locateLegacyFile(TextureType type, String hash) {
        String settingKey = type == TextureType.CAPE ? "cape_storage_path" : "skin_storage_path";
        String configured = Jdbc.queryOne(database, rs -> rs.getString("setting_value"),
                "SELECT setting_value FROM system_settings WHERE setting_key = ?", settingKey);
        String fallback = type == TextureType.CAPE ? "capes" : "skins";
        String root = configured == null || configured.isBlank() ? fallback : configured;
        Path base = Path.of(root);
        if (!base.isAbsolute()) {
            base = Path.of(System.getProperty("user.dir")).resolve(base);
        }
        for (Path candidate : new Path[]{base.resolve(hash), base.resolve(hash + ".png"),
                Path.of(System.getProperty("user.dir")).resolve(fallback).resolve(hash),
                Path.of(System.getProperty("user.dir")).resolve(fallback).resolve(hash + ".png")}) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String legacyAdminName(String type, String hash) {
        String name = Jdbc.queryOne(database, rs -> rs.getString("admin_file_name"),
                "SELECT admin_file_name FROM texture_file_meta WHERE type = ? AND hash = ?",
                type, hash);
        if (name == null || name.isBlank()) {
            name = Jdbc.queryOne(database, rs -> rs.getString("admin_alias"),
                    "SELECT admin_alias FROM texture_meta WHERE hash = ?", hash);
        }
        return name;
    }

    private void repairBlobStorage(TextureBlob blob, String storageKey, long size,
                                   MigrationReport report) {
        boolean currentExists = blob.storageKey() != null
                && Files.isRegularFile(Path.of(blob.storageKey()));
        if (!currentExists) {
            im.xz.cn.infrastructure.persistence.Jdbc.update(database,
                    "UPDATE texture_blobs SET storage_key = ?, size_bytes = ? WHERE id = ?",
                    storageKey, size, blob.id());
            report.add("blobStorageRepaired", 1);
        }
    }

    private static int[] readPngDimensions(byte[] data) {
        if (data.length >= 24 && data[12] == 'I' && data[13] == 'H' && data[14] == 'D'
                && data[15] == 'R') {
            int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16)
                    | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
            int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16)
                    | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
            if (width > 0 && height > 0) {
                return new int[]{width, height};
            }
        }
        throw new IllegalArgumentException("invalid PNG header");
    }

    private static Instant parseLegacyTime(String value, MigrationReport report) {
        if (value == null || value.isBlank()) {
            report.warn("missing legacy timestamp; using current time");
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            
            try {
                report.add("timestampsAssumedUtc", 1);
                return LocalDateTime.parse(value, LEGACY_TIME).toInstant(ZoneOffset.UTC);
            } catch (RuntimeException error) {
                report.warn("unparseable legacy timestamp: " + value);
                return Instant.now();
            }
        }
    }
}
