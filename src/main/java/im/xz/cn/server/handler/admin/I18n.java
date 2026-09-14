package im.xz.cn.server.handler.admin;

final class I18n {
    private I18n() {}

    static String t(String key, Object... args) {
        return im.xz.cn.i18n.AdminI18n.t(key, args);
    }

    static String tOrNull(String key) {
        return im.xz.cn.i18n.AdminI18n.tOrNull(key);
    }
}
