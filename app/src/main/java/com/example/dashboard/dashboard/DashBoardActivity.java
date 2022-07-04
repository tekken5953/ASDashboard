package com.example.dashboard.dashboard;

import static com.example.dashboard.BluetoothAPI.REQUEST_INDIVIDUAL_STATE;
import static com.example.dashboard.BluetoothAPI.analyzedControlBody;
import static com.example.dashboard.BluetoothAPI.analyzedRequestBody;
import static com.example.dashboard.BluetoothAPI.byteArrayToHexString;
import static com.example.dashboard.BluetoothAPI.makeFrame;
import static com.example.dashboard.BluetoothAPI.separatedFrame;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.dashboard.BluetoothAPI;
import com.example.dashboard.BluetoothThread;
import com.example.dashboard.R;
import com.example.dashboard.SegmentedProgressBar;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.connect.ConnectDeviceActivity;
import com.example.dashboard.databinding.ActivityDashboardBinding;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DashBoardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;

    String currentTimeIndex;

    int barViewWidth, barViewHeight, arrowWidth, VIEW_REQUEST_INTERVAL = 3, DRAW_CHART_INTERVAL = 1000 * 2;

    DisplayMetrics dm = new DisplayMetrics();

    ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private Boolean isMenuShow = false;

    BluetoothAdapter bluetoothAdapter;
    BluetoothThread bluetoothThread;
    ArrayList<BluetoothDevice> arrayListDevice;
    BluetoothThread.DataShareViewModel viewModel;
    Set<BluetoothDevice> paredDevice;

    //Line Chart Values
    ArrayList<Entry> chartData = new ArrayList<>();
    ArrayList<ILineDataSet> lineDataSet = new ArrayList<>();
    LineData lineData;
    Legend legend = new Legend();

    Observer<String> data;
    int device_type, chartYIndex = 0;
    String serialNumber = null;

    private final String PROTOCOL_BLUETOOTH = "PROTOCOL_BLUETOOTH";
    private final String GET_CONTROL = "GET_CONTROL";
    private final String RESPONSE_SETUP_DATE = "RESPONSE_SETUP_DATE";
    private final String RESPONSE_SERIAL_NUMBER = "RESPONSE_SERIAL_NUMBER";
    private final String RESPONSE_DEVICE_PORT = "RESPONSE_DEVICE_PORT";
    private final String RESPONSE_DATA_INTERVAL = "RESPONSE_DATA_INTERVAL";
    private final String RESPONSE_BATTERY_REMAINED = "RESPONSE_BATTERY_REMAINED";
    private final String GET_WIFI_STATE = "GET_WIFI_STATE";
    private final String RESPONSE_SERVER_IP = "RESPONSE_SERVER_IP";
    private final String GET_CONTROL_RESULT = "GET_CONTROL_RESULT";
    Bundle dashBundle;
    String temp_str = null, humid_str = null, pm_str = null, co_str = null, co2_str = null, tvoc_str = null;
    String pm_grade = null, co_grade = null, co2_grade = null, tvoc_grade = null;
    String aqi_str = null;
    Short aqi_short = 0;


    Timer data_scheduler, chart_scheduler;

    ChartDataClass chartDataClass = new ChartDataClass();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothThread.isConnected()) {
            bluetoothThread.closeSocket();
        }
        if (bluetoothThread.isRunning()) {
            bluetoothThread.interrupt();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(this).get(BluetoothThread.DataShareViewModel.class);
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);

        hideNavigationBar(); // 하단 바 없애기
        init(); //뷰 바인딩


        Configuration configuration = new Configuration();

        if (SharedPreferenceManager.getString(DashBoardActivity.this, "finalLanguage").equals("en")) {
            configuration.setLocale(Locale.US);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else if (SharedPreferenceManager.getString(DashBoardActivity.this, "finalLanguage").equals("ko")) {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }

        getWindowManager().getDefaultDisplay().getMetrics(dm); // 기기 해상도를 구하기 위함

        CreateSegmentProgressView(); // AQI 바 차트 그리기

        // Line Chart 그리기
        // https://weeklycoding.com/mpandroidchart/ 공식 홈페이지
        // https://junyoung-developer.tistory.com/174 x축을 시간형태로 변경
        // https://junyoung-developer.tistory.com/173?category=960204 그리기 참고자료

    }

    public void init() {

        currentTimeIndex(); // 현재 시간 적용

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        binding.paringDeviceTv.setText(getIntent().getExtras().getString("device_name"));

        binding.hambugerMenuIv.setOnClickListener(this::onClick);
//        addSideView(); //사이드 메뉴 활성화

        // 데이터 관리
        data = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                byte[] recvHex = BluetoothAPI.hexStringToByteArray(s);
                if (recvHex == null) return;

                byte[][] bundleOfHex = separatedFrame(recvHex);
            /*
            String stx          = byteArrayToHexString(bundleOfHex[0]);
            String length       = byteArrayToHexString(bundleOfHex[1]);
            String sequence     = byteArrayToHexString(bundleOfHex[2]);
            String command      = byteArrayToHexString(bundleOfHex[3]);
            String body         = byteArrayToHexString(bundleOfHex[4]);
            String etx          = byteArrayToHexString(bundleOfHex[5]);
            */
                String command = byteArrayToHexString(bundleOfHex[3]);
                Bundle body = null;

                switch (command) {
                    case "81":
                    case "83":
                        body = analyzedRequestBody(bundleOfHex[4]);
                        break;
                    case "82":
                        body = analyzedControlBody(bundleOfHex[4]);
                        break;
                }

                if (body == null) return;
                System.out.println("body is " + body);

                if (command.equals("82")) {
                    processControlBody(body);
                } else {
                    processRequestBody(body);
                }
            }
        };
        viewModel.getReceiveData().observe(this, data);

        startCheckBluetooth();

        ChartTimerTask(300);

    }

    @SuppressLint("MissingPermission")
    public void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "해당 기기는 블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                binding.contentProgressPb.setVisibility(View.VISIBLE);
                try {
                    pairedDeviceConnect();
                } catch (Exception e) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DashBoardActivity.this, "장치가 작동중인지 확인하여주세요!", Toast.LENGTH_SHORT).show();
                        }
                    }, 0);

                    backToConnectDevice();
                }

            } else {
                //븥루투스가 꺼져있음
                final AlertDialog.Builder builder = new AlertDialog.Builder(DashBoardActivity.this);
                final AlertDialog alertDialog = builder.create();
                builder.setTitle(getString(R.string.causion_title))
                        .setMessage(getString(R.string.causion_message))
                        .setPositiveButton(getString(R.string.causion_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivity(enableBtIntent);
                            }
                        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                backToConnectDevice();
                            }
                        }).setCancelable(false).show();
            }
        }
    }

    private void processRequestBody(Bundle body) {
        if (body.containsKey("47")) {
            device_type = BluetoothAPI.getDeviceType(body.getCharArray("47"));
            System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));

            if (device_type == 5) { // DeviceFragment.DEVICE_TYPE_ERROR
                Toast.makeText(this, "Error BT TAG 47", Toast.LENGTH_SHORT).show();
                return;
            }

            if (device_type == 3) { // DeviceFragment.DEVICE_TYPE_MINI
                // Wifi State 확인
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{
                                        0x48, 0x00, 0x00, // S/N
                                        0x65, 0x00, 0x00  // WIFI Connect State
                                },
                                bluetoothThread.getSequence()
                        )
                );

            } else {
                bluetoothThread.writeHex(makeFrame(
                        new byte[]{REQUEST_INDIVIDUAL_STATE},
                        new byte[]{
                                0x43, 0x00, 0x00, // GPS 위도
                                0x44, 0x00, 0x00, // GPS 경도
                                0x45, 0x00, 0x00, // 펌웨어버전
                                0x46, 0x00, 0x00  // 모듈설치날짜
                        },
                        bluetoothThread.getSequence()
                ));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPushData(device_type, body);
                }
            });
        }

        bluetoothThread.writeHex(makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{
                        0x10, 0x00, 0x00, // 온도
                        0x12, 0x00, 0x00, // 습도
//                        0x15, 0x00, 0x00, // H2 수소
                        0x1B, 0x00, 0x00, // CO 일산화탄소
                        0x1E, 0x00, 0x00, // CO2 이산화탄소
                        0x21, 0x00, 0x00, // TVOC 휘발성 유기화합물
                        0x09, 0x00, 0x00, // PM 미세먼지
                        0x0B, 0x00, 0x00  // AQI 지수
                },
                bluetoothThread.getSequence()
        ));

        bluetoothThread.writeHex(makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{
//                        0x11, 0x00, 0x00, // 온도 등급
//                        0x13, 0x00, 0x00, // 습도 등급
//                        0x16, 0x00, 0x00, // H2 수소 등급
                        0x1C, 0x00, 0x00, // CO 일산화탄소 등급
                        0x1F, 0x00, 0x00, // CO2 이산화탄소 등급
                        0x22, 0x00, 0x00, // TVOC 휘발성 유기화합물 등급
                        0x0A, 0x00, 0x00 // PM 미세먼지 등급
                },
                bluetoothThread.getSequence()
        ));

        if (body.containsKey("48")) {
            serialNumber = new String(body.getCharArray("48"));
        }

        if (body.containsKey("10")) {
            temp_str = body.getString("10").substring(0, 4);
            binding.tempTv.setText(temp_str);
        }

        if (body.containsKey("12")) {
            humid_str = body.getString("12").substring(0, 4);
            binding.humidTv.setText(humid_str);
        }

        if (body.containsKey("09")) {

            pm_str = body.getString("09");
            binding.listCardPMIndex.setText(pm_str);
        }

        if (body.containsKey("21")) {
            tvoc_str = body.getString("21");
            binding.listCardTVOCIndex.setText(tvoc_str);
        }

        if (body.containsKey("1B")) {
            co_str = body.getString("1B");
            binding.listCardCOIndex.setText(co_str);
        }

        if (body.containsKey("1E")) {
            co2_str = body.getString("1E");
            binding.listCardCO2Index.setText(co2_str);
        }

        if (body.containsKey("0A")) {
            pm_grade = body.getByte("0A") + "";
            binding.listCardPMGrade.setText(pm_grade);
            CardItemTextColor(pm_grade, binding.listCardPMUnit, binding.listCardPMGrade, binding.listCardPMIndex);
        }

        if (body.containsKey("22")) {
            tvoc_grade = body.getByte("22") + "";
            binding.listCardTVOCGrade.setText(tvoc_grade);
            CardItemTextColor(tvoc_grade, binding.listCardTVOCUnit, binding.listCardTVOCGrade, binding.listCardTVOCIndex);
        }

        if (body.containsKey("1C")) {
            co_grade = body.getByte("1C") + "";
            binding.listCardCOGrade.setText(co_grade);
            CardItemTextColor(co_grade, binding.listCardCOUnit, binding.listCardCOGrade, binding.listCardCOIndex);
        }

        if (body.containsKey("1F")) {
            co2_grade = body.getByte("1F") + "";
            binding.listCardCO2Grade.setText(co2_grade);
            CardItemTextColor(co2_grade, binding.listCardCO2Unit, binding.listCardCO2Grade, binding.listCardCO2Index);
        }

        if (body.containsKey("0B")) {
            aqi_str = body.getShort("0B") + "";
            aqi_short = body.getShort("0B");
            binding.aqiCurrentArrow.setText(aqi_str);
            moveBarChart((int) aqi_short);
        }
    }

    private void setPushData(int device_type, Bundle bundle) {
        bundle.putString("protocol", PROTOCOL_BLUETOOTH);
        bundle.putInt("47", device_type);
    }

    private void processControlBody(Bundle body) {
        // 임시
        if (body.containsKey("50") || body.containsKey("51")) {
            Toast.makeText(this, "BS-M이 종료됐습니다.", Toast.LENGTH_SHORT).show();
            backToConnectDevice();

            if (bluetoothThread.isConnected()) {
                bluetoothThread.closeSocket();
            }

        } else if (body.containsKey("46")) {
            if (body.getByte("46") == 0x01) {
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{0x46, 0x00, 0x00},
                                bluetoothThread.getSequence()
                        )
                );
                Toast.makeText(this, "설치날짜 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "설치날짜 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (body.containsKey("48")) {
            if (body.getByte("48") == 0x01) {
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{0x48, 0x00, 0x00},
                                bluetoothThread.getSequence()
                        )
                );
                Toast.makeText(this, "S/N 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "S/N 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (body.containsKey("49")) {
            if (body.getByte("49") == 0x01) {
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{0x49, 0x00, 0x00},
                                bluetoothThread.getSequence()
                        )
                );
                Toast.makeText(this, "포트 설정 정보 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "포트 설정 정보 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (body.containsKey("57")) {
            if (body.getByte("57") == 0x01) {
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{0x57, 0x00, 0x00},
                                bluetoothThread.getSequence()
                        )
                );
                Toast.makeText(this, "데이터 전송 간격 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "데이터 전송 간격 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (body.containsKey("69")) {
            if (body.getByte("69") == 0x01) {
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{0x69, 0x00, 0x00},
                                bluetoothThread.getSequence()
                        )
                );
                Toast.makeText(this, "Server IP 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Server IP 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Iterator<String> iterator = body.keySet().iterator();
            try {
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    byte result = body.getByte(key);

                    if (result == (byte) 0x01) {
                        Toast.makeText(this, "성공적으로 변경했습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NullPointerException | ClassCastException e) {
                e.printStackTrace();
            }
        }
        dashBundle.putBundle(GET_CONTROL_RESULT, body);
        Log.e("dashBundle", dashBundle.getBundle(GET_CONTROL_RESULT).toString());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            barViewWidth = binding.aqiBarChartPb.getWidth();
            barViewHeight = binding.aqiBarChartPb.getHeight();
            arrowWidth = binding.aqiCurrentArrow.getWidth();

            binding.aqiTitleTv.setText(getResources().getString(R.string.aqi));
            binding.aqiContentTv.setText(getResources().getString(R.string.good));
            binding.tempTv.setText(getResources().getString(R.string.temp));
            binding.humidIndexTv.setText(getResources().getString(R.string.humid));
            binding.textView7.setText(getResources().getString(R.string.aqi_1_hour));
            binding.category1.setText(getResources().getString(R.string.aqi));
            binding.category2.setText(getResources().getString(R.string.fine_dust));
            binding.category3.setText(getResources().getString(R.string.tvoc));
            binding.category4.setText(getResources().getString(R.string.co2));
            binding.category5.setText(getResources().getString(R.string.co));
            binding.category6.setText(getResources().getString(R.string.virus));

            //barChart 가로세로 구하기

            params.setMargins(-arrowWidth / 2, 0, 0, (int) getResources().getDimension(R.dimen.arrowBottom));
            binding.aqiCurrentArrow.setLayoutParams(params);

        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceConnect() {
        //페어링 된 디바이스
        if (getIntent().getExtras().getString("paired").equals("paired")) {

            int position = getIntent().getExtras().getInt("device_position");
            arrayListDevice = new ArrayList<>();
            bluetoothThread = new BluetoothThread(DashBoardActivity.this);
            paredDevice = bluetoothAdapter.getBondedDevices();
            if (!paredDevice.isEmpty()) {
                arrayListDevice.addAll(paredDevice);
                bluetoothThread.setBluetoothDevice(arrayListDevice.get(position));
                bluetoothThread.setConnectedSocketEventListener(new BluetoothThread.connectedSocketEventListener() {
                    @Override
                    public void onConnectedEvent() {
                        binding.contentProgressPb.setVisibility(View.GONE);
                        Toast.makeText(DashBoardActivity.this, "정상적으로 연결되었습니다", Toast.LENGTH_SHORT).show();
                        Log.d("bluetoothThread", "Bluetooth Socket is Connected");
                        Log.d("bluetoothThread", "setDevice by : " + bluetoothThread.getDeviceName());
//                     센서 장착 여부 및 GPS 정보 요청
                        bluetoothThread.writeHex(makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{
                                        0x35, 0x00, 0x00, // 센서연결확인
                                        //0x43, 0x00, 0x00, // GPS 위도
                                        //0x44, 0x00, 0x00, // GPS 경도
                                        //0x45, 0x00, 0x00, // 펌웨어버전
                                        //0x46, 0x00, 0x00, // 모듈설치날짜
                                        0x47, 0x00, 0x00  // 모델명
                                },
                                bluetoothThread.getSequence()
                        ));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    }
                });

                bluetoothThread.setDisconnectedSocketEventListener(new BluetoothThread.disConnectedSocketEventListener() {
                    @Override
                    public void onDisconnectedEvent() {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("bluetoothThread", "Bluetooth Socket is Disconnected");
                                Toast.makeText(DashBoardActivity.this, "블루투스 통신이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }, 0);

                        binding.contentProgressPb.setVisibility(View.VISIBLE);
                        bluetoothThread.connectSocket();
                    }
                });

                bluetoothThread.connectSocket();

                if (!bluetoothThread.isRunning()) {
                    bluetoothThread.start();
                    Log.d("bluetoothThread", "BluetoothThread is Run");
                } else {
                    binding.paringDeviceTv.setText(getString(R.string.not_paring));
                }
            }
            //페어링 되지 않은 디바이스
            else {
            }
            regParentListener(VIEW_REQUEST_INTERVAL, data_scheduler);
        }
    }

    private void regParentListener(int interval, Timer scheduler) {
        try {
            scheduler.cancel();
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        loopReceiveData(interval, scheduler);
    }

    private void loopReceiveData(int interval, Timer scheduler) {
        Timer loopScheduler = scheduler;
        TimerTask data_timerTask = new TimerTask() {
            @Override
            public void run() {
                if (bluetoothThread.isConnected())
                    bluetoothThread.writeHex(makeFrame(new byte[]{0x03}, new byte[]{(byte) 0xFF, 0x00, 0x00}, bluetoothThread.getSequence()));
                else
                    try {
                        loopScheduler.cancel();
                    } catch (NullPointerException | IllegalStateException e) {
                        e.printStackTrace();
                        backToConnectDevice();
                    }
            }
        };
        scheduler = new Timer();
        scheduler.scheduleAtFixedRate(data_timerTask, 0, interval * 1000L);
    }

    private void ChartTimerTask(int yMax) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chartDataClass.initChart(yMax);
                    }
                });
            }
        };
        chart_scheduler = new Timer();
        chart_scheduler.scheduleAtFixedRate(timerTask, 0, DRAW_CHART_INTERVAL);

    }

    //AQI Index 별 차트 이동거리 계산
    public void moveBarChart(int aqiNumber) {
        params.setMargins((aqiNumber * barViewWidth / 300) - (arrowWidth / 2),
                0,
                0,
                (int) getResources().getDimension(R.dimen.arrowBottom));  // 왼쪽, 위, 오른쪽, 아래 순서
        binding.aqiCurrentArrow.setLayoutParams(params);
        binding.aqiCurrentArrow.setText(aqiNumber + "");

        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
            if (aqiNumber < 51) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good_1920, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.good));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal_1920, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.normal));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad1920, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.bad));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressBad));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressBad));
            } else {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad_1920, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.baddest));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressWorst));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressWorst));
            }
        } else {
            if (aqiNumber < 51) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good1280, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.good));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal1280, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.normal));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad1280, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.bad));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressBad));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressBad));
            } else {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad1280, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.baddest));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressWorst));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressWorst));
            }
        }
    }

    //현재 시간 불러오기
    public void currentTimeIndex() {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler(Looper.getMainLooper()) {
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
                    binding.dayOfNightTv.setText(a);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s + ":mm");
                    simpleDateFormat.setCalendar(calendar);
                    binding.currentTimeTv.setText(simpleDateFormat.format(currentTime));
                    currentTimeIndex = simpleDateFormat.format(currentTime);
                } else {
                    //오후일때
                    s -= 12;
                    a = getResources().getString(R.string.night);
                    binding.dayOfNightTv.setText(a);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(s + ":mm");
                    simpleDateFormat.setCalendar(calendar);
                    simpleDateFormat.format(currentTime);
                    binding.currentTimeTv.setText(simpleDateFormat.format(currentTime));
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

//    //햄버거 메뉴 보여주기
//    public void showMenu() {
//        isMenuShow = true;
//        Animation slide = AnimationUtils.loadAnimation(DashBoardActivity.this, R.anim.sidebar_show);
//        binding.viewSildebar.startAnimation(slide);
//        binding.flSilde.setVisibility(View.VISIBLE);
//        binding.flSilde.setEnabled(true);
//        binding.idMain.setEnabled(false);
//        binding.flSilde.bringToFront();
//    }

    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.hambugerMenuIv):
                // 사이드 메뉴 클릭 시 이벤트
//                if (!isMenuShow) {
//                    showMenu();
//                } else {
//                    closeMenu();
//                }
                break;
            case (R.id.category1):
                // 공기질 통합지수 카테고리 클릭
                CategoryChoice(binding.category1);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category5, binding.category6);
                break;
            // 미세먼지 통합지수 카테고리 클릭
            case (R.id.category2):
                CategoryChoice(binding.category2);
                CategoryNotChoice(binding.category1, binding.category3, binding.category4, binding.category5, binding.category6);
                break;
            // 휘발성 유기화합물 통합지수 카테고리 클릭
            case (R.id.category3):
                CategoryChoice(binding.category3);
                CategoryNotChoice(binding.category2, binding.category1, binding.category4, binding.category5, binding.category6);
                break;
            // 이산화탄소 통합지수 카테고리 클릭
            case (R.id.category4):
                CategoryChoice(binding.category4);
                CategoryNotChoice(binding.category2, binding.category3, binding.category1, binding.category5, binding.category6);
                break;
            // 일산화탄소 통합지수 카테고리 클릭
            case (R.id.category5):
                CategoryChoice(binding.category5);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category1, binding.category6);
                break;
            // 바이러스 위험지수 통합지수 카테고리 클릭
            case (R.id.category6):
                CategoryChoice(binding.category6);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category5, binding.category1);
                break;
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(선택)
    public void CategoryChoice(TextView tv) {
        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
            tv.setTextSize(18);
            tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline));

            if (tv.getText().toString().equals(getString(R.string.aqi))) {
                chartYIndex = aqi_short;
                ChartTimerTask(300);
            } else if (tv.getText().toString().equals(getString(R.string.fine_dust))) {
                chartYIndex = Integer.parseInt(binding.listCardPMIndex.getText().toString());
                ChartTimerTask(75);
            } else if (tv.getText().toString().equals(getString(R.string.co))) {
                chartYIndex = Integer.parseInt(binding.listCardCOIndex.getText().toString());
                ChartTimerTask(11);
            } else if (tv.getText().toString().equals(getString(R.string.co2))) {
                chartYIndex = Integer.parseInt(binding.listCardCO2Index.getText().toString());
                ChartTimerTask(1200);
            } else if (tv.getText().toString().equals(getString(R.string.tvoc))) {
                chartYIndex = Integer.parseInt(binding.listCardTVOCIndex.getText().toString());
                ChartTimerTask(1);
            } else if (tv.getText().toString().equals(getString(R.string.virus))) {
                chartYIndex = Integer.parseInt("0");
                ChartTimerTask(300);
            }

        } else {
            tv.setTextSize(14);
            tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline_small));
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(미선택)
    public void CategoryNotChoice(TextView tv1, TextView tv2, TextView tv3, TextView
            tv4, TextView tv5) {
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

    private class ChartDataClass {

        //LineChart Draw
        // 차트 데이터 초기화
        void initChartData() {
            // 차트 그리는 엔트리 부분

            if (lineDataSet.isEmpty()) {
                LineDataSet set = new LineDataSet(chartData, null);
                lineDataSet.add(set);
                lineData = new LineData(lineDataSet);

                set.setFillColor(Color.parseColor("#147AD6")); // 차트 색상
                set.setDrawFilled(true);
                set.setHighlightEnabled(false);
                set.setLineWidth(2F); // 그래프 선 굵기
                set.setDrawValues(false); // 차트에 값 표시
                set.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
                set.setDrawCircleHole(false); // 원 안에 작은 원 표시
                set.setDrawCircles(false); // 원 표시
                set.setColor(Color.parseColor("#147AD6"));
            }
            addLastData();
        }

        void addLastData() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                        Log.e("Timertask", "index : " + chartIndex + ", x : " + (900 * chartXDataFloat / 24) + ", y : " + aqi_short);

                    chartData.add(new Entry(0, aqi_short));
                    chartData.add(new Entry(1, aqi_short));
                    chartData.add(new Entry(2, aqi_short));
                    chartData.add(new Entry(3, aqi_short));
                    chartData.add(new Entry(4, aqi_short));
                    chartData.add(new Entry(5, aqi_short));
                    Log.e("Timertask", lineData.getEntryCount() + "");

                    binding.virusLineChart.notifyDataSetChanged();
                }
            });
        }

        void removeFirstData() {
            chartData.remove(0);
        }

        // 차트 처리
        private void initChart(int setYMax) {
            binding.virusLineChart.setAutoScaleMinMaxEnabled(true);
            binding.virusLineChart.setDrawGridBackground(false);
            binding.virusLineChart.setBackgroundColor(Color.TRANSPARENT);
            binding.virusLineChart.setDrawBorders(false);
            binding.virusLineChart.setDragEnabled(false);
            binding.virusLineChart.setTouchEnabled(false);
            binding.virusLineChart.setScaleEnabled(false);

            legend.setEnabled(false);

            // X축
            XAxis xAxis = binding.virusLineChart.getXAxis();
            xAxis.setDrawLabels(true); // 라벨 표시 여부
            xAxis.setLabelCount(7); // 라벨 갯수

            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
            xAxis.setDrawAxisLine(false); // AxisLine 표시
            xAxis.setDrawGridLines(false); // GridLine 표시
//            xAxis.setValueFormatter(new TimeAxisValueFormat());

            // Y축
            YAxis yAxis = binding.virusLineChart.getAxisLeft();
            yAxis.setAxisMaximum(setYMax);
            yAxis.setAxisMinimum((0));

            yAxis.setTextColor(Color.parseColor("#FFFFFF"));
            yAxis.setValueFormatter(new YAxisValueFormat());
            yAxis.setGranularityEnabled(false);
            yAxis.setDrawLabels(true); // Y축 라벨 위치
            yAxis.setDrawGridLines(false); // GridLine 표시
            yAxis.setDrawAxisLine(false); // AxisLine 표시

            // 오른쪽 Y축 값
            YAxis yRAxisVal = binding.virusLineChart.getAxisRight();
            yRAxisVal.setDrawLabels(false);
            yRAxisVal.setDrawAxisLine(false);
            yRAxisVal.setDrawGridLines(false);

            binding.virusLineChart.getDescription().setEnabled(false); // 설명
            binding.virusLineChart.setData(lineData); // 데이터 설정
            initChartData(); // 차트 초기화
            binding.virusLineChart.invalidate(); // 다시 그리기
        }

        //Y축 엔트리 포멧
        private class YAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                String newValue = value + "";
                return newValue.substring(0, newValue.length() - 2);
            }
        }

        // X축 엔트리 포멧
        private class TimeAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                long valueToMinutes = TimeUnit.MINUTES.toMillis((long) value);
                Date timeMinutes = new Date(valueToMinutes);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatMinutes = new SimpleDateFormat("HH:mm");
                return formatMinutes.format(timeMinutes);
            }
        }

        private void removeChartView() {
            chartData.clear();
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

        binding.aqiBarChartPb.setContexts(barList);
    }

    public void CardItemTextColor(String s1, TextView s2, TextView tv1, TextView tv2) {
        tv1.setVisibility(View.VISIBLE);
        tv2.setVisibility(View.VISIBLE);
        s2.setVisibility(View.VISIBLE);
        switch (s1) {
            case "0":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                tv1.setText("좋음");
                break;
            case "1":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                tv1.setText("보통");
                break;
            case "2":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                tv1.setText("나쁨");
                break;
            case "3":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                tv1.setText("매우나쁨");
                break;
        }
    }

    public void backToConnectDevice() {
        Intent intent = new Intent(DashBoardActivity.this, ConnectDeviceActivity.class);
        startActivity(intent);
        finish();
    }

    private void hideNavigationBar() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.d("immersivemod", "Turning immersive mode mode off. ");
        } else {
            Log.d("immersivemod", "Turning immersive mode mode on.");
        }
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}