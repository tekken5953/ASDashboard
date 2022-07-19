package com.example.dashboard.language;

import android.Manifest;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.example.dashboard.OuterClass;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;

import java.util.Locale;

public class LanguageSelectActivity extends AppCompatActivity {

    boolean FLAG_KOR_SELECTED = false;
    boolean FLAG_ENG_SELECTED = false;
    String FINAL_LANGUAGE = null;
    String SKIP_SELECT_LANGUAGE = null;
    private final String LANGUAGE_LOG = "language_log";
    OuterClass outerClass = new OuterClass();

    Button langOkTv;
    ImageView koreaFlag, englishFlag;
    TextView langKorTitleTv, langEngTitleTv;
    Activity context = LanguageSelectActivity.this;

    @Override
    protected void onResume() {
        super.onResume();
        outerClass.FullScreenMode(LanguageSelectActivity.this);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_select_activity);

        checkBTPermissions();

        langOkTv = findViewById(R.id.langOkTv);
        koreaFlag = findViewById(R.id.langKorIconIv);
        englishFlag = findViewById(R.id.langEngIconIv);
        langKorTitleTv = findViewById(R.id.langKorTitleTv);
        langEngTitleTv = findViewById(R.id.langEngTitleTv);
        context = LanguageSelectActivity.this;

        FINAL_LANGUAGE = SharedPreferenceManager.getString(context, "final");
        SKIP_SELECT_LANGUAGE = SharedPreferenceManager.getString(context, "skip_lang");

        koreaFlag.setImageAlpha(76);
        englishFlag.setImageAlpha(76);

        Log.d(LANGUAGE_LOG, "final lang : " + FINAL_LANGUAGE + "\nskip : " + SKIP_SELECT_LANGUAGE);

        Configuration configuration = new Configuration();

        // 언어 설정 스킵 YES or NO
        if (SKIP_SELECT_LANGUAGE.equals("no")) {
            // 현재 사용중인 언어 분류
            if (FINAL_LANGUAGE != null) {
                // 한국어 일 때
                if (FINAL_LANGUAGE.equals("ko")) {
                    SelectedKoreaFlag();
                    configuration.setLocale(Locale.KOREA);
                    getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                }
                // 영어 일 때
                else if (FINAL_LANGUAGE.equals("en")) {
                    SelectedEnglishFlag();
                    configuration.setLocale(Locale.ENGLISH);
                    getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                }
            }
        } else if (SKIP_SELECT_LANGUAGE.equals("ok")) {
            if (SharedPreferenceManager.getString(this, "final").equals("ko")) {
                configuration.setLocale(Locale.KOREA);
            } else {
                configuration.setLocale(Locale.ENGLISH);
            }
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
            Toast.makeText(context, getString(R.string.skip_lang_msg), Toast.LENGTH_SHORT).show();
            outerClass.backToConnectDevice(context);
        } else {
            // 현재 선택 된 언어가 없을 때
            SelectedNothing();
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            outerClass.FullScreenMode(LanguageSelectActivity.this);

            koreaFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedKoreaFlag();
                    SharedPreferenceManager.setString(context, "current", "ko");
                }
            });

            englishFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedEnglishFlag();
                    SharedPreferenceManager.setString(context, "current", "en");
                }
            });

            langOkTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (langOkTv.isEnabled()) {
                        outerClass.CallVibrate(context, 10);
                        if (SharedPreferenceManager.getString(context, "current").equals("en")) {
                            Configuration configuration = new Configuration();
                            configuration.setLocale(Locale.ENGLISH);
                            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                            SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
                            SharedPreferenceManager.setString(context, "skip_lang", "ok");
                            Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                            outerClass.backToConnectDevice(context);
                        } else if (SharedPreferenceManager.getString(context, "current").equals("ko")) {
                            Configuration configuration = new Configuration();
                            configuration.setLocale(Locale.KOREA);
                            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                            SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
                            SharedPreferenceManager.setString(context, "skip_lang", "ok");
                            Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                            outerClass.backToConnectDevice(context);
                        }
                    }
                }
            });
        }
    }

    public void SelectedKoreaFlag() {
        koreaFlag.setImageAlpha(255);
        englishFlag.setImageAlpha(76);
        langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = true;
        FLAG_ENG_SELECTED = false;
        langOkTv.setEnabled(true);
        langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_w));
        langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    public void SelectedEnglishFlag() {
        englishFlag.setImageAlpha(255);
        koreaFlag.setImageAlpha(76);
        langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = false;
        FLAG_ENG_SELECTED = true;
        langOkTv.setEnabled(true);
        langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_w));
        langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    public void SelectedNothing() {
        koreaFlag.setImageAlpha(76);
        englishFlag.setImageAlpha(76);
        langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = false;
        FLAG_ENG_SELECTED = false;
        langOkTv.setEnabled(false);
        langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_b));
        langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        // ref) https://it-recording.tistory.com/15
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = context.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += context.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            if (permissionCheck != 0) {
                context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            } else {
                Log.d("checkPermission", "No need to check permissions. SDK version < LoLLIPOP");
            }
        }
    }
}