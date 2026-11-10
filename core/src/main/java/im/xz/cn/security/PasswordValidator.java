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
package im.xz.cn.security;

import im.xz.cn.i18n.I18n;

import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 12;
    private static final String ALLOWED_SPECIAL = "@$!%*?&";
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    private static final Pattern INVALID_CHAR_PATTERN = 
        Pattern.compile("[^A-Za-z\\d@$!%*?&]");

    private static final int USER_MIN_LENGTH = 6;
    private static final Pattern USER_PASSWORD_PATTERN =
        Pattern.compile("^[A-Za-z\\d@$!%*?&]{6,}$");

    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return I18n.t("password.empty");
        }
        if (password.length() < MIN_LENGTH) {
            return I18n.t("password.minLength", MIN_LENGTH);
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            if (INVALID_CHAR_PATTERN.matcher(password).find()) {
                return I18n.t("password.invalidChars", ALLOWED_SPECIAL);
            }
            return I18n.t("password.composition", ALLOWED_SPECIAL);
        }
        return null;
    }

    public static boolean isValid(String password) {
        return validate(password) == null;
    }

    public static String validateUser(String password) {
        if (password == null || password.isEmpty()) {
            return I18n.t("password.empty");
        }
        if (password.length() < USER_MIN_LENGTH) {
            return I18n.t("password.minLength", USER_MIN_LENGTH);
        }
        if (!USER_PASSWORD_PATTERN.matcher(password).matches()) {
            return I18n.t("password.userChars", ALLOWED_SPECIAL);
        }
        return null;
    }

    public static int getMinLength() {
        return MIN_LENGTH;
    }

    public static int getUserMinLength() {
        return USER_MIN_LENGTH;
    }
}
