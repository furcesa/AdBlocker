package com.example.adblocker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdBlockService extends AccessibilityService {

    private static final String TAG = "AdBlockService";

    private static final long CLICK_COOLDOWN_MS = 1200L;
    private static final long MIN_AD_LIFETIME_MS = 200L;

    private long lastClickTime = 0L;
    private String lastClickedTag = "";
    private long adActivityEnterTime = 0L;
    private String currentAdActivity = "";
    private int adBackCount = 0;
    private long firstAdBackTime = 0L;

    private static final String[] AD_ACTIVITY_TAGS = {
            "SplashAdActivity", "SplashActivity", "AdActivity", "AdSplashActivity",
            "AppOpenAdActivity", "TencentAdActivity", "TTAdActivity",
            "ad.Splash", "ad.splash", "openadsdk", "csj.Splash",
            "com.qq.e.ads", "com.bytedance.sdk.openadsdk",
            "KsSplashActivity", "BaiduAdActivity", "MiAdActivity"
    };

    private static final String[] INSTALLER_PACKAGES = {
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.samsung.android.packageinstaller"
    };

    private static final String[] SUSPICIOUS_HINTS = {
            "跳过", "关闭", "Skip", "广告", "推广", "赞助",
            "摇一摇", "跳转", "详情", "查看", "点击"
    };

    private static final Set<String> SYSTEM_WHITE_LIST = new HashSet<>(Arrays.asList(
            "com.android.settings",
            "com.android.systemui",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller"
    ));

    private StrengthConfig strength;
    private WhitelistStore whitelist;
    private AppOverrideStore overrideStore;
    private RuleRepository ruleRepo;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        strength = new StrengthConfig(this);
        whitelist = WhitelistStore.get(this);
        overrideStore = AppOverrideStore.get(this);
        ruleRepo = RuleRepository.get(this);
        Log.i(TAG, "服务连接，强度: " + strength.getLevel().label);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || strength == null) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) return;
        String pkg = pkgCs.toString();

        if (pkg.equals(getPackageName())
                || SYSTEM_WHITE_LIST.contains(pkg)
                || whitelist.contains(pkg)) return;

        if (isEnabledFor(pkg, StrengthConfig.K_INSTALL_BLOCK) && isInstallerPackage(pkg)) {
            AccessibilityNodeInfo r = getRootInActiveWindow();
            if (r != null) {
                try { if (handleInstallBlocker(r)) return; }
                finally { r.recycle(); }
            }
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && isEnabledFor(pkg, StrengthConfig.K_ROGUE_APP)) {
                RogueAppDetector.get(this).recordPopup(pkg);
            }

            if (isEnabledFor(pkg, StrengthConfig.K_AD_ACTIVITY)
                    && type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                CharSequence cls = event.getClassName();
                String cn = cls == null ? "" : cls.toString();
                if (isAdActivity(cn)) {
                    handleAdActivity(pkg, cn);
                    return;
                } else {
                    currentAdActivity = "";
                    adActivityEnterTime = 0L;
                }
            }

            if (isEnabledFor(pkg, StrengthConfig.K_SHAKE_AD) && isWebViewAd(root)) {
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

            if (applyRulesWithStrength(root, pkg)) return;

            if (isEnabledFor(pkg, StrengthConfig.K_KEYWORD)) {
                recordSuspiciousNodes(root, pkg);
            }

            if (isEnabledFor(pkg, StrengthConfig.K_COORDINATE)) {
                clickTopRightCorner();
            }
        } finally {
            root.recycle();
        }
    }

    private boolean isEnabledFor(String pkg, String key) {
        int override = overrideStore.getStrength(pkg);
        if (override >= 0) {
            StrengthLevel lv = StrengthLevel.fromCode(override);
            if (lv == StrengthLevel.CUSTOM) {
                Boolean custom = overrideStore.getCustom(pkg, key);
                if (custom != null) return custom;
                return strength.isEnabled(key);
            }
            return StrengthConfig.defaultFor(lv, key);
        }
        return strength.isEnabled(key);
    }

    private boolean applyRulesWithStrength(AccessibilityNodeInfo root, String pkg) {
        boolean allowKeyword = isEnabledFor(pkg, StrengthConfig.K_KEYWORD);
        boolean allowViewId  = isEnabledFor(pkg, StrengthConfig.K_VIEW_ID);
        if (!allowKeyword && !allowViewId) return false;

        List<AdRule> rules = ruleRepo.getEnabled();
        for (AdRule rule : rules) {
            if (rule.packageName != null && !rule.packageName.isEmpty()
                    && !pkg.contains(rule.packageName)) continue;

            if (allowKeyword) {
                String[] kws = rule.keywordArray();
                if (kws.length > 0 && clickByKeywords(root, kws)) return true;
            }
            if (allowViewId && rule.viewId != null && !rule.viewId.isEmpty()
                    && clickByViewIdList(root, rule.viewId)) return true;
        }
        return false;
    }

    private boolean clickByKeywords(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) return false;
        CharSequence tCs = node.getText();
        CharSequence dCs = node.getContentDescription();
        String text = tCs == null ? "" : tCs.toString();
        String desc = dCs == null ? "" : dCs.toString();

        if (!text.isEmpty() || !desc.isEmpty()) {
            for (String kw : keywords) {
                if (text.contains(kw) || desc.contains(kw)) {
                    Rect b = new Rect();
                    node.getBoundsInScreen(b);
                    boolean tooBig = b.width() > getScreenWidth() * 0.8
                            || b.height() > getScreenHeight() * 0.4;
                    if (!tooBig && tryClick(node, "kw:" + kw)) return true;
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c == null) continue;
            boolean hit = clickByKeywords(c, keywords);
            c.recycle();
            if (hit) return true;
        }
        return false;
    }

    private boolean clickByViewIdList(AccessibilityNodeInfo node, String idList) {
        if (node == null) return false;
        String viewId = node.getViewIdResourceName();
        if (viewId != null) {
            for (String part : idList.split("[,，]")) {
                String p = part.trim().toLowerCase();
                if (!p.isEmpty() && viewId.toLowerCase().contains(p)) {
                    if (tryClick(node, "id:" + p)) return true;
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c == null) continue;
            boolean hit = clickByViewIdList(c, idList);
            c.recycle();
            if (hit) return true;
        }
        return false;
    }

    private boolean tryClick(AccessibilityNodeInfo node, String tag) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS && tag.equals(lastClickedTag)) return false;

        AccessibilityNodeInfo clickable = findClickableNode(node);
        if (clickable == null) return false;

        try {
            boolean ok = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (ok) { lastClickTime = now; lastClickedTag = tag; Log.i(TAG, "点击: " + tag); }
            return ok;
        } finally {
            if (clickable != node) clickable.recycle();
        }
    }

    private AccessibilityNodeInfo findClickableNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        int depth = 0;
        while (cur != null && depth < 8) {
            if (cur.isClickable() && cur.isEnabled()) return cur;
            AccessibilityNodeInfo parent = cur.getParent();
            if (parent == null) return null;
            if (cur != node) cur.recycle();
            cur = parent;
            depth++;
        }
        return cur;
    }

    private boolean isAdActivity(String className) {
        if (className == null || className.isEmpty()) return false;
        String lower = className.toLowerCase();
        for (String tag : AD_ACTIVITY_TAGS) {
            if (lower.contains(tag.toLowerCase())) return true;
        }
        return false;
    }

    private void handleAdActivity(String pkg, String className) {
        long now = System.currentTimeMillis();
        if (!className.equals(currentAdActivity)) {
            currentAdActivity = className;
            adActivityEnterTime = now;
            Log.i(TAG, "进入广告页: " + className + " (" + pkg + ")");
        }
        if (now - adActivityEnterTime < MIN_AD_LIFETIME_MS) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                if (clickByKeywords(root, new String[]{"跳过", "Skip", "关闭广告", "關閉"})) {
                    lastClickTime = now;
                    lastClickedTag = "ad_skip_btn";
                    return;
                }
            } finally { root.recycle(); }
        }
        safeBack();
    }

    private boolean safeBack() {
        long now = System.currentTimeMillis();
        if (now - firstAdBackTime > 5000) {
            adBackCount = 0;
            firstAdBackTime = now;
        }
        if (adBackCount >= 3) return false;
        boolean ok = performGlobalAction(GLOBAL_ACTION_BACK);
        if (ok) { adBackCount++; lastClickTime = now; lastClickedTag = "ad_back"; Log.i(TAG, "秒退广告页"); }
        return ok;
    }

    private boolean isWebViewAd(AccessibilityNodeInfo root) {
        if (root == null) return false;
        CharSequence cls = root.getClassName();
        if (cls == null || !cls.toString().contains("WebView")) return false;
        return containsText(root, new String[]{
                "摇一摇", "摇动手机", "扭一扭", "翻转手机", "跳转详情", "跳转第三方", "shake"
        });
    }

    private boolean containsText(AccessibilityNodeInfo node, String[] keywords) {
        if (node == null) return false;
        CharSequence t = node.getText();
        if (t != null) {
            for (String kw : keywords) if (t.toString().contains(kw)) return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c == null) continue;
            boolean hit = containsText(c, keywords);
            c.recycle();
            if (hit) return true;
        }
        return false;
    }

    private boolean isInstallerPackage(String pkg) {
        for (String p : INSTALLER_PACKAGES) if (pkg.equals(p)) return true;
        return pkg.toLowerCase().contains("packageinstaller");
    }

    private boolean handleInstallBlocker(AccessibilityNodeInfo root) {
        if (root == null) return false;
        List<AccessibilityNodeInfo> installBtns = root.findAccessibilityNodeInfosByText("安装");
        if (installBtns == null || installBtns.isEmpty())
            installBtns = root.findAccessibilityNodeInfosByText("下一步");
        if (installBtns == null || installBtns.isEmpty()) return false;

        List<AccessibilityNodeInfo> cancelBtns = root.findAccessibilityNodeInfosByText("取消");
        if (cancelBtns == null || cancelBtns.isEmpty())
            cancelBtns = root.findAccessibilityNodeInfosByText("Cancel");

        if (cancelBtns == null || cancelBtns.isEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return true;
        }
        for (AccessibilityNodeInfo node : cancelBtns) {
            AccessibilityNodeInfo clickable = findClickableNode(node);
            if (clickable != null
                    && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Toast.makeText(this, "已阻止自动安装", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    private void recordSuspiciousNodes(AccessibilityNodeInfo node, String pkg) {
        if (node == null) return;
        CharSequence tCs = node.getText();
        String text = tCs == null ? "" : tCs.toString();
        CharSequence dCs = node.getContentDescription();
        String desc = dCs == null ? "" : dCs.toString();

        String combined = text + " " + desc;
        for (String hint : SUSPICIOUS_HINTS) {
            if (combined.contains(hint)) {
                CharSequence cls = node.getClassName();
                RuleLogStore.record(this, pkg, combined.trim(),
                        node.getViewIdResourceName(),
                        cls == null ? "" : cls.toString());
                break;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo c = node.getChild(i);
            if (c == null) continue;
            recordSuspiciousNodes(c, pkg);
            c.recycle();
        }
    }

    private void clickTopRightCorner() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) return;
        int x = getScreenWidth() - dp2px(60);
        int y = getStatusBarHeight() + dp2px(60);
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 80))
                .build();
        if (dispatchGesture(g, null, null)) {
            lastClickTime = now;
            lastClickedTag = "gesture";
        }
    }

    private int getScreenWidth() { return getResources().getDisplayMetrics().widthPixels; }
    private int getScreenHeight() { return getResources().getDisplayMetrics().heightPixels; }
    private int dp2px(float dp) { return (int) (dp * getResources().getDisplayMetrics().density + 0.5f); }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    @Override
    public void onInterrupt() { Log.w(TAG, "无障碍服务被中断"); }
}
