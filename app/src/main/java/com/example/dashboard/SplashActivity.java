package com.example.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dashboard.language.LanguageSelectActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        OuterClass outerClass = new OuterClass();
        outerClass.FullScreenMode(SplashActivity.this);

        Intent intent = new Intent(SplashActivity.this, LanguageSelectActivity.class);
        startActivity(intent);
        finish();
    }
}