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

import im.xz.cn.auth.AuthService;
import im.xz.cn.model.PlayerProfile;
import im.xz.cn.model.User;

import java.util.List;


public final class AccountPasswordLoginService {

    public enum Status {
        
        OK,
        
        INVALID_CREDENTIALS,
        
        DISABLED,
        
        ACCOUNT_INACCESSIBLE,
        
        EMAIL_UNVERIFIED,
        
        NO_PROFILES
    }

    public record Result(Status status, User user, List<PlayerProfile> profiles) {
        public Result {
            profiles = profiles == null ? List.of() : List.copyOf(profiles);
        }

        public boolean ok() {
            return status == Status.OK;
        }
    }

    private final AuthService authService;
    private final UserPreferenceRepository preferences;
    private final boolean requireVerifiedEmail;

    public AccountPasswordLoginService(AuthService authService,
                                       UserPreferenceRepository preferences,
                                       boolean requireVerifiedEmail) {
        this.authService = authService;
        this.preferences = preferences;
        this.requireVerifiedEmail = requireVerifiedEmail;
    }

    public Result login(String identifier, String password, String clientIp) {
        if (identifier == null || identifier.isBlank() || password == null || password.isEmpty()) {
            return new Result(Status.INVALID_CREDENTIALS, null, List.of());
        }
        User user = authService.authenticateUser(identifier.trim(), password, clientIp);
        if (user == null) {
            return new Result(Status.INVALID_CREDENTIALS, null, List.of());
        }
        if (!preferences.isAccountPasswordLoginAllowed(user.getId())) {
            return new Result(Status.DISABLED, user, List.of());
        }
        if (!authService.isUserAccessible(user)) {
            return new Result(Status.ACCOUNT_INACCESSIBLE, user, List.of());
        }
        if (requireVerifiedEmail && !user.isEmailVerified()) {
            return new Result(Status.EMAIL_UNVERIFIED, user, List.of());
        }
        List<PlayerProfile> profiles = authService.getProfileDao().findByUserId(user.getId());
        if (profiles.isEmpty()) {
            return new Result(Status.NO_PROFILES, user, profiles);
        }
        return new Result(Status.OK, user, profiles);
    }
}
