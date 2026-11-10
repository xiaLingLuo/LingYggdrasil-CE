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
package im.xz.cn.plugin.runtime;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;


final class PluginClassLoader extends URLClassLoader {

    private static final String[] SHARED_PREFIXES = {
            "java.", "javax.", "jdk.", "sun.", "com.sun.",
            "im.xz.cn.plugin.spi."
    };

    PluginClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[]{jarUrl}, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isShared(name)) {
            return super.loadClass(name, resolve);
        }
        
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException notInPlugin) {
                    throw new ClassNotFoundException("plugin class not found: " + name
                            + " (only JDK and im.xz.cn.plugin.spi are visible)");
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private static boolean isShared(String name) {
        for (String prefix : SHARED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
