/**
 * 에어시그널 태블릿 대쉬보드 (사용자용)
 * 개발자 LeeJaeYoung (jy5953@airsignal.kr)
 * 개발시작 2022-06-20
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

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

    String FAN_CONTROL_COMPLETE = "com.example.dashboard";

    private final String TAG_BTThread = "BTThread";
    private final String TAG_LifeCycle = "DashBoardLifeCycle";
    private final byte REQUEST_CONTROL = (byte) 0x02;
    private final byte REQUEST_INDIVIDUAL_STATE = (byte) 0x01;
    int count = 0;

    int VIEW_REQUEST_INTERVAL = 10, DRAW_CHART_INTERVAL = 60 * 10;

    ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private Boolean isMenuShow = false;

    DisplayMetrics dm = new DisplayMetrics();

    BluetoothAdapter bluetoothAdapter;
    BluetoothThread bluetoothThread;
    ArrayList<BluetoothDevice> arrayListDevice;
    BluetoothThread.DataShareViewModel viewModel;
    Set<BluetoothDevice> paredDevice;

    Observer<String> data;
    int setup_date;
    String serialNumber, deviceType, modelName, setUpDateStr;

    long CHART_MADE_TIME = 0;

    Timer dataScheduler, chartScheduler;
    TimerTask data_timerTask;

    String temp_str, humid_str, pm_str, co_str, co2_str, tvoc_str;
    String pm_grade, co_grade, co2_grade, tvoc_grade, virusIndex;
    Short pm_aqi_short;
    int virusValue, barViewWidth, barViewHeight, arrowWidth, cqiIndex = 0;
    Float pm_float, co_float, co2_float, tvoc_float, humid_float, temp_float;
    byte fan_control_byte, current_fan_byte, power_control_byte;

    SideBarCustomView sidebar;

    OuterClass outerClass = new OuterClass();
    DrawGraphClass drawGraphClass = new DrawGraphClass();
    VirusFormulaClass virusFormulaClass = new VirusFormulaClass();
    ArrayList<String> xLabelList = new ArrayList<>();

    @Override
    protected void onDestroy() {
        Log.d(TAG_BTThread, "onDestroy");
        super.onDestroy();

        if (data_timerTask != null)
            data_timerTask.cancel();

        if (dataScheduler != null)
            dataScheduler.cancel();

        if (!viewModel.getReceiveData().hasActiveObservers())
            viewModel.getReceiveData().removeObservers(DashBoardActivity.this);

        if (bluetoothThread.isRunning())
            bluetoothThread.interrupt();

        if (bluetoothThread.isConnected())
            bluetoothThread.closeSocket();

        drawGraphClass.reDrawChart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG_BTThread, "onResume");
        super.onResume();
        outerClass.FullScreenMode(context);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG_BTThread, "onCreate");
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(DashBoardActivity.this, R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(DashBoardActivity.this).get(BluetoothThread.DataShareViewModel.class);

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
        Log.d(TAG_BTThread, "onWindowFocusChanged");
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
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);
        StartLoading();
        binding.listCardVIRUSIndex.setVisibility(View.GONE);
        binding.listCardVIRUSOCGrade.setVisibility(View.GONE);
        binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));

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

        // 기기의 해상도를 구합니다
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        // 데이터 관리
        data = new Observer<>() {
            @RequiresApi(api = Build.VERSION_CODES.S)
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
                    } else {
                        processRequestBody(body);
                    }
                } catch (ArrayIndexOutOfBoundsException | InterruptedException e) {
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
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    pairedDeviceConnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //븥루투스가 꺼져있음
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
    private void processRequestBody(Bundle body) throws InterruptedException {

        if (body.containsKey("47")) {
            CompletableFuture.runAsync(() -> {
                deviceType = Arrays.toString(body.getCharArray("47"));

                Log.d(TAG_BTThread, "Device Type is " + deviceType);

                System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));
            }).thenAcceptAsync(b -> {
                // DeviceFragment.DEVICE_TYPE_MINI
                if (deviceType.equals("[T, I]")) {

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
                                    0x0A, 0x00, 0x00 // PM 미세먼지 등급
                            },
                            bluetoothThread.getSequence()
                    ));
                }
            });
        }

        // S/N
        if (body.containsKey("48")) {
            serialNumber = new String(body.getCharArray("48"));
            Log.d(TAG_BTThread, "Serial Number is " + serialNumber);
        }

        // 설치날짜
        if (body.containsKey("46")) {
            setup_date = body.getInt("46");
            setUpDateStr = setup_date + "";
            Log.d(TAG_BTThread, "SetUp Date is " + setup_date + "");
        }

        // 현재 풍량 정보
        if (body.containsKey("3A")) {
            current_fan_byte = body.getByte("3A");
            Log.d(TAG_BTThread, "Current Fan is " + current_fan_byte);
        }

        // 온도
        if (body.containsKey("10")) {
            temp_str = body.getString("10").substring(0, 4);
            temp_float = Float.parseFloat(temp_str);
            // 온도는 영하 20도 부터 영상 50도 까지만 변화를 적용합니다
            if (Float.parseFloat(temp_str) > -20f && Float.parseFloat(temp_str) < 50f) {
                binding.tempTv.setText(temp_str);
            }
        }

        // 습도
        if (body.containsKey("12")) {
            humid_str = body.getString("12").substring(0, 4);
            humid_float = Float.parseFloat(humid_str);
            // 습도는 0% 부터 100% 까지만 변화를 적용합니다
            if (Float.parseFloat(humid_str) >= 0f && Float.parseFloat(humid_str) <= 100f) {
                binding.humidTv.setText(humid_str);
            }
        }

        // 미세먼지 2.5 지수
        if (body.containsKey("09")) {
            pm_str = body.getString("09");
            pm_float = Float.parseFloat(pm_str);
            int i = Math.round(pm_float);
            // 0 ~ 150
            if (i >= 0 && i <= 100) {
                binding.listCardPMIndex.setText(i + "");
            }
        }

        // 휘발성 유기화합물
        if (body.containsKey("21")) {
            tvoc_str = body.getString("21");
            tvoc_float = Float.parseFloat(tvoc_str);
            // 0 ~ 5
            if (tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCIndex.setText(tvoc_str);
            }
        }

        // 일산화탄소
        if (body.containsKey("1B")) {
            co_str = body.getString("1B");
            co_float = Float.parseFloat(co_str);
            // 0 ~ 18
            if (co_float >= 0f && co_float <= 15f) {
                binding.listCardCOIndex.setText(co_str);
            }
        }

        // 이산화탄소
        if (body.containsKey("1E")) {
            co2_str = body.getString("1E");
            co2_float = Float.parseFloat(co2_str);
            // 소수점 첫번째 자리에서 반올림
            int i = Math.round(co2_float);
            // 0 ~ 2500
            if (i >= 0 && i <= 2000) {
                binding.listCardCO2Index.setText(i + "");
            }
        }

        // 미세먼지 등급
        if (body.containsKey("0A")) {
            pm_grade = body.getByte("0A") + "";
            // 0 ~ 150
            if (pm_float != null && pm_float >= 0f && pm_float <= 100f) {
                binding.listCardPMGrade.setText(pm_grade);
                CardItemTextColor(pm_grade, binding.listCardPMUnit, binding.listCardPMGrade, binding.listCardPMIndex, binding.listCardPMLoadingTv);
            }
        }

        // 휘발성 유기화합물 등급
        if (body.containsKey("22")) {
            tvoc_grade = body.getByte("22") + "";
            // 0 ~ 5
            if (tvoc_float != null && tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCGrade.setText(tvoc_grade);
                CardItemTextColor(tvoc_grade, binding.listCardTVOCUnit, binding.listCardTVOCGrade, binding.listCardTVOCIndex, binding.listCardTVOCLoadingTv);
            }
        }

        // 일산화탄소 등급
        if (body.containsKey("1C")) {
            co_grade = body.getByte("1C") + "";
            // 0 ~ 18
            if (co_float != null && co_float >= 0f && co_float <= 15f) {
                binding.listCardCOGrade.setText(outerClass.translateData(co_grade, context));
                CardItemTextColor(co_grade, binding.listCardCOUnit, binding.listCardCOGrade, binding.listCardCOIndex, binding.listCardCOLoadingTv);
            }
        }

        // 이산화탄소 등급
        if (body.containsKey("1F")) {
            co2_grade = body.getByte("1F") + "";
            // 0 ~ 2500
            if (co2_float != null && Math.round(co2_float) >= 0 && Math.round(co2_float) <= 2000) {
                binding.listCardCO2Grade.setText(outerClass.translateData(co2_grade, context));
                CardItemTextColor(co2_grade, binding.listCardCO2Unit, binding.listCardCO2Grade, binding.listCardCO2Index, binding.listCardCO2LoadingTv);
            }
        }

        // PM 2.5 AQI 지수
        if (body.containsKey("0B")) {
            pm_aqi_short = body.getShort("0B");
        }

        if (pm_float != null && co_float != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CompletableFuture.runAsync(() -> {
                        GetCQIData();
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
                        // 어플 강제종료
//                        android.os.Process.killProcess(android.os.Process.myPid());
                        Toast.makeText(context, getString(R.string.exit_complete), Toast.LENGTH_SHORT).show();
                        // 어플 재시작
                        finishAffinity();
                        Intent intent = new Intent(DashBoardActivity.this, LanguageSelectActivity.class);
                        startActivity(intent);
                        System.exit(0);
                    }
                });
            }
        }
        // 펜의 풍량 제어
        else if (body.containsKey("3B")) {
            if (fan_control_byte != 0x00) {
                if (fan_control_byte == 0x01) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x01}), bluetoothThread.getSequence()));
                    Log.d(TAG_BTThread, "Fan 수면 단계");
                } else if (fan_control_byte == 0x02) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x02}), bluetoothThread.getSequence()));
                    Log.d(TAG_BTThread, "Fan 약 단계");
                } else if (fan_control_byte == 0x03) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x03}), bluetoothThread.getSequence()));
                    Log.d(TAG_BTThread, "Fan 강 단계");
                } else if (fan_control_byte == 0x04) {
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, generateTag((byte) 0x3A, new byte[]{0x04}), bluetoothThread.getSequence()));
                    Log.d(TAG_BTThread, "Fan 터보 단계");
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
                    Log.d(TAG_BTThread, "Bluetooth Socket is Connected");
                    Log.d(TAG_BTThread, "setDevice by : " + bluetoothThread.getDeviceName());

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
                }
            });

            // 소켓연결에 실패하였을 경우 발생하는 이벤트 리스너입니다
            bluetoothThread.setDisconnectedSocketEventListener(new BluetoothThread.disConnectedSocketEventListener() {
                @Override
                public void onDisconnectedEvent() {
                    binding.dashboardMainLayout.setEnabled(false);
                    Log.d(TAG_BTThread, "Bluetooth Socket is Disconnected");
                    Log.d(TAG_BTThread, "Running : " + bluetoothThread.isRunning());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (bluetoothThread.isRunning()) {
                                bluetoothThread.connectSocket();
                            } else {
                                outerClass.GoToConnectFromDashboard(context);
                            }
                            HideLoading();
                        }
                    });
                }
            });

            // 소켓을 연결합니다
            bluetoothThread.connectSocket();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (bluetoothThread.isConnected()) {
                bluetoothThread.start();
                Log.d(TAG_BTThread, "BluetoothThread is Run");

                try {
                    regDataListener(dataScheduler);
                    Log.d(TAG_BTThread, "Loop Data Request");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
                    } catch (NullPointerException | IllegalStateException e) {
                        e.printStackTrace();
                        outerClass.GoToConnectFromLang(context);
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

                // 바이러스 지수 불러오기
                virusValue = Math.round(virusFormulaClass.GetVirusValue(pm_float, temp_float, humid_float, co2_float, tvoc_float));
                virusIndex = virusFormulaClass.GetVirusIndex(pm_float, temp_float, humid_float, co2_float, tvoc_float);

                try {
                    binding.listCardVIRUSIndex.setText(virusValue + "");

                    HideLoading();

                    // 바이러스 지수에 따라 데이터 표시하기
                    switch (virusIndex) {
                        case "0":
                            CardItemTextColor("0", null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "1":
                            CardItemTextColor("1", null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "2":
                            CardItemTextColor("2", null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        case "3":
                            CardItemTextColor("3", null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                        default:
                            CardItemTextColor("4", null, binding.listCardVIRUSOCGrade, binding.listCardVIRUSIndex, binding.listCardVIRUSLoadingTv);
                            break;
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    binding.listCardVIRUSOCGrade.setVisibility(View.GONE);
                    binding.listCardVIRUSIndex.setVisibility(View.GONE);
                    binding.listCardVIRUSLoadingTv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void GetCQIData() {
        int cqiIndexValue = virusFormulaClass.GetCQIValue(pm_float, co_float);
        cqiIndex = cqiIndexValue;
        moveBarChart(cqiIndexValue);

        String cqiGradeValue = virusFormulaClass.GetCQIGrade(pm_float, co_float);
        binding.aqiContentTv.setText(cqiGradeValue);

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
        binding.aqiCurrentArrow.setText(cqiNumber + "");

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
        IntentFilter filter = new IntentFilter();
        filter.addAction(FAN_CONTROL_COMPLETE);
        registerReceiver(mReceiver, filter);

        Log.d(TAG_BTThread, "Add Side Menu Complete");
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
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan1, dialog_fan2, dialog_fan3, dialog_fan4, context);
                            } else if (fan_control_byte == 0x02) {
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan2, dialog_fan1, dialog_fan3, dialog_fan4, context);
                            } else if (fan_control_byte == 0x03) {
                                outerClass.CallVibrate(context, 10);
                                outerClass.fanBackgroundChange(dialog_fan3, dialog_fan2, dialog_fan1, dialog_fan4, context);
                            } else if (fan_control_byte == 0x04) {
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
    }

    //햄버거 메뉴 보여주기
    private void showMenu() {
        outerClass.CallVibrate(context, 10);
        isMenuShow = true;

        Animation slide = AnimationUtils.loadAnimation(context, R.anim.sidebar_show);
        binding.viewSildebar.startAnimation(slide);
        binding.flSilde.setVisibility(View.VISIBLE);
        binding.flSilde.setEnabled(true);
        binding.idMain.setEnabled(false);
        binding.flSilde.bringToFront();
    }

    public void onClick(View view) {
        if (binding.hambugerMenuIv.equals(view)) {//사이드 메뉴 클릭 시 이벤트
            if (!isMenuShow) {
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
                    if (bluetoothThread.isConnected()) {
                        tv.setEnabled(false);
                        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                        tv.setBackground(AppCompatResources.getDrawable(context, R.drawable.category_text_outline));
                        xLabelList.clear();
                        CHART_MADE_TIME = System.currentTimeMillis();

                        ClickCategory(tv, getString(R.string.aqi), 300, "cqi");
                        ClickCategory(tv, getString(R.string.fine_dust), 80, "pm");
                        ClickCategory(tv, getString(R.string.co), 10, "co");
                        ClickCategory(tv, getString(R.string.co2), co2_float.intValue() + 500, "co2");
                        ClickCategory(tv, getString(R.string.tvoc), 5, "tvoc");
                        ClickCategory(tv, getString(R.string.virus), 100, "virus");

                    } else {
                        outerClass.GoToConnectFromDashboard(context);
                    }
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
            outerClass.GoToConnectFromDashboard(context);
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
            binding.virusLineChart.setExtraOffsets(5f, 5f, 5f, 5f); // 차트 Padding 설정
            binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));
            binding.virusLineChart.getAxisRight().setEnabled(false); // 라인차트 오른쪽 데이터 비활성화

            // Y축
            YAxis yAxis = binding.virusLineChart.getAxisLeft();
            yAxis.setAxisMaximum(setYMax); // Y축 값 최대값 설정
            yAxis.setAxisMinimum((0)); // Y축 값 최솟값 설정
            yAxis.setTextColor(Color.parseColor("#FFFFFF")); // y축 글자 색상
            yAxis.setValueFormatter(new YAxisValueFormat()); // y축 데이터 포맷
            yAxis.setGranularityEnabled(false); // y축 간격을 제한하는 세분화 기능
            yAxis.setDrawLabels(true); // Y축 라벨 위치
            yAxis.setLabelCount(2); // Y축 라벨 개수
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

        // 현재 시간기준으로 그래프의 X축 라벨을 포맷합니다
        private String chartTimeDivider(ArrayList<String> arrayList, int count) {
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm");
                long lArray;
                if (count == 0) {
                    lArray = CHART_MADE_TIME - (10 * 60 * 1000);
                    arrayList.add(count + 1, simpleDateFormat.format(lArray));
                    return arrayList.get(count + 1);
                } else if (count == 1) {
                    lArray = CHART_MADE_TIME;
                    arrayList.add(count + 1, simpleDateFormat.format(lArray));
                    return arrayList.get(count + 1);
                } else {
                    lArray = CHART_MADE_TIME + ((long) (count - 1) * 10 * 60 * 1000);
                    arrayList.add(count + 1, simpleDateFormat.format(lArray));
                    return arrayList.get(count + 1);
                }
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                Toast.makeText(context, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                outerClass.GoToConnectFromDashboard(context);
                Log.e(TAG_BTThread, "IndexOutOfBoundsException : " + e);
            }
            return null;
        }
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

    private void ClickCategory(TextView tv_on, String equals, int yMax, String filter) {
        if (tv_on.getText().toString().equals(equals)) {
            binding.topFrameLayout.setEnabled(false);
            drawGraphClass.reDrawChart();
            try {
                count = 0;
                binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));
                drawGraphClass.drawFirstEntry(yMax, filter);
                ChartTimerTask(yMax, filter);
                binding.topFrameLayout.setEnabled(true);
            } catch (IndexOutOfBoundsException e) {
                count = 1;
                Log.e(TAG_BTThread, "Error is : " + e);
                binding.virusLineChart.setNoDataText(getString(R.string.no_data_error));
            }
        }
    }

    // CQI 바 차트 그리기
    private void CreateSegmentProgressView() {

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

    // 하단 아이템 텍스트, 컬러 설정
    private void CardItemTextColor(String title, TextView unit, TextView grade, TextView index, TextView loading) {
        if (unit != null)
            unit.setVisibility(View.VISIBLE);
        grade.setVisibility(View.VISIBLE);
        index.setVisibility(View.VISIBLE);

        if (loading.getVisibility() == View.VISIBLE)
            loading.setVisibility(View.GONE);

        switch (title) {
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
            Log.d(TAG_BTThread, "Language is ENGLISH");
            outerClass.setLocaleToEnglish(context);
        } else if (SharedPreferenceManager.getString(context, "final").equals("ko")) {
            Log.d(TAG_BTThread, "Language is KOREAN");
            outerClass.setLocaleToKorea(context);
        } else {
            Log.d(TAG_BTThread, "Language is DEFAULT");
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.exit_app_title));
        builder.setMessage(getString(R.string.exit_app_message));
        builder.setPositiveButton(getString(R.string.exit_app_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (bluetoothThread.isConnected()) {
                    bluetoothThread.closeSocket();
                }

                if (bluetoothThread.isInterrupted()) {
                    bluetoothThread.interrupt();
                }
                dialog.dismiss();
                DashBoardActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton(getString(R.string.exit_app_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (!bluetoothThread.isInterrupted()) {
                        bluetoothThread.start();
                    }

                    if (!bluetoothThread.isConnected()) {
                        bluetoothThread.connectSocket();
                    }
                } catch (IllegalThreadStateException e) {
                    e.printStackTrace();
                }

                dialog.dismiss();
            }
        });
        builder.show();
    }
}