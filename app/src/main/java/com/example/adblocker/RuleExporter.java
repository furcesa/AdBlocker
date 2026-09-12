package com.example.adblocker;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class RuleExporter {

    public static String exportToJson(Context ctx) {
        try {
            List<AdRule> rules = RuleRepository.get(ctx).getAll();
            JSONArray arr = new JSONArray();
            for (AdRule r : rules) {
                JSONObject o = new JSONObject();
                o.put("name", r.name);
                o.put("keyword", r.keyword);
                o.put("viewId", r.viewId);
                o.put("packageName", r.packageName);
                o.put("enabled", r.enabled);
                arr.put(o);
            }
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("exportTime", System.currentTimeMillis());
            root.put("rules", arr);
            return root.toString(2);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean writeToUri(Context ctx, Uri uri) {
        String json = exportToJson(ctx);
        if (json == null) return false;
        try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
            if (os == null) return false;
            os.write(json.getBytes("UTF-8"));
            os.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static int importFromUri(Context ctx, Uri uri, boolean merge) {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            if (is == null) return -1;
            byte[] buf = new byte[is.available()];
            int read = is.read(buf);
            if (read <= 0) return -1;
            String json = new String(buf, 0, read, "UTF-8");
            return importFromJson(ctx, json, merge);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int importFromJson(Context ctx, String json, boolean merge) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("rules");
            if (arr == null) return -1;

            RuleRepository repo = RuleRepository.get(ctx);
            if (!merge) {
                for (AdRule r : repo.getAll()) repo.delete(r.id);
            }

            int count = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                AdRule r = new AdRule();
                r.name = o.optString("name", "导入规则");
                r.keyword = o.optString("keyword", "");
                r.viewId = o.optString("viewId", "");
                r.packageName = o.optString("packageName", "");
                r.enabled = o.optBoolean("enabled", true);
                if (r.keyword.isEmpty() && r.viewId.isEmpty()) continue;
                repo.add(r);
                count++;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
}
