package com.example.adblocker;

public enum StrengthLevel {
    NORMAL("普通", 0),
    MEDIUM("中等", 1),
    STRONG("强力", 2),
    CUSTOM("自定义", 3);

    public final String label;
    public final int code;

    StrengthLevel(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public static StrengthLevel fromCode(int code) {
        for (StrengthLevel l : values()) if (l.code == code) return l;
        return NORMAL;
    }
}
