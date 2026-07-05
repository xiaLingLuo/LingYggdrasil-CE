package im.xz.cn.server.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.TextureDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.util.TextureService;
import im.xz.cn.util.TimeUtil;
import im.xz.cn.something.web.UserPage;
import im.xz.cn.something.web.Shared;
import im.xz.cn.web.PageRenderer;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserSkinHandler {
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;

    public UserSkinHandler(TextureDao textureDao, TextureService textureService, UserDao userDao) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
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
        List<Texture> skins = textureDao.findByUserId(user.getId(), "SKIN");
        ctx.json(Map.of("success", true, "skins", skins.stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("alias", t.getAlias());
            map.put("originalName", t.getOriginalName());
            map.put("hash", t.getHash());
            map.put("size", t.getSize());
            map.put("createdAt", t.getCreatedAt());
            return map;
        }).toList()));
    }

    public void uploadSkin(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;

        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.json(Map.of("success", false, "message", "请选择文件"));
            return;
        }
        if (!"image/png".equals(file.contentType())) {
            ctx.json(Map.of("success", false, "message", "仅支持 PNG 格式"));
            return;
        }
        if (!TextureService.hasPngExtension(file.filename())) {
            ctx.json(Map.of("success", false, "message", "仅支持 .png 扩展名"));
            return;
        }

        try {
            byte[] data = file.content().readAllBytes();
            if (!TextureService.isPng(data)) {
                ctx.json(Map.of("success", false, "message", "文件内容不是有效的 PNG 图片"));
                return;
            }
            long size = data.length;
            int maxSize = textureService.getMaxSize("SKIN");
            if (size > maxSize * 1024L) {
                ctx.json(Map.of("success", false, "message", "文件大小超过限制 (最大 " + maxSize + " KiB)"));
                return;
            }

            if (!textureService.checkRateLimit(user.getId(), "SKIN")) {
                ctx.json(Map.of("success", false, "message", "上传频率超限"));
                return;
            }

            int currentCount = textureDao.countByUserId(user.getId(), "SKIN");
            if (!textureService.checkCountLimit(user.getId(), "SKIN", currentCount, 0)) {
                ctx.json(Map.of("success", false, "message", "皮肤数量已达上限"));
                return;
            }

            long currentSize = textureDao.sumSizeByUserId(user.getId(), "SKIN");
            if (!textureService.checkTotalSizeLimit(user.getId(), "SKIN", currentSize, size, 0)) {
                ctx.json(Map.of("success", false, "message", "皮肤总大小已达上限"));
                return;
            }

            String hash = textureService.computeHash(data);
            Texture existing = textureDao.findByHash("SKIN", hash);
            if (existing != null) {
                ctx.json(Map.of("success", false, "message", "该皮肤已存在"));
                return;
            }

            textureService.saveFile("SKIN", hash, data);
            textureService.recordUpload(user.getId(), "SKIN");

            String alias = ctx.formParam("alias");
            if (alias == null || alias.isBlank()) {
                String originalName = file.filename();
                if (originalName.contains(".")) {
                    alias = originalName.substring(0, originalName.lastIndexOf('.'));
                } else {
                    alias = originalName;
                }
            }

            String id = UUID.randomUUID().toString();
            String createdAt = TimeUtil.now();
            Texture texture = new Texture(id, user.getId(), "SKIN", hash, alias, file.filename(), size, "image/png", createdAt);
            textureDao.insert(texture);

            ctx.json(Map.of("success", true, "message", "上传成功"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
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
                ctx.json(Map.of("success", false, "message", "缺少皮肤ID"));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", "皮肤不存在或无权操作"));
                return;
            }
            textureService.deleteFile("SKIN", texture.getHash());
            textureDao.delete(id);
            ctx.json(Map.of("success", true, "message", "皮肤已删除"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
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
                ctx.json(Map.of("success", false, "message", "缺少皮肤ID"));
                return;
            }
            Texture texture = textureDao.findById(id);
            if (texture == null || !texture.getUserId().equals(user.getId())) {
                ctx.json(Map.of("success", false, "message", "皮肤不存在或无权操作"));
                return;
            }
            textureDao.updateAlias(id, alias);
            ctx.json(Map.of("success", true, "message", "别名已更新"));
        } catch (Exception e) {
            ctx.json(Map.of("success", false, "message", "请求格式错误"));
        }
    }

    public void downloadSkin(Context ctx) {
        User user = checkAuth(ctx);
        if (user == null) return;
        String id = ctx.queryParam("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        Texture texture = textureDao.findById(id);
        if (texture == null || !texture.getUserId().equals(user.getId())) {
            ctx.status(404).json(Map.of("success", false, "message", "皮肤不存在或无权操作"));
            return;
        }
        byte[] data = textureService.readFile("SKIN", texture.getHash());
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", "文件不存在"));
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
        if (user == null) {
            SessionManager.invalidate(ctx);
            ctx.redirect("/login");
            return null;
        }
        return user;
    }

}
