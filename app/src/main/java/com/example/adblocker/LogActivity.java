package com.example.adblocker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogActivity extends AppCompatActivity {

    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        RecyclerView rv = findViewById(R.id.rv_logs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btn_clear).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("清空日志")
                        .setMessage("确定清空所有日志？")
                        .setPositiveButton("清空", (d, w) -> {
                            RuleLogStore.clear(this);
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("取消", null).show());
    }

    @Override
    protected void onResume() { super.onResume(); adapter.notifyDataSetChanged(); }

    class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        private final List<RuleLogStore.LogEntry> logs = RuleLogStore.getAll(LogActivity.this);

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            RuleLogStore.LogEntry e = logs.get(pos);
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);
            h.time.setText(sdf.format(new Date(e.time)));
            h.text.setText("文本: " + (e.text.isEmpty() ? "(无)" : e.text));
            h.id.setText("ID: " + (e.viewId == null || e.viewId.isEmpty() ? "(无)" : e.viewId));
            h.pkg.setText("包名: " + e.pkg);
            h.cls.setText("类名: " + (e.className == null ? "" : e.className));

            h.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(LogActivity.this)
                        .setTitle("生成规则")
                        .setMessage("将根据此日志生成一条新规则：\n\n" +
                                "包名：" + e.pkg + "\n" +
                                "关键词：" + e.text + "\n" +
                                "控件ID：" + e.viewId)
                        .setPositiveButton("生成", (d, w) -> {
                            AdRule r = new AdRule();
                            r.name = "日志生成-" + e.pkg.substring(
                                    Math.max(0, e.pkg.lastIndexOf('.') + 1));
                            r.keyword = e.text;
                            r.viewId = e.viewId == null ? "" : e.viewId;
                            r.packageName = e.pkg;
                            r.enabled = true;
                            if (r.keyword.isEmpty() && r.viewId.isEmpty()) {
                                Toast.makeText(LogActivity.this,
                                        "节点信息为空，无法生成", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            RuleRepository.get(LogActivity.this).add(r);
                            Toast.makeText(LogActivity.this,
                                    "已生成规则，可在规则库查看", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }

        @Override public int getItemCount() { return logs.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView time, text, id, pkg, cls;
            VH(View v) {
                super(v);
                time = v.findViewById(R.id.tv_time);
                text = v.findViewById(R.id.tv_text);
                id = v.findViewById(R.id.tv_id);
                pkg = v.findViewById(R.id.tv_pkg);
                cls = v.findViewById(R.id.tv_class);
            }
        }
    }
}
