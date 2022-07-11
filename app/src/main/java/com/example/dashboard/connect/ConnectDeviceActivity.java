package com.example.dashboard.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
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

    int SELECTED_POSITION = -1;

    ImageView selectLanguage, refresh;
    Context context;
    RecyclerView deviceList, pairedDeviceList;
    ArrayList<ConnectRecyclerItem> cList = new ArrayList<>();
    ArrayList<PairedDeviceItem> pList = new ArrayList<>();
    ConnectRecyclerAdapter cAdapter;
    PairedDeviceAdapter pAdapter;
    ProgressBar loadingPb;

    TextView connConnectableDeviceTv;

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> noBondedList = new ArrayList<>();
    ArrayList<BluetoothDevice> bondedList = new ArrayList<>();
    BluetoothThread bluetoothThread;

    String[] deviceNameStrLeft, deviceNameStrRight;
    RelativeLayout activityLayout;
    int noPairingPosition = 0;

    IntentFilter filter = new IntentFilter();

    AppCompatButton ok_btn;

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

                startCheckBluetooth();

                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                registerReceiver(mReceiver, filter);

                if (!bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.startDiscovery();
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
        if (hasFocus) {
            HideNavigationBarClass hideNavigationBarClass = new HideNavigationBarClass();
            hideNavigationBarClass.hide(ConnectDeviceActivity.this); // 하단 바 없애기
            Log.d("LifeCycle", "On WindowFocusChanged");
            if (!bluetoothAdapter.isDiscovering()) {
                refresh.setVisibility(View.VISIBLE);
            } else {
                refresh.setVisibility(View.GONE);
            }
        }
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
        bluetoothThread = new BluetoothThread(this);
        loadingPb = findViewById(R.id.loadingParingPb);
        loadingPb.setVisibility(View.GONE);
        activityLayout = findViewById(R.id.connMainLayout);
        ok_btn = findViewById(R.id.connOkTv);
        ok_btn.setEnabled(false);
        ok_btn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_b, null));
        refresh = findViewById(R.id.connRefreshIv);
        refresh.setVisibility(View.GONE);

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
                            Toast.makeText(context, getString(R.string.already_connected), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
            }
        });

        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ok_btn.isEnabled()) {
                    loadingPb.setVisibility(View.VISIBLE);
                    activityLayout.setAlpha(0.3f);
                    Intent intent = new Intent(context, DashBoardActivity.class);
                    intent.putExtra("device_position", SELECTED_POSITION);
                    overridePendingTransition(0,0);
                    startActivity(intent);
                    finish();
                }
            }
        });

        pAdapter.setOnItemClickListener(new PairedDeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {

                if (SELECTED_POSITION == -1) {
                    v.setAlpha(1f);
                    SELECTED_POSITION = position;
                    ok_btn.setEnabled(true);
                    ok_btn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_w, null));
                } else if (SELECTED_POSITION == position) {
                    v.setAlpha(0.5f);
                    SELECTED_POSITION = -1;
                    ok_btn.setEnabled(false);
                    ok_btn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_b, null));
                    ok_btn.setTextColor(Color.WHITE);
                } else {
                    for (int i = 0; i < pairedDeviceList.getAdapter().getItemCount(); i++) {
                        View otherView = pairedDeviceList.getLayoutManager().findViewByPosition(i);
                        if (otherView != v) {
                            otherView.setAlpha(0.5f);
                        }
                    }
                    v.setAlpha(1f);
                    SELECTED_POSITION = position;
                }
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

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResume();
            }
        });
    }

    //Connectable Device Item
    public void addCItem(Drawable img, String name, String address) {
        ConnectRecyclerItem item = new ConnectRecyclerItem(img, name, address);

        item.setDevice_img(img);
        item.setDevice_name(name);
        item.setDevice_address(address);

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
                    deviceNameStrLeft = device.getName().split(" ");
                    addPItem(ResourcesCompat.getDrawable(getResources(), R.drawable.m_connect, null),
                            deviceNameStrLeft[0],
                            deviceNameStrLeft[1]);
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
//                    deviceNameStrRight = deviceName.split(" ");
                    //필터링 없이 하려면 주석 해제 + 밑에 필터링부분 주석처리
//                    notPairedDeviceList.add(device);
//                    addCItem(ResourcesCompat.getDrawable(getResources(), R.drawable.side_100, null),
//                                deviceNameStr[0],
//                                deviceNameStr[1]);
//                    cAdapter.notifyDataSetChanged();

//                     필터링
                    if ((deviceName != null && deviceName.contains("BioT")) || (deviceName != null && deviceName.contains("BS"))) {
                        noBondedList.add(device);
                        if (deviceName.contains(" ")) {
                            deviceNameStrRight = deviceName.split(" ");
                            addCItem(ResourcesCompat.getDrawable(getResources(), R.drawable.side_100, null),
                                    deviceNameStrRight[0],
                                    deviceNameStrRight[1]);
                        } else {
                            addCItem(ResourcesCompat.getDrawable(getResources(), R.drawable.side_100, null),
                                    deviceName,
                                    "(No Serial Number)");
                        }
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

    @SuppressLint("MissingPermission")
    public void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "해당 기기는 블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    findPairedDevice();
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
                                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }
                                }).setCancelable(false).show();
                    }
                });
            }
        }
    }
}