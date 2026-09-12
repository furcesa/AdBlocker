package com.example.adblocker;

public class AdRule {
    public long id;
    public String name;
    public String keyword;
    public String viewId;
    public String packageName;
    public boolean enabled;

    public AdRule() {}

    public String[] keywordArray() {
        if (keyword == null || keyword.trim().isEmpty()) return new String[0];
        String[] arr = keyword.split("[,，]");
        for (int i = 0; i < arr.length; i++) arr[i] = arr[i].trim();
        return arr;
    }
}
