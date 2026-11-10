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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.common.IpUtil;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.AuditLogger;
import im.xz.cn.profile.ProfileTransferService;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.Map;


public class AdminProfileTransferHandler {

    private final DatabaseManager database;
    private final ProfileTransferService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminProfileTransferHandler(DatabaseManager database) {
        this.database = database;
        this.service = new ProfileTransferService(database);
    }

    public void transfer(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.profiles.transfer")) {
            return;
        }
        Map<String, Object> body;
        try {
            body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid JSON body"));
            return;
        }
        String profileId = body.get("id") instanceof String id ? id : ctx.pathParam("id");
        String toUserId = body.get("toUserId") instanceof String to ? to
                : body.get("userId") instanceof String userId ? userId : null;
        if (profileId == null || profileId.isBlank() || toUserId == null || toUserId.isBlank()) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "id and userId are required"));
            return;
        }
        im.xz.cn.model.PlayerProfile profile = new im.xz.cn.database.dao.ProfileDao(database).findById(profileId);
        if (profile == null) {
            ctx.status(404).json(Map.of("success", false, "code", "PROFILE_NOT_FOUND",
                    "message", "Profile not found"));
            return;
        }
        String fromUserId = profile.getUserId();
        ProfileTransferService.Result result = service.transfer(profileId, fromUserId, toUserId);
        int status = switch (result.status()) {
            case TRANSFERRED -> 200;
            case PROFILE_NOT_FOUND -> 404;
            case NOT_OWNER, TARGET_NOT_FOUND, TARGET_INACCESSIBLE, SAME_OWNER -> 400;
        };
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.status() == ProfileTransferService.Status.TRANSFERRED);
        response.put("status", result.status().name());
        if (result.status() == ProfileTransferService.Status.TRANSFERRED) {
            response.put("message", "所有权已转移");
            response.put("worldTreeKey", result.worldTreeKey());
            response.put("sessionsRevoked", result.sessionsRevoked());
            response.put("bindingsKept", result.bindingsKept());
            response.put("bindingsRemoved", result.bindingsRemoved());
            String actor = ctx.sessionAttribute("adminUsername");
            AuditLogger.logSensitiveOperation(actor,
                    "PROFILE_TRANSFER:" + profileId + ":" + fromUserId + "->" + toUserId,
                    IpUtil.getClientIp(ctx));
            new im.xz.cn.audit.AuditEventRepository(database).append("ADMIN", actor,
                    "PROFILE_TRANSFER", "PROFILE", profileId, "SUCCESS",
                    fromUserId + "->" + toUserId, null, java.time.Instant.now());
        }
        ctx.status(status).json(response);
    }
}
