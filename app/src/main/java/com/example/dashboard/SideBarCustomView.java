package com.example.dashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


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
        cancelIv.setOnClickListener(this);
        koreanTv.setOnClickListener(this);
        englishTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                koreanTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
                englishTv.setTextColor(getResources().getColor(R.color.white));
                Toast.makeText(getContext(), "언어를 영어로 변경 완료", Toast.LENGTH_SHORT).show();
            }
        });

        koreanTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                koreanTv.setTextColor(getResources().getColor(R.color.white));
                englishTv.setTextColor(getResources().getColor(R.color.sidemenun_gray));
                Toast.makeText(getContext(), "언어를 한글로 변경 완료", Toast.LENGTH_SHORT).show();
            }
        });
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

