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
package im.xz.cn.server.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InstallRateLimiter {
    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 3600_000; // 一时辰

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> attempts = new ConcurrentHashMap<>();

    public boolean allow(String ip) {
        long now = System.currentTimeMillis();
        CopyOnWriteArrayList<Long> list = attempts.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());
        
        list.removeIf(t -> now - t > WINDOW_MS);
        
        if (list.size() >= MAX_ATTEMPTS) {
            return false;
        }
        
        list.add(now);
        return true;
    }
}
