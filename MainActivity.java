package com.hbyswbq.anytext;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwitchMaterial wechatSwitch = findViewById(R.id.switch_wechat);
        wechatSwitch.setChecked(getSharedPreferences("AnyText", MODE_PRIVATE)
                .getBoolean("wechat_message_edit", true));
        
        wechatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getSharedPreferences("AnyText", MODE_PRIVATE)
                    .edit()
                    .putBoolean("wechat_message_edit", isChecked)
                    .apply();
        });
    }
}
