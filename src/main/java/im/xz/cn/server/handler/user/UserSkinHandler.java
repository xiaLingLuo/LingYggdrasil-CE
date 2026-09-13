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


import im.xz.cn.i18n.I18n;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.dao.TextureDao;
import im.xz.cn.database.dao.TextureVisibilityDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.texture.TextureService;
import im.xz.cn.logging.logApi;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.web.view.UserPage;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserSkinHandler {
    private static final logApi log = logApi.getLogger(UserSkinHandler.class);
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;
    private final TextureVisibilityDao visibilityDao;
    private final SystemConfig systemConfig;

    public UserSkinHandler(TextureDao textureDao, TextureService textureService, UserDao userDao, TextureVisibilityDao visibilityDao, SystemConfig systemConfig) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
        this.visibilityDao = visibilityDao;
        this.systemConfig = systemConfig;
    }

    public void skinsPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderSkinsPage(csrfToken));
    }

    public void getSkins(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<Texture> skins = textureDao.findSelfUploaded(user.getId(), "SKIN");
        ctx.json(Map.of("success", true, "skins", skins.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("alias", t.getAlias());
            map.put("originalName", t.getOriginalName());
            map.put("hash", t.getHash());
            map.put("size", t.getSize());
            map.put("createdAt", t.getCreatedAt());
            map.put("isPublic", visibilityDao.isPublic(user.getId(), t.getId()));
            return map;
        }).toList()));
    }

    public void uploadSkin(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.pleaseSelectFile")));
            return;
        }
        if (!"image/png".equals(file.contentType())) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.onlyPng")));
            return;
        }
        if (!TextureService.hasPngExtension(file.filename())) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.onlyPngExt")));
            return;
        }

        byte[] data;
        try (var in = file.content()) {
            data = in.readAllBytes();
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.readFailed")));
            return;
        }

        if (!TextureService.isPng(data)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.fileNotPng")));
            return;
        }

        long size = data.length;
        int maxSize = textureService.getMaxSize("SKIN");
        if (size > maxSize * 1024L) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.fileTooLargeParam", maxSize)));
            return;
        }

        if (!textureService.checkRateLimit(user.getId(), "SKIN")) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.uploadRateLimited")));
            return;
        }

        int currentCount = textureDao.countByUserId(user.getId(), "SKIN");
        if (!textureService.checkCountLimit(user.getId(), "SKIN", currentCount, 0)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.skinCountLimit")));
            return;
        }

        long currentSize = textureDao.sumSizeByUserId(user.getId(), "SKIN");
        if (!textureService.checkTotalSizeLimit(user.getId(), "SKIN", currentSize, size, 0)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.skinTotalSizeLimit")));
            return;
        }

        String explicitAlias = ctx.formParam("alias");
        if (explicitAlias != null && !explicitAlias.isBlank()
                && systemConfig.isSkinNameBlacklisted(explicitAlias.trim())) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.nameTaken")));
            return;
        }

        String hash = textureService.computeHash(data);

        Texture sameUserExisting = textureDao.findByUserAndHash(user.getId(), "SKIN", hash);
        if (sameUserExisting != null) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.skinExists")));
            return;
        }

        try {
            Texture globalExisting = textureDao.findByHash("SKIN", hash);
            if (globalExisting == null) {
                textureService.saveFile("SKIN", hash, data);
            }

            textureService.recordUpload(user.getId(), "SKIN");

            String sanitizedOriginal = systemConfig.sanitizeTextureFileName(file.filename(), true);
            String alias = explicitAlias;
            if (alias == null || alias.isBlank()) {
                if (sanitizedOriginal.contains(".")) {
                    alias = sanitizedOriginal.substring(0, sanitizedOriginal.lastIndexOf('.'));
                } else {
                    alias = sanitizedOriginal;
                }
            }
            if (alias != null && alias.length() > 100) {
                alias = alias.substring(0, 100);
            }

            String id = UUID.randomUUID().toString();
            String createdAt = TimeUtil.now();
            Texture texture = new Texture(id, user.getId(), "SKIN", hash, alias, sanitizedOriginal, size, "image/png", createdAt);
            textureDao.insert(texture);

            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "texture");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.uploadSuccess")));
        } catch (Exception e) {
            log.error("[UserSkinHandler] upload failed: {}", e.getMessage(), e);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.uploadFailed")));
        }
    }

    public void deleteSkin(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String id = (String) body.get("id");
            if (id == null || id.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingSkinId")));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            textureDao.delete(id);
            if (textureDao.countByHash("SKIN", texture.getHash()) == 0) {
                textureService.deleteFile("SKIN", texture.getHash());
            }
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "texture");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.textureDeleted")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void updateAlias(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(
                    ctx.body(),
                    new TypeReference<>() {}
            );
            String id = (String) body.get("id");
            String alias = (String) body.get("alias");
            if (id == null || id.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingSkinId")));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            if (alias != null && systemConfig.isSkinNameBlacklisted(alias.trim())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.nameTaken")));
                return;
            }
            textureDao.updateAlias(id, alias);
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "texture");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.aliasUpdated")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    public void downloadSkin(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String id = ctx.queryParam("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        Texture texture = textureDao.findById(id);
        if (texture == null || !texture.getUserId().equals(user.getId())) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
            return;
        }
        byte[] data = textureService.readFile("SKIN", texture.getHash());
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.fileMissing")));
            return;
        }
        ctx.contentType("image/png");
        ctx.result(data);
    }

    private User checkAuth(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.redirect("/login");
            return null;
        }
        User user = userDao.findById(userId);
        if (user == null || !im.xz.cn.security.UserPermissions.isAccessible()) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }

}
