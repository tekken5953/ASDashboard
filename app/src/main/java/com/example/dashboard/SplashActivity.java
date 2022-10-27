/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 */

package com.example.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        OuterClass outerClass = new OuterClass();
        outerClass.FullScreenMode(SplashActivity.this);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, PermissionActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);


    }
}