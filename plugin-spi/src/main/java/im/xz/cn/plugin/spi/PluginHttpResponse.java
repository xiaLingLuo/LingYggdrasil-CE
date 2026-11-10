package im.xz.cn.plugin.spi;


public record PluginHttpResponse(int status, String contentType, byte[] body) {
    public PluginHttpResponse {
        body = body == null ? new byte[0] : body.clone();
        if (status < 100 || status > 599) throw new IllegalArgumentException("invalid status");
    }
    public static PluginHttpResponse text(int status, String text) {
        return new PluginHttpResponse(status, "text/plain; charset=utf-8",
                (text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
