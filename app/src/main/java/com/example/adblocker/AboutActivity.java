package com.example.adblocker;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tvVersion = findViewById(R.id.tv_version);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("版本 " + pi.versionName);
        } catch (Exception e) {
            tvVersion.setText("版本 1.0");
        }

        findViewById(R.id.row_privacy).setOnClickListener(v ->
                openDoc("隐私政策", R.raw.privacy));
        findViewById(R.id.row_permissions).setOnClickListener(v ->
                openDoc("权限说明", R.raw.permissions));
        findViewById(R.id.row_guide).setOnClickListener(v ->
                openDoc("使用指南", R.raw.user_guide));
        findViewById(R.id.row_changelog).setOnClickListener(v ->
                openDoc("更新日志", R.raw.changelog));
        findViewById(R.id.row_checklist).setOnClickListener(v ->
                openDoc("兼容性测试清单", R.raw.test_checklist));
        findViewById(R.id.row_disclaimer).setOnClickListener(v -> showDisclaimer());
        findViewById(R.id.row_contact).setOnClickListener(v -> showContact());
    }

    private void openDoc(String title, int rawId) {
        Intent i = new Intent(this, DocViewerActivity.class);
        i.putExtra(DocViewerActivity.EXTRA_TITLE, title);
        i.putExtra(DocViewerActivity.EXTRA_RAW_ID, rawId);
        startActivity(i);
    }

    private void showDisclaimer() {
        new AlertDialog.Builder(this)
                .setTitle("免责声明")
                .setMessage("本应用仅用于个人学习与自用设备优化。\n\n" +
                        "使用者需自行承担因误触、误拦截导致的一切后果。\n" +
                        "请勿用于商业用途或违反相关法律法规的场景。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showContact() {
        new AlertDialog.Builder(this)
                .setTitle("反馈与建议")
                .setMessage("如遇问题或有建议，欢迎反馈。\n\n邮箱：feedback@example.com")
                .setPositiveButton("复制邮箱", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("email", "feedback@example.com"));
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .show();
    }
}
