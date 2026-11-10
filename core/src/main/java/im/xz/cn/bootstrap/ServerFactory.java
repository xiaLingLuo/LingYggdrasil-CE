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
import im.xz.cn.config.ServerConfig;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.web.ErrorPageHandler;

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
        return createServer(port, staticDir, routeConfig);
    }

    public static void configureSessionCookie(JavalinConfig config) {
        configureSessionCookie(config, "JSESSIONID");
    }

    public static void configureSessionCookie(JavalinConfig config, String cookieName) {
        configureSessionCookie(config, cookieName, false);
    }

    
    public static void configureSessionCookie(JavalinConfig config, String cookieName, boolean secure) {
        config.jetty.modifyServletContextHandler(handler -> {
            org.eclipse.jetty.ee10.servlet.SessionHandler sessionHandler = handler.getSessionHandler();
            if (sessionHandler == null) {
                sessionHandler = new org.eclipse.jetty.ee10.servlet.SessionHandler();
                handler.setSessionHandler(sessionHandler);
            }
            sessionHandler.getSessionCookieConfig().setName(cookieName);
            sessionHandler.getSessionCookieConfig().setHttpOnly(true);
            sessionHandler.getSessionCookieConfig().setSecure(secure);
            sessionHandler.setSameSite(HttpCookie.SameSite.STRICT);
        });
    }

    public static boolean isHttpsDomain(String domain) {
        return domain != null && domain.trim().toLowerCase(java.util.Locale.ROOT).startsWith("https://");
    }

    public static java.util.List<String> configuredCorsOrigins() {
        java.util.List<String> origins = new java.util.ArrayList<>();
        String raw = im.xz.cn.config.SystemConfig.getInstance().getCorsOrigins();
        if (raw == null || raw.isBlank()) {
            for (String fallback : DEFAULT_CORS_ORIGINS) origins.add(fallback);
            return origins;
        }
        for (String line : raw.split("\\r?\\n")) {
            String origin = line.trim();
            if (!origin.isEmpty() && im.xz.cn.config.SystemConfig.isValidCorsOrigin(origin)) {
                origins.add(origin);
            }
        }
        if (origins.isEmpty()) {
            for (String fallback : DEFAULT_CORS_ORIGINS) origins.add(fallback);
        }
        return origins;
    }

    public static void configureCors(JavalinConfig config, int port) {
        ServerConfig serverConfig = ServerConfig.getInstance();
        boolean adminService = port == serverConfig.getAdminPort();
        boolean yggdrasilService = port == serverConfig.getYggdrasilPort();
        java.util.List<String> origins = new java.util.ArrayList<>();

        
        if (adminService) {
            String adminDomain = serverConfig.getAdminDomain();
            String adminOrigin = adminDomain;
            if (adminOrigin == null || adminOrigin.isBlank()
                    || sameOriginHost(adminOrigin, serverConfig.getEffectiveUserDomain())
                    || sameOriginHost(adminOrigin, serverConfig.getEffectiveYggdrasilDomain())) {
                
                adminOrigin = serviceOrigin(adminDomain == null || adminDomain.isBlank()
                        ? serverConfig.getEffectiveUserDomain() : adminDomain, port);
            }
            addCorsOrigin(origins, adminOrigin);
        } else {
            addCorsOrigin(origins, yggdrasilService
                    ? serverConfig.getEffectiveYggdrasilDomain()
                    : serverConfig.getEffectiveUserDomain());
            addCorsOrigin(origins, serverConfig.getCommonDomain());
            int[] siblingPorts = { serverConfig.getUserPort(), serverConfig.getYggdrasilPort(),
                    serverConfig.getAdminPort() };
            for (String custom : configuredCorsOrigins()) {
                boolean sibling = false;
                for (int siblingPort : siblingPorts) {
                    if (siblingPort != port && originPort(custom) == siblingPort) {
                        sibling = true;
                        break;
                    }
                }
                if (!sibling) {
                    addCorsOrigin(origins, custom);
                }
            }
        }
        addCorsOrigin(origins, "http://127.0.0.1:" + port);
        addCorsOrigin(origins, "http://localhost:" + port);
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            for (String origin : origins) {
                rule.allowHost(origin);
            }
            rule.allowCredentials = true;
        }));
    }

    
    private static boolean sameOriginHost(String a, String b) {
        String hostA = originHost(a);
        String hostB = originHost(b);
        return hostA != null && hostA.equalsIgnoreCase(hostB);
    }

    private static String originHost(String origin) {
        if (origin == null) return null;
        try {
            String host = java.net.URI.create(origin.trim()).getHost();
            return host == null || host.isBlank() ? null : host;
        } catch (RuntimeException error) {
            return null;
        }
    }

    
    private static String serviceOrigin(String origin, int port) {
        if (origin == null) return null;
        try {
            java.net.URI uri = java.net.URI.create(origin.trim());
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String host = uri.getHost();
            if (host == null || host.isBlank()) return null;
            String hostPart = host.contains(":") ? "[" + host + "]" : host;
            return scheme + "://" + hostPart + ":" + port;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static int originPort(String origin) {
        if (origin == null) return -1;
        try {
            java.net.URI uri = java.net.URI.create(origin.trim());
            return uri.getPort();
        } catch (RuntimeException error) {
            return -1;
        }
    }

    private static void addCorsOrigin(java.util.List<String> origins, String origin) {
        if (origin == null) return;
        String value = origin.trim();
        if (value.isEmpty() || origins.contains(value)) return;
        if (SystemConfig.isValidCorsOrigin(value)) {
            origins.add(value);
        }
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
            im.xz.cn.config.SystemConfig sysConfig = im.xz.cn.config.SystemConfig.getInstance();
            String nonce = im.xz.cn.web.Csp.newNonce();
            setHeader(ctx, "Content-Security-Policy", sysConfig.getHeaderCsp().replace("{nonce}", nonce));
            setHeader(ctx, "Strict-Transport-Security", sysConfig.getHeaderHsts());
            setHeader(ctx, "X-Content-Type-Options", sysConfig.getHeaderContentTypeOptions());
            setHeader(ctx, "X-Frame-Options", sysConfig.getHeaderFrameOptions());
            setHeader(ctx, "X-XSS-Protection", sysConfig.getHeaderXssProtection());
            setHeader(ctx, "Referrer-Policy", sysConfig.getHeaderReferrerPolicy());
            setHeader(ctx, "Permissions-Policy", sysConfig.getHeaderPermissionsPolicy());
            setHeader(ctx, "Cache-Control", sysConfig.getHeaderCacheControl());
        });
    }

    private static void setHeader(Context ctx, String name, String value) {
        if (value == null) return;
        String sanitized = value.replace("\r", "").replace("\n", "").trim();
        if (sanitized.isEmpty()) return;
        if (sanitized.length() > 4096) {
            sanitized = sanitized.substring(0, 4096);
        }
        ctx.header(name, sanitized);
    }

    public static void registerIconRoutes(RoutesConfig routes) {
        routes.get("/icons/{name}", ctx -> serveIcon(ctx, false));
        routes.get("/builtin-icons/{name}", ctx -> serveIcon(ctx, true));
        
        routes.get("/favicon.ico", ctx -> ctx.redirect("/icons/app.ico"));
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
            ErrorPageHandler.html(ctx, 404);
            return;
        }
        String relative = path.substring(prefix.length());
        if (relative.isEmpty() || relative.contains("..") || relative.indexOf('\\') >= 0) {
            ErrorPageHandler.html(ctx, 404);
            return;
        }
        InputStream in = ServerFactory.class.getResourceAsStream("/static/" + dir + "/" + relative);
        if (in == null) {
            ErrorPageHandler.html(ctx, 404);
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
            ErrorPageHandler.html(ctx, 404);
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

    private static Javalin createServer(int port, String staticDir, Consumer<RoutesConfig> routeConfig) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            configureSessionCookie(config, "LING_API_SESSION",
                    isHttpsDomain(im.xz.cn.config.ServerConfig.getInstance().getEffectiveYggdrasilDomain()));
            configureThreadLocalCleanup(config);

            configureCors(config, port);

            configureSecurityHeaders(config);

            
            
            configureErrorHandling(config);

            if (staticDir != null) {
                registerStaticRoutes(config.routes);
            }

            config.jsonMapper(new JavalinJackson());

            registerIconRoutes(config.routes);

            if (routeConfig != null) {
                routeConfig.accept(config.routes);
            }
            ErrorPageHandler.register(config.routes);
        });
        registerErrorMappers(app);
        return app;
    }

    
    public static void configureErrorHandling(JavalinConfig config) {
        config.router.handlerWrapper(endpoint -> ctx -> {
            try {
                endpoint.handler.handle(ctx);
            } catch (io.javalin.http.HttpResponseException httpError) {
                throw httpError;
            } catch (Exception error) {
                boolean badRequest = error instanceof ClassCastException
                        || error instanceof IllegalArgumentException
                        || error instanceof com.fasterxml.jackson.core.JsonProcessingException
                        || error instanceof io.javalin.util.JavalinBindException;
                if (badRequest) {
                    renderError(ctx, 400, "INVALID_ARGUMENT", "Invalid request body");
                } else {
                    log.error("[Server] unhandled exception on {} {}: {}",
                            ctx.method(), ctx.path(), error.getMessage(), error);
                    renderError(ctx, 500, "INTERNAL_ERROR", "Internal server error");
                }
            }
        });
    }

    
    private static void registerErrorMappers(Javalin app) {
        app.unsafe.internalRouter.addHttpExceptionHandler(io.javalin.util.JavalinBindException.class,
                (io.javalin.util.JavalinBindException error, Context ctx) -> renderError(ctx, 400, "INVALID_ARGUMENT", "Invalid request body"));
        app.unsafe.internalRouter.addHttpExceptionHandler(ClassCastException.class,
                (ClassCastException error, Context ctx) -> renderError(ctx, 400, "INVALID_ARGUMENT", "Invalid parameter type"));
        app.unsafe.internalRouter.addHttpExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class,
                (com.fasterxml.jackson.core.JsonProcessingException error, Context ctx) -> renderError(ctx, 400, "INVALID_ARGUMENT", "Invalid JSON body"));
        app.unsafe.internalRouter.addHttpExceptionHandler(IllegalArgumentException.class,
                (IllegalArgumentException error, Context ctx) -> renderError(ctx, 400, "INVALID_ARGUMENT",
                        error.getMessage() == null ? "Invalid argument" : error.getMessage()));
        app.unsafe.internalRouter.addHttpExceptionHandler(Exception.class, (Exception error, Context ctx) -> {
            log.error("[Server] unhandled exception on {} {}: {}",
                    ctx.method(), ctx.path(), error.getMessage(), error);
            renderError(ctx, 500, "INTERNAL_ERROR", "Internal server error");
        });
    }

    private static void renderError(Context ctx, int status, String code, String message) {
        if (ctx.res().isCommitted()) {
            return;
        }
        String path = ctx.path();
        boolean apiPath = path.startsWith("/api/")
                || path.startsWith("/admin/api/")
                || path.startsWith("/authserver/")
                || path.startsWith("/sessionserver/")
                || path.startsWith("/textures/");
        if (path.startsWith("/authserver/") || path.startsWith("/sessionserver/")) {
            ctx.status(status).json(java.util.Map.of(
                    "error", status == 400 ? "IllegalArgumentException" : "InternalServerException",
                    "errorMessage", message));
        } else if (apiPath) {
            ctx.status(status).json(java.util.Map.of("success", false, "code", code, "message", message));
        } else {
            ErrorPageHandler.handle(ctx, status);
        }
    }

    private static final im.xz.cn.logging.logApi log =
            im.xz.cn.logging.logApi.getLogger(ServerFactory.class);
}
