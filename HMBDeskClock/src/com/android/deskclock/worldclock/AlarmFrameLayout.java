package com.android.deskclock.worldclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.FrameLayout;
import android.widget.Scroller;


/**
 * Created by yubai on 17-7-31.
 */

public class AlarmFrameLayout extends FrameLayout {
    private float mDownY, mLastY;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    public AlarmFrameLayout(Context context) {
        super(context);
    }

    public AlarmFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlarmFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context);
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float curY, deltaY;
        Drawable background = getBackground();
        mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getRawY();
                mLastY = mDownY;
                break;
            case MotionEvent.ACTION_UP:
                curY = event.getRawY();
                deltaY = curY - mDownY;
//                int scrollY = this.getScrollY();
                if (getTranslationY() == 0) {
                    if (background != null) {
                        background.clearColorFilter();
                    }
                    return true;
                }

                if (Math.abs(mVelocityTracker.getYVelocity()) > 4000f) {
                    if (mVelocityTracker.getYVelocity() < 0f) {
                        //正向逻辑代码
//                        smoothScrollTo(0, getHeight());
//                        ObjectAnimator.ofFloat(this, "translationY", -getHeight()).start();
                        moveMoveView(-getHeight(), true);
                    } else {
                        //反向逻辑代码
//                        smoothScrollTo(0, 0);
//                        ObjectAnimator.ofFloat(this, "translationY", 0).start();
                        moveMoveView(0, false);
                    }
                    return true;
                }

                if (Math.abs(deltaY) < 600) {
//                    smoothScrollTo(0, 0);
//                    ObjectAnimator.ofFloat(this, "translationY", 0).start();
                    moveMoveView(0, false);
                } else {
//                    smoothScrollTo(0, getHeight());
//                    ObjectAnimator.ofFloat(this, "translationY", -getHeight()).start();
                    moveMoveView(-getHeight(), true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                curY = event.getRawY();
                deltaY = curY - mDownY;
                // 阻止视图在原来位置时向下滚动
                if (deltaY > 0 && getTranslationY() <= 0) {
//                    smoothScrollBy(0, -getScrollY());
                    setTranslationY(0);
                } else {
                    // 随着手指移动
                    // method 1 using scroller
//                    smoothScrollBy(0, -(int)(curY - mLastY));

                    // method 2
//                    MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
//                    params.bottomMargin -= curY - mLastY;
//                    params.topMargin += curY - mLastY;
//                    requestLayout();

                    //method 3
                    float y = getTranslationY() + curY - mLastY;
                    setTranslationY(y);
                    if(background != null){
                        int alpha = (int) ((1- (getHeight() - y) / (float) getHeight()) * 255);
                        if (alpha != 0) {
                            int color = Color.argb(alpha, 150, 150, 150);
                            background.setColorFilter(color, PorterDuff.Mode.DARKEN);
                        }
                    }
                }
                mLastY = curY;
                mVelocityTracker.computeCurrentVelocity(1000);
                break;
        }
        return true;
    }

    private void moveMoveView(float to, boolean exit){
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", to);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                if(getBackground()!=null){
//                    int alpha = (int) ((getHeight() - getTranslationY()) / (float) getHeight() * 255);
//                    getBackground().setAlpha(alpha);
//                }
//            }
//        });//随移动动画更新背景透明度
        animator.setDuration(250).start();
        getBackground().clearColorFilter();
        if (exit) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
//                mainHandler.obtainMessage(LockScreenActivity.MSG_LAUNCH_HOME).sendToTarget();
                    mDismissAlarmListener.dismissAlarm();
                    super.onAnimationEnd(animation);
                }
            });
            //监听动画结束，利用Handler通知Activity退出
        }
    }

    @Override
    public void computeScroll() {
        //先判断mScroller滚动是否完成
        if (mScroller.computeScrollOffset()) {

            //这里调用View的scrollTo()完成实际的滚动
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());

            //必须调用该方法，否则不一定能看到滚动效果
            postInvalidate();
        }
        super.computeScroll();
    }

    //调用此方法滚动到目标位置
    public void smoothScrollTo(int fx, int fy) {
        int dx = fx - mScroller.getFinalX();
        int dy = fy - mScroller.getFinalY();
        smoothScrollBy(dx, dy);
    }

    //调用此方法设置滚动的相对偏移
    public void smoothScrollBy(int dx, int dy) {

        //设置mScroller的滚动偏移量
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy);
        invalidate();//这里必须调用invalidate()才能保证computeScroll()会被调用，否则不一定会刷新界面，看不到滚动效果
    }

    public interface DismissAlarmListener {
        void dismissAlarm();
    }

    DismissAlarmListener mDismissAlarmListener;

    public void setOnDismissAlarmListener(DismissAlarmListener listener) {
        mDismissAlarmListener = listener;
    }

}
