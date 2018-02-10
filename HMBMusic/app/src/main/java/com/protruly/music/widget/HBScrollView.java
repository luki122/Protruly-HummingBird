package com.protruly.music.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;
/**
 * Created by hujianwei on 17-8-30.
 */

public class HBScrollView extends ScrollView {
    private ScrollViewListener scrollViewListener;

    private boolean mbNotScroll = false;

    public HBScrollView(Context context) {
        super(context);
    }

    public HBScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HBScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface ScrollViewListener {
        void onScrollChanged(HBScrollView scrollView, int x, int y, int oldx, int oldy);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mbNotScroll) {
            return false;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mbNotScroll) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    public void setViewCanScroll(boolean bsrcoll) {
        mbNotScroll = bsrcoll;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if (scrollViewListener != null) {
            scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
}
