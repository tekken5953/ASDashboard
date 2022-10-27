/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 */

package com.example.dashboard.language;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.example.dashboard.OuterClass;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.databinding.LanguageSelectActivityBinding;

public class LanguageSelectActivity extends AppCompatActivity {
    LanguageSelectActivityBinding binding;

    boolean FLAG_KOR_SELECTED = false;
    boolean FLAG_ENG_SELECTED = false;
    String FINAL_LANGUAGE = null;
    String SKIP_SELECT_LANGUAGE = null;
    OuterClass outerClass = new OuterClass();

    Activity context;

    @Override
    protected void onResume() {
        super.onResume();
        // 화면 시작 시 풀 스크린으로 설정합니다
        outerClass.FullScreenMode(LanguageSelectActivity.this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LanguageSelectActivityBinding.inflate(getLayoutInflater());
        View v = binding.getRoot();
        setContentView(v);

        context = LanguageSelectActivity.this;

        // SharedPreference 로 부터 최종 설정 된 언어와 언어선택 스킵 여부를 불러옵니다
        FINAL_LANGUAGE = SharedPreferenceManager.getString(context, "final");
        SKIP_SELECT_LANGUAGE = SharedPreferenceManager.getString(context, "skip_lang");

        // 미 선택 된 이미지의 Capacity 설정 입니다
        binding.langKorIconIv.setImageAlpha(76);
        binding.langEngIconIv.setImageAlpha(76);

        String LANGUAGE_LOG = "language_log";
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
            outerClass.GoToConnectFromLang(context);
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
            binding.langKorIconIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedKoreaFlag();
                    // current 라는 현재 선택 된 언어를 SharedPreference 에 저장합니다
                    SharedPreferenceManager.setString(context, "current", "ko");
                }
            });

            // 영어 이미지를 클릭 하였을 경우
            binding.langEngIconIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedEnglishFlag();
                    // current 라는 현재 선택 된 언어를 SharedPreference 에 저장합니다
                    SharedPreferenceManager.setString(context, "current", "en");
                }
            });

            // 확인 버튼을 눌렀을 경우 이벤트 리스너
            binding.langOkTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (binding.langOkTv.isEnabled()) {
                        outerClass.CallVibrate(context, 10);
                        // 현재 선택된 언어를 불러옵니다
                        // 그 언어를 바탕으로 국가를 설정하고 현재 선택된 언어를 최종 언어로 변경하여 저장합니다
                        // 디바이스 연결 화면으로 이동합니다
                        if (SharedPreferenceManager.getString(context, "current").equals("en")) {
                            outerClass.setLocaleToEnglish(context);
                            SetCurrentToFinal();
                        } else if (SharedPreferenceManager.getString(context, "current").equals("ko")) {
                            outerClass.setLocaleToKorea(context);
                            SetCurrentToFinal();
                        }
                    }
                }
            });
        }
    }

    private void SetCurrentToFinal() {
        SharedPreferenceManager.setString(context, "final", SharedPreferenceManager.getString(context, "current"));
        SharedPreferenceManager.setString(context, "skip_lang", "ok");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, getString(R.string.complete_select_lang), Toast.LENGTH_SHORT).show();
                outerClass.GoToConnectFromLang(context);
            }
        });
    }

    private void SelectedKoreaFlag() {
        binding.langKorIconIv.setImageAlpha(255);
        binding.langEngIconIv.setImageAlpha(76);
        binding.langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        binding.langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = true;
        FLAG_ENG_SELECTED = false;
        binding.langOkTv.setEnabled(true);
        binding.langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_w));
        binding.langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    private void SelectedEnglishFlag() {
        binding.langEngIconIv.setImageAlpha(255);
        binding.langKorIconIv.setImageAlpha(76);
        binding.langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        binding.langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = false;
        FLAG_ENG_SELECTED = true;
        binding.langOkTv.setEnabled(true);
        binding.langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_w));
        binding.langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    private void SelectedNothing() {
        binding.langKorIconIv.setImageAlpha(76);
        binding.langEngIconIv.setImageAlpha(76);
        binding.langEngTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        binding.langKorTitleTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        FLAG_KOR_SELECTED = false;
        FLAG_ENG_SELECTED = false;
        binding.langOkTv.setEnabled(false);
        binding.langOkTv.setBackground(AppCompatResources.getDrawable(context, R.drawable.lang_ok_b));
        binding.langOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog alertDialog = builder.create();
        alertDialog.setTitle(getString(R.string.exit_app_title));
        alertDialog.setMessage(getString(R.string.exit_app_message));
        alertDialog.setIcon(R.drawable.icon);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.exit_app_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.exit_app_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }
}