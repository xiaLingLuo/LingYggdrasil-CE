package im.xz.cn.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import im.xz.cn.model.PlayerProfile;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class YggdrasilUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String buildTexturesValue(PlayerProfile profile) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("timestamp", System.currentTimeMillis());
            root.put("profileId", UuidUtil.toHex(profile.getId()));
            root.put("profileName", profile.getName());

            ObjectNode textures = mapper.createObjectNode();

            if (profile.getSkinUrl() != null && !profile.getSkinUrl().isEmpty()) {
                ObjectNode skin = mapper.createObjectNode();
                skin.put("url", profile.getSkinUrl());
                if ("slim".equals(profile.getSkinModel())) {
                    ObjectNode metadata = mapper.createObjectNode();
                    metadata.put("model", "slim");
                    skin.set("metadata", metadata);
                }
                textures.set("SKIN", skin);
            }

            if (profile.getCapeUrl() != null && !profile.getCapeUrl().isEmpty()) {
                ObjectNode cape = mapper.createObjectNode();
                cape.put("url", profile.getCapeUrl());
                textures.set("CAPE", cape);
            }

            root.set("textures", textures);

            String json = mapper.writeValueAsString(root);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Failed to build textures value: " + e.getMessage());
            return "";
        }
    }

    public static Map<String, Object> buildProfileResponse(PlayerProfile profile) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UuidUtil.toHex(profile.getId()));
        response.put("name", profile.getName());

        String texturesValue = buildTexturesValue(profile);

        List<Map<String, String>> properties = new ArrayList<>();
        Map<String, String> texturesProp = new LinkedHashMap<>();
        texturesProp.put("name", "textures");
        texturesProp.put("value", texturesValue);

        YggdrasilKeyManager km = YggdrasilKeyManager.getInstance();
        if (km.isLoaded()) {
            String signature = km.sign(texturesValue);
            if (signature != null && !signature.isEmpty()) {
                texturesProp.put("signature", signature);
            }
        }

        properties.add(texturesProp);

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
