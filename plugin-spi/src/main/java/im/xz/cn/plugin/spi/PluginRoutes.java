package im.xz.cn.plugin.spi;


public interface PluginRoutes {
    default void get(String path, PluginHttpHandler handler) { register("GET", path, handler); }
    default void post(String path, PluginHttpHandler handler) { register("POST", path, handler); }
    void register(String method, String path, PluginHttpHandler handler);
}
