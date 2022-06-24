package com.example.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class BroadCastReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();   //입력된 action
        Toast.makeText(context, "받은 액션 : " + action, Toast.LENGTH_SHORT).show();
        Log.d("Bluetooth action", action);
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String name = null;
        if (device != null) {
            name = device.getName();    //broadcast를 보낸 기기의 이름을 가져온다.
        }
        //입력된 action에 따라서 함수를 처리한다
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED: //블루투스의 연결 상태 변경
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:

                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:

                        break;
                    case BluetoothAdapter.STATE_ON:

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:

                        break;
                }

                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:  //블루투스 기기 연결

                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:

                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:   //블루투스 기기 끊어짐

                break;

            case BluetoothAdapter.ACTION_DISCOVERY_STARTED: //블루투스 기기 검색 시작
                //프로그래스 바 보이기

                break;
            case BluetoothDevice.ACTION_FOUND:  //블루투스 기기 검색 됨, 블루투스 기기가 근처에서 검색될 때마다 수행됨
                assert device != null;
                String device_name = device.getName();
                String device_Address = device.getAddress();

                if (device_name != null && device_name.length() > 4) {
                    Log.d("Bluetooth Name: ", device_name);
                    Log.d("Bluetooth Mac Address: ", device_Address);
                    //Filtering
//                        if (device_name.substring(0, 3).equals("GSM")) {
//                            bluetooth_device.add(device);
//                        }
                }
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                break;
        }

        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //BluetoothAdapter.ACTION_STATE_CHANGED : 블루투스 상태변화 액션
        stateFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); //연결 확인
        stateFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //연결 끊김 확인
        stateFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        stateFilter.addAction(BluetoothDevice.ACTION_FOUND);    //기기 검색됨
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);   //기기 검색 시작
        stateFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //기기 검색 종료
        stateFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        context.registerReceiver(this, stateFilter);

    }
}


