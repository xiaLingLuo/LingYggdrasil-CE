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
package im.xz.cn.identity;

import java.util.Objects;


public record Actor(ActorType type, String id) {

    public Actor {
        Objects.requireNonNull(type, "type");
        if ((type == ActorType.USER || type == ActorType.ADMIN || type == ActorType.PROFILE)
                && (id == null || id.isBlank())) {
            throw new IllegalArgumentException(type + " actor requires a non-blank id");
        }
    }

    public static Actor anonymous() {
        return new Actor(ActorType.ANONYMOUS, null);
    }

    public static Actor system() {
        return new Actor(ActorType.SYSTEM, null);
    }

    public static Actor user(String userId) {
        return new Actor(ActorType.USER, userId);
    }

    public static Actor admin(String adminId) {
        return new Actor(ActorType.ADMIN, adminId);
    }

    public static Actor profile(String profileId) {
        return new Actor(ActorType.PROFILE, profileId);
    }

    public boolean authenticated() {
        return type == ActorType.USER || type == ActorType.ADMIN;
    }
}
