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

import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.AuthService;
import im.xz.cn.model.User;
import im.xz.cn.security.UserPermissions;


public final class AccountCancellationService {

    
    public static final String GROUP_CANCELLED = "cancelled";

    public enum Status {
        CANCELLED,
        ALREADY_CANCELLED,
        INVALID_PASSWORD,
        USER_NOT_FOUND,
        CHALLENGE_REQUIRED,
        CHALLENGE_INVALID
    }

    private final AuthService authService;

    public AccountCancellationService(AuthService authService) {
        this.authService = authService;
    }

    public Status cancel(String userId, String password) {
        return cancel(userId, password, null, false);
    }

    
    public Status cancel(String userId, String password, String challengeToken) {
        return cancel(userId, password, challengeToken, true);
    }

    private Status cancel(String userId, String password, String challengeToken, boolean requireChallenge) {
        if (userId == null || userId.isBlank() || password == null || password.isEmpty()) {
            return Status.INVALID_PASSWORD;
        }
        User user = authService.getUserDao().findById(userId);
        if (user == null) {
            return Status.USER_NOT_FOUND;
        }
        if (isCancelled(user)) {
            return Status.ALREADY_CANCELLED;
        }
        boolean passwordValid;
        try {
            passwordValid = Argon2Hasher.verify(password, user.getPasswordHash());
        } catch (RuntimeException error) {
            passwordValid = false;
        }
        if (!passwordValid) {
            return Status.INVALID_PASSWORD;
        }
        if (requireChallenge) {
            if (challengeToken == null || challengeToken.isBlank()) {
                return Status.CHALLENGE_REQUIRED;
            }
            boolean consumed = new VerificationChallengeService(
                    authService.getDatabaseManager())
                    .consume("ACCOUNT_CANCEL", userId, challengeToken);
            if (!consumed) {
                return Status.CHALLENGE_INVALID;
            }
        }
        authService.getUserDao().updatePermGroup(userId, GROUP_CANCELLED);
        return Status.CANCELLED;
    }

    public boolean isCancelled(User user) {
        return UserPermissions.isCancelled(authService.getUserPermGroupDao(), user);
    }

    
    public static String displayName(User user, boolean cancelled) {
        if (user == null) {
            return "";
        }
        if (cancelled) {
            return "账号已注销";
        }
        String nickname = user.getNickname();
        return nickname == null || nickname.isBlank() ? user.getUsername() : nickname;
    }
}
