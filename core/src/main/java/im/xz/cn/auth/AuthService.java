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
package im.xz.cn.auth;

import im.xz.cn.database.dao.*;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.model.*;
import im.xz.cn.common.TimeUtil;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.logging.logApi;

import java.security.SecureRandom;
import java.util.UUID;

public class AuthService {
    private static final logApi log = logApi.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UserDao userDao;
    private final AdminDao adminDao;
    private final TokenDao tokenDao;
    private final ProfileDao profileDao;
    private final CacheDao cacheDao;
    private final UserPermGroupDao userPermGroupDao;
    private final DatabaseManager databaseManager;

    public AuthService(DatabaseManager db) {
        this.databaseManager = db;
        this.userDao = new UserDao(db);
        this.adminDao = new AdminDao(db);
        this.tokenDao = new TokenDao(db);
        this.profileDao = new ProfileDao(db);
        this.cacheDao = new CacheDao(db);
        this.userPermGroupDao = new UserPermGroupDao(db);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UserPermGroupDao getUserPermGroupDao() {
        return userPermGroupDao;
    }

    public boolean isUserAccessible(User user) {
        return im.xz.cn.security.UserPermissions.isAccessible(userPermGroupDao, user);
    }

    public User authenticateUser(String usernameOrEmail, String password, String clientIp) {
        User user = userDao.findByUsernameOrEmail(usernameOrEmail);
        if (user == null) return null;
        if (!Argon2Hasher.verify(password, user.getPasswordHash())) return null;

        try {
            if (Argon2Hasher.needsRehash(user.getPasswordHash())) {
                String newHash = Argon2Hasher.hash(password);
                userDao.updatePassword(user.getId(), newHash);
            }
        } catch (Exception ignored) {

        }

        userDao.updateLastLoginWithIp(user.getId(), TimeUtil.now(), clientIp);
        user.setLastLogin(TimeUtil.now());
        user.setLastLoginIp(clientIp);

        if (user.getRegisteredIp() == null || user.getRegisteredIp().isEmpty()) {
            userDao.updateRegisteredIp(user.getId(), clientIp);
            user.setRegisteredIp(clientIp);
        }

        return user;
    }

    public User registerUser(String username, String email, String password, String nickname, String registeredIp) {
        if (userDao.findByUsername(username) != null) return null;
        if (userDao.findByEmail(email) != null) return null;

        String id = UuidUtil.generateUserUuid();
        String passwordHash = Argon2Hasher.hash(password);
        String createdAt = TimeUtil.now();

        User user = new User(id, username, email, passwordHash, nickname,
                false, createdAt, null, registeredIp);
        try {
            userDao.insert(user);
        } catch (Exception e) {
            log.error("registerUser failed: {}", e.getMessage(), e);
            return null;
        }
        return user;
    }

    public User registerUserWithHashedPassword(String username, String email, String passwordHash, String nickname, String registeredIp) {
        if (userDao.findByUsername(username) != null) return null;
        if (userDao.findByEmail(email) != null) return null;
        String id = UuidUtil.generateUserUuid();
        String createdAt = TimeUtil.now();
        User user = new User(id, username, email, passwordHash, nickname, false, createdAt, null, registeredIp);
        try {
            userDao.insert(user);
        } catch (Exception e) {
            log.error("registerUserWithHashedPassword failed: {}", e.getMessage(), e);
            return null;
        }
        return user;
    }

    public Admin authenticateAdmin(String username, String password) {
        Admin admin = adminDao.findByUsername(username);
        if (admin == null) return null;
        if (!Argon2Hasher.verify(password, admin.getPasswordHash())) return null;

        try {
            if (Argon2Hasher.needsRehash(admin.getPasswordHash())) {
                String newHash = Argon2Hasher.hash(password);
                adminDao.updatePassword(admin.getId(), newHash);
            }
        } catch (Exception ignored) {
        }

        return admin;
    }

    public AuthToken createToken(String userId, String clientToken, String profileId) {
        return createToken(userId, clientToken, profileId, null);
    }

    public AuthToken createToken(String userId, String clientToken, String profileId, String authSource) {
        String accessToken = generateSecureToken();
        if (clientToken == null || clientToken.isEmpty()) {
            clientToken = UUID.randomUUID().toString().replace("-", "");
        }
        String createdAt = TimeUtil.now();
        
        String expiresAt = TimeUtil.plusSeconds(resolveAccessTokenTtlSeconds(profileId != null));

        AuthToken token = new AuthToken(accessToken, clientToken, userId, profileId,
                null, createdAt, expiresAt, authSource);
        enforceSessionLimit(userId, profileId);
        tokenDao.insert(token);
        return token;
    }

    
    private void enforceSessionLimit(String userId, String profileId) {
        int limit = SystemConfig.getInstance().getMaxTokensPerProfile();
        if (limit <= 0 || userId == null) {
            return;
        }
        java.util.List<AuthToken> existing = tokenDao.findSessionsInScope(userId, profileId);
        int excess = existing.size() - limit + 1;
        for (int i = existing.size() - 1; i >= 0 && excess > 0; i--, excess--) {
            tokenDao.deleteByAccessToken(existing.get(i).getAccessToken());
        }
    }

    
    public static long resolveAccessTokenTtlSeconds(boolean boundToProfile) {
        SystemConfig config = SystemConfig.getInstance();
        int minutes = boundToProfile ? config.getTokenPermanentExpiry() : config.getTokenTempExpiry();
        if (minutes <= 0) {
            minutes = boundToProfile ? 10080 : 4320;
        }
        return Math.max(60L, minutes) * 60L;
    }

    
    public AuthToken refreshToken(String oldAccessToken, String clientToken, String profileId) {
        if (oldAccessToken == null) return null;
        AuthToken oldToken = tokenDao.findByAccessToken(oldAccessToken);
        if (oldToken == null || oldToken.isExpired()) return null;

        if (clientToken != null && !clientToken.isEmpty()
                && !clientToken.equals(oldToken.getClientToken())) {
            return null;
        }

        String boundProfileId = oldToken.getProfileId();
        if (boundProfileId != null && !boundProfileId.isBlank()
                && profileId != null && !boundProfileId.equals(profileId)) {
            return null;
        }

        User user = userDao.findById(oldToken.getUserId());
        if (user == null || !isUserAccessible(user) || !isAuthSourceAllowed(oldToken)) {
            return null;
        }

        String targetProfileId = profileId != null
                ? profileId
                : (boundProfileId == null || boundProfileId.isBlank() ? null : boundProfileId);
        AuthToken newToken = createToken(oldToken.getUserId(), oldToken.getClientToken(),
                targetProfileId, oldToken.getAuthSource());
        tokenDao.deleteByAccessToken(oldAccessToken);
        return newToken;
    }

    
    public boolean validateToken(String accessToken, String clientToken) {
        if (accessToken == null) return false;
        AuthToken token = tokenDao.findByAccessToken(accessToken);
        if (token == null) return false;
        if (token.isExpired()) {
            tokenDao.deleteByAccessToken(accessToken);
            return false;
        }
        User user = userDao.findById(token.getUserId());
        if (user == null || !isUserAccessible(user) || !isAuthSourceAllowed(token)) {
            tokenDao.deleteByAccessToken(accessToken);
            return false;
        }
        if (clientToken == null || clientToken.isEmpty()) {
            return true;
        }
        return clientToken.equals(token.getClientToken());
    }

    
    public boolean isAuthSourceAllowed(AuthToken token) {
        if (token == null || token.getAuthSource() == null) {
            return true;
        }
        if (!"ACCOUNT_PASSWORD".equalsIgnoreCase(token.getAuthSource())) {
            return true;
        }
        return new im.xz.cn.identity.UserPreferenceRepository(getDatabaseManager())
                .isAccountPasswordLoginAllowed(token.getUserId());
    }

    public void invalidateToken(String accessToken) {
        tokenDao.deleteByAccessToken(accessToken);
    }

    public void invalidateAllUserTokens(String userId) {
        tokenDao.deleteByUserId(userId);
    }

    public boolean joinServer(String accessToken, String profileId, String serverId) {
        AuthToken token = tokenDao.findByAccessToken(accessToken);
        if (token == null) return false;
        if (token.isExpired()) {
            tokenDao.deleteByAccessToken(accessToken);
            return false;
        }
        if (token.getProfileId() != null) {
            if (!token.getProfileId().equals(profileId)) {
                return false;
            }
        } else {
            PlayerProfile profile = profileDao.findById(profileId);
            if (profile == null || !profile.getUserId().equals(token.getUserId())) {
                return false;
            }
        }
        tokenDao.updateServerId(accessToken, serverId);
        return true;
    }

    
    public PlayerProfile hasJoinedServer(String username, String serverId, String ip) {
        if (username == null || serverId == null) {
            return null;
        }
        PlayerProfile profile = profileDao.findByName(username);
        if (profile == null) {
            log.debug("[AuthService] hasJoinedServer: username={} -> profile未找到", username);
            return null;
        }
        String profileId = UuidUtil.toHex(profile.getId());
        TokenDao.JoinTicketRow ticket = tokenDao.findJoinTicket(serverId, profileId);
        if (ticket == null) {
            log.debug("[AuthService] hasJoinedServer: 无匹配 join ticket, serverId={}, profileId={}",
                    serverId, profileId);
            return null;
        }
        if (ticket.userId() == null || !ticket.userId().equals(profile.getUserId())) {
            log.debug("[AuthService] hasJoinedServer: ticket 归属不匹配, profileId={}", profileId);
            return null;
        }
        if (ticket.expiresAt() == null || ticket.expiresAt().compareTo(TimeUtil.now()) <= 0) {
            log.debug("[AuthService] hasJoinedServer: join ticket 已过期, profileId={}", profileId);
            return null;
        }
        if (ip != null && !ip.isBlank() && ticket.ip() != null && !ticket.ip().isBlank()
                && !ip.equals(ticket.ip())) {
            log.debug("[AuthService] hasJoinedServer: join ticket IP 不匹配, profileId={}", profileId);
            return null;
        }
        AuthToken session = tokenDao.findByAccessToken(ticket.accessToken());
        if (session == null || session.isExpired() || !isAuthSourceAllowed(session)) {
            log.debug("[AuthService] hasJoinedServer: access session 无效, profileId={}", profileId);
            return null;
        }
        log.debug("[AuthService] hasJoinedServer: 匹配成功, profileId={}", profileId);
        return profile;
    }

    public UserDao getUserDao() { return userDao; }
    public AdminDao getAdminDao() { return adminDao; }
    public TokenDao getTokenDao() { return tokenDao; }
    public ProfileDao getProfileDao() { return profileDao; }
    public CacheDao getCacheDao() { return cacheDao; }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String generateYggdrasilToken() {
        return generateYggdrasilToken(SystemConfig.getInstance().getWorldTreeKeyLength());
    }

    
    public static String generateYggdrasilToken(int length) {
        int safeLength = SystemConfig.clampWorldTreeKeyLength(length);
        StringBuilder sb = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            sb.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}
