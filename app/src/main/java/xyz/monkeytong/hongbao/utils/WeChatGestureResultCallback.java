package xyz.monkeytong.hongbao.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class WeChatGestureResultCallback extends AccessibilityService.GestureResultCallback {

    private AccessibilityService mService;

    private String mAction;

    public WeChatGestureResultCallback(AccessibilityService service, String action) {
        mService = service;
        mAction = action;
    }

    @Override
    public void onCompleted(GestureDescription gestureDescription) {
        super.onCompleted(gestureDescription);
    }
}
