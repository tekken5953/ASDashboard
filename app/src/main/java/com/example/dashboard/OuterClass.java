package com.example.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.dashboard.connect.ConnectDeviceActivity;

public class OuterClass {

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String translateData(String before, Activity activity) {
        String after;

        switch (before) {
            case "0":
                after = activity.getString(R.string.good);
                break;
            case "1":
                after = activity.getString(R.string.normal);
                break;
            case "2":
                after = activity.getString(R.string.bad);
                break;
            case "3":
                after = activity.getString(R.string.baddest);
                break;
            case "공기질 통합지수":
                after = activity.getString(R.string.aqi);
                break;
            case "미세먼지":
                after = activity.getString(R.string.fine_dust);
                break;
            case "휘발성 유기화합물":
                after = activity.getString(R.string.tvoc);
                break;
            case "이산화탄소":
                after = activity.getString(R.string.co2);
                break;
            case "일산화탄소":
                after = activity.getString(R.string.co);
                break;
            case "바이러스 위험지수":
                after = activity.getString(R.string.virus);
                break;
            case "습도":
                after = activity.getString(R.string.humid);
                break;
            default:
                after = activity.getString(R.string.error);
                break;
        }
        return after;
    }

    public void fanBackgroundChange(TextView select, TextView nonSelect1, TextView nonSelect2, TextView nonSelect3, Activity context) {
        select.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
        select.setTextColor(Color.parseColor("#5CC2E4"));
        nonSelect1.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_nofan_tv_bg));
        nonSelect2.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_nofan_tv_bg));
        nonSelect3.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_nofan_tv_bg));
        nonSelect1.setTextColor(Color.parseColor("#A0A0A0"));
        nonSelect2.setTextColor(Color.parseColor("#A0A0A0"));
        nonSelect3.setTextColor(Color.parseColor("#A0A0A0"));
    }

    public void backToConnectDevice(Activity activity) {
        Intent intent = new Intent(activity, ConnectDeviceActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    public void FullScreenMode(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

}
