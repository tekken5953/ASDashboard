package com.example.dashboard.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dashboard.BluetoothThread;
import com.example.dashboard.LanguageSelectActivity;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.dashboard.DashBoardActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

@SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
public class ConnectDeviceActivity extends AppCompatActivity {

    ImageView selectLanguage, connRefreshIv;
    Context context;
    RecyclerView deviceList, pairedDeviceList;
    ArrayList<ConnectRecyclerItem> cList = new ArrayList<>();
    ArrayList<PairedDeviceItem> pList = new ArrayList<>();
    ConnectRecyclerAdapter cAdapter;
    PairedDeviceAdapter pAdapter;
    ProgressBar progressBar, loadingPb;

    TextView connConnectableDeviceTv;

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> notPairedDeviceList = new ArrayList<>();
    BluetoothThread bluetoothThread;

    String[] deviceNameStr;
    ConstraintLayout activityLayout;

    private static final int SELECT_DEVICE_REQUEST_CODE = 42;
    int selectDevice = 0;
    boolean pairing = true;

    // 블루투스 브로드캐스트 호출 - 주변기기 검색
    @Override
    protected void onResume() {
        super.onResume();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cList.clear();
                pList.clear();
                findPairedDevice();
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);

                if (!bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.startDiscovery();
                    progressBar.setVisibility(View.VISIBLE);
                    connRefreshIv.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    connRefreshIv.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (bluetoothAdapter.isDiscovering()) {
            unregisterReceiver(mReceiver);
            bluetoothAdapter.cancelDiscovery();
        }
        cList.clear();
        pList.clear();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_bluetooth_activity);

        selectLanguage = findViewById(R.id.connTopBackIv);
        context = ConnectDeviceActivity.this;
        pairedDeviceList = findViewById(R.id.connPairedDeviceRv);
        deviceList = findViewById(R.id.connConnectableList);
        cAdapter = new ConnectRecyclerAdapter(cList);
        pAdapter = new PairedDeviceAdapter(pList);
        deviceList.setAdapter(cAdapter);
        pairedDeviceList.setAdapter(pAdapter);
        connConnectableDeviceTv = findViewById(R.id.connConnectableDeviceTv);
        progressBar = findViewById(R.id.connConnectableDevicePb);
        connRefreshIv = findViewById(R.id.connRefreshIv);
        bluetoothThread = new BluetoothThread(this);
        loadingPb = findViewById(R.id.loadingParingPb);
        loadingPb.setVisibility(View.GONE);
        activityLayout = findViewById(R.id.activityLayout);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        cAdapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                //https://ghj1001020.tistory.com/291
                try {
                    //선택한 디바이스 페어링 요청
                    Log.d("paringDevice", "position : " + position + " name : " + notPairedDeviceList.get(position).getName());
                    BluetoothDevice device = notPairedDeviceList.get(position);
                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(device, (Object[]) null);
                    while (pairing) {
                        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
                        if (!pairedDevice.isEmpty()) {
                            finish(); //인텐트 종료
                            overridePendingTransition(0, 0);//인텐트 효과 없애기
                            Intent intent = getIntent(); //인텐트
                            startActivity(intent); //액티비티 열기
                            overridePendingTransition(0, 0);//인텐트 효과 없애기
                            pairing = false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        pAdapter.setOnItemClickListener(new PairedDeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                loadingPb.setVisibility(View.VISIBLE);
                activityLayout.setAlpha(0.3f);

                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Intent intent = new Intent(context, DashBoardActivity.class);
                        intent.putExtra("device_position", position);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingPb.setVisibility(View.GONE);
                                activityLayout.setAlpha(1f);
                            }
                        });
                        startActivity(intent);
                        finish();
                    }
                }, 2000);
            }
        });

        selectLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferenceManager.setString(context, "skip_lang", "no");
                Intent intent = new Intent(context, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }
        });

        connRefreshIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResume();
            }
        });
    }

    //Connectable Device Item
    public void addCItem(String name) {
        ConnectRecyclerItem item = new ConnectRecyclerItem(name);

        item.setDevice_name(name);

        cList.add(item);
    }

    //Paired Device Item
    public void addPItem(Drawable icon, String name, String address) {
        PairedDeviceItem item = new PairedDeviceItem(icon, name, address);

        item.setIcon(icon);
        item.setName(name);
        item.setAddress(address);

        pList.add(item);
    }

    // 페어링 된 디바이스 불러오기
    public void findPairedDevice() {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        if (!pairedDevice.isEmpty()) {
            for (BluetoothDevice device : pairedDevice) {
                selectDevice++;
                pairedDeviceList.setVisibility(View.VISIBLE);
                deviceNameStr = device.getName().split(" ");
                addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_connect, null),
                        deviceNameStr[0],
                        deviceNameStr[1].substring(1, deviceNameStr[1].length() - 1));
                pAdapter.notifyDataSetChanged();
            }
        } else {
            pairedDeviceList.setVisibility(View.GONE);
        }
        cAdapter.notifyDataSetChanged();
    }

    // 사용 가능한 디바이스 불러오는 브로드캐스트 리시버
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { //각각의 디바이스로부터 정보를 받으려면 만들어야함
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    String deviceName = device.getName();
                    //필터링 없이 하려면 주석 해제 + 밑에 필터링부분 주석처리
//                    notPairedDeviceList.add(device);
//                    addCItem(deviceName + "(" + deviceAddress + ")");
//                    cAdapter.notifyDataSetChanged();
//                     필터링
                    if ((deviceName != null && deviceName.contains("BioT")) || (deviceName != null && deviceName.contains("BS"))) {
                        notPairedDeviceList.add(device);
                        addCItem(deviceName);
                        cAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };
}