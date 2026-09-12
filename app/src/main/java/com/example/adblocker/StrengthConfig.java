package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;

public class StrengthConfig {

    private static final String PREF = "strength_cfg";

    public static final String K_KEYWORD       = "keyword";
    public static final String K_VIEW_ID       = "view_id";
    public static final String K_COORDINATE    = "coordinate";
    public static final String K_AD_ACTIVITY   = "ad_activity";
    public static final String K_ROGUE_APP     = "rogue_app";
    public static final String K_INSTALL_BLOCK = "install_block";
    public static final String K_SHAKE_AD      = "shake_ad";

    private final SharedPreferences sp;

    public StrengthConfig(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public StrengthLevel getLevel() {
        return StrengthLevel.fromCode(sp.getInt("level", StrengthLevel.NORMAL.code));
    }

    public void setLevel(StrengthLevel level) {
        sp.edit().putInt("level", level.code).apply();
        if (level != StrengthLevel.CUSTOM) applyPreset(level);
    }

    public boolean isEnabled(String key) {
        return sp.getBoolean(key, defaultFor(getLevel(), key));
    }

    public void setEnabled(String key, boolean v) {
        sp.edit().putBoolean(key, v).apply();
    }

    public static boolean defaultFor(StrengthLevel level, String key) {
        switch (level) {
            case NORMAL:
                return K_KEYWORD.equals(key);
            case MEDIUM:
                return K_KEYWORD.equals(key) || K_VIEW_ID.equals(key)
                        || K_AD_ACTIVITY.equals(key);
            case STRONG:
            case CUSTOM:
            default:
                return true;
        }
    }

    private void applyPreset(StrengthLevel level) {
        String[] keys = {K_KEYWORD, K_VIEW_ID, K_COORDINATE, K_AD_ACTIVITY,
                K_ROGUE_APP, K_INSTALL_BLOCK, K_SHAKE_AD};
        SharedPreferences.Editor e = sp.edit();
        for (String k : keys) e.putBoolean(k, defaultFor(level, k));
        e.apply();
    }
}
