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
package im.xz.cn.infrastructure.persistence;

import im.xz.cn.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


public final class Jdbc {

    private Jdbc() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    public static int update(DatabaseManager database, String sql, Object... params) {
        Connection connection = acquire(database);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException error) {
            throw new DataAccessException(sql, error);
        } finally {
            release(connection);
        }
    }

    
    public static long insert(DatabaseManager database, String sql, Object... params) {
        Connection connection = acquire(database);
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DataAccessException(sql, new SQLException("no generated key returned"));
        } catch (SQLException error) {
            throw new DataAccessException(sql, error);
        } finally {
            release(connection);
        }
    }

    public static <T> T queryOne(DatabaseManager database, RowMapper<T> mapper, String sql, Object... params) {
        List<T> rows = queryMany(database, mapper, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static <T> List<T> queryMany(DatabaseManager database, RowMapper<T> mapper, String sql, Object... params) {
        Connection connection = acquire(database);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException error) {
            throw new DataAccessException(sql, error);
        } finally {
            release(connection);
        }
    }

    public static long queryCount(DatabaseManager database, String sql, Object... params) {
        Long count = queryOne(database, rs -> rs.getLong(1), sql, params);
        return count == null ? 0L : count;
    }

    public static Instant readInstant(ResultSet resultSet, String column) throws SQLException {
        String raw = resultSet.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseInstant(raw.trim());
    }

    
    public static Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw).truncatedTo(ChronoUnit.MILLIS);
        } catch (DateTimeParseException ignored) {
            
        }
        String value = raw.replace('T', ' ');
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1);
        }
        for (DateTimeFormatter formatter : SQL_DATETIME_FORMATS) {
            try {
                return LocalDateTime.parse(value, formatter).toInstant(java.time.ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                
            }
        }
        throw new DateTimeParseException("unsupported instant format", raw, 0);
    }

    
    public static String writeInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return SQL_DATETIME_FORMAT.format(instant.truncatedTo(ChronoUnit.MILLIS));
    }

    private static final DateTimeFormatter SQL_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(java.time.ZoneOffset.UTC);
    private static final DateTimeFormatter[] SQL_DATETIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private static Connection acquire(DatabaseManager database) {
        Connection inTransaction = UnitOfWork.currentConnection();
        if (inTransaction != null) {
            return inTransaction;
        }
        try {
            return database.getConnection();
        } catch (SQLException error) {
            throw new DataAccessException("acquire connection", error);
        }
    }

    private static void release(Connection connection) {
        if (connection == UnitOfWork.currentConnection()) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    
    public static final class DataAccessException extends RuntimeException {
        private final String sql;

        public DataAccessException(String sql, Throwable cause) {
            super(cause.getMessage(), cause);
            this.sql = sql;
        }

        public String sql() {
            return sql;
        }
    }
}
