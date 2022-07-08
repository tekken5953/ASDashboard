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

import com.example.dashboard.HideNavigationBarClass;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.bluetooth.BluetoothThread;
import com.example.dashboard.dashboard.DashBoardActivity;
import com.example.dashboard.language.LanguageSelectActivity;

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
    ArrayList<BluetoothDevice> noBondedList = new ArrayList<>();
    ArrayList<BluetoothDevice> bondedList = new ArrayList<>();
    BluetoothThread bluetoothThread;

    String[] deviceNameStr;
    ConstraintLayout activityLayout;
    int noPairingPosition = 0;

    IntentFilter filter = new IntentFilter();

    // 블루투스 브로드캐스트 호출 - 주변기기 검색
    @Override
    protected void onResume() {
        super.onResume();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("LifeCycle", "On Resume");
                cList.clear();
                pList.clear();
                findPairedDevice();

                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LifeCycle", "On Pause");
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
        HideNavigationBarClass hideNavigationBarClass = new HideNavigationBarClass();
        hideNavigationBarClass.hide(ConnectDeviceActivity.this); // 하단 바 없애기
        Log.d("LifeCycle", "On WindowFocusChanged");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_bluetooth_activity);
        Log.d("LifeCycle", "On Create");

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
                loadingPb.setVisibility(View.VISIBLE);
                activityLayout.setAlpha(0.3f);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //선택한 디바이스 페어링 요청
                            Log.d("paringDevice", "position : " + position + " name : " + noBondedList.get(position).getName());
                            noPairingPosition = position;
                            BluetoothDevice device = noBondedList.get(position);
                            Method method = device.getClass().getMethod("createBond", (Class[]) null);
                            method.invoke(device, (Object[]) null);
                            loadingPb.setVisibility(View.GONE);
                            activityLayout.setAlpha(1f);
                        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "이미 연결된 디바이스 입니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
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
                pairedDeviceList.setVisibility(View.VISIBLE);
                bondedList.add(device);
                if (device.getName().contains(" ")) {
                    deviceNameStr = device.getName().split(" ");
                    addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_connect, null),
                            deviceNameStr[0],
                            deviceNameStr[1]);
                } else {
                    addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_connect, null), device.getName(), null);
                }
                pAdapter.notifyDataSetChanged();
            }
        } else {
            pairedDeviceList.setVisibility(View.GONE);
        }
        cAdapter.notifyDataSetChanged();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { //각각의 디바이스로부터 정보를 받으려면 만들어야함
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 사용 가능한 디바이스 불러오는 브로드캐스트 리시버
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
                        noBondedList.add(device);
                        addCItem(deviceName);
                        cAdapter.notifyDataSetChanged();
                    }
                }
            }

            // 디바이스 페어링 리시버
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                loadingPb.setVisibility(View.VISIBLE);
                activityLayout.setAlpha(0.3f);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadingPb.setVisibility(View.GONE);
                        activityLayout.setAlpha(1f);
                        try {
                            cList.remove(noPairingPosition);
                            addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_connect, null), noBondedList.get(noPairingPosition).getName(), null);
                            cAdapter.notifyDataSetChanged();
                            pAdapter.notifyDataSetChanged();
                        } catch (IndexOutOfBoundsException e) {
                            finish(); //인텐트 종료
                            overridePendingTransition(0, 0);//인텐트 효과 없애기
                            Intent refresh = getIntent(); //인텐트
                            startActivity(refresh); //액티비티 열기
                            overridePendingTransition(0, 0);//인텐트 효과 없애기
                        }
                    }
                }, 2000);
            }
        }
    };
}