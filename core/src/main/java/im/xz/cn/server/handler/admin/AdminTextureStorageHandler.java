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

import im.xz.cn.common.IpUtil;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.CacheDao;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.texture.TextureService;
import im.xz.cn.texture.storage.TextureStorageReconciliationService;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;


public class AdminTextureStorageHandler {

    private final TextureStorageReconciliationService service;

    public AdminTextureStorageHandler(DatabaseManager database) {
        this.service = new TextureStorageReconciliationService(database,
                new TextureService(SystemConfig.getInstance(), new CacheDao(database)));
    }

    public void report(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.skins.orphans")) {
            return;
        }
        TextureStorageReconciliationService.Report report = service.scan();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("unregisteredFiles", report.unregisteredFiles());
        body.put("unreferencedBlobs", report.unreferencedBlobs());
        body.put("missingFiles", report.missingFiles());
        body.put("sampleUnregisteredHashes", report.sampleUnregisteredHashes());
        ctx.json(body);
    }

    public void cleanup(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.skins.orphans")) {
            return;
        }
        int deleted = service.deleteUnregisteredFiles();
        AuditLogger.logSensitiveOperation(ctx.sessionAttribute("adminUsername"),
                "DELETE_ORPHAN_TEXTURE_FILES:" + deleted, IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "deletedFiles", deleted));
    }
}
