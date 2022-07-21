package com.example.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.dashboard.connect.ConnectDeviceActivity;
import com.example.dashboard.dashboard.DashBoardActivity;
import com.example.dashboard.language.LanguageSelectActivity;

import java.util.Locale;

public class OuterClass {

    // 펌웨어와 통신 중 불러온 데이터를 설정된 국가의 언어로 변경합니다
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

    // 펜 제어 시 다이얼로그의 백그라운드와 컬러를 변경합니다
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

    // 디바이스 연결 화면으로 Intent 합니다
    public void GoToConnectByLang(Activity activity) {
        Intent intent = new Intent(activity, ConnectDeviceActivity.class);
        intent.putExtra("dialog", "no");
        activity.startActivity(intent);
        activity.finish();
    }

    public void GoToLanguageByConnect(Activity activity) {
        Intent intent = new Intent(activity, LanguageSelectActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    public void GoToConnectByDashboard(Activity activity) {
        Intent intent = new Intent(activity, ConnectDeviceActivity.class);
        intent.putExtra("dialog", "yes");
        activity.startActivity(intent);
        activity.finish();
    }

    // 화면을 풀 스크린으로 사용합니다
    public void FullScreenMode(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // 이벤트 발생 시 진동 효과를 줍니다
    public void CallVibrate(Activity activity, long time) {
        Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(time);
        }
    }

    // 국가를 대한민국으로 설정합니다
    public void setLocaleToKorea(Context context) {
        Configuration configuration = new Configuration();
        configuration.setLocale(Locale.KOREA);
        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }

    // 국가를 영어권으로 설정합니다
    public void setLocaleToEnglish(Context context) {
        Configuration configuration = new Configuration();
        configuration.setLocale(Locale.ENGLISH);
        context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
    }

    // 절대 절전모드에 빠지지 않습니다
    // 퍼미션을 사용자에게 요청합니다
    @SuppressLint("BatteryLife")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void DoNotGoingSleepMode(Activity activity) {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        String packageName = activity.getPackageName();
        if (pm.isIgnoringBatteryOptimizations(packageName)) {

        }
        // 메모리 최적화가 되어 있다면, 풀기 위해 설정 화면 띄움.
        else {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            activity.startActivity(intent);
        }
    }
}
