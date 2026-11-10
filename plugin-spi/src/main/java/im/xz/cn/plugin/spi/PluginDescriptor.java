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
package im.xz.cn.plugin.spi;

import java.util.List;
import java.util.regex.Pattern;


public record PluginDescriptor(
        String id,
        String name,
        String version,
        String mainClass,
        String apiVersion,
        String author,
        String description,
        List<String> dependencies,
        List<String> permissions
) {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{1,63}");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+){0,3}([-+][0-9A-Za-z.-]+)?");

    public PluginDescriptor {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("invalid plugin id: " + id);
        }
        if (mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("plugin mainClass is required");
        }
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("invalid plugin version: " + version);
        }
        if (apiVersion == null || !VERSION_PATTERN.matcher(apiVersion).matches()) {
            throw new IllegalArgumentException("invalid plugin apiVersion: " + apiVersion);
        }
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        name = name == null || name.isBlank() ? id : name;
    }
}
