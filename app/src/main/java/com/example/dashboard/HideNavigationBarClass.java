package com.example.dashboard;

import android.app.Activity;
import android.util.Log;
import android.view.View;

public class HideNavigationBarClass {

     public void hide(Activity context) {
        int uiOptions = context.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.d("immersivemod", "Turning immersive mode mode off. ");
        } else {
            Log.d("immersivemod", "Turning immersive mode mode on.");
        }
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        context.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
