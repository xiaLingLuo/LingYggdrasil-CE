package im.xz.cn.database;

import java.util.Locale;

public final class SqlLike {
    private SqlLike() {}

    public static String contains(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
