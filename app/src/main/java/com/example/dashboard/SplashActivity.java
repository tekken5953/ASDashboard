/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 */

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