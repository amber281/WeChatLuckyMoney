package xyz.monkeytong.hongbao.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class HongbaoSignature {

    private static final String TAG = "HongbaoSignature";
    public String sender, content, commentString;
    public boolean others;

    public boolean generateSignature(AccessibilityNodeInfo hongbaoNode, String excludeWords) {
        try {
            //红包节点必须为LinearLayout
            if (!"android.widget.LinearLayout".equals(hongbaoNode.getClassName())) return false;
            //红包内容
            List<AccessibilityNodeInfo> hongbaoContentNode = hongbaoNode.findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_HONGBAO_CONTENT);
            if (hongbaoContentNode == null || hongbaoContentNode.isEmpty()) return false;
            String hongbaoContent = hongbaoContentNode.get(0).getText().toString();
            //排除掉关键字
            String[] excludeWordsArray = excludeWords.split(" +");
            for (String word : excludeWordsArray) {
                if (word.length() > 0 && hongbaoContent.contains(word)) return false;
            }
            AccessibilityNodeInfo messageNode = hongbaoNode.getParent();
            Rect bounds = new Rect();
            messageNode.getBoundsInScreen(bounds);
            if (bounds.top < 0) return false;
            //获取发送者
            String hongbaoSender = getSenderFromNode(messageNode);
            this.sender = hongbaoSender;
            this.content = hongbaoContent;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "generateSignature: ", e);
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getSignature(this.sender, this.content);
    }

    private String getSignature(String... strings) {
        String signature = "";
        for (String str : strings) {
            if (str == null) return null;
            signature += str + "|";
        }
        return signature.substring(0, signature.length() - 1);
    }

    /**
     * 获取发送者
     *
     * @param node
     * @return
     */
    private String getSenderFromNode(AccessibilityNodeInfo node) {
        String sender = "unknownSender";
        CharSequence contentDescription = null;
        try {
            contentDescription = node.getChild(1).getContentDescription();
        } catch (Exception e) {
            List<AccessibilityNodeInfo> list = node.getParent().findAccessibilityNodeInfosByViewId(Constant.WECHAT_RID_PERSONAL_IMAGE_NODE);
            if (list != null && !list.isEmpty()) {
                contentDescription = list.get(0).getContentDescription();
            }
        }
        if (contentDescription != null) {
            sender = contentDescription.toString().replaceAll("头像$", "");
        }
        return sender;
    }

    public void cleanSignature() {
        this.content = "";
        this.sender = "";
    }
}
