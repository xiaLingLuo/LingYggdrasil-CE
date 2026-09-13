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

package im.xz.cn.web.view;

import static im.xz.cn.web.Shared.esc;
import im.xz.cn.config.SystemConfig;
import im.xz.cn.i18n.I18n;
import im.xz.cn.i18n.LocaleContext;
import im.xz.cn.model.User;
import im.xz.cn.web.PageRenderer;
import im.xz.cn.web.Shared;

public class HomePage {

    public static String renderHomePage(User user, String csrfToken) {
        SystemConfig sysConfig = SystemConfig.getInstance();
        String siteName = sysConfig.getSiteName();

        String accountBlock;
        if (user != null) {
            accountBlock = """
                <div class="account-banner">
                    <span class="account-greeting">%s</span>
                    <div class="account-actions">
                        <a href="/dashboard" class="btn btn-primary">%s</a>
                        <a href="/logout" class="btn btn-secondary">%s</a>
                    </div>
                </div>
                """.formatted(
                    I18n.t("home.welcome", esc(user.getDisplayName())),
                    I18n.t("home.dashboard"),
                    I18n.t("home.logout"));
        } else {
            accountBlock = """
                <div class="hero-actions">
                    <a href="/login" class="btn btn-primary">%s</a>
                </div>
                """.formatted(I18n.t("home.loginRegister"));
        }

        String template = I18n.homeTemplate(LocaleContext.get());
        template = template.replace("{{account}}", accountBlock);
        template = template.replace("{{siteName}}", esc(siteName));
        template = PageRenderer.tr(template);

        String body = PageRenderer.renderNavbar(siteName, "home", false)
                + "<div class=\"petal-bg\" id=\"petalContainer\"></div>"
                + "<div class=\"main-container\">"
                + template
                + """
                </div>
                <script>
                  (function() {
                    var container = document.getElementById('petalContainer');
                    if (!container) return;
                    for (var i = 0; i < 25; i++) {
                      var petal = document.createElement('div');
                      petal.className = 'petal';
                      var size = Math.floor(Math.random() * 18 + 10);
                      petal.style.width = size + 'px';
                      petal.style.height = size + 'px';
                      petal.style.left = (Math.random() * 100) + '%';
                      petal.style.top = (Math.random() * 100) + '%';
                      petal.style.animationDelay = (Math.random() * 15) + 's';
                      petal.style.animationDuration = (Math.random() * 12 + 10) + 's';
                      petal.style.opacity = (Math.random() * 0.3 + 0.1);
                      container.appendChild(petal);
                    }
                  })();
                </script>
                """;

        if (csrfToken != null && !csrfToken.isBlank()) {
            body = Shared.csrfInject(csrfToken) + body;
        }

        return PageRenderer.renderPage(siteName, body, "user",
                Css.getUserCssLink(), Css.getHomeCss());
    }
}
