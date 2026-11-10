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


public final class SqliteDialect implements SqlDialect {

    @Override
    public String id() {
        return "sqlite";
    }

    @Override
    public String autoIncrementPrimaryKey() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public String bigIntType() {
        return "INTEGER";
    }

    @Override
    public String textType(int length) {
        return "TEXT";
    }

    @Override
    public String booleanType() {
        return "INTEGER";
    }

    @Override
    public String timestampType() {
        return "TEXT";
    }

    @Override
    public String limitOffset(String sql, int limit, int offset) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String escapeLike(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public boolean supportsTransactionalDdl() {
        return true;
    }
}
