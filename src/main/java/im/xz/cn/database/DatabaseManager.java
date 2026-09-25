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
package im.xz.cn.database;

import im.xz.cn.logging.logApi;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import im.xz.cn.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final logApi log = logApi.getLogger(DatabaseManager.class);
    private final HikariDataSource dataSource;
    private final String dbType;

    public DatabaseManager(DatabaseConfig config) {
        this.dbType = config.getType().toLowerCase();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        if ("sqlite".equals(dbType)) {
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setConnectionInitSql("PRAGMA foreign_keys=ON");
        } else {
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
        }

        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public String getDbType() {
        return dbType;
    }

    public static boolean isDuplicateKeyViolation(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                String state = sqlException.getSQLState();
                String message = sqlException.getMessage() == null
                        ? "" : sqlException.getMessage().toLowerCase(java.util.Locale.ROOT);
                if ("23505".equals(state)
                        || sqlException.getErrorCode() == 1062
                        || (sqlException.getErrorCode() == 19
                        && (message.contains("unique constraint") || message.contains("primary key constraint")))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDuplicateIndexName(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 1061 || "42S11".equals(sqlException.getSQLState()))) {
                return true;
            }
        }
        return false;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public int executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Database executeUpdate failed: {} | SQL: {}", e.getMessage(), sql, e);
            throw new RuntimeException("Database update failed: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        row.put(colName, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Database executeQuery failed: {} | SQL: {}", e.getMessage(), sql, e);
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        }
        return results;
    }

    public Map<String, Object> executeQuerySingle(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        row.put(colName, rs.getObject(i));
                    }
                    return row;
                }
            }
        } catch (SQLException e) {
            log.error("Database executeQuerySingle failed: {} | SQL: {}", e.getMessage(), sql, e);
            throw new RuntimeException("Database query failed: " + e.getMessage(), e);
        }
        return null;
    }

    @Deprecated
    void executeRaw(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("Database executeRaw failed: {} | SQL: {}", e.getMessage(), sql, e);
            throw new RuntimeException("Database executeRaw failed: " + e.getMessage(), e);
        }
    }

    public void initializeSchema() {
        DatabaseSchema.initialize(this);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }
}
