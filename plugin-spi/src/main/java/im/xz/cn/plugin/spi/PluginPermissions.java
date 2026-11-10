package im.xz.cn.plugin.spi;


public interface PluginPermissions {
    void registerUser(String key, String category, String description);
    void registerAdmin(String key, String category, String description);
}
