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
package im.xz.cn.server.handler.admin;


import im.xz.cn.auth.SessionManager;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.TextureDao;
import im.xz.cn.database.dao.TextureMetaDao;
import im.xz.cn.database.dao.UserDao;
import im.xz.cn.model.Texture;
import im.xz.cn.model.User;
import im.xz.cn.web.view.AdminPage;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.common.IpUtil;
import im.xz.cn.texture.TextureService;

import io.javalin.http.Context;

import java.util.*;

public class AdminCapeHandler {
    private final TextureDao textureDao;
    private final TextureService textureService;
    private final UserDao userDao;
    private final TextureMetaDao metaDao;
    private final DatabaseManager db;
    private final SystemConfig systemConfig;

    public AdminCapeHandler(TextureDao textureDao, TextureService textureService, UserDao userDao, DatabaseManager db, SystemConfig systemConfig) {
        this.textureDao = textureDao;
        this.textureService = textureService;
        this.userDao = userDao;
        this.metaDao = new TextureMetaDao(db);
        this.db = db;
        this.systemConfig = systemConfig;
    }

    public void capesPage(Context ctx) {
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderCapesPage(adminUsername, adminRole, csrfToken));
    }

    public void getCapes(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.capes.view")) return;
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        String sql = """
            SELECT t.hash, t.size,
                   MIN(t.created_at) AS created_at,
                   MIN(t.original_name) AS original_name,
                   COUNT(*) AS ref_count
            FROM textures t
            WHERE t.type = 'CAPE'
            GROUP BY t.hash
            ORDER BY created_at DESC
            """;
        var rows = db.executeQuery(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> hashes = rows.stream().map(row -> String.valueOf(row.get("hash"))).toList();
        Map<String, String> aliases = metaDao.getAdminAliases(hashes);
        for (var row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            String hash = String.valueOf(row.get("hash"));
            map.put("hash", hash);
            String adminAlias = aliases.get(hash);
            map.put("adminAlias", adminAlias != null ? adminAlias : "");
            map.put("originalName", row.get("original_name"));
            map.put("size", row.get("size"));
            map.put("refCount", ((Number) row.get("ref_count")).intValue());
            map.put("createdAt", String.valueOf(row.get("created_at")));
            result.add(map);
        }
        ctx.json(Map.of("success", true, "textures", result));
    }

    public void uploadCape(Context ctx) {
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        ctx.json(Map.of("success", false, "message", I18n.t("msg.adminNoUploadCape")));
    }

    @SuppressWarnings("unchecked")
    public void deleteCape(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.capes.delete")) return;
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String hash = body.get("hash");
        if (hash == null || hash.isBlank()) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        List<Texture> all = textureDao.findAll("CAPE");
        int deleted = 0;
        for (Texture t : all) {
            if (t.getHash().equals(hash)) {
                textureDao.delete(t.getId());
                deleted++;
            }
        }
        textureService.deleteFile("CAPE", hash);
        metaDao.setAdminAlias(hash, null);
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_CAPE_HASH:" + hash, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.deletedCapeRecords", deleted)));
    }

    public void deleteOrphanCapes(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.capes.orphans")) return;
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        int deleted = deleteOrphans("CAPE");
        AuditLogger.logSensitiveOperation(getAdminName(ctx), "DELETE_ORPHAN_CAPES:" + deleted, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.deletedOrphanCapes", deleted)));
    }

    private int deleteOrphans(String type) {
        int count = 0;
        List<Map<String, Object>> rows = db.executeQuery(
                "SELECT DISTINCT t.hash AS hash FROM textures t WHERE t.type = ? AND NOT EXISTS ("
                        + "SELECT 1 FROM textures s WHERE s.type = t.type AND s.hash = t.hash "
                        + "AND (s.reference_type IS NULL OR s.reference_type = '' OR s.reference_type = 'self'))",
                type);
        for (Map<String, Object> row : rows) {
            String hash = String.valueOf(row.get("hash"));
            db.executeUpdate("DELETE FROM textures WHERE type = ? AND hash = ?", type, hash);
            textureService.deleteFile(type, hash);
            metaDao.setAdminAlias(hash, null);
            count++;
        }
        for (String hash : textureService.listStoredHashes(type)) {
            if (textureDao.countByHash(type, hash) == 0) {
                textureService.deleteFile(type, hash);
                metaDao.setAdminAlias(hash, null);
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public void updateAlias(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.capes.alias")) return;
        if (!isRoot(ctx)) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return;
        }
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String hash = body.get("hash");
        String alias = body.get("alias");
        if (hash == null || hash.isBlank()) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        if (alias != null && !alias.isBlank() && systemConfig.isCapeNameBlacklisted(alias.trim())) {
            ctx.json(Map.of("success", false, "message", I18n.t("msg.nameTaken")));
            return;
        }
        metaDao.setAdminAlias(hash, alias != null && !alias.isBlank() ? alias.trim() : null);
        ctx.json(Map.of("success", true, "message", I18n.t("msg.aliasUpdated")));
    }

    public void downloadCape(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.capes.download")) return;
        String hash = ctx.queryParam("hash");
        if (hash == null || hash.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }
        byte[] data = textureService.readFile("CAPE", hash);
        if (data == null) {
            ctx.status(404).json(Map.of("success", false, "message", I18n.t("msg.fileMissing")));
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
        return SessionManager.isAdminRoot(ctx);
    }
}
