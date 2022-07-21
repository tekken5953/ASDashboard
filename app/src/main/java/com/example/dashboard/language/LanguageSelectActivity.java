package com.example.dashboard.language;

import android.Manifest;
import android.app.Activity;
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
        // 화면 시작 시 풀 스크린으로 설정합니다
        outerClass.FullScreenMode(LanguageSelectActivity.this);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
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

        // 블루투스 퍼미션 체크 다이얼로그를 출력합니다
        checkBTPermissions();

        // SharedPreference 로 부터 최종 설정 된 언어와 언어선택 스킵 여부를 불러옵니다
        FINAL_LANGUAGE = SharedPreferenceManager.getString(context, "final");
        SKIP_SELECT_LANGUAGE = SharedPreferenceManager.getString(context, "skip_lang");

        // 미 선택 된 이미지의 Capacity 설정 입니다
        koreaFlag.setImageAlpha(76);
        englishFlag.setImageAlpha(76);

        Log.d(LANGUAGE_LOG, "final lang : " + FINAL_LANGUAGE + "\nskip : " + SKIP_SELECT_LANGUAGE);

        // 언어 설정이 스킵되지 않았을 경우
        if (SKIP_SELECT_LANGUAGE.equals("no")) {
            // 현재 사용중인 언어를 분류합니다
            if (FINAL_LANGUAGE != null) {
                // 한국어 일 때
                if (FINAL_LANGUAGE.equals("ko")) {
                    SelectedKoreaFlag();
                    outerClass.setLocaleToKorea(context);
                }
                // 영어 일 때
                else if (FINAL_LANGUAGE.equals("en")) {
                    SelectedEnglishFlag();
                    outerClass.setLocaleToEnglish(context);
                }
            }
        }
        // 언어 설정이 스킵되었을 경우
        else if (SKIP_SELECT_LANGUAGE.equals("ok")) {
            // 로케이션을 현재 선택된 언어로 설정합니다
            if (SharedPreferenceManager.getString(this, "final").equals("ko")) {
                outerClass.setLocaleToKorea(context);
            } else {
                outerClass.setLocaleToEnglish(context);
            }
            Toast.makeText(context, getString(R.string.skip_lang_msg), Toast.LENGTH_SHORT).show();
            // 현재 액티비티를 스킵하고 디바이스 연결 화면으로 넘어갑니다
            outerClass.backToConnectDevice(context);
        }
        // 현재 선택 된 언어가 없을 때
        else {
            SelectedNothing();
            outerClass.setLocaleToKorea(context);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            outerClass.FullScreenMode(LanguageSelectActivity.this);

            // 한국어 이미지를 클릭 하였을 경우
            koreaFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedKoreaFlag();
                    // current 라는 현재 선택 된 언어를 SharedPreference 에 저장합니다
                    SharedPreferenceManager.setString(context, "current", "ko");
                }
            });

            // 영어 이미지를 클릭 하였을 경우
            englishFlag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedEnglishFlag();
                    // current 라는 현재 선택 된 언어를 SharedPreference 에 저장합니다
                    SharedPreferenceManager.setString(context, "current", "en");
                }
            });

            // 확인 버튼을 눌렀을 경우 이벤트 리스너
            langOkTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (langOkTv.isEnabled()) {
                        outerClass.CallVibrate(context, 10);

                        // 현재 선택된 언어를 불러옵니다
                        // 그 언어를 바탕으로 국가를 설정하고 현재 선택된 언어를 최종 언어로 변경하여 저장합니다
                        // 디바이스 연결 화면으로 이동합니다
                        if (SharedPreferenceManager.getString(context, "current").equals("en")) {
                            outerClass.setLocaleToEnglish(context);
                            SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
                            SharedPreferenceManager.setString(context, "skip_lang", "ok");
                            Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                            outerClass.backToConnectDevice(context);
                        } else if (SharedPreferenceManager.getString(context, "current").equals("ko")) {
                            outerClass.setLocaleToKorea(context);
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

    private void SelectedKoreaFlag() {
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

    private void SelectedEnglishFlag() {
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

    private void SelectedNothing() {
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


    // 블루투스 퍼미션을 체크합니다
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