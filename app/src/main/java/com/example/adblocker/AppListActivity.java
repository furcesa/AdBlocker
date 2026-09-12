package com.example.adblocker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppListActivity extends AppCompatActivity {

    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> shownApps = new ArrayList<>();
    private AppAdapter adapter;
    private EditText etSearch;
    private CheckBox cbShowSystem;
    private final WhitelistStore whitelist = WhitelistStore.get(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        etSearch = findViewById(R.id.et_search);
        cbShowSystem = findViewById(R.id.cb_show_system);

        RecyclerView rv = findViewById(R.id.rv_apps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter();
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(); }
            public void afterTextChanged(Editable s) {}
        });
        cbShowSystem.setOnCheckedChangeListener((v, c) -> filter());

        findViewById(R.id.tv_loading).setVisibility(View.VISIBLE);
        AppListLoader.loadAsync(this, apps -> {
            findViewById(R.id.tv_loading).setVisibility(View.GONE);
            allApps.clear();
            allApps.addAll(apps);
            for (AppInfo a : allApps) a.whitelisted = whitelist.contains(a.packageName);
            filter();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (AppInfo a : allApps) a.whitelisted = whitelist.contains(a.packageName);
        adapter.notifyDataSetChanged();
    }

    private void filter() {
        String q = etSearch.getText().toString().trim().toLowerCase();
        boolean showSys = cbShowSystem.isChecked();
        shownApps.clear();
        for (AppInfo a : allApps) {
            if (!showSys && a.systemApp) continue;
            if (!q.isEmpty() && !a.appName.toLowerCase().contains(q)
                    && !a.packageName.toLowerCase().contains(q)) continue;
            shownApps.add(a);
        }
        adapter.notifyDataSetChanged();
        ((TextView) findViewById(R.id.tv_count)).setText("共 " + shownApps.size() + " 个应用");
    }

    class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_app, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            AppInfo a = shownApps.get(pos);
            RogueAppDetector detector = RogueAppDetector.get(AppListActivity.this);
            boolean suspicious = detector.isSuspicious(a.packageName);
            if (suspicious) {
                h.name.setText("⚠ " + a.appName);
                h.name.setTextColor(ContextCompat.getColor(AppListActivity.this, R.color.danger));
            } else {
                h.name.setText(a.appName);
                h.name.setTextColor(ContextCompat.getColor(AppListActivity.this, R.color.text_primary));
            }
            h.pkg.setText(a.packageName);
            if (a.icon != null) h.icon.setImageDrawable(a.icon);
            else h.icon.setImageResource(android.R.drawable.sym_def_app_icon);

            h.whitelist.setOnCheckedChangeListener(null);
            h.whitelist.setChecked(a.whitelisted);
            h.whitelist.setOnCheckedChangeListener((v, checked) -> {
                a.whitelisted = checked;
                if (checked) whitelist.add(a.packageName);
                else whitelist.remove(a.packageName);
                Toast.makeText(AppListActivity.this,
                        checked ? "已加入白名单" : "已移出白名单", Toast.LENGTH_SHORT).show();
            });

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(AppListActivity.this, AppDetailActivity.class);
                i.putExtra(AppDetailActivity.EXTRA_PKG, a.packageName);
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return shownApps.size(); }
        class VH extends RecyclerView.ViewHolder {
            ImageView icon; TextView name, pkg; CheckBox whitelist;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.iv_icon);
                name = v.findViewById(R.id.tv_app_name);
                pkg = v.findViewById(R.id.tv_pkg);
                whitelist = v.findViewById(R.id.cb_whitelist);
            }
        }
    }
}
