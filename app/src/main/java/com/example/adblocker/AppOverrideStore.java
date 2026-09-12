package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;

public class AppOverrideStore {

    private static final String PREF = "app_override";
    private static final String KEY_STRENGTH_PREFIX = "strength_";
    private static final String KEY_CUSTOM_PREFIX = "custom_";
    private static AppOverrideStore instance;
    private final SharedPreferences sp;

    private AppOverrideStore(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized AppOverrideStore get(Context ctx) {
        if (instance == null) instance = new AppOverrideStore(ctx);
        return instance;
    }

    public int getStrength(String pkg) {
        return sp.getInt(KEY_STRENGTH_PREFIX + pkg, -1);
    }

    public void setStrength(String pkg, int code) {
        if (code < 0) sp.edit().remove(KEY_STRENGTH_PREFIX + pkg).apply();
        else sp.edit().putInt(KEY_STRENGTH_PREFIX + pkg, code).apply();
    }

    public Boolean getCustom(String pkg, String key) {
        String k = KEY_CUSTOM_PREFIX + pkg + "_" + key;
        if (!sp.contains(k)) return null;
        return sp.getBoolean(k, false);
    }

    public void setCustom(String pkg, String key, boolean value) {
       
