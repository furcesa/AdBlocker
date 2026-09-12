package com.example.adblocker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RuleListActivity extends AppCompatActivity {

    private RuleRepository repo;
    private RuleAdapter adapter;
    private boolean pendingMerge = true;

    private static final int REQ_EXPORT = 1001;
    private static final int REQ_IMPORT = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_list);
        repo = RuleRepository.get(this);

        RecyclerView rv = findViewById(R.id.rv_rules);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btn_add).setOnClickListener(v ->
                startActivity(new Intent(this, RuleEditActivity.class)));
        findViewById(R.id.btn_export).setOnClickListener(v -> exportRules());
        findViewById(R.id.btn_import).setOnClickListener(v -> importRules());
    }

    @Override
    protected void onResume() { super.onResume(); adapter.notifyDataSetChanged(); }

    private void exportRules() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,
                "adblock_rules_" + System.currentTimeMillis() + ".json");
        startActivityForResult(i, REQ_EXPORT);
    }

    private void importRules() {
        new AlertDialog.Builder(this)
                .setTitle("导入方式")
                .setItems(new String[]{"合并到现有规则", "覆盖全部规则"}, (d, which) -> {
                    pendingMerge = (which == 0);
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("application/json");
                    startActivityForResult(i, REQ_IMPORT);
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        if (requestCode == REQ_EXPORT) {
            boolean ok = RuleExporter.writeToUri(this, data.getData());
            Toast.makeText(this, ok ? "导出成功" : "导出失败", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_IMPORT) {
            int count = RuleExporter.importFromUri(this, data.getData(), pendingMerge);
            if (count >= 0) {
                Toast.makeText(this, "导入 " + count + " 条规则", Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "导入失败，文件格式错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_rule, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            List<AdRule> list = repo.getAll();
            if (pos >= list.size()) return;
            AdRule r = list.get(pos);
            h.name.setText(r.name);
            StringBuilder sb = new StringBuilder();
            if (!r.keyword.isEmpty()) sb.append("关键词: ").append(r.keyword);
            if (!r.viewId.isEmpty()) { if (sb.length() > 0) sb.append("\n"); sb.append("ID: ").append(r.viewId); }
            if (!r.packageName.isEmpty()) { if (sb.length() > 0) sb.append("\n"); sb.append("限定: ").append(r.packageName); }
            if (sb.length() == 0) sb.append("(空规则)");
            h.desc.setText(sb.toString());

            h.enabled.setOnCheckedChangeListener(null);
            h.enabled.setChecked(r.enabled);
            h.enabled.setOnCheckedChangeListener((v, c) -> { r.enabled = c; repo.update(r); });

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(RuleListActivity.this, RuleEditActivity.class);
                i.putExtra("id", r.id);
                startActivity(i);
            });
            h.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(RuleListActivity.this)
                        .setTitle("删除规则")
                        .setMessage("确定删除「" + r.name + "」？")
                        .setPositiveButton("删除", (d, w) -> { repo.delete(r.id); notifyDataSetChanged(); })
                        .setNegativeButton("取消", null).show();
                return true;
            });
        }
        @Override public int getItemCount() { return repo.getAll().size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView name, desc; Switch enabled;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_name);
                desc = v.findViewById(R.id.tv_desc);
                enabled = v.findViewById(R.id.sw_enabled);
            }
        }
    }
}
