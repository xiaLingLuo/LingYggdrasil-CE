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
package im.xz.cn.profile;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlobRepository;


public final class ProfileTextureResolver {

    private final ProfileTextureBindingRepository bindings;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;

    public ProfileTextureResolver(DatabaseManager database) {
        this.bindings = new ProfileTextureBindingRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.blobs = new TextureBlobRepository(database);
    }

    
    public String hashFor(PlayerProfile profile, String slot) {
        if (profile == null) {
            return null;
        }
        String normalized = "CAPE".equalsIgnoreCase(slot) ? "CAPE" : "SKIN";
        var binding = bindings.find(profile.getId(), normalized);
        if (binding.isPresent()) {
            String hash = assets.findById(binding.get().assetId())
                    .filter(asset -> asset.state() == im.xz.cn.texture.asset.TextureAsset.AssetState.READY)
                    .flatMap(asset -> blobs.findById(asset.blobId()))
                    .filter(blob -> blob.state() == im.xz.cn.texture.asset.BlobState.READY)
                    .map(blob -> blob.sha256())
                    .orElse(null);
            
            return hash == null || hash.isBlank() ? null : hash;
        }
        return "CAPE".equals(normalized) ? profile.getCapeHash() : profile.getSkinHash();
    }

    
    public boolean hasBinding(String profileId, String slot) {
        String normalized = "CAPE".equalsIgnoreCase(slot) ? "CAPE" : "SKIN";
        return bindings.find(profileId, normalized).isPresent();
    }
}
