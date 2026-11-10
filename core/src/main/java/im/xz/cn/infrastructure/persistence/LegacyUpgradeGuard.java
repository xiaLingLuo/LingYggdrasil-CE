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
package im.xz.cn.infrastructure.persistence;

import im.xz.cn.database.DatabaseManager;


public final class LegacyUpgradeGuard {

    private LegacyUpgradeGuard() {
    }

    public static void requireMigrated(DatabaseManager database) {
        if (tableExists(database, "texture_assets") && tableExists(database, "instance_meta")
                && hasSchemaVersion(database)) {
            return;
        }
        if (!tableExists(database, "users")) {
            return;
        }
        long users = count(database, "users");
        long legacyTextures = tableExists(database, "textures") ? count(database, "textures") : 0;
        if (users == 0 && legacyTextures == 0) {
            return;
        }
        throw new IllegalStateException(
                "检测到未迁移的旧数据库。请先停止主程序并运行 LingYggdrasil-migrator "
                        + "detect/plan/all/verify（含 secrets）后再启动。");
    }

    private static boolean hasSchemaVersion(DatabaseManager database) {
        try {
            return Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM instance_meta WHERE meta_key = 'schema_version'") > 0;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static long count(DatabaseManager database, String table) {
        try {
            return Jdbc.queryCount(database, "SELECT COUNT(*) FROM " + table);
        } catch (RuntimeException error) {
            return 0;
        }
    }

    private static boolean tableExists(DatabaseManager database, String table) {
        try (java.sql.Connection connection = database.getConnection();
             java.sql.ResultSet tables = connection.getMetaData()
                     .getTables(null, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch (java.sql.SQLException error) {
            return false;
        }
    }
}
