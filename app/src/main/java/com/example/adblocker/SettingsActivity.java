package com.example.adblocker;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvServiceStatus;
    private TextView tvStrengthValue;
    private StrengthConfig strengthConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        strengthConfig = new StrengthConfig(this);

        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvStrengthValue = findViewById(R.id.tv_strength_value);

        findViewById(R.id.row_service).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.row_battery).setOnClickListener(v -> openBatterySettings());
        findViewById(R.id.row_strength).setOnClickListener(v ->
                startActivity(new Intent(this, StrengthActivity.class)));
        findViewById(R.id.row_rules).setOnClickListener(v ->
                startActivity(new Intent(this, RuleListActivity.class)));
        findViewById(R.id.row_logs).setOnClickListener(v ->
                startActivity(new Intent(this, LogActivity.class)));
        findViewById(R.id.row_apps).setOnClickListener(v ->
                startActivity(new Intent(this, AppListActivity.class)));
        findViewById(R.id.row_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
        findViewById(R.id.row_reset).setOnClickListener(v -> confirmReset());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = MainActivity.isAccessibilityEnabled(this, AdBlockService.class);
        tvServiceStatus.setText(enabled ? "已开启" : "未开启");
        tvServiceStatus.setTextColor(ContextCompat.getColor(this,
                enabled ? R.color.success : R.color.danger));
        tvStrengthValue.setText(strengthConfig.getLevel().label);
    }

    private void openBatterySettings() {
        try {
            Intent i;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
            } else {
                i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            }
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("清空所有数据")
                .setMessage("将删除：\n• 所有自定义广告规则\n• 所有应用白名单\n• 所有应用强度覆盖\n• 强度设置\n\n此操作不可恢复，确定继续？")
                .setPositiveButton("清空", (d, w) -> {
                    getSharedPreferences("ad_rules", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("app_whitelist", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("app_override", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("strength_cfg", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("rule_logs", MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences("rogue_stats", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "已清空，请重启无障碍服务生效",
                            Toast.LENGTH_LONG).show();
                    recreate();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
