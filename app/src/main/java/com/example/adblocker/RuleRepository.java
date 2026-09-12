package com.example.adblocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RuleRepository {

    private static final String PREF = "ad_rules";
    private static final String KEY_RULES = "rules_json";
    private static RuleRepository instance;

    private final SharedPreferences sp;
    private final List<AdRule> cache = new ArrayList<>();

    private RuleRepository(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized RuleRepository get(Context ctx) {
        if (instance == null) instance = new RuleRepository(ctx);
        return instance;
    }

    public List<AdRule> getAll() { return new ArrayList<>(cache); }

    public List<AdRule> getEnabled() {
        List<AdRule> list = new ArrayList<>();
        for (AdRule r : cache) if (r.enabled) list.add(r);
        return list;
    }

    private void load() {
        cache.clear();
        String json = sp.getString(KEY_RULES, "");
        if (TextUtils.isEmpty(json)) {
            cache.addAll(defaultRules());
            save();
            return;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                AdRule r = new AdRule();
                r.id = o.optLong("id");
                r.name = o.optString("name");
                r.keyword = o.optString("keyword");
                r.viewId = o.optString("viewId");
                r.packageName = o.optString("packageName");
                r.enabled = o.optBoolean("enabled", true);
                cache.add(r);
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        try {
            JSONArray arr = new JSONArray();
            for (AdRule r : cache) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("name", r.name);
                o.put("keyword", r.keyword);
                o.put("viewId", r.viewId);
                o.put("packageName", r.packageName);
                o.put("enabled", r.enabled);
                arr.put(o);
            }
            sp.edit().putString(KEY_RULES, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public void add(AdRule rule) {
        if (rule.id == 0) rule.id = System.currentTimeMillis();
        cache.add(rule);
        save();
    }

    public void update(AdRule rule) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id == rule.id) { cache.set(i, rule); break; }
        }
        save();
    }

    public void delete(long id) {
        cache.removeIf(r -> r.id == id);
        save();
    }

    private List<AdRule> defaultRules() {
        List<AdRule> list = new ArrayList<>();

        AdRule r1 = new AdRule();
        r1.id = 1;
        r1.name = "通用跳过按钮";
        r1.keyword = "跳过,跳過,Skip,skip,关闭广告,關閉廣告,点击跳过,点击关闭";
        r1.viewId = "";
        r1.packageName = "";
        r1.enabled = true;
        list.add(r1);

        AdRule r2 = new AdRule();
        r2.id = 2;
        r2.name = "开屏广告常见ID";
        r2.keyword = "";
        r2.viewId = "iv_close,btn_skip,close_ad,skip_ad,btn_close,iv_skip";
        r2.packageName = "";
        r2.enabled = true;
        list.add(r2);

        return list;
    }
}
