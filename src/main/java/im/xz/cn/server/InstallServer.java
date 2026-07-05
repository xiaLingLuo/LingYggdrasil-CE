package im.xz.cn.server;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import im.xz.cn.server.handler.InstallHandler;
import im.xz.cn.util.FooterInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallServer {
    private static final Logger logger = LoggerFactory.getLogger(InstallServer.class);

    private Javalin app;
    private static final String TOKEN_FILE = ".INSTALL_TOKEN";

    public void start() {
        String token = generateOrLoadToken();
        InstallHandler.setInstallToken(token);
        InstallHandler.setInstallServer(this);

        int port = 35598;

        //暂时禁用安装页面安全认证
        //logger.info("[InstallServer] 安装地址: http://localhost:{}/?token={}", port, token);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "text/html; charset=utf-8";

            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
                rule.allowHost(
                    "http://localhost:35565",
                    "http://localhost:35577",
                    "http://localhost:35599",
                    "http://localhost:35598"
                );
                rule.allowCredentials = true;
            }));
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.jsonMapper(new JavalinJackson());

            config.routes.get("/", InstallHandler::renderInstallPage);
            config.routes.get("/api/status", InstallHandler::getStatus);
            config.routes.post("/api/install", InstallHandler::doInstall);

            config.routes.get("/api/footer-info", ctx -> ctx.json(FooterInfo.getFooterData()));
        });

        app.start("127.0.0.1", port);
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
