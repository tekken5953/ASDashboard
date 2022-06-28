package com.example.dashboard;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.dashboard.dashboard.DashBoardFragment;

import java.util.ArrayList;

public class SearchDeviceActivity extends AppCompatActivity {

    ListView listView;
    final ArrayList<String> arrayList = new ArrayList<>();
    ArrayAdapter<String> adapter;

    BluetoothAdapter bluetoothAdapter;

    BroadCastReceiver receiver;

    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);

        BluetoothPermission(); //블루투스 연결 엑세스 동의

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, R.layout.search_device_listitem, arrayList);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            receiver = new BroadCastReceiver();
            filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);

            Intent intent = new Intent();
            intent.setAction(BluetoothDevice.ACTION_FOUND);
            sendBroadcast(intent);
            Log.d("Bluetooth action", "받은액션 : " + intent.getAction());
        }
    }

    public void onClick(View view) {
        arrayList.clear();
        Intent intent = new Intent(SearchDeviceActivity.this, DashBoardFragment.class);
        startActivity(intent);
        finish();
    }

    //블루투스 퍼미션
    private void BluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.BLUETOOTH,
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE,
                            android.Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH
                    },
                    1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}

//https://keykat7.blogspot.com/2021/01/android-broadcast-receiver.html
//http://jinyongjeong.github.io/2018/09/27/bluetoothpairing/
