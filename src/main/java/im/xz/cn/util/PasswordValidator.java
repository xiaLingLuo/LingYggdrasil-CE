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
package im.xz.cn.util;

import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 12;
    private static final String ALLOWED_SPECIAL = "@$!%*?&";
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    private static final Pattern INVALID_CHAR_PATTERN = 
        Pattern.compile("[^A-Za-z\\d@$!%*?&]");

    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < MIN_LENGTH) {
            return "密码长度至少" + MIN_LENGTH + "位";
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            if (INVALID_CHAR_PATTERN.matcher(password).find()) {
                return "密码含有不允许的字符，仅可使用以下特殊字符：" + ALLOWED_SPECIAL;
            }
            return "密码必须包含：大写字母、小写字母、数字，以及至少一个特殊字符（" + ALLOWED_SPECIAL + "）";
        }
        return null;
    }

    public static boolean isValid(String password) {
        return validate(password) == null;
    }

    public static int getMinLength() {
        return MIN_LENGTH;
    }
}
