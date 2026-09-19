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
package im.xz.cn.permission;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PermissionRegistry {
    public static final String BUILTIN_SOURCE = "LingYggdrasil";

    private static final PermissionRegistry INSTANCE = new PermissionRegistry();

    private final Map<String, List<PermissionNode>> bySource = new LinkedHashMap<>();
    private final List<String> sourceOrder = new ArrayList<>();
    private final Map<PermissionType, Set<String>> validKeys = new EnumMap<>(PermissionType.class);
    private final Map<String, Boolean> highRisk = new HashMap<>();

    private PermissionRegistry() {
        for (PermissionType type : PermissionType.values()) {
            validKeys.put(type, new LinkedHashSet<>());
        }
    }

    public static PermissionRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void registerSource(String source, List<PermissionNode> nodes) {
        if (source == null || source.isBlank()) return;
        if (nodes == null) nodes = List.of();
        if (!bySource.containsKey(source)) {
            sourceOrder.add(source);
        }
        bySource.put(source, List.copyOf(nodes));
        rebuild();
    }

    public synchronized void unregisterSource(String source) {
        if (bySource.remove(source) != null) {
            sourceOrder.remove(source);
            rebuild();
        }
    }

    private void rebuild() {
        for (Set<String> keys : validKeys.values()) keys.clear();
        highRisk.clear();
        for (String source : sourceOrder) {
            for (PermissionNode node : bySource.get(source)) {
                validKeys.get(node.type()).add(node.key());
                if (node.highRisk()) highRisk.put(node.key(), Boolean.TRUE);
            }
        }
    }

    public synchronized List<PermissionNode> all(PermissionType type) {
        List<PermissionNode> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String source : sourceOrder) {
            for (PermissionNode node : bySource.get(source)) {
                if (node.type() == type && seen.add(node.key())) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    public synchronized Set<String> validKeys(PermissionType type) {
        return Set.copyOf(validKeys.get(type));
    }

    public synchronized boolean isHighRisk(String key) {
        return Boolean.TRUE.equals(highRisk.get(key));
    }

    public synchronized List<String> sources() {
        return List.copyOf(sourceOrder);
    }

    public synchronized List<PermissionNode> nodesOfSource(String source) {
        List<PermissionNode> nodes = bySource.get(source);
        return nodes == null ? List.of() : List.copyOf(nodes);
    }
}
