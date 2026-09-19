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
package im.xz.cn.bootstrap;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.json.JavalinJackson;
import io.javalin.router.Endpoint;
import im.xz.cn.common.AppIcons;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

import org.eclipse.jetty.http.HttpCookie;

public class ServerFactory {

    private static final String[] DEFAULT_CORS_ORIGINS = {
        "http://localhost:35565",
        "http://localhost:35577",
        "http://localhost:35599",
        "http://localhost:35598"
    };

    public static Javalin create(int port, String staticDir) {
        return create(port, staticDir, null);
    }

    public static Javalin create(int port, String staticDir, Consumer<RoutesConfig> routeConfig) {
        return create(port, staticDir, routeConfig, DEFAULT_CORS_ORIGINS);
    }

    public static void configureSessionCookie(JavalinConfig config) {
        configureSessionCookie(config, "JSESSIONID");
    }

    public static void configureSessionCookie(JavalinConfig config, String cookieName) {
        config.jetty.modifyServletContextHandler(handler -> {
            org.eclipse.jetty.ee10.servlet.SessionHandler sessionHandler = handler.getSessionHandler();
            if (sessionHandler == null) {
                sessionHandler = new org.eclipse.jetty.ee10.servlet.SessionHandler();
                handler.setSessionHandler(sessionHandler);
            }
            sessionHandler.getSessionCookieConfig().setName(cookieName);
            sessionHandler.getSessionCookieConfig().setHttpOnly(true);
            sessionHandler.getSessionCookieConfig().setSecure(true);
            sessionHandler.setSameSite(HttpCookie.SameSite.STRICT);
        });
    }

    public static void configureThreadLocalCleanup(JavalinConfig config) {
        config.jetty.modifyServletContextHandler(handler ->
            handler.addFilter((request, response, chain) -> {
                try {
                    chain.doFilter(request, response);
                } finally {
                    im.xz.cn.i18n.LocaleContext.clear();
                    im.xz.cn.security.AdminPermissions.clear();
                    im.xz.cn.security.UserPermissions.clear();
                    im.xz.cn.web.Csp.clear();
                    im.xz.cn.logging.ServiceLog.clear();
                }
            }, "/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST))
        );
    }

    public static void configureSecurityHeaders(JavalinConfig config) {
        config.routes.before(ctx -> {
            String nonce = im.xz.cn.web.Csp.newNonce();
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("X-XSS-Protection", "0");
            ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
            ctx.header("Cache-Control", "no-store");
            ctx.header("Content-Security-Policy",
                    "default-src 'self'; "
                    + "script-src 'self' 'nonce-" + nonce + "'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data: https: http:; "
                    + "font-src 'self' data:; "
                    + "connect-src 'self'; "
                    + "frame-ancestors 'none'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'; "
                    + "object-src 'none';");
            ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            ctx.header("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
        });
    }

    public static void registerIconRoutes(RoutesConfig routes) {
        routes.get("/icons/{name}", ctx -> serveIcon(ctx, false));
        routes.get("/builtin-icons/{name}", ctx -> serveIcon(ctx, true));
    }

    private static final String[] STATIC_DIRECTORIES = {"css", "js", "img", "fonts"};

    private static final java.util.Map<String, String> STATIC_MIME = java.util.Map.ofEntries(
        java.util.Map.entry("css", "text/css"),
        java.util.Map.entry("js", "text/javascript"),
        java.util.Map.entry("mjs", "text/javascript"),
        java.util.Map.entry("json", "application/json"),
        java.util.Map.entry("xml", "text/xml"),
        java.util.Map.entry("svg", "image/svg+xml"),
        java.util.Map.entry("ico", "image/x-icon"),
        java.util.Map.entry("png", "image/png"),
        java.util.Map.entry("jpg", "image/jpeg"),
        java.util.Map.entry("jpeg", "image/jpeg"),
        java.util.Map.entry("gif", "image/gif"),
        java.util.Map.entry("webp", "image/webp"),
        java.util.Map.entry("woff2", "font/woff2"),
        java.util.Map.entry("woff", "font/woff"),
        java.util.Map.entry("ttf", "font/ttf"),
        java.util.Map.entry("txt", "text/plain")
    );

    public static void registerStaticRoutes(RoutesConfig routes) {
        for (String dir : STATIC_DIRECTORIES) {
            routes.get("/" + dir + "/*", ctx -> serveStatic(ctx, dir));
        }
    }

    private static void serveStatic(Context ctx, String dir) {
        String prefix = "/" + dir + "/";
        String path = ctx.path();
        if (!path.startsWith(prefix)) {
            ctx.status(404).result("Not Found");
            return;
        }
        String relative = path.substring(prefix.length());
        if (relative.isEmpty() || relative.contains("..") || relative.indexOf('\\') >= 0) {
            ctx.status(404).result("Not Found");
            return;
        }
        InputStream in = ServerFactory.class.getResourceAsStream("/static/" + dir + "/" + relative);
        if (in == null) {
            ctx.status(404).result("Not Found");
            return;
        }
        String ext = relative.contains(".")
                ? relative.substring(relative.lastIndexOf('.') + 1).toLowerCase()
                : "";
        ctx.contentType(STATIC_MIME.getOrDefault(ext, "application/octet-stream"));
        ctx.result(in);
    }

    private static final HandlerType[] PLUGIN_CATCH_ALL_METHODS = {
        HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.DELETE, HandlerType.PATCH
    };

    public static void registerPluginCatchAll(Javalin app, boolean userServer) {
        im.xz.cn.plugin.PluginManager manager = im.xz.cn.plugin.PluginManager.getInstance();
        manager.captureBuiltinRoutes(app.unsafe.internalRouter, userServer);

        Handler handler = ctx -> {
            HandlerType method = ctx.method();
            if (method == HandlerType.GET || method == HandlerType.HEAD) {
                ResourceHandler resources = app.unsafe.resourceHandler;
                if (resources != null && resources.canHandle(ctx) && resources.handle(ctx)) {
                    return;
                }
            }
            boolean handled = userServer
                    ? manager.handleUserRequest(ctx)
                    : manager.handleYggdrasilRequest(ctx);
            if (!handled) throw new NotFoundResponse();
        };
        for (HandlerType method : PLUGIN_CATCH_ALL_METHODS) {
            app.unsafe.internalRouter.addHttpEndpoint(new Endpoint(method, "/*", handler));
        }
    }

    private static void serveIcon(Context ctx, boolean builtinOnly) {
        String name = ctx.pathParam("name");
        if (!builtinOnly && AppIcons.hasExternal()) {
            File external = AppIcons.externalFile(name);
            if (external != null) {
                ctx.contentType(iconContentType(name));
                try {
                    ctx.result(new FileInputStream(external));
                    return;
                } catch (Exception ignored) {
                }
            }
        }
        InputStream builtin = AppIcons.builtinStream(name);
        if (builtin == null) {
            ctx.status(404).result("Icon not found");
            return;
        }
        ctx.contentType(iconContentType(name));
        ctx.result(builtin);
    }

    private static String iconContentType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".ico")) return "image/x-icon";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    public static Javalin create(int port, String staticDir, Consumer<RoutesConfig> routeConfig, String[] corsOrigins) {
        return Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            configureSessionCookie(config, "LING_API_SESSION");
            configureThreadLocalCleanup(config);

            String[] origins = (corsOrigins != null && corsOrigins.length > 0) ? corsOrigins : DEFAULT_CORS_ORIGINS;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    for (String origin : origins) {
                        rule.allowHost(origin);
                    }
                    rule.allowCredentials = true;
                });
            });

            configureSecurityHeaders(config);

            if (staticDir != null) {
                registerStaticRoutes(config.routes);
            }

            config.jsonMapper(new JavalinJackson());

            registerIconRoutes(config.routes);

            if (routeConfig != null) {
                routeConfig.accept(config.routes);
            }
        });
    }
}
