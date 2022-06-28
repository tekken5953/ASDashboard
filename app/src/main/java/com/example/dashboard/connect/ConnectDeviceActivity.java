package com.example.dashboard.connect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dashboard.LanguageSelectActivity;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.dashboard.DashBoardFragment;

import java.util.ArrayList;
import java.util.Set;

public class ConnectDeviceActivity extends AppCompatActivity {

    ImageView selectLanguage;
    Context context;
    RecyclerView deviceList, pairedDeviceList;
    ArrayList<ConnectRecyclerItem> cList = new ArrayList<>();
    ArrayList<PairedDeviceItem> pList = new ArrayList<>();
    ConnectRecyclerAdapter cAdapter;
    PairedDeviceAdapter pAdapter;

    TextView connConnectableDeviceTv;

    BluetoothAdapter bluetoothAdapter;

    DisplayMetrics dm = new DisplayMetrics();

    // 블루투스 브로드캐스트 호출 - 주변기기 검색
    @Override
    protected void onResume() {
        super.onResume();
        cList.clear();
        pList.clear();
        findPairedDevice();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
            Log.d("Bluetooth", "Discovering...");
        } else {
            Log.d("Bluetooth", "Not Discovering...");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        bluetoothAdapter.cancelDiscovery();
        cList.clear();
        pList.clear();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

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

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        getWindowManager().getDefaultDisplay().getMetrics(dm); // 기기 해상도를 구하기 위함

        cAdapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Intent intent = new Intent(context, DashBoardFragment.class);
                intent.putExtra("device_name", cList.get(position).getDevice_name());
                startActivity(intent);
                finish();
            }
        });

        pAdapter.setOnItemClickListener(new PairedDeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Intent intent = new Intent(context, DashBoardFragment.class);
                intent.putExtra("device_name", pList.get(position).getName() + "(" + pList.get(position).getAddress() + ")");
                startActivity(intent);
                finish();
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
                if (dm.widthPixels > 1900 && dm.heightPixels > 1000) {
                    addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_1920, null), device.getName(), device.getAddress());
                    pAdapter.notifyDataSetChanged();
                } else {
                    addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_1280, null), device.getName(), device.getAddress());
                    pAdapter.notifyDataSetChanged();
                }
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
                    String deviceAddress = device.getAddress();
                    //필터링 없이 하려면 주석 해제 + 밑에 필터링부분 주석처리
//                    addCItem(deviceName + "(" + deviceAddress + ")", "연결하기");
//                    cAdapter.notifyDataSetChanged();
                    // 필터링
                    if (deviceName != null && deviceName.contains("BioT")) {
                        addCItem(deviceName + "(" + deviceAddress + ")");
                        cAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };
}