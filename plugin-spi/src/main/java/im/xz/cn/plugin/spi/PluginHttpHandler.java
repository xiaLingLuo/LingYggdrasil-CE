package im.xz.cn.plugin.spi;

@FunctionalInterface
public interface PluginHttpHandler {
    PluginHttpResponse handle(PluginHttpRequest request);
}
