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
package im.xz.cn.server;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.config.RoutesConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;

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
        config.jetty.modifyServletContextHandler(handler -> {
            org.eclipse.jetty.ee10.servlet.SessionHandler sessionHandler = handler.getSessionHandler();
            if (sessionHandler == null) {
                sessionHandler = new org.eclipse.jetty.ee10.servlet.SessionHandler();
                handler.setSessionHandler(sessionHandler);
            }
            sessionHandler.getSessionCookieConfig().setHttpOnly(true);
            sessionHandler.getSessionCookieConfig().setSecure(true);
            sessionHandler.setSameSite(HttpCookie.SameSite.STRICT);
        });
    }

    public static Javalin create(int port, String staticDir, Consumer<RoutesConfig> routeConfig, String[] corsOrigins) {
        return Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            configureSessionCookie(config);

            String[] origins = (corsOrigins != null && corsOrigins.length > 0) ? corsOrigins : DEFAULT_CORS_ORIGINS;
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    for (String origin : origins) {
                        rule.allowHost(origin);
                    }
                    rule.allowCredentials = true;
                });
            });

            config.routes.before(ctx -> {
                ctx.header("X-Content-Type-Options", "nosniff");
                ctx.header("X-Frame-Options", "DENY");
                ctx.header("X-XSS-Protection", "0");
                ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
                ctx.header("Cache-Control", "no-store");
                ctx.header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';");
                ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
                ctx.header("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
            });

            if (staticDir != null) {
                config.staticFiles.add(staticDir, Location.CLASSPATH);
            }

            config.jsonMapper(new JavalinJackson());

            if (routeConfig != null) {
                routeConfig.accept(config.routes);
            }
        });
    }
}
