/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package im.xz.cn.yggdrasil;

import java.util.regex.Pattern;

public final class YggdrasilLoginingDelAts {
    private static final Pattern EMAIL_STYLE_NAME = Pattern.compile(
            "^[^\\s@]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,}$");

    private YggdrasilLoginingDelAts() {
    }

    public static String normalize(String value) {
        if (value == null) return null;

        String normalized = value.trim();
        if (normalized.isEmpty()) return null;

        int at = normalized.indexOf('@');
        if (at < 0) return normalized;
        if (at != normalized.lastIndexOf('@') || !EMAIL_STYLE_NAME.matcher(normalized).matches()) {
            return null;
        }

        String profileName = normalized.substring(0, at);
        return profileName.isEmpty() ? null : profileName;
    }
}
