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
package im.xz.cn.server.handler.admin;

import im.xz.cn.auth.SessionManager;
import im.xz.cn.plugin.PluginIconProvider;
import im.xz.cn.plugin.PluginManager;
import im.xz.cn.plugin.PluginSystemLog;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.web.view.AdminPage;

import io.javalin.http.Context;

import java.util.Map;

public class AdminPluginHandler {

    public AdminPluginHandler() {}

    public void pluginsPage(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.view")) return;
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderPluginsPage(adminUsername, adminRole, csrfToken));
    }

    public void pluginMenuPage(Context ctx) {
        String name = ctx.pathParam("plugin");
        String menuId = ctx.pathParam("menu");
        PluginManager manager = PluginManager.getInstance();
        String fragment = manager.renderMenu(name, menuId, ctx);
        if (fragment == null) {
            ctx.status(404).result("404 Not Found");
            return;
        }
        String title = manager.menuEntries().stream()
                .filter(e -> e.pluginName().equalsIgnoreCase(name) && e.menuId().equals(menuId))
                .map(e -> e.title())
                .findFirst().orElse(name);
        String adminUsername = ctx.sessionAttribute("adminUsername");
        String adminRole = AdminLayoutHelper.roleLabel(ctx);
        String csrfToken = SessionManager.getOrCreateCsrfToken(ctx);
        ctx.html(AdminPage.renderPluginMenuPage(name, menuId, title, fragment, adminUsername, adminRole, csrfToken));
    }

    public void list(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.view")) return;
        ctx.json(Map.of("success", true, "plugins", PluginManager.getInstance().pluginInfos()));
    }

    public void enable(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.edit")) return;
        String name = ctx.pathParam("name");
        String error = PluginManager.getInstance().enable(name);
        if (error == null) audit(ctx, "ENABLE_PLUGIN:" + name);
        respond(ctx, error);
    }

    public void disable(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.edit")) return;
        String name = ctx.pathParam("name");
        String error = PluginManager.getInstance().disable(name);
        if (error == null) audit(ctx, "DISABLE_PLUGIN:" + name);
        respond(ctx, error);
    }

    public void reload(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.edit")) return;
        String name = ctx.pathParam("name");
        String error = PluginManager.getInstance().reload(name);
        if (error == null) audit(ctx, "RELOAD_PLUGIN:" + name);
        respond(ctx, error);
    }

    public void icon(Context ctx) {
        if (!AdminPermissions.require(ctx, "admin.plugin.overall.view")) return;
        String name = ctx.pathParam("name");
        PluginManager manager = PluginManager.getInstance();
        if (manager.find(name) == null) {
            ctx.status(404).result("Plugin not found");
            return;
        }
        PluginIconProvider.IconData data = manager.iconData(name);
        if (data == null) data = PluginIconProvider.defaultIcon();
        if (data == null) {
            ctx.status(404).result("Icon not found");
            return;
        }
        ctx.contentType(data.contentType());
        ctx.result(data.data());
    }

    public void api(Context ctx) {
        String name = ctx.pathParam("name");
        String prefix = "/admin/api/plugins/" + name + "/api";
        String path = ctx.path();
        String suffix = path.length() > prefix.length() ? path.substring(prefix.length()) : "";
        if (!PluginManager.getInstance().handleApi(ctx, name, suffix)) {
            ctx.status(404).json(Map.of("success", false, "message", "Unknown plugin API route"));
        }
    }

    private void respond(Context ctx, String error) {
        if (error == null) {
            ctx.json(Map.of("success", true, "message", I18n.t("msg.pluginOperationSuccess")));
            return;
        }
        String key = switch (error) {
            case "notFound" -> "msg.pluginNotFound";
            case "notHotReloadable" -> "msg.pluginNotHotReloadable";
            case "missingDependency" -> "msg.pluginMissingDependency";
            case "incompatible" -> "msg.pluginIncompatible";
            case "notEnabled" -> "msg.pluginNotEnabled";
            default -> "msg.pluginLoadFailed";
        };
        ctx.status(400).json(Map.of("success", false, "message", I18n.t(key), "code", error));
    }

    private void audit(Context ctx, String action) {
        String admin = ctx.sessionAttribute("adminUsername");
        PluginSystemLog.get().info("[Admin:{}] {}", admin == null ? "?" : admin, action);
    }
}
