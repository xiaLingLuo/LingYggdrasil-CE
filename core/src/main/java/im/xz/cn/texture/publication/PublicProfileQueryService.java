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
package im.xz.cn.texture.publication;

import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.interaction.InteractionRepository;
import im.xz.cn.texture.library.LibraryEntry;
import im.xz.cn.texture.library.LibraryEntryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class PublicProfileQueryService {

    private final PublicationRepository publications;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;
    private final LibraryEntryRepository library;
    private final InteractionRepository interactions;

    public PublicProfileQueryService(PublicationRepository publications,
                                     TextureAssetRepository assets,
                                     TextureBlobRepository blobs,
                                     LibraryEntryRepository library,
                                     InteractionRepository interactions) {
        this.publications = publications;
        this.assets = assets;
        this.blobs = blobs;
        this.library = library;
        this.interactions = interactions;
    }

    public List<PublicTextureSummary> listPublic(String publisherId, int limit, int offset) {
        if (publisherId == null || publisherId.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        List<PublicTextureSummary> result = new ArrayList<>();
        for (TexturePublication publication : publications.findAvailableByPublisher(
                publisherId, safeLimit, safeOffset)) {
            summarize(publication).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    
    public Optional<PublicTextureSummary> findPublic(String publisherId, String hash, TextureType type) {
        if (publisherId == null || publisherId.isBlank() || hash == null || hash.isBlank()) {
            return Optional.empty();
        }
        Optional<TextureBlob> blob = blobs.findBySha256(hash.trim().toLowerCase(java.util.Locale.ROOT));
        if (blob.isEmpty()) {
            return Optional.empty();
        }
        TextureType[] candidates = type == null ? TextureType.values() : new TextureType[]{type};
        for (TextureType candidate : candidates) {
            Optional<TextureAsset> asset = assets.findByTypeAndBlob(candidate, blob.get().id());
            if (asset.isEmpty()) {
                continue;
            }
            Optional<TexturePublication> publication =
                    publications.findByPublisherAndAsset(publisherId, asset.get().id());
            if (publication.isPresent() && publication.get().available()) {
                Optional<PublicTextureSummary> summary = summarize(publication.get());
                if (summary.isPresent()) {
                    return summary;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<PublicTextureSummary> summarize(TexturePublication publication) {
        Optional<TextureAsset> asset = assets.findById(publication.assetId());
        if (asset.isEmpty() || asset.get().state() != TextureAsset.AssetState.READY) {
            return Optional.empty();
        }
        Optional<TextureBlob> blob = blobs.findById(asset.get().blobId());
        if (blob.isEmpty()) {
            return Optional.empty();
        }
        String displayName = resolveDisplayName(publication.publisherId(), asset.get(), blob.get());
        long likes = interactions.countLikes(publication.id());
        long favorites = interactions.countFavorites(publication.id());
        return Optional.of(new PublicTextureSummary(
                publication.id(),
                asset.get().id(),
                asset.get().type(),
                blob.get().sha256(),
                displayName,
                asset.get().authorDisplayName(),
                likes,
                favorites,
                publication.firstPublishedAt(),
                publication.lastPublishedAt()));
    }

    
    private String resolveDisplayName(String publisherId, TextureAsset asset, TextureBlob blob) {
        Optional<LibraryEntry> entry = library.findByUserAndAsset(publisherId, asset.id());
        if (entry.isPresent() && entry.get().alias() != null && !entry.get().alias().isBlank()) {
            return entry.get().alias();
        }
        if (asset.adminFileName() != null && !asset.adminFileName().isBlank()) {
            return asset.adminFileName();
        }
        return blob.sha256().substring(0, 12);
    }
}
