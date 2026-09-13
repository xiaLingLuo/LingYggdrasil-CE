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
package im.xz.cn.security;

import im.xz.cn.database.DatabaseManager;

public final class RootIntegrityGuard {
    public static final String MESSAGE =
            "数据库核心遭到篡改！为确保安全，目前已关闭35599端口的任何功能！请立即检查系统安全！";

    private RootIntegrityGuard() {}

    public static boolean isValid(DatabaseManager db) {
        try {
            var row = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM root_info");
            if (row == null || row.get("cnt") == null) return false;
            return ((Number) row.get("cnt")).longValue() == 1L;
        } catch (Exception e) {
            return false;
        }
    }
}
