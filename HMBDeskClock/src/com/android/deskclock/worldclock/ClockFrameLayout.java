package com.android.deskclock.worldclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by yubai on 17-5-18.
 */

class ClockFrameLayout extends LinearLayout {
    public ClockFrameLayout(Context context) {
        super(context);
    }

    public ClockFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
