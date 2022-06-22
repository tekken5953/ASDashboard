package com.example.dashboard;

import static com.example.dashboard.BluetoothAPI.analyzedControlBody;
import static com.example.iaq.Bluetooth.BluetoothAPI.REQUEST_CONTROL;
import static com.example.iaq.Bluetooth.BluetoothAPI.REQUEST_INDIVIDUAL_STATE;
import static com.example.iaq.Bluetooth.BluetoothAPI.analyzedControlBody;
import static com.example.iaq.Bluetooth.BluetoothAPI.analyzedRequestBody;
import static com.example.iaq.Bluetooth.BluetoothAPI.combineArray;
import static com.example.iaq.Bluetooth.BluetoothAPI.doubleToByteArray;
import static com.example.iaq.Bluetooth.BluetoothAPI.generateTag;
import static com.example.iaq.Bluetooth.BluetoothAPI.hexStringToByteArray;
import static com.example.iaq.Bluetooth.BluetoothAPI.makeFrame;
import static com.example.iaq.Bluetooth.BluetoothAPI.separatedFrame;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_FORCED_FAN;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_INTERVAL;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_OFF_TIME;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_ON_TIME;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_OPERATION;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_POWER;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.CONTROL_REBOOT;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.GET_CONTROL;
import static com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment.GET_STATE;
import static com.example.iaq.Fragment.Settings.SettingsFragment.PREFERENCE_BT;
import static com.example.iaq.Fragment.Settings.SettingsFragment.PREFERENCE_BT_INTERVAL;
import static com.example.iaq.MainActivity.FRAGMENT_DEVICE_DETAIL;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaq.Fragment.CustomFragment;
import com.example.iaq.Fragment.Device.DeviceControl_Bluetooth_Fragment;
import com.example.iaq.Fragment.Device.DeviceFragment;
import com.example.iaq.GPS.GpsTracker;
import com.example.iaq.HTTP.HttpAPI;
import com.example.iaq.MainActivity;
import com.example.iaq.Mqtt.Mqtt;
import com.example.iaq.R;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class BluetoothFragment extends CustomFragment {
  public final static String KEY_BLUETOOTH_CONNECT = "KEY_BLUETOOTH_CONNECT";
  public final static String TAG_BLUETOOTH_PAIRING = "TAG_BLUETOOTH_PAIRING_LIST";
  public final static String TAG_BLUETOOTH_DEVICE_FRAGMENT = "TAG_BLUETOOTH_DEVICE_FRAGMENT";

  private final int BT_CONTAINER = R.id.fl_bt_container;
  private final int DEFAULT_INTERVAL = 5;
  public final static String[] sensorValid_id = {
      "01", "0F", "1A", "1D", "23", "20", "17", "14", "29", "26", "2C", "2F", "32"
  };
  public final static String[] sensorValue_id = {
      // PM                          CO    CO2   O3   TVOC  CH2O   H2    H2S   NH3   CH4  C3H8   NO2
      "02", "04", "06", "09", "0C", "1B", "1E", "24", "21", "18", "15", "2A", "27", "2D", "30", "33"
  };
  public final static String[] sensorState_id = {
      // PM                          CO    CO2   O3   TVOC  CH2O   H2    H2S   NH3   CH4  C3H8   NO2
      "03", "05", "07", "0A", "0D", "1C", "1F", "25", "22", "19", "16", "2B", "28", "2E", "31", "34"
  };

  private BluetoothThread bluetoothThread;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothDevice[] pairedDevices;
  private Set<BluetoothDevice> bluetoothDevices;

  ArrayList<String> valueList;
  ArrayList<String> validList;

  ListView deviceListView;
  ArrayAdapter<String> listAdapter;
  ArrayList<String> listItem;

  DrawerLayout drawerLayout;
  ImageView iv_notFoundDevice;

  Timer data_scheduler, recvData_scheduler, screen_scheduler;
  BluetoothThread.DataShareViewModel viewModel;

  SharedPreferences preferences;
  int time_interval;
  boolean isBind, isInit;

  String serialNumber;
  int device_type;
  HttpAPI httpAPI;
  Mqtt mqtt;

  public BluetoothFragment() {
    super();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    preferences = requireActivity().getSharedPreferences(PREFERENCE_BT, Context.MODE_PRIVATE);
    time_interval = preferences.getInt(PREFERENCE_BT_INTERVAL, DEFAULT_INTERVAL);

    isBind = false; isInit = false;
    serialNumber = null;
    device_type = DeviceFragment.DEVICE_TYPE_ERROR;

    mqtt = null;
  }
  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_bluetooth, container, false);
  }
  @Override
  public void onResume() {
    super.onResume();

    if(!isBind) isBind = bindActivity();
    if(!isInit) isInit = init();

    //startMini();
    searchPairedDevices();
  }

  @Override
  public void onDestroyView() {
    try {
      data_scheduler.cancel();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }

    Fragment fragment = requireActivity().getSupportFragmentManager().findFragmentByTag(FRAGMENT_DEVICE_DETAIL);
    if(fragment != null) {
      FragmentTransaction fragmentTransaction = requireActivity().getSupportFragmentManager().beginTransaction();
      fragmentTransaction.remove(fragment);
      fragmentTransaction.commitAllowingStateLoss();
    }
    if(bluetoothThread.isRunning()) {
      bluetoothThread.setRunning(false);
      bluetoothThread.interrupt();
    }
    if(bluetoothThread.isConnected()) {
      bluetoothThread.closeSocket();
    }

    if(mqtt != null) {
      if(mqtt.isConnected()) {
        mqtt.disconnect();
      }
      mqtt.interrupt();
    }

    super.onDestroyView();
  }

  private boolean bindActivity() {
    drawerLayout = requireActivity().findViewById(R.id.dl_bt_device_list);
    deviceListView = requireActivity().findViewById(R.id.lv_bt_device_list);
    iv_notFoundDevice = requireActivity().findViewById(R.id.iv_notFoundDevice);

    return true;
  }
  private boolean init() {
    bluetoothThread = new BluetoothThread(this);
    bluetoothThread.setListContext(requireActivity());
    viewModel = new ViewModelProvider(this).get(BluetoothThread.DataShareViewModel.class);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    listItem = new ArrayList<String>();
    listAdapter = new ArrayAdapter<String>(
        requireActivity().getApplicationContext(),
        android.R.layout.simple_list_item_1,
        listItem
    );
    deviceListView.setAdapter(listAdapter);

    if(!bluetoothThread.isConnected()) {
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
      drawerLayout.openDrawer(GravityCompat.START);
    }

    // 반복 타임 태스크
    //data_scheduler = new Timer();
    recvData_scheduler = new Timer();
    screen_scheduler = new Timer();

    valueList = new ArrayList<>();
    validList = new ArrayList<>();



    Observer<String> data = new Observer<String>() {
      @Override
      public void onChanged(String s) {
        byte[] recvHex = hexStringToByteArray(s);
        if(recvHex == null) return;

        byte[][] bundleOfHex = separatedFrame(recvHex);
            /*
            String stx          = byteArrayToHexString(bundleOfHex[0]);
            String length       = byteArrayToHexString(bundleOfHex[1]);
            String sequence     = byteArrayToHexString(bundleOfHex[2]);
            String command      = byteArrayToHexString(bundleOfHex[3]);
            String body         = byteArrayToHexString(bundleOfHex[4]);
            String etx          = byteArrayToHexString(bundleOfHex[5]);
            */
        String command = BluetoothAPI.byteArrayToHexString(bundleOfHex[3]);
        Bundle body = null;

        switch (command) {
          case "81": case "83":
            body = analyzedRequestBody(bundleOfHex[4]);
            break;
          case "82":
            body = analyzedControlBody(bundleOfHex[4]);
            break;
        }

        if(body == null) return;
        System.out.println("body is " + body);

        if(command.equals("82")) {
          processControlBody(body);
        } else {
          processRequestBody(body);
        }
      }
    };
    viewModel.getReceiveData().observe(this, data);



    deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(position == listItem.size()-1) {
          // 페어링 도우미
          FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
          FragmentTransaction transaction = fragmentManager.beginTransaction();

          Fragment findFragment = fragmentManager.findFragmentByTag(TAG_BLUETOOTH_PAIRING);
          if(findFragment != null) {
            transaction.remove(findFragment);
          }

          BluetoothPairingFragment fragment = new BluetoothPairingFragment();
          transaction.add(R.id.fl_main_container, fragment, TAG_BLUETOOTH_PAIRING);
          transaction.addToBackStack(TAG_BLUETOOTH_PAIRING);
          transaction.commit();
        } else{
          // 블루투스 연결 시작
          progressOn(getResources().getString(R.string.progress_loading));
          connectBluetoothDevice(position);

          if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START);
          }
        }
      }
    });

    bluetoothThread.setDisconnectedSocketEventListener(new BluetoothThread.disConnectedSocketEventListener() {
      @Override
      public void onDisconnectedEvent() {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progressOff();
            if(!drawerLayout.isDrawerOpen(GravityCompat.START)){
              drawerLayout.openDrawer(GravityCompat.START);
            }

            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                try {
                  FragmentManager manager = requireActivity().getSupportFragmentManager();
                  Fragment fragment = manager.findFragmentByTag(TAG_BLUETOOTH_DEVICE_FRAGMENT);

                  if(fragment != null) {
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.remove(fragment);
                    transaction.commit();
                  }
                } catch (IllegalStateException | NullPointerException e) {
                  e.printStackTrace();
                }


                Toast.makeText(requireActivity(), "블루투스 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show();
              }
            });
          }
        });
      }
    });
    bluetoothThread.setConnectedSocketEventListener(new BluetoothThread.connectedSocketEventListener() {
      @Override
      public void onConnectedEvent() {
        // 센서 장착 여부 및 GPS 정보 요청
        bluetoothThread.writeHex(makeFrame(
            new byte[]{REQUEST_INDIVIDUAL_STATE},
            new byte[]{
                0x35, 0x00, 0x00, // 센서연결확인
                //0x43, 0x00, 0x00, // GPS 위도
                //0x44, 0x00, 0x00, // GPS 경도
                //0x45, 0x00, 0x00, // 펌웨어버전
                //0x46, 0x00, 0x00, // 모듈설치날짜
                0x47, 0x00, 0x00  // 모델명
            },
            bluetoothThread.getSequence()
        ));
        //loopReceiveData(time_interval);

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progressOff();
          }
        });
      }
    });

    regParentListener();

    return true;
  }

  private void processRequestBody(Bundle body) {
    if(body.containsKey("47")) {
      device_type = BluetoothAPI.getDeviceType(body.getCharArray("47"));
      System.out.println("Device Type : " + Arrays.toString(body.getCharArray("47")));

      if(device_type == DeviceFragment.DEVICE_TYPE_ERROR) {
        Toast.makeText(requireActivity(), "Error BT TAG 47", Toast.LENGTH_SHORT).show();
        return;
      }

      if(device_type == DeviceFragment.DEVICE_TYPE_MINI) {
        // Wifi State 확인
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{
                    0x48, 0x00, 0x00, // S/N
                    0x65, 0x00, 0x00  // WIFI Connect State
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
                0x46, 0x00, 0x00  // 모듈설치날짜
            },
            bluetoothThread.getSequence()
        ));
      }

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          setDetailFragment(device_type, body);
        }
      });
    }
    if(body.containsKey("48")) {
      serialNumber = new String(body.getCharArray("48"));
    }



    getParentFragmentManager().setFragmentResult(DeviceFragment.PROTOCOL_BLUETOOTH, body);

    // 임시
    if(body.containsKey("52") || body.containsKey("55") || body.containsKey("56")) {
      getParentFragmentManager().setFragmentResult(GET_CONTROL, body);
    }

    if(body.containsKey("46")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_SETUP_DATE, body);
    }
    if(body.containsKey("48")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_SERIAL_NUMBER, body);
    }
    if(body.containsKey("49")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_DEVICE_PORT, body);
    }
    if(body.containsKey("57")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_DATA_INTERVAL, body);
    }
    if(body.containsKey("58") && body.containsKey("59") && body.containsKey("5A") && body.containsKey("5B")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_BATTERY_REMAINED, body);
    }
    if(body.containsKey("65")) {
      getParentFragmentManager().setFragmentResult(DeviceFragment.GET_WIFI_STATE, body);

//      if(body.getByte("65") == 0x00) {
//
//        if(serialNumber != null) {
//          try {
//            if(!mqtt.isConnected()) {
//              mqtt.interrupt();
//            }
//          } catch (NullPointerException e) {
//            e.printStackTrace();
//          }
//          mqtt = new Mqtt(requireActivity(), serialNumber);
//          mqtt.connect();
//        }
//
//      }
    }
    if(body.containsKey("69")) {
      getParentFragmentManager().setFragmentResult(DeviceControl_Bluetooth_Fragment.RESPONSE_SERVER_IP, body);
    }

    // FF 데이터 처리
    if(body.containsKey("FF")) {
      //getParentFragmentManager().setFragmentResult(DeviceFragment.DATA_STACK, body);

      mqtt.publish_measured(DeviceFragment.bundleToJson(body));
    }

  }
  private void processControlBody(Bundle body) {
    // 임시
    if(body.containsKey("50") || body.containsKey("51")) {
      Toast.makeText(requireActivity(), "BS-M이 종료됐습니다.", Toast.LENGTH_SHORT).show();

      FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      Fragment fragment = fragmentManager.findFragmentByTag(TAG_BLUETOOTH_DEVICE_FRAGMENT);

      if(fragment != null) {
        transaction.remove(fragment);
        transaction.commitAllowingStateLoss();

        if(bluetoothThread.isConnected()) {
          bluetoothThread.closeSocket();
        }

        if(!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START);
      }
    } else if(body.containsKey("46")) {
      if(body.getByte("46") == 0x01) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{0x46, 0x00, 0x00},
                bluetoothThread.getSequence()
            )
        );
        Toast.makeText(requireActivity(), "설치날짜 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(requireActivity(), "설치날짜 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
      }
    } else if(body.containsKey("48")) {
      if(body.getByte("48") == 0x01) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{0x48, 0x00, 0x00},
                bluetoothThread.getSequence()
            )
        );
        Toast.makeText(requireActivity(), "S/N 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(requireActivity(), "S/N 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
      }
    } else if(body.containsKey("49")) {
      if(body.getByte("49") == 0x01) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{0x49, 0x00, 0x00},
                bluetoothThread.getSequence()
            )
        );
        Toast.makeText(requireActivity(), "포트 설정 정보 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(requireActivity(), "포트 설정 정보 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
      }
    } else if(body.containsKey("57")) {
      if(body.getByte("57") == 0x01) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{0x57, 0x00, 0x00},
                bluetoothThread.getSequence()
            )
        );
        Toast.makeText(requireActivity(), "데이터 전송 간격 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(requireActivity(), "데이터 전송 간격 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
      }
    } else if(body.containsKey("69")) {
      if(body.getByte("69") == 0x01) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{0x69, 0x00, 0x00},
                bluetoothThread.getSequence()
            )
        );
        Toast.makeText(requireActivity(), "Server IP 변경에 성공했습니다.", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(requireActivity(), "Server IP 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
      }
    } else {
      Iterator<String> iterator = body.keySet().iterator();
      try {
        while(iterator.hasNext()) {
          String key = (String) iterator.next();
          byte result = body.getByte(key);

          if(result == (byte) 0x01) {
            Toast.makeText(requireActivity(), "성공적으로 변경했습니다.", Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(requireActivity(), "변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
          }
        }
      } catch (NullPointerException | ClassCastException e) {
        e.printStackTrace();
      }
    }
    //getParentFragmentManager().setFragmentResult(GET_CONTROL_RESULT, body);
  }
  private void regParentListener() {
    getParentFragmentManager().setFragmentResultListener(PREFERENCE_BT_INTERVAL, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        try {
          data_scheduler.cancel();
        } catch (NullPointerException | IllegalStateException e) {
          e.printStackTrace();
        }

        int interval = result.getInt(PREFERENCE_BT_INTERVAL);
        if(interval == -1) {
          //interval = preferences.getInt(PREFERENCE_BT_INTERVAL, DEFAULT_INTERVAL);
          interval = time_interval;
        } else if(interval == 0) {
          interval = DEFAULT_INTERVAL;
        }
        loopReceiveData(interval);
      }
    });


    // 임시
    // 제어 리스너
    getParentFragmentManager().setFragmentResultListener(CONTROL_POWER, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte power = result.getByte(CONTROL_POWER);
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x50, new byte[]{power}),
                        bluetoothThread.getSequence()
                )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_REBOOT, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte power = result.getByte(CONTROL_REBOOT);
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x51, new byte[]{power}),
                        bluetoothThread.getSequence()
                )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_OPERATION, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte control = result.getByte(CONTROL_OPERATION);
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_CONTROL},
                BluetoothAPI.generateTag((byte) 0x52, new byte[]{control}),
                bluetoothThread.getSequence()
            )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_ON_TIME, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        short onTime = result.getShort(CONTROL_ON_TIME);
        byte[] byteArrayOnTime = ByteBuffer.allocate(2).putShort(onTime).array();
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x55, byteArrayOnTime),
                        bluetoothThread.getSequence()
                )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_OFF_TIME, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        short offTime = result.getShort(CONTROL_OFF_TIME);
        byte[] byteArrayOffTime = ByteBuffer.allocate(2).putShort(offTime).array();
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x56, byteArrayOffTime),
                        bluetoothThread.getSequence()
                )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_INTERVAL, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        short transInterval = result.getShort(CONTROL_INTERVAL);
        byte[] byteArrayTransInterval = ByteBuffer.allocate(2).putShort(transInterval).array();
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x57, byteArrayTransInterval),
                        bluetoothThread.getSequence()
                )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(CONTROL_FORCED_FAN, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte forcedFan = result.getByte(CONTROL_FORCED_FAN);
        bluetoothThread.writeHex(
                makeFrame(
                        new byte[]{REQUEST_CONTROL},
                        BluetoothAPI.generateTag((byte) 0x3B, new byte[]{forcedFan}),
                        bluetoothThread.getSequence()
                )
        );
      }
    });

    getParentFragmentManager().setFragmentResultListener(GET_STATE, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] frame = null;
        if(device_type == DeviceFragment.DEVICE_TYPE_MINI) {
          frame = makeFrame(
              new byte[]{REQUEST_INDIVIDUAL_STATE},
              new byte[]{
                  0x57, 0x00, 0x00  // 데이터 간격
              },
              bluetoothThread.getSequence()
          );
        } else {
          frame = makeFrame(
              new byte[]{REQUEST_INDIVIDUAL_STATE},
              new byte[]{
                  0x52, 0x00, 0x00, // 전원
                  0x55, 0x00, 0x00, // 온타임
                  0x56, 0x00, 0x00, // 오프타임
                  0x57, 0x00, 0x00, // 데이터 간격
                  0x3B, 0x00, 0x00
              },
              bluetoothThread.getSequence()
            );
        }
        bluetoothThread.writeHex(frame);
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.GET_MINI_STATE, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        bluetoothThread.writeHex(
            makeFrame(
                new byte[]{REQUEST_INDIVIDUAL_STATE},
                new byte[]{
                    0x46, 0x00, 0x00, // 설치날짜
                    0x48, 0x00, 0x00, // Serial Number
                    0x49, 0x00, 0x00, // Port Info
                    0x57, 0x00, 0x00, // 데이터 간격
                    0x69, 0x00, 0x00  // Broker IP
                },
                bluetoothThread.getSequence()
            )
        );
      }
    });
    getParentFragmentManager().setFragmentResultListener(KEY_BLUETOOTH_CONNECT, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        try {
          //connectBluetoothDevice(result.getInt(KEY_BLUETOOTH_CONNECT));
          //connectBluetoothDevice((BluetoothDevice) result.getParcelable(BluetoothFragment.KEY_BLUETOOTH_CONNECT));
          onResume();
        } catch (NullPointerException e) {
          e.printStackTrace();
        }
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_FAN, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte byFanSpeed = result.getByte(DeviceControl_Bluetooth_Fragment.CONTROL_FAN_VALUE);
        byte[] bytes = makeFrame(
                new byte[]{REQUEST_CONTROL},
                BluetoothAPI.generateTag((byte) 0x3A, new byte[]{byFanSpeed}),
                bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(
                bytes
        );
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_WIFI_STATE, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] ssid = result.getString("ssid").getBytes(StandardCharsets.UTF_8);
        byte[] pass = result.getString("pass").getBytes(StandardCharsets.UTF_8);

        if(ssid == null || pass == null) return;
        byte[] bytesTagWifiInfo = BluetoothAPI.combineArray(
            BluetoothAPI.generateTag((byte) 0x66, BluetoothAPI.changeByteOrder(ssid, true)),
            BluetoothAPI.generateTag((byte) 0x67, BluetoothAPI.changeByteOrder(pass, true))
        );
        byte[] bytes = makeFrame(
                new byte[]{REQUEST_CONTROL},
                bytesTagWifiInfo,
                bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_SETUP_DATE, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] bySetupDate = result.getByteArray(DeviceControl_Bluetooth_Fragment.CONTROL_SETUP_DATE);

        if(bySetupDate == null) return;
        byte[] bytes = makeFrame(
            new byte[]{REQUEST_CONTROL},
            BluetoothAPI.generateTag((byte) 0x46, BluetoothAPI.changeByteOrder(bySetupDate, false)),
            bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_SERIAL_NUMBER, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] bySerialNumber = result.getByteArray(DeviceControl_Bluetooth_Fragment.CONTROL_SERIAL_NUMBER);

        if(bySerialNumber == null) return;
        byte[] bytes = makeFrame(
            new byte[]{REQUEST_CONTROL},
            BluetoothAPI.generateTag((byte) 0x48, BluetoothAPI.changeByteOrder(bySerialNumber, true)),
            bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_SERVER_IP, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] byServerIp = result.getByteArray(DeviceControl_Bluetooth_Fragment.CONTROL_SERVER_IP);

        if(byServerIp == null) return;
        byte[] bytes = makeFrame(
            new byte[]{REQUEST_CONTROL},
            BluetoothAPI.generateTag((byte) 0x69, BluetoothAPI.changeByteOrder(byServerIp, true)),
            bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });

    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.CONTROL_DEVICE_PORT, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte byDevicePort = result.getByte(DeviceControl_Bluetooth_Fragment.CONTROL_DEVICE_PORT);

        byte[] bytes = makeFrame(
            new byte[]{REQUEST_CONTROL},
            BluetoothAPI.generateTag((byte) 0x49, new byte[]{byDevicePort}),
            bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });
    getParentFragmentManager().setFragmentResultListener(DeviceControl_Bluetooth_Fragment.REQUEST_BATTERY_REMAINED, this, new FragmentResultListener() {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
        byte[] bytes = makeFrame(
            new byte[]{REQUEST_INDIVIDUAL_STATE},
            new byte[]{
                0x58, 0x00, 0x00, // 배터리 상태
                0x59, 0x00, 0x00, // 배터리 잔량
                0x5A, 0x00, 0x00, // 예상 동작 가능 시간
                0x5B, 0x00, 0x00  // 예상 충전 시간
            },
            bluetoothThread.getSequence()
        );
        bluetoothThread.writeHex(bytes);
      }
    });
  }
  private void setDetailFragment(int device_type, Bundle bundle) {
    Fragment fragment = new DeviceFragment();

    bundle.putString("protocol", DeviceFragment.PROTOCOL_BLUETOOTH);
    bundle.putInt("47", device_type);
    fragment.setArguments(bundle);

    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
    transaction.add(BT_CONTAINER, fragment, TAG_BLUETOOTH_DEVICE_FRAGMENT);
    //transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_left);
    transaction.addToBackStack(null);
    transaction.commit();

    iv_notFoundDevice.setVisibility(View.GONE);
  }

  private void searchPairedDevices() {
    bluetoothDevices = bluetoothAdapter.getBondedDevices();
    pairedDevices = bluetoothDevices.toArray(new BluetoothDevice[0]);
    listItem.clear();

    if(pairedDevices.length != 0) {
      for (BluetoothDevice pairedDevice : pairedDevices) {
        listItem.add(pairedDevice.getName());
      }
    };

    addListViewLastItem();
    listAdapter.notifyDataSetChanged();
  }
  private void connectBluetoothDevice(int position) {
    bluetoothThread.setBluetoothDevice(pairedDevices[position]);

    CompletableFuture<Void> completableFuture = CompletableFuture.supplyAsync(() -> {
      return bluetoothThread.connectSocket();
    }).thenAcceptAsync((b) -> {
      if (b) {
        if (!bluetoothThread.isRunning()) {
          bluetoothThread.start();

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              //progressOff();
            }
          });
        }
      }
    });
  }
  private void connectBluetoothDevice(BluetoothDevice device) {
    bluetoothThread.setBluetoothDevice(device);

    CompletableFuture<Void> completableFuture = CompletableFuture.supplyAsync(() -> {
      return bluetoothThread.connectSocket();
    }).thenAcceptAsync((b) -> {
      if (b) {
        if (!bluetoothThread.isRunning()) {
          bluetoothThread.start();

          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              //progressOff();
            }
          });
        }
      }
    });
  }
  private void addListViewLastItem() {
    listItem.add(getString(R.string.bluetooth_device_list_add));
  }



  private void loopReceiveData(int interval) {
    TimerTask data_timerTask = new TimerTask() {
      @Override
      public void run() {
        if(bluetoothThread.isConnected())
          bluetoothThread.writeHex(makeFrame(new byte[]{0x03}, new byte[]{(byte) 0xFF, 0x00, 0x00}, bluetoothThread.getSequence()));
        else
          try {
            data_scheduler.cancel();
          } catch(NullPointerException | IllegalStateException e) {
            e.printStackTrace();
          }
      }
    };
    data_scheduler = new Timer();
    data_scheduler.scheduleAtFixedRate(data_timerTask, 0, interval * 1000L);
  }
  private void processRecvData(Bundle bundle) {
    // IAQ 장착 센서 체크
    if (bundle.containsKey("35")) {
      // 데이터 반복 받기
      //loopReceiveData();
    }
    // GPS 동위 여부 체크
    if(bundle.containsKey("43") && bundle.containsKey("44")) {
      checkGPS(bundle.getDouble("43"), bundle.getDouble("44"));
    }

    String firmware = null, date = null;
    // 펌웨어 체크
    if(bundle.containsKey("45")) {
      firmware = bundle.getString("45");
    }
    // 날짜 체크
    if(bundle.containsKey("46")) {
      int dateValue = bundle.getInt("46");
      int year, month, day;

      year = (dateValue / 10000);
      dateValue = dateValue % 10000;
      month = dateValue / 100;
      dateValue = dateValue % 100;
      day = dateValue;

      date = year + "-" + month + "-" + day;
    }

    // 임시
    if(firmware != null && date != null) {
      //Toast.makeText(requireActivity(), "Firmware : " + firmware + "\n" + "Date : " + date, Toast.LENGTH_LONG).show();
    }
  }
  private void checkGPS(double latitude, double longitude) {
    + gpsTracker = ((MainActivity)requireActivity()).getGpsTracker();
    double lat = gpsTracker.getLatitude();
    double lng = gpsTracker.getLongitude();

    if(latitude == lat && longitude == lng) {
      return;
    } else {
      byte[] byteArrayLat = generateTag((byte) 0x43, doubleToByteArray(lat));
      byte[] byteArrayLng = generateTag((byte) 0x44, doubleToByteArray(lng));

      byte[] byteArrayGPS = combineArray(new byte[][]{byteArrayLat, byteArrayLng});
      makeFrame(new byte[]{REQUEST_CONTROL}, byteArrayGPS, bluetoothThread.getSequence());
    }
  }

  public void startMini() {
    Bluetooth_Mini mini = new Bluetooth_Mini(requireActivity());
    BluetoothDevice[] devices = mini.getPairedDevices();

    final int[] position = {-1};

    String[] deviceNames = new String[devices.length];
    boolean[] checkedList = new boolean[devices.length];
    for(int i=0; i<devices.length; i++) {
      deviceNames[i] = devices[i].getName();
      checkedList[i] = false;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
    builder.setTitle("연결할 장치를 선택하세요.")
        .setMultiChoiceItems(deviceNames, checkedList, new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
          position[0] = which;
        }
      })
        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            mini.connectBluetoothDevice(position[0]);

            mini.setProcessRequestBodyListener(new Bluetooth_Mini.processRequestBodyListener() {
              @Override
              public void onProcessRequestBodyListener(byte[] bytesBody) {
                Bundle body = analyzedRequestBody(bytesBody);

                // 센서 장착 목록
                if(body.containsKey("35")) {

                }
                // 장치 모델명
                if(body.containsKey("47")) {
                  //if(body.getChar("47") == 'T') {
                  if(Arrays.equals(body.getCharArray("47"), new char[]{'T', 'I'})) {
                    // 모델이 Mini
                    if(body.containsKey("48")) {
                      char[] char_id = body.getCharArray("48");
                      StringBuilder sb = new StringBuilder();
                      for(char ch : char_id) {
                        sb.append(ch);
                      }
                      String deviceID = sb.toString();

                      // MQTT 연결
                      mini.mqttConnect(deviceID);
                    }
                  }
                }
              }
            });
            mini.setProcessControlBodyListener(new Bluetooth_Mini.processControlBodyListener() {
              @Override
              public void onProcessControlBodyListener(byte[] bytesBody) {
                Bundle body = analyzedControlBody(bytesBody);

                // 제어 명령 응답 처리
                if(body.containsKey("52") || body.containsKey("55") || body.containsKey("56") || body.containsKey("57")) {
                  //getParentFragmentManager().setFragmentResult(GET_CONTROL_RESULT, body);
                }
              }
            });
          }
        })
        .create()
        .show();
  }
}
