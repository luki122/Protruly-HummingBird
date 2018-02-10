package com.protruly.music.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import hb.widget.ViewPager;

/**
 * Created by xiaobin on 17-9-28.
 */

public class HBNoScrollViewPager extends ViewPager {

    private boolean noScroll = false;

    public HBNoScrollViewPager(Context context) {
        super(context);
    }

    public HBNoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isNoScroll() {
        return noScroll;
    }

    public void setNoScroll(boolean noScroll) {
        this.noScroll = noScroll;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (noScroll) {
            return false;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (noScroll) {
            return false;
        } else {
            return super.onTouchEvent(ev);
        }
    }

}
