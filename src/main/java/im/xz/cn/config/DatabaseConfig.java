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
