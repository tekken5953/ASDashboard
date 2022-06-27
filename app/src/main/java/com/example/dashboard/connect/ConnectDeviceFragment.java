package com.example.dashboard.connect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.example.dashboard.LanguageSelectActivity;
import com.example.dashboard.R;
import com.example.dashboard.SharedPreferenceManager;
import com.example.dashboard.dashboard.DashBoardFragment;
import com.example.dashboard.dashboard.DashboardRecyclerItem;

import java.util.ArrayList;

public class ConnectDeviceFragment extends AppCompatActivity {

    ImageView selectLanguage;
    Context context;
    RecyclerView deviceList;
    ArrayList<ConnectRecyclerItem> mList = new ArrayList<>();
    ConnectRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_bluetooth_fragment);

        selectLanguage = findViewById(R.id.connTopBackIv);
        context = ConnectDeviceFragment.this;
        deviceList = findViewById(R.id.connConnectableList);
        adapter = new ConnectRecyclerAdapter(mList);
        deviceList.setAdapter(adapter);
        mList.clear();
        for (int i = 1; i < 10; i++) {
            addItem("Test" + i, "연결하기");
            adapter.notifyDataSetChanged();
        }

        adapter.setOnItemClickListener(new ConnectRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Intent intent = new Intent(context, DashBoardFragment.class);
                intent.putExtra("device_name", mList.get(position).getDevice_name());
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

    public void addItem(String name, String connect) {
        ConnectRecyclerItem item = new ConnectRecyclerItem(name, connect);

        item.setDevice_name(name);
        item.setConnect(connect);

        mList.add(item);
    }
}