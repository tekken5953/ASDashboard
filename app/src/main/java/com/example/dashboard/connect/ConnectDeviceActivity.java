package com.example.dashboard.connect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

    static final String TAG_LIFECYCLE = "LIFECYCLE";

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

    IntentFilter foundFilter = new IntentFilter();
    IntentFilter pairingFilter = new IntentFilter();

    OuterClass outerClass = new OuterClass();

    @Override
    protected void onResume() {
        super.onResume();
        outerClass.FullScreenMode(context);

        // 연결 가능한 주변 블루투스 기기들을 발견할 경우 BroadCast Receiver에 IntentFilter를 통해 메시지를 호출합니다
        Handler FindConnectableHandler = new Handler(Looper.getMainLooper());
        FindConnectableHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG_LIFECYCLE, "On Resume");
                        // 리스트를 초기화합니다
                        cList.clear();
                        // 블루투스가 가능한 상태인지 체크합니다
                        startCheckBluetooth();

                        // ACTION_FOUND가 호출될 때 마다 데이터를 전송하는 역할을 합니다
                        foundFilter.addAction(BluetoothDevice.ACTION_FOUND);
                        // 디바이스와 페어링 작업이 요청 될 때 마다 호출됩니다
                        pairingFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        // 실제 리시버에 등록합니다
                        registerReceiver(mReceiver, foundFilter);
                        registerReceiver(pReceiver, pairingFilter);
                    }
                });
            }
        }, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG_LIFECYCLE, "On Destroy");
        // 액티비티의 LifeCycle이 종료 될 때 어댑터를 초기화 하고
        // 등록된 리시버를 해제합니다
        // 디바이스 스캔 작업도 취소합니다
        if (bluetoothAdapter.isDiscovering()) {
            unregisterReceiver(mReceiver);
            unregisterReceiver(pReceiver);
            bluetoothAdapter.cancelDiscovery();
        }
        cList.clear();
        pList.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG_LIFECYCLE, "On Pause");
        // 디바이스 스캔이 예기치 못하게 중지되었을 경우 리스트를 새로고침 할 수 있는 이미지를 보여줍니다
        binding.connRefreshIv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            outerClass.FullScreenMode(context);
            Log.d(TAG_LIFECYCLE, "On WindowFocusChanged");
            // 액티비티의 뷰가 완성되었을 때 페어링 된 디바이스를 모두 불러옵니다
            findPairedDevice();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ConnectBluetoothActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        Log.d(TAG_LIFECYCLE, "On Create");

        context = ConnectDeviceActivity.this;
        cAdapter = new ConnectRecyclerAdapter(cList);
        pAdapter = new PairedDeviceAdapter(pList);
        binding.connConnectableRv.setAdapter(cAdapter);
        binding.connPairedDeviceRv.setAdapter(pAdapter);
        bluetoothThread = new BluetoothThread(this);
        binding.loadingParingPb.setVisibility(View.GONE);
        binding.connOkTv.setEnabled(false);
        binding.connOkTv.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_b, null));
        binding.connOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        binding.connRefreshIv.setVisibility(View.GONE);

        // 블루투스 어댑터를 초기화합니다
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 연결 가능한 디바이스의 리스트를 클릭했을 경우의 이벤트 리스너
        cAdapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // https://ghj1001020.tistory.com/291
                // ProgressView를 보여주고 레이아웃의 명암을 낮춥니다
                binding.loadingParingPb.setVisibility(View.VISIBLE);
                binding.connMainLayout.setAlpha(0.3f);

                // 선택한 아이템의 디바이스를 페어링하는 핸들러입니다
                Handler ParingDeviceHandler = new Handler(Looper.getMainLooper());
                ParingDeviceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (position < cAdapter.getItemCount()) {
                            try {
                                // 선택한 디바이스 페어링 요청
                                noPairingPosition = position;
                                BluetoothDevice device = noBondedList.get(position);
                                // 페어링 다이얼로그 호출
                                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                                method.invoke(device, (Object[]) null);
                            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                                e.printStackTrace();
                                Toast.makeText(context, getString(R.string.already_connected), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, getString(R.string.retry_please), Toast.LENGTH_SHORT).show();
                            binding.loadingParingPb.setVisibility(View.GONE);
                            binding.connMainLayout.setAlpha(1f);
                            onResume();
                        }
                    }
                }, 1000);

            }
        });

        // 확인 버튼 클릭시 이벤트 리스너입니다
        binding.connOkTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.connOkTv.isEnabled()) {
                    outerClass.CallVibrate(context, 10);
                    binding.loadingParingPb.setVisibility(View.VISIBLE);
                    binding.connMainLayout.setAlpha(0.3f);
                    // 대쉬보드 화면으로 이동합니다
                    Intent intent = new Intent(context, DashBoardActivity.class);
                    // 해당 클릭아이템의 포지션을 인텐트와 함께 전송합니다
                    intent.putExtra("device_position", SELECTED_POSITION);
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                    finish();
                }
            }
        });

        // 페어링 된 리스트 아이템 클릭 시 이벤트 리스너
        pAdapter.setOnItemClickListener(new PairedDeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // 포지션에 따라 아이템의 명암과 색상을 변경합니다
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

        // 페어링 된 아이템을 길게 클릭 했을 경우 페어링을 취소하는 이벤트리스너
        pAdapter.setOnItemLongClickListener(new PairedDeviceAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View v, int position) {
                // 페어링 된 아이템을 모두 불러옵니다
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice bt : pairedDevices) {
                    if (bt.getName().contains(pList.get(position).getName())) {
                        // 페어링 취소 여부를 확인하는 다이얼로그
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.unpairing_title));
                        builder.setMessage(getString(R.string.unpairing_msg));
                        builder.setPositiveButton(getString(R.string.unpairing_ok_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    // 확인 클릭 시 다이얼로그를 종료
                                    dialog.dismiss();
                                    deviceNameStrLeft = bt.getName().split(" ");
                                    // 페어링 취소 메서드 호출
                                    Method m = bt.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(bt, (Object[]) null);
                                    onResume();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton(getString(R.string.unpairing_cancel_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.show();
                    }
                }
            }
        });

        // 뒤로가기 버튼을 클릭하면 언어설정 스킵을 no로 변경하고 이동합니다
        binding.connTopBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerClass.CallVibrate(context, 10);
                SharedPreferenceManager.setString(context, "skip_lang", "no");
                Intent intent = new Intent(context, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 리스트 새로고침 클릭 시 이벤트리스너
        binding.connRefreshIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerClass.CallVibrate(context, 10);
                binding.connRefreshIv.setVisibility(View.GONE);
                onResume();
            }
        });
    }

    // 연결 가능한 아이템 추가
    public void addCItem(Drawable img, String name, String address) {
        ConnectRecyclerItem item = new ConnectRecyclerItem(img, name, address);

        item.setDevice_img(img);
        item.setDevice_name(name);
        item.setDevice_address(address);

        cList.add(item);
    }

    // 페어링 된 디바이스 아이템 추가
    public void addPItem(Drawable icon, String name, String address) {
        PairedDeviceItem item = new PairedDeviceItem(icon, name, address);

        item.setIcon(icon);
        item.setName(name);
        item.setAddress(address);

        pList.add(item);
    }

    // 페어링 된 디바이스 불러오기
    public void findPairedDevice() {
        pList.clear();
        bondedList.clear();
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        if (!pairedDevice.isEmpty()) {
            for (BluetoothDevice device : pairedDevice) {
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

    private final BroadcastReceiver pReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 디바이스 페어링 리시버
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                binding.loadingParingPb.setVisibility(View.VISIBLE);
                binding.connMainLayout.setAlpha(0.3f);
                try {
                    binding.connPairedDeviceRv.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.loadingParingPb.setVisibility(View.GONE);
                            binding.connMainLayout.setAlpha(1f);
                            findPairedDevice();
                        }
                    }, 1000);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    onResume();
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { //각각의 디바이스로부터 정보를 받으려면 만들어야함
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 사용 가능한 디바이스 불러오는 브로드캐스트 리시버
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

//                    deviceNameStrRight = deviceName.split(" ");
                // 필터링 없이 하려면 주석 해제 + 밑에 필터링부분 주석처리
//                    notPairedDeviceList.add(device);
//                    addCItem(filteringImage(deviceNameStr[0],
//                                deviceNameStr[0],
//                                deviceNameStr[1]);
//                    cAdapter.notifyDataSetChanged();

                // 디바이스 이름으로 필터링
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        String deviceName = device.getName();
                        if (deviceName != null && (deviceName.contains("BS_"))) {
                            if (deviceName.contains(" ")) {
                                deviceNameStrRight = deviceName.split(" ");
                                for (int i = 0; i < pList.size(); i++) {
                                    Log.d(TAG_LIFECYCLE, deviceNameStrRight[0] + deviceNameStrRight[1] + " : " + pList.get(i).getAddress());
                                    if (!deviceNameStrRight[1].equals(pList.get(i).getAddress())) {
                                        if (i == pList.size() - 1) {
                                            noBondedList.add(device);
                                            addCItem(filteringImage(deviceNameStrRight[0]),
                                                    deviceNameStrRight[0],
                                                    deviceNameStrRight[1]);
                                            cAdapter.notifyDataSetChanged();
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }, 1000);
            }
        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.connTopLangIconIv):
            case (R.id.connTopLangTitleTv):
            case (R.id.connTopLangContentTv):
                SharedPreferenceManager.setString(context, "skip_lang", "no");
                Intent intent = new Intent(context, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    // 장치 종류 별 이미지 설정
    private Drawable filteringImage(String s) {
        if (s.contains("BS_M")) {
            return ResourcesCompat.getDrawable(getResources(), R.drawable.mini_icon, null);
        } else if (s.contains("BS_100")) {
            return ResourcesCompat.getDrawable(getResources(), R.drawable.bs_100_icon, null);
        } else {
            return null;
        }
    }

    // 블루투스 연결 및 사용가능 여부 확인
    @SuppressLint("MissingPermission")
    public void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.no_bluetooth_device), Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {

                    if (!bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.startDiscovery();
                    }
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