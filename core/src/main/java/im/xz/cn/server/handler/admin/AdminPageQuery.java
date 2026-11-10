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
package im.xz.cn.server.handler.admin;

import io.javalin.http.Context;

import java.util.Set;

public record AdminPageQuery(int requestedPage, int pageSize, String search) {
    private static final Set<Integer> PAGE_SIZES = Set.of(50, 100, 200, 500, 1000);
    private static final int MAX_SEARCH_LENGTH = 200;

    public static AdminPageQuery from(Context ctx) {
        int page = parsePositiveInt(ctx.queryParam("page"), 1);
        int requestedSize = parsePositiveInt(ctx.queryParam("pageSize"), 100);
        int pageSize = PAGE_SIZES.contains(requestedSize) ? requestedSize : 100;
        String search = ctx.queryParam("q");
        search = search == null ? "" : search.trim();
        if (search.length() > MAX_SEARCH_LENGTH) search = search.substring(0, MAX_SEARCH_LENGTH);
        return new AdminPageQuery(page, pageSize, search);
    }

    public int pageForTotal(int total) {
        int pageCount = Math.max(1, (int) Math.ceil((double) total / pageSize));
        return Math.min(requestedPage, pageCount);
    }

    public long offsetForPage(int page) {
        return (long) (page - 1) * pageSize;
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
