/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package im.xz.cn.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class logApi {
    private final Logger logger;

    private logApi(Class<?> type) {
        this.logger = LoggerFactory.getLogger(type);
    }

    public static logApi getLogger(Class<?> type) {
        return new logApi(type);
    }

    public void trace(String message, Object... arguments) { logger.trace(message, arguments); }
    public void debug(String message, Object... arguments) { logger.debug(message, arguments); }
    public void info(String message, Object... arguments) { logger.info(message, arguments); }
    public void warn(String message, Object... arguments) { logger.warn(message, arguments); }
    public void error(String message, Object... arguments) { logger.error(message, arguments); }
}
