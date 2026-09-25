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
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.ProfileDao;
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

public class UserCapeHandler {
    private static final logApi log = logApi.getLogger(UserCapeHandler.class);
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;
    private final TextureVisibilityDao visibilityDao;
    private final SystemConfig systemConfig;
    private final ProfileDao profileDao;

    public UserCapeHandler(TextureDao textureDao, TextureService textureService, UserDao userDao, TextureVisibilityDao visibilityDao, SystemConfig systemConfig, ProfileDao profileDao) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
        this.visibilityDao = visibilityDao;
        this.systemConfig = systemConfig;
        this.profileDao = profileDao;
    }

    public void capesPage(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(UserPage.renderCapesPage(csrfToken));
    }

    public void getCapes(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        List<Texture> capes = textureDao.findSelfUploaded(user.getId(), "CAPE");
        Map<String, Boolean> visibility = visibilityDao.batchGetVisibility(
                user.getId(), capes.stream().map(Texture::getId).toList());
        ctx.json(Map.of("success", true, "capes", capes.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("alias", t.getAlias());
            map.put("originalName", t.getOriginalName());
            map.put("hash", t.getHash());
            map.put("size", t.getSize());
            map.put("createdAt", t.getCreatedAt());
            map.put("isPublic", visibility.getOrDefault(t.getId(), false));
            return map;
        }).toList()));
    }

    public void uploadCape(Context ctx) {
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

        long maxBytes = textureService.getMaxUploadBytes("CAPE");
        int maxSize = (int) (maxBytes / 1024L);
        if (file.size() > maxBytes) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.fileTooLargeParam", maxSize)));
            return;
        }

        TextureService.PngUploadResult uploadResult;
        try (var in = file.content()) {
            uploadResult = textureService.readAndNormalizePng(in, "CAPE");
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.readFailed")));
            return;
        }

        try (uploadResult) {
            processCapeUpload(ctx, user, file, uploadResult, maxSize);
        }
    }

    private void processCapeUpload(Context ctx, User user, UploadedFile file,
                                  TextureService.PngUploadResult uploadResult, int maxSize) {
        if (uploadResult.status() == TextureService.PngUploadStatus.TOO_LARGE) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.fileTooLargeParam", maxSize)));
            return;
        }
        if (uploadResult.status() == TextureService.PngUploadStatus.BUSY) {
            ctx.status(429).json(Map.of("success", false, "message", I18n.t("msg.uploadRateLimited")));
            return;
        }
        String sourceHash = uploadResult.sourceHash();
        Texture sameUserExisting = sourceHash == null ? null
                : textureDao.findByUserAndHash(user.getId(), "CAPE", sourceHash);
        if (sameUserExisting != null) {
            ctx.json(Map.of("success", false,
                    "message", I18n.t("msg.capeExists", existingTextureName(sameUserExisting))));
            return;
        }
        if (uploadResult.status() != TextureService.PngUploadStatus.VALID) {
            if (Boolean.TRUE.equals(ctx.attribute("uploadRateLimited"))) {
                ctx.status(429).json(Map.of("success", false, "message", I18n.t("msg.tooFrequent")));
                return;
            }
            ctx.json(Map.of("success", false, "message", I18n.t("msg.fileNotPng")));
            return;
        }
        byte[] data = uploadResult.data();
        long size = data.length;
        String hash = textureService.computeHash(data);
        if (!hash.equals(sourceHash)) {
            sameUserExisting = textureDao.findByUserAndHash(user.getId(), "CAPE", hash);
        } else {
            sameUserExisting = null;
        }
        if (sameUserExisting != null) {
            ctx.json(Map.of("success", false,
                    "message", I18n.t("msg.capeExists", existingTextureName(sameUserExisting))));
            return;
        }
        if (Boolean.TRUE.equals(ctx.attribute("uploadRateLimited"))) {
            ctx.status(429).json(Map.of("success", false, "message", I18n.t("msg.tooFrequent")));
            return;
        }

        int currentCount = textureDao.countByUserId(user.getId(), "CAPE");
        if (!textureService.checkCountLimit(user.getId(), "CAPE", currentCount, 0)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.capeCountLimit")));
            return;
        }

        long currentSize = textureDao.sumSizeByUserId(user.getId(), "CAPE");
        if (!textureService.checkTotalSizeLimit(user.getId(), "CAPE", currentSize, size, 0)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.capeTotalSizeLimit")));
            return;
        }

        String explicitAlias = ctx.formParam("alias");
        if (explicitAlias != null && !explicitAlias.isBlank()
                && systemConfig.isCapeNameBlacklisted(explicitAlias.trim())) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.nameTaken")));
            return;
        }

        try {
            Texture globalExisting = textureDao.findByHash("CAPE", hash);
            if (!textureService.tryRecordUpload(user.getId(), "CAPE")) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.uploadRateLimited")));
                return;
            }
            if (globalExisting == null) {
                textureService.saveFile("CAPE", hash, data);
            }

            String sanitizedOriginal = systemConfig.sanitizeTextureFileName(file.filename(), false);
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
            Texture texture = new Texture(id, user.getId(), "CAPE", hash, alias, sanitizedOriginal, size, "image/png", createdAt);
            textureDao.insert(texture);

            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "texture");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.uploadSuccess")));
        } catch (Exception e) {
            if (DatabaseManager.isDuplicateKeyViolation(e)) {
                Texture duplicate = textureDao.findByUserAndHash(user.getId(), "CAPE", hash);
                if (duplicate != null) {
                    ctx.json(Map.of("success", false,
                            "message", I18n.t("msg.capeExists", existingTextureName(duplicate))));
                    return;
                }
            }
            log.error("[UserCapeHandler] upload failed: {}", e.getMessage(), e);
            ctx.json(Map.of("success", false, "message", I18n.t("msg.uploadFailed")));
        }
    }

    private static String existingTextureName(Texture texture) {
        if (texture.getAlias() != null && !texture.getAlias().isBlank()) return texture.getAlias();
        if (texture.getOriginalName() != null && !texture.getOriginalName().isBlank()) return texture.getOriginalName();
        return texture.getHash() == null ? "" : texture.getHash();
    }

    public void deleteCape(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        try {
            Map<String, Object> body = new ObjectMapper().readValue(
                    ctx.body(),
                    new TypeReference< >() {}
            );
            String id = (String) body.get("id");
            if (id == null || id.isBlank()) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingCapeId")));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId()) || !isSelfReference(texture)) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            textureDao.delete(id);
            revokeSharedRefs("CAPE", texture.getHash(), user.getId());
            im.xz.cn.logging.UserActionLogger.log(user.getId(), im.xz.cn.common.IpUtil.getClientIp(ctx), "texture");
            ctx.json(Map.of("success", true, "message", I18n.t("msg.textureDeleted")));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.requestError")));
        }
    }

    private static boolean isSelfReference(Texture texture) {
        String referenceType = texture.getReferenceType();
        return referenceType == null || referenceType.isBlank() || "self".equalsIgnoreCase(referenceType);
    }

    private void revokeSharedRefs(String type, String hash, String ownerId) {
        for (Texture ref : textureDao.findRefsByOwner(type, hash, ownerId)) {
            profileDao.clearTextureRefByHash(ref.getUserId(), type, hash);
        }
        textureDao.deleteRefsByOwner(type, hash, ownerId);
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
                ctx.json(Map.of("success", false, "message", I18n.t("msg.missingCapeId")));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", I18n.t("msg.textureNotFound")));
                return;
            }
            if (alias != null && systemConfig.isCapeNameBlacklisted(alias.trim())) {
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

    public void downloadCape(Context ctx) {
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
        byte[] data = textureService.readFile("CAPE", texture.getHash());
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
