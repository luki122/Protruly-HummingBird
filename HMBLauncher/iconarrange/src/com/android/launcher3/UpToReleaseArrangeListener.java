package com.android.launcher3;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by lijun on 17-4-12.
 */

public class UpToReleaseArrangeListener extends GestureDetector.SimpleOnGestureListener {
    private GestureDetector mGestureDetector;
    private Launcher mLauncher;

    public UpToReleaseArrangeListener(Launcher launcher) {
        mLauncher = launcher;
        mGestureDetector = new GestureDetector(mLauncher, this);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mGestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // 向上手势触发条件：速度大于100，斜率大于0.4，Y轴向上滑动距离大于100
        if(e1 == null || e2 == null)return false;
        if (Math.abs(velocityY) > 60 && Math.abs(e1.getRawX() - e2.getRawX()) / (e1.getRawY() - e2.getRawY()) < 0.6 && (e1.getRawY() - e2.getRawY()) > 50) {
            if (mLauncher != null) {
                ArrangeNavigationBar arrangeNavigationBar = mLauncher.getArrangeNavigationBar();
                if (arrangeNavigationBar != null) {
                    arrangeNavigationBar.mutilpleAdd2Other();
                    return true;
                }
            }
        }
        return false;
    }
}
