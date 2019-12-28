package xyz.monkeytong.hongbao.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class GestureUtil {

    /**
     * 模拟手指触摸点击事件
     *
     * @param service
     * @param action      具体的动作（例如：按住说话）
     * @param x           横坐标
     * @param y           纵坐标
     * @param millisecond 点击时间间隔
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean dispatchGestureClick(AccessibilityService service, String action, int x, int y, int millisecond) {
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(p, 0, millisecond));
        GestureDescription gesture = builder.build();
        return service.dispatchGesture(gesture, new WeChatGestureResultCallback(service, action), null);
    }


    /**
     * 模拟点击返回
     *
     * @param service
     * @param resourceId 返回按钮的resoucesid
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void performWxBack(AccessibilityService service, String resourceId) {
        AccessibilityNodeInfo rootNode = service.getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }
        List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByViewId(resourceId);
        if (list != null && list.size() > 0) {//返回按钮存在，模拟点击返回按钮
            performClick(list.get(0));
        } else {//返回按钮不存在，模拟点击系统返回
            Rect rect = new Rect(22, 54, 86, 184);
            String action = null;
            dispatchGestureClick(service, action, rect.centerX(), rect.centerY(), 100);
        }
    }

    /**
     * 模拟点击微信首页的“搜索”按钮
     *
     * @param service
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void performWxSearchClick(AccessibilityService service, Rect outBounds) {
        String action = null;
        dispatchGestureClick(service, action, outBounds.centerX(), outBounds.centerY(), 100);
    }


    /**
     * 模拟点击事件
     *
     * @param nodeInfo
     * @return
     */
    public static boolean performClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return false;
        }
        if (nodeInfo.isClickable()) {
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            return performClick(nodeInfo.getParent());
        }
    }
}
