package com.example.dashboard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LanguageSelectActivity extends AppCompatActivity {

    TextView langOkTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_select_activity);

        langOkTv = findViewById(R.id.langOkTv);

        langOkTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LanguageSelectActivity.this, SelectBluetoothDeviceFragment.class);
                startActivity(intent);
                finish();
            }
        });
    }
}