package com.example.adblocker;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StrengthActivity extends AppCompatActivity {

    private StrengthConfig cfg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strength);
        cfg = new StrengthConfig(this);

        RadioGroup rg = findViewById(R.id.rg_level);
        Switch swKeyword = findViewById(R.id.sw_keyword);
        Switch swViewId = findViewById(R.id.sw_view_id);
        Switch swCoord = findViewById(R.id.sw_coordinate);
        Switch swAdAct = findViewById(R.id.sw_ad_activity);
        Switch swRogue = findViewById(R.id.sw_rogue);
        Switch swInstall = findViewById(R.id.sw_install);
        Switch swShake = findViewById(R.id.sw_shake);

        switch (cfg.getLevel()) {
            case NORMAL: rg.check(R.id.rb_normal); break;
            case MEDIUM: rg.check(R.id.rb_medium); break;
            case STRONG: rg.check(R.id.rb_strong); break;
            case CUSTOM: rg.check(R.id.rb_custom); break;
        }

        bindSwitch(swKeyword, StrengthConfig.K_KEYWORD);
        bindSwitch(swViewId, StrengthConfig.K_VIEW_ID);
        bindSwitch(swCoord, StrengthConfig.K_COORDINATE);
        bindSwitch(swAdAct, StrengthConfig.K_AD_ACTIVITY);
        bindSwitch(swRogue, StrengthConfig.K_ROGUE_APP);
        bindSwitch(swInstall, StrengthConfig.K_INSTALL_BLOCK);
        bindSwitch(swShake, StrengthConfig.K_SHAKE_AD);

        rg.setOnCheckedChangeListener((g, id) -> {
            StrengthLevel level;
            if (id == R.id.rb_normal) level = StrengthLevel.NORMAL;
            else if (id == R.id.rb_medium) level = StrengthLevel.MEDIUM;
            else if (id == R.id.rb_strong) level = StrengthLevel.STRONG;
            else level = StrengthLevel.CUSTOM;
            cfg.setLevel(level);
            if (level != StrengthLevel.CUSTOM) {
                refreshSwitches(swKeyword, swViewId, swCoord, swAdAct,
                        swRogue, swInstall, swShake);
                Toast.makeText(this, "已切换为「" + level.label + "」", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSwitch(Switch sw, String key) {
        sw.setChecked(cfg.isEnabled(key));
        sw.setOnCheckedChangeListener((v, checked) -> {
            cfg.setEnabled(key, checked);
            if (cfg.getLevel() != StrengthLevel.CUSTOM) {
                cfg.setLevel(StrengthLevel.CUSTOM);
                ((RadioGroup) findViewById(R.id.rg_level)).check(R.id.rb_custom);
            }
        });
    }

    private void refreshSwitches(Switch... switches) {
        String[] keys = {StrengthConfig.K_KEYWORD, StrengthConfig.K_VIEW_ID,
                StrengthConfig.K_COORDINATE, StrengthConfig.K_AD_ACTIVITY,
                StrengthConfig.K_ROGUE_APP, StrengthConfig.K_INSTALL_BLOCK,
                StrengthConfig.K_SHAKE_AD};
        for (int i = 0; i < switches.length; i++) {
            Switch sw = switches[i];
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(cfg.isEnabled(keys[i]));
            bindSwitch(sw, keys[i]);
        }
    }
}
