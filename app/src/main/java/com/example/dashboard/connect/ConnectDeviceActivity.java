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

    BluetoothThread bluetoothThread;

    String[] deviceNameStrLeft, deviceNameStrRight;
    int noPairingPosition = 0;

    IntentFilter foundFilter = new IntentFilter();

    OuterClass outerClass = new OuterClass();

    ArrayList<BluetoothDevice> noBondedList;
    ArrayList<BluetoothDevice> bondedList;

    @Override
    protected void onResume() {
        super.onResume();
        outerClass.FullScreenMode(context);
        Log.d(TAG_LIFECYCLE, "On Resume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG_LIFECYCLE, "On Destroy");
        // 액티비티의 LifeCycle이 종료 될 때 어댑터를 초기화 하고
        // 등록된 리시버를 해제합니다
        // 디바이스 스캔 작업도 취소합니다
        if (bluetoothAdapter.isDiscovering()) {
            unregisterReceiver(connectReceiver);
            bluetoothAdapter.cancelDiscovery();
        }
        cList.clear();
        noPairingPosition = 0;
        pList.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG_LIFECYCLE, "On Pause");
        // 디바이스 스캔이 예기치 못하게 중지되었을 경우 리스트를 새로고침 할 수 있는 이미지를 보여줍니다
        ShowRefresh();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            outerClass.FullScreenMode(context);
            Log.d(TAG_LIFECYCLE, "On WindowFocusChanged");
            startCheckBluetooth();
            getConnectableDeviceList();
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
        bluetoothThread = new BluetoothThread(context);
        binding.loadingParingPb.setVisibility(View.GONE);
        binding.connOkTv.setEnabled(false);
        binding.connOkTv.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_b, null));
        binding.connOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.statusUnitText, null));
        noBondedList = new ArrayList<>();
        bondedList = new ArrayList<>();

        // 블루투스 어댑터를 초기화합니다
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        HideRefresh();

        Intent intent = getIntent();
        if (intent.getExtras().getString("dialog").equals("yes")) {
            DisconnectBTDialog();
        }

        // 연결 가능한 디바이스의 리스트를 클릭했을 경우의 이벤트 리스너
        cAdapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // https://ghj1001020.tistory.com/291
                // ProgressView를 보여주고 레이아웃의 명암을 낮춥니다
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        visibleProgress();
                    }
                });

                // 선택한 아이템의 디바이스를 페어링하는 핸들러입니다
                if (position < cAdapter.getItemCount()) {

                    // 선택한 디바이스 페어링 요청
                    BluetoothDevice device = noBondedList.get(position);

                    // 페어링 다이얼로그 호출
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            device.createBond();
                        }
                    }).start();

                } else {
                    Toast.makeText(context, getString(R.string.retry_please), Toast.LENGTH_SHORT).show();
                    getConnectableDeviceList();
                }
            }
        });

        // 확인 버튼 클릭시 이벤트 리스너입니다
        binding.connOkTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.connOkTv.isEnabled()) {
                    outerClass.CallVibrate(context, 10);
                    visibleProgress();
                    // 대쉬보드 화면으로 이동합니다
                    Intent intent = new Intent(context, DashBoardActivity.class);
                    // 해당 클릭아이템의 포지션을 인텐트와 함께 전송합니다
                    intent.putExtra("device_position", SELECTED_POSITION);
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
                    SetOkBtnEnable();
                } else if (SELECTED_POSITION == position) {
                    // 이미 선택 된 아이템을 다시 클릭 했을 경우
                    // 아무 작업도 진행하지 않습니다
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
                    if (bt.getName().equals(pList.get(position).getName() + " " + pList.get(position).getAddress())) {

                        // 페어링 취소 여부를 확인하는 다이얼로그
                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(getString(R.string.unpairing_title));
                        builder.setMessage(getString(R.string.unpairing_msg));
                        builder.setPositiveButton(getString(R.string.unpairing_ok_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    deviceNameStrLeft = bt.getName().split(" ");

                                    // 페어링 취소 메서드 호출
                                    Method m = bt.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(bt, (Object[]) null);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 확인 클릭 시 다이얼로그를 종료
                                            dialog.dismiss();
                                        }
                                    });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton(getString(R.string.unpairing_cancel_btn), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                getConnectableDeviceList();
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
                outerClass.GoToLanguageByConnect(context);
            }
        });

        // 리스트 새로고침 클릭 시 이벤트리스너
        binding.connRefreshIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outerClass.CallVibrate(context, 10);
                HideRefresh();
                getConnectableDeviceList();
            }
        });
    }

    private void ShowRefresh() {
        binding.connRefreshIv.setVisibility(View.VISIBLE);
    }

    private void HideRefresh() {
        binding.connRefreshIv.setVisibility(View.GONE);
    }

    // 연결 가능한 아이템 추가
    private void addCItem(Drawable img, String name, String address) {
        ConnectRecyclerItem item = new ConnectRecyclerItem(img, name, address);

        item.setDevice_img(img);
        item.setDevice_name(name);
        item.setDevice_address(address);

        cList.add(noPairingPosition, item);
    }

    // 페어링 된 디바이스 아이템 추가
    private void addPItem(Drawable icon, String name, String address) {
        PairedDeviceItem item = new PairedDeviceItem(icon, name, address);

        item.setIcon(icon);
        item.setName(name);
        item.setAddress(address);

        pList.add(item);
    }

    // 페어링 된 디바이스 불러오기
    private void findPairedDevice() {

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

    // 브로드캐스트 리시버
    private final BroadcastReceiver connectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                // 디바이스가 페어링 되어있지 않다면
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 이름이 NULL이 아니라면
                    if (deviceName != null) {
                        Log.d("paringReceiver", noPairingPosition + " : " + deviceName);
                        if (filterAlreadyAddList(deviceName)) {
                            // 디바이스 이름으로 필터링
                            // 이름이 BS_ 로 시작한다면
                            if (deviceName.contains("BS_")) {

                                // S/N가 존재한다면
                                if (deviceName.contains(" ")) {

                                    // 공백으로 구분
                                    deviceNameStrRight = deviceName.split(" ");

                                    // 페어링 되지 않은 디바이스 리스트에 저장
                                    noBondedList.add(noPairingPosition, device);

                                    // 기기명과 S/N로 구분
                                    addCItem(filteringImage(deviceNameStrRight[0]),
                                            deviceNameStrRight[0],
                                            deviceNameStrRight[1]);
                                    noPairingPosition++;
                                    cAdapter.notifyDataSetChanged();
                                    Log.d("paringReceiver", "ADD THIS!");
                                }
                            }
                        }

                    }
                }
            }

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    goneProgress();
                                    getConnectableDeviceList();
                                }
                            }, 1000);
                        }
                    });
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    getConnectableDeviceList();
                }
            }
        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.connTopLangIconIv):
            case (R.id.connTopLangTitleTv):
            case (R.id.connTopLangContentTv):
                SharedPreferenceManager.setString(context, "skip_lang", "no");
                outerClass.GoToLanguageByConnect(context);
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

    // 이미 존재하는 디바이스인지 필터링
    private boolean filterAlreadyAddList(String name) {
        for (int i = 0; i < noBondedList.size(); i++) {
            if (name.equals(noBondedList.get(i).getName())) {
                Log.d("paringReceiver", "Already Exist!");
                return false;
            }
        }
        return true;
    }

    private void visibleProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.loadingParingPb.setVisibility(View.VISIBLE);
                binding.connMainLayout.setAlpha(0.3f);
                binding.mainLayout.setEnabled(false);
            }
        });
    }

    private void goneProgress() {
        binding.loadingParingPb.setVisibility(View.GONE);
        binding.connMainLayout.setAlpha(1f);
        binding.mainLayout.setEnabled(true);
    }

    private void SetOkBtnEnable() {
        binding.connOkTv.setEnabled(true);
        binding.connOkTv.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.lang_ok_w, null));
        binding.connOkTv.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
    }

    private void getConnectableDeviceList() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (binding.loadingParingPb.getVisibility() == View.VISIBLE)
                    goneProgress();

                noBondedList.clear();
                cList.clear();
                noPairingPosition = 0;

                // ACTION_FOUND가 호출될 때 마다 데이터를 전송하는 역할을 합니다
                foundFilter.addAction(BluetoothDevice.ACTION_FOUND);
                // 디바이스와 페어링 작업이 요청 될 때 마다 호출됩니다
                foundFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                // 실제 리시버에 등록합니다
                registerReceiver(connectReceiver, foundFilter);

                Handler handler1 = new Handler(Looper.getMainLooper());
                handler1.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findPairedDevice();
                    }
                }, 1000);
            }
        });
    }

    // 블루투스 연결 끊김 시 디바이스 연결 액티비티로 전환
    private void DisconnectBTDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 장치와의 연결이 끊어짐을 알리고 디바이스 연결 화면으로 돌아갑니다
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.create();
                builder.setTitle(getString(R.string.caution_title));
                builder.setMessage(getString(R.string.caution_message));
                builder.setPositiveButton(getString(R.string.caution_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                if (!context.isDestroyed())
                    builder.show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
                ConnectDeviceActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton(getString(R.string.exit_app_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if (!context.isDestroyed())
        builder.show();
    }

    // 블루투스 연결 및 사용가능 여부 확인
    @SuppressLint("MissingPermission")
    private void startCheckBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.no_bluetooth_device), Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                try {
                    if (!bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.startDiscovery();
                    }
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