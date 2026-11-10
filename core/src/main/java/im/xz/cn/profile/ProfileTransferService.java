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
package im.xz.cn.profile;

import im.xz.cn.auth.AuthService;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.infrastructure.persistence.UnitOfWork;
import im.xz.cn.model.User;
import im.xz.cn.security.UserPermissions;
import im.xz.cn.texture.library.TextureUploadRepository;
import im.xz.cn.texture.publication.PublicationRepository;
import im.xz.cn.texture.sharing.ShareGrantRepository;

import java.util.List;


public final class ProfileTransferService {

    public enum Status {
        TRANSFERRED,
        PROFILE_NOT_FOUND,
        NOT_OWNER,
        TARGET_NOT_FOUND,
        TARGET_INACCESSIBLE,
        SAME_OWNER
    }

    public record Result(Status status, String worldTreeKey, int sessionsRevoked,
                         int bindingsKept, int bindingsRemoved) {
    }

    private final UnitOfWork unitOfWork;
    private final DatabaseManager database;
    private final TextureUploadRepository uploads;
    private final PublicationRepository publications;
    private final ShareGrantRepository grants;

    public ProfileTransferService(DatabaseManager database) {
        this.database = database;
        this.unitOfWork = new UnitOfWork(database);
        this.uploads = new TextureUploadRepository(database);
        this.publications = new PublicationRepository(database);
        this.grants = new ShareGrantRepository(database);
    }

    public Result transfer(String profileId, String fromUserId, String toUserId) {
        return unitOfWork.execute(connection -> {
            ProfileRow profile = findProfile(profileId);
            if (profile == null) {
                return new Result(Status.PROFILE_NOT_FOUND, null, 0, 0, 0);
            }
            if (!profile.userId().equals(fromUserId)) {
                return new Result(Status.NOT_OWNER, null, 0, 0, 0);
            }
            if (profile.userId().equals(toUserId)) {
                return new Result(Status.SAME_OWNER, null, 0, 0, 0);
            }
            User target = findUser(toUserId);
            if (target == null) {
                return new Result(Status.TARGET_NOT_FOUND, null, 0, 0, 0);
            }
            
            if (!isAccessible(target)) {
                return new Result(Status.TARGET_INACCESSIBLE, null, 0, 0, 0);
            }

            String newKey = AuthService.generateYggdrasilToken();
            Jdbc.update(database,
                    "UPDATE player_profiles SET user_id = ?, owner_user_id = ?, yggdrasil_token = ? WHERE id = ?",
                    toUserId, toUserId, newKey, profile.id());

            String hexId = UuidUtil.toHex(profile.id());
            int revoked = Jdbc.update(database,
                    "DELETE FROM access_sessions WHERE profile_id = ? OR profile_id = ?", profile.id(), hexId);
            Jdbc.update(database,
                    "UPDATE users SET display_profile_id = NULL WHERE id = ?"
                            + " AND (display_profile_id = ? OR display_profile_id = ?)",
                    fromUserId, profile.id(), hexId);
            try {
                new ProfileCredentialService(database).rotate(profile.id(), newKey);
            } catch (RuntimeException ignored) {
                
            }
            Jdbc.update(database,
                    "UPDATE user_social_profiles SET display_profile_id = NULL"
                            + " WHERE user_id = ? AND (display_profile_id = ? OR display_profile_id = ?)",
                    fromUserId, profile.id(), hexId);
            try {
                new ProfileCredentialService(database).rotate(profile.id(), newKey);
            } catch (RuntimeException ignored) {
                
            }

            int kept = 0;
            int removed = 0;
            List<Long> bindingAssets = Jdbc.queryMany(database,
                    resultSet -> resultSet.getLong("asset_id"),
                    "SELECT asset_id FROM profile_texture_bindings WHERE profile_id = ?", profile.id());
            for (Long assetId : bindingAssets) {
                if (hasAccess(toUserId, assetId)) {
                    kept++;
                    continue;
                }
                Jdbc.update(database,
                        "DELETE FROM profile_texture_bindings WHERE profile_id = ? AND asset_id = ?",
                        profile.id(), assetId);
                removed++;
            }
            return new Result(Status.TRANSFERRED, newKey, revoked, kept, removed);
        });
    }

    private boolean hasAccess(String userId, long assetId) {
        return uploads.hasActiveUpload(userId, assetId)
                || publications.hasAvailableForAsset(assetId)
                || grants.hasActiveForRecipient(userId, assetId);
    }

    
    private boolean isAccessible(User user) {
        String stored = Jdbc.queryOne(database, resultSet -> resultSet.getString(1),
                "SELECT permissions FROM user_perm_group WHERE name = ?", user.getPermGroup());
        if (stored == null || stored.isBlank()) {
            return false;
        }
        for (String part : stored.split(",")) {
            String node = part.trim();
            if (UserPermissions.WILDCARD.equals(node) || UserPermissions.ACCESSIBLE.equals(node)) {
                return true;
            }
        }
        return false;
    }

    private ProfileRow findProfile(String profileId) {
        return Jdbc.queryOne(database, resultSet -> new ProfileRow(
                resultSet.getString("id"), resultSet.getString("user_id")),
                "SELECT id, user_id FROM player_profiles WHERE id = ? OR id = ?",
                profileId, UuidUtil.toHex(profileId));
    }

    private User findUser(String userId) {
        return Jdbc.queryOne(database, resultSet -> {
            User user = new User(
                    resultSet.getString("id"),
                    resultSet.getString("username"),
                    resultSet.getString("email"),
                    resultSet.getString("password_hash"),
                    resultSet.getString("nickname"),
                    resultSet.getInt("email_verified") == 1,
                    resultSet.getString("created_at"),
                    resultSet.getString("last_login"));
            user.setPermGroup(resultSet.getString("perm_group"));
            return user;
        }, "SELECT * FROM users WHERE id = ?", userId);
    }

    private record ProfileRow(String id, String userId) {
    }
}
