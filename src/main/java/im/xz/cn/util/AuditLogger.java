package im.xz.cn.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private AuditLogger() {
    }

    public static void logLogin(String user, String ip, boolean success) {
        if (success) {
            logger.info("[AUDIT] LOGIN_SUCCESS user={} ip={}", user, ip);
        } else {
            logger.warn("[AUDIT] LOGIN_FAILURE user={} ip={}", user, ip);
        }
    }

    public static void logLogout(String user, String ip) {
        logger.info("[AUDIT] LOGOUT user={} ip={}", user, ip);
    }

    public static void logPasswordChange(String user, String ip) {
        logger.info("[AUDIT] PASSWORD_CHANGE user={} ip={}", user, ip);
    }

    public static void logPermissionChange(String admin, String target, String action, String ip) {
        logger.info("[AUDIT] PERMISSION_CHANGE admin={} target={} action={} ip={}", admin, target, action, ip);
    }

    public static void logSensitiveOperation(String user, String action, String ip) {
        logger.info("[AUDIT] SENSITIVE_OPERATION user={} action={} ip={}", user, action, ip);
    }
}
