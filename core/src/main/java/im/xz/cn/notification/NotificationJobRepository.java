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
package im.xz.cn.notification;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public final class NotificationJobRepository {

    private final DatabaseManager database;

    public NotificationJobRepository(DatabaseManager database) {
        this.database = database;
    }

    
    public long enqueue(String type, String target, String payloadJson, String locale,
                        String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Long existing = Jdbc.queryOne(database, resultSet -> resultSet.getLong(1),
                    "SELECT id FROM notification_jobs WHERE idempotency_key = ?", idempotencyKey);
            if (existing != null) {
                return existing;
            }
        }
        return Jdbc.insert(database,
                "INSERT INTO notification_jobs (type, target, payload_json, locale, state,"
                        + " attempts, next_attempt_at, idempotency_key, created_at)"
                        + " VALUES (?, ?, ?, ?, 'PENDING', 0, NULL, ?, ?)",
                type, target, payloadJson, locale,
                idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey,
                Jdbc.writeInstant(Instant.now()));
    }

    public Optional<NotificationJob> findById(long id) {
        return Optional.ofNullable(Jdbc.queryOne(database, NotificationJobRepository::map,
                "SELECT * FROM notification_jobs WHERE id = ?", id));
    }

    
    public List<NotificationJob> findDue(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return Jdbc.queryMany(database, NotificationJobRepository::map,
                "SELECT * FROM notification_jobs"
                        + " WHERE state IN ('PENDING', 'RETRYING')"
                        + " AND (next_attempt_at IS NULL OR next_attempt_at <= ?)"
                        + " ORDER BY id LIMIT ?",
                Jdbc.writeInstant(Instant.now()), safeLimit);
    }

    public void markSent(long id) {
        Jdbc.update(database,
                "UPDATE notification_jobs SET state = 'SENT', next_attempt_at = NULL WHERE id = ?", id);
    }

    
    public NotificationState markFailed(long id, int maxAttempts, Instant retryAt) {
        NotificationJob job = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("notification job not found: " + id));
        int attempts = job.attempts() + 1;
        if (attempts >= Math.max(1, maxAttempts)) {
            Jdbc.update(database,
                    "UPDATE notification_jobs SET state = 'FAILED', attempts = ?, next_attempt_at = NULL"
                            + " WHERE id = ?", attempts, id);
            return NotificationState.FAILED;
        }
        Jdbc.update(database,
                "UPDATE notification_jobs SET state = 'RETRYING', attempts = ?, next_attempt_at = ?"
                        + " WHERE id = ?", attempts, Jdbc.writeInstant(retryAt), id);
        return NotificationState.RETRYING;
    }

    public long countByState(NotificationState state) {
        return Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM notification_jobs WHERE state = ?", state.name());
    }

    private static NotificationJob map(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new NotificationJob(
                resultSet.getLong("id"),
                resultSet.getString("type"),
                resultSet.getString("target"),
                resultSet.getString("payload_json"),
                resultSet.getString("locale"),
                NotificationState.parse(resultSet.getString("state")),
                resultSet.getInt("attempts"),
                Jdbc.readInstant(resultSet, "next_attempt_at"),
                resultSet.getString("idempotency_key"),
                Jdbc.readInstant(resultSet, "created_at"));
    }
}
