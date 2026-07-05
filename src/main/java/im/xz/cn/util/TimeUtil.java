package im.xz.cn.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String plusSeconds(long seconds) {
        return LocalDateTime.now().plusSeconds(seconds).format(FORMATTER);
    }
}
