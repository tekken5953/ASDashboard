package com.example.dashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;


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

        void fan1();

        void fan2();

        void fan3();

        void fan4();

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
        findViewById(R.id.sideMenuFan1Tv).setOnClickListener(this);
        findViewById(R.id.sideMenuFan2Tv).setOnClickListener(this);
        findViewById(R.id.sideMenuFan3Tv).setOnClickListener(this);
        findViewById(R.id.sideMenuFan4Tv).setOnClickListener(this);
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
            case R.id.sideMenuFan1Tv:
                listener.fan1();
                break;
            case R.id.sideMenuFan2Tv:
                listener.fan2();
                break;
            case R.id.sideMenuFan3Tv:
                listener.fan3();
                break;
            case R.id.sideMenuFan4Tv:
                listener.fan4();
                break;
            default:
                break;
        }
    }

}

