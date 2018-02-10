package com.android.deskclock;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import hb.widget.ViewPager;

/**
 * Created by yubai on 17-5-9.
 */

public class AlarmViewPager extends ViewPager {

    ViewPagerScrollHost mHost;

    public interface ViewPagerScrollHost {
        boolean isScrollEventIntercepted();
    }

    public AlarmViewPager(Context context) {
        super(context);
    }

    public AlarmViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setViewPagerScrollHost(ViewPagerScrollHost host) {
        mHost = host;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mHost != null) {
            if (mHost.isScrollEventIntercepted()
                    && (ev.getAction() == MotionEvent.ACTION_MOVE
                            || ev.getAction() == MotionEvent.ACTION_DOWN)) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }
}
