package im.xz.cn.database;

import im.xz.cn.auth.AuthService;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSchema {

    public static void initialize(DatabaseManager db) {
        String type = db.getDbType();
        switch (type) {
            case "mysql" -> initializeMySQL(db);
            case "pgsql" -> initializePostgreSQL(db);
            default -> initializeSQLite(db);
        }
        migrateYggdrasilToken(db);
        migrateRegisteredIp(db);
        migrateLastLoginIp(db);
    }

    private static void migrateYggdrasilToken(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE player_profiles ADD COLUMN yggdrasil_token VARCHAR(64)";
                case "pgsql" -> "ALTER TABLE player_profiles ADD COLUMN yggdrasil_token TEXT";
                default -> "ALTER TABLE player_profiles ADD COLUMN yggdrasil_token TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                System.out.println("[DB Migration] Added yggdrasil_token column to player_profiles");
            }
        } catch (Exception ignored) {

        }
        try {
            var nullRows = db.executeQuery("SELECT id FROM player_profiles WHERE yggdrasil_token IS NULL");
            if (!nullRows.isEmpty()) {
                System.out.println("[DB Migration] Generating yggdrasil_token for " + nullRows.size() + " existing profiles");
                for (var row : nullRows) {
                    String id = String.valueOf(row.get("id"));
                    String token = AuthService.generateYggdrasilToken();
                    db.executeUpdate("UPDATE player_profiles SET yggdrasil_token = ? WHERE id = ?", token, id);
                }
            }
        } catch (Exception e) {
            System.err.println("[DB Migration] Failed to populate yggdrasil_token: " + e.getMessage());
        }
    }

    private static void migrateRegisteredIp(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN registered_ip VARCHAR(45)";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN registered_ip TEXT";
                default -> "ALTER TABLE users ADD COLUMN registered_ip TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                System.out.println("[DB Migration] Added registered_ip column to users");
            }
        } catch (Exception ignored) {

        }
    }

    private static void migrateLastLoginIp(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN last_login_ip VARCHAR(45)";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN last_login_ip TEXT";
                default -> "ALTER TABLE users ADD COLUMN last_login_ip TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                System.out.println("[DB Migration] Added last_login_ip column to users");
            }
        } catch (Exception ignored) {

        }
    }

    private static void initializeSQLite(DatabaseManager db) {
        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS admins (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'op',
                created_at TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                nickname TEXT,
                role TEXT NOT NULL DEFAULT 'default',
                email_verified INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                last_login TEXT,
                registered_ip TEXT,
                last_login_ip TEXT
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS player_profiles (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                name TEXT UNIQUE NOT NULL,
                skin_url TEXT,
                cape_url TEXT,
                skin_model TEXT DEFAULT 'default',
                yggdrasil_token TEXT,
                created_at TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS auth_tokens (
                access_token TEXT PRIMARY KEY,
                client_token TEXT,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                profile_id TEXT,
                server_id TEXT,
                created_at TEXT NOT NULL,
                expires_at TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS cache_store (
                cache_key TEXT PRIMARY KEY,
                cache_value TEXT NOT NULL,
                cache_type TEXT NOT NULL,
                created_at TEXT NOT NULL,
                expires_at TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS system_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS textures (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                type TEXT NOT NULL,
                hash TEXT NOT NULL,
                alias TEXT,
                original_name TEXT,
                size INTEGER,
                content_type TEXT,
                created_at TEXT NOT NULL,
                UNIQUE(type, hash)
            )
        """);
    }

    private static void initializeMySQL(DatabaseManager db) {
        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS admins (
                id VARCHAR(36) PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                email VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'op',
                created_at DATETIME NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                nickname VARCHAR(255),
                role VARCHAR(50) NOT NULL DEFAULT 'default',
                email_verified TINYINT(1) NOT NULL DEFAULT 0,
                created_at DATETIME NOT NULL,
                last_login DATETIME,
                registered_ip VARCHAR(45),
                last_login_ip VARCHAR(45)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS player_profiles (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                name VARCHAR(255) UNIQUE NOT NULL,
                skin_url VARCHAR(255),
                cape_url VARCHAR(255),
                skin_model VARCHAR(50) DEFAULT 'default',
                yggdrasil_token VARCHAR(64),
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS auth_tokens (
                access_token VARCHAR(255) PRIMARY KEY,
                client_token VARCHAR(255),
                user_id VARCHAR(36) NOT NULL,
                profile_id VARCHAR(36),
                server_id VARCHAR(255),
                created_at DATETIME NOT NULL,
                expires_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS cache_store (
                cache_key VARCHAR(255) PRIMARY KEY,
                cache_value TEXT NOT NULL,
                cache_type VARCHAR(50) NOT NULL,
                created_at DATETIME NOT NULL,
                expires_at DATETIME NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS system_settings (
                setting_key VARCHAR(255) PRIMARY KEY,
                setting_value TEXT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS textures (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                type VARCHAR(10) NOT NULL,
                hash VARCHAR(64) NOT NULL,
                alias VARCHAR(255),
                original_name VARCHAR(255),
                size BIGINT,
                content_type VARCHAR(100),
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY uk_type_hash (type, hash)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
    }

    private static void initializePostgreSQL(DatabaseManager db) {
        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS admins (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'op',
                created_at TIMESTAMP NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                nickname TEXT,
                role TEXT NOT NULL DEFAULT 'default',
                email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL,
                last_login TIMESTAMP,
                registered_ip TEXT,
                last_login_ip TEXT
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS player_profiles (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                name TEXT UNIQUE NOT NULL,
                skin_url TEXT,
                cape_url TEXT,
                skin_model TEXT DEFAULT 'default',
                yggdrasil_token TEXT,
                created_at TIMESTAMP NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS auth_tokens (
                access_token TEXT PRIMARY KEY,
                client_token TEXT,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                profile_id TEXT,
                server_id TEXT,
                created_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS cache_store (
                cache_key TEXT PRIMARY KEY,
                cache_value TEXT NOT NULL,
                cache_type TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS system_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS textures (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                type TEXT NOT NULL,
                hash TEXT NOT NULL,
                alias TEXT,
                original_name TEXT,
                size BIGINT,
                content_type TEXT,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(type, hash)
            )
        """);
    }
}
