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
package im.xz.cn.social;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.database.dao.ProfileDao;
import im.xz.cn.database.dao.UserPermGroupDao;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.model.PermGroup;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.security.UserPermissions;

import java.util.Optional;
import java.util.function.Function;


public final class SocialIdentityService {

    public static final String DEFAULT_AVATAR = "/img/juststeve_face.png";
    public static final String CANCELLED_DISPLAY_NAME = "账号已注销";

    private final DatabaseManager database;
    private final ProfileDao profileDao;
    private final UserPermGroupDao permGroupDao;
    private final Function<String, String> skinUrlResolver;
    private final im.xz.cn.profile.ProfileTextureResolver textures;

    public SocialIdentityService(DatabaseManager database, Function<String, String> skinUrlResolver) {
        this.textures = new im.xz.cn.profile.ProfileTextureResolver(database);
        this.database = database;
        this.profileDao = new ProfileDao(database);
        this.permGroupDao = new UserPermGroupDao(database);
        this.skinUrlResolver = skinUrlResolver == null ? hash -> null : skinUrlResolver;
    }

    public Optional<SocialIdentity> findByCode(String code) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            return Optional.empty();
        }
        UserRow row = Jdbc.queryOne(database, SocialIdentityService::mapRow,
                "SELECT id, username, nickname, perm_group, display_profile_id, friend_code"
                        + " FROM users WHERE friend_code = ? OR friend_code = ?",
                code.trim(), normalized);
        return row == null ? Optional.empty() : Optional.of(toIdentity(row));
    }

    
    public Optional<SocialIdentity> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        UserRow row = Jdbc.queryOne(database, SocialIdentityService::mapRow,
                "SELECT id, username, nickname, perm_group, display_profile_id, friend_code"
                        + " FROM users WHERE id = ?", userId);
        return row == null ? Optional.empty() : Optional.of(toIdentity(row));
    }

    private static UserRow mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new UserRow(
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("nickname"),
                resultSet.getString("perm_group"),
                resultSet.getString("display_profile_id"),
                resultSet.getString("friend_code"));
    }

    private SocialIdentity toIdentity(UserRow row) {
        boolean cancelled = isCancelled(row.permGroup());
        String displayName = cancelled ? CANCELLED_DISPLAY_NAME : displayName(row);
        String avatar = cancelled ? DEFAULT_AVATAR : resolveAvatar(row.id(), row.displayProfileId());
        return new SocialIdentity(row.id(), row.friendCode(), displayName, avatar, !cancelled, cancelled);
    }

    private boolean isCancelled(String permGroup) {
        if (permGroup == null || permGroup.isBlank()) {
            return false;
        }
        PermGroup group = permGroupDao.findByName(permGroup);
        if (group == null || group.getPermissions() == null) {
            return false;
        }
        for (String part : group.getPermissions().split(",")) {
            if (UserPermissions.STATE_CANCELLED.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private String resolveAvatar(String userId, String displayProfileId) {
        if (displayProfileId == null || displayProfileId.isBlank()) {
            return DEFAULT_AVATAR;
        }
        PlayerProfile profile = profileDao.findById(displayProfileId);
        if (profile == null || userId == null || !userId.equals(profile.getUserId())) {
            return DEFAULT_AVATAR;
        }
        String skinHash = textures.hashFor(profile, "SKIN");
        if (skinHash == null || skinHash.isBlank()) {
            return DEFAULT_AVATAR;
        }
        
        String url = skinUrlResolver.apply(skinHash);
        return url == null || url.isBlank() ? DEFAULT_AVATAR : url;
    }

    private static String displayName(UserRow row) {
        if (row.nickname() != null && !row.nickname().isBlank()) {
            return row.nickname();
        }
        return row.username() == null ? "" : row.username();
    }

    
    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String stripped = code.trim().replace("-", "");
        return stripped.length() == 16 ? stripped : code.trim();
    }

    private record UserRow(String id, String username, String nickname, String permGroup,
                           String displayProfileId, String friendCode) {
    }
}
