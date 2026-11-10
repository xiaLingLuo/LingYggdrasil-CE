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

import java.time.Instant;


public record NotificationJob(
        long id,
        String type,
        String target,
        String payloadJson,
        String locale,
        NotificationState state,
        int attempts,
        Instant nextAttemptAt,
        String idempotencyKey,
        Instant createdAt
) {

    public NotificationJob {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
    }

    public boolean terminal() {
        return state == NotificationState.SENT || state == NotificationState.FAILED;
    }
}
