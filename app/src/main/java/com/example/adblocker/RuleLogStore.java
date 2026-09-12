package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RuleLogStore {

    private static final String PREF = "rule_logs";
    private static final String KEY = "logs";
    private static final int MAX_SIZE = 200;

    public static class LogEntry {
        public String pkg;
        public String text;
        public String viewId;
        public String className;
        public long time;
    }

    public static void record(Context ctx, String pkg, String text,
                              String viewId, String className) {
        try {
            SharedPreferences sp = ctx.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));

            long now = System.currentTimeMillis();
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                if (pkg.equals(o.optString("pkg"))
                        && TextUtils.equals(text, o.optString("text"))
                        && TextUtils.equals(viewId, o.optString("viewId"))
                        && now - o.optLong("time") < 5000) {
                    return;
                }
            }

            JSONObject o = new JSONObject();
            o.put("pkg", pkg);
            o.put("text", text == null ? "" : text);
            o.put("viewId", viewId == null ? "" : viewId);
            o.put("className", className == null ? "" : className);
            o.put("time", now);
            arr.put(o);

            while (arr.length() > MAX_SIZE) arr.remove(0);

            sp.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<LogEntry> getAll(Context ctx) {
        List<LogEntry> list = new ArrayList<>();
        try {
            SharedPreferences sp = ctx.getApplicationContext()
                    .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = arr.length() - 1; i >= 0; i--) {
                JSONObject o = arr.getJSONObject(i);
                LogEntry e = new LogEntry();
                e.pkg = o.optString("pkg");
                e.text = o.optString("text");
                e.viewId = o.optString("viewId");
                e.className = o.optString("className");
                e.time = o.optLong("time");
                list.add(e);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void clear(Context ctx) {
        ctx.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}
