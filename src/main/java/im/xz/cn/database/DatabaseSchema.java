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

import im.xz.cn.auth.AuthService;
import im.xz.cn.common.UuidUtil;
import im.xz.cn.logging.logApi;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class DatabaseSchema {
    private static final logApi log = logApi.getLogger(DatabaseSchema.class);

    public static void initialize(DatabaseManager db) {
        boolean rootInfoExisted = tableExists(db, "root_info");
        String type = db.getDbType();
        switch (type) {
            case "mysql" -> initializeMySQL(db);
            case "pgsql" -> initializePostgreSQL(db);
            default -> initializeSQLite(db);
        }
        migrateYggdrasilToken(db);
        migrateRegisteredIp(db);
        migrateLastLoginIp(db);
        migrateTextureUniqueConstraint(db);
        migrateDisplayProfileId(db);
        migrateFriendCode(db);
        migrateTextureLikes(db);
        migrateTextureFavorites(db);
        migrateTextureVisibility(db);
        migrateFriendSharedTextures(db);
        migrateTextureReferenceType(db);
        migrateUserTheme(db);
        migrateUserLanguage(db);
        migrateAdminColumns(db);
        migrateRootInfo(db, rootInfoExisted);
        migratePermGroups(db);
        migrateUserPermGroups(db);
        seedReservedUser(db);
        migrateUserLogs(db);
        dropLegacyRoleColumns(db);
    }

    private static void migrateUserLogs(DatabaseManager db) {
        String type = db.getDbType();
        String sql = switch (type) {
            case "mysql" -> "CREATE TABLE IF NOT EXISTS user_logs ("
                    + "id VARCHAR(36) PRIMARY KEY,"
                    + "user_id VARCHAR(36) NOT NULL,"
                    + "action VARCHAR(64) NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at DATETIME NOT NULL,"
                    + "INDEX idx_user_logs_user (user_id, created_at),"
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            case "pgsql" -> "CREATE TABLE IF NOT EXISTS user_logs ("
                    + "id TEXT PRIMARY KEY,"
                    + "user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                    + "action TEXT NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at TIMESTAMP NOT NULL)";
            default -> "CREATE TABLE IF NOT EXISTS user_logs ("
                    + "id TEXT PRIMARY KEY,"
                    + "user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                    + "action TEXT NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL)";
        };
        db.executeUpdate(sql);
        if (!"mysql".equals(type)) {
            db.executeUpdate("CREATE INDEX IF NOT EXISTS idx_user_logs_user ON user_logs (user_id, created_at)");
        } else {
            try {
                db.executeUpdate("CREATE INDEX idx_user_logs_user ON user_logs (user_id, created_at)");
            } catch (Exception ignored) {
            }
        }
    }

    private static void dropLegacyRoleColumns(DatabaseManager db) {
        for (String table : new String[]{"admins", "users"}) {
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + table + " DROP COLUMN role");
                log.info("[DB Migration] Dropped legacy role column from {}", table);
            } catch (Exception ignored) {
            }
        }
    }

    private static void migrateUserPermGroups(DatabaseManager db) {
        boolean added = false;
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN perm_group VARCHAR(255) DEFAULT 'banned'";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN perm_group TEXT DEFAULT 'banned'";
                default -> "ALTER TABLE users ADD COLUMN perm_group TEXT DEFAULT 'banned'";
            };
            stmt.execute(sql);
            added = true;
            log.info("[DB Migration] Added perm_group column to users");
        } catch (Exception ignored) {}

        try {
            var countRow = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM user_perm_group");
            long count = (countRow != null && countRow.get("cnt") != null)
                    ? ((Number) countRow.get("cnt")).longValue() : 0;
            if (count == 0) {
                String now = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                db.executeUpdate("INSERT INTO user_perm_group (id, name, permissions, created_at) VALUES (?, ?, ?, ?)",
                        "default", "default", "*", now);
                db.executeUpdate("INSERT INTO user_perm_group (id, name, permissions, created_at) VALUES (?, ?, ?, ?)",
                        "banned", "banned", "", now);
                log.info("[DB] Seeded built-in user permission groups (default, banned)");
            }
        } catch (Exception e) {
            log.warn("[DB] Failed to seed user permission groups: {}", e.getMessage());
        }

        if (added) {
            try {
                db.executeUpdate("UPDATE users SET perm_group = 'banned' WHERE role = 'banned'");
                db.executeUpdate("UPDATE users SET perm_group = 'default' WHERE role != 'banned'");
                log.info("[DB Migration] Mapped legacy role to user permission groups");
            } catch (Exception e) {
                log.warn("[DB Migration] Failed to map legacy role: {}", e.getMessage());
            }
        }
    }

    private static boolean tableExists(DatabaseManager db, String table) {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + table + " WHERE 1=0");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void migrateRootInfo(DatabaseManager db, boolean rootInfoExisted) {
        try {
            var countRow = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM root_info");
            long rootCount = (countRow != null && countRow.get("cnt") != null)
                    ? ((Number) countRow.get("cnt")).longValue() : 0;
            if (rootCount > 0) return;

            var roots = db.executeQuery("SELECT * FROM admins WHERE role = 'root'");
            if (roots.isEmpty()) return; 
            if (!rootInfoExisted) {
                log.info("[DB Migration] root_info missing -> migrating root account from admins (legacy)");
            }
            for (var row : roots) {
                db.executeUpdate(
                    "INSERT INTO root_info (id, username, email, password_hash, created_at, last_login, theme, language) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    row.get("id"), row.get("username"), row.get("email"), row.get("password_hash"),
                    row.get("created_at"), row.get("last_login"), row.get("theme"), row.get("language"));
                db.executeUpdate("DELETE FROM admins WHERE id = ?", row.get("id"));
                log.info("[DB Migration] Migrated root '{}' to root_info", row.get("username"));
            }
        } catch (Exception e) {
            log.warn("[DB Migration] migrateRootInfo failed: {}", e.getMessage());
        }
    }

    private static void migratePermGroups(DatabaseManager db) {
        try {
            var countRow = db.executeQuerySingle("SELECT COUNT(*) AS cnt FROM op_perm_group");
            long count = (countRow != null && countRow.get("cnt") != null)
                    ? ((Number) countRow.get("cnt")).longValue() : 0;
            if (count > 0) return;
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            db.executeUpdate(
                "INSERT INTO op_perm_group (id, name, permissions, created_at) VALUES (?, ?, ?, ?)",
                "op", "op", "*", now);
            log.info("[DB] Seeded default permission group 'op'");
        } catch (Exception e) {
            log.warn("[DB] Failed to seed default permission group: {}", e.getMessage());
        }
    }

    public static final String UNASSIGNED_USER_ID = "-99";

    private static void seedReservedUser(DatabaseManager db) {
        try {
            var rows = db.executeQuery("SELECT id FROM users WHERE id = ?", UNASSIGNED_USER_ID);
            if (!rows.isEmpty()) return;
            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Object emailVerified = "pgsql".equals(db.getDbType()) ? Boolean.TRUE : 1;
            db.executeUpdate(
                "INSERT INTO users (id, username, email, password_hash, nickname, email_verified, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UNASSIGNED_USER_ID, "__unassigned__", "unassigned@lingyggdrasil.local",
                "!", "未分配", emailVerified, now);
            log.info("[DB] Seeded reserved unassigned user");
        } catch (Exception e) {
            log.warn("[DB] Failed to seed reserved user: {}", e.getMessage());
        }
    }

    private static void migrateUserLanguage(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN language VARCHAR(16) DEFAULT 'zh-CN'";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN language TEXT DEFAULT 'zh-CN'";
                default -> "ALTER TABLE users ADD COLUMN language TEXT DEFAULT 'zh-CN'";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("[DB Migration] Added language column to users");
            }
        } catch (Exception ignored) {}
    }

    private static void migrateAdminColumns(DatabaseManager db) {
        String[][] columns = {
            { "last_login", "TEXT", "DATETIME", "TIMESTAMP" },
            { "theme", "TEXT DEFAULT 'light'", "VARCHAR(16) DEFAULT 'light'", "TEXT DEFAULT 'light'" },
            { "language", "TEXT DEFAULT 'zh-CN'", "VARCHAR(16) DEFAULT 'zh-CN'", "TEXT DEFAULT 'zh-CN'" },
            { "perm_group", "TEXT DEFAULT 'op'", "VARCHAR(255) DEFAULT 'op'", "TEXT DEFAULT 'op'" }
        };
        int typeIndex = switch (db.getDbType()) {
            case "mysql" -> 2;
            case "pgsql" -> 3;
            default -> 1;
        };
        for (String[] col : columns) {
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE admins ADD COLUMN " + col[0] + " " + col[typeIndex]);
                log.info("[DB Migration] Added {} column to admins", col[0]);
            } catch (Exception ignored) {}
        }
    }

    private static void migrateUserTheme(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN theme VARCHAR(16) DEFAULT 'light'";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN theme TEXT DEFAULT 'light'";
                default -> "ALTER TABLE users ADD COLUMN theme TEXT DEFAULT 'light'";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("[DB Migration] Added theme column to users");
            }
        } catch (Exception ignored) {}
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
                log.info("[DB Migration] Added yggdrasil_token column to player_profiles");
            }
        } catch (Exception ignored) {

        }
        try {
            var nullRows = db.executeQuery("SELECT id FROM player_profiles WHERE yggdrasil_token IS NULL");
            if (!nullRows.isEmpty()) {
                log.info("[DB Migration] Generating yggdrasil_token for {} existing profiles", nullRows.size());
                for (var row : nullRows) {
                    String id = String.valueOf(row.get("id"));
                    String token = AuthService.generateYggdrasilToken();
                    db.executeUpdate("UPDATE player_profiles SET yggdrasil_token = ? WHERE id = ?", token, id);
                }
            }
        } catch (Exception e) {
            log.error("[DB Migration] Failed to populate yggdrasil_token: {}", e.getMessage(), e);
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
                log.info("[DB Migration] Added registered_ip column to users");
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
                log.info("[DB Migration] Added last_login_ip column to users");
            }
        } catch (Exception ignored) {

        }
    }

    private static void migrateTextureUniqueConstraint(DatabaseManager db) {
        try {
            String dbType = db.getDbType();
            switch (dbType) {
                case "mysql" -> {
                    try {
                        db.executeUpdate("ALTER TABLE textures DROP INDEX uk_type_hash");
                        db.executeUpdate("ALTER TABLE textures ADD UNIQUE INDEX uk_user_type_hash (user_id, type, hash)");
                        log.info("[DB Migration] Updated textures UNIQUE constraint (MySQL)");
                    } catch (Exception e) {
                        try {
                            db.executeUpdate("ALTER TABLE textures ADD UNIQUE INDEX uk_user_type_hash (user_id, type, hash)");
                            log.info("[DB Migration] Added textures UNIQUE constraint (MySQL, old was missing)");
                        } catch (Exception ex) {
                        }
                    }
                }
                case "pgsql" -> {
                    try {
                        db.executeUpdate("ALTER TABLE textures DROP CONSTRAINT IF EXISTS textures_type_hash_key");
                        db.executeUpdate("ALTER TABLE textures ADD CONSTRAINT textures_user_type_hash_key UNIQUE (user_id, type, hash)");
                        log.info("[DB Migration] Updated textures UNIQUE constraint (PostgreSQL)");
                    } catch (Exception e) {
                    }
                }
                default -> {
                    boolean hasOldConstraint = false;
                    try {
                        var rows = db.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='textures'");
                        if (!rows.isEmpty()) {
                            String sql = String.valueOf(rows.get(0).get("sql"));
                            if (sql != null && sql.contains("UNIQUE(type, hash)") && !sql.contains("UNIQUE(user_id, type, hash)")) {
                                hasOldConstraint = true;
                            }
                        }
                    } catch (Exception ignored) {}
                    if (hasOldConstraint) {
                        log.info("[DB Migration] Migrating textures table (SQLite)...");
                        db.executeUpdate("CREATE TABLE textures_new ("
                                + "id TEXT PRIMARY KEY,"
                                + "user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                                + "type TEXT NOT NULL,"
                                + "hash TEXT NOT NULL,"
                                + "alias TEXT,"
                                + "original_name TEXT,"
                                + "size INTEGER,"
                                + "content_type TEXT,"
                                + "created_at TEXT NOT NULL,"
                                + "reference_type TEXT DEFAULT 'self',"
                                + "ref_owner_id TEXT,"
                                + "ref_created_at TEXT,"
                                + "UNIQUE(user_id, type, hash)"
                                + ")");
                        db.executeUpdate("INSERT INTO textures_new SELECT * FROM textures");
                        db.executeUpdate("DROP TABLE textures");
                        db.executeUpdate("ALTER TABLE textures_new RENAME TO textures");
                        log.info("[DB Migration] Successfully migrated textures table (SQLite)");
                    }
                }
            }
        } catch (Exception e) {
            log.error("[DB Migration] migrateTextureUniqueConstraint failed: {}", e.getMessage(), e);
        }
    }

    private static void migrateDisplayProfileId(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN display_profile_id VARCHAR(36)";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN display_profile_id TEXT";
                default -> "ALTER TABLE users ADD COLUMN display_profile_id TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("[DB Migration] Added display_profile_id column to users");
            }
        } catch (Exception ignored) {}
    }

    private static void migrateFriendCode(DatabaseManager db) {
        try {
            String alterSql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE users ADD COLUMN friend_code VARCHAR(16)";
                case "pgsql" -> "ALTER TABLE users ADD COLUMN friend_code TEXT";
                default -> "ALTER TABLE users ADD COLUMN friend_code TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(alterSql);
                log.info("[DB Migration] Added friend_code column to users");
            }
        } catch (Exception ignored) {}
        try {
            List<Map<String, Object>> rows = db.executeQuery("SELECT id FROM users WHERE friend_code IS NULL");
            if (!rows.isEmpty()) {
                log.info("[DB Migration] Generating friend_code for {} existing users", rows.size());
                for (var row : rows) {
                    String id = String.valueOf(row.get("id"));
                    String code = UuidUtil.generateFriendCode(id);
                    db.executeUpdate("UPDATE users SET friend_code = ? WHERE id = ?", code, id);
                }
            }
        } catch (Exception e) {
            log.error("[DB Migration] Failed to populate friend_code: {}", e.getMessage(), e);
        }
    }

    private static void migrateTextureLikes(DatabaseManager db) {
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "CREATE TABLE IF NOT EXISTS texture_likes (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, texture_id VARCHAR(36) NOT NULL, created_at DATETIME NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE, UNIQUE KEY uk_user_texture_like (user_id, texture_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                case "pgsql" -> "CREATE TABLE IF NOT EXISTS texture_likes (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL, UNIQUE(user_id, texture_id))";
                default -> "CREATE TABLE IF NOT EXISTS texture_likes (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, created_at TEXT NOT NULL, UNIQUE(user_id, texture_id))";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
    }

    private static void migrateTextureFavorites(DatabaseManager db) {
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "CREATE TABLE IF NOT EXISTS texture_favorites (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, texture_id VARCHAR(36) NOT NULL, alias VARCHAR(255), created_at DATETIME NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE, UNIQUE KEY uk_user_texture_fav (user_id, texture_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                case "pgsql" -> "CREATE TABLE IF NOT EXISTS texture_favorites (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, alias TEXT, created_at TIMESTAMP NOT NULL, UNIQUE(user_id, texture_id))";
                default -> "CREATE TABLE IF NOT EXISTS texture_favorites (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, alias TEXT, created_at TEXT NOT NULL, UNIQUE(user_id, texture_id))";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
    }

    private static void migrateTextureVisibility(DatabaseManager db) {
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "CREATE TABLE IF NOT EXISTS texture_visibility (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, texture_id VARCHAR(36) NOT NULL, is_public TINYINT(1) NOT NULL DEFAULT 0, created_at DATETIME NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE, UNIQUE KEY uk_user_texture_vis (user_id, texture_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                case "pgsql" -> "CREATE TABLE IF NOT EXISTS texture_visibility (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, is_public BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL, UNIQUE(user_id, texture_id))";
                default -> "CREATE TABLE IF NOT EXISTS texture_visibility (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, is_public INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL, UNIQUE(user_id, texture_id))";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
    }

    private static void migrateFriendSharedTextures(DatabaseManager db) {
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "CREATE TABLE IF NOT EXISTS friend_shared_textures (id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36) NOT NULL, friend_id VARCHAR(36) NOT NULL, texture_id VARCHAR(36) NOT NULL, created_at DATETIME NOT NULL, FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE, UNIQUE KEY uk_owner_friend_texture (owner_id, friend_id, texture_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                case "pgsql" -> "CREATE TABLE IF NOT EXISTS friend_shared_textures (id TEXT PRIMARY KEY, owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, friend_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL, UNIQUE(owner_id, friend_id, texture_id))";
                default -> "CREATE TABLE IF NOT EXISTS friend_shared_textures (id TEXT PRIMARY KEY, owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, friend_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE, created_at TEXT NOT NULL, UNIQUE(owner_id, friend_id, texture_id))";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
    }

    private static void migrateTextureReferenceType(DatabaseManager db) {
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE textures ADD COLUMN reference_type VARCHAR(10) DEFAULT 'self'";
                case "pgsql" -> "ALTER TABLE textures ADD COLUMN reference_type TEXT DEFAULT 'self'";
                default -> "ALTER TABLE textures ADD COLUMN reference_type TEXT DEFAULT 'self'";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE textures ADD COLUMN ref_owner_id VARCHAR(36)";
                case "pgsql" -> "ALTER TABLE textures ADD COLUMN ref_owner_id TEXT";
                default -> "ALTER TABLE textures ADD COLUMN ref_owner_id TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
        try {
            String sql = switch (db.getDbType()) {
                case "mysql" -> "ALTER TABLE textures ADD COLUMN ref_created_at DATETIME";
                case "pgsql" -> "ALTER TABLE textures ADD COLUMN ref_created_at TIMESTAMP";
                default -> "ALTER TABLE textures ADD COLUMN ref_created_at TEXT";
            };
            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (Exception ignored) {}
    }

    private static void initializeSQLite(DatabaseManager db) {
        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS admins (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL,
                last_login TEXT,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN',
                perm_group TEXT DEFAULT 'op'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS root_info (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL,
                last_login TEXT,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS op_perm_group (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
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
                email_verified INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                last_login TEXT,
                registered_ip TEXT,
                last_login_ip TEXT,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN',
                perm_group TEXT DEFAULT 'banned'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS user_perm_group (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
                created_at TEXT NOT NULL
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
                reference_type TEXT DEFAULT 'self',
                ref_owner_id TEXT,
                ref_created_at TEXT,
                UNIQUE(user_id, type, hash)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friends (
                id TEXT PRIMARY KEY,
                user_id_lower TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                user_id_higher TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TEXT NOT NULL,
                UNIQUE(user_id_lower, user_id_higher)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS confirming_friends (
                id TEXT PRIMARY KEY,
                sender_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                receiver_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TEXT NOT NULL,
                UNIQUE(sender_id, receiver_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS blocked_users (
                id TEXT PRIMARY KEY,
                blocker_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                blocked_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TEXT NOT NULL,
                UNIQUE(blocker_id, blocked_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_meta (
                hash TEXT PRIMARY KEY,
                admin_alias TEXT
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_likes (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                created_at TEXT NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_favorites (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                alias TEXT,
                created_at TEXT NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_visibility (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                is_public INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friend_shared_textures (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                friend_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                created_at TEXT NOT NULL,
                UNIQUE(owner_id, friend_id, texture_id)
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
                created_at DATETIME NOT NULL,
                last_login DATETIME,
                theme VARCHAR(16) DEFAULT 'light',
                language VARCHAR(16) DEFAULT 'zh-CN',
                perm_group VARCHAR(255) DEFAULT 'op'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS root_info (
                id VARCHAR(36) PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                email VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                created_at DATETIME NOT NULL,
                last_login DATETIME,
                theme VARCHAR(16) DEFAULT 'light',
                language VARCHAR(16) DEFAULT 'zh-CN'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS op_perm_group (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
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
                email_verified TINYINT(1) NOT NULL DEFAULT 0,
                created_at DATETIME NOT NULL,
                last_login DATETIME,
                registered_ip VARCHAR(45),
                last_login_ip VARCHAR(45),
                theme VARCHAR(16) DEFAULT 'light',
                language VARCHAR(16) DEFAULT 'zh-CN',
                perm_group VARCHAR(255) DEFAULT 'banned'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS user_perm_group (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(255) UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
                created_at DATETIME NOT NULL
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
                reference_type VARCHAR(10) DEFAULT 'self',
                ref_owner_id VARCHAR(36),
                ref_created_at DATETIME,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY uk_user_type_hash (user_id, type, hash)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friends (
                id VARCHAR(36) PRIMARY KEY,
                user_id_lower VARCHAR(36) NOT NULL,
                user_id_higher VARCHAR(36) NOT NULL,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id_lower) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id_higher) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY uk_friends_pair (user_id_lower, user_id_higher)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS confirming_friends (
                id VARCHAR(36) PRIMARY KEY,
                sender_id VARCHAR(36) NOT NULL,
                receiver_id VARCHAR(36) NOT NULL,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY uk_confirming_pair (sender_id, receiver_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS blocked_users (
                id VARCHAR(36) PRIMARY KEY,
                blocker_id VARCHAR(36) NOT NULL,
                blocked_id VARCHAR(36) NOT NULL,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY uk_blocked_pair (blocker_id, blocked_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_meta (
                hash VARCHAR(64) PRIMARY KEY,
                admin_alias VARCHAR(255)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_likes (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                texture_id VARCHAR(36) NOT NULL,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE,
                UNIQUE KEY uk_user_texture_like (user_id, texture_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_favorites (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                texture_id VARCHAR(36) NOT NULL,
                alias VARCHAR(255),
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE,
                UNIQUE KEY uk_user_texture_fav (user_id, texture_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_visibility (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                texture_id VARCHAR(36) NOT NULL,
                is_public TINYINT(1) NOT NULL DEFAULT 0,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE,
                UNIQUE KEY uk_user_texture_vis (user_id, texture_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friend_shared_textures (
                id VARCHAR(36) PRIMARY KEY,
                owner_id VARCHAR(36) NOT NULL,
                friend_id VARCHAR(36) NOT NULL,
                texture_id VARCHAR(36) NOT NULL,
                created_at DATETIME NOT NULL,
                FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (texture_id) REFERENCES textures(id) ON DELETE CASCADE,
                UNIQUE KEY uk_owner_friend_texture (owner_id, friend_id, texture_id)
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
                created_at TIMESTAMP NOT NULL,
                last_login TIMESTAMP,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN',
                perm_group TEXT DEFAULT 'op'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS root_info (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                email TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                last_login TIMESTAMP,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS op_perm_group (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
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
                email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL,
                last_login TIMESTAMP,
                registered_ip TEXT,
                last_login_ip TEXT,
                theme TEXT DEFAULT 'light',
                language TEXT DEFAULT 'zh-CN',
                perm_group TEXT DEFAULT 'banned'
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS user_perm_group (
                id TEXT PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                permissions TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL
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
                reference_type TEXT DEFAULT 'self',
                ref_owner_id TEXT,
                ref_created_at TIMESTAMP,
                UNIQUE(user_id, type, hash)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friends (
                id TEXT PRIMARY KEY,
                user_id_lower TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                user_id_higher TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(user_id_lower, user_id_higher)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS confirming_friends (
                id TEXT PRIMARY KEY,
                sender_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                receiver_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(sender_id, receiver_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS blocked_users (
                id TEXT PRIMARY KEY,
                blocker_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                blocked_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(blocker_id, blocked_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_meta (
                hash TEXT PRIMARY KEY,
                admin_alias TEXT
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_likes (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_favorites (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                alias TEXT,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS texture_visibility (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                is_public BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(user_id, texture_id)
            )
        """);

        db.executeRaw("""
            CREATE TABLE IF NOT EXISTS friend_shared_textures (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                friend_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                texture_id TEXT NOT NULL REFERENCES textures(id) ON DELETE CASCADE,
                created_at TIMESTAMP NOT NULL,
                UNIQUE(owner_id, friend_id, texture_id)
            )
        """);
    }
}
