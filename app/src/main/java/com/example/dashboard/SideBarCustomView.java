package com.example.dashboard;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;


public class SideBarCustomView extends RelativeLayout implements View.OnClickListener {

    // 메뉴버튼 클릭 이벤트 리스너
    public EventListener listener;

    public void setEventListener(EventListener l) {
        listener = l;
    }

    // 메뉴버튼 클릭 이벤트 리스너 인터페이스
    public interface EventListener {

        void btnCancel(); //닫기

    }

    public SideBarCustomView(Context context) {
        this(context, null);
        init();
    }

    public SideBarCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.sidemenu, this, true);
        final TextView koreanTv = findViewById(R.id.sideMenuKorean);
        final TextView englishTv = findViewById(R.id.sideMenuEnglish);
        final ImageView cancelIv = findViewById(R.id.sideMenuCancelIv);
        final TextView title = findViewById(R.id.sideMenuLanguageTitle);
        Configuration configuration = new Configuration();

        cancelIv.setOnClickListener(this);
        koreanTv.setOnClickListener(this);
        englishTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                koreanTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
                englishTv.setTextColor(getResources().getColor(R.color.white));
                configuration.setLocale(Locale.US);
                getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                SharedPreferenceManager.setString(getContext(), "currentLanguage", "en");
                Log.e("languageLog", "Choice en\n" + "current : " + SharedPreferenceManager.getString(getContext(), "currentLanguage")
                        + " final : " + SharedPreferenceManager.getString(getContext(), "finalLanguage"));
            }
        });

        koreanTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                koreanTv.setTextColor(getResources().getColor(R.color.white));
                englishTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
                configuration.setLocale(Locale.KOREA);
                getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                SharedPreferenceManager.setString(getContext(), "currentLanguage", "ko");
                Log.e("languageLog", "Choice ko\n" + "current : " + SharedPreferenceManager.getString(getContext(), "currentLanguage")
                        + " final : " + SharedPreferenceManager.getString(getContext(), "finalLanguage"));
            }
        });

        if (SharedPreferenceManager.getString(getContext(), "finalLanguage").equals("en")) {
            koreanTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
            englishTv.setTextColor(getResources().getColor(R.color.white));
            title.setText("Language");
            Log.e("languageLog", "View Create en\n" + "current : " + SharedPreferenceManager.getString(getContext(), "currentLanguage")
                    + " final : " + SharedPreferenceManager.getString(getContext(), "finalLanguage"));
        } else {
            koreanTv.setTextColor(getResources().getColor(R.color.white));
            englishTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
            title.setText("언어설정");
            Log.e("languageLog", "View Create ko\n" + "current : " + SharedPreferenceManager.getString(getContext(), "currentLanguage")
                    + " final : " + SharedPreferenceManager.getString(getContext(), "finalLanguage"));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sideMenuCancelIv:
                listener.btnCancel();
                break;
            default:
                break;
        }
    }

}

