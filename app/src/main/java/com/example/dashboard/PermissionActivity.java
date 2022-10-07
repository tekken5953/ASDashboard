package com.example.dashboard;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dashboard.language.LanguageSelectActivity;

//https://ddangeun.tistory.com/158
//https://full-stack.tistory.com/entry/Android-Permission-%EA%B6%8C%ED%95%9C-1-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B6%8C%ED%95%9C-feat-Bluetooth
public class PermissionActivity extends AppCompatActivity {
    String[] permissionUnder31 = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET};
    String[] permissionAbove31 = {Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.INTERNET};

    static final int REQUEST_UP_PERMISSIONS = 2;
    static final int REQUEST_DOWN_PERMISSIONS = 1;

    TextView grant, deny;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_permission);

        grant = findViewById(R.id.grantTx);

        bleInitialize();

        deny = findViewById(R.id.denyTx);

        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_UP_PERMISSIONS) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                    && grantResults[3] == PackageManager.PERMISSION_GRANTED
            ) {
                if (grantResults[4] == PackageManager.PERMISSION_GRANTED
                        && grantResults[5] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    AlertDialog alertDialog = builder.create();
                    alertDialog.setTitle("권한");
                    alertDialog.setMessage("인터넷 권한 거부로 인해 \n측정 데이터를 서버로 전송 할 수 없습니다");
                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "돌아가기", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            alertDialog.dismiss();
                        }
                    });
                }
            }
        } else if (requestCode == REQUEST_DOWN_PERMISSIONS) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog alertDialog = builder.create();
                alertDialog.setTitle("권한");
                alertDialog.setMessage("인터넷 권한 거부로 인해 \n측정 데이터를 서버로 전송 할 수 없습니다");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "돌아가기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
            }
        }
    }

    private void bleInitialize() {
        // 런타임 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                grant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestBlePermissions();
                    }
                });
            } else {
                Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                grant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestBlePermissions();
                    }
                });
            } else {
                Intent intent = new Intent(PermissionActivity.this, LanguageSelectActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    permissionAbove31, REQUEST_UP_PERMISSIONS);
        } else {
            requestPermissions(
                    permissionUnder31,
                    REQUEST_DOWN_PERMISSIONS);
        }
    }
}