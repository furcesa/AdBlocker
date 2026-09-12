package com.example.adblocker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.tv_status);

        findViewById(R.id.btn_open_settings).setOnClickListener(v -> openAccessibility());
        findViewById(R.id.btn_battery).setOnClickListener(v -> openBattery());
        findViewById(R.id.row_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.row_about).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isAccessibilityEnabled(this, AdBlockService.class);
        statusText.setText(enabled ? "服务已开启，正在拦截广告" : "服务未开启");
        statusText.setTextColor(ContextCompat.getColor(this,
                enabled ? R.color.success : R.color.text_primary));
        findViewById(R.id.v_status_dot).setBackgroundResource(
                enabled ? R.drawable.bg_dot_green : R.drawable.bg_dot_red);
    }

    private void openAccessibility() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "请找到「广告拦截助手」并开启开关", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "无法打开设置: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openBattery() {
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

    public static boolean isAccessibilityEnabled(Context ctx, Class<?> serviceClass) {
        String serviceId = ctx.getPackageName() + "/" + serviceClass.getName();
        try {
            String enabled = Settings.Secure.getString(
                    ctx.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(enabled)) return false;
            for (String s : enabled.split(":")) {
                if (s.equalsIgnoreCase(serviceId)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
