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

import im.xz.cn.plugin.api.PluginResponse;
import io.javalin.http.Context;

final class PluginResponseImpl implements PluginResponse {

    private final Context ctx;

    PluginResponseImpl(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public PluginResponse status(int status) {
        ctx.status(status);
        return this;
    }

    @Override
    public PluginResponse contentType(String contentType) {
        ctx.contentType(contentType);
        return this;
    }

    @Override
    public PluginResponse header(String name, String value) {
        ctx.header(name, value);
        return this;
    }

    @Override
    public void text(String value) {
        ctx.contentType("text/plain; charset=utf-8");
        ctx.result(value == null ? "" : value);
    }

    @Override
    public void html(String value) {
        ctx.contentType("text/html; charset=utf-8");
        ctx.result(value == null ? "" : value);
    }

    @Override
    public void json(Object value) {
        try {
            ctx.json(value);
        } catch (Exception e) {
            if (!ctx.res().isCommitted()) {
                ctx.status(500).result("{\"success\":false}");
            }
        }
    }
}
