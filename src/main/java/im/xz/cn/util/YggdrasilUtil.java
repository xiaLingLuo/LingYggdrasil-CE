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
package im.xz.cn.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import im.xz.cn.model.PlayerProfile;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilUtil {
    private static final Logger logger = LoggerFactory.getLogger(YggdrasilUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String buildTexturesValue(PlayerProfile profile) {
        return buildTexturesValue(profile, true);
    }

    public static String buildTexturesValue(PlayerProfile profile, boolean signatureRequired) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", System.currentTimeMillis());
            root.put("profileId", UuidUtil.toHex(profile.getId()));
            root.put("profileName", profile.getName());
            root.put("isPublic", true);

            ObjectNode textures = mapper.createObjectNode();
            boolean hasSkin = profile.getSkinUrl() != null && !profile.getSkinUrl().isEmpty();
            boolean hasCape = profile.getCapeUrl() != null && !profile.getCapeUrl().isEmpty();

            if (hasSkin) {
                ObjectNode skin = mapper.createObjectNode();
                skin.put("url", profile.getSkinUrl());
                if ("slim".equals(profile.getSkinModel())) {
                    ObjectNode metadata = mapper.createObjectNode();
                    metadata.put("model", "slim");
                    skin.set("metadata", metadata);
                }
                textures.set("SKIN", skin);
            }

            if (hasCape) {
                ObjectNode cape = mapper.createObjectNode();
                cape.put("url", profile.getCapeUrl());
                textures.set("CAPE", cape);
            }

            root.set("textures", textures);

            if (signatureRequired) {
                root.put("signatureRequired", true);
            }

            String json = mapper.writeValueAsString(root);
            logger.debug("[YggdrasilUtil] buildTexturesValue: name={}, hasSkin={}, hasCape={}, signatureRequired={}, skinUrl={}",
                    profile.getName(), hasSkin, hasCape, signatureRequired, profile.getSkinUrl());
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
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UuidUtil.toHex(profile.getId()));
        response.put("name", profile.getName());

        String texturesValue = buildTexturesValue(profile, !unsigned);

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
