/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
 * 1차 배포수준 개발 종료 : 2022-09-10
 * 1차 업데이트 종료 - MQTT 추가 : 2022-10-11
 */

package com.example.dashboard.dashboard;

import static com.example.dashboard.bluetooth.BluetoothAPI.analyzedControlBody;
import static com.example.dashboard.bluetooth.BluetoothAPI.analyzedRequestBody;
import static com.example.dashboard.bluetooth.BluetoothAPI.byteArrayToHexString;
import static com.example.dashboard.bluetooth.BluetoothAPI.generateTag;
import static com.example.dashboard.bluetooth.BluetoothAPI.makeFrame;
import static com.example.dashboard.bluetooth.BluetoothAPI.separatedFrame;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageView;
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

import com.example.dashboard.Mqtt;
import com.example.dashboard.OnSingleClickListener;
import com.example.dashboard.OuterClass;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.bluetooth.BluetoothAPI;
import com.example.dashboard.bluetooth.BluetoothThread;
import com.example.dashboard.bluetooth.VirusFormulaClass;
import com.example.dashboard.databinding.ActivityDashboardBinding;
import com.example.dashboard.language.LanguageSelectActivity;
import com.example.dashboard.ui.SegmentedProgressBar;
import com.example.dashboard.ui.SideBarCustomView;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class DashBoardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;

    Activity context = DashBoardActivity.this;

    private final String TAG_BTThread = "BTThread";
    private final String TAG_LifeCycle = "DashBoardLifeCycle";
    private final String TAG_MQTT = "MqttLog";
    final String FAN_CONTROL_COMPLETE = "com.example.dashboard";
    private final byte REQUEST_CONTROL = (byte) 0x02;
    private final byte REQUEST_INDIVIDUAL_STATE = (byte) 0x01;
    final int VIEW_REQUEST_INTERVAL = 7, DRAW_CHART_INTERVAL = 60 * 10;
    int count = 0;
    int mqttCount = 0;

    private Boolean isMenuShow = false;

    BluetoothAdapter bluetoothAdapter;
    BluetoothThread bluetoothThread;
    ArrayList<BluetoothDevice> arrayListDevice;
    ArrayList<String> xLabelList = new ArrayList<>();
    BluetoothThread.DataShareViewModel viewModel;
    Set<BluetoothDevice> paredDevice;

    Observer<String> data;
    String serialNumber, deviceType, modelName, setUpDateStr;

    long CHART_MADE_TIME = 0;

    Timer dataScheduler, chartScheduler;
    TimerTask data_timerTask;

    String temp_str, humid_str, pm_str, co_str, co2_str, tvoc_str;
    String pm_grade, co_grade, co2_grade, tvoc_grade, virusIndex;
    Short pm_aqi_short;
    int virusValue, barViewWidth, barViewHeight, arrowWidth, cqiIndex = 0, failCount = 0;
    Float pm_float, co_float, co2_float, tvoc_float, humid_float, temp_float;
    byte fan_control_byte, current_fan_byte, power_control_byte;

    SideBarCustomView sidebar;
    OuterClass outerClass = new OuterClass();
    DrawGraphClass drawGraphClass = new DrawGraphClass();
    VirusFormulaClass virusFormulaClass = new VirusFormulaClass();

    Mqtt mqtt;
    JSONObject jsonMeasure = new JSONObject();
    JSONObject jsonSensor = new JSONObject();
    //    int jsonSensor = 0;
    String userCode = null;
    private boolean isConnected = false;
    ConnectivityManager manager;
    float batteryPct = -1;

    @Override
    protected void onDestroy() {
        Log.i(TAG_LifeCycle, "onDestroy");

        if (data_timerTask != null)
            data_timerTask.cancel();

        if (dataScheduler != null)
            dataScheduler.cancel();

        drawGraphClass.reDrawChart();

        if (bluetoothThread.isConnected())
            bluetoothThread.closeSocket();

        if (bluetoothThread.isRunning())
            bluetoothThread.interrupt();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.i(TAG_LifeCycle, "onResume");
        super.onResume();
        outerClass.FullScreenMode(context);

        // 배터리잔량 변화 불러오기
        GetBatteryState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG_LifeCycle, "onPause");
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG_LifeCycle, "onCreate");
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(DashBoardActivity.this, R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(DashBoardActivity.this).get(BluetoothThread.DataShareViewModel.class);
        binding.dashboardMainLayout.setEnabled(false);

        // 바인딩
        init();

        // 받아온 현재 기기의 국가를 설정합니다
        SetFinalLocale();

        // CQI 바 차트의 구성요소들의 길이정보를 구합니다
        GetBarChartDimens();

        // CQI 바 차트를 그립니다
        CreateSegmentProgressView();

        CHART_MADE_TIME = System.currentTimeMillis();

        binding.category1.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 공기질 통합지수 카테고리 클릭
                CategoryChoice(binding.category1);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category5, binding.category6);
            }
        });

        binding.category2.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 미세먼지 통합지수 카테고리 클릭
                CategoryChoice(binding.category2);
                CategoryNotChoice(binding.category1, binding.category3, binding.category4, binding.category5, binding.category6);
            }
        });

        binding.category3.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 휘발성 유기화합물 통합지수 카테고리 클릭
                CategoryChoice(binding.category3);
                CategoryNotChoice(binding.category2, binding.category1, binding.category4, binding.category5, binding.category6);
            }
        });

        binding.category4.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 이산화탄소 통합지수 카테고리 클릭
                CategoryChoice(binding.category4);
                CategoryNotChoice(binding.category2, binding.category3, binding.category1, binding.category5, binding.category6);
            }
        });

        binding.category5.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 일산화탄소 통합지수 카테고리 클릭
                CategoryChoice(binding.category5);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category1, binding.category6);
            }
        });

        binding.category6.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                // 바이러스 위험지수 통합지수 카테고리 클릭
                CategoryChoice(binding.category6);
                CategoryNotChoice(binding.category2, binding.category3, binding.category4, binding.category5, binding.category1);
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG_LifeCycle, "onWindowFocusChanged");
        if (hasFocus) {

            outerClass.FullScreenMode(context);

            // 븥루투스가 꺼져있을 경우 재 연결을 시도합니다
            outerClass.IfBluetoothIsNull(context, bluetoothAdapter);

            // CQI 바 차트의 구성요소들의 길이정보를 구합니다
            GetBarChartDimens();
        }
    }

    public void init() {
        // 초기화 작업
        userCode = getIntent().getExtras().getString("userCode");
        manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        isConnected = (networkInfo != null && networkInfo.isConnectedOrConnecting());
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);
        StartLoading();
        switchCheckedChange();
        isWifiLinked();
        checkWifiState();
        binding.listCardVIRUSIndex.setVisibility(View.GONE);
        binding.listCardVIRUSOCGrade.setVisibility(View.GONE);
        binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));

        SetSwitchState();

        // 사이드 메뉴의 클릭 이벤트를 등록합니다
        binding.hambugerMenuIv.setOnClickListener(this::onClick);
        binding.category1.setEnabled(false);

        CompletableFuture.runAsync(() -> {
            // 블루투스 어댑터를 초기화합니다
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }).thenAcceptAsync(b -> {
            // 블루투스 사용 여부를 체크합니다
            startCheckBluetooth();
        });

        // 데이터 관리
        data = new Observer<String>() {
            @Override
            public void onChanged(String s) {
                byte[] recvHex = BluetoothAPI.hexStringToByteArray(s);
                if (recvHex == null) return;

                byte[][] bundleOfHex = separatedFrame(recvHex);
                /**
                 String stx          = byteArrayToHexString(bundleOfHex[0]);
                 String length       = byteArrayToHexString(bundleOfHex[1]);
                 String sequence     = byteArrayToHexString(bundleOfHex[2]);
                 String command      = byteArrayToHexString(bundleOfHex[3]);
                 String body         = byteArrayToHexString(bundleOfHex[4]);
                 String etx          = byteArrayToHexString(bundleOfHex[5]);
                 */
                try {
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
                    } else if (command.equals("83")) {
                        HideLoading();
                        if (!isConnected && binding.dashWifiSwitch.isChecked()) {
                            try {
                                mqtt.connect();
                                Snackbar.make(binding.idMain, getString(R.string.mqtt_wifi_reconnect_s), Snackbar.LENGTH_SHORT).show();
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                Snackbar.make(binding.idMain, getString(R.string.mqtt_wifi_reconnect_f), Snackbar.LENGTH_SHORT).show();
                            }
                        }
                        if (jsonMeasure.length() != 0 && userCode != null) {
                            if (binding.dashWifiSwitch.isChecked()) {
                                try {
                                    //MQTT 센서 데이터 측정합니다
                                    if (mqttCount % 10 == 0 && jsonSensor.length() != 0) {
                                        try {
                                            //측정 데이터 퍼블리시
                                            publishMeasureChk();
                                        } catch (NullPointerException e) {
                                            // 만약 실패하면 2초 후에 다시 보냅니다
                                            Handler handler = new Handler(Looper.getMainLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // 측정 데이터 퍼블리시
                                                    publishMeasureChk();
                                                }
                                            }, 2000);
                                        }
                                    }
                                    // 데이터 전송
                                    publishMeasured();
                                    mqttCount++;
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        processRequestBody(body);
                    } else {
                        processRequestBody(body);
                    }
                } catch (ArrayIndexOutOfBoundsException | InterruptedException | JSONException e) {
                    Log.e(TAG_BTThread, "Error : " + e);
                }
            }
        };

        // 이제부터 뷰모델이 하단 아이템의 데이터를 관리합니다
        viewModel.getReceiveData().observe(this, data);
    }

    @SuppressLint("MissingPermission")
    public void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.no_bluetooth_device), Toast.LENGTH_SHORT).show();
            binding.dashBtIcon.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    binding.dashBtIcon.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.white, null));
                    pairedDeviceConnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //븥루투스가 꺼져있음
                        binding.dashBtIcon.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        final AlertDialog alertDialog = builder.create();
                        builder.setTitle(getString(R.string.caution_title))
                                .setMessage(getString(R.string.reconnect_bt_msg))
                                .setPositiveButton(getString(R.string.reconnect_bt_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        alertDialog.dismiss();
                                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                        startActivity(enableBtIntent);
                                    }
                                }).setNegativeButton(getString(R.string.caution_back), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        outerClass.GoToConnectFromLang(context);
                                    }
                                }).setCancelable(false).show();
                    }
                });
            }
        }
    }

    // 블루투스 요청 프로세스를 처리하는 함수입니다
    private void processRequestBody(Bundle body) throws InterruptedException, JSONException {
        if (body.containsKey("47")) {
            CompletableFuture.runAsync(() -> {
                deviceType = Arrays.toString(body.getCharArray("47"));

                Log.i(TAG_BTThread, "Device Type is " + deviceType);

                System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));
            }).thenAcceptAsync(b -> {

                bluetoothThread.writeHex(makeFrame(
                        new byte[]{REQUEST_INDIVIDUAL_STATE},
                        new byte[]{
                                0x10, 0x00, 0x00, // 온도
                                0x12, 0x00, 0x00, // 습도
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
                                0x1C, 0x00, 0x00, // CO 일산화탄소 등급
                                0x1F, 0x00, 0x00, // CO2 이산화탄소 등급
                                0x22, 0x00, 0x00, // TVOC 휘발성 유기화합물 등급
                                0x0A, 0x00, 0x00, // PM 미세먼지 등급
                                0x3A, 0x00, 0x00 // Fan 정보
                        },
                        bluetoothThread.getSequence()
                ));

            });
        }

        // S/N
        if (body.containsKey("48")) {
            serialNumber = new String(body.getCharArray("48"));
            Log.i(TAG_BTThread, "Serial Number is " + serialNumber);
        }

        // 설치날짜
        if (body.containsKey("46")) {
            int setup_date = body.getInt("46");
            setUpDateStr = setup_date + "";
            Log.i(TAG_BTThread, "SetUp Date is " + setup_date + "");
        }

        // 현재 풍량 정보
        if (body.containsKey("3A")) {
            current_fan_byte = body.getByte("3A");
            Log.i(TAG_BTThread, "Current Fan is " + current_fan_byte);
        }

        // 장착 센서 정보
        if (body.containsKey("35")) {
            String sensorMeasureStr = body.getString("35");
            StringBuilder sb = new StringBuilder(sensorMeasureStr);
            String reverseSensor = sb.reverse().toString();
            Log.d(TAG_MQTT, reverseSensor);
            int sensorMeasure = Integer.parseInt(reverseSensor, 2);
            jsonSensor.put("response", "getMeasure");
            jsonSensor.put("MeasureChk", sensorMeasure);
            Log.i(TAG_MQTT, "Total Sensor index : " + jsonSensor);
        }

        // 온도
        if (body.containsKey("10")) {
            temp_str = body.getString("10").substring(0, 4);
            temp_float = Float.parseFloat(temp_str);
            // 온도는 영하 20도 부터 영상 50도 까지만 변화를 적용합니다
            if (Float.parseFloat(temp_str) > -20f && Float.parseFloat(temp_str) < 50f) {
                binding.tempTv.setText(temp_str);
                jsonMeasure.put("TEMPval", temp_float);
            }
        }

        // 습도
        if (body.containsKey("12")) {
            humid_str = body.getString("12").substring(0, 4);
            humid_float = Float.parseFloat(humid_str);
            // 습도는 0% 부터 100% 까지만 변화를 적용합니다
            if (Float.parseFloat(humid_str) >= 0f && Float.parseFloat(humid_str) <= 100f) {
                binding.humidTv.setText(humid_str);
                jsonMeasure.put("HUMIDval", humid_float);
            }
        }

        //PM 유효정보
        if (body.containsKey("01")) {
            jsonMeasure.put("ValildPM", body.getByte(("01")));
        }

        //H2 유효정보
        if (body.containsKey("14")) {
            jsonMeasure.put("ValidH2", body.getByte("14"));
        }

        //H2 값
        if (body.containsKey("15")) {
            String h2_float = body.getString("15");
            if (body.getByte("14") == 1) {
                jsonMeasure.put("H2val", Float.parseFloat(h2_float));
            }
        }

        //H2 등급
        if (body.containsKey("16")) {
            byte h2_grade = body.getByte("16");
            if (body.getByte("14") == 1) {
                jsonMeasure.put("H2lvl", h2_grade);
            }
        }

        // PM2.5 값
        if (body.containsKey("09")) {
            pm_str = body.getString("09");
            pm_float = Float.parseFloat(pm_str);
            int i = Math.round(pm_float);
            // 0 ~ 150
            if (i >= 0 && i <= 100) {
                binding.listCardPMIndex.setText(i + "");
                jsonMeasure.put("PM2P5val", i);
            }
        }

        //TVOC 유효정보
        if (body.containsKey("20")) {
            jsonMeasure.put("ValidTVOC", body.getByte("20"));
        }

        // TVOC 값
        if (body.containsKey("21")) {
            tvoc_str = body.getString("21");
            tvoc_float = Float.parseFloat(tvoc_str);
            // 0 ~ 5
            if (body.getByte("20") == 1 && tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCIndex.setText(tvoc_str);
                jsonMeasure.put("TVOCval", tvoc_float);
            }
        }

        //CO 유효정보
        if (body.containsKey("1A")) {
            jsonMeasure.put("ValidCO", body.getByte("1A"));
        }

        // CO 값
        if (body.containsKey("1B")) {
            co_str = body.getString("1B");
            co_float = Float.parseFloat(co_str);
            // 0 ~ 18
            if (body.getByte("1A") == 1 && co_float >= 0f && co_float <= 50) {
                binding.listCardCOIndex.setText(co_str);
                jsonMeasure.put("COval", co_float);
            }
        }

        // CO2 유효정보
        if (body.containsKey("1D")) {
            jsonMeasure.put("ValidCO2", body.getByte("1D"));
        }

        // CO2 값
        if (body.containsKey("1E")) {
            co2_str = body.getString("1E");
            co2_float = Float.parseFloat(co2_str);
            // 0 ~ 2500
            if (body.getByte("1D") == 1 && co2_float >= 0 && co2_float <= 1999) {
                StringBuilder s = new StringBuilder();
                s.append(Math.round(co2_float));
                binding.listCardCO2Index.setText(s);
                jsonMeasure.put("CO2val", co2_float);
            }
        }

        // 미세먼지 등급
        if (body.containsKey("0A")) {
            pm_grade = body.getByte("0A") + "";
            // 0 ~ 150
            if (pm_float != null && pm_float >= 0f && pm_float <= 100f) {
                jsonMeasure.put("PM2P5lvl", body.getByte("0A"));
                binding.listCardPMGrade.setText(pm_grade);
                CardItemTextColor(pm_grade, binding.listCardPMTitle, binding.listCardPMUnit,
                        binding.listCardPMGrade, binding.listCardPMIndex, binding.listCardPMLoadingTv);
            }
        }

        // TVOC 등급
        if (body.containsKey("22")) {
            tvoc_grade = body.getByte("22") + "";
            // 0 ~ 5
            if (body.getByte("20") == 1 && tvoc_float != null && tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCGrade.setText(tvoc_grade);
                CardItemTextColor(tvoc_grade, binding.listCardTVOCTitle, binding.listCardTVOCUnit,
                        binding.listCardTVOCGrade, binding.listCardTVOCIndex, binding.listCardTVOCLoadingTv);
                jsonMeasure.put("TVOClvl", body.getByte("22"));
            }
        }

        //NH3 유효정보
        if (body.containsKey("26")) {
            jsonMeasure.put("ValideNH3", body.getByte("26"));
        }
        //NH3 값
        if (body.containsKey("27")) {
            if (body.getByte("26") == 1) {
                float nh3Index = Float.parseFloat(body.getString("27"));
                jsonMeasure.put("NH3val", nh3Index);
            }
        }
        //NH3 등급
        if (body.containsKey("28")) {
            if (body.getByte("26") == 1) {
                jsonMeasure.put("NH3lvl", body.getByte("28"));
            }
        }

        // CO 등급
        if (body.containsKey("1C")) {
            co_grade = body.getByte("1C") + "";
            // 0 ~ 18
            if (body.getByte("1A") == 1 && co_float != null && co_float >= 0f && co_float <= 15f) {
                binding.listCardCOGrade.setText(outerClass.translateData(co_grade, context));
                CardItemTextColor(co_grade, binding.listCardCOTitle, binding.listCardCOUnit,
                        binding.listCardCOGrade, binding.listCardCOIndex, binding.listCardCOLoadingTv);
                jsonMeasure.put("COlvl", body.getByte("1C"));
            }
        }

        // CO2 등급
        if (body.containsKey("1F")) {
            co2_grade = body.getByte("1F") + "";
            // 0 ~ 2500
            if (body.getByte("1D") == 1 && co2_float != null && Math.round(co2_float) >= 0 && Math.round(co2_float) <= 2000) {
                binding.listCardCO2Grade.setText(outerClass.translateData(co2_grade, context));
                CardItemTextColor(co2_grade, binding.listCardCO2Title, binding.listCardCO2Unit,
                        binding.listCardCO2Grade, binding.listCardCO2Index, binding.listCardCO2LoadingTv);
                jsonMeasure.put("CO2lvl", body.getByte("1F"));
            }
        }

        // PM 2.5 AQI 지수
        if (body.containsKey("0B")) {
            pm_aqi_short = body.getShort("0B");
            jsonMeasure.put("PM2P5aqi", Float.parseFloat(String.valueOf(pm_aqi_short)));
        }

        // 온습도 유효정보
        if (body.containsKey("0F")) {
            jsonMeasure.put("ValidTH", body.getByte("0F"));
        }

        //CQI 데이터 불러오고 그래프 그리기
        if (pm_float != null && co_float != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CompletableFuture.runAsync(() -> {
                        try {
                            GetCQIData();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }).thenAcceptAsync(d -> {
                        if (count == 0) {
                            if (bluetoothThread.isConnected()) {
                                DrawingGraphMethod();
                                count++;
                            }
                        }
                    });
                }
            });
        }

        if (pm_float != null && temp_float != null && humid_float != null && co2_float != null && tvoc_float != null) {
            GetVirusData();
        }
    }

    private void processControlBody(Bundle body) {
        // 임시
        if (body.containsKey("50")) {

            // 장치 꺼짐 호출
            if (power_control_byte == 0x01) {

                bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x50, new byte[]{0x01}), bluetoothThread.getSequence()));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bluetoothThread.getDeviceName().startsWith("BS_M")) {
                            // 어플 강제종료
//                        android.os.Process.killProcess(android.os.Process.myPid());
                            Toast.makeText(context, getString(R.string.exit_complete), Toast.LENGTH_SHORT).show();
                            // 어플 재시작
                            finishAffinity();
                            Intent intent = new Intent(DashBoardActivity.this, LanguageSelectActivity.class);
                            startActivity(intent);
                            System.exit(0);
                        }
                    }
                });
            }
        }
        // 펜의 풍량 제어
        else if (body.containsKey("3B")) {
            if (fan_control_byte != 0x00) {
                if (fan_control_byte == 0x01) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x01}), bluetoothThread.getSequence()));
                    Log.i(TAG_BTThread, "Fan 수면 단계");
                } else if (fan_control_byte == 0x02) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x02}), bluetoothThread.getSequence()));
                    Log.i(TAG_BTThread, "Fan 약 단계");
                } else if (fan_control_byte == 0x03) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x03}), bluetoothThread.getSequence()));
                    Log.i(TAG_BTThread, "Fan 강 단계");
                } else if (fan_control_byte == 0x04) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x04}), bluetoothThread.getSequence()));
                    Log.i(TAG_BTThread, "Fan 터보 단계");
                }
            } else {
                Log.e(TAG_BTThread, "Error 발생");
            }
        } else {
            Iterator<String> iterator = body.keySet().iterator();
            try {
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    byte result = body.getByte(key);

                    if (result == (byte) 0x01) {
                        Intent intent = new Intent();
                        intent.setAction(FAN_CONTROL_COMPLETE);
                        sendBroadcast(intent);

//                        Toast.makeText(this, "성공적으로 변경했습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.fail_to_change), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (NullPointerException | ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceConnect() {
        // 페어링 된 디바이스 포지션을 받아옵니다
        int position = getIntent().getExtras().getInt("device_position");

        arrayListDevice = new ArrayList<>();
        bluetoothThread = new BluetoothThread(context);
        paredDevice = bluetoothAdapter.getBondedDevices();

        // 페어링 된 디바이스가 없다면 불러온 디바이스를 모두 추가시킵니다
        if (!paredDevice.isEmpty()) {
            arrayListDevice.addAll(paredDevice);
            bluetoothThread.setBluetoothDevice(arrayListDevice.get(position));

            // 소켓이 연결 되었을 경우 발생하는 이벤트 리스너
            bluetoothThread.setConnectedSocketEventListener(new BluetoothThread.connectedSocketEventListener() {
                @Override
                public void onConnectedEvent() {
                    binding.dashboardMainLayout.setEnabled(true);
                    // 모델명을 불러옵니다
                    modelName = bluetoothThread.getDeviceName();
                    Log.i(TAG_BTThread, "Bluetooth Socket is Connected");
                    Log.i(TAG_BTThread, "setDevice by : " + bluetoothThread.getDeviceName());

                    // 최초 데이터인 모듈 설치날짜와 모델명을 API 를 통해 스레드에 요청합니다
//                                센서 장착 여부 및 GPS 정보 요청
                    bluetoothThread.writeHex(makeFrame(
                            new byte[]{REQUEST_INDIVIDUAL_STATE},
                            new byte[]{
                                    0x46, 0x00, 0x00, // 모듈설치날짜
                                    0x47, 0x00, 0x00,  // 모델명
                                    0x48, 0x00, 0x00,  // S/N
                                    0x3A, 0x00, 0x00,  // 현재바람세기
                                    0x35, 0x00, 0x00   // 모듈 정보
                            },
                            bluetoothThread.getSequence()
                    ));

                    try {
                        bluetoothThread.start();
                        regDataListener(dataScheduler);
                        Log.i(TAG_BTThread, "Loop Data Request");
                    } catch (InterruptedException | IllegalThreadStateException e) {
                        e.printStackTrace();
                    }
                }
            });

            // 소켓연결에 실패하였을 경우 발생하는 이벤트 리스너입니다
            bluetoothThread.setDisconnectedSocketEventListener(new BluetoothThread.disConnectedSocketEventListener() {
                @Override
                public void onDisconnectedEvent() {
                    binding.dashboardMainLayout.setEnabled(false);
                    Log.i(TAG_BTThread, "Bluetooth Socket is Disconnected");
                    Log.i(TAG_BTThread, "Running : " + bluetoothThread.isRunning());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bluetoothThread.isRunning()) {
                                bluetoothThread.connectSocket();
                            } else {
                                if (failCount == 2) {
                                    Snackbar.make(binding.idMain,
                                            getString(R.string.fail_count),
                                            Snackbar.LENGTH_SHORT).show();
                                    outerClass.GoToLanguageFromConnect(context);
                                } else {
                                    failCount++;
                                    HideLoading();
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            StartLoading();
                                            CompletableFuture.runAsync(() -> {
                                                bluetoothThread.connectSocket();
                                            }).thenAcceptAsync(b -> {
                                                try {
                                                    regDataListener(dataScheduler);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                Log.i(TAG_BTThread, "Loop Data Request");
                                            });
                                        }
                                    }, 2000);
                                }
                            }
                        }
                    });
                }
            });

            // 소켓을 연결합니다
            if (!bluetoothThread.isConnected())
                bluetoothThread.connectSocket();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG_BTThread, "pairedDevice is null");
        }
    }

    // 지속적으로 공기질 데이터를 요청하는 스케줄러입니다
    private void regDataListener(Timer scheduler) throws InterruptedException {
        try {
            if (scheduler != null) {
                scheduler.cancel();
            }
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        loopReceiveData(VIEW_REQUEST_INTERVAL);
    }

    // 스케쥴러가 호출하면 작업을 진행 할 타이머테스크입니다
    private void loopReceiveData(int interval) {
        if (bluetoothThread.isConnected()) {
            data_timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        bluetoothThread.writeHex(makeFrame(new byte[]{0x03}, new byte[]{(byte) 0xFF, 0x00, 0x00}, bluetoothThread.getSequence()));
                        Log.d(TAG_MQTT, "JSONObject is " + jsonMeasure);

                    } catch (NullPointerException | IllegalStateException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                HideLoading();
                            }
                        });
                    }
                }
            };
            Timer scheduler = new Timer();
            scheduler.scheduleAtFixedRate(data_timerTask, 0, interval * 1000L);
        }
    }

    // 일정 시간마다 선 그래프를 그립니다
    private void ChartTimerTask(int yMax, String s) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 사용자가 요청 한 정보에 따라 다른 그래프를 그립니다
                        switch (s) {
                            case "cqi":
                                drawGraphClass.feedMultiple(yMax, cqiIndex);
                                break;
                            case "pm":
                                drawGraphClass.feedMultiple(yMax, pm_float.intValue());
                                break;
                            case "tvoc":
                                drawGraphClass.feedMultiple(yMax, tvoc_float);
                                break;
                            case "co2":
                                drawGraphClass.feedMultiple(yMax, co2_float);
                                break;
                            case "co":
                                drawGraphClass.feedMultiple(yMax, co_float.intValue());
                                break;
                            case "virus":
                                drawGraphClass.feedMultiple(yMax, virusValue);
                                break;
                        }
                    }
                });
            }
        };
        chartScheduler = new Timer();
        chartScheduler.schedule(timerTask, 0, 1000 * (long) DRAW_CHART_INTERVAL);
    }

    private void GetVirusData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.listCardVIRUSIndex.setVisibility(View.VISIBLE);
                binding.listCardVIRUSOCGrade.setVisibility(View.VISIBLE);
                binding.listCardVIRUSTitle.setVisibility(View.VISIBLE);

                // 바이러스 지수 불러오기
                virusValue = Math.round(virusFormulaClass.GetVirusValue(pm_float, temp_float, humid_float, co2_float, tvoc_float));
                virusIndex = virusFormulaClass.GetVirusIndex(pm_float, temp_float, humid_float, co2_float, tvoc_float);

                try {
                    binding.listCardVIRUSIndex.setText(virusValue + "");
                    jsonMeasure.put("Virusval", virusValue);
                    jsonMeasure.put("Viruslvl", Integer.parseInt(virusIndex));

                    HideLoading();

                    // 바이러스 지수에 따라 데이터 표시하기
                    switch (virusIndex) {
                        case "0":
                            CardItemTextColor("0", binding.listCardVIRUSTitle, null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "1":
                            CardItemTextColor("1", binding.listCardVIRUSTitle, null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "2":
                            CardItemTextColor("2", binding.listCardVIRUSTitle, null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "3":
                            CardItemTextColor("3", binding.listCardVIRUSTitle, null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        default:
                            CardItemTextColor("4", binding.listCardVIRUSTitle, null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                    }
                } catch (NullPointerException | JSONException e) {
                    e.printStackTrace();
                    binding.listCardVIRUSOCGrade.setVisibility(View.GONE);
                    binding.listCardVIRUSIndex.setVisibility(View.GONE);
                    binding.listCardVIRUSTitle.setVisibility(View.GONE);
                    binding.listCardVIRUSLoadingTv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void GetCQIData() throws JSONException {
        int cqiIndexValue = virusFormulaClass.GetCQIValue(pm_float, co_float);
        cqiIndex = cqiIndexValue;
        moveBarChart(cqiIndexValue);

        String cqiGradeValue = virusFormulaClass.GetCQIGrade(pm_float, co_float);
        binding.aqiContentTv.setText(cqiGradeValue);

        jsonMeasure.put("CAIval", cqiIndexValue);
        jsonMeasure.put("CAIlvl", Integer.parseInt(cqiGradeValue));

        //  CQI 등급을 불러오고 표시하기
        if (binding.aqiContentTv.getText().toString().equals("0")) {
            CqiGradeChange("0", binding.aqiContentTv, binding.aqiCurrentArrow);
        } else if (binding.aqiContentTv.getText().toString().equals("1")) {
            CqiGradeChange("1", binding.aqiContentTv, binding.aqiCurrentArrow);
        } else if (binding.aqiContentTv.getText().toString().equals("2")) {
            CqiGradeChange("2", binding.aqiContentTv, binding.aqiCurrentArrow);
        } else if (binding.aqiContentTv.getText().toString().equals("3")) {
            CqiGradeChange("3", binding.aqiContentTv, binding.aqiCurrentArrow);
        } else {
            CqiGradeChange("4", binding.aqiContentTv, binding.aqiCurrentArrow);
        }
    }

    //AQI Index 별 차트 이동거리를 계산합니다
    public void moveBarChart(int cqiNumber) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (cqiNumber != 0) {
            params.setMargins((cqiNumber * barViewWidth / 300) - (arrowWidth / 2) + 10,
                    0,
                    0,
                    15);  // 왼쪽, 위, 오른쪽, 아래 순서
        } else {
            params.setMargins(0,
                    0,
                    0,
                    15);  // 왼쪽, 위, 오른쪽, 아래 순서
        }

        binding.aqiCurrentArrow.setLayoutParams(params);
        binding.aqiCurrentArrow.setText(String.valueOf(cqiNumber));

        if (cqiNumber < 51) {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.good));
            binding.aqiContentTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
            binding.aqiCurrentArrow.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
        } else if (cqiNumber < 101) {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.normal));
            binding.aqiContentTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
            binding.aqiCurrentArrow.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
        } else if (cqiNumber < 251) {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.bad));
            binding.aqiContentTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
            binding.aqiCurrentArrow.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
        } else {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.cqi_baddest));
            binding.aqiContentTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
            binding.aqiCurrentArrow.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
        }
    }

    // 햄버거 메뉴 추가
    private void addSideView() {
        binding.dashboardMainLayout.setEnabled(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FAN_CONTROL_COMPLETE);
        registerReceiver(mReceiver, filter);

        Log.i(TAG_BTThread, "Add Side Menu Complete");
        sidebar = new SideBarCustomView(context);

        binding.viewSildebar.addView(sidebar);
        final TextView dialog_setupDate = sidebar.findViewById(R.id.sideMenuSetUpDateTv);
        final TextView dialog_serialNumber = sidebar.findViewById(R.id.sideMenuSerialNumTv);
        final TextView dialog_productName = sidebar.findViewById(R.id.SideMenuProductTv);
        final TextView dialog_fan1 = sidebar.findViewById(R.id.sideMenuFan1Tv);
        final TextView dialog_fan2 = sidebar.findViewById(R.id.sideMenuFan2Tv);
        final TextView dialog_fan3 = sidebar.findViewById(R.id.sideMenuFan3Tv);
        final TextView dialog_fan4 = sidebar.findViewById(R.id.sideMenuFan4Tv);
        final ImageView dialog_product_img = sidebar.findViewById(R.id.sideMenuProductIv);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (bluetoothThread.isConnected() && bluetoothThread != null) {
                    // 대쉬보드가 불러와지면서 요청한 데이터들을 화면에 표시합니다
                    dialog_setupDate.setText(setUpDateStr);
                    dialog_serialNumber.setText(serialNumber);

                    // 모델명에 따른 필터링
                    if (modelName.contains(" ")) {
                        String[] s = modelName.split(" ");
                        dialog_productName.setText(s[0]);
                    } else {
                        dialog_productName.setText(modelName);
                    }

                    if (modelName.startsWith("BS_M")) {
                        dialog_product_img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.side_m, null));
                        dialog_product_img.setAlpha(0.85f);
                    } else if (modelName.startsWith("BS_100")) {
                        dialog_product_img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.side_100, null));
                        dialog_product_img.setAlpha(0.85f);
                    } else {
                        dialog_product_img.setImageDrawable(null);
                    }

                    // 펜 제어에 따른 UI 변경
                    if (current_fan_byte == 0x01) {
                        dialog_fan1.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                        dialog_fan1.setTextColor(Color.parseColor("#5CC2E4"));
                    } else if (current_fan_byte == 0x02) {
                        dialog_fan2.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                        dialog_fan2.setTextColor(Color.parseColor("#5CC2E4"));
                    } else if (current_fan_byte == 0x03) {
                        dialog_fan3.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                        dialog_fan3.setTextColor(Color.parseColor("#5CC2E4"));
                    } else if (current_fan_byte == 0x04) {
                        dialog_fan4.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                        dialog_fan4.setTextColor(Color.parseColor("#5CC2E4"));
                    }
                }
            }
        });

        // 사이드 메뉴 이벤트 리스너
        sidebar.setEventListener(new SideBarCustomView.EventListener() {
            @Override
            public void btnCancel() {
                // 메뉴 닫기
                closeMenu();
            }

            // 전원 종료
            @Override
            public void powerOff() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
                final View view = LayoutInflater.from(context).inflate(R.layout.sidemenu_dialog, null, false);
                builder.setView(view);
                final AlertDialog alertDialog = builder.create();
                final TextView dialog_ok = view.findViewById(R.id.sidedialogOkTv);
//                final TextView dialog_msg = view.findViewById(R.id.sideDialogContextTv);
                final TextView dialog_cancel = view.findViewById(R.id.sidedialogCancelTv);

                dialog_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bluetoothThread.isConnected()) {
                            power_control_byte = 0x01;
                            bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x50, 0x00, 0x00}, bluetoothThread.getSequence()));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    outerClass.CallVibrate(context, 10);
                                    alertDialog.dismiss();
                                }
                            });
                        }
                    }
                });

                dialog_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        outerClass.CallVibrate(context, 10);
                        alertDialog.dismiss();
                    }
                });

                alertDialog.show();
            }

            // 펜 제어
            @Override
            public void fan1() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x01;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                }
            }

            @Override
            public void fan2() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x02;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                }
            }

            @Override
            public void fan3() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x03;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                }
            }

            @Override
            public void fan4() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x04;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                }
            }
        });
    }

    // 팬 제어 완료상태를 전달받기 위한 리시버
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiveContext, Intent intent) {
            String action = intent.getAction();

            if (action.equals(FAN_CONTROL_COMPLETE)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (sidebar != null) {
                            final TextView dialog_fan1 = sidebar.findViewById(R.id.sideMenuFan1Tv);
                            final TextView dialog_fan2 = sidebar.findViewById(R.id.sideMenuFan2Tv);
                            final TextView dialog_fan3 = sidebar.findViewById(R.id.sideMenuFan3Tv);
                            final TextView dialog_fan4 = sidebar.findViewById(R.id.sideMenuFan4Tv);

                            if (fan_control_byte == 0x01) {
                                current_fan_byte = 0x01;
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan1, dialog_fan2, dialog_fan3, dialog_fan4, context);
                            } else if (fan_control_byte == 0x02) {
                                current_fan_byte = 0x02;
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan2, dialog_fan1, dialog_fan3, dialog_fan4, context);
                            } else if (fan_control_byte == 0x03) {
                                current_fan_byte = 0x03;
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan3, dialog_fan2, dialog_fan1, dialog_fan4, context);
                            } else if (fan_control_byte == 0x04) {
                                current_fan_byte = 0x04;
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan4, dialog_fan2, dialog_fan3, dialog_fan1, context);
                            }
                        }
                    }
                });
            }
        }
    };

    //햄버거 메뉴 닫기
    private void closeMenu() {
        outerClass.CallVibrate(context, 10);
        isMenuShow = false;
        Animation slide = AnimationUtils.loadAnimation(context, R.anim.sidebar_hidden);
        binding.viewSildebar.startAnimation(slide);
        binding.flSilde.setVisibility(View.GONE);
        binding.flSilde.setEnabled(false);
        binding.idMain.setEnabled(true);
        binding.idMain.bringToFront();
        System.out.println("CloseMenu");
    }

    //햄버거 메뉴 보여주기
    private void showMenu() {
        outerClass.CallVibrate(context, 10);
        isMenuShow = true;

        if (bluetoothThread.isRunning()) {
            // 모델명을 불러옵니다
            modelName = bluetoothThread.getDeviceName();
            Log.i(TAG_BTThread, "setDevice by : " + bluetoothThread.getDeviceName());

            // 최초 데이터인 모듈 설치날짜와 모델명을 API 를 통해 스레드에 요청합니다
//                                센서 장착 여부 및 GPS 정보 요청
            bluetoothThread.writeHex(makeFrame(
                    new byte[]{REQUEST_INDIVIDUAL_STATE},
                    new byte[]{
                            0x46, 0x00, 0x00, // 모듈설치날짜
                            0x47, 0x00, 0x00,  // 모델명
                            0x48, 0x00, 0x00,  // S/N
                            0x3A, 0x00, 0x00  // 현재바람세기
                    },
                    bluetoothThread.getSequence()
            ));

            Animation slide = AnimationUtils.loadAnimation(context, R.anim.sidebar_show);
            binding.viewSildebar.startAnimation(slide);
            binding.flSilde.setVisibility(View.VISIBLE);
            binding.flSilde.setEnabled(true);
            binding.idMain.setEnabled(false);
            binding.flSilde.bringToFront();
            System.out.println("ShowMenu");
        }
    }

    public void onClick(View view) {
        if (binding.hambugerMenuIv.equals(view)) {//사이드 메뉴 클릭 시 이벤트
            if (!isMenuShow) {
                addSideView();
                showMenu();
            } else {
                closeMenu();
            }
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(선택)
    private void CategoryChoice(TextView tv) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothThread.isRunning()) {
                        tv.setEnabled(false);
                        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                        tv.setBackground(AppCompatResources.getDrawable(context, R.drawable.category_text_outline));

                        ClickCategory(tv, getString(R.string.aqi), 300, "cqi");
                        ClickCategory(tv, getString(R.string.fine_dust), 80, "pm");
                        ClickCategory(tv, getString(R.string.co), 10, "co");
                        ClickCategory(tv, getString(R.string.co2), co2_float.intValue() + 500, "co2");
                        ClickCategory(tv, getString(R.string.tvoc), 5, "tvoc");
                        ClickCategory(tv, getString(R.string.virus), 100, "virus");
                    }
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(미선택)
    private void CategoryNotChoice(TextView tv1, TextView tv2, TextView tv3, TextView
            tv4, TextView tv5) {
        try {
            tv1.setEnabled(true);
            tv1.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
            tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
            tv2.setEnabled(true);
            tv2.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
            tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
            tv3.setEnabled(true);
            tv3.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
            tv3.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
            tv4.setEnabled(true);
            tv4.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
            tv4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
            tv5.setEnabled(true);
            tv5.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
            tv5.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // 그래프를 그리고 바이러스, CQI 지수를 그리는 함수
    private void DrawingGraphMethod() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 그래프 그리기
                        drawGraphClass.reDrawChart();
                        drawGraphClass.drawFirstEntry(300, "cqi");
                        ChartTimerTask(300, "cqi");
                        addSideView();
                        StartMqtt();
                    }
                });
            }
        }, 1000);
    }

    // 선 그래프 설정
    private class DrawGraphClass extends Thread {
        // 참고 :  https://github.com/PhilJay/MPAndroidChart

        LineData lineData = new LineData();
        Legend legend = new Legend();
        LineDataSet lineDataSet;

        // 그래프 X축, Y축 설정 및 모양 설정
        void setChart(int setYMax) {
            // X축
            XAxis xAxis = binding.virusLineChart.getXAxis();
            xAxis.setDrawLabels(true); // 라벨 표시 여부
            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
            xAxis.setDrawAxisLine(false); // AxisLine 표시
            xAxis.setDrawGridLines(false); // GridLine 표시
            xAxis.setGranularityEnabled(false); // x축 간격을 제한하는 세분화 기능
            xAxis.setValueFormatter(new XAxisValueFormat()); // X축 라벨데이터 포멧
            xAxis.setGranularity(1);
            binding.virusLineChart.setAutoScaleMinMaxEnabled(true); // Max = Count
            xAxis.setLabelCount(6); // 라벨 갯수
            xAxis.setTextSize(12); // x축 데이터 크기

            binding.virusLineChart.moveViewToX(lineData.getEntryCount()); // 계속 X축을 데이터의 오른쪽 끝으로 옮기기
            binding.virusLineChart.setVisibleXRangeMaximum(5); // X축 최대 표현 개수
            binding.virusLineChart.setPinchZoom(false); // 확대 설정
            binding.virusLineChart.setDoubleTapToZoomEnabled(false); // 더블탭 설정
            binding.virusLineChart.getDescription().setEnabled(false); // 차트 값 설명 유효화
            binding.virusLineChart.setBackgroundColor(Color.TRANSPARENT); // 차트 배경색 설정
            binding.virusLineChart.setExtraOffsets(20f, 7f, 15f, 7f); // 차트 Padding 설정
            binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));
            binding.virusLineChart.getAxisRight().setEnabled(false); // 라인차트 오른쪽 데이터 비활성화
            binding.virusLineChart.setClickable(false); // 클릭 이벤트 차단
            binding.virusLineChart.setTouchEnabled(false); // 터치 이벤트 차단
            binding.virusLineChart.setEnabled(false);

            // Y축
            YAxis yAxis = binding.virusLineChart.getAxisLeft();
            yAxis.setAxisMaximum(setYMax); // Y축 값 최대값 설정
            yAxis.setAxisMinimum((0)); // Y축 값 최솟값 설정
            yAxis.setTextColor(Color.parseColor("#FFFFFF")); // y축 글자 색상
            yAxis.setValueFormatter(new YAxisValueFormat()); // y축 데이터 포맷
            yAxis.setGranularityEnabled(false); // y축 간격을 제한하는 세분화 기능
            yAxis.setDrawLabels(true); // Y축 라벨 위치
            yAxis.setLabelCount(0); // Y축 라벨 개수
            yAxis.setTextSize(12); // Y축 라벨 텍스트 사이즈
            yAxis.setDrawGridLines(false); // GridLine 표시
            yAxis.setDrawAxisLine(false); // AxisLine 표시

            legend.setEnabled(false); // 범례 비활성화

            binding.virusLineChart.setData(lineData); // 라인차트 데이터 설정
        }

        // 차트에 쓰일 목록 UI Thread 에서 가져오기
        void feedMultiple(int SetYMax, float yData) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setChart(SetYMax);
                    addEntry(yData);
                }
            });
        }

        // 엔트리 추가하기
        void addEntry(float yData) {
            // 라인 차트
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (lineData != null) {
                        lineData = binding.virusLineChart.getData();
                        createSet();
                        lineData.addDataSet(lineDataSet);
                        lineData.addEntry(new Entry(lineData.getEntryCount(), yData), 0); // 데이터 엔트리 추가
                        Log.d("GraphAdd", "Add Graph Entry " + lineData.getEntryCount() + " : " + yData);
                        lineData.notifyDataChanged(); // 데이터 변경 알림
                        binding.virusLineChart.notifyDataSetChanged(); // 라인차트 변경 알림
                    }
                }

            });
        }

        void createSet() {
            lineDataSet = new LineDataSet(null, null); // 범례, yVals 설정
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT); // Y값 데이터를 왼쪽으로
            lineDataSet.setFillColor(Color.parseColor("#147AD6")); // 차트 채우기 색상
            lineDataSet.setDrawFilled(true); // 차트 채우기 설정
            lineDataSet.setHighlightEnabled(false); // 하이라이트 설정
            lineDataSet.setLineWidth(2F); // 그래프 선 굵기
            lineDataSet.setValueTextColor(Color.WHITE);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
            lineDataSet.setDrawCircleHole(false); // 원 안에 작은 원 표시
            lineDataSet.setDrawCircles(false); // 원 표시
            lineDataSet.setDrawValues(true); // 차트포인트에 값 표시
            lineDataSet.setValueFormatter(new DataSetValueFormat());
            lineDataSet.setValueTextSize(11); // 차트 텍스트 크기
            lineDataSet.setColor(ResourcesCompat.getColor(getResources(), R.color.lineChartLine, null)); // 색상 지정
        }

        //Y축 엔트리 포멧
        public class YAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                String newValue = value + "";
                if (newValue.contains("\\.")) {
                    String[] s = newValue.split("\\.");
                    return s[0];
                } else {
                    return newValue.substring(0, newValue.length() - 2);
                }
            }
        }

        //X축 엔트리 포멧
        public class XAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                return chartTimeDivider(xLabelList, (int) value);
            }
        }

        public class DataSetValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "";
            }
        }

        // 그래프를 초기화 하고 새로 그릴 준비를 합니다
        void reDrawChart() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (chartScheduler != null) {
                        chartScheduler.cancel();
                        chartScheduler.purge();
                    }
                    lineData.clearValues();
                    lineData.notifyDataChanged();
                    binding.virusLineChart.clear();
                    binding.virusLineChart.removeAllViews();
                    binding.virusLineChart.notifyDataSetChanged();
                }
            });
        }

        // 공기질 수치에 따라 첫번째 엔트리를 미리 그립니다
        void drawFirstEntry(int setYMax, String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (s) {
                        case "cqi":
                            drawGraphClass.feedMultiple(setYMax, cqiIndex);
                            break;
                        case "pm":
                            drawGraphClass.feedMultiple(setYMax, pm_float.intValue());
                            break;
                        case "tvoc":
                            drawGraphClass.feedMultiple(setYMax, tvoc_float);
                            break;
                        case "co2":
                            drawGraphClass.feedMultiple(setYMax, co2_float);
                            break;
                        case "co":
                            drawGraphClass.feedMultiple(setYMax, co_float.intValue());
                            break;
                        case "virus":
                            drawGraphClass.feedMultiple(setYMax, virusValue);
                            break;
                    }
                }
            });
        }
    }

    // 현재 시간기준으로 그래프의 X축 라벨을 포맷합니다
    private String chartTimeDivider(ArrayList<String> arrayList, int mCount) {
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm");
            CHART_MADE_TIME = System.currentTimeMillis();
            long lArray;
            if (mCount == 0) {
                lArray = CHART_MADE_TIME - (10 * 60 * 1000);
                arrayList.add(mCount + 1, simpleDateFormat.format(lArray));
                return arrayList.get(mCount + 1);
            } else if (mCount == 1) {
                lArray = CHART_MADE_TIME;
                arrayList.add(mCount + 1, simpleDateFormat.format(lArray));
                return arrayList.get(mCount + 1);
            } else {
                lArray = CHART_MADE_TIME + ((long) (mCount - 1) * 10 * 60 * 1000);
                arrayList.add(mCount + 1, simpleDateFormat.format(lArray));
                return arrayList.get(mCount + 1);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            Log.e(TAG_BTThread, "IndexOutOfBoundsException : " + e);
        }
        return " ";
    }


    // CQI 등급 변경
    private void CqiGradeChange(String s1, TextView cqi, TextView arrow) {
        cqi.setVisibility(View.VISIBLE);
        arrow.setVisibility(View.VISIBLE);

        switch (s1) {
            case "0":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                cqi.setText(getString(R.string.good));
                break;
            case "1":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                cqi.setText(getString(R.string.normal));
                break;
            case "2":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                cqi.setText(getString(R.string.bad));
                break;
            case "3":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                cqi.setText(getString(R.string.cqi_baddest));
                break;
            case "4":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                cqi.setText(getString(R.string.error));
        }
    }

    //인터넷 연결 상태 체크
    private void isWifiLinked() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        manager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // 네트워크를 사용할 준비가 되었을 때
                Log.e("MqttLog", "The default network is now: " + network);
                binding.dashWifiIcon.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.white, null));
                binding.dashWifiSwitch.setChecked(true);
                isConnected = true;
                mqtt = new Mqtt(context, userCode);
            }

            @Override
            public void onLost(@NonNull Network network) {
                // 네트워크가 끊겼을 때
                Log.e("MqttLog", "The application no longer has a default network. The last default network was " + network);
                binding.dashWifiIcon.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                isConnected = false;
                checkWifiState();
            }
        });
    }

    //Mqtt 통신 설정 스위치 상태 불러오기
    private void SetSwitchState() {
        if (SharedPreferenceManager.getString(context, "usingWifi").equals("true")) {
            binding.dashWifiSwitch.setChecked(true);
            binding.dashWifiSwitch.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
        } else if (SharedPreferenceManager.getString(context, "usingWifi").equals("false")) {
            binding.dashWifiSwitch.setChecked(false);
            binding.dashWifiSwitch.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        }
    }

    private void switchCheckedChange() {
        binding.dashWifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                outerClass.CallVibrate(context, 10);
                if (isChecked) {
                    if (mqtt != null && bluetoothThread != null && bluetoothThread.isConnected()) {
                        if (!mqtt.isConnected())
                            mqtt.connect();
                    }
                    Log.d(TAG_MQTT, "Accept Request Data");
                    SharedPreferenceManager.setString(context, "usingWifi", "true");
                    buttonView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                    Snackbar.make(binding.idMain, R.string.mqtt_accept, Snackbar.LENGTH_SHORT).show();
                } else {
                    if (mqtt != null && bluetoothThread != null && bluetoothThread.isConnected()) {
                        if (mqtt.isConnected())
                            mqtt.disconnect();
                    }
                    Log.d(TAG_MQTT, "Deny Request Data");
                    SharedPreferenceManager.setString(context, "usingWifi", "false");
                    buttonView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                    Snackbar.make(binding.idMain, R.string.mqtt_deny, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 모듈 데이터 퍼블리시
    private void publishMeasureChk() {
        mqtt.publishMeasureChk(jsonSensor);
        Log.i(TAG_MQTT, "getMeasure success lately: " + jsonSensor);
    }

    // 센서 측정 데이터 퍼블리시
    private void publishMeasured() {
        mqtt.publish_measured(jsonMeasure);
        Log.i(TAG_MQTT, "Success to publish : " + mqttCount);
    }

    //와이파이 연결 여부 확인
    private void checkWifiState() {
        if (!isConnected) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setTitle(R.string.caution_title);
                    alertDialog.setMessage(getString(R.string.mqtt_caution_msg));
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.mqtt_connect_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!wifiManager.isWifiEnabled())
                                wifiManager.setWifiEnabled(true);
                            isConnected = true;
                            alertDialog.dismiss();
                        }
                    });
                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.perm_deny), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                            //인터넷 연결 사용 해제
                            if (wifiManager.isWifiEnabled())
                                wifiManager.setWifiEnabled(false);
                            isConnected = false;
                            binding.dashWifiSwitch.setChecked(false);
                            SharedPreferenceManager.setString(context, "usingWifi", "false");
                        }
                    });
                    alertDialog.show();
                }
            });
        }
    }

    // 배터리 잔량 및 충전여부를 암시적 인텐트로 브로드캐스팅
    private void GetBatteryState() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            PowerConnectionReceiver receiver = new PowerConnectionReceiver();
            Intent batteryStatus = context.registerReceiver(receiver, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level * 100 / (float) scale;
            final int i = (int) batteryPct;
            if (i > -1) {
                Log.d("Battery", "Get Battery Status : " + batteryPct);
                final String s = i + "%";
                binding.dashBatteryTx.setText(s);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // 충전케이블 연결 여부를 불러 와 잔량을 표시하는 뷰를 업데이트
    public class PowerConnectionReceiver extends BroadcastReceiver {
        @SuppressLint("UnsafeProtectedBroadcastReceiver")
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
            if (batteryPct != -1) {
                if (isCharging) {
                    Log.i("Battery", "Charging");
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_charging,
                            null));
                } else if (batteryPct == 100) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_full,
                            null));
                } else if (batteryPct < 100 && batteryPct > 84.5) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_6,
                            null));
                } else if (batteryPct < 84.5 && batteryPct > 67.5) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_5,
                            null));
                } else if (batteryPct < 67.5 && batteryPct > 50.5) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_4,
                            null));
                } else if (batteryPct < 50.5 && batteryPct > 33.5) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_3,
                            null));
                } else if (batteryPct < 33.5 && batteryPct > 16.5) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_2,
                            null));
                } else if (batteryPct < 16.5 && batteryPct > 0) {
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_1,
                            null));
                } else {
                    Log.i("Battery", "DisCharging");
                    binding.dashBatteryIv.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                            R.drawable.battery_empty,
                            null));
                }
            }
        }
    }

    private void ClickCategory(TextView tv_on, String equals, int yMax, String filter) {
        if (tv_on.getText().toString().equals(equals)) {
            binding.topFrameLayout.setEnabled(false);
            drawGraphClass.reDrawChart();
            try {
                binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));
                Thread.sleep(1000);
                drawGraphClass.drawFirstEntry(yMax, filter);
                ChartTimerTask(yMax, filter);
                binding.topFrameLayout.setEnabled(true);
            } catch (IndexOutOfBoundsException | InterruptedException e) {
                Log.e(TAG_BTThread, "Error is : " + e);
                binding.virusLineChart.setNoDataText(getString(R.string.no_data_error));
            }
        }
    }

    // CQI 바 차트 그리기
    private void CreateSegmentProgressView() {
        ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

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

    private void StartMqtt() {
        Log.d(TAG_MQTT, "user_id : " + userCode);
        try {
            if (mqttCount == 0) {
                if (userCode != null) {
                    mqtt = new Mqtt(context, userCode);
                    mqtt.connect();
                    Log.d(TAG_MQTT, "Mqtt is Connected");
                } else {
                    Toast.makeText(context, getString(R.string.mqtt_error_get_user_id), Toast.LENGTH_SHORT).show();
                    CompletableFuture.runAsync(() -> {
                        SharedPreferenceManager.setString(context, "usingWifi", "false");
                    }).thenAcceptAsync(b -> {
                        SetSwitchState();
                    });
                }
            } else {
                Log.d(TAG_MQTT, "Mqtt is Already Connected");
            }
        } catch (Exception e) {
            Log.e(TAG_MQTT, "Connect Error : " + e);
        }
    }

    // 하단 아이템 텍스트, 컬러 설정
    private void CardItemTextColor(String titleStr, TextView title, TextView unit, TextView grade, TextView index, TextView loading) {
        if (unit != null)
            unit.setVisibility(View.VISIBLE);
        grade.setVisibility(View.VISIBLE);
        index.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);

        if (loading.getVisibility() == View.VISIBLE)
            loading.setVisibility(View.GONE);

        switch (titleStr) {
            case "0":
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                grade.setText(getString(R.string.good));
                break;
            case "1":
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                grade.setText(getString(R.string.normal));
                break;
            case "2":
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                grade.setText(getString(R.string.bad));
                break;
            case "3":
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                grade.setText(getString(R.string.baddest));
                break;
            case "4":
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                grade.setText(getString(R.string.error));
        }
    }

    private void SetFinalLocale() {
        if (SharedPreferenceManager.getString(context, "final").equals("en")) {
            Log.i(TAG_BTThread, "Language is ENGLISH");
            outerClass.setLocaleToEnglish(context);
        } else if (SharedPreferenceManager.getString(context, "final").equals("ko")) {
            Log.i(TAG_BTThread, "Language is KOREAN");
            outerClass.setLocaleToKorea(context);
        } else {
            Log.i(TAG_BTThread, "Language is DEFAULT");
            outerClass.setLocaleToKorea(context);
        }
    }

    private void HideLoading() {
        binding.loadingPb.setVisibility(View.GONE);
        binding.idMain.setEnabled(true);
        binding.idMain.setAlpha(1f);
    }

    private void StartLoading() {
        binding.loadingPb.setVisibility(View.VISIBLE);
        binding.idMain.setEnabled(false);
        binding.idMain.setAlpha(0.3f);
    }

    private void GetBarChartDimens() {
        barViewWidth = binding.aqiBarChartPb.getWidth();
        barViewHeight = binding.aqiBarChartPb.getHeight();
        arrowWidth = binding.aqiCurrentArrow.getWidth();
    }

    //  앱 종료 메시지 창 띄우기
    @Override
    public void onBackPressed() {
        if (context.getApplicationContext() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            AlertDialog alertDialog = builder.create();
            alertDialog.setTitle(getString(R.string.exit_app_title));
            alertDialog.setMessage(getString(R.string.exit_app_message));
            alertDialog.setIcon(R.drawable.icon);
            alertDialog.setCancelable(true);
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_title), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                    DashBoardActivity.super.onBackPressed();
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_conn), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                    onDestroy();
                    Intent intent = new Intent(DashBoardActivity.this, LanguageSelectActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            alertDialog.show();
        }
    }
}