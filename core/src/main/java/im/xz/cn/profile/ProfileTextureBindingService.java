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
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureType;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.sharing.ShareGrantRepository;

import java.time.Instant;
import java.util.Locale;


public final class ProfileTextureBindingService {

    public enum Qualification {
        UPLOAD, SHARE_GRANT, PUBLIC
    }

    private final ProfileTextureBindingRepository bindings;
    private final TextureAssetRepository assets;
    private final TextureUploadRepository uploads;
    private final ShareGrantRepository grants;
    private final im.xz.cn.texture.publication.PublicationRepository publications;

    public ProfileTextureBindingService(DatabaseManager database) {
        this.bindings = new ProfileTextureBindingRepository(database);
        this.assets = new TextureAssetRepository(database);
        this.uploads = new TextureUploadRepository(database);
        this.grants = new ShareGrantRepository(database);
        this.publications = new im.xz.cn.texture.publication.PublicationRepository(database);
    }

    public Qualification apply(String profileId, String ownerUserId, String slot, long assetId, String skinModel) {
        TextureType type = parseSlot(slot);
        TextureAsset asset = assets.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("asset not found: " + assetId));
        if (asset.type() != type) {
            throw new IllegalArgumentException("asset type does not match slot " + slot);
        }
        if (asset.state() != TextureAsset.AssetState.READY) {
            throw new IllegalArgumentException("texture is not available");
        }
        Qualification qualification;
        if (uploads.hasActiveUpload(ownerUserId, assetId)) {
            qualification = Qualification.UPLOAD;
        } else if (grants.hasActiveForRecipient(ownerUserId, assetId)) {
            qualification = Qualification.SHARE_GRANT;
        } else if (publications.hasAvailableForAsset(assetId)) {
            qualification = Qualification.PUBLIC;
        } else {
            throw new IllegalArgumentException("texture is neither uploaded, shared, nor public");
        }
        String model = normalizeModel(skinModel);
        bindings.upsert(profileId, type.name(), assetId, model, Instant.now());
        return qualification;
    }

    public java.util.Optional<ProfileTextureBindingRepository.Binding> find(String profileId, String slot) {
        return bindings.find(profileId, parseSlot(slot).name());
    }

    public void remove(String profileId, String slot) {
        bindings.remove(profileId, parseSlot(slot).name());
    }

    public void removeAll(String profileId) {
        bindings.removeByProfile(profileId);
    }

    private static TextureType parseSlot(String slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot is required");
        }
        try {
            return TextureType.valueOf(slot.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("invalid slot: " + slot);
        }
    }

    private static String normalizeModel(String skinModel) {
        if (skinModel == null || skinModel.isBlank()) {
            return "DEFAULT";
        }
        String model = skinModel.toUpperCase(Locale.ROOT);
        return "SLIM".equals(model) ? "SLIM" : "DEFAULT";
    }
}
