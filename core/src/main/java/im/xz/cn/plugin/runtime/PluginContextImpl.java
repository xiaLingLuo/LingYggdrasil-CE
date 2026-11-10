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
package im.xz.cn.plugin.runtime;

import im.xz.cn.plugin.spi.PluginContext;
import im.xz.cn.plugin.spi.PluginHttpHandler;
import im.xz.cn.plugin.spi.PluginHttpRequest;
import im.xz.cn.plugin.spi.PluginHttpResponse;
import im.xz.cn.plugin.spi.PluginLogger;
import im.xz.cn.plugin.spi.PluginMenus;
import im.xz.cn.plugin.spi.PluginRoutes;
import im.xz.cn.plugin.spi.PluginScheduler;
import im.xz.cn.plugin.spi.PluginStorage;

import java.nio.file.Path;
import java.util.Map;


public final class PluginContextImpl implements PluginContext {

    private final String pluginId;
    private final Path dataDirectory;
    private final PluginLogger logger;
    private final PluginStorage storage;
    private final PluginScheduler scheduler;
    private final im.xz.cn.plugin.PluginManager manager;

    public PluginContextImpl(String pluginId, Path dataDirectory, PluginLogger logger,
                             PluginStorage storage, PluginScheduler scheduler,
                             im.xz.cn.plugin.PluginManager manager) {
        this.pluginId = pluginId;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.storage = storage;
        this.scheduler = scheduler;
        this.manager = manager;
    }

    @Override
    public String pluginId() {
        return pluginId;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }

    @Override
    public PluginLogger logger() {
        return logger;
    }

    @Override
    public PluginStorage storage() {
        return storage;
    }

    @Override
    public PluginScheduler scheduler() {
        return scheduler;
    }

    @Override
    public PluginRoutes routes() {
        return (method, path, handler) -> {
            if (handler == null) return;
            manager.registerUserRouteForPlugin(pluginId, method, path, (apiRequest, apiResponse) -> {
                PluginHttpResponse response = handler.handle(toRequest(apiRequest));
                if (response == null) {
                    apiResponse.status(204);
                    return;
                }
                apiResponse.status(response.status());
                if (response.contentType() != null) {
                    apiResponse.contentType(response.contentType());
                }
                apiResponse.text(new String(response.body(),
                        java.nio.charset.StandardCharsets.UTF_8));
            });
        };
    }

    @Override
    public im.xz.cn.plugin.spi.PluginPermissions permissions() {
        return new im.xz.cn.plugin.spi.PluginPermissions() {
            private final java.util.Set<im.xz.cn.permission.PermissionNode> nodes =
                    new java.util.LinkedHashSet<>();

            @Override
            public void registerUser(String key, String category, String description) {
                add(new im.xz.cn.permission.PermissionNode(key,
                        im.xz.cn.permission.PermissionType.USER,
                        category == null ? "plugin" : category, pluginId, description, false));
            }

            @Override
            public void registerAdmin(String key, String category, String description) {
                add(new im.xz.cn.permission.PermissionNode(key,
                        im.xz.cn.permission.PermissionType.ADMIN,
                        category == null ? "plugin" : category, pluginId, description, false));
            }

            private void add(im.xz.cn.permission.PermissionNode node) {
                nodes.add(node);
                im.xz.cn.permission.PermissionRegistry.getInstance()
                        .registerSource(pluginId, java.util.List.copyOf(nodes));
            }
        };
    }

    @Override
    public PluginMenus menus() {
        return (id, title, renderer) -> {
            if (renderer == null) return;
            manager.registerMenuForPlugin(pluginId,
                    new im.xz.cn.plugin.api.PluginMenu(id, title,
                            request -> renderer.render(toRequest(request))));
        };
    }

    private static PluginHttpRequest toRequest(im.xz.cn.plugin.api.PluginRequest request) {
        return new PluginHttpRequest(request.method(), request.path(), null, Map.of(),
                request.body() == null ? new byte[0]
                        : request.body().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
