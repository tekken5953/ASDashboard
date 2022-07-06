package com.example.dashboard.dashboard;

import static com.example.dashboard.BluetoothAPI.REQUEST_CONTROL;
import static com.example.dashboard.BluetoothAPI.REQUEST_INDIVIDUAL_STATE;
import static com.example.dashboard.BluetoothAPI.analyzedControlBody;
import static com.example.dashboard.BluetoothAPI.analyzedRequestBody;
import static com.example.dashboard.BluetoothAPI.byteArrayToHexString;
import static com.example.dashboard.BluetoothAPI.generateTag;
import static com.example.dashboard.BluetoothAPI.makeFrame;
import static com.example.dashboard.BluetoothAPI.separatedFrame;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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
import com.example.dashboard.SideBarCustomView;
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

public class DashBoardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;

    Context context = DashBoardActivity.this;

    int barViewWidth, barViewHeight, arrowWidth, VIEW_REQUEST_INTERVAL = 3, DRAW_CHART_INTERVAL = 1000 * 3;

    ArrayList<SegmentedProgressBar.BarContext> barList = new ArrayList<>();

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    private Boolean isMenuShow = false;

    BluetoothAdapter bluetoothAdapter;
    BluetoothThread bluetoothThread;
    ArrayList<BluetoothDevice> arrayListDevice;
    BluetoothThread.DataShareViewModel viewModel;
    Set<BluetoothDevice> paredDevice;

    DisplayMetrics dm = new DisplayMetrics();

    Observer<String> data;
    int device_type, setup_date;
    String serialNumber = null, currentTimeIndex, deviceType, modelName, setUpDateStr;

    DrawLineChartClass drawLineChartClass = new DrawLineChartClass();

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
    String temp_str = null, humid_str = null, pm_str = null, co_str = null, co2_str = null, tvoc_str = null;
    String pm_grade = null, co_grade = null, co2_grade = null, tvoc_grade = null;
    String aqi_str = null;
    Short aqi_short;
    Float pm_float, co_float, co2_float, tvoc_float;
    byte fan_control_byte, current_fan_byte, power_control_byte;

    Timer data_scheduler, chart_scheduler;

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
        getWindowManager().getDefaultDisplay().getMetrics(dm); // 기기 해상도를 구하기 위함

        Configuration configuration = new Configuration();

        if (SharedPreferenceManager.getString(context, "finalLanguage").equals("en")) {
            configuration.setLocale(Locale.US);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else if (SharedPreferenceManager.getString(context, "finalLanguage").equals("ko")) {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        } else {
            configuration.setLocale(Locale.KOREA);
            getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
        }

        CreateSegmentProgressView(); // AQI 바 차트 그리기

        binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));

        binding.loadingPb.setVisibility(View.VISIBLE);

    }

    public void init() {

        currentTimeIndex(); // 현재 시간 적용

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        binding.hambugerMenuIv.setOnClickListener(this::onClick);

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
    }

    @SuppressLint("MissingPermission")
    public void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "해당 기기는 블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    pairedDeviceConnect();
                } catch (Exception e) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "장치가 작동중인지 확인하여주세요!", Toast.LENGTH_SHORT).show();
                        }
                    }, 0);

                    backToConnectDevice();
                }

            } else {
                //븥루투스가 꺼져있음
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
            deviceType = Arrays.toString(body.getCharArray("47"));

            Log.d("bluetoothThread", "Device Type is " + deviceType);

            System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));

            if (BluetoothAPI.getDeviceType(body.getCharArray("47")) == 3) { // DeviceFragment.DEVICE_TYPE_MINI
                // Wifi State 확인
                bluetoothThread.writeHex(
                        makeFrame(
                                new byte[]{REQUEST_INDIVIDUAL_STATE},
                                new byte[]{
                                        0x48, 0x00, 0x00, // S/N
                                        0x65, 0x00, 0x00,  // WIFI Connect State
                                        0x3A, 0x00, 0x00  // 현재바람세기
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
                                0x46, 0x00, 0x00,  // 모듈설치날짜
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
        }

        if (body.containsKey("48")) {
            serialNumber = new String(body.getCharArray("48"));
            Log.d("bluetoothThread", "Serial Number is " + serialNumber);
        }

        if (body.containsKey("46")) {
            setup_date = body.getInt("46");
            setUpDateStr = setup_date + "";
            Log.d("bluetoothThread", "SetUp Date is " + setup_date + "");
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
            pm_float = Float.parseFloat(pm_str);
            binding.listCardPMIndex.setText(pm_str);
        }

        if (body.containsKey("21")) {
            tvoc_str = body.getString("21");
            tvoc_float = Float.parseFloat(tvoc_str);
            binding.listCardTVOCIndex.setText(tvoc_str);
        }

        if (body.containsKey("1B")) {
            co_str = body.getString("1B");
            co_float = Float.parseFloat(co_str);
            binding.listCardCOIndex.setText(co_str);
        }

        if (body.containsKey("1E")) {
            co2_str = body.getString("1E");
            co2_float = Float.parseFloat(co2_str);
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

        if (body.containsKey("3A")) {
            current_fan_byte = body.getByte("3A");
            Log.d("bluetoothThread", "Current Fan is " + current_fan_byte);
        }

    }

    private void setPushData(int device_type, Bundle bundle) {
        bundle.putString("protocol", PROTOCOL_BLUETOOTH);
        bundle.putInt("47", device_type);
    }

    private void processControlBody(Bundle body) {
        // 임시
        if (body.containsKey("50") || body.containsKey("51")) {

            if (power_control_byte == 0x01) {

                bluetoothThread.writeHex(generateTag((byte) 0x50, new byte[]{0x01}));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 어플 강제종료
//                        android.os.Process.killProcess(android.os.Process.myPid());
                        Toast.makeText(context, "BS-M을 종료합니다", Toast.LENGTH_SHORT).show();
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

            if (body.getByte("3B") != 0) {

                if (fan_control_byte == 0x00) {
                    bluetoothThread.writeHex(generateTag((byte) 0x3B, new byte[]{0x00}));
                    Log.d("bluetoothThread", "Fan 꺼짐");
                } else if (fan_control_byte == 0x01) {
                    bluetoothThread.writeHex(generateTag((byte) 0x3B, new byte[]{0x01}));
                    Log.d("bluetoothThread", "Fan 수면 단계");
                } else if (fan_control_byte == 0x02) {
                    bluetoothThread.writeHex(generateTag((byte) 0x3B, new byte[]{0x02}));
                    Log.d("bluetoothThread", "Fan 약 단계");
                } else if (fan_control_byte == 0x03) {
                    bluetoothThread.writeHex(generateTag((byte) 0x3B, new byte[]{0x03}));
                    Log.d("bluetoothThread", "Fan 강 단계");
                } else if (fan_control_byte == 0x04) {
                    bluetoothThread.writeHex(generateTag((byte) 0x3B, new byte[]{0x04}));
                    Log.d("bluetoothThread", "Fan 터보 단계");
                } else {
                    Toast.makeText(context, "바람 제어에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("bluetoothThread", "바람제어 에러 발생");
                }

            } else {
                Toast.makeText(context, "바람 제어에 실패했습니다.", Toast.LENGTH_SHORT).show();
                Log.e("bluetoothThread", "바람제어 에러 발생");
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

            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                if (aqi_short != null) {
                                    ChartTimerTask(300, getString(R.string.aqi), (float) aqi_short);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }, 4000);

            Handler handler1 = new Handler(Looper.getMainLooper());
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addSideView();
                            binding.loadingPb.setVisibility(View.GONE);
                        }
                    });
                }
            }, 3000);
        }
    }

    @SuppressLint("MissingPermission")
    private void pairedDeviceConnect() {
        //페어링 된 디바이스
        int position = getIntent().getExtras().getInt("device_position");
        arrayListDevice = new ArrayList<>();
        bluetoothThread = new BluetoothThread((Activity) context);
        paredDevice = bluetoothAdapter.getBondedDevices();
        if (!paredDevice.isEmpty()) {
            arrayListDevice.addAll(paredDevice);
            bluetoothThread.setBluetoothDevice(arrayListDevice.get(position));
            bluetoothThread.setConnectedSocketEventListener(new BluetoothThread.connectedSocketEventListener() {
                @Override
                public void onConnectedEvent() {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                                센서 장착 여부 및 GPS 정보 요청
                            bluetoothThread.writeHex(makeFrame(
                                    new byte[]{REQUEST_INDIVIDUAL_STATE},
                                    new byte[]{
                                            0x35, 0x00, 0x00, // 센서연결확인
                                            //0x43, 0x00, 0x00, // GPS 위도
                                            //0x44, 0x00, 0x00, // GPS 경도
                                            //0x45, 0x00, 0x00, // 펌웨어버전
                                            0x46, 0x00, 0x00, // 모듈설치날짜
                                            0x47, 0x00, 0x00,  // 모델명
                                    },
                                    bluetoothThread.getSequence()
                            ));

                            Log.d("bluetoothThread", "Bluetooth Socket is Connected");
                            Log.d("bluetoothThread", "setDevice by : " + bluetoothThread.getDeviceName());
                            modelName = bluetoothThread.getDeviceName();
                        }
                    }, 1000);

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

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "장치와의 연결이 불안정합니다\n확인 후 재시작하여주십시오", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }, 2000);
                }
            });
            bluetoothThread.connectSocket();

            if (!bluetoothThread.isRunning()) {
                bluetoothThread.start();
                Log.d("bluetoothThread", "BluetoothThread is Run");
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

    private void ChartTimerTask(int yMax, String title, float yData) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawLineChartClass.feedMultiple(yMax, title, yData);
                    }
                });
            }
        };

        chart_scheduler = new Timer();

        chart_scheduler.schedule(timerTask, 0, DRAW_CHART_INTERVAL);

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
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.good));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.normal));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
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
        } else {
            if (aqiNumber < 51) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_good, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.good));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressGood));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressGood));
            } else if (aqiNumber < 101) {
                binding.apiCircleChartPb.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.signal_normal, null));
                binding.aqiContentTv.setText(getResources().getString(R.string.normal));
                binding.aqiContentTv.setTextColor(getResources().getColor(R.color.progressNormal));
                binding.aqiCurrentArrow.setTextColor(getResources().getColor(R.color.progressNormal));
            } else if (aqiNumber < 251) {
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

    //햄버거 메뉴 추가
    private void addSideView() {
        Log.d("bluetoothThread", "Add Side Menu Complete");
        SideBarCustomView sidebar = new SideBarCustomView(context);
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
                if (bluetoothThread.isConnected()) {
                    dialog_setupDate.setText(setUpDateStr);
                    dialog_serialNumber.setText(serialNumber);
                    dialog_productName.setText(modelName);

                    if (modelName.startsWith("BS-M")) {
                        dialog_product_img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.side_m, null));
                    }

                    if (current_fan_byte == 0x01) {
                        dialog_fan1.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    } else if (current_fan_byte == 0x02) {
                        dialog_fan2.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    } else if (current_fan_byte == 0x03) {
                        dialog_fan3.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    } else if (current_fan_byte == 0x04) {
                        dialog_fan4.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    }

                } else {
                    Toast.makeText(context, "블루투스 연결상태를 확인해주세요", Toast.LENGTH_SHORT).show();
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
                        //TODO Turn OFF Control
                        if (bluetoothThread.isConnected()) {
                            power_control_byte = 0x01;
                            bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{(byte) 0x50, 0x00, 0x00}, bluetoothThread.getSequence()));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertDialog.dismiss();
                                }
                            });
                        }
                    }
                });

                dialog_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });

                alertDialog.show();
            }

            @Override
            public void fan1() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x01;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                    dialog_fan1.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    dialog_fan2.setBackground(null);
                    dialog_fan3.setBackground(null);
                    dialog_fan4.setBackground(null);
                }
            }

            @Override
            public void fan2() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x02;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                    dialog_fan2.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    dialog_fan1.setBackground(null);
                    dialog_fan3.setBackground(null);
                    dialog_fan4.setBackground(null);
                }
            }

            @Override
            public void fan3() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x03;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                    dialog_fan3.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    dialog_fan2.setBackground(null);
                    dialog_fan1.setBackground(null);
                    dialog_fan4.setBackground(null);
                }
            }

            @Override
            public void fan4() {
                if (bluetoothThread.isConnected()) {
                    fan_control_byte = 0x04;
                    bluetoothThread.writeHex(makeFrame(new byte[]{REQUEST_CONTROL}, new byte[]{0x3B, 0x00, 0x00}, bluetoothThread.getSequence()));
                    dialog_fan4.setBackground(AppCompatResources.getDrawable(context, R.drawable.side_menu_fan_tv_bg));
                    dialog_fan2.setBackground(null);
                    dialog_fan3.setBackground(null);
                    dialog_fan1.setBackground(null);
                }
            }
        });
    }

    //햄버거 메뉴 닫기
    public void closeMenu() {
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
            if (bluetoothThread.isConnected()) {
                tv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
                    tv.setTextSize(18);
                    tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline));

                } else {
                    tv.setTextSize(14);
                    tv.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_text_outline_small));
                }
                if (tv.getText().toString().equals(getString(R.string.aqi))) {
                    Toast.makeText(this, getString(R.string.aqi), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(300, getString(R.string.aqi), aqi_short);
                    ChartTimerTask(300, getString(R.string.aqi), aqi_short);
                } else if (tv.getText().toString().equals(getString(R.string.fine_dust))) {
                    Toast.makeText(this, getString(R.string.fine_dust), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(75, getString(R.string.fine_dust), pm_float);
                    ChartTimerTask(75, getString(R.string.fine_dust), pm_float);
                } else if (tv.getText().toString().equals(getString(R.string.co))) {
                    Toast.makeText(this, getString(R.string.co), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(11, getString(R.string.co), co_float);
                    ChartTimerTask(11, getString(R.string.co), co_float);
                } else if (tv.getText().toString().equals(getString(R.string.co2))) {
                    Toast.makeText(this, getString(R.string.co2), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(1200, getString(R.string.co2), co2_float);
                    ChartTimerTask(1200, getString(R.string.co2), co2_float);
                } else if (tv.getText().toString().equals(getString(R.string.tvoc))) {
                    Toast.makeText(this, getString(R.string.tvoc), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(1, getString(R.string.tvoc), tvoc_float);
                    ChartTimerTask(1, getString(R.string.tvoc), tvoc_float);
                } else if (tv.getText().toString().equals(getString(R.string.virus))) {
                    Toast.makeText(this, getString(R.string.virus), Toast.LENGTH_SHORT).show();
                    drawLineChartClass.reDrawChart();
                    drawLineChartClass.drawFirstEntry(300, getString(R.string.virus), (short) 123);
                    ChartTimerTask(300, getString(R.string.virus), (short) 123);
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
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
                } else {
                    tv1.setTextSize(14);
                    tv2.setTextSize(14);
                    tv3.setTextSize(14);
                    tv4.setTextSize(14);
                    tv5.setTextSize(14);
                    tv1.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline_small));
                    tv1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                    tv2.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline_small));
                    tv2.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                    tv3.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline_small));
                    tv3.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                    tv4.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline_small));
                    tv4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                    tv5.setBackground(AppCompatResources.getDrawable(this, R.drawable.category_nontext_outline_small));
                    tv5.setTextColor(ResourcesCompat.getColor(getResources(), R.color.lineChartCategoryNonSelectText, null));
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // https://medium.com/hongbeomi-dev/mpandroidchart-%EB%9D%BC%EC%9D%B4%EB%B8%8C%EB%9F%AC%EB%A6%AC%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-chart-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-kotlin-93c18ae7568e

    private class DrawLineChartClass {
        LineData lineData = new LineData();
        ILineDataSet iLineDataSet;
        Legend legend = new Legend();
        LineDataSet lineDataSet;

        void setChart(int setYMax) {
            // X축
            XAxis xAxis = binding.virusLineChart.getXAxis();
            xAxis.setDrawLabels(true); // 라벨 표시 여부
            xAxis.setLabelCount(7); // 라벨 갯수

            xAxis.setTextColor(Color.WHITE);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
            xAxis.setDrawAxisLine(false); // AxisLine 표시
            xAxis.setDrawGridLines(false); // GridLine 표시
            xAxis.setGranularityEnabled(false); // x축 간격을 제한하는 세분화 기능
            xAxis.setGranularity(1);
            binding.virusLineChart.setAutoScaleMinMaxEnabled(true); // Max = Count
            xAxis.setAxisMinimum(0);

            binding.virusLineChart.getAxisRight().setEnabled(false); // 라인차트 오른쪽 데이터 비활성화
            // Y축
            YAxis yAxis = binding.virusLineChart.getAxisLeft();
            yAxis.setAxisMaximum(setYMax); // Y축 값 최대값 설정
            yAxis.setAxisMinimum((0)); // Y축 값 최솟값 설정

            yAxis.setTextColor(Color.parseColor("#FFFFFF")); // y축 글자 색상
            yAxis.setValueFormatter(new YAxisValueFormat()); // y축 데이터 포맷
            yAxis.setGranularityEnabled(false); // y축 간격을 제한하는 세분화 기능
            yAxis.setDrawLabels(true); // Y축 라벨 위치
            yAxis.setDrawGridLines(false); // GridLine 표시
            yAxis.setDrawAxisLine(false); // AxisLine 표시

            legend.setTextColor(Color.WHITE);
            legend.setEnabled(false); // 범례 비활성화

            binding.virusLineChart.setData(lineData); // 라인차트 데이터 설정

        }

        // 차트에 쓰일 목록 UI Thread 에서 가져오기
        void feedMultiple(int SetYMax, String s, float yData) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    setChart(SetYMax);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry(s, yData);
                        }
                    });
                }
            });
            thread.start();
        }

        // 엔트리 추가하기
        void addEntry(String s, float yData) {
            lineData = binding.virusLineChart.getData();
            // 라인 차트
            if (lineData != null) {
                createSet(s);
                lineData.addDataSet(lineDataSet);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("Timertask", yData + "");
                        Log.e("Timertask", lineData.getEntryCount() + "");
                        lineData.addEntry(new Entry(lineData.getEntryCount(), yData), 0); // 데이터 엔트리 추가
                        lineData.notifyDataChanged(); // 데이터 변경 알림
                        binding.virusLineChart.notifyDataSetChanged(); // 라인차트 변경 알림
                    }
                });
                binding.virusLineChart.moveViewToX(lineData.getEntryCount()); // 계속 X축을 데이터의 오른쪽 끝으로 옮기기
                binding.virusLineChart.setVisibleXRangeMaximum(7); // X축 최대 표현 개수
                binding.virusLineChart.setPinchZoom(false); // 확대 설정
                binding.virusLineChart.setDoubleTapToZoomEnabled(false); // 더블탭 설정
                binding.virusLineChart.getDescription().setEnabled(false); // 차트 값 설명 유효화
                binding.virusLineChart.setBackgroundColor(Color.TRANSPARENT); // 차트 배경색 설정
                binding.virusLineChart.setExtraOffsets(5f, 5f, 5f, 5f); // 차트 Padding 설정
                binding.virusLineChart.setNoDataText(getString(R.string.no_data_text));
            }
        }

        void createSet(String s) {
            lineDataSet = new LineDataSet(null, s); // 범례, yVals 설정
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT); // Y값 데이터를 왼쪽으로
            lineDataSet.setFillColor(Color.parseColor("#147AD6")); // 차트 채우기 색상
            lineDataSet.setDrawFilled(true); // 차트 채우기 설정
            lineDataSet.setHighlightEnabled(false); // 하이라이트 설정
            lineDataSet.setLineWidth(2F); // 그래프 선 굵기
            lineDataSet.setValueTextColor(Color.TRANSPARENT);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
            lineDataSet.setDrawCircleHole(false); // 원 안에 작은 원 표시
            lineDataSet.setDrawCircles(false); // 원 표시
            lineDataSet.setColor(ResourcesCompat.getColor(getResources(), R.color.lineChartLine, null)); // 색상 지정
        }

        //Y축 엔트리 포멧
        private class YAxisValueFormat extends IndexAxisValueFormatter {
            @Override
            public String getFormattedValue(float value) {
                String newValue = value + "";
                return newValue.substring(0, newValue.length() - 2);
            }
        }

        void reDrawChart() {
            lineData.clearValues();
            lineData.notifyDataChanged();
            binding.virusLineChart.clear();
            binding.virusLineChart.notifyDataSetChanged();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chart_scheduler.cancel();
                    binding.virusLineChart.removeAllViews();
                }
            });
        }

        void reFrashChart() {
            binding.virusLineChart.clear();
            binding.virusLineChart.notifyDataSetChanged();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    binding.virusLineChart.removeAllViews();
                }
            });

        }

        void drawFirstEntry(int setYMax, String s, float yData) {
            if (yData != 0) {
                feedMultiple(setYMax, s, yData);
            }
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
        }
    }

    public void backToConnectDevice() {
        Intent intent = new Intent(context, ConnectDeviceActivity.class);
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
        if (bluetoothThread.isConnected()) {
            bluetoothThread.closeSocket();
        }

        if (bluetoothThread.isRunning()) {
            bluetoothThread.interrupt();
        }

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}