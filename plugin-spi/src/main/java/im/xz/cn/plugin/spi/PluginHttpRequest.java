package im.xz.cn.plugin.spi;

import java.util.Map;


public record PluginHttpRequest(String method, String path, String queryString,
                                Map<String, String> headers, byte[] body) {
    public PluginHttpRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body == null ? new byte[0] : body.clone();
    }
}
