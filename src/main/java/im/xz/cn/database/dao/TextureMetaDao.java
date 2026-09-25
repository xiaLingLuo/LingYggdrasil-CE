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
package im.xz.cn.database.dao;

import im.xz.cn.database.DatabaseManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, String> getAdminAliases(List<String> hashes) {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (hashes == null || hashes.isEmpty()) return aliases;
        int batchSize = 500;
        for (int offset = 0; offset < hashes.size(); offset += batchSize) {
            List<String> batch = hashes.subList(offset, Math.min(offset + batchSize, hashes.size()));
            String placeholders = String.join(",", java.util.Collections.nCopies(batch.size(), "?"));
            List<Object> params = new ArrayList<>(batch);
            for (Map<String, Object> row : db.executeQuery(
                    "SELECT hash, admin_alias FROM texture_meta WHERE hash IN (" + placeholders + ")",
                    params.toArray())) {
                String hash = String.valueOf(row.get("hash"));
                Object alias = row.get("admin_alias");
                aliases.put(hash, alias == null ? null : String.valueOf(alias));
            }
        }
        return aliases;
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
