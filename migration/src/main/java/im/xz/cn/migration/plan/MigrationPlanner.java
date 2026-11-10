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
package im.xz.cn.migration.plan;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.detection.LegacySchemaDetector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public final class MigrationPlanner {

    private final DatabaseManager database;

    public MigrationPlanner(DatabaseManager database) {
        this.database = database;
    }

    public MigrationPlan plan(boolean includeSecrets) {
        LegacySchemaDetector.Detection detection = new LegacySchemaDetector(database).detect();
        List<MigrationPlan.Step> steps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        long textures = detection.counts().getOrDefault("selfTextures", 0L);
        if (textures > 0) {
            steps.add(new MigrationPlan.Step("texture",
                    "blob/asset/upload/library/publication/互动/授权", textures));
        }
        long profiles = count("player_profiles");
        if (profiles > 0) {
            steps.add(new MigrationPlan.Step("profiles", "角色纹理绑定", profiles));
        }
        long settings = count("system_settings");
        if (settings > 0) {
            steps.add(new MigrationPlan.Step("config", "类型化配置组", settings));
        }
        steps.add(new MigrationPlan.Step("identity", "权限组与节点",
                count("user_perm_group")));
        steps.add(new MigrationPlan.Step("social", "好友/申请/拉黑",
                count("friends") + count("confirming_friends") + count("blocked_users")));
        steps.add(new MigrationPlan.Step("sessions", "旧会话令牌失效（需重新登录）",
                count("auth_tokens")));
        if (includeSecrets) {
            steps.add(new MigrationPlan.Step("secrets", "ENC: 密文 → secret_store",
                    count("system_settings")));
        } else {
            warnings.add("secrets step skipped (--keyfile not provided)");
        }

        if (!detection.installed()) {
            warnings.add("legacy installation not detected; verify --sqlite path");
        }
        if (detection.newSchemaReady()) {
            warnings.add("target schema already present; steps are idempotent but re-verify counts");
        }
        return new MigrationPlan(detection.databaseType(), detection.installed(),
                detection.newSchemaReady(), steps, warnings, Instant.now());
    }

    private long count(String table) {
        try {
            return Jdbc.queryCount(database, "SELECT COUNT(*) FROM " + table);
        } catch (RuntimeException missingTable) {
            return 0L;
        }
    }
}
