package com.example.adblocker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RuleEditActivity extends AppCompatActivity {

    private RuleRepository repo;
    private AdRule editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_edit);
        repo = RuleRepository.get(this);

        long id = getIntent().getLongExtra("id", 0);
        String pkgPreset = getIntent().getStringExtra("pkg_preset");
        if (id != 0) {
            for (AdRule r : repo.getAll()) if (r.id == id) { editing = r; break; }
        }
        if (editing == null) editing = new AdRule();
        if (editing.id == 0 && pkgPreset != null && !pkgPreset.isEmpty()) {
            editing.packageName = pkgPreset;
        }

        EditText etName = findViewById(R.id.et_name);
        EditText etKeyword = findViewById(R.id.et_keyword);
        EditText etViewId = findViewById(R.id.et_viewid);
        EditText etPkg = findViewById(R.id.et_pkg);
        Button btnSave = findViewById(R.id.btn_save);

        etName.setText(editing.name);
        etKeyword.setText(editing.keyword);
        etViewId.setText(editing.viewId);
        etPkg.setText(editing.packageName);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "请填写规则名称", Toast.LENGTH_SHORT).show(); return; }
            editing.name = name;
            editing.keyword = etKeyword.getText().toString().trim();
            editing.viewId = etViewId.getText().toString().trim();
            editing.packageName = etPkg.getText().toString().trim();
            if (editing.keyword.isEmpty() && editing.viewId.isEmpty()) {
                Toast.makeText(this, "关键词和控件ID至少填一项", Toast.LENGTH_SHORT).show();
                return;
            }
            if (editing.id == 0) { editing.enabled = true; repo.add(editing); }
            else repo.update(editing);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
