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
package im.xz.cn.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class MigrationReport {

    private final String stepId;
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    public MigrationReport(String stepId) {
        this.stepId = stepId;
    }

    public String stepId() {
        return stepId;
    }

    public void add(String counter, long delta) {
        counters.merge(counter, delta, Long::sum);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public Map<String, Long> counters() {
        return Map.copyOf(counters);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public long get(String counter) {
        return counters.getOrDefault(counter, 0L);
    }

    
    public boolean hasFailures() {
        return get("failedTextures") > 0 || get("failedFiles") > 0
                || get("skippedMissingFiles") > 0;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        builder.append("step=").append(stepId).append('\n');
        counters.forEach((key, value) -> builder.append("  ").append(key).append('=').append(value).append('\n'));
        if (!warnings.isEmpty()) {
            builder.append("  warnings=").append(warnings.size()).append('\n');
            for (String warning : warnings) {
                builder.append("    - ").append(warning).append('\n');
            }
        }
        return builder.toString();
    }
}
