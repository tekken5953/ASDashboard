package com.example.dashboard.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.dashboard.OuterClass;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.bluetooth.BluetoothThread;
import com.example.dashboard.dashboard.DashBoardActivity;
import com.example.dashboard.databinding.ConnectBluetoothActivityBinding;
import com.example.dashboard.language.LanguageSelectActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

@SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
public class ConnectDeviceActivity extends AppCompatActivity {
    ConnectBluetoothActivityBinding binding;

    int SELECTED_POSITION = -1;

    Activity context = ConnectDeviceActivity.this;
    ArrayList<ConnectRecyclerItem> cList = new ArrayList<>();
    ArrayList<PairedDeviceItem> pList = new ArrayList<>();
    ConnectRecyclerAdapter cAdapter;
    PairedDeviceAdapter pAdapter;

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> noBondedList = new ArrayList<>();
    ArrayList<BluetoothDevice> bondedList = new ArrayList<>();
    BluetoothThread bluetoothThread;

    String[] deviceNameStrLeft, deviceNameStrRight;
    int noPairingPosition = 0;

    IntentFilter filter = new IntentFilter();

    OuterClass outerClass = new OuterClass();

    // 블루투스 브로드캐스트 호출 - 주변기기 검색
    @Override
    protected void onResume() {
        super.onResume();
        outerClass.FullScreenMode(context);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
        }, 1500);
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
    protected void onPause() {
        super.onPause();
        binding.connRefreshIv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            outerClass.FullScreenMode(context); // 하단 바 없애기
            Log.d("LifeCycle", "On WindowFocusChanged");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ConnectBluetoothActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        Log.d("LifeCycle", "On Create");

        context = ConnectDeviceActivity.this;
        cAdapter = new ConnectRecyclerAdapter(cList);
        pAdapter = new PairedDeviceAdapter(pList);
        binding.connConnectableList.setAdapter(cAdapter);
        binding.connPairedDeviceRv.setAdapter(pAdapter);
        bluetoothThread = new BluetoothThread(this);
        binding.loadingParingPb.setVisibility(View.GONE);
        binding.connOkTv.setEnabled(false);
        binding.connOkTv.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_b, null));
        binding.connOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        binding.connRefreshIv.setVisibility(View.GONE);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        cAdapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                //https://ghj1001020.tistory.com/291
                binding.loadingParingPb.setVisibility(View.VISIBLE);
                binding.connMainLayout.setAlpha(0.3f);

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
                            binding.loadingParingPb.setVisibility(View.GONE);
                            binding.connMainLayout.setAlpha(1f);
                        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                            e.printStackTrace();
                            Toast.makeText(context, getString(R.string.already_connected), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 2000);
            }
        });

        binding.connOkTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.connOkTv.isEnabled()) {
                    binding.loadingParingPb.setVisibility(View.VISIBLE);
                    binding.connMainLayout.setAlpha(0.3f);
                    Intent intent = new Intent(context, DashBoardActivity.class);
                    intent.putExtra("device_position", SELECTED_POSITION);
                    overridePendingTransition(0, 0);
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
                    binding.connOkTv.setEnabled(true);
                    binding.connOkTv.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_w, null));
                    binding.connOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                } else if (SELECTED_POSITION == position) {

                } else {
                    for (int i = 0; i < binding.connPairedDeviceRv.getAdapter().getItemCount(); i++) {
                        View otherView = binding.connPairedDeviceRv.getLayoutManager().findViewByPosition(i);
                        if (otherView != v) {
                            otherView.setAlpha(0.5f);
                        }
                    }
                    v.setAlpha(1f);
                    SELECTED_POSITION = position;
                }
            }
        });

        binding.connTopBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferenceManager.setString(context, "skip_lang", "no");
                Intent intent = new Intent(context, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }
        });

        binding.connRefreshIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.connRefreshIv.setVisibility(View.GONE);
                cList.clear();
                pList.clear();
                startCheckBluetooth();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, intentFilter);

                if (!bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.startDiscovery();
                }
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
                binding.connPairedDeviceRv.setVisibility(View.VISIBLE);
                bondedList.add(device);
                if (device.getName().contains(" ")) {
                    deviceNameStrLeft = device.getName().split(" ");
                    addPItem(filteringImage(deviceNameStrLeft[0]),
                            deviceNameStrLeft[0],
                            deviceNameStrLeft[1]);
                } else {
                    addPItem(filteringImage(device.getName()), device.getName(), null);
                }
                pAdapter.notifyDataSetChanged();
            }
        }
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
//                    addCItem(filteringImage(deviceNameStr[0],
//                                deviceNameStr[0],
//                                deviceNameStr[1]);
//                    cAdapter.notifyDataSetChanged();

//                     필터링
                    if (deviceName != null && deviceName.contains("BS_")) {

                        noBondedList.add(device);
                        if (deviceName.contains(" ")) {
                            deviceNameStrRight = deviceName.split(" ");
                            addCItem(filteringImage(deviceNameStrRight[0]),
                                    deviceNameStrRight[0],
                                    deviceNameStrRight[1]);
                        } else {
                            addCItem(filteringImage(deviceName),
                                    deviceName,
                                    "(No Serial Number)");
                        }
                        cAdapter.notifyDataSetChanged();
                    }
                }
            }

            // 디바이스 페어링 리시버
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                binding.loadingParingPb.setVisibility(View.VISIBLE);
                binding.connMainLayout.setAlpha(0.3f);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.loadingParingPb.setVisibility(View.GONE);
                        binding.connMainLayout.setAlpha(1f);
                        try {
                            addPItem(filteringImage(noBondedList.get(noPairingPosition).getName()), noBondedList.get(noPairingPosition).getName(), null);
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

    private Drawable filteringImage(String s) {
        if (s.contains("BS_M")) {
            return ResourcesCompat.getDrawable(getResources(), R.drawable.mini_icon, null);
        } else if (s.contains("BS_100")) {
            return ResourcesCompat.getDrawable(getResources(), R.drawable.bs_100_icon, null);
        } else {
            return null;
        }
    }

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