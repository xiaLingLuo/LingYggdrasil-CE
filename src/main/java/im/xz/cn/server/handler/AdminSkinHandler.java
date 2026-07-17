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
package im.xz.cn.server.handler;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.TextureDao;
import im.xz.cn.database.TextureMetaDao;
import im.xz.cn.database.UserDao;
import im.xz.cn.model.Admin;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.model.enums.AdminRole;
import im.xz.cn.something.web.AdminPage;
import im.xz.cn.util.AuditLogger;
import im.xz.cn.util.IpUtil;
import im.xz.cn.util.TextureService;

import io.javalin.http.Context;

import java.util.*;

public class AdminSkinHandler {
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;
    private final TextureMetaDao metaDao;
    private final DatabaseManager db;

    public AdminSkinHandler(TextureDao textureDao, TextureService textureService, UserDao userDao, DatabaseManager db) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
        this.metaDao = new TextureMetaDao(db);
        this.db = db;
    }

    public void skinsPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = SessionManager.getAdminRole(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderSkinsPage(adminUsername, adminRole, csrfToken));
    }

    public void getSkins(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", "仅 root 管理员"));
            return;
        }
        // Group by hash: first uploader info + ref count
        String sql = """
            SELECT t.hash, t.size,
                   MIN(t.created_at) AS created_at,
                   MIN(t.original_name) AS original_name,
                   COUNT(*) AS ref_count
            FROM textures t
            WHERE t.type = 'SKIN'
            GROUP BY t.hash
            ORDER BY created_at DESC
            """;
        var rows = db.executeQuery(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            String hash = String.valueOf(row.get("hash"));
            map.put("hash", hash);
            String adminAlias = metaDao.getAdminAlias(hash);
            map.put("adminAlias", adminAlias != null ? adminAlias : "");
            map.put("originalName", row.get("original_name"));
            map.put("size", row.get("size"));
            map.put("refCount", ((Number) row.get("ref_count")).intValue());
            map.put("createdAt", String.valueOf(row.get("created_at")));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    public void uploadSkin(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", "仅 root 管理员"));
            return;
        }
        ctx.json(Map.of("success", false, "message", "管理员暂不支持直接上传皮肤"));
    }

    @SuppressWarnings("unchecked")
    public void deleteSkin(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", "仅 root 管理员"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String hash = body.get("hash");
        if (hash == null || hash.isBlank()) {
            ctx.json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        // Delete all texture records for this hash
        List<Texture> all = textureDao.findAll("SKIN");
        int deleted = 0;
        for (Texture t : all) {
            if (t.getHash().equals(hash)) {
                textureDao.delete(t.getId());
                deleted++;
            }
        }
        textureService.deleteFile("SKIN", hash);
        metaDao.setAdminAlias(hash, null);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_SKIN_HASH:" + hash, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", "已删除 " + deleted + " 条皮肤记录"));
    }

    @SuppressWarnings("unchecked")
    public void updateAlias(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", "仅 root 管理员"));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String hash = body.get("hash");
        String alias = body.get("alias");
        if (hash == null || hash.isBlank()) {
            ctx.json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        metaDao.setAdminAlias(hash, alias != null && !alias.isBlank() ? alias.trim() : null);
        ctx.json(Map.of("success", true, "message", "别名已更新"));
    }

    public void downloadSkin(Context ctx) {
        String hash = ctx.queryParam("hash");
        if (hash == null || hash.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", "参数缺失"));
            return;
        }
        byte[] data = textureService.readFile("SKIN", hash);
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
