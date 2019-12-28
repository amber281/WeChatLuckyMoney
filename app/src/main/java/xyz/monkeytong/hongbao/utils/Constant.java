package xyz.monkeytong.hongbao.utils;

public class Constant {
    //微信通知包含文字
    public static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";

    public static final String WECHAT_RID_CLICKED_NODE = "com.tencent.mm:id/atb";//微信可点击的红包节点
    public static final String WECHAT_RID_PERSONAL_IMAGE_NODE = "com.tencent.mm:id/po";//个人图像
    public static final String WECHAT_RID_HONGBAO_CONTENT = "com.tencent.mm:id/auk";//红包内容
    public static final String WECHAT_RID_HONGBAO_OPEN = "com.tencent.mm:id/dan";//打开红包按钮
    public static final String WECHAT_RID_GROUP = "com.tencent.mm:id/lt";//左上角的返回按钮旁边的信息
    public static final String WECHAT_RID_LIST_UN_READ = "com.tencent.mm:id/op";//聊天列表界面未读标记
    public static final String WECHAT_RID_LIST_DESC = "com.tencent.mm:id/bal";//聊天列表界面描述
    public static final String WECHAT_RID_TEXT = "com.tencent.mm:id/aqe"; //聊天输入框
    public static final String WECHAT_RID_SEND_BTN = "com.tencent.mm:id/aql";//发送按钮
    public static final String WECHAT_RID_CHANGE_BTN = "com.tencent.mm:id/aqc";//切换文本和语音按钮


    public static final String[] WECHAT_RECEIVED_MARKER = new String[]{"已领取", "已被领完", "已过期"};
    public static final String[] WECHAT_COMPLETE_MARKER = new String[]{"红包记录", "手慢了", "过期", "已超过24小时", "Details", "Better luck next time!"};
}
