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

import im.xz.cn.i18n.I18n;
import im.xz.cn.social.SocialIdentity;
import im.xz.cn.texture.publication.PublicTextureSummary;
import im.xz.cn.web.PageRenderer;
import im.xz.cn.web.Shared;

import java.util.List;
import java.util.function.Function;


public final class UserHomePage {

    private UserHomePage() {
    }

    public static String renderProfile(String csrfToken, String siteName, SocialIdentity identity,
                                       List<PublicTextureSummary> items,
                                       Function<String, String> textureUrlResolver) {
        String header = "<div class='card'>"
                + "<div style='display:flex;gap:16px;align-items:center'>"
                + "<img src='" + PageRenderer.escapeHtml(identity.avatarUrl())
                + "' alt='' onerror=\"this.onerror=null;this.src='/img/juststeve_face.png'\""
                + " style='width:72px;height:72px;border-radius:14px;background:#fff0f5;object-fit:contain'>"
                + "<div><div style='font-size:20px;font-weight:600'>"
                + PageRenderer.escapeHtml(identity.displayName()) + "</div>"
                + "<div class='text-muted' style='font-size:12px'>"
                + PageRenderer.escapeHtml(identity.userCode() == null ? "" : identity.userCode())
                + "</div></div></div></div>";
        String toolbar = "<div class='world-toolbar' style='margin-top:16px'>"
                + "<div class='world-tabs' role='tablist'>"
                + "<button type='button' class='btn btn-secondary' data-world-lowend>低端显卡支持</button>"
                + "</div></div>";

        StringBuilder grid = new StringBuilder();
        grid.append("<div id='userhomeGrid' style='display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:14px;margin-top:16px'>");
        for (PublicTextureSummary item : items) {
            String url = textureUrlResolver.apply(item.type().name() + "/" + item.hash());
            String safeUrl = PageRenderer.escapeHtml(url == null ? "" : url);
            grid.append("<a class='card' href='/userhome/")
                    .append(PageRenderer.escapeHtml(identity.userCode()))
                    .append("/public/").append(PageRenderer.escapeHtml(item.hash()))
                    .append("?type=").append(item.type().name()).append("'")
                    .append(" style='text-decoration:none;color:inherit;padding:10px'>")
                    .append("<div style='height:150px;display:flex;align-items:center;justify-content:center;background:#fff7fb;border-radius:10px;overflow:hidden'>")
                    .append("<canvas data-texture-3d data-type='").append(PageRenderer.escapeHtml(item.type().name()))
                    .append("' data-url='").append(safeUrl)
                    .append("' style='width:100%;height:100%;display:block'></canvas>")
                    .append("</div>")
                    .append("<div style='margin-top:8px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap'>")
                    .append(PageRenderer.escapeHtml(item.displayName())).append("</div>")
                    .append("<div class='text-muted' style='font-size:12px'>")
                    .append(I18n.t("userhome.author")).append(": ")
                    .append(PageRenderer.escapeHtml(item.authorName())).append("</div>")
                    .append("<div class='text-muted' style='font-size:12px'>")
                    .append(I18n.t("userhome.likes")).append(" ").append(item.likes())
                    .append(" · ").append(I18n.t("userhome.favorites")).append(" ").append(item.favorites())
                    .append("</div></a>");
        }
        grid.append("</div>");
        grid.append("""
            <div id="userhomeMoreGrid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:14px;margin-top:14px"></div>
            <div style="margin-top:16px"><button type="button" class="btn btn-secondary" id="userhomeMore">__MORE_LABEL__</button></div>
            <script>
            (function () {
                var offset = __OFFSET__;
                var busy = false;
                var button = document.getElementById('userhomeMore');
                if (!button) return;
                button.addEventListener('click', function () {
                    if (busy) return;
                    busy = true;
                    fetch('/api/userhomes/__CODE__/publications?limit=20&offset=' + offset)
                        .then(function (r) { return r.json(); })
                        .then(function (data) {
                            var target = document.getElementById('userhomeMoreGrid');
                            var items = data.items || [];
                            items.forEach(function (item) {
                                var link = document.createElement('a');
                                link.className = 'card';
                                link.style.textDecoration = 'none';
                                link.style.color = 'inherit';
                                link.style.padding = '10px';
                                link.href = '/userhome/__CODE__/public/' + encodeURIComponent(item.hash)
                                    + '?type=' + encodeURIComponent(item.type);
                                link.innerHTML = '<div style="height:150px;display:flex;align-items:center;'
                                    + 'justify-content:center;background:#fff7fb;border-radius:10px;overflow:hidden">'
                                    + '<canvas data-texture-3d data-type="' + item.type + '" data-url="/api/v2/texture-files/'
                                    + encodeURIComponent(item.type) + '/' + encodeURIComponent(item.hash)
                                    + '" style="width:100%;height:100%;display:block"></canvas></div>'
                                    + '<div style="margin-top:8px;font-weight:600">' + item.displayName + '</div>';
                                target.appendChild(link);
                                offset++;
                            });
                            if (window.Texture3D) window.Texture3D.hydrate(target);
                            busy = false;
                            if (items.length < 20) button.style.display = 'none';
                        })
                        .catch(function () { busy = false; });
                });
            })();
            </script>
            """.replace("__MORE_LABEL__", PageRenderer.escapeHtml(I18n.t("userhome.more")))
                .replace("__OFFSET__", Integer.toString(items.size()))
                .replace("__CODE__", PageRenderer.escapeHtml(identity.userCode())));
        if (items.isEmpty()) {
            grid.append("<p class='text-muted' style='margin-top:16px'>")
                    .append(I18n.t("userhome.empty")).append("</p>");
        }

        String content = header + toolbar + grid + """
                <script src="/js/skinview3d.bundle.js"></script>
                <script src="/js/texture-3d.js"></script>
                <script>
                (function () {
                    if (!window.Texture3D) return;
                    window.Texture3D.hydrate(document.getElementById('userhomeGrid'));
                    window.Texture3D.bindToggle(document.querySelector('[data-world-lowend]'), function () {
                        location.reload();
                    });
                })();
                </script>
                """;
        String body = Shared.buildPublicLayout(siteName, content);
        return PageRenderer.renderPage(I18n.t("userhome.title"), Shared.csrfInject(csrfToken) + body,
                "user", Css.getUserCssLink(), Css.getUserDashboardCss(), Css.getTextureCss());
    }

    public static String renderPublication(String csrfToken, String siteName, SocialIdentity identity,
                                           PublicTextureSummary item, String textureUrl) {
        String content = "<div class='card'>"
                + "<div style='display:flex;gap:20px;flex-wrap:wrap'>"
                + "<div style='width:260px;min-height:260px;background:#fff7fb;border-radius:14px;display:flex;align-items:center;justify-content:center'>"
                + "<canvas id='userhomeDetail3d' data-texture-3d data-type='" + PageRenderer.escapeHtml(item.type().name())
                + "' data-url='" + PageRenderer.escapeHtml(textureUrl == null ? "" : textureUrl)
                + "' style='width:260px;height:260px;display:block'></canvas></div>"
                + "<div style='flex:1;min-width:220px'>"
                + "<div style='font-size:22px;font-weight:600'>"
                + PageRenderer.escapeHtml(item.displayName()) + "</div>"
                + "<div class='text-muted' style='margin-top:6px'>"
                + I18n.t("userhome.author") + ": " + PageRenderer.escapeHtml(item.authorName()) + "</div>"
                + "<div class='text-muted' style='margin-top:6px'>"
                + I18n.t("userhome.likes") + " " + item.likes()
                + " · " + I18n.t("userhome.favorites") + " " + item.favorites()
                + " · " + I18n.t("userhome.heat") + " " + item.heat() + "</div>"
                + "<div class='text-muted' style='margin-top:6px'>HASH: "
                + PageRenderer.escapeHtml(item.hash()) + "</div>"
                + "<div style='margin-top:18px' class='material-detail-actions'>"
                + "<button type='button' class='btn btn-secondary' id='userhomeLike'>"
                + I18n.t("userhome.likes") + " " + item.likes() + "</button>"
                + "<button type='button' class='btn btn-secondary' id='userhomeFavorite'>"
                + I18n.t("userhome.favorites") + " " + item.favorites() + "</button>"
                + "<a class='btn btn-secondary' href='" + PageRenderer.escapeHtml(textureUrl == null ? "" : textureUrl)
                + "' target='_blank' rel='noopener'>下载</a>"
                + "<a class='btn btn-secondary' href='/userhome/"
                + PageRenderer.escapeHtml(identity.userCode()) + "'>"
                + I18n.t("userhome.back") + "</a></div>"
                + "</div></div></div>"
                + "<script>(function(){var id=" + item.publicationId() + ";"
                + "function headers(){var h={'Content-Type':'application/json'};"
                + "if(window.CSRF_TOKEN)h['X-CSRF-Token']=window.CSRF_TOKEN;return h;}"
                + "function toggle(kind,button,active,count){var next=!active;"
                + "fetch('/api/v2/publications/'+id+'/'+kind,{method:next?'POST':'DELETE',headers:headers(),"
                + "body:next?'{}':undefined}).then(function(r){if(r.status===401){location.href='/login';return null;}"
                + "return r.json();}).then(function(d){if(!d||!d.success){return;}"
                + "button.textContent=(kind==='like'?'点赞 ':'收藏 ')+(kind==='like'?d.likes:d.favorites);"
                + "button.dataset.active=next?'1':'0';button.className='btn '+(next?'btn-primary':'btn-secondary');});}"
                + "var l=document.getElementById('userhomeLike'),f=document.getElementById('userhomeFavorite');"
                + "l.addEventListener('click',function(){toggle('like',l,l.dataset.active==='1',0);});"
                + "f.addEventListener('click',function(){toggle('favorite',f,f.dataset.active==='1',0);});"
                + "})();</script>"
                + "<script src='/js/skinview3d.bundle.js'></script>"
                + "<script src='/js/texture-3d.js'></script>"
                + "<script>(function(){var c=document.getElementById('userhomeDetail3d');"
                + "if(c&&window.Texture3D){Texture3D.mount(c,c.getAttribute('data-type'),"
                + "c.getAttribute('data-url'),{force:true,rotate:true});}})();</script>";
        String body = Shared.buildPublicLayout(siteName, content);
        return PageRenderer.renderPage(PageRenderer.escapeHtml(item.displayName()),
                Shared.csrfInject(csrfToken) + body, "user",
                Css.getUserCssLink(), Css.getWorldDetailCss());
    }
}
