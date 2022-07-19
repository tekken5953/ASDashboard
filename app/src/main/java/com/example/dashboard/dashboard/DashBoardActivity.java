package com.example.dashboard.dashboard;

import static com.example.dashboard.bluetooth.BluetoothAPI.REQUEST_CONTROL;
import static com.example.dashboard.bluetooth.BluetoothAPI.REQUEST_INDIVIDUAL_STATE;
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
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

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
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class DashBoardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;

    Activity context = DashBoardActivity.this;

    String FAN_CONTROL_COMPLETE = "com.example.dashboard";
    static final String TAG_BTThread = "BTThread";

    int barViewWidth, barViewHeight, arrowWidth, VIEW_REQUEST_INTERVAL = 3, DRAW_CHART_INTERVAL = 60 * 10, cqiIndex;

    ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private Boolean isMenuShow = false;

    Configuration configuration;

    BluetoothAdapter bluetoothAdapter;
    BluetoothThread bluetoothThread;
    ArrayList<BluetoothDevice> arrayListDevice;
    BluetoothThread.DataShareViewModel viewModel;
    Set<BluetoothDevice> paredDevice;

    DisplayMetrics dm = new DisplayMetrics();

    Observer<String> data;
    int setup_date;
    String serialNumber = null, currentTimeIndex, deviceType, modelName, setUpDateStr;

    long CHART_MADE_TIME = 0;

    Timer dataScheduler, chartScheduler, virusScheduler;
    TimerTask data_timerTask;

    String temp_str = null, humid_str = null, pm_str = null, co_str = null, co2_str = null, tvoc_str = null;
    String pm_grade = null, co_grade = null, co2_grade = null, tvoc_grade = null, virusIndex = null, cqiGrade = null;
    Short aqi_short;
    int virusValue;
    Float pm_float, co_float, co2_float, tvoc_float, humid_float, temp_float;
    byte fan_control_byte, current_fan_byte, power_control_byte;
    ArrayList<String> xLabelList = new ArrayList<>();

    //    BroadcastReceiver mReceiver;
    SideBarCustomView sidebar;

    OuterClass outerClass = new OuterClass();
    DrawGraphClass drawGraphClass = new DrawGraphClass();
    VirusFormulaClass virusFormulaClass = new VirusFormulaClass();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothThread.isConnected()) {
            bluetoothThread.closeSocket();
        }
        if (bluetoothThread.isRunning()) {
            bluetoothThread.interrupt();
        }
        if (mReceiver.isInitialStickyBroadcast())
            unregisterReceiver(mReceiver);

        drawGraphClass.reDrawChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        outerClass.FullScreenMode(context); // 하단 바 없애기
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(DashBoardActivity.this, R.layout.activity_dashboard);
        viewModel = new ViewModelProvider(DashBoardActivity.this).get(BluetoothThread.DataShareViewModel.class);
        binding.setLifecycleOwner(this);
        binding.setViewModel(viewModel);

        init(); //뷰 바인딩
        getWindowManager().getDefaultDisplay().getMetrics(dm); // 기기 해상도를 구하기 위함

        configuration = new Configuration();

        if (SharedPreferenceManager.getString(context, "final").equals("en")) {
            Log.d(TAG_BTThread, "Language is ENGLISH");
            configuration.setLocale(Locale.US);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else if (SharedPreferenceManager.getString(context, "final").equals("ko")) {
            Log.d(TAG_BTThread, "Language is KOREAN");
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else {
            Log.d(TAG_BTThread, "Language is DEFAULT");
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }

        CreateSegmentProgressView(); // AQI 바 차트 그리기

        binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));

        binding.loadingPb.setVisibility(View.VISIBLE);
        binding.idMain.setEnabled(false);
        binding.idMain.setAlpha(0.3f);

        binding.listCardVIRUSIndex.setVisibility(View.GONE);
        binding.listCardVIRUSOCGrade.setVisibility(View.GONE);

    }

    public void init() {

        currentTimeIndex(); // 현재 시간 적용

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        binding.hambugerMenuIv.setOnClickListener(this::onClick);

        // 데이터 관리
        data = new Observer<>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
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
                                        outerClass.backToConnectDevice(context);
                                    }
                                }).setCancelable(false).show();
                    }
                });
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void processRequestBody(Bundle body) {

        if (body.containsKey("47")) {
            deviceType = Arrays.toString(body.getCharArray("47"));

            Log.d(TAG_BTThread, "Device Type is " + deviceType);

            System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));

            if (deviceType.equals("[T, I]")) { // DeviceFragment.DEVICE_TYPE_MINI
                // Wifi State 확인
                Handler ProcessRequestHandler = new Handler(Looper.getMainLooper());
                ProcessRequestHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothThread.writeHex(
                                makeFrame(
                                        new byte[]{REQUEST_INDIVIDUAL_STATE},
                                        new byte[]{
                                                0x48, 0x00, 0x00,  // S/N
//                                                0x65, 0x00, 0x00,  // WIFI Connect State
                                                0x3A, 0x00, 0x00  // 현재바람세기
                                        },
                                        bluetoothThread.getSequence()
                                )
                        );

                        GraphDataSideHandler();
                    }
                }, 1500);


            } else {
                bluetoothThread.writeHex(makeFrame(
                        new byte[]{REQUEST_INDIVIDUAL_STATE},
                        new byte[]{
//                                0x43, 0x00, 0x00, // GPS 위도
//                                0x44, 0x00, 0x00, // GPS 경도
//                                0x45, 0x00, 0x00, // 펌웨어버전
//                                0x46, 0x00, 0x00,  // 모듈설치날짜
                        },
                        bluetoothThread.getSequence()
                ));
            }
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
            Log.d(TAG_BTThread, "Serial Number is " + serialNumber);
        }

        if (body.containsKey("46")) {
            setup_date = body.getInt("46");
            setUpDateStr = setup_date + "";
            Log.d(TAG_BTThread, "SetUp Date is " + setup_date + "");
        }

        if (body.containsKey("10")) {
            temp_str = body.getString("10").substring(0, 4);
            temp_float = Float.parseFloat(temp_str);
            if (Float.parseFloat(temp_str) > -20f && Float.parseFloat(temp_str) < 50f) {
                binding.tempTv.setText(temp_str);
            }
        }

        if (body.containsKey("12")) {
            humid_str = body.getString("12").substring(0, 4);
            humid_float = Float.parseFloat(humid_str);
            if (Float.parseFloat(humid_str) >= 0f && Float.parseFloat(humid_str) <= 100f) {
                binding.humidTv.setText(humid_str);
            }
        }

        if (body.containsKey("09")) {
            pm_str = body.getString("09");
            pm_float = Float.parseFloat(pm_str);
            int i = Math.round(pm_float);
            if (i >= 0 && i <= 100) {
                binding.listCardPMIndex.setText(i + "");
            }
        }

        if (body.containsKey("21")) {
            tvoc_str = body.getString("21");
            tvoc_float = Float.parseFloat(tvoc_str);
            if (tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCIndex.setText(tvoc_str);
            }
        }

        if (body.containsKey("1B")) {
            co_str = body.getString("1B");
            co_float = Float.parseFloat(co_str);
            if (co_float >= 0f && co_float <= 15f) {
                binding.listCardCOIndex.setText(co_str);
            }
        }

        if (body.containsKey("1E")) {
            co2_str = body.getString("1E");
            co2_float = Float.parseFloat(co2_str);
            int i = Math.round(co2_float);
            if (i >= 0 && i <= 2000) {
                binding.listCardCO2Index.setText(i + "");
            }
        }

        if (body.containsKey("0A")) {
            pm_grade = body.getByte("0A") + "";
            if (pm_float != null && pm_float >= 0f && pm_float <= 100f) {
                binding.listCardPMGrade.setText(pm_grade);
                CardItemTextColor(pm_grade, binding.listCardPMUnit, binding.listCardPMGrade, binding.listCardPMIndex);
            }
        }

        if (body.containsKey("22")) {
            tvoc_grade = body.getByte("22") + "";
            if (tvoc_float != null && tvoc_float >= 0f && tvoc_float <= 3f) {
                binding.listCardTVOCGrade.setText(tvoc_grade);
                CardItemTextColor(tvoc_grade, binding.listCardTVOCUnit, binding.listCardTVOCGrade, binding.listCardTVOCIndex);
            }
        }

        if (body.containsKey("1C")) {
            co_grade = body.getByte("1C") + "";
            if (co_float != null && co_float >= 0f && co_float <= 15f) {
                binding.listCardCOGrade.setText(outerClass.translateData(co_grade, context));
                CardItemTextColor(co_grade, binding.listCardCOUnit, binding.listCardCOGrade, binding.listCardCOIndex);
            }
        }

        if (body.containsKey("1F")) {
            co2_grade = body.getByte("1F") + "";
            if (co2_float != null && co2_float >= 0f && co_float <= 2000f) {
                binding.listCardCO2Grade.setText(outerClass.translateData(co2_grade, context));
                CardItemTextColor(co2_grade, binding.listCardCO2Unit, binding.listCardCO2Grade, binding.listCardCO2Index);
            }
        }

        if (body.containsKey("0B")) {
            aqi_short = body.getShort("0B");
        }

        if (body.containsKey("3A")) {
            current_fan_byte = body.getByte("3A");
            Log.d(TAG_BTThread, "Current Fan is " + current_fan_byte);
        }
    }

    private void processControlBody(Bundle body) {
        // 임시
        if (body.containsKey("50")) {

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
        } else if (body.containsKey("3B")) {
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

//        } else if (body.containsKey("48")) {
//
//            if (body.getByte("48") == 0x01) {
//                bluetoothThread.writeHex(
//                        makeFrame(
//                                new byte[]{REQUEST_INDIVIDUAL_STATE},
//                                new byte[]{0x48, 0x00, 0x00},
//                                bluetoothThread.getSequence()
//                        )
//                );
//
//                Toast.makeText(this, "S/N 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
//
//            } else {
//
//                Toast.makeText(this, "S/N 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
//
//            }
//        } else if (body.containsKey("49")) {
//
//            if (body.getByte("49") == 0x01) {
//
//                bluetoothThread.writeHex(
//                        makeFrame(
//                                new byte[]{REQUEST_INDIVIDUAL_STATE},
//                                new byte[]{0x49, 0x00, 0x00},
//                                bluetoothThread.getSequence()
//                        )
//                );
//
//                Toast.makeText(this, "포트 설정 정보 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
//
//            } else {
//
//                Toast.makeText(this, "포트 설정 정보 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
//
//            }
//        } else if (body.containsKey("57")) {
//
//            if (body.getByte("57") == 0x01) {
//                bluetoothThread.writeHex(
//                        makeFrame(
//                                new byte[]{REQUEST_INDIVIDUAL_STATE},
//                                new byte[]{0x57, 0x00, 0x00},
//                                bluetoothThread.getSequence()
//                        )
//                );
//
//                Toast.makeText(this, "데이터 전송 간격 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
//
//            } else {
//
//                Toast.makeText(this, "데이터 전송 간격 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
//
//            }
//        } else if (body.containsKey("69")) {
//
//            if (body.getByte("69") == 0x01) {
//                bluetoothThread.writeHex(
//                        makeFrame(
//                                new byte[]{REQUEST_INDIVIDUAL_STATE},
//                                new byte[]{0x69, 0x00, 0x00},
//                                bluetoothThread.getSequence()
//                        )
//                );
//
//                Toast.makeText(this, "Server IP 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
//
//            } else {
//
//                Toast.makeText(this, "Server IP 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
//
//            }
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
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            barViewWidth = binding.aqiBarChartPb.getWidth();
            barViewHeight = binding.aqiBarChartPb.getHeight();
            arrowWidth = binding.aqiCurrentArrow.getWidth();

            outerClass.FullScreenMode(context);// 하단 바 없애기

            //barChart 가로세로 구하기
            params.setMargins(-arrowWidth / 2, 0, 0, 15);
            binding.aqiCurrentArrow.setLayoutParams(params);

            if (!bluetoothAdapter.isEnabled()) {
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
                                outerClass.backToConnectDevice(context);
                            }
                        }).setCancelable(false).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceConnect() {
        //페어링 된 디바이스
        int position = getIntent().getExtras().getInt("device_position");
        arrayListDevice = new ArrayList<>();
        bluetoothThread = new BluetoothThread(context);
        paredDevice = bluetoothAdapter.getBondedDevices();
        if (!paredDevice.isEmpty()) {
            arrayListDevice.addAll(paredDevice);
            bluetoothThread.setBluetoothDevice(arrayListDevice.get(position));
            bluetoothThread.setConnectedSocketEventListener(new BluetoothThread.connectedSocketEventListener() {
                @Override
                public void onConnectedEvent() {
                    modelName = bluetoothThread.getDeviceName();
                    Log.d(TAG_BTThread, "Bluetooth Socket is Connected");
                    Log.d(TAG_BTThread, "setDevice by : " + bluetoothThread.getDeviceName());

                    Handler ConnectedSocketHandler = new Handler(Looper.getMainLooper());
                    ConnectedSocketHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                                센서 장착 여부 및 GPS 정보 요청
                            bluetoothThread.writeHex(makeFrame(
                                    new byte[]{REQUEST_INDIVIDUAL_STATE},
                                    new byte[]{
//                                            0x35, 0x00, 0x00, // 센서연결확인
                                            //0x43, 0x00, 0x00, // GPS 위도
                                            //0x44, 0x00, 0x00, // GPS 경도
                                            //0x45, 0x00, 0x00, // 펌웨어버전
                                            0x46, 0x00, 0x00, // 모듈설치날짜
                                            0x47, 0x00, 0x00,  // 모델명
                                    },
                                    bluetoothThread.getSequence()
                            ));
                        }
                    }, 500);
                    regDataListener(VIEW_REQUEST_INTERVAL, dataScheduler);
                }
            });

            bluetoothThread.setDisconnectedSocketEventListener(new BluetoothThread.disConnectedSocketEventListener() {
                @Override
                public void onDisconnectedEvent() {
                    Log.d(TAG_BTThread, "Bluetooth Socket is Disconnected");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.create();
                            builder.setTitle(getString(R.string.caution_title));
                            builder.setMessage(getString(R.string.caution_message));
                            builder.setPositiveButton(getString(R.string.caution_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(data_timerTask != null){
                                        data_timerTask.cancel();
                                    }

                                    dialog.dismiss();
                                    outerClass.backToConnectDevice(context);
                                    drawGraphClass.reDrawChart();
                                    bluetoothThread.closeSocket();
                                }
                            });
                            if (!context.isDestroyed())
                                builder.show();
                        }
                    });
                }
            });

            bluetoothThread.connectSocket();

            if (!bluetoothThread.isRunning()) {
                bluetoothThread.start();
                Log.d(TAG_BTThread, "BluetoothThread is Run");
            }
        }
    }

    private void regDataListener(int interval, Timer scheduler) {
        try {
            if (scheduler != null) {
                scheduler.cancel();
            }
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        loopReceiveData(interval);
    }

    private void loopReceiveData(int interval) {

        if (bluetoothThread.isConnected()) {
             data_timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        bluetoothThread.writeHex(makeFrame(new byte[]{0x03}, new byte[]{(byte) 0xFF, 0x00, 0x00}, bluetoothThread.getSequence()));
                    } catch (NullPointerException | IllegalStateException e) {
                        e.printStackTrace();
                        outerClass.backToConnectDevice(context);
                    }
                }
            };
            Timer scheduler = new Timer();
            scheduler.scheduleAtFixedRate(data_timerTask, 4000, interval * 1000L);
        }
    }

    private void regVirusListener(int interval, Timer scheduler) {
        try {
            if (scheduler != null) {
                scheduler.cancel();
            }
        } catch (NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        loopReceiveVirus(interval);
    }

    private void loopReceiveVirus(int interval) {

        if (bluetoothThread.isConnected()) {
            TimerTask data_timerTask = new TimerTask() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                binding.listCardVIRUSIndex.setVisibility(View.VISIBLE);
                                binding.listCardVIRUSOCGrade.setVisibility(View.VISIBLE);

                                // CQI 불러오기
                                cqiIndex = virusFormulaClass.GetCQIValue(aqi_short, co_float);
                                cqiGrade = virusFormulaClass.GetCQIGrade(aqi_short, co_float);
                                binding.aqiContentTv.setText(cqiGrade);

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

                                moveBarChart(cqiIndex);

                                Log.d("CqiValue", "\n" + "\npm 2.5 aqi : " + aqi_short + "\nco : " + co_float + "\nCQI Index : " + cqiIndex +
                                        "\nCQI Grade : " + cqiGrade);

                                // 바이러스 지수 불러오기
                                virusValue = Math.round(virusFormulaClass.GetVirusValue((float) aqi_short, temp_float, humid_float, co2_float, tvoc_float));
                                virusIndex = virusFormulaClass.GetVirusIndex((float) aqi_short, temp_float, humid_float, co2_float, tvoc_float);

                                try {
                                    Log.d("VirusValue", "\n" + "온도 : " + temp_float + "\n습도 : " + humid_float +
                                            "\nPM AQI : " + aqi_short + "\nCO2 AQI : " + co2_float + "\nTVOC AQI : " +
                                            tvoc_float + "\nVirusValue : " + virusValue + "\nVirusIndex : " + virusIndex);
                                    binding.listCardVIRUSIndex.setText(virusValue + "");
                                    switch (virusIndex) {
                                        case "0":
                                            VirusItemTextColor("0", binding.listCardVIRUSIndex, binding.listCardVIRUSOCGrade);
                                            break;
                                        case "1":
                                            VirusItemTextColor("1", binding.listCardVIRUSIndex, binding.listCardVIRUSOCGrade);
                                            break;
                                        case "2":
                                            VirusItemTextColor("2", binding.listCardVIRUSIndex, binding.listCardVIRUSOCGrade);
                                            break;
                                        case "3":
                                            VirusItemTextColor("3", binding.listCardVIRUSIndex, binding.listCardVIRUSOCGrade);
                                            break;
                                        default:
                                            VirusItemTextColor("4", binding.listCardVIRUSIndex, binding.listCardVIRUSOCGrade);
                                            break;
                                    }
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                    binding.listCardVIRUSOCGrade.setText("에러");
                                    binding.listCardVIRUSIndex.setText("0");
                                }
                            } catch (NullPointerException | IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };
            Timer scheduler = new Timer();
            scheduler.scheduleAtFixedRate(data_timerTask, 0, interval * 1000L);
        }
    }

    private void ChartTimerTask(int yMax, String s) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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

    //AQI Index 별 차트 이동거리 계산
    public void moveBarChart(int cqiNumber) {

        if (cqiNumber != 0) {
            params.setMargins((cqiNumber * barViewWidth / 300) - (arrowWidth / 2),
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
            binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
            binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
        } else if (cqiNumber < 101) {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.normal));
            binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
            binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
        } else if (cqiNumber < 251) {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_bad, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.bad));
            binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressBad));
            binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressBad));
        } else {
            binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_verybad, null));
            binding.aqiContentTv.setText(getResources().getString(R.string.baddest));
            binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressWorst));
            binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressWorst));
        }
    }

    //현재 시간 불러오기
    public void currentTimeIndex() {
        final Handler CurrentTimeHandler = new Handler(Looper.getMainLooper()) {
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
                CurrentTimeHandler.sendEmptyMessage(1); // 핸들러 호출(시간 최신화)
            }
        };

        Thread thread = new Thread(task);
        thread.start();
    }

    //햄버거 메뉴 추가
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
                    dialog_setupDate.setText(setUpDateStr);
                    dialog_serialNumber.setText(serialNumber);

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

        sidebar.setEventListener(new SideBarCustomView.EventListener() {
            @Override
            public void btnCancel() {
                closeMenu();
            }

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
    public void closeMenu() {
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
    public void showMenu() {
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
        switch (view.getId()) {
            case (R.id.hambugerMenuIv):
                //사이드 메뉴 클릭 시 이벤트
                if (!isMenuShow) {
                    showMenu();
                } else {
                    closeMenu();
                }
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
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothThread.isConnected()) {
                        tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                        if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
                            tv.setTextSize(18);

                        } else {
                            tv.setTextSize(14);
                        }
                        tv.setBackground(AppCompatResources.getDrawable(context, R.drawable.category_text_outline));
                        xLabelList.clear();
                        CHART_MADE_TIME = System.currentTimeMillis();
                        if (tv.getText().toString().equals(getString(R.string.aqi))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(300, "cqi");
                            ChartTimerTask(300, "cqi");
                        } else if (tv.getText().toString().equals(getString(R.string.fine_dust))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(75, "pm");
                            ChartTimerTask(75, "pm");
                        } else if (tv.getText().toString().equals(getString(R.string.co))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(11, "co");
                            ChartTimerTask(11, "co");
                        } else if (tv.getText().toString().equals(getString(R.string.co2))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(co2_float.intValue() + 500, "co2");
                            ChartTimerTask(co2_float.intValue() + 500, "co2");
                        } else if (tv.getText().toString().equals(getString(R.string.tvoc))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(2, "tvoc");
                            ChartTimerTask(2, "tvoc");
                        } else if (tv.getText().toString().equals(getString(R.string.virus))) {
                            drawGraphClass.reDrawChart();
                            drawGraphClass.drawFirstEntry(100, "virus");
                            ChartTimerTask(100, "virus");
                        }
                    }
                }
            });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void GraphDataSideHandler() {
        Handler GetDataHandler = new Handler(Looper.getMainLooper());
        GetDataHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            binding.loadingPb.setVisibility(View.GONE);
                            binding.idMain.setEnabled(true);
                            binding.idMain.setAlpha(1f);

                            regVirusListener(VIEW_REQUEST_INTERVAL, virusScheduler);

                            Handler DrawGraphHandler = new Handler(Looper.getMainLooper());
                            DrawGraphHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    drawGraphClass.reDrawChart();
                                    drawGraphClass.drawFirstEntry(300, "cqi");
                                    ChartTimerTask(300, "cqi");
                                    Handler addSideViewHandler = new Handler(Looper.getMainLooper());
                                    addSideViewHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            addSideView();
                                        }
                                    }, 500);
                                }
                            }, 500);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 500);
    }

    public class DrawGraphClass extends Thread {
        // https://medium.com/hongbeomi-dev/mpandroidchart-%EB%9D%BC%EC%9D%B4%EB%B8%8C%EB%9F%AC%EB%A6%AC%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-chart-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-kotlin-93c18ae7568e

        LineData lineData = new LineData();
        Legend legend = new Legend();
        LineDataSet lineDataSet;

        void setChart(int setYMax) {
            // X축
            XAxis xAxis = binding.virusLineChart.getXAxis();
            xAxis.setDrawLabels(true); // 라벨 표시 여부
            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
            xAxis.setDrawAxisLine(false); // AxisLine 표시
            xAxis.setDrawGridLines(false); // GridLine 표시
            xAxis.setGranularityEnabled(false); // x축 간격을 제한하는 세분화 기능
            xAxis.setValueFormatter(new XAxisValueFormat());
            xAxis.setGranularity(1);
            binding.virusLineChart.setAutoScaleMinMaxEnabled(true); // Max = Count
            xAxis.setLabelCount(6); // 라벨 갯수

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
            yAxis.setLabelCount(2);
            yAxis.setDrawGridLines(false); // GridLine 표시
            yAxis.setDrawAxisLine(false); // AxisLine 표시

            legend.setTextColor(Color.WHITE);
            legend.setEnabled(false); // 범례 비활성화
            legend.setTextSize(11);

            binding.virusLineChart.setData(lineData); // 라인차트 데이터 설정
        }

        // 차트에 쓰일 목록 UI Thread 에서 가져오기
        public void feedMultiple(int SetYMax, float yData) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setChart(SetYMax);
                            addEntry(yData);
                        }
                    });
                }
            });
            thread.start();
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
            lineDataSet.setDrawValues(true);
            lineDataSet.setValueTextSize(11);
            lineDataSet.setColor(ResourcesCompat.getColor(getResources(), R.color.lineChartLine, null)); // 색상 지정
        }

        //Y축 엔트리 포멧
        private class YAxisValueFormat extends IndexAxisValueFormatter {
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
        private class XAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                return chartTimeDivider(xLabelList, (int) value);
            }
        }

        public void reDrawChart() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (chartScheduler != null) {
                        chartScheduler.cancel();
                    }
                    lineData.clearValues();
                    lineData.notifyDataChanged();
                    binding.virusLineChart.clear();
                    binding.virusLineChart.notifyDataSetChanged();
                    binding.virusLineChart.removeAllViews();
                }
            });
        }

        public void drawFirstEntry(int setYMax, String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (s != null) {
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
                }
            });
        }

        private String chartTimeDivider(ArrayList<String> arrayList, int count) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm");
            long lArray;
            if (count == 0) {
                CHART_MADE_TIME = System.currentTimeMillis();
                lArray = CHART_MADE_TIME - (10 * 60 * 1000);
            } else if (count == 1) {
                lArray = CHART_MADE_TIME;
            } else {
                lArray = CHART_MADE_TIME + ((long) (count - 1) * 10 * 60 * 1000);
            }
            arrayList.add(count + 1, simpleDateFormat.format(lArray));
            return arrayList.get(count + 1);
        }
    }

    // 그래프차트 카테고리 클릭 이벤트(미선택)
    public void CategoryNotChoice(TextView tv1, TextView tv2, TextView tv3, TextView
            tv4, TextView tv5) {
        try {
            if (bluetoothThread.isConnected()) {
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
                tv1.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                tv2.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                tv3.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
                tv3.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                tv4.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
                tv4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                tv5.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline));
                tv5.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    public void CqiGradeChange(String s1, TextView cqi, TextView arrow) {
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
                cqi.setText(getString(R.string.baddest));
                break;
            case "4":
                cqi.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                cqi.setText(getString(R.string.error));
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
        s2.setVisibility(View.VISIBLE);
        tv1.setVisibility(View.VISIBLE);
        tv2.setVisibility(View.VISIBLE);

        switch (s1) {
            case "0":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                tv1.setText(getString(R.string.good));
                break;
            case "1":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                tv1.setText(getString(R.string.normal));
                break;
            case "2":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                tv1.setText(getString(R.string.bad));
                break;
            case "3":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                tv1.setText(getString(R.string.baddest));
                break;
            case "4":
                tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                tv1.setText(getString(R.string.error));
        }
    }

    public void VirusItemTextColor(String i, TextView index, TextView grade) {
        index.setVisibility(View.VISIBLE);
        grade.setVisibility(View.VISIBLE);

        switch (i) {
            case "0":
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressGood, null));
                grade.setText(getString(R.string.good));
                break;
            case "1":
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressNormal, null));
                grade.setText(getString(R.string.normal));
                break;
            case "2":
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressBad, null));
                grade.setText(getString(R.string.bad));
                break;
            case "3":
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.progressWorst, null));
                grade.setText(getString(R.string.baddest));
                break;
            case "4":
                index.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                grade.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
                grade.setText(getString(R.string.error));
        }
    }

    @Override
    public void onBackPressed() {
        //  앱 종료 메시지 창 띄우기
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.exit_app_title));
        builder.setMessage(getString(R.string.exit_app_message));
        builder.setPositiveButton(getString(R.string.exit_app_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (bluetoothThread.isConnected()) {
                    bluetoothThread.closeSocket();
                }

                if (bluetoothThread.isRunning()) {
                    bluetoothThread.interrupt();
                }
                dialog.dismiss();
                DashBoardActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton(getString(R.string.exit_app_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });
        builder.show();
    }
}