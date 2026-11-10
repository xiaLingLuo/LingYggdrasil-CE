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
package im.xz.cn.identity;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.time.Instant;


public final class UserPreferenceRepository {

    private final DatabaseManager database;

    public UserPreferenceRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isAccountPasswordLoginAllowed(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Boolean allowed = Jdbc.queryOne(database,
                resultSet -> resultSet.getBoolean("allow_account_password_login"),
                "SELECT allow_account_password_login FROM user_preferences WHERE user_id = ?", userId);
        return allowed == null || allowed;
    }

    public void setAccountPasswordLoginAllowed(String userId, boolean allowed) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        String now = Jdbc.writeInstant(Instant.now());
        long existing = Jdbc.queryCount(database,
                "SELECT COUNT(*) FROM user_preferences WHERE user_id = ?", userId);
        if (existing > 0) {
            Jdbc.update(database,
                    "UPDATE user_preferences SET allow_account_password_login = ?, updated_at = ?"
                            + " WHERE user_id = ?", allowed, now, userId);
        } else {
            Jdbc.update(database,
                    "INSERT INTO user_preferences (user_id, allow_account_password_login, updated_at)"
                            + " VALUES (?, ?, ?)", userId, allowed, now);
        }
    }

    
    public long countRows() {
        return Jdbc.queryCount(database, "SELECT COUNT(*) FROM user_preferences");
    }
}
