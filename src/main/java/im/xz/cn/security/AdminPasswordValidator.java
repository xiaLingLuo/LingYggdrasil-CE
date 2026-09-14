package im.xz.cn.security;

import im.xz.cn.i18n.AdminI18n;

import java.util.regex.Pattern;

public final class AdminPasswordValidator {
    private static final int MIN_LENGTH = 12;
    private static final String ALLOWED_SPECIAL = "@$!%*?&";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[^A-Za-z\\d@$!%*?&]");
    private static final int USER_MIN_LENGTH = 6;
    private static final Pattern USER_PASSWORD_PATTERN = Pattern.compile("^[A-Za-z\\d@$!%*?&]{6,}$");

    private AdminPasswordValidator() {}

    public static String validate(String password) {
        if (password == null || password.isEmpty()) return AdminI18n.t("password.empty");
        if (password.length() < MIN_LENGTH) return AdminI18n.t("password.minLength", MIN_LENGTH);
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            if (INVALID_CHAR_PATTERN.matcher(password).find()) return AdminI18n.t("password.invalidChars", ALLOWED_SPECIAL);
            return AdminI18n.t("password.composition", ALLOWED_SPECIAL);
        }
        return null;
    }

    public static String validateUser(String password) {
        if (password == null || password.isEmpty()) return AdminI18n.t("password.empty");
        if (password.length() < USER_MIN_LENGTH) return AdminI18n.t("password.minLength", USER_MIN_LENGTH);
        if (!USER_PASSWORD_PATTERN.matcher(password).matches()) return AdminI18n.t("password.userChars", ALLOWED_SPECIAL);
        return null;
    }
}
