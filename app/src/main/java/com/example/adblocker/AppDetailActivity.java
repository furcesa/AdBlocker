package com.example.adblocker;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PKG = "pkg";

    private String pkg;
    private AppOverrideStore overrideStore;
    private WhitelistStore whitelist;
    private RuleRepository ruleRepo;

    private TextView tvName, tvPkg, tvInfo, tvStrength;
    private ImageView ivIcon;
    private LinearLayout ruleContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        pkg = getIntent().getStringExtra(EXTRA_PKG);
        if (TextUtils.isEmpty(pkg)) { finish(); return; }

        overrideStore = AppOverrideStore.get(this);
        whitelist = WhitelistStore.get(this);
        ruleRepo = RuleRepository.get(this);

        tvName = findViewById(R.id.tv_app_name);
        tvPkg = findViewById(R.id.tv_pkg);
        tvInfo = findViewById(R.id.tv_info);
        tvStrength = findViewById(R.id.tv_strength_value);
        ivIcon = findViewById(R.id.iv_icon);
        ruleContainer = findViewById(R.id.rule_container);

        bindBasicInfo();
        bindStrength();
        bindActions();
        loadRules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
        tvStrength.setText(getStrengthLabel(overrideStore.getStrength(pkg)));
    }

    private void bindBasicInfo() {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            tvName.setText(pm.getApplicationLabel(ai));
            ivIcon.setImageDrawable(pm.getApplicationIcon(ai));
            long t = pm.getPackageInfo(pkg, 0).firstInstallTime;
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                    .format(new Date(t));
            boolean sys = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            RogueAppDetector detector = RogueAppDetector.get(this);
            boolean suspicious = detector.isSuspicious(pkg);
            int count = detector.getRecentCount(pkg);

            StringBuilder sb = new StringBuilder();
            sb.append("安装时间：").append(date);
            sb.append("\n类型：").append(sys ? "系统应用" : "第三方应用");
            sb.append("\n是否白名单：").append(whitelist.contains(pkg) ? "是" : "否");
            if (suspicious) {
                sb.append("\n⚠ 可疑：近期弹窗 ").append(count).append(" 次");
            }
            tvInfo.setText(sb.toString());

            if (suspicious) {
                tvInfo.setTextColor(ContextCompat.getColor(this, R.color.danger));
                new AlertDialog.Builder(this)
                        .setTitle("⚠ 可疑应用")
                        .setMessage("该应用近期高频弹窗，可能是流氓软件。\n\n" +
                                "建议：卸载或清除数据。\n\n" +
                                "若确认正常，可点击「忽略」取消标记。")
                        .setPositiveButton("去卸载", (d, w) -> confirmUninstall())
                        .setNeutralButton("忽略", (d, w) -> {
                            detector.clearFlag(pkg);
                            bindBasicInfo();
                        })
                        .setNegativeButton("稍后", null)
                        .show();
            }
        } catch (Exception e) {
            tvName.setText(pkg);
            tvInfo.setText("无法读取应用信息");
        }
        tvPkg.setText(pkg);
    }

    private void bindStrength() {
        LinearLayout btn = findViewById(R.id.btn_strength);
        tvStrength.setText(getStrengthLabel(overrideStore.getStrength(pkg)));
        btn.setOnClickListener(v -> showStrengthDialog());
    }

    private String getStrengthLabel(int code) {
        if (code < 0) return "跟随全局";
        return StrengthLevel.fromCode(code).label;
    }

    private void showStrengthDialog() {
        final String[] items = {
                "跟随全局（默认）",
                StrengthLevel.NORMAL.label,
                StrengthLevel.MEDIUM.label,
                StrengthLevel.STRONG.label,
                StrengthLevel.CUSTOM.label + "（逐项开关）"
        };
        new AlertDialog.Builder(this)
                .setTitle("单独设置此应用的拦截强度")
                .setItems(items, (d, which) -> {
                    if (which == 4) {
                        showCustomSwitches();
                    } else {
                        int code = (which == 0) ? -1 : (which - 1);
                        overrideStore.setStrength(pkg, code);
                        overrideStore.clearCustom(pkg);
                        tvStrength.setText(getStrengthLabel(code));
                    }
                })
                .show();
    }

    private void showCustomSwitches() {
        final String[] labels = {
                "关键词匹配", "控件ID匹配", "坐标兜底",
                "秒退广告页", "流氓软件识别", "阻止自动安装", "摇一摇识别"
        };
        final String[] keys = {
                StrengthConfig.K_KEYWORD, StrengthConfig.K_VIEW_ID,
                StrengthConfig.K_COORDINATE, StrengthConfig.K_AD_ACTIVITY,
                StrengthConfig.K_ROGUE_APP, StrengthConfig.K_INSTALL_BLOCK,
                StrengthConfig.K_SHAKE_AD
        };
        final boolean[] checked = new boolean[keys.length];
        StrengthConfig globalCfg = new StrengthConfig(this);
        for (int i = 0; i < keys.length; i++) {
            Boolean custom = overrideStore.getCustom(pkg, keys[i]);
            checked[i] = custom != null ? custom : globalCfg.isEnabled(keys[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("自定义开关")
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("保存", (d, w) -> {
                    overrideStore.setStrength(pkg, StrengthLevel.CUSTOM.code);
                    for (int i = 0; i < keys.length; i++) {
                        overrideStore.setCustom(pkg, keys[i], checked[i]);
                    }
                    tvStrength.setText("自定义");
                    Toast.makeText(this, "已保存自定义开关", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void bindActions() {
        LinearLayout btnWhitelist = findViewById(R.id.btn_whitelist);
        updateWhitelistButton(btnWhitelist);
        btnWhitelist.setOnClickListener(v -> {
            if (whitelist.contains(pkg)) whitelist.remove(pkg);
            else whitelist.add(pkg);
            updateWhitelistButton(btnWhitelist);
            bindBasicInfo();
        });

        findViewById(R.id.btn_add_rule).setOnClickListener(v -> {
            Intent i = new Intent(this, RuleEditActivity.class);
            i.putExtra("pkg_preset", pkg);
            startActivity(i);
        });

        findViewById(R.id.btn_sys_settings).setOnClickListener(v -> openSystemAppInfo());
        findViewById(R.id.btn_uninstall).setOnClickListener(v -> confirmUninstall());
        findViewById(R.id.btn_clear_data).setOnClickListener(v -> confirmClearData());
    }

    private void updateWhitelistButton(LinearLayout row) {
        TextView tv = (TextView) row.getChildAt(0);
        boolean in = whitelist.contains(pkg);
        tv.setText(in ? "✓ 已在白名单（点击移出）" : "加入白名单（不拦截）");
        tv.setTextColor(ContextCompat.getColor(this,
                in ? R.color.success : R.color.primary));
    }

    private void loadRules() {
        ruleContainer.removeAllViews();
        List<AdRule> all = ruleRepo.getAll();
        int count = 0;
        for (AdRule r : all) {
            if (!TextUtils.isEmpty(r.packageName) && !pkg.equals(r.packageName)) continue;
            count++;
            View item = getLayoutInflater().inflate(
                    android.R.layout.simple_list_item_2, ruleContainer, false);
            TextView t1 = item.findViewById(android.R.id.text1);
            TextView t2 = item.findViewById(android.R.id.text2);
            String scope = TextUtils.isEmpty(r.packageName) ? "[全局]" : "[本应用]";
            t1.setText(scope + " " + r.name);
            t1.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            StringBuilder sb = new StringBuilder();
            if (!r.keyword.isEmpty()) sb.append("关键词: ").append(r.keyword);
            if (!r.viewId.isEmpty()) { if (sb.length() > 0) sb.append("  "); sb.append("ID: ").append(r.viewId); }
            if (!r.enabled) sb.append("  (已禁用)");
            t2.setText(sb.toString());
            t2.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            item.setOnClickListener(v -> {
                Intent i = new Intent(this, RuleEditActivity.class);
                i.putExtra("id", r.id);
                startActivity(i);
            });
            item.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("删除规则")
                        .setMessage("删除「" + r.name + "」？")
                        .setPositiveButton("删除", (d, w) -> { ruleRepo.delete(r.id); loadRules(); })
                        .setNegativeButton("取消", null).show();
                return true;
            });
            ruleContainer.addView(item);
        }
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("暂无针对此应用的规则，点击下方「＋ 新增规则」添加");
            empty.setPadding(24, 24, 24, 24);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            ruleContainer.addView(empty);
        }
    }

    private void openSystemAppInfo() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmUninstall() {
        new AlertDialog.Builder(this)
                .setTitle("卸载应用")
                .setMessage("将跳转到系统卸载界面，请手动确认。\n\n包名：" + pkg)
                .setPositiveButton("去卸载", (d, w) -> startActivity(
                        new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg))))
                .setNegativeButton("取消", null).show();
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
                .setTitle("清除数据")
                .setMessage("将跳转到系统应用信息页，请手动点击「存储 → 清除数据」。")
                .setPositiveButton("去设置", (d, w) -> openSystemAppInfo())
                .setNegativeButton("取消", null).show();
    }
}
