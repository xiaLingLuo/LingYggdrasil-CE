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
package im.xz.cn.migration.steps.social;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.MigrationReport;

import java.time.Instant;
import java.util.List;


public final class SocialMigrationStep {

    public static final String STEP_ID = "v2_3_16.social.001";

    private final DatabaseManager database;

    public SocialMigrationStep(DatabaseManager database) {
        this.database = database;
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        migrateFriendships(report);
        migrateRequests(report);
        migrateBlocks(report);
        return report;
    }

    private void migrateFriendships(MigrationReport report) {
        List<String[]> rows = Jdbc.queryMany(database, resultSet -> new String[]{
                resultSet.getString("user_id_lower"),
                resultSet.getString("user_id_higher"),
                resultSet.getString("created_at")
        }, "SELECT user_id_lower, user_id_higher, created_at FROM friends ORDER BY created_at, id");
        for (String[] row : rows) {
            report.add("friendshipsSeen", 1);
            String low = row[0];
            String high = row[1];
            if (low == null || high == null || low.equals(high)) {
                report.add("skippedFriendships", 1);
                report.warn("invalid friendship pair skipped");
                continue;
            }
            String first = low.compareTo(high) <= 0 ? low : high;
            String second = low.compareTo(high) <= 0 ? high : low;
            long existing = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                    first, second);
            if (existing > 0) {
                continue;
            }
            Jdbc.update(database,
                    "INSERT INTO friendships (user_low_id, user_high_id, created_at) VALUES (?, ?, ?)",
                    first, second, createdAt(row[2]));
            report.add("friendshipsMigrated", 1);
        }
    }

    private void migrateRequests(MigrationReport report) {
        List<String[]> rows = Jdbc.queryMany(database, resultSet -> new String[]{
                resultSet.getString("sender_id"),
                resultSet.getString("receiver_id"),
                resultSet.getString("created_at")
        }, "SELECT sender_id, receiver_id, created_at FROM confirming_friends ORDER BY created_at, id");
        for (String[] row : rows) {
            report.add("requestsSeen", 1);
            String from = row[0];
            String to = row[1];
            if (from == null || to == null || from.equals(to)) {
                report.add("skippedRequests", 1);
                report.warn("invalid friend request skipped");
                continue;
            }
            long existing = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM friend_requests WHERE from_user_id = ? AND to_user_id = ?"
                            + " AND state = 'PENDING'", from, to);
            if (existing > 0) {
                continue;
            }
            Jdbc.update(database,
                    "INSERT INTO friend_requests (from_user_id, to_user_id, state, created_at)"
                            + " VALUES (?, ?, 'PENDING', ?)", from, to, createdAt(row[2]));
            report.add("requestsMigrated", 1);
        }
    }

    private void migrateBlocks(MigrationReport report) {
        List<String[]> rows = Jdbc.queryMany(database, resultSet -> new String[]{
                resultSet.getString("blocker_id"),
                resultSet.getString("blocked_id"),
                resultSet.getString("created_at")
        }, "SELECT blocker_id, blocked_id, created_at FROM blocked_users ORDER BY created_at, id");
        for (String[] row : rows) {
            report.add("blocksSeen", 1);
            String blocker = row[0];
            String blocked = row[1];
            if (blocker == null || blocked == null || blocker.equals(blocked)) {
                report.add("skippedBlocks", 1);
                report.warn("invalid block relation skipped");
                continue;
            }
            long existing = Jdbc.queryCount(database,
                    "SELECT COUNT(*) FROM user_blocks WHERE blocker_id = ? AND blocked_id = ?",
                    blocker, blocked);
            if (existing > 0) {
                continue;
            }
            Jdbc.update(database,
                    "INSERT INTO user_blocks (blocker_id, blocked_id, created_at) VALUES (?, ?, ?)",
                    blocker, blocked, createdAt(row[2]));
            report.add("blocksMigrated", 1);
        }
    }

    private static String createdAt(String legacy) {
        return legacy == null || legacy.isBlank() ? Jdbc.writeInstant(Instant.now()) : legacy;
    }
}
