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
import im.xz.cn.infrastructure.sql.SqlDialect;
import im.xz.cn.infrastructure.sql.SqlDialects;
import im.xz.cn.logging.logApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class SchemaBaseline {

    public static final long SCHEMA_VERSION = 4L;
    public static final long CONFIG_FORMAT_VERSION = 1L;
    public static final long STORAGE_FORMAT_VERSION = 1L;
    public static final String PLUGIN_API_VERSION = "2.10.0";

    private static final logApi log = logApi.getLogger(SchemaBaseline.class);

    private final SqlDialect dialect;

    public SchemaBaseline(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public SqlDialect dialect() {
        return dialect;
    }

    
    public static boolean isCurrent(DatabaseManager database) {
        try {
            String value = Jdbc.queryOne(database, rs -> rs.getString("meta_value"),
                    "SELECT meta_value FROM instance_meta WHERE meta_key = 'schema_version'");
            return value != null && Long.parseLong(value.trim()) >= SCHEMA_VERSION;
        } catch (RuntimeException error) {
            return false;
        }
    }

    
    
    private void renameLegacyPrefixedTables(DatabaseManager database) {
        int renamed = 0;
        for (String table : LEGACY_PREFIXED_TABLES) {
            String legacy = "ly_" + table;
            if (!tableExists(database, legacy) || tableExists(database, table)) {
                continue;
            }
            database.executeUpdate("ALTER TABLE " + legacy + " RENAME TO " + table);
            renamed++;
        }
        if (renamed > 0) {
            log.info("[SchemaBaseline] renamed {} legacy ly_ tables to unprefixed names", renamed);
        }
    }

    private static boolean tableExists(DatabaseManager database, String table) {
        try (java.sql.Connection connection = database.getConnection();
             java.sql.ResultSet tables = connection.getMetaData()
                     .getTables(null, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch (java.sql.SQLException error) {
            return false;
        }
    }

    private static boolean columnExists(DatabaseManager database, String table, String column) {
        try (java.sql.Connection connection = database.getConnection();
             java.sql.ResultSet columns = connection.getMetaData()
                     .getColumns(null, null, table, column)) {
            return columns.next();
        } catch (java.sql.SQLException error) {
            return false;
        }
    }

    private static final String[] LEGACY_PREFIXED_TABLES = {
            "access_sessions",
            "audit_events",
            "friend_requests",
            "friendships",
            "group_permission_nodes",
            "instance_meta",
            "library_entries",
            "migration_history",
            "migration_mappings",
            "minecraft_join_tickets",
            "notification_jobs",
            "pending_registrations",
            "permission_groups",
            "profile_credentials",
            "profile_texture_bindings",
            "publication_favorites",
            "publication_likes",
            "secret_store",
            "settings",
            "settings_revision",
            "texture_access_events",
            "texture_assets",
            "texture_blobs",
            "texture_publications",
            "texture_share_grants",
            "texture_uploads",
            "user_blocks",
            "user_preferences",
            "user_social_profiles",
            "verification_challenges",
    };
    
    private void upgradeLegacyProfileColumns(DatabaseManager database) {
        if (!tableExists(database, "player_profiles")) {
            return;
        }
        addColumnIfMissing(database, "player_profiles", "owner_user_id", dialect.textType(64));
        addColumnIfMissing(database, "player_profiles", "state", dialect.textType(16) + " DEFAULT 'ACTIVE'");
        addColumnIfMissing(database, "player_profiles", "version", dialect.bigIntType() + " DEFAULT 0");
        database.executeUpdate("UPDATE player_profiles SET owner_user_id = user_id"
                + " WHERE owner_user_id IS NULL");
        database.executeUpdate("UPDATE player_profiles SET state = 'ACTIVE' WHERE state IS NULL");
        database.executeUpdate("UPDATE player_profiles SET version = 0 WHERE version IS NULL");
    }

    
    private void upgradeGrantHistoryConstraint(DatabaseManager database) {
        if (!tableExists(database, "texture_share_grants")) {
            return;
        }
        String dialectId = dialect.id();
        try {
            if ("mysql".equalsIgnoreCase(dialectId)) {
                try {
                    database.executeUpdate("ALTER TABLE texture_share_grants"
                            + " DROP INDEX uq_ly_grant_owner_recipient_asset_state");
                } catch (RuntimeException ignored) {
                    
                }
            } else if ("pgsql".equalsIgnoreCase(dialectId)) {
                try {
                    database.executeUpdate("ALTER TABLE texture_share_grants"
                            + " DROP CONSTRAINT IF EXISTS uq_ly_grant_owner_recipient_asset_state");
                } catch (RuntimeException ignored) {
                    
                }
            } else {
                String sql = Jdbc.queryOne(database, rs -> rs.getString("sql"),
                        "SELECT sql FROM sqlite_master WHERE type = 'table'"
                                + " AND name = 'texture_share_grants'");
                if (sql != null && sql.contains("uq_ly_grant_owner_recipient_asset_state")) {
                    database.executeUpdate("ALTER TABLE texture_share_grants RENAME TO texture_share_grants_old");
                    for (String statement : statements()) {
                        if (statement.startsWith("CREATE TABLE IF NOT EXISTS texture_share_grants")) {
                            database.executeUpdate(statement);
                            break;
                        }
                    }
                    database.executeUpdate("INSERT INTO texture_share_grants (id, owner_upload_id,"
                            + " owner_id, recipient_id, asset_id, state, created_at, ended_at)"
                            + " SELECT id, owner_upload_id, owner_id, recipient_id, asset_id, state,"
                            + " created_at, ended_at FROM texture_share_grants_old");
                    database.executeUpdate("DROP TABLE texture_share_grants_old");
                }
            }
        } catch (RuntimeException error) {
            log.warn("[SchemaBaseline] grant history constraint upgrade skipped: {}", error.getMessage());
        }
    }

    
    private void upgradeAccessSessionColumns(DatabaseManager database) {
        if (!tableExists(database, "access_sessions")) {
            return;
        }
        addColumnIfMissing(database, "access_sessions", "server_id", dialect.textType(128));
        addColumnIfMissing(database, "access_sessions", "join_expires_at", dialect.timestampType());
        addColumnIfMissing(database, "access_sessions", "join_ip", dialect.textType(64));
    }

    
    private void upgradeTextureAssetColumns(DatabaseManager database) {
        if (!tableExists(database, "texture_assets")) {
            return;
        }
        addColumnIfMissing(database, "texture_assets", "skin_model",
                dialect.textType(8) + " NOT NULL DEFAULT 'DEFAULT'");
    }

    
    private void upgradeUnassignedProfiles(DatabaseManager database) {
        if (!tableExists(database, "player_profiles")) {
            return;
        }
        String dialectId = dialect.id();
        try {
            if ("mysql".equalsIgnoreCase(dialectId)) {
                database.executeUpdate("ALTER TABLE player_profiles MODIFY COLUMN user_id VARCHAR(36) NULL");
            } else if ("pgsql".equalsIgnoreCase(dialectId)) {
                database.executeUpdate("ALTER TABLE player_profiles ALTER COLUMN user_id DROP NOT NULL");
            } else {
                rebuildSqliteProfilesForNullableOwner(database);
            }
        } catch (RuntimeException error) {
            log.warn("[SchemaBaseline] nullable profile owner upgrade skipped: {}", error.getMessage());
        }
        try {
            database.executeUpdate("UPDATE player_profiles SET user_id = NULL, owner_user_id = NULL"
                    + " WHERE user_id = '-99' OR owner_user_id = '-99'");
            database.executeUpdate("DELETE FROM users WHERE id = '-99'");
        } catch (RuntimeException error) {
            log.warn("[SchemaBaseline] reserved user cleanup skipped: {}", error.getMessage());
        }
    }

    private void rebuildSqliteProfilesForNullableOwner(DatabaseManager database) {
        boolean nullable = false;
        try {
            for (String[] column : Jdbc.queryMany(database, resultSet -> new String[]{
                    resultSet.getString("name"), resultSet.getString("notnull")},
                    "PRAGMA table_info(player_profiles)")) {
                if ("user_id".equals(column[0])) {
                    nullable = "0".equals(column[1]);
                }
            }
        } catch (RuntimeException error) {
            return;
        }
        if (nullable) {
            return;
        }
        database.executeUpdate("ALTER TABLE player_profiles RENAME TO player_profiles_old");
        database.executeUpdate("CREATE TABLE player_profiles ("
                + "id TEXT PRIMARY KEY,"
                + "user_id TEXT,"
                + "name TEXT UNIQUE NOT NULL,"
                + "skin_hash TEXT, cape_hash TEXT, skin_model TEXT DEFAULT 'default',"
                + "yggdrasil_token TEXT, created_at TEXT NOT NULL,"
                + "owner_user_id TEXT, state TEXT DEFAULT 'ACTIVE', version INTEGER DEFAULT 0)");
        database.executeUpdate("INSERT INTO player_profiles (id, user_id, name, skin_hash, cape_hash,"
                + " skin_model, yggdrasil_token, created_at, owner_user_id, state, version)"
                + " SELECT id, user_id, name, skin_hash, cape_hash, skin_model, yggdrasil_token,"
                + " created_at, owner_user_id, state, version FROM player_profiles_old");
        database.executeUpdate("DROP TABLE player_profiles_old");
    }

    private static void addColumnIfMissing(DatabaseManager database, String table, String column, String definition) {
        if (columnExists(database, table, column)) {
            return;
        }
        database.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }
    public void apply(DatabaseManager database) {
        renameLegacyPrefixedTables(database);
        upgradeLegacyProfileColumns(database);
        upgradeAccessSessionColumns(database);
        upgradeTextureAssetColumns(database);
        upgradeUnassignedProfiles(database);
        upgradeGrantHistoryConstraint(database);
        int created = 0;
        for (String sql : statements()) {
            try {
                database.executeUpdate(sql);
                created++;
            } catch (RuntimeException error) {
                if (DatabaseManager.isDuplicateIndexName(error) || isAlreadyExists(error)) {
                    continue;
                }
                throw error;
            }
        }
        seedMeta(database, "schema_version", Long.toString(SCHEMA_VERSION));
        seedMeta(database, "config_format_version", Long.toString(CONFIG_FORMAT_VERSION));
        seedMeta(database, "storage_format_version", Long.toString(STORAGE_FORMAT_VERSION));
        ensureSecretCiphertextCapacity(database);
        seedMeta(database, "plugin_api_version", PLUGIN_API_VERSION);
        log.info("[SchemaBaseline] applied {} statements (dialect={}, schemaVersion={})",
                created, dialect.id(), SCHEMA_VERSION);
    }

    
    private void ensureSecretCiphertextCapacity(DatabaseManager database) {
        if (!"mysql".equalsIgnoreCase(dialect.id()) || !tableExists(database, "secret_store")) {
            return;
        }
        try (java.sql.Connection connection = database.getConnection();
             java.sql.ResultSet columns = connection.getMetaData()
                     .getColumns(null, null, "secret_store", "ciphertext")) {
            if (columns.next() && "LONGTEXT".equalsIgnoreCase(columns.getString("TYPE_NAME"))) {
                return;
            }
            database.executeUpdate(
                    "ALTER TABLE secret_store MODIFY COLUMN ciphertext LONGTEXT NOT NULL");
            log.info("[SchemaBaseline] widened secret_store.ciphertext to LONGTEXT");
        } catch (java.sql.SQLException error) {
            log.warn("[SchemaBaseline] Cannot inspect secret_store.ciphertext: {}", error.getMessage());
        } catch (RuntimeException error) {
            log.warn("[SchemaBaseline] Cannot widen secret_store.ciphertext: {}", error.getMessage());
        }
    }

    public List<String> statements() {
        String pk = dialect.autoIncrementPrimaryKey();
        String big = dialect.bigIntType();
        
        String idText = dialect.textType(64);
        String ts = dialect.timestampType();
        String bool = dialect.booleanType();

        List<String> sql = new ArrayList<>();

        
        sql.add(table("texture_blobs",
                "id " + pk
                        + ", sha256 " + dialect.textType(64) + " NOT NULL"
                        + ", size_bytes " + big + " NOT NULL"
                        + ", content_type " + dialect.textType(64) + " NOT NULL"
                        + ", storage_key " + dialect.textType(512) + " NOT NULL"
                        + ", state " + dialect.textType(16) + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_blob_sha UNIQUE (sha256)"
                        + ", CONSTRAINT uq_ly_blob_storage UNIQUE (storage_key)"
                        + ", CONSTRAINT ck_ly_blob_size CHECK (size_bytes >= 0)"));

        sql.add(table("texture_assets",
                "id " + pk
                        + ", blob_id " + big + " NOT NULL"
                        + ", type " + dialect.textType(8) + " NOT NULL"
                        + ", width " + big + " NOT NULL"
                        + ", height " + big + " NOT NULL"
                        + ", admin_file_name " + dialect.textType(255)
                        + ", author_name " + dialect.textType(128)
                        + ", skin_model " + dialect.textType(8) + " NOT NULL DEFAULT 'DEFAULT'"
                        + ", state " + dialect.textType(16) + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_asset_type_blob UNIQUE (type, blob_id)"
                        + ", CONSTRAINT fk_ly_asset_blob FOREIGN KEY (blob_id) REFERENCES texture_blobs (id)"
                        + ", CONSTRAINT ck_ly_asset_size CHECK (width > 0 AND height > 0)"));

        sql.add(table("texture_uploads",
                "id " + pk
                        + ", user_id " + idText + " NOT NULL"
                        + ", asset_id " + big + " NOT NULL"
                        + ", original_name " + dialect.textType(255)
                        + ", created_at " + ts + " NOT NULL"
                        + ", state " + dialect.textType(16) + " NOT NULL DEFAULT 'ACTIVE'"
                        + ", CONSTRAINT uq_ly_upload_user_asset UNIQUE (user_id, asset_id)"
                        + ", CONSTRAINT fk_ly_upload_asset FOREIGN KEY (asset_id) REFERENCES texture_assets (id)"));

        sql.add(table("library_entries",
                "id " + pk
                        + ", user_id " + idText + " NOT NULL"
                        + ", asset_id " + big + " NOT NULL"
                        + ", alias " + dialect.textType(255)
                        + ", hidden " + bool + " NOT NULL DEFAULT 0"
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_library_user_asset UNIQUE (user_id, asset_id)"
                        + ", CONSTRAINT fk_ly_library_asset FOREIGN KEY (asset_id) REFERENCES texture_assets (id)"));

        sql.add(table("texture_publications",
                "id " + pk
                        + ", publisher_id " + idText + " NOT NULL"
                        + ", asset_id " + big + " NOT NULL"
                        + ", state " + dialect.textType(16) + " NOT NULL"
                        + ", first_published_at " + ts
                        + ", last_published_at " + ts
                        + ", withdrawn_at " + ts
                        + ", version " + big + " NOT NULL DEFAULT 0"
                        + ", CONSTRAINT uq_ly_publication_publisher_asset UNIQUE (publisher_id, asset_id)"
                        + ", CONSTRAINT fk_ly_publication_asset FOREIGN KEY (asset_id) REFERENCES texture_assets (id)"));

        sql.add(table("publication_likes",
                "user_id " + idText + " NOT NULL"
                        + ", publication_id " + big + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", PRIMARY KEY (user_id, publication_id)"
                        + ", CONSTRAINT fk_ly_like_publication FOREIGN KEY (publication_id) REFERENCES texture_publications (id)"));

        sql.add(table("publication_favorites",
                "id " + pk
                        + ", user_id " + idText + " NOT NULL"
                        + ", publication_id " + big + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_favorite_user_publication UNIQUE (user_id, publication_id)"
                        + ", CONSTRAINT fk_ly_favorite_publication FOREIGN KEY (publication_id) REFERENCES texture_publications (id)"));

        sql.add(table("texture_share_grants",
                "id " + pk
                        + ", owner_upload_id " + big + " NOT NULL"
                        + ", owner_id " + idText + " NOT NULL"
                        + ", recipient_id " + idText + " NOT NULL"
                        + ", asset_id " + big + " NOT NULL"
                        + ", state " + dialect.textType(24) + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", ended_at " + ts
                        + ", CONSTRAINT fk_ly_grant_upload FOREIGN KEY (owner_upload_id) REFERENCES texture_uploads (id)"
                        + ", CONSTRAINT fk_ly_grant_asset FOREIGN KEY (asset_id) REFERENCES texture_assets (id)"
                        + ", CONSTRAINT ck_ly_grant_parties CHECK (owner_id <> recipient_id)"));

        sql.add(table("profile_texture_bindings",
                "profile_id " + idText + " NOT NULL"
                        + ", slot " + dialect.textType(8) + " NOT NULL"
                        + ", asset_id " + big + " NOT NULL"
                        + ", skin_model " + dialect.textType(8) + " NOT NULL"
                        + ", updated_at " + ts + " NOT NULL"
                        + ", PRIMARY KEY (profile_id, slot)"
                        + ", CONSTRAINT fk_ly_binding_asset FOREIGN KEY (asset_id) REFERENCES texture_assets (id)"));

        sql.add(table("texture_access_events",
                "id " + pk
                        + ", user_id " + idText
                        + ", profile_id " + idText
                        + ", asset_id " + big
                        + ", reason " + dialect.textType(128) + " NOT NULL"
                        + ", occurred_at " + ts + " NOT NULL"));

        
        sql.add(table("user_preferences",
                "user_id " + idText + " NOT NULL PRIMARY KEY"
                        + ", theme " + dialect.textType(16)
                        + ", language " + dialect.textType(16)
                        + ", allow_account_password_login " + bool + " NOT NULL DEFAULT 1"
                        + ", updated_at " + ts));

        sql.add(table("user_social_profiles",
                "user_id " + idText + " NOT NULL PRIMARY KEY"
                        + ", display_profile_id " + idText
                        + ", updated_at " + ts));

        sql.add(table("permission_groups",
                "id " + pk
                        + ", code " + dialect.textType(64) + " NOT NULL"
                        + ", display_name " + dialect.textType(128) + " NOT NULL"
                        + ", scope " + dialect.textType(8) + " NOT NULL"
                        + ", builtin " + bool + " NOT NULL DEFAULT 0"
                        + ", created_at " + ts
                        + ", CONSTRAINT uq_ly_perm_group_code_scope UNIQUE (code, scope)"));

        sql.add(table("group_permission_nodes",
                "group_id " + big + " NOT NULL"
                        + ", node " + dialect.textType(128) + " NOT NULL"
                        + ", kind " + dialect.textType(16) + " NOT NULL"
                        + ", PRIMARY KEY (group_id, node)"
                        + ", CONSTRAINT fk_ly_group_node_group FOREIGN KEY (group_id) REFERENCES permission_groups (id)"));

        sql.add(table("profile_credentials",
                "profile_id " + idText + " NOT NULL PRIMARY KEY"
                        + ", key_cipher " + dialect.textType(1024) + " NOT NULL"
                        + ", key_digest " + dialect.textType(128) + " NOT NULL"
                        + ", key_version " + big + " NOT NULL DEFAULT 1"
                        + ", updated_at " + ts));

        sql.add(table("friendships",
                "user_low_id " + idText + " NOT NULL"
                        + ", user_high_id " + idText + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", PRIMARY KEY (user_low_id, user_high_id)"
                        + ", CONSTRAINT ck_ly_friendship_order CHECK (user_low_id < user_high_id)"));

        sql.add(table("friend_requests",
                "id " + pk
                        + ", from_user_id " + idText + " NOT NULL"
                        + ", to_user_id " + idText + " NOT NULL"
                        + ", state " + dialect.textType(16) + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT ck_ly_friend_request_parties CHECK (from_user_id <> to_user_id)"));

        sql.add(table("user_blocks",
                "blocker_id " + idText + " NOT NULL"
                        + ", blocked_id " + idText + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", PRIMARY KEY (blocker_id, blocked_id)"
                        + ", CONSTRAINT ck_ly_block_parties CHECK (blocker_id <> blocked_id)"));

        
        sql.add(table("access_sessions",
                "id " + pk
                        + ", token_digest " + dialect.textType(128) + " NOT NULL"
                        + ", client_token " + dialect.textType(64)
                        + ", user_id " + idText
                        + ", profile_id " + idText
                        + ", auth_source " + dialect.textType(24) + " NOT NULL"
                        + ", credential_version " + big + " NOT NULL DEFAULT 1"
                        + ", issued_at " + ts + " NOT NULL"
                        + ", expires_at " + ts + " NOT NULL"
                        + ", invalid_before " + ts
                        + ", revoked_at " + ts
                        + ", server_id " + dialect.textType(128)
                        + ", join_expires_at " + ts
                        + ", join_ip " + dialect.textType(64)
                        + ", CONSTRAINT uq_ly_session_token UNIQUE (token_digest)"));

        sql.add(table("minecraft_join_tickets",
                "id " + pk
                        + ", access_session_id " + big + " NOT NULL"
                        + ", profile_id " + idText + " NOT NULL"
                        + ", server_id " + dialect.textType(128) + " NOT NULL"
                        + ", client_ip " + dialect.textType(64)
                        + ", created_at " + ts + " NOT NULL"
                        + ", expires_at " + ts + " NOT NULL"));

        sql.add(table("verification_challenges",
                "id " + pk
                        + ", purpose " + dialect.textType(32) + " NOT NULL"
                        + ", subject_id " + idText
                        + ", target " + dialect.textType(255)
                        + ", digest " + dialect.textType(128) + " NOT NULL"
                        + ", expires_at " + ts + " NOT NULL"
                        + ", remaining_attempts " + big + " NOT NULL DEFAULT 5"
                        + ", consumed_at " + ts
                        + ", context_json " + dialect.textType(1024)));

        sql.add(table("pending_registrations",
                "id " + pk
                        + ", username " + dialect.textType(64) + " NOT NULL"
                        + ", email " + dialect.textType(255)
                        + ", password_hash " + dialect.textType(255) + " NOT NULL"
                        + ", created_at " + ts + " NOT NULL"
                        + ", expires_at " + ts + " NOT NULL"));

        sql.add(table("notification_jobs",
                "id " + pk
                        + ", type " + dialect.textType(64) + " NOT NULL"
                        + ", target " + dialect.textType(255)
                        + ", payload_json " + dialect.textType(4096)
                        + ", locale " + dialect.textType(16)
                        + ", state " + dialect.textType(16) + " NOT NULL"
                        + ", attempts " + big + " NOT NULL DEFAULT 0"
                        + ", next_attempt_at " + ts
                        + ", idempotency_key " + dialect.textType(128)
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_notification_idempotency UNIQUE (idempotency_key)"));

        sql.add(table("audit_events",
                "id " + pk
                        + ", actor_type " + dialect.textType(16) + " NOT NULL"
                        + ", actor_id " + dialect.textType(64)
                        + ", action " + dialect.textType(64) + " NOT NULL"
                        + ", target_type " + dialect.textType(32)
                        + ", target_id " + dialect.textType(64)
                        + ", result " + dialect.textType(16) + " NOT NULL"
                        + ", reason " + dialect.textType(255)
                        + ", correlation_id " + dialect.textType(64)
                        + ", occurred_at " + ts + " NOT NULL"));

        sql.add(table("settings",
                "group_name " + dialect.textType(64) + " NOT NULL"
                        + ", setting_key " + dialect.textType(128) + " NOT NULL"
                        + ", value_json " + dialect.textType(4096)
                        + ", revision " + big + " NOT NULL DEFAULT 1"
                        + ", PRIMARY KEY (group_name, setting_key)"));

        sql.add(table("settings_revision",
                "group_name " + dialect.textType(64) + " NOT NULL PRIMARY KEY"
                        + ", revision " + big + " NOT NULL DEFAULT 1"
                        + ", updated_at " + ts));

        sql.add(table("secret_store",
                "name " + dialect.textType(128) + " NOT NULL PRIMARY KEY"
                        + ", algorithm " + dialect.textType(64) + " NOT NULL"
                        + ", key_version " + big + " NOT NULL"
                        + ", ciphertext " + dialect.textType(0) + " NOT NULL"
                        + ", updated_at " + ts + " NOT NULL"));

        sql.add(table("instance_meta",
                "meta_key " + dialect.textType(128) + " NOT NULL PRIMARY KEY"
                        + ", meta_value " + dialect.textType(1024)
                        + ", updated_at " + ts));

        sql.add(table("migration_history",
                "id " + pk
                        + ", step_id " + dialect.textType(128) + " NOT NULL"
                        + ", from_version " + dialect.textType(64)
                        + ", to_version " + dialect.textType(64)
                        + ", checksum " + dialect.textType(128)
                        + ", tool_version " + dialect.textType(32)
                        + ", started_at " + ts
                        + ", finished_at " + ts
                        + ", status " + dialect.textType(16) + " NOT NULL"
                        + ", records " + big + " NOT NULL DEFAULT 0"
                        + ", checkpoint " + dialect.textType(255)
                        + ", failure " + dialect.textType(1024)));

        sql.add(table("migration_mappings",
                "id " + pk
                        + ", kind " + dialect.textType(32) + " NOT NULL"
                        + ", old_id " + dialect.textType(128) + " NOT NULL"
                        + ", new_id " + dialect.textType(128) + " NOT NULL"
                        + ", note " + dialect.textType(255)
                        + ", created_at " + ts + " NOT NULL"
                        + ", CONSTRAINT uq_ly_mapping_kind_old UNIQUE (kind, old_id)"));

        sql.add(index("idx_ly_publication_publisher", "texture_publications", "publisher_id, state"));
        sql.add(index("idx_ly_publication_state_time", "texture_publications", "state, last_published_at"));
        sql.add(index("idx_ly_publication_asset", "texture_publications", "asset_id, state"));
        sql.add(index("idx_ly_like_publication", "publication_likes", "publication_id"));
        sql.add(index("idx_ly_favorite_publication", "publication_favorites", "publication_id"));
        sql.add(index("idx_ly_favorite_user", "publication_favorites", "user_id"));
        sql.add(index("idx_ly_grant_recipient", "texture_share_grants", "recipient_id, state"));
        sql.add(index("idx_ly_grant_owner", "texture_share_grants", "owner_id, state"));
        sql.add(index("idx_ly_grant_asset", "texture_share_grants", "asset_id, state"));
        sql.add(index("idx_ly_grant_tuple", "texture_share_grants",
                "owner_id, recipient_id, asset_id, state"));
        sql.add(index("idx_ly_binding_asset", "profile_texture_bindings", "asset_id"));
        sql.add(index("idx_ly_upload_user", "texture_uploads", "user_id, state"));
        sql.add(index("idx_ly_upload_asset", "texture_uploads", "asset_id, state"));
        sql.add(index("idx_ly_library_user", "library_entries", "user_id"));
        sql.add(index("idx_ly_session_user", "access_sessions", "user_id"));
        sql.add(index("idx_ly_session_profile", "access_sessions", "profile_id"));
        sql.add(index("idx_ly_session_expires", "access_sessions", "expires_at"));
        sql.add(index("idx_ly_ticket_lookup", "minecraft_join_tickets", "server_id, profile_id, expires_at"));
        sql.add(index("idx_ly_challenge_expires", "verification_challenges", "expires_at"));
        sql.add(index("idx_ly_job_state", "notification_jobs", "state, next_attempt_at"));
        sql.add(index("idx_ly_audit_time", "audit_events", "occurred_at"));
        sql.add(index("idx_ly_friend_request_to", "friend_requests", "to_user_id, state"));
        sql.add(index("idx_ly_access_event_user", "texture_access_events", "user_id, occurred_at"));

        return List.copyOf(sql);
    }

    private void seedMeta(DatabaseManager database, String key, String value) {
        Map<String, Object> existing = database.executeQuerySingle(
                "SELECT meta_key FROM instance_meta WHERE meta_key = ?", key);
        if (existing == null || existing.isEmpty()) {
            database.executeUpdate(
                    "INSERT INTO instance_meta (meta_key, meta_value) VALUES (?, ?)", key, value);
        } else {
            database.executeUpdate(
                    "UPDATE instance_meta SET meta_value = ? WHERE meta_key = ?", value, key);
        }
    }

    
    private String index(String name, String table, String columns) {
        String prefix = "sqlite".equals(dialect.id()) ? "CREATE INDEX IF NOT EXISTS " : "CREATE INDEX ";
        return prefix + name + " ON " + table + " (" + columns + ")";
    }
    private static String table(String name, String columns) {
        return "CREATE TABLE IF NOT EXISTS " + name + " (" + columns + ")";
    }

    
    private static boolean isAlreadyExists(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("already exists")) {
                return true;
            }
        }
        return false;
    }
}



