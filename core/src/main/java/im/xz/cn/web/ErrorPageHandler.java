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
package im.xz.cn.web;

import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import im.xz.cn.i18n.I18n;
import im.xz.cn.i18n.LocaleContext;
import im.xz.cn.logging.logApi;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ErrorPageHandler {
    private static final logApi log = logApi.getLogger(ErrorPageHandler.class);
    private static final Map<Integer, String> PAGE_CACHE = new ConcurrentHashMap<>();
    private static final int[] ERROR_STATUSES = {400, 401, 403, 404, 405, 409, 413, 429, 500, 502, 503};
    private static final Set<String> JSON_PATH_PREFIXES = Set.of("/api/", "/admin/api/");
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");
    private static final String BANNED_PAGE = loadResource("/errorpage/bannedPage.html");

    private ErrorPageHandler() {}

    public static Context html(Context ctx, int status) {
        ctx.attribute("errorPageRendered", true);
        return ctx.status(status).contentType("text/html; charset=utf-8").result(page(status));
    }

    public static Context banned(Context ctx) {
        ctx.attribute("errorPageRendered", true);
        String body = BANNED_PAGE.isEmpty() ? page(403) : renderTemplate(BANNED_PAGE);
        return ctx.status(403).contentType("text/html; charset=utf-8").result(body);
    }

    public static Context json(Context ctx, int status, Object body) {
        return ctx.status(status).json(body);
    }

    public static void register(RoutesConfig routes) {
        for (int status : ERROR_STATUSES) {
            routes.error(status, ctx -> handle(ctx, status));
        }
        routes.after(ErrorPageHandler::normalizeResponse);
    }

    public static void normalizeResponse(Context ctx) {
        int status = ctx.statusCode();
        if (status < 400) return;
        if (Boolean.TRUE.equals(ctx.attribute("errorPageRendered"))) return;
        String contentType = ctx.res().getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) return;
        handle(ctx, status);
    }

    public static Context handle(Context ctx, int status) {
        if (Boolean.TRUE.equals(ctx.attribute("errorPageRendered"))) return ctx;
        String contentType = ctx.res().getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) return ctx;
        if (isYggdrasilApiRequest(ctx)) {
            return json(ctx, status, Map.of(
                    "error", "NotFoundException",
                    "errorMessage", reason(status),
                    "cause", ""));
        }
        if (expectsJson(ctx)) {
            return json(ctx, status, Map.of(
                    "success", false,
                    "message", reason(status),
                    "status", status));
        }
        return html(ctx, status);
    }

    private static boolean isYggdrasilApiPath(String path) {
        return path.startsWith("/authserver/")
                || path.startsWith("/sessionserver/")
                || path.startsWith("/api/yggdrasil/")
                || path.startsWith("/api/profiles/lookup/minecraft")
                || path.startsWith("/api/user/profile/profiles/minecraft")
                || path.startsWith("/textures/");
    }

    private static boolean isYggdrasilApiRequest(Context ctx) {
        return isYggdrasilApiPath(ctx.path());
    }

    private static boolean expectsJson(Context ctx) {
        String path = ctx.path();
        String accept = ctx.header("Accept");
        return JSON_PATH_PREFIXES.stream().anyMatch(path::startsWith)
                || path.startsWith("/api/install")
                || (accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json"));
    }

    private static String page(int status) {
        return PAGE_CACHE.computeIfAbsent(status, ErrorPageHandler::loadPage);
    }

    private static String loadPage(int status) {
        String resource = "/errorpage/" + status + ".html";
        try (InputStream input = ErrorPageHandler.class.getResourceAsStream(resource)) {
            if (input == null) {
                log.error("Missing error page template: {}", resource);
                return "<!doctype html><html><head><title>" + status + " " + reason(status)
                        + "</title></head><body><h1>" + status + " " + reason(status) + "</h1></body></html>";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read error page template {}: {}", resource, e.getMessage(), e);
            return "<!doctype html><html><head><title>" + status + " " + reason(status)
                    + "</title></head><body><h1>" + status + " " + reason(status) + "</h1></body></html>";
        }
    }

    private static String renderTemplate(String template) {
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(I18n.t(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString().replace("__LANG__", LocaleContext.get());
    }

    private static String loadResource(String resource) {
        try (InputStream input = ErrorPageHandler.class.getResourceAsStream(resource)) {
            if (input == null) {
                log.error("Missing error page resource: {}", resource);
                return "";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read error page resource {}: {}", resource, e.getMessage(), e);
            return "";
        }
    }

    private static String reason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 413 -> "Payload Too Large";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }
}
