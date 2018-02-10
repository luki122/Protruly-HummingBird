package com.protruly.music.ui.lock;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * Created by wxue on 17-9-18.
 */
public class SlideUnlockLayout extends FrameLayout {
    private static final String TAG = "SlidingLayout";
    /**
     * 滑动的最小距离
     */
    private int mTouchSlop;
    private int downX;
    private int downY;
    private int tempY;
    private Scroller mScroller;
    private int viewHeight;
    private boolean isSilding;

    private OnSildingFinishListener onSildingFinishListener;
    private boolean isFinish;


    public SlideUnlockLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideUnlockLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            viewHeight = this.getHeight();
        }
    }

    /**
     * 设置OnSildingFinishListener, 在onSildingFinish()方法中finish Activity
     *
     * @param onSildingFinishListener
     */
    public void setOnSildingFinishListener(
            OnSildingFinishListener onSildingFinishListener) {
        this.onSildingFinishListener = onSildingFinishListener;
    }

    /**
     * 滚动出界面
     */
    private void scrollTop() {
        final int delta = (viewHeight - getScrollY());
        Log.d(TAG,"---scrollTop()--getScrollY = " + getScrollY() + " delta = " + delta);
        // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
        mScroller.startScroll(0, getScrollY(), 0, delta,
                Math.abs(400));
        postInvalidate();
    }

    /**
     * 滚动到起始位置
     */
    private void scrollOrigin() {
        int delta = getScrollY();
        Log.d(TAG,"---scrollOrigin()--delta = " + delta);
        mScroller.startScroll(0, getScrollY(), 0, -delta,
                Math.abs(delta));
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getRawX();  // 触摸点距离屏幕左边界的距离
                downY = tempY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveY = (int)event.getRawY();
                int deltaY = tempY - moveY;
                tempY = moveY;
                if (Math.abs(moveY - downY) > mTouchSlop && Math.abs((int) event.getRawX() - downX) < mTouchSlop) {
                    isSilding = true;
                }

                int dis = downY - moveY;
                if(isSilding){
                    // 防止先上滑再下滑滑出下边界（当上滑距离 < 下滑距离）
                    if(dis > 0 && (getScrollY() + deltaY) > 0){
                        scrollBy(0,deltaY);
                    }else{
                        scrollTo(0,0);
                        isSilding = false;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                isSilding = false;
                if (getScrollY() >= viewHeight / 3) {
                    isFinish = true;
                    scrollTop();
                } else {
                    scrollOrigin();
                    isFinish = false;
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }else{
            if (mScroller.isFinished() && onSildingFinishListener != null && isFinish) {
                onSildingFinishListener.onSildingFinish();
            }
        }
    }

    public interface OnSildingFinishListener {
        public void onSildingFinish();
    }

}


