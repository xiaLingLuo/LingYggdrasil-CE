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
package im.xz.cn.database;

public class TextureMetaDao {
    private final DatabaseManager db;

    public TextureMetaDao(DatabaseManager db) {
        this.db = db;
    }

    public String getAdminAlias(String hash) {
        var result = db.executeQuerySingle("SELECT admin_alias FROM texture_meta WHERE hash = ?", hash);
        if (result != null && result.get("admin_alias") != null) {
            return (String) result.get("admin_alias");
        }
        return null;
    }

    public void setAdminAlias(String hash, String alias) {
        if (alias == null || alias.isBlank()) {
            db.executeUpdate("DELETE FROM texture_meta WHERE hash = ?", hash);
            return;
        }
        int updated = db.executeUpdate("UPDATE texture_meta SET admin_alias = ? WHERE hash = ?", alias, hash);
        if (updated == 0) {
            db.executeUpdate("INSERT INTO texture_meta (hash, admin_alias) VALUES (?, ?)", hash, alias);
        }
    }
}
