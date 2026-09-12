package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class WhitelistStore {

    private static final String PREF = "app_whitelist";
    private static final String KEY = "pkgs";
    private static WhitelistStore instance;
    private final SharedPreferences sp;
    private final Set<String> set = new HashSet<>();

    private WhitelistStore(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY, "");
        if (!TextUtils.isEmpty(raw)) {
            for (String s : raw.split(",")) if (!s.isEmpty()) set.add(s);
        }
    }

    public static synchronized WhitelistStore get(Context ctx) {
        if (instance == null) instance = new WhitelistStore(ctx);
        return instance;
    }

    public boolean contains(String pkg) { return set.contains(pkg); }
    public void add(String pkg) { set.add(pkg); save(); }
    public void remove(String pkg) { set.remove(pkg); save(); }
    public Set<String> all() { return new HashSet<>(set); }

    private void save() {
        sp.edit().putString(KEY, TextUtils.join(",", set)).apply();
    }
}
