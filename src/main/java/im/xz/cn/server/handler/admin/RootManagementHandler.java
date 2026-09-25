package im.xz.cn.server.handler.admin;

import im.xz.cn.auth.Argon2Hasher;
import im.xz.cn.auth.SessionManager;
import im.xz.cn.common.IpUtil;
import im.xz.cn.database.DatabaseManager;
import im.xz.cn.logging.AuditLogger;
import io.javalin.http.Context;

import java.util.Map;
import java.util.regex.Pattern;

public final class RootManagementHandler {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,128}$");

    private final DatabaseManager db;

    public RootManagementHandler(DatabaseManager db) {
        this.db = db;
    }

    public void getSettings(Context ctx) {
        String rootId = requireCurrentRoot(ctx);
        if (rootId == null) return;
        var root = db.executeQuerySingle("SELECT username FROM root_info WHERE id = ?", rootId);
        if (root == null) {
            rejectInvalidRootSession(ctx);
            return;
        }
        ctx.json(Map.of("username", String.valueOf(root.get("username"))));
    }

    @SuppressWarnings("unchecked")
    public void updateSettings(Context ctx) {
        String rootId = requireCurrentRoot(ctx);
        if (rootId == null) return;

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (body == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        String currentPassword = stringValue(body.get("currentPassword"));
        String newUsername = stringValue(body.get("newUsername"));
        String newPassword = stringValue(body.get("newPassword"));
        String confirmPassword = stringValue(body.get("confirmPassword"));
        if (currentPassword == null || currentPassword.isEmpty()
                || newUsername == null || newPassword == null || confirmPassword == null) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.paramsMissing")));
            return;
        }

        boolean updatePassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();
        if (updatePassword) {
            String passwordError = validatePassword(newPassword);
            if (passwordError != null) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t(passwordError)));
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.passwordMismatch")));
                return;
            }
        }

        var root = db.executeQuerySingle(
                "SELECT username, password_hash FROM root_info WHERE id = ?", rootId);
        if (root == null) {
            rejectInvalidRootSession(ctx);
            return;
        }
        String currentUsername = String.valueOf(root.get("username"));
        String currentHash = String.valueOf(root.get("password_hash"));
        if (!Argon2Hasher.verify(currentPassword, currentHash)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.usernamePasswordWrong")));
            return;
        }

        boolean updateUsername = !currentUsername.equals(newUsername);
        if (updateUsername && (newUsername.length() < 3 || newUsername.length() > 32)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameLength")));
            return;
        }
        if (updateUsername && !USERNAME_PATTERN.matcher(newUsername).matches()) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameCharset")));
            return;
        }
        if (!updateUsername && !updatePassword) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.security.rootNoChanges")));
            return;
        }
        if (updateUsername) {
            var existingAdmin = db.executeQuerySingle(
                    "SELECT id FROM admins WHERE LOWER(username) = LOWER(?)", newUsername);
            if (existingAdmin != null) {
                ctx.status(400).json(Map.of("success", false, "message", I18n.t("msg.usernameInUse")));
                return;
            }
        }
        if (updatePassword && newPassword.equals(currentPassword)) {
            ctx.status(400).json(Map.of("success", false, "message", I18n.t("admin.security.rootNoChanges")));
            return;
        }

        String newHash = updatePassword ? Argon2Hasher.hash(newPassword) : currentHash;
        int updated = db.executeUpdate(
                "UPDATE root_info SET username = ?, password_hash = ? WHERE id = ? AND username = ? AND password_hash = ?",
                newUsername, newHash, rootId, currentUsername, currentHash);
        if (updated != 1) {
            ctx.status(409).json(Map.of("success", false, "message", I18n.t("admin.security.rootUpdateConflict")));
            return;
        }

        ctx.sessionAttribute("adminUsername", newUsername);
        AuditLogger.logSensitiveOperation(
                newUsername,
                "ROOT_CREDENTIAL_UPDATE:" + (updateUsername ? "USERNAME" : "") + (updatePassword ? ":PASSWORD" : ""),
                IpUtil.getClientIp(ctx));
        ctx.json(Map.of("success", true, "message", I18n.t("msg.saveSuccess"), "username", newUsername));
    }

    private String requireCurrentRoot(Context ctx) {
        if (!SessionManager.isAdminRoot(ctx)) {
            ctx.status(403).json(Map.of("success", false, "message", I18n.t("msg.rootOnly")));
            return null;
        }
        String rootId = SessionManager.getAdminId(ctx);
        if (rootId == null || db.executeQuerySingle("SELECT id FROM root_info WHERE id = ?", rootId) == null) {
            rejectInvalidRootSession(ctx);
            return null;
        }
        return rootId;
    }

    private void rejectInvalidRootSession(Context ctx) {
        SessionManager.invalidateAdmin(ctx);
        ctx.status(401).json(Map.of("success", false, "message", I18n.t("msg.accountInvalid")));
    }

    private static String stringValue(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static String validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            return "admin.users.passwordWeak";
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) return "admin.users.passwordWeak";
        return null;
    }
}
