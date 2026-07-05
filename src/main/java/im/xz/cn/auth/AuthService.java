package im.xz.cn.auth;

import im.xz.cn.database.*;
import im.xz.cn.model.*;
import im.xz.cn.model.enums.UserRole;
import im.xz.cn.util.TimeUtil;
import im.xz.cn.util.UuidUtil;

import java.security.SecureRandom;
import java.util.UUID;

public class AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UserDao userDao;
    private final AdminDao adminDao;
    private final TokenDao tokenDao;
    private final ProfileDao profileDao;
    private final CacheDao cacheDao;

    public AuthService(DatabaseManager db) {
        this.userDao = new UserDao(db);
        this.adminDao = new AdminDao(db);
        this.tokenDao = new TokenDao(db);
        this.profileDao = new ProfileDao(db);
        this.cacheDao = new CacheDao(db);
    }

    public User authenticateUser(String usernameOrEmail, String password, String clientIp) {
        User user = userDao.findByUsernameOrEmail(usernameOrEmail);
        if (user == null) return null;
        if (user.getRole() == UserRole.BANNED) return null;
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
                UserRole.DEFAULT, false, createdAt, null, registeredIp);
        try {
            userDao.insert(user);
        } catch (Exception e) {
            System.err.println("registerUser failed: " + e.getMessage());
            return null;
        }
        return user;
    }

    public User registerUserWithHashedPassword(String username, String email, String passwordHash, String nickname, String registeredIp) {
        if (userDao.findByUsername(username) != null) return null;
        if (userDao.findByEmail(email) != null) return null;
        String id = UuidUtil.generateUserUuid();
        String createdAt = TimeUtil.now();
        User user = new User(id, username, email, passwordHash, nickname, UserRole.DEFAULT, false, createdAt, null, registeredIp);
        try {
            userDao.insert(user);
        } catch (Exception e) {
            System.err.println("registerUserWithHashedPassword failed: " + e.getMessage());
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
        String accessToken = generateSecureToken();
        if (clientToken == null || clientToken.isEmpty()) {
            clientToken = UUID.randomUUID().toString().replace("-", "");
        }
        String createdAt = TimeUtil.now();
        String expiresAt = TimeUtil.plusSeconds(7 * 24 * 3600L);

        AuthToken token = new AuthToken(accessToken, clientToken, userId, profileId,
                null, createdAt, expiresAt);
        tokenDao.insert(token);
        return token;
    }

    public AuthToken refreshToken(String oldAccessToken, String clientToken, String profileId) {
        AuthToken oldToken = tokenDao.findByAccessToken(oldAccessToken);
        if (oldToken == null) return null;

        if (clientToken != null && !clientToken.isEmpty()) {
            if (!clientToken.equals(oldToken.getClientToken())) return null;
        }

        tokenDao.deleteByAccessToken(oldAccessToken);

        return createToken(oldToken.getUserId(), oldToken.getClientToken(),
                profileId != null ? profileId : oldToken.getProfileId());
    }

    public boolean validateToken(String accessToken, String clientToken) {
        AuthToken token = tokenDao.findByAccessToken(accessToken);
        if (token == null) return false;
        if (token.isExpired()) {
            tokenDao.deleteByAccessToken(accessToken);
            return false;
        }
        if (clientToken == null || clientToken.isEmpty() ||
                !clientToken.equals(token.getClientToken())) {
            return false;
        }
        return true;
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

    public PlayerProfile hasJoinedServer(String username, String serverId) {
        PlayerProfile profile = profileDao.findByName(username);
        if (profile == null) return null;

        var tokens = tokenDao.findByUserId(profile.getUserId());
        boolean found = false;
        for (AuthToken token : tokens) {
            if (serverId.equals(token.getServerId()) && !token.isExpired()) {
                found = true;
                tokenDao.updateServerId(token.getAccessToken(), null);
                break;
            }
        }

        return found ? profile : null;
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
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}
