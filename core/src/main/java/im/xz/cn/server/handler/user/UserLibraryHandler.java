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
package im.xz.cn.server.handler.user;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.texture.library.LibraryEntry;
import im.xz.cn.texture.library.LibraryEntryRepository;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class UserLibraryHandler {

    private final LibraryEntryRepository library;

    public UserLibraryHandler(DatabaseManager database) {
        this.library = new LibraryEntryRepository(database);
    }

    public void list(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED",
                    "message", "Login required"));
            return;
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (LibraryEntry entry : library.findByUser(userId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", entry.id());
            item.put("assetId", entry.assetId());
            item.put("alias", entry.alias());
            item.put("hidden", entry.hidden());
            item.put("createdAt", entry.createdAt() == null ? null : entry.createdAt().toString());
            items.add(item);
        }
        ctx.json(Map.of("success", true, "items", items));
    }
}
