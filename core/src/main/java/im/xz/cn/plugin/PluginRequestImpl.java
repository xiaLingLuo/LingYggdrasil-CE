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
package im.xz.cn.plugin;

import im.xz.cn.plugin.api.PluginRequest;
import im.xz.cn.security.AdminPermissions;
import im.xz.cn.security.UserPermissions;
import io.javalin.http.Context;

final class PluginRequestImpl implements PluginRequest {

    private final Context ctx;
    private final String suffix;

    PluginRequestImpl(Context ctx, String suffix) {
        this.ctx = ctx;
        this.suffix = suffix;
    }

    @Override
    public String method() {
        return ctx.method().name();
    }

    @Override
    public String path() {
        return ctx.path();
    }

    @Override
    public String suffix() {
        return suffix;
    }

    @Override
    public String pathParam(String name) {
        return ctx.pathParam(name);
    }

    @Override
    public String query(String name) {
        return ctx.queryParam(name);
    }

    @Override
    public String header(String name) {
        return ctx.header(name);
    }

    @Override
    public String body() {
        return ctx.body();
    }

    @Override
    public String sessionAttribute(String name) {
        Object value = ctx.sessionAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public boolean hasAdminPermission(String node) {
        return AdminPermissions.has(node);
    }

    @Override
    public boolean hasUserPermission(String node) {
        return UserPermissions.has(node);
    }
}
