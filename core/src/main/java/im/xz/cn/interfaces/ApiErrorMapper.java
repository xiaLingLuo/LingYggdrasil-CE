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
package im.xz.cn.interfaces;

import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.settings.RevisionConflictException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;


public final class ApiErrorMapper {

    private ApiErrorMapper() {
    }

    public record ApiError(ApiErrorCode code, String message) {
        public Map<String, Object> toMap() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("code", code.name());
            body.put("message", message);
            return body;
        }
    }

    public static ApiError map(Throwable error) {
        if (error == null) {
            return new ApiError(ApiErrorCode.INTERNAL, "Internal server error");
        }
        if (error instanceof Jdbc.DataAccessException) {
            return new ApiError(ApiErrorCode.INTERNAL, "Internal server error");
        }
        if (error instanceof RevisionConflictException) {
            return new ApiError(ApiErrorCode.REVISION_CONFLICT,
                    "Settings were modified concurrently; reload and retry");
        }
        if (error instanceof IllegalArgumentException) {
            return new ApiError(ApiErrorCode.INVALID_ARGUMENT,
                    textOr(error, "Invalid argument"));
        }
        if (error instanceof SecurityException) {
            return new ApiError(ApiErrorCode.FORBIDDEN, "Forbidden");
        }
        if (error instanceof NoSuchElementException) {
            return new ApiError(ApiErrorCode.NOT_FOUND, "Not found");
        }
        if (error instanceof IllegalStateException) {
            return new ApiError(ApiErrorCode.CONFLICT, textOr(error, "State conflict"));
        }
        return new ApiError(ApiErrorCode.INTERNAL, "Internal server error");
    }

    private static String textOr(Throwable error, String fallback) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}
