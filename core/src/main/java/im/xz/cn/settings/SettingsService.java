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
package im.xz.cn.settings;

import im.xz.cn.database.DatabaseManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


public final class SettingsService {

    
    public record GroupSnapshot(String group, long revision, Map<String, String> values) {
        public GroupSnapshot {
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public String get(String key) {
            return values.get(key);
        }
    }

    private final SettingsRepository repository;
    private final Map<String, AtomicReference<GroupSnapshot>> snapshots = new ConcurrentHashMap<>();

    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    public static SettingsService create(DatabaseManager database) {
        return new SettingsService(new SettingsRepository(database));
    }

    
    public GroupSnapshot snapshot(String group) {
        return snapshots.computeIfAbsent(group, key -> new AtomicReference<>(
                new GroupSnapshot(group, repository.revision(group), repository.load(group)))).get();
    }

    
    public GroupSnapshot commit(String group, Map<String, String> values, long expectedRevision) {
        
        GroupSnapshot current = snapshot(group);
        Map<String, String> merged = new java.util.LinkedHashMap<>(current.values());
        if (values != null) {
            merged.putAll(values);
        }
        long newRevision = repository.save(group, merged, expectedRevision);
        GroupSnapshot snapshot = new GroupSnapshot(group, newRevision, merged);
        snapshots.computeIfAbsent(group, key -> new AtomicReference<>(snapshot)).set(snapshot);
        return snapshot;
    }
}
