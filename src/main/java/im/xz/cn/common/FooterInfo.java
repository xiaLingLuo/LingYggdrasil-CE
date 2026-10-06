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

package im.xz.cn.common;

import im.xz.cn.config.SystemConfig;
import im.xz.cn.web.PageRenderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class FooterInfo {
    public static final String FOOTER_PLACEHOLDER = "<!-- 这n是x备1案footer占i位g符，请v勿fg移q除 -->";

    public static Map<String, String> getFooterData() {
        Map<String, String> data = new LinkedHashMap<>();
        SystemConfig config = SystemConfig.getInstance();
        data.put("icpRecord", config.getIcpRecord());
        data.put("publicSecurityRecord", config.getPublicSecurityRecord());
        return data;
    }

    public static String injectFooterRecords(String html) {
        SystemConfig config = SystemConfig.getInstance();
        String icp = config.getIcpRecord();
        String psr = config.getPublicSecurityRecord();
        boolean hasIcp = icp != null && !icp.isBlank();
        boolean hasPsr = psr != null && !psr.isBlank();

        if (hasIcp || hasPsr) {
            StringBuilder footer = new StringBuilder();
            if (hasIcp) {
                footer.append("<a href=\"https://beian.miit.gov.cn\" target=\"_blank\">")
                        .append(escapeHtml(icp.trim()))
                        .append("</a>");
            }
            if (hasIcp && hasPsr) footer.append(" | ");
            if (hasPsr) {
                String code = URLEncoder.encode(beianCode(psr.trim()), StandardCharsets.UTF_8);
                footer.append("<img src=\"/img/beian.png\" class=\"align-top\" style=\"width: 17px\"> ")
                        .append("<a href=\"https://beian.mps.gov.cn/#/query/webSearch?code=")
                        .append(escapeHtml(code))
                        .append("\" rel=\"noreferrer\" target=\"_blank\">")
                        .append(escapeHtml(psr.trim()))
                        .append("</a>");
            }
            return html.replace(FOOTER_PLACEHOLDER, footer.toString());
        } else {
            return html.replace(FOOTER_PLACEHOLDER, "");
        }
    }

    private static String beianCode(String record) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(record);
        return matcher.find() ? matcher.group() : record;
    }

    private static String escapeHtml(String input) {
        return PageRenderer.escapeHtml(input);
    }

}
