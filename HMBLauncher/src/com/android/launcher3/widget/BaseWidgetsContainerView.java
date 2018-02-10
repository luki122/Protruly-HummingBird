package com.android.launcher3.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.android.launcher3.BaseContainerView;
import com.android.launcher3.IconCache;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.model.WidgetsModel;

/**
 * Created by lijun on 17-3-7.
 */

public abstract class BaseWidgetsContainerView extends BaseContainerView {

    public BaseWidgetsContainerView(Context context) {
        super(context);
    }

    public BaseWidgetsContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseWidgetsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public abstract void addWidgets(WidgetsModel model);
    public abstract void scrollToTop();
    public abstract boolean isEmpty();
}
