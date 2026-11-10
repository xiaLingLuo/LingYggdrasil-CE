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

import im.xz.cn.audit.AuditEventRepository;
import im.xz.cn.database.DatabaseManager;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class AdminAuditHandler {

    private final AuditEventRepository audit;

    public AdminAuditHandler(DatabaseManager database) {
        this.audit = new AuditEventRepository(database);
    }

    public void list(Context ctx) {
        if (!im.xz.cn.security.AdminPermissions.require(ctx, "admin.system.view")) {
            return;
        }
        int limit = 100;
        String rawLimit = ctx.queryParam("limit");
        if (rawLimit != null && !rawLimit.isBlank()) {
            try {
                limit = Integer.parseInt(rawLimit.trim());
            } catch (NumberFormatException ignored) {
                limit = 100;
            }
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (AuditEventRepository.AuditEvent event : audit.recent(limit)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", event.id());
            item.put("actorType", event.actorType());
            item.put("actorId", event.actorId());
            item.put("action", event.action());
            item.put("targetType", event.targetType());
            item.put("targetId", event.targetId());
            item.put("result", event.result());
            item.put("reason", event.reason());
            item.put("correlationId", event.correlationId());
            item.put("occurredAt", event.occurredAt() == null ? null : event.occurredAt().toString());
            items.add(item);
        }
        ctx.json(Map.of("success", true, "items", items));
    }
}
