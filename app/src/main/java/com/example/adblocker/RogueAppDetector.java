package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RogueAppDetector {

    private static final String PREF = "rogue_stats";
    private static final int POPUP_THRESHOLD = 5;
    private static final long WINDOW_MS = 60_000L;

    private static final Map<String, LinkedList<Long>> popupHistory = new HashMap<>();
    private static RogueAppDetector instance;
    private final SharedPreferences sp;

    private RogueAppDetector(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized RogueAppDetector get(Context ctx) {
        if (instance == null) instance = new RogueAppDetector(ctx);
        return instance;
    }

    public void recordPopup(String pkg) {
        long now = System.currentTimeMillis();
        LinkedList<Long> times = popupHistory.get(pkg);
        if (times == null) {
            times = new LinkedList<>();
            popupHistory.put(pkg, times);
        }
        times.add(now);
        while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS) {
            times.pollFirst();
        }
        if (times.size() >= POPUP_THRESHOLD) {
            sp.edit().putLong("flag_" + pkg, now).apply();
        }
    }

    public boolean isSuspicious(String pkg) {
        return sp.contains("flag_" + pkg);
    }

    public int getRecentCount(String pkg) {
        LinkedList<Long> times = popupHistory.get(pkg);
        return times == null ? 0 : times.size();
    }

    public void clearFlag(String pkg) {
        sp.edit().remove("flag_" + pkg).apply();
    }

    public static boolean isSuspiciousName(String appName) {
        if (appName == null || appName.trim().isEmpty()) return true;
        String name = appName.trim();
        if (name.length() < 2) return true;
        return name.matches("^[\\p{P}\\p{S}\\d]+$");
    }

    public boolean isRogue(String pkg, String appName) {
        return isSuspicious(pkg) || isSuspiciousName(appName);
    }
}
