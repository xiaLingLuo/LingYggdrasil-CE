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
import io.javalin.json.JavalinJackson;
import im.xz.cn.i18n.LocaleContext;
import im.xz.cn.i18n.LocaleResolver;
import im.xz.cn.server.handler.install.InstallHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import im.xz.cn.logging.logApi;

public class InstallServer {
    private static final logApi logger = logApi.getLogger(InstallServer.class);

    private static final String TOKEN_FILE = ".INSTALL_TOKEN";
    private static final String BIND_HOST = "127.0.0.1";
    private static final int PORT = 35598;

    private Javalin app;

    public void start() {
        String token = generateOrLoadToken();
        InstallHandler.setInstallToken(token);
        InstallHandler.setInstallServer(this);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";
            config.jsonMapper(new JavalinJackson());

            config.routes.before(ctx -> {
                String lang = ctx.cookie("LING_INSTALL_LANG");
                if (lang == null) lang = LocaleResolver.fromAcceptLanguage(ctx.header("Accept-Language"));
                LocaleContext.set(LocaleResolver.normalize(lang));
            });
            config.routes.after(ctx -> LocaleContext.clear());

            config.routes.get("/", InstallHandler::renderInstallPage);
            config.routes.get("/api/status", InstallHandler::getStatus);
            config.routes.post("/api/install", InstallHandler::doInstall);
        });

        app.start(BIND_HOST, PORT);
        logger.info("[InstallServer] 安装向导已启动: http://{}:{}", BIND_HOST, PORT);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            logger.info("[InstallServer] 安装向导已停止");
        }
    }

    private String generateOrLoadToken() {
        try {
            Path path = Path.of(TOKEN_FILE);
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            }
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            Files.writeString(path, token);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate install token", e);
        }
    }

    public static void deleteTokenFile() {
        try {
            Files.deleteIfExists(Path.of(TOKEN_FILE));
        } catch (Exception e) {
            logger.error("Failed to delete install token file: {}", e.getMessage(), e);
        }
    }
}
