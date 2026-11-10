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
package im.xz.cn.migration.plan;

import java.time.Instant;
import java.util.List;


public record MigrationPlan(
        String databaseType,
        boolean installed,
        boolean newSchemaReady,
        List<Step> steps,
        List<String> warnings,
        Instant generatedAt
) {

    public record Step(String command, String description, long items) {
    }

    public MigrationPlan {
        steps = List.copyOf(steps);
        warnings = List.copyOf(warnings);
    }

    public String toText() {
        StringBuilder text = new StringBuilder();
        text.append("plan generated=").append(generatedAt).append('\n');
        text.append("database=").append(databaseType)
                .append(" installed=").append(installed)
                .append(" newSchemaReady=").append(newSchemaReady).append('\n');
        if (steps.isEmpty()) {
            text.append("steps: none\n");
        } else {
            text.append("steps:\n");
            for (Step step : steps) {
                text.append("  - ").append(step.command())
                        .append(": ").append(step.description())
                        .append(" (items=").append(step.items()).append(")\n");
            }
        }
        for (String warning : warnings) {
            text.append("warning: ").append(warning).append('\n');
        }
        return text.toString();
    }
}
