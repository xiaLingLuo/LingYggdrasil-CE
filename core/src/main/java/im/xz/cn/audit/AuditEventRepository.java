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
package im.xz.cn.audit;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;


public final class AuditEventRepository {

    public record AuditEvent(long id, String actorType, String actorId, String action,
                             String targetType, String targetId, String result, String reason,
                             String correlationId, Instant occurredAt) {
    }

    private final DatabaseManager database;

    public AuditEventRepository(DatabaseManager database) {
        this.database = database;
    }

    public void append(String actorType, String actorId, String action, String targetType,
                       String targetId, String result, String reason, String correlationId,
                       Instant occurredAt) {
        Jdbc.insert(database,
                "INSERT INTO audit_events (actor_type, actor_id, action, target_type, target_id,"
                        + " result, reason, correlation_id, occurred_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                actorType, actorId, action, targetType, targetId, result, reason, correlationId,
                Jdbc.writeInstant(occurredAt == null ? Instant.now() : occurredAt));
    }

    public List<AuditEvent> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return Jdbc.queryMany(database, resultSet -> new AuditEvent(
                        resultSet.getLong("id"),
                        resultSet.getString("actor_type"),
                        resultSet.getString("actor_id"),
                        resultSet.getString("action"),
                        resultSet.getString("target_type"),
                        resultSet.getString("target_id"),
                        resultSet.getString("result"),
                        resultSet.getString("reason"),
                        resultSet.getString("correlation_id"),
                        Jdbc.readInstant(resultSet, "occurred_at")),
                "SELECT * FROM audit_events ORDER BY id DESC LIMIT ?", safeLimit);
    }

    public long count() {
        return Jdbc.queryCount(database, "SELECT COUNT(*) FROM audit_events");
    }
}
