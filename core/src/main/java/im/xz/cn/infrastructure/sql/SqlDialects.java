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
package im.xz.cn.infrastructure.sql;

import java.util.Locale;
import java.util.Map;


public final class SqlDialects {

    private static final Map<String, SqlDialect> BY_ID = Map.of(
            "sqlite", new SqliteDialect(),
            "mysql", new MySqlDialect()
    );

    private SqlDialects() {
    }

    public static SqlDialect of(String dbType) {
        String key = dbType == null ? "" : dbType.trim().toLowerCase(Locale.ROOT);
        SqlDialect dialect = BY_ID.get(key);
        if (dialect == null) {
            throw new UnsupportedDatabaseException(key);
        }
        return dialect;
    }

    public static SqlDialect sqlite() {
        return BY_ID.get("sqlite");
    }

    public static SqlDialect mysql() {
        return BY_ID.get("mysql");
    }

    
    public static final class UnsupportedDatabaseException extends IllegalArgumentException {
        private final String databaseType;

        public UnsupportedDatabaseException(String databaseType) {
            super("Unsupported database type: '" + databaseType
                    + "' (supported: sqlite, mysql; postgresql is not enabled)");
            this.databaseType = databaseType;
        }

        public String databaseType() {
            return databaseType;
        }
    }
}
