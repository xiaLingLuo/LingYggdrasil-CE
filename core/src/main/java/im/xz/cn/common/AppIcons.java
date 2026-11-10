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
package im.xz.cn.common;

import java.io.File;
import java.io.InputStream;

public final class AppIcons {

    public static final String EXTERNAL_DIR = "icons";
    public static final String DEFAULT_ICON = "app.ico";

    private static volatile boolean externalAvailable = false;

    private AppIcons() {}

    public static void init() {
        externalAvailable = new File(EXTERNAL_DIR).isDirectory();
    }

    public static boolean hasExternal() {
        return externalAvailable;
    }

    public static File externalFile(String name) {
        if (!isSafeName(name)) return null;
        File file = new File(EXTERNAL_DIR, name);
        return file.isFile() ? file : null;
    }

    public static InputStream builtinStream(String name) {
        if (!isSafeName(name)) return null;
        return AppIcons.class.getClassLoader().getResourceAsStream("icons/" + name);
    }

    private static boolean isSafeName(String name) {
        if (name == null || name.isEmpty() || name.contains("..")) return false;
        return name.matches("[A-Za-z0-9._-]+");
    }
}
