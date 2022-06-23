package com.example.dashboard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.robinhood.spark.SparkView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import mobi.gspd.segmentedbarview.Segment;
import mobi.gspd.segmentedbarview.SegmentedBarView;
import mobi.gspd.segmentedbarview.SegmentedBarViewSideStyle;

public class MainActivity extends AppCompatActivity {

    ArrayList<RecyclerViewItem> mList = new ArrayList<>();
    RecyclerViewAdapter adapter;
    RecyclerView recyclerView = null;

    ProgressBar circleChart;

    SparkView sparkView;

    TextView currentTimeTv, category1, category2, category3, category4, category5, category6, categoryTitle;
    TextView aqiContentTv, aqiTitleTv, tempTitleTv, humidTitleTv, dayOfNightTv;
    ImageView menu;

    private ViewGroup mainLayout;   //사이드 나왔을때 클릭방지할 영역
    private ViewGroup viewLayout;   //전체 감싸는 영역
    private ViewGroup sideLayout;   //사이드바만 감싸는 영역
    private Boolean isMenuShow = false;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init(); //변수 초기화

        currentTimeIndex(); // 현재 시간 적용

        Configuration configuration = new Configuration();

        if (SharedPreferenceManager.getString(MainActivity.this, "language").equals("en")) {
            configuration.setLocale(Locale.US);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else if (SharedPreferenceManager.getString(MainActivity.this, "language").equals("ko")) {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }

        // 넘버차트 아이템 추가
        addItem(getResources().getString(R.string.fine_dust), "5888", "ug/㎥", getResources().getString(R.string.good));
        addItem(getResources().getString(R.string.Volatile_organic_compounds), "8.888", "mg/㎥", getResources().getString(R.string.normal));
        addItem(getResources().getString(R.string.co2), "1888", "ppm", getResources().getString(R.string.baddest));
        addItem(getResources().getString(R.string.co), "1000", "ppm", getResources().getString(R.string.bad));
        addItem(getResources().getString(R.string.virus), "123", null, getResources().getString(R.string.error));

        adapter.notifyDataSetChanged(); // 데이터 갱신

    }

    public void init() {

        recyclerView = findViewById(R.id.recyclerView);
        circleChart = findViewById(R.id.apiCircleChartPb);
        currentTimeTv = findViewById(R.id.currentTimeTv);
        menu = findViewById(R.id.hambugerMenuIv);
        categoryTitle = findViewById(R.id.textView7);
        category1 = findViewById(R.id.category1);
        category2 = findViewById(R.id.category2);
        category3 = findViewById(R.id.category3);
        category4 = findViewById(R.id.category4);
        category5 = findViewById(R.id.category5);
        category6 = findViewById(R.id.category6);
        sparkView = findViewById(R.id.virusLineChart);   //선그래프
        dayOfNightTv = findViewById(R.id.dayOfNightTv);
        aqiContentTv = findViewById(R.id.aqiContentTv);
        aqiTitleTv = findViewById(R.id.aqiTitleTv);
        tempTitleTv = findViewById(R.id.textView6);
        humidTitleTv = findViewById(R.id.textView2);

        menu.setOnClickListener(this::onClick);
        mainLayout = findViewById(R.id.id_main); // 대쉬보드 메인화면
        viewLayout = findViewById(R.id.fl_silde); // 사이드메뉴 전체프레임
        sideLayout = findViewById(R.id.view_sildebar); // 사이드메뉴 컨텐츠뷰
        addSideView(); //사이드 메뉴 활성화

        circleChart.setProgress(100);

        adapter = new RecyclerViewAdapter(mList); // 외부어댑터 연동
        recyclerView.setAdapter(adapter); // 어댑터 설정
    }

    public void addItem(String title, String number, String unit, String status) {
        RecyclerViewItem item = new RecyclerViewItem(title, number, unit, status);

        item.setTitle(title);
        item.setNumber(number);
        item.setUnit(unit);
        item.setStatus(status);

        mList.add(item);
    }

    public void currentTimeIndex() {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Date currentTime = new Date(System.currentTimeMillis());
                Calendar calendar = Calendar.getInstance();
                String a = null;
                super.handleMessage(msg);
                int s = calendar.get(Calendar.HOUR_OF_DAY);
                //오전일때
                if (calendar.get(Calendar.HOUR_OF_DAY) < 12) {
                    a = getResources().getString(R.string.day);
                    dayOfNightTv.setText(a);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s + ":mm");
                    simpleDateFormat.setCalendar(calendar);
                    currentTimeTv.setText(simpleDateFormat.format(currentTime));
                } else {
                    //오후일때
                    s -= 12;
                    a = getResources().getString(R.string.night);
                    dayOfNightTv.setText(a);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s + ":mm");
                    simpleDateFormat.setCalendar(calendar);
                    simpleDateFormat.format(currentTime);
                    currentTimeTv.setText(simpleDateFormat.format(currentTime));
                }

                aqiTitleTv.setText(getResources().getString(R.string.aqi));
                aqiContentTv.setText(getResources().getString(R.string.good));
                tempTitleTv.setText(getResources().getString(R.string.temp));
                humidTitleTv.setText(getResources().getString(R.string.humid));
                categoryTitle.setText(getResources().getString(R.string.aqi_1_hour));
                category1.setText(getResources().getString(R.string.aqi));
                category2.setText(getResources().getString(R.string.fine_dust));
                category3.setText(getResources().getString(R.string.Volatile_organic_compounds));
                category4.setText(getResources().getString(R.string.co2));
                category5.setText(getResources().getString(R.string.co));
                category6.setText(getResources().getString(R.string.virus));

            }
        };

        Runnable task = () -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(1); // 핸들러 호출(시간 최신화)
            }
        };

        Thread thread = new Thread(task);
        thread.start();
    }

    //햄버거 메뉴 추가
    private void addSideView() {
        SideBarCustomView sidebar = new SideBarCustomView(MainActivity.this);
        sideLayout.addView(sidebar);

        viewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        sidebar.setEventListener(new SideBarCustomView.EventListener() {
            @Override
            public void btnCancel() {
                closeMenu();
            }
        });
    }

    public void closeMenu() {
        isMenuShow = false;
        Animation slide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.sidebar_hidden);
        sideLayout.startAnimation(slide);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                viewLayout.setVisibility(View.GONE);
                viewLayout.setEnabled(false);
                mainLayout.setEnabled(true);
                mainLayout.bringToFront();

                //어플 재시작
                Intent intent = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
        }, 400);
    }

    public void showMenu() {
        isMenuShow = true;
        Animation slide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.sidebar_show);
        sideLayout.startAnimation(slide);
        viewLayout.setVisibility(View.VISIBLE);
        viewLayout.setEnabled(true);
        mainLayout.setEnabled(false);
        viewLayout.bringToFront();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.hambugerMenuIv):
                // 사이드 메뉴 클릭 시 이벤트
                if (!isMenuShow) {
                    showMenu();
                } else {
                    closeMenu();
                }
                break;
            case (R.id.category1):
                // 공기질 통합지수 카테고리 클릭 시 이벤트
                CategoryChoice(category1);
                CategoryNotChoice(category2, category3, category4, category5, category6);
                break;
            case (R.id.category2):
                CategoryChoice(category2);
                CategoryNotChoice(category1, category3, category4, category5, category6);
                break;

            case (R.id.category3):
                CategoryChoice(category3);
                CategoryNotChoice(category2, category1, category4, category5, category6);
                break;

            case (R.id.category4):
                CategoryChoice(category4);
                CategoryNotChoice(category2, category3, category1, category5, category6);
                break;

            case (R.id.category5):
                CategoryChoice(category5);
                CategoryNotChoice(category2, category3, category4, category1, category6);
                break;

            case (R.id.category6):
                CategoryChoice(category6);
                CategoryNotChoice(category2, category3, category4, category5, category1);
                break;
        }
    }

    public void CategoryChoice(TextView tv) {
        tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline));
        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    public void CategoryNotChoice(TextView tv1, TextView tv2, TextView tv3, TextView tv4, TextView tv5) {
        tv1.setBackground(null);
        tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
        tv2.setBackground(null);
        tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
        tv3.setBackground(null);
        tv3.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
        tv4.setBackground(null);
        tv4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
        tv5.setBackground(null);
        tv5.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
    }
}