package im.xz.cn.util;

import java.security.SecureRandom;

public class VerificationCode {
    private static final int CODE_LENGTH = 8;
    private static final int EXPIRY_SECONDS = 300;

    public static String generate() {
        SecureRandom random = new SecureRandom();
        return String.format("%08d", random.nextInt(100000000));
    }

    public static int getExpirySeconds() {
        return EXPIRY_SECONDS;
    }
}
