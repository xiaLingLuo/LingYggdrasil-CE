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
import java.sql.SQLException;


public final class UnitOfWork {

    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private final DatabaseManager databaseManager;

    public UnitOfWork(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @FunctionalInterface
    public interface Work<T> {
        T apply(Connection connection) throws Exception;
    }

    
    public <T> T execute(Work<T> work) {
        Connection joined = CURRENT.get();
        if (joined != null) {
            try {
                return work.apply(joined);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new WorkFailedException(e, true);
            }
        }

        try (Connection connection = databaseManager.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            CURRENT.set(connection);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new WorkFailedException(e, false);
            } finally {
                CURRENT.remove();
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    
                }
            }
        } catch (SQLException e) {
            throw new WorkFailedException(e, false);
        }
    }

    
    public static Connection currentConnection() {
        return CURRENT.get();
    }

    public static boolean inTransaction() {
        return CURRENT.get() != null;
    }

    
    public static final class WorkFailedException extends RuntimeException {
        private final boolean nested;

        WorkFailedException(Throwable cause, boolean nested) {
            super(cause.getMessage(), cause);
            this.nested = nested;
        }

        public boolean nested() {
            return nested;
        }
    }
}
