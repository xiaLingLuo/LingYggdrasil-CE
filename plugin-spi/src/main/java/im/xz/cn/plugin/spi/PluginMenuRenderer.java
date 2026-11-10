package im.xz.cn.plugin.spi;

@FunctionalInterface
public interface PluginMenuRenderer {
    String render(PluginHttpRequest request);
}
