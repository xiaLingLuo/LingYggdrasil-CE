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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.identity.UserPreferenceRepository;
import io.javalin.http.Context;

import java.util.Map;


public class UserPreferenceHandler {

    private final UserPreferenceRepository preferences;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserPreferenceHandler(DatabaseManager database) {
        this.preferences = new UserPreferenceRepository(database);
    }

    public void get(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        ctx.json(Map.of("success", true,
                "allowAccountPasswordLogin", preferences.isAccountPasswordLoginAllowed(userId)));
    }

    public void update(Context ctx) {
        String userId = requireUser(ctx);
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> body = mapper.readValue(ctx.body(), new TypeReference<>() {
            });
            Object value = body.get("allowAccountPasswordLogin");
            if (!(value instanceof Boolean allowed)) {
                ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                        "message", "allowAccountPasswordLogin must be boolean"));
                return;
            }
            preferences.setAccountPasswordLoginAllowed(userId, allowed);
            ctx.json(Map.of("success", true, "allowAccountPasswordLogin", allowed));
        } catch (Exception error) {
            ctx.status(400).json(Map.of("success", false, "code", "INVALID_ARGUMENT",
                    "message", "Invalid JSON body"));
        }
    }

    private static String requireUser(Context ctx) {
        String userId = SessionManager.getUserId(ctx);
        if (userId == null) {
            ctx.status(401).json(Map.of("success", false, "code", "UNAUTHORIZED",
                    "message", "Login required"));
        }
        return userId;
    }
}
