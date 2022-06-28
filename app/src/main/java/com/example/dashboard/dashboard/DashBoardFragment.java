package com.example.dashboard.dashboard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dashboard.R;
import com.example.dashboard.SegmentedProgressBar;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.connect.ConnectDeviceFragment;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DashBoardFragment extends AppCompatActivity {

    ArrayList<DashboardRecyclerItem> mList = new ArrayList<>();
    DashboardRecyclerAdapter adapter;
    RecyclerView recyclerView = null;

    TextView currentTimeTv, category1, category2, category3, category4, category5, category6, categoryTitle;
    TextView aqiContentTv, aqiTitleTv, tempTitleTv, humidTitleTv, dayOfNightTv, aqiCurrentArrow, paringDeviceTv;
    ImageView menu, circleChart;
    String currentTimeIndex;

    int barViewWidth, barViewHeight, arrowWidth;

    DisplayMetrics dm = new DisplayMetrics();

    SegmentedProgressBar barView;
    ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private ViewGroup mainLayout;   //사이드 나왔을때 클릭방지할 영역
    private ViewGroup viewLayout;   //전체 감싸는 영역
    private ViewGroup sideLayout;   //사이드바만 감싸는 영역
    private Boolean isMenuShow = false;
    Boolean isExitFlag = false;

    BluetoothAdapter bluetoothAdapter;

    Date date = new Date(System.currentTimeMillis());

    //Line Chart Values
    ArrayList<Entry> chartData = new ArrayList<>();
    ArrayList<ILineDataSet> lineDataSet = new ArrayList<>();
    LineData lineData;
    com.github.mikephil.charting.charts.LineChart lineChart;
    Legend legend = new Legend();

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dashboard);

        init(); //변수 초기화

        currentTimeIndex(); // 현재 시간 적용

        Configuration configuration = new Configuration();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (SharedPreferenceManager.getString(DashBoardFragment.this, "finalLanguage").equals("en")) {
            configuration.setLocale(Locale.US);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else if (SharedPreferenceManager.getString(DashBoardFragment.this, "finalLanguage").equals("ko")) {
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

        getWindowManager().getDefaultDisplay().getMetrics(dm); // 기기 해상도를 구하기 위함

        CreateSegmentProgressView(); // AQI 바 차트 그리기

        //AQI Index 별 이동 메서드
        humidTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBarChart(65);
            }
        });

        dayOfNightTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBarChart(235);
            }
        });

        // Line Chart 그리기
        // https://weeklycoding.com/mpandroidchart/ 공식 홈페이지
        // https://junyoung-developer.tistory.com/174 x축을 시간형태로 변경
        // https://junyoung-developer.tistory.com/173?category=960204 그리기 참고자료

        initChart(); //그래프 차트 그리기

    }

    public void init() {

        barView = findViewById(R.id.aqiBarChartPb);

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
        lineChart = findViewById(R.id.virusLineChart);   //선그래프
        dayOfNightTv = findViewById(R.id.dayOfNightTv);
        aqiContentTv = findViewById(R.id.aqiContentTv);
        aqiTitleTv = findViewById(R.id.aqiTitleTv);
        tempTitleTv = findViewById(R.id.textView6);
        humidTitleTv = findViewById(R.id.textView2);
        aqiCurrentArrow = findViewById(R.id.aqiCurrentArrow);
        paringDeviceTv = findViewById(R.id.paringDeviceTv);

        menu.setOnClickListener(this::onClick);
        mainLayout = findViewById(R.id.id_main); // 대쉬보드 메인화면
        viewLayout = findViewById(R.id.fl_silde); // 사이드메뉴 전체프레임
        sideLayout = findViewById(R.id.view_sildebar); // 사이드메뉴 컨텐츠뷰
//        addSideView(); //사이드 메뉴 활성화

        adapter = new DashboardRecyclerAdapter(mList); // 외부어댑터 연동
        recyclerView.setAdapter(adapter); // 어댑터 설정
    }

    public void addItem(String title, String number, String unit, String status) {
        DashboardRecyclerItem item = new DashboardRecyclerItem(title, number, unit, status);

        item.setTitle(title);
        item.setNumber(number);
        item.setUnit(unit);
        item.setStatus(status);

        mList.add(item);
    }

    //LineChart Draw
    // 차트 데이터 초기화
    private void initChartData() {
        // 차트 그리는 엔트리 부분
        chartData.add(0, new Entry(180, 0f));
        chartData.add(1, new Entry(190, 0.7f));
        chartData.add(2, new Entry(200, 1f));
        chartData.add(3, new Entry(210, 4f));
        chartData.add(4, new Entry(220, 2f));
        chartData.add(5, new Entry(230, 1.2f));
        chartData.add(6, new Entry(240, 3f));

        LineDataSet set = new LineDataSet(chartData, "test data1");
        lineDataSet.add(set);
        lineData = new LineData(lineDataSet);

        set.setFillColor(R.color.lineChartLine); // 차트 색상
        set.setDrawFilled(true);
        set.setLineWidth(2F); // 그래프 선 굵기
        set.setDrawValues(false); // 차트에 값 표시
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
        set.setDrawCircleHole(false); // 원 안에 작은 원 표시
        set.setDrawCircles(false); // 원 표시
    }

    // 차트 처리
    private void initChart() {
        initChartData();
        // 차트 초기화
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setDrawBorders(false);
        lineChart.setAutoScaleMinMaxEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setScaleEnabled(false);

        legend.setEnabled(false);

        // X축
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(true); // 라벨 표시 여부
        xAxis.setAxisMaximum(240); // 60min * 24hours
        xAxis.setAxisMinimum(180);
        xAxis.setLabelCount(7, true); // 라벨 갯수

        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
        xAxis.setDrawAxisLine(false); // AxisLine 표시
        xAxis.setDrawGridLines(false); // GridLine 표시
        xAxis.setValueFormatter(new TimeAxisValueFormat());

        // Y축
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMaximum(4.5f);
        yAxis.setAxisMinimum(-0.5f);

        // Y축 도메인 변경
        String[] yAxisVal = new String[]{"0", "51", "101", "251", "300"};

        yAxis.setTextColor(R.color.white);
        yAxis.setValueFormatter(new IndexAxisValueFormatter(yAxisVal));
        yAxis.setGranularityEnabled(false);
        yAxis.setDrawLabels(true); // Y축 라벨 위치
        yAxis.setDrawGridLines(false); // GridLine 표시
        yAxis.setDrawAxisLine(false); // AxisLine 표시

        // 오른쪽 Y축 값
        YAxis yRAxisVal = lineChart.getAxisRight();
        yRAxisVal.setDrawLabels(false);
        yRAxisVal.setDrawAxisLine(false);
        yRAxisVal.setDrawGridLines(false);

        lineChart.getDescription().setEnabled(false); // 설명
        lineChart.setData(lineData); // 데이터 설정
        lineChart.invalidate(); // 다시 그리기
    }

    private static class TimeAxisValueFormat extends IndexAxisValueFormatter {

        @Override
        public String getFormattedValue(float value) {
            //Float(min) -> Date
//            Date currentTime = new Date(System.currentTimeMillis());
//            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatMinutes = new SimpleDateFormat("HH:mm");
//            Calendar calendar = Calendar.getInstance();
//            formatMinutes.setCalendar(calendar);
//            return formatMinutes.format(currentTime);
            long valueToMinutes = TimeUnit.MINUTES.toMillis((long)value);
            Date timeMinutes = new Date(valueToMinutes);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatMinutes = new SimpleDateFormat("HH:mm");
            return formatMinutes.format(timeMinutes);
        }
    }

    //barChart 가로세로 구하기
    @SuppressLint("MissingPermission")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            barViewWidth = barView.getWidth();
            barViewHeight = barView.getHeight();
            arrowWidth = aqiCurrentArrow.getWidth();

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

            params.setMargins(-arrowWidth / 2, 0, 0, (int) getResources().getDimension(R.dimen.arrowBottom));
            aqiCurrentArrow.setLayoutParams(params);

            Set<BluetoothDevice> paredDevice = bluetoothAdapter.getBondedDevices();
            if (!paredDevice.isEmpty()) {
                for (BluetoothDevice device : paredDevice) {
//                    paringDeviceTv.setText(device.getName());
                    paringDeviceTv.setText(getIntent().getExtras().getString("device_name"));
                }
            } else {
                paringDeviceTv.setText(getString(R.string.not_paring));
                final AlertDialog.Builder builder = new AlertDialog.Builder(DashBoardFragment.this);
                final AlertDialog alertDialog = builder.create();
                builder.setTitle(getString(R.string.causion_title))
                        .setMessage(getString(R.string.causion_message))
                        .setPositiveButton(getString(R.string.causion_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                                Intent intent = new Intent(DashBoardFragment.this, ConnectDeviceFragment.class);
                                startActivity(intent);
                                finish();
                            }
                        }).show();
            }
        }
    }

    //AQI Index 별 차트 이동거리 계산
    public void moveBarChart(int aqiNumber) {
        params.setMargins((aqiNumber * barViewWidth / 300) - (arrowWidth / 2),
                0,
                0,
                (int) getResources().getDimension(R.dimen.arrowBottom));  // 왼쪽, 위, 오른쪽, 아래 순서입니다.
        aqiCurrentArrow.setLayoutParams(params);
        aqiCurrentArrow.setText(aqiNumber + "");

        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
            if (aqiNumber < 51) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good_1920, null));
                aqiContentTv.setText(getResources().getString(R.string.good));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal_1920, null));
                aqiContentTv.setText(getResources().getString(R.string.normal));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad1920, null));
                aqiContentTv.setText(getResources().getString(R.string.bad));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressBad));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressBad));
            } else {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad_1920, null));
                aqiContentTv.setText(getResources().getString(R.string.baddest));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressWorst));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressWorst));
            }
        } else {
            if (aqiNumber < 51) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good1280, null));
                aqiContentTv.setText(getResources().getString(R.string.good));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal1280, null));
                aqiContentTv.setText(getResources().getString(R.string.normal));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad1280, null));
                aqiContentTv.setText(getResources().getString(R.string.bad));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressBad));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressBad));
            } else {
                circleChart.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad1280, null));
                aqiContentTv.setText(getResources().getString(R.string.baddest));
                aqiContentTv.setTextColor(getResources().getColor(R.color.progressWorst));
                aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressWorst));
            }
        }

        Toast.makeText(this, "차트의 총 길이(dp) : " + (int) getResources().getDimension(R.dimen.barWidth)
                        + "\nAqi 지수(int) : " + aqiNumber + "\n최고수치 300기준 차트이동거리(dp) : "
                        + aqiNumber * barViewWidth / 300,
                Toast.LENGTH_SHORT).show();
    }


    //현재 시간 불러오기
    public void currentTimeIndex() {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Date currentTime = new Date(System.currentTimeMillis());
                Calendar calendar = Calendar.getInstance();
                String a;
                super.handleMessage(msg);
                int s = calendar.get(Calendar.HOUR_OF_DAY);
                //오전일때
                if (calendar.get(Calendar.HOUR_OF_DAY) < 12) {
                    a = getResources().getString(R.string.day);
                    dayOfNightTv.setText(a);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s + ":mm");
                    simpleDateFormat.setCalendar(calendar);
                    currentTimeTv.setText(simpleDateFormat.format(currentTime));
                    currentTimeIndex = simpleDateFormat.format(currentTime);
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

//    //햄버거 메뉴 추가
//    private void addSideView() {
//        SideBarCustomView sidebar = new SideBarCustomView(DashBoardFragment.this);
//        sideLayout.addView(sidebar);
//
//        viewLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//            }
//        });
//
//        sidebar.setEventListener(new SideBarCustomView.EventListener() {
//            @Override
//            public void btnCancel() {
//                closeMenu();
//            }
//        });
//    }

    // 햄버거 메뉴 닫기
//    public void closeMenu() {
//        isMenuShow = false;
//        Animation slide = AnimationUtils.loadAnimation(DashBoardFragment.this, R.anim.sidebar_hidden);
//        sideLayout.startAnimation(slide);
//        viewLayout.setVisibility(View.GONE);
//        viewLayout.setEnabled(false);
//        mainLayout.setEnabled(true);
//        mainLayout.bringToFront();
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (!SharedPreferenceManager.getString(DashBoardFragment.this, "finalLanguage")
//                        .equals(SharedPreferenceManager.getString(DashBoardFragment.this, "currentLanguage"))) {
//                    SharedPreferenceManager.setString(DashBoardFragment.this, "finalLanguage",
//                            SharedPreferenceManager.getString(DashBoardFragment.this, "currentLanguage"));
//
//                    Log.e("languageLog", "Close menu\n" + "current : " + SharedPreferenceManager.getString(DashBoardFragment.this, "currentLanguage")
//                            + " final : " + SharedPreferenceManager.getString(DashBoardFragment.this, "finalLanguage"));
//
//                    //어플 재시작
//                    Intent intent = getBaseContext().getPackageManager()
//                            .getLaunchIntentForPackage(getBaseContext().getPackageName());
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
//                    overridePendingTransition(0, 0);
//                    finish();
//                }
//            }
//        }, 400);
//    }

    //햄버거 메뉴 보여주기
    public void showMenu() {
        isMenuShow = true;
        Animation slide = AnimationUtils.loadAnimation(DashBoardFragment.this, R.anim.sidebar_show);
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
//                    closeMenu();
                }
                break;
            case (R.id.category1):
                // 공기질 통합지수 카테고리 클릭
                CategoryChoice(category1);
                CategoryNotChoice(category2, category3, category4, category5, category6);
                break;
            // 미세먼지 통합지수 카테고리 클릭
            case (R.id.category2):
                CategoryChoice(category2);
                CategoryNotChoice(category1, category3, category4, category5, category6);
                break;
            // 휘발성 유기화합물 통합지수 카테고리 클릭
            case (R.id.category3):
                CategoryChoice(category3);
                CategoryNotChoice(category2, category1, category4, category5, category6);
                break;
            // 이산화탄소 통합지수 카테고리 클릭
            case (R.id.category4):
                CategoryChoice(category4);
                CategoryNotChoice(category2, category3, category1, category5, category6);
                break;
            // 일산화탄소 통합지수 카테고리 클릭
            case (R.id.category5):
                CategoryChoice(category5);
                CategoryNotChoice(category2, category3, category4, category1, category6);
                break;
            // 바이러스 위험지수 통합지수 카테고리 클릭
            case (R.id.category6):
                CategoryChoice(category6);
                CategoryNotChoice(category2, category3, category4, category5, category1);
                break;
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(선택)
    public void CategoryChoice(TextView tv) {
        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
            tv.setTextSize(18);
            tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline));
        } else {
            tv.setTextSize(14);
            tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline_small));
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(미선택)
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

        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
            tv1.setTextSize(18);
            tv2.setTextSize(18);
            tv3.setTextSize(18);
            tv4.setTextSize(18);
            tv5.setTextSize(18);
        } else {
            tv1.setTextSize(14);
            tv2.setTextSize(14);
            tv3.setTextSize(14);
            tv4.setTextSize(14);
            tv5.setTextSize(14);
        }
    }

    // AQI 바 차트 그리기
    public void CreateSegmentProgressView() {

        barList.add(new SegmentedProgressBar.BarContext(
                Color.parseColor("#5CC2E4"),  //gradient start
                Color.parseColor("#5CC2E4"),  //gradient stop
                0.169f //percentage for segment
        ));
        barList.add(new SegmentedProgressBar.BarContext(
                Color.parseColor("#1ccf7f"),
                Color.parseColor("#1ccf7f"),
                0.169f));
        barList.add(new SegmentedProgressBar.BarContext(
                Color.parseColor("#FBC93D"),
                Color.parseColor("#FBC93D"),
                0.5f));
        barList.add(new SegmentedProgressBar.BarContext(
                Color.parseColor("#FB4F4F"),
                Color.parseColor("#FB4F4F"),
                0.162f));

        barView.setContexts(barList);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(DashBoardFragment.this, ConnectDeviceFragment.class);
        startActivity(intent);
        finish();
    }
}
