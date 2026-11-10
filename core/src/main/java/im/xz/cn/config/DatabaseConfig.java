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
package im.xz.cn.config;

public class DatabaseConfig {
    private String type = "sqlite";
    private String sqlitePath = "./data.db";
    private String host = "localhost";
    private int port;
    private String database = "yggdrasil";
    private String username;
    private String password;

    public String getJdbcUrl() {
        return switch (type.toLowerCase()) {
            case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + database + "?serverTimezone=UTC";
            case "pgsql" -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
            default -> "jdbc:sqlite:" + sqlitePath;
        };
    }

    public String getDriverClassName() {
        return switch (type.toLowerCase()) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "pgsql" -> "org.postgresql.Driver";
            default -> "org.sqlite.JDBC";
        };
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSqlitePath() { return sqlitePath; }
    public void setSqlitePath(String sqlitePath) { this.sqlitePath = sqlitePath; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
