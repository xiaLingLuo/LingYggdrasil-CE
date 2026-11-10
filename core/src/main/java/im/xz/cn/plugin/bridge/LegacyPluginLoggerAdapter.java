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
package im.xz.cn.plugin.bridge;

import im.xz.cn.plugin.api.PluginLogger;


public final class LegacyPluginLoggerAdapter implements PluginLogger {

    private final im.xz.cn.plugin.spi.PluginLogger delegate;

    public LegacyPluginLoggerAdapter(im.xz.cn.plugin.spi.PluginLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void debug(String message, Object... arguments) {
        if (delegate != null) {
            delegate.info("[legacy-debug] " + message, arguments);
        }
    }

    @Override
    public void info(String message, Object... arguments) {
        if (delegate != null) {
            delegate.info(message, arguments);
        }
    }

    @Override
    public void warn(String message, Object... arguments) {
        if (delegate != null) {
            delegate.warn(message, arguments);
        }
    }

    @Override
    public void error(String message, Object... arguments) {
        if (delegate != null) {
            delegate.error(message, arguments);
        }
    }
}
