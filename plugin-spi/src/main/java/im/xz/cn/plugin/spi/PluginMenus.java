package im.xz.cn.plugin.spi;


public interface PluginMenus {
    void register(String id, String title, PluginMenuRenderer renderer);
}
