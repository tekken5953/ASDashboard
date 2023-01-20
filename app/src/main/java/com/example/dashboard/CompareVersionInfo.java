package com.example.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.facebook.shimmer.BuildConfig;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class CompareVersionInfo {
    PackageInfo pi = null;
    final String packageName = BuildConfig.APPLICATION_ID;
    // 현재 버전과 코드명
    final String currentName = BuildConfig.VERSION_NAME;
    final int currentCode = BuildConfig.VERSION_CODE;

    public boolean isRecentVersion(Context context) {
        // 현재 버전 코드가 최신이 아닌경우
        //goToPlayStore 메서드로 구글플레이 페이지로 이동
        try {
            pi = context.getPackageManager().getPackageInfo(packageName, 0);
            if (!currentName.equals(getMarketVersion())) {
                // 최신 버전과 현재 버전을 비교하는 로그 출력
                Log.e(getClass().getSimpleName(),
                        "최신버전이 아님. 현재 : " + currentName + " 최신 : " + getMarketVersion());

                final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setMessage("새로운 버전이 있습니다")
                        .setPositiveButton("설치", (dialog, which) -> {
                            dialog.dismiss();
                            goToPlayStore(context);
                        });
                builder.show();
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

    public String getMarketVersion() {
        try {
            Document doc = Jsoup.connect(
                    "https://play.google.com/store/apps/details?id="
                            + "airsignal.remote.AS_Remote").get();
            Elements Version = doc.select(".content");

            for (Element mElement : Version) {
                if (mElement.attr("itemprop").equals("softwareVersion")) {
                    return mElement.text().trim();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    //플레이스토어로 이동
    public void goToPlayStore(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + "airsignal.remote.AS_Remote"));
        context.startActivity(intent);
    }
}
