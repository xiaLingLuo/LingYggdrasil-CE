package im.xz.cn.model.enums;

public enum UserRole {
    DEFAULT,
    BANNED;

    public String toDbValue() {
        return name().toLowerCase();
    }

    public static UserRole fromDbValue(String value) {
        if (value == null) return DEFAULT;
        return valueOf(value.toUpperCase());
    }
}
