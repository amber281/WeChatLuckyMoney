package xyz.monkeytong.hongbao.services;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import xyz.monkeytong.hongbao.utils.Constant;
import xyz.monkeytong.hongbao.utils.GestureUtil;
import xyz.monkeytong.hongbao.utils.HongbaoSignature;
import xyz.monkeytong.hongbao.utils.PowerUtil;

public class HongbaoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "";
    private static final boolean DEBUG = false;
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = ".plugin.luckymoney.ui";//com.tencent.mm/.plugin.luckymoney.ui.En_fba4b94f  com.tencent.mm/com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    private AccessibilityNodeInfo rootNodeInfo, mReceiveNode, mUnpackNode, mSendNode;
    //戳开标记，接收标记,回复标记
    private boolean mLuckyMoneyPicked, mLuckyMoneyReceived;
    private int mUnpackCount = 0;//未戳开红包数
    private boolean mMutex = false, mListMutex = false, mChatMutex = false;
    private HongbaoSignature signature = new HongbaoSignature();

    private PowerUtil powerUtil;
    private SharedPreferences sharedPreferences;

    /**
     * AccessibilityEvent
     *
     * @param event 事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (sharedPreferences == null) return;
        setCurrentActivityName(event);
        Log.d(TAG, "=========================onAccessibilityEvent: " + event.getEventType() + " ==============================");
        if (!mMutex) {
            //监视通知
            boolean pref_watch_notification = sharedPreferences.getBoolean("pref_watch_notification", false);
            if (pref_watch_notification && watchNotifications(event)) {
                return;
            }
            //监视聊天列表
            if (sharedPreferences.getBoolean("pref_watch_list", false) && watchList(event)) {
                return;
            }
            mListMutex = false;
        }
        if (!mChatMutex) {
            mChatMutex = true;
            //自动拆开红包
            if (sharedPreferences.getBoolean("pref_watch_chat", false)) {
                watchChat(event);
            }
            mChatMutex = false;
        }
    }

    /**
     * 聊天界面
     *
     * @param event
     */
    private void watchChat(AccessibilityEvent event) {
        this.rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo == null) return;
        mUnpackNode = null;
        checkNodeInfo(event.getEventType());
        /* 如果已经接收到红包并且还没有拆开 */
        if (DEBUG)
            Log.d(TAG, "点开红包！ mLuckyMoneyReceived:" + mLuckyMoneyReceived + " mLuckyMoneyPicked:" + mLuckyMoneyPicked + " mReceiveNode:" + mReceiveNode);
        if (mLuckyMoneyReceived && (mReceiveNode != null)) {
            mMutex = true;
            mReceiveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
        }
        /* 如果戳开但还未领取 */
        if (DEBUG)
            Log.d(TAG, "领取红包！" + " mUnpackCount: " + mUnpackCount + " mUnpackNode: " + mUnpackNode);
        if (mUnpackCount >= 1 && (mUnpackNode != null)) {
            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 1000;
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            try {
                                GestureUtil.performClick(mUnpackNode);
                            } catch (Exception e) {
                                mMutex = false;
                                mLuckyMoneyPicked = false;
                                mUnpackCount = 0;
                            }
                        }
                    },
                    delayFlag);
        }
    }

    /**
     * 设置当前Activity
     *
     * @param event
     */
    private void setCurrentActivityName(AccessibilityEvent event) {
        try {
            ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
        }
    }

    private boolean watchList(AccessibilityEvent event) {
        if (mListMutex) return false;
        mListMutex = true;
        AccessibilityNodeInfo listNode = getRootInActiveWindow();
        // Not a message
        if (listNode == null) return false;
        // Not un read message
        List<AccessibilityNodeInfo> list = listNode.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_LIST_UN_READ);
        if (list == null || list.isEmpty()) return false;
        AccessibilityNodeInfo hongbaoNode = null;
        for (int i = 0; i < list.size(); i++) {
            AccessibilityNodeInfo contentNode = list.get(i).getParent();
            if (!contentNode.getClassName().equals("android.widget.LinearLayout")) {
                contentNode = contentNode.getParent();
            }
            List<AccessibilityNodeInfo> nodes = contentNode.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_LIST_DESC);
            if (!nodes.isEmpty() && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
                String contentDesc = nodes.get(0).getText().toString();
                if (contentDesc.contains(Constant.WECHAT_NOTIFICATION_TIP)) {
                    hongbaoNode = contentNode;
                    break;
                }
            }
        }
        if (hongbaoNode == null) return false;
        if (hongbaoNode.isClickable()) {
            hongbaoNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }


    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;
        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(Constant.WECHAT_NOTIFICATION_TIP)) {
            return true;
        }
        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                /* 清除signature,避免进入会话后误判 */
                signature.cleanSignature();
                notification.contentIntent.send();//点击通知进入
                if (DEBUG)
                    Log.d(TAG, "监控到通知...");
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "watchNotifications: ", e);
            }
        }
        return true;
    }

    @Override
    public void onInterrupt() {
    }

    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        List<AccessibilityNodeInfo> openButtonList = node.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_HONGBAO_OPEN);
        if (openButtonList == null || openButtonList.isEmpty()) {
            return null;
        }
        AccessibilityNodeInfo button = openButtonList.get(0);
        if (!"android.widget.Button".equals(button.getClassName())) return null;
        return button;
    }

    private void checkNodeInfo(int eventType) {
        if (this.rootNodeInfo == null) return;
        //自动发送信息
        if (signature.commentString != null) {
            sendComment();
        }
        AccessibilityNodeInfo hongbaoNode = getTheLastNode();
        if (DEBUG) {
            Log.d(TAG, "hongbaoNode=" + hongbaoNode);
        }
        /* 接收红包（只是获取节点+标记）*/
        if (hongbaoNode != null && (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))) {
            String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");//不拆开包含这些文字的红包
            if (this.signature.generateSignature(hongbaoNode, excludeWords)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = hongbaoNode;
                if (DEBUG)
                    Log.d("signature=", this.signature.toString());
            }
            if (DEBUG)
                Log.d(TAG, "mLuckyMoneyReceived=" + mLuckyMoneyReceived + "  mReceiveNode=" + mReceiveNode);
            return;
        }

        /* 戳开红包（只是获取节点+标记）*/
        AccessibilityNodeInfo openButton = findOpenButton(this.rootNodeInfo);
        if (DEBUG)
            Log.d(TAG, "openButton=" + openButton);
        if (openButton != null && (mUnpackNode == null || mUnpackNode != null && !mUnpackNode.equals(openButton))) {
            mUnpackNode = openButton;
            mUnpackCount += 1;
            if (DEBUG)
                Log.d(TAG, "mUnpackCount=" + mUnpackCount + " mUnpackNode=" + mUnpackNode);
            return;
        }

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = this.hasOneOfThoseNodes(Constant.WECHAT_COMPLETE_MARKER);
        if (DEBUG)
            Log.d(TAG, "hasNodes=" + hasNodes + " mMutex=" + mMutex);
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && hasNodes
                && (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY)
                || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))) {
            //防止页面还没加载就点击返回
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            mMutex = false;
            mLuckyMoneyPicked = false;
            mUnpackCount = 0;
            performGlobalAction(GLOBAL_ACTION_BACK);
            signature.commentString = generateCommentString();
            if (DEBUG)
                Log.d(TAG, "signature.commentString: " + signature.commentString);
        }
    }

    private void sendComment() {
        List<AccessibilityNodeInfo> textNode = this.rootNodeInfo.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_TEXT);
        if (textNode == null || textNode.isEmpty()) {
            List<AccessibilityNodeInfo> changeButton = this.rootNodeInfo.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_CHANGE_BTN);
            if (changeButton != null && !changeButton.isEmpty()) {
                GestureUtil.performClick(changeButton.get(0));
            }
            return;
        }
        //粘贴
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, signature.commentString);
        textNode.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        //自动发送
        CharSequence content = textNode.get(0).getText();
        if (content == null || content.equals("")) return;
        List<AccessibilityNodeInfo> sendNode = this.rootNodeInfo.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_SEND_BTN);
        if (sendNode == null || sendNode.isEmpty()) return;
        mSendNode = sendNode.get(0);
        int delayFlag = sharedPreferences.getInt("pref_comment_delay", 0) * 1000;
        new android.os.Handler().postDelayed(new Runnable() {
            public void run() {
                GestureUtil.performClick(mSendNode);
                signature.commentString = null;
            }
        }, delayFlag);
    }

    private boolean hasOneOfThoseNodes(String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;
            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo getTheLastNode() {
        //是否允许拆开自己发的红包
        boolean pref_watch_self = sharedPreferences.getBoolean("pref_watch_self", false);
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes = this.rootNodeInfo.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_CLICKED_NODE);
        if (DEBUG) {
            Log.d(TAG, "聊天页面红包数: " + nodes.size());
        }
        if (nodes != null && !nodes.isEmpty()) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                tempNode = nodes.get(i);
                //过滤非红包节点
                if (!tempNode.getClassName().equals("android.widget.LinearLayout") && !tempNode.isClickable())
                    continue;
                //已经领取过的不能领取
                if (isReceived(tempNode)) continue;
                AccessibilityNodeInfo parentNode = tempNode.getParent();
                //非群聊，自己发的红包不能领取
                if (!isGroupChat() && isSelfHongBao(parentNode)) continue;
                //没有设置允许拆开自己的红包，自己发的红包不能领取
                if (!pref_watch_self && isSelfHongBao(parentNode)) continue;

                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
                    signature.others = true;
                    return lastNode;
                }
            }
        }
        return lastNode;
    }

    /**
     * 是否是自己发的红包
     *
     * @param parentNode
     * @return
     */
    private boolean isSelfHongBao(AccessibilityNodeInfo parentNode) {
        if (parentNode == null) return false;
        AccessibilityNodeInfo selfNode = null;
        try {
            selfNode = parentNode.getChild(1);
        } catch (Exception e) {
            List<AccessibilityNodeInfo> list = parentNode.getParent().findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_PERSONAL_IMAGE_NODE);
            if (list != null && !list.isEmpty()) {
                selfNode = list.get(0);
            }
        }
        if (selfNode != null) {
            Rect outBounds = new Rect();
            selfNode.getBoundsInScreen(outBounds);
            if (outBounds.left > 200) {
                return true;
            }
        }
        return false;
    }

    /**
     * 该红包是否已被领取
     *
     * @param hongbaoNode 红包节点
     * @return
     */
    private boolean isReceived(AccessibilityNodeInfo hongbaoNode) {
        String[] receivedString = Constant.WECHAT_RECEIVED_MARKER;
        for (int j = 0; j < receivedString.length; j++) {
            List<AccessibilityNodeInfo> list = hongbaoNode.findAccessibilityNodeInfosByText(receivedString[j]);
            if (list != null && !list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否是群聊界面
     *
     * @return
     */
    private boolean isGroupChat() {
        List<AccessibilityNodeInfo> list = this.rootNodeInfo.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_GROUP);
        if (list != null && !list.isEmpty()) {
            String text = list.get(0).getText().toString();
            if (text.indexOf("(") > 0 && text.indexOf(")") > 0) {
                String str = text.substring(text.indexOf("(") + 1, text.indexOf(")"));
                try {
                    Integer i = Integer.valueOf(str);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.powerUtil = new PowerUtil(this);
        Boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_watch_on_lock", false);
        this.powerUtil.handleWakeLock(watchOnLockFlag);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_watch_on_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    @Override
    public void onDestroy() {
        this.powerUtil.handleWakeLock(false);
        super.onDestroy();
    }

    /**
     * 生成回复信息
     *
     * @return
     */
    private String generateCommentString() {
        if (!signature.others) return null;//未领取红包，不生成回复内容

        Boolean needComment = sharedPreferences.getBoolean("pref_comment_switch", false);
        if (!needComment) return null;//未开启自动回复，不生成回复内容

        String[] wordsArray = sharedPreferences.getString("pref_comment_words", "").split(" +");
        if (wordsArray.length == 0) return null;//未定义自动回复内容，不生成回复内容

        Boolean atSender = sharedPreferences.getBoolean("pref_comment_at", false);//是否@发红包的人
        if (atSender) {
            return "@" + signature.sender + " " + wordsArray[(int) (Math.random() * wordsArray.length)];
        } else {
            return wordsArray[(int) (Math.random() * wordsArray.length)];
        }
    }
}