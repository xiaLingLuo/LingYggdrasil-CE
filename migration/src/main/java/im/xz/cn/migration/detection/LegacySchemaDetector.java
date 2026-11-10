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
package im.xz.cn.migration.detection;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;

import java.util.LinkedHashMap;
import java.util.Map;


public final class LegacySchemaDetector {

    private final DatabaseManager database;

    public LegacySchemaDetector(DatabaseManager database) {
        this.database = database;
    }

    public Detection detect() {
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, String> problems = new LinkedHashMap<>();

        counts.put("users", count("SELECT COUNT(*) FROM users", problems, "users"));
        counts.put("textures", count("SELECT COUNT(*) FROM textures", problems, "textures"));
        counts.put("selfTextures", count("SELECT COUNT(*) FROM textures WHERE reference_type IS NULL"
                + " OR reference_type = '' OR reference_type = 'self'", problems, "textures.reference_type"));
        counts.put("publicVisibility", count("SELECT COUNT(*) FROM texture_visibility WHERE is_public = 1",
                problems, "texture_visibility"));
        counts.put("likes", count("SELECT COUNT(*) FROM texture_likes", problems, "texture_likes"));
        counts.put("favorites", count("SELECT COUNT(*) FROM texture_favorites", problems, "texture_favorites"));
        counts.put("friendShares", count("SELECT COUNT(*) FROM friend_shared_textures", problems, "friend_shared_textures"));

        
        boolean newSchema = exists("SELECT COUNT(*) FROM texture_assets")
                || exists("SELECT COUNT(*) FROM ly_texture_assets");
        boolean installed = exists("SELECT COUNT(*) FROM root_info");
        return new Detection(installed, newSchema, counts, problems, database.getDbType());
    }

    private long count(String sql, Map<String, String> problems, String label) {
        try {
            return Jdbc.queryCount(database, sql);
        } catch (RuntimeException error) {
            problems.put(label, error.getMessage());
            return 0L;
        }
    }

    private boolean exists(String sql) {
        try {
            Jdbc.queryCount(database, sql);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    
    public record Detection(boolean installed, boolean newSchemaReady, Map<String, Long> counts,
                            Map<String, String> problems, String databaseType) {

        public String toText() {
            StringBuilder builder = new StringBuilder();
            builder.append("database=").append(databaseType)
                    .append(" installed=").append(installed)
                    .append(" newSchemaReady=").append(newSchemaReady).append('\n');
            counts.forEach((key, value) -> builder.append("  ").append(key).append('=').append(value).append('\n'));
            if (!problems.isEmpty()) {
                builder.append("  problems:\n");
                problems.forEach((key, value) ->
                        builder.append("    - ").append(key).append(": ").append(value).append('\n'));
            }
            return builder.toString();
        }
    }
}
