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
package im.xz.cn.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class logApi {
    private final Logger logger;
    private final String name;

    private logApi(Class<?> type) {
        this.logger = LoggerFactory.getLogger(type);
        this.name = type.getName();
    }

    public static logApi getLogger(Class<?> type) {
        return new logApi(type);
    }

    public void trace(String message, Object... arguments) {
        if (logger.isTraceEnabled()) {
            logger.trace(message, arguments);
            ServiceLog.writeCurrent("TRACE", name, LogFormat.format(message, arguments));
        }
    }

    public void debug(String message, Object... arguments) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, arguments);
            ServiceLog.writeCurrent("DEBUG", name, LogFormat.format(message, arguments));
        }
    }

    public void info(String message, Object... arguments) {
        if (logger.isInfoEnabled()) {
            logger.info(message, arguments);
            ServiceLog.writeCurrent("INFO", name, LogFormat.format(message, arguments));
        }
    }

    public void warn(String message, Object... arguments) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, arguments);
            ServiceLog.writeCurrent("WARN", name, LogFormat.format(message, arguments));
        }
    }

    public void error(String message, Object... arguments) {
        if (logger.isErrorEnabled()) {
            logger.error(message, arguments);
            ServiceLog.writeCurrent("ERROR", name, LogFormat.format(message, arguments));
        }
    }
}
