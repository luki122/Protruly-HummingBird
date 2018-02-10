package com.hb.thememanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import hb.widget.ViewPager;

/**
 * Created by alexluo on 17-7-10.
 */

public class CenterItemViewPager extends ViewPager {
    public CenterItemViewPager(Context context) {
        super(context);
    }

    public CenterItemViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);

    }


}
