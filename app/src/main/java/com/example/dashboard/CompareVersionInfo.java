package com.example.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class CompareVersionInfo {
    PackageInfo pi = null;
    final String packageName = "airsignal.remote.AS_Remote";
    //TODO 최신 버전 네임과 코드를 적어주세요
    final String recentName = "1.0";
    final int recentCode = 1;

    // 현재 버전과 코드명
    String currentName;
    int currentCode;

    private String getVersionName() {
        currentName = pi.versionName;
        Log.i(getClass().getSimpleName(), "현재 버전네임은 " + currentName + "입니다");
        return currentName;
    }

    private int getVersionCode() {
        currentCode = pi.versionCode;
        Log.i(getClass().getSimpleName(), "현재 버전코드는 " + currentCode + "입니다");
        return currentCode;
    }

    public boolean isRecentVersion(Context context) {
        // 현재 버전 코드가 최신이 아닌경우
        //goToPlayStore 메서드로 구글플레이 페이지로 이동가능
        try {
            pi = context.getPackageManager().getPackageInfo(packageName, 0);
            if (getVersionCode() != recentCode) {
                Log.e(getClass().getSimpleName(), "버전코드가 최신이아닙니다. 현재 : " + currentCode
                        + " 최신 : " + recentCode);
                return false;
            } else if (!getVersionName().equals(recentName)) {
                Log.e(getClass().getSimpleName(), "버전네임이 최신이아닙니다. 현재 : " + currentName
                        + " 최신 : " + recentName);
                return false;
            }
            // 최신 버전인 경우
            else {
                Log.i(getClass().getSimpleName(), "최신버전입니다");
                return true;
            }
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void goToPlayStore(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageName));
        context.startActivity(intent);
    }
}
