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

import im.xz.cn.i18n.I18n;
import im.xz.cn.security.UserPermissions;
import im.xz.cn.texture.asset.TextureType;
import io.javalin.http.Context;

import java.util.Map;


public final class UserAccessGuard {

    private UserAccessGuard() {
    }

    public static boolean requireTexture(Context ctx, TextureType type) {
        return require(ctx, type == TextureType.CAPE ? "user.capes" : "user.skins");
    }

    public static boolean requireAnyTexture(Context ctx) {
        if (UserPermissions.has("user.skins") || UserPermissions.has("user.capes")) {
            return true;
        }
        forbidden(ctx);
        return false;
    }

    public static boolean require(Context ctx, String permission) {
        if (UserPermissions.has(permission)) {
            return true;
        }
        forbidden(ctx);
        return false;
    }

    private static void forbidden(Context ctx) {
        ctx.status(403).json(Map.of("success", false, "code", "FORBIDDEN",
                "message", I18n.t("msg.noPermission")));
    }
}
