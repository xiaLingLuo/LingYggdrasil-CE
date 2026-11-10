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
package im.xz.cn.server.handler.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.profile.ProfileTextureBindingService;
import im.xz.cn.texture.asset.TextureAsset;
import im.xz.cn.texture.asset.TextureAssetRepository;
import im.xz.cn.texture.asset.TextureBlob;
import im.xz.cn.texture.asset.TextureBlobRepository;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class UserProfileTextureHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProfileDao profiles;
    private final ProfileTextureBindingService bindings;
    private final TextureAssetRepository assets;
    private final TextureBlobRepository blobs;

    public UserProfileTextureHandler(DatabaseManager database) {
        this.profiles = new ProfileDao(database);
        this.bindings = new ProfileTextureBindingService(database);
        this.assets = new TextureAssetRepository(database);
        this.blobs = new TextureBlobRepository(database);
    }

    public void list(Context ctx) {
        PlayerProfile profile = ownedProfile(ctx);
        if (profile == null) {
            return;
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (String slot : new String[]{"SKIN", "CAPE"}) {
            bindings.find(profile.getId(), slot).ifPresent(binding -> items.add(toMap(slot, binding)));
        }
        ctx.json(Map.of("success", true, "items", items));
    }

    public void apply(Context ctx) {
        PlayerProfile profile = ownedProfile(ctx);
        if (profile == null) {
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, parseSlot(ctx.pathParam("slot")))) {
            return;
        }
        Map<String, Object> body;
        try {
            body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "message", "Invalid JSON body"));
            return;
        }
        Object rawAssetId = body.get("assetId");
        if (!(rawAssetId instanceof Number number)) {
            ctx.status(400).json(Map.of("success", false, "message", "assetId is required"));
            return;
        }
        long assetId = number.longValue();
        String slot = ctx.pathParam("slot");
        String skinModel = body.get("skinModel") instanceof String value ? value : null;
        try {
            ProfileTextureBindingService.Qualification qualification =
                    bindings.apply(profile.getId(), profile.getUserId(), slot, assetId, skinModel);
            TextureAsset asset = assets.findById(assetId).orElse(null);
            String hash = asset == null ? null : blobs.findById(asset.blobId())
                    .map(TextureBlob::sha256).orElse(null);
            applyLegacyMirror(profile, slot, hash, skinModel);
            ctx.json(Map.of("success", true,
                    "slot", slot.toUpperCase(java.util.Locale.ROOT),
                    "assetId", assetId,
                    "hash", hash == null ? "" : hash,
                    "qualification", qualification.name()));
        } catch (IllegalArgumentException error) {
            ctx.status(400).json(Map.of("success", false, "message", error.getMessage()));
        }
    }

    public void delete(Context ctx) {
        PlayerProfile profile = ownedProfile(ctx);
        if (profile == null) {
            return;
        }
        if (!UserAccessGuard.requireTexture(ctx, parseSlot(ctx.pathParam("slot")))) {
            return;
        }
        String slot = ctx.pathParam("slot");
        try {
            bindings.remove(profile.getId(), slot);
        } catch (IllegalArgumentException error) {
            ctx.status(400).json(Map.of("success", false, "message", error.getMessage()));
            return;
        }
        applyLegacyMirror(profile, slot, null, null);
        ctx.json(Map.of("success", true, "slot", slot.toUpperCase(java.util.Locale.ROOT)));
    }

    private void applyLegacyMirror(PlayerProfile profile, String slot, String hash, String skinModel) {
        if ("CAPE".equalsIgnoreCase(slot)) {
            profile.setCapeHash(hash);
        } else {
            profile.setSkinHash(hash);
        }
        if (skinModel != null && !skinModel.isBlank()) {
            profile.setSkinModel("slim".equalsIgnoreCase(skinModel) ? "slim" : "default");
        }
        profiles.update(profile);
    }

    private Map<String, Object> toMap(String slot, im.xz.cn.profile.ProfileTextureBindingRepository.Binding binding) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("slot", slot);
        item.put("assetId", binding.assetId());
        item.put("skinModel", binding.skinModel());
        assets.findById(binding.assetId()).flatMap(asset -> blobs.findById(asset.blobId()))
                .ifPresent(blob -> {
                    item.put("hash", blob.sha256());
                    item.put("url", "/api/v2/texture-files/" + slot + "/" + blob.sha256());
                });
        return item;
    }

    private static im.xz.cn.texture.asset.TextureType parseSlot(String slot) {
        return "CAPE".equalsIgnoreCase(slot)
                ? im.xz.cn.texture.asset.TextureType.CAPE
                : im.xz.cn.texture.asset.TextureType.SKIN;
    }

    private PlayerProfile ownedProfile(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED", "message", "Login required"));
            return null;
        }
        String profileId = ctx.pathParam("id");
        PlayerProfile profile = profileId == null ? null : profiles.findById(profileId);
        if (profile == null || !userId.equals(profile.getUserId())) {
            ctx.status(404).json(Map.of("success", false, "message", "Profile not found"));
            return null;
        }
        return profile;
    }
}
