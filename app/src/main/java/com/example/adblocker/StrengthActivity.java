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
        Switch swRogue = findViewById(R.id.sw_rog
