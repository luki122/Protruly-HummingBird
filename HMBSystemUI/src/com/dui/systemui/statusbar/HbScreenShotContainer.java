package com.dui.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.FrameLayout;

/**
 * Created by chenheliang on 17-6-23.
 */

public class HbScreenShotContainer extends FrameLayout{
    private OnKeyListener mOnKeyListener;
    public HbScreenShotContainer(Context context) {
        super(context);
    }

    public HbScreenShotContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HbScreenShotContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyListener!=null && mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        mOnKeyListener = l;
    }
}
