package com.example.dashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dashboard.connect.ConnectDeviceFragment;

import java.util.Locale;

public class LanguageSelectActivity extends AppCompatActivity {

    boolean FLAG_KOR_SELECTED = false;
    boolean FLAG_ENG_SELECTED = false;
    String FINAL_LANGUAGE = null;
    String SKIP_SELECT_LANGUAGE = null;
    private final String LANGUAGE_LOG = "language_log";

    Button langOkTv;
    ImageView koreaFlag, englishFlag;
    TextView langKorTitleTv, langEngTitleTv;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_select_activity);

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

        Log.d(LANGUAGE_LOG,"final lang : " + FINAL_LANGUAGE + "\nskip : " + SKIP_SELECT_LANGUAGE);

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
                    configuration.setLocale(Locale.US);
                    getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                }
            }
            // 현재 선택 된 언어가 없을 때
            else {
                SelectedNothing();
                configuration.setLocale(Locale.KOREA);
                getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
            }
        } else {
            Intent intent = new Intent(context, ConnectDeviceFragment.class);
            Toast.makeText(context, getString(R.string.skip_lang_msg), Toast.LENGTH_SHORT).show();
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

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
                    if (SharedPreferenceManager.getString(context, "current").equals("en")) {
                        Configuration configuration = new Configuration();
                        configuration.setLocale(Locale.US);
                        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                        SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
                        SharedPreferenceManager.setString(context,"skip_lang", "ok");
                        Intent intent = new Intent(LanguageSelectActivity.this, ConnectDeviceFragment.class);
                        Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        finish();
                    } else if (SharedPreferenceManager.getString(context, "current").equals("ko")) {
                        Configuration configuration = new Configuration();
                        configuration.setLocale(Locale.KOREA);
                        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                        SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
                        SharedPreferenceManager.setString(context,"skip_lang", "ok");
                        Intent intent = new Intent(LanguageSelectActivity.this, ConnectDeviceFragment.class);
                        Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });
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
}