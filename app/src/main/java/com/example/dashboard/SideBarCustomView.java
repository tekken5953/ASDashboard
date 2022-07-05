package com.example.dashboard;

import android.annotation.SuppressLint;
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

        void btnCancel(); // 닫기

        void powerOff(); // 전원 끄기

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

        findViewById(R.id.sideMenuCancelIv).setOnClickListener(this);
        findViewById(R.id.sideMenuPowerOffTv).setOnClickListener(this);
        findViewById(R.id.sideMenuPowerIv).setOnClickListener(this);

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sideMenuCancelIv:
                listener.btnCancel();
                break;
            case R.id.sideMenuPowerOffTv:
            case R.id.sideMenuPowerIv:
                listener.powerOff();
                break;
            default:
                break;
        }
    }

}

