package im.xz.cn.model.enums;

public enum AdminRole {
    ROOT,
    OP;

    public String toDbValue() {
        return name().toLowerCase();
    }

    public static AdminRole fromDbValue(String value) {
        if (value == null) return OP;
        return valueOf(value.toUpperCase());
    }
}
