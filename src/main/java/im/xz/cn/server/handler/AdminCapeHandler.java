package im.xz.cn.server.handler;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.TextureDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TextureService;

import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminCapeHandler {
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;
    private final DatabaseManager db;

    public AdminCapeHandler(TextureDao textureDao, TextureService textureService, UserDao userDao, DatabaseManager db) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
        this.db = db;
    }

    public void capesPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderCapesPage(adminUsername, adminRole, csrfToken));
    }

    public void getCapes(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        List<Texture> capes = textureDao.findAll("CAPE");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Texture t : capes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("userId", t.getUserId());
            map.put("alias", t.getAlias());
            map.put("hash", t.getHash());
            map.put("size", t.getSize());
            map.put("createdAt", t.getCreatedAt());
            User user = userDao.findById(t.getUserId());
            map.put("username", user != null ? user.getUsername() : "");
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    public void uploadCape(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        ctx.json(Map.of("success", false, "message", "管理员暂不支持直接上传披风"));
    }

    @SuppressWarnings("unchecked")
    public void deleteCape(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        Texture texture = textureDao.findById(id);
        if (texture == null) {
            ctx.status(404).json(Map.of("success", false, "message", "披风不存在"));
            return;
        }
        textureService.deleteFile("CAPE", texture.getHash());
        textureDao.delete(id);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_CAPE:" + id, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "披风已删除"));
    }

    @SuppressWarnings("unchecked")
    public void updateAlias(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", "仅 root 管理员可执行此操作"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String id = body.get("id");
        String alias = body.get("alias");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        Texture texture = textureDao.findById(id);
        if (texture == null) {
            ctx.status(404).json(Map.of("success", false, "message", "披风不存在"));
            return;
        }
        textureDao.updateAlias(id, alias);
        ctx.json(Map.of("success", true, "message", "别名已更新"));
    }

    public void downloadCape(Context ctx) {
        String id = ctx.queryParam("id");
        if (id == null) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        Texture texture = textureDao.findById(id);
        if (texture == null) {
            ctx.status(404).json(Map.of("success", false, "message", "披风不存在"));
            return;
        }
        byte[] data = textureService.readFile("CAPE", texture.getHash());
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", "文件不存在"));
            return;
        }
        ctx.contentType("image/png");
        ctx.result(data);
    }

    private String getAdminName(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return "unknown";
        return ctx.sessionAttribute("adminUsername");
    }

    private boolean isRoot(Context ctx) {
        String adminId = SessionManager.getAdminId(ctx);
        if (adminId == null) return false;
        return "ROOT".equals(SessionManager.getAdminRole(ctx));
    }
}
