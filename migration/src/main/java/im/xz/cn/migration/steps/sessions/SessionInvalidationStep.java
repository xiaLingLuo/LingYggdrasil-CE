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
package im.xz.cn.migration.steps.sessions;

import im.xz.cn.database.DatabaseManager;
import im.xz.cn.infrastructure.persistence.Jdbc;
import im.xz.cn.migration.MigrationReport;


public final class SessionInvalidationStep {

    public static final String STEP_ID = "v2_3_16.sessions.001";

    private final DatabaseManager database;

    public SessionInvalidationStep(DatabaseManager database) {
        this.database = database;
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport(STEP_ID);
        long tokens = Jdbc.queryCount(database, "SELECT COUNT(*) FROM auth_tokens");
        report.add("legacyTokensSeen", tokens);
        if (tokens > 0) {
            Jdbc.update(database, "DELETE FROM auth_tokens");
            report.add("legacyTokensInvalidated", tokens);
            report.warn("all legacy sessions invalidated; users must sign in again after upgrade");
        }
        report.add("activeSessionsRemaining",
                Jdbc.queryCount(database, "SELECT COUNT(*) FROM access_sessions"));
        report.add("pendingJoinTicketsRemaining",
                Jdbc.queryCount(database, "SELECT COUNT(*) FROM minecraft_join_tickets"));
        return report;
    }
}
