package com.android.launcher3.dynamicui;

import android.content.Context;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;

/**
 * Created by liuzuo on 17-4-1.
 */

public interface IDynamicIcon {
    boolean init(Context context, BubbleTextView bubbleTextView, ItemInfo info);
    void removeDynamicReceiver();
    boolean updateDynamicIcon(boolean register);
    void clearDynamicIcon();
}
