package com.example.asdashboard.utils;

import android.app.Activity;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;

public class SnackBarUtils {
    Snackbar snackbar;
    public void makeSnack(View view, Activity activity, String msg) {
        Runnable r = () -> {
            snackbar = Snackbar.make(view,msg,Snackbar.LENGTH_SHORT);
            if (snackbar.isShown()) {
                snackbar.dismiss();
            }
            snackbar.show();
        };
        activity.runOnUiThread(r);
    }
}
