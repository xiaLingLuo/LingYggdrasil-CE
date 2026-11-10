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
package im.xz.cn.yggdrasil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.config.ServerConfig;
import im.xz.cn.security.YggdrasilKeyManager;

import java.nio.charset.StandardCharsets;
import java.util.*;

import im.xz.cn.logging.logApi;

public class YggdrasilUtil {
    private static final logApi logger = logApi.getLogger(YggdrasilUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String buildTexturesValue(PlayerProfile profile) {
        return buildTexturesValue(profile, true);
    }

    public static String buildTexturesValue(PlayerProfile profile, boolean signatureRequired) {
        return buildTexturesValue(profile, signatureRequired, null);
    }

    public static String buildTexturesValue(PlayerProfile profile, boolean signatureRequired,
                                            im.xz.cn.profile.ProfileTextureResolver resolver) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", System.currentTimeMillis());
            root.put("profileId", UuidUtil.toHex(profile.getId()));
            root.put("profileName", profile.getName());
            root.put("isPublic", true);

            ObjectNode textures = mapper.createObjectNode();
            String skinHash = resolver == null ? profile.getSkinHash() : resolver.hashFor(profile, "SKIN");
            String capeHash = resolver == null ? profile.getCapeHash() : resolver.hashFor(profile, "CAPE");
            String skinUrl = ServerConfig.getInstance().getTextureUrl("SKIN", skinHash);
            String capeUrl = ServerConfig.getInstance().getTextureUrl("CAPE", capeHash);
            boolean hasSkin = skinUrl != null;
            boolean hasCape = capeUrl != null;

            if (hasSkin) {
                ObjectNode skin = mapper.createObjectNode();
                skin.put("url", skinUrl);
                if ("slim".equals(profile.getSkinModel())) {
                    ObjectNode metadata = mapper.createObjectNode();
                    metadata.put("model", "slim");
                    skin.set("metadata", metadata);
                }
                textures.set("SKIN", skin);
            }

            if (hasCape) {
                ObjectNode cape = mapper.createObjectNode();
                cape.put("url", capeUrl);
                textures.set("CAPE", cape);
            }

            root.set("textures", textures);

            if (signatureRequired) {
                root.put("signatureRequired", true);
            }

            String json = mapper.writeValueAsString(root);
            logger.debug("[YggdrasilUtil] buildTexturesValue: name={}, hasSkin={}, hasCape={}, signatureRequired={}, skinHash={}",
                    profile.getName(), hasSkin, hasCape, signatureRequired, profile.getSkinHash());
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("[YggdrasilUtil] Failed to build textures value: {}", e.getMessage(), e);
            return "";
        }
    }

    public static Map<String, Object> buildProfileResponse(PlayerProfile profile) {
        return buildProfileResponse(profile, true);
    }

    public static Map<String, Object> buildProfileResponse(PlayerProfile profile, boolean unsigned) {
        return buildProfileResponse(profile, unsigned, null);
    }

    public static Map<String, Object> buildProfileResponse(PlayerProfile profile, boolean unsigned,
                                                           im.xz.cn.profile.ProfileTextureResolver resolver) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UuidUtil.toHex(profile.getId()));
        response.put("name", profile.getName());

        String texturesValue = buildTexturesValue(profile, !unsigned, resolver);

        List<Map<String, String>> properties = new ArrayList<>();
        Map<String, String> texturesProp = new LinkedHashMap<>();
        texturesProp.put("name", "textures");
        texturesProp.put("value", texturesValue);

        YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
        if (!unsigned) {
            if (km.isLoaded()) {
                String signature = km.sign(texturesValue);
                if (signature != null && !signature.isEmpty()) {
                    texturesProp.put("signature", signature);
                    logger.debug("[YggdrasilUtil] buildProfileResponse: 已签名, 模式={}", km.getMode());
                } else {
                    logger.warn("[YggdrasilUtil] buildProfileResponse: 签名失败!");
                }
            } else {
                logger.warn("[YggdrasilUtil] buildProfileResponse: unsigned=false 但密钥未加载!");
            }
        } else {
            logger.debug("[YggdrasilUtil] buildProfileResponse: unsigned=true, 跳过签名");
        }

        properties.add(texturesProp);

        Map<String, String> uploadableProp = new LinkedHashMap<>();
        uploadableProp.put("name", "uploadableTextures");
        uploadableProp.put("value", "skin,cape");
        properties.add(uploadableProp);

        response.put("properties", properties);
        return response;
    }

    public static Map<String, Object> buildErrorResponse(String error, String errorMessage, String cause) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", error);
        response.put("errorMessage", errorMessage);
        if (cause != null && !cause.isEmpty()) {
            response.put("cause", cause);
        }
        return response;
    }
}
