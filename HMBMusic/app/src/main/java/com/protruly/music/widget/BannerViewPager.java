package com.protruly.music.widget;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import com.protruly.music.model.CustomDurationScroller;
import com.protruly.music.util.LogUtil;
import java.lang.reflect.Field;

/**
 * Created by hujianwei on 17-9-4.
 */

public class BannerViewPager  extends ViewPager{

    private static final String TAG = "BannerViewPager";
    /** 触摸时按下的点 **/
    PointF downP = new PointF();

    /** 触摸时当前的点 **/
    PointF curP = new PointF();
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    private int direction = RIGHT;

    public static final int DEFAULT_INTERVAL = 1500;
    private long interval = DEFAULT_INTERVAL;
    private boolean isAutoScroll = false;
    public static final int SCROLL_WHAT = 0;

    // 是否循环
    private boolean isCycle = true;

    // 是否需要动画
    private boolean isBorderAnimation = true;

    // 触摸时是否停止自动滚动
    private boolean stopScrollWhenTouch = true;

    // 是否触摸时停止的滚动
    private boolean isStopByTouch = false;
    private CustomDurationScroller scroller = null;
    private Handler handler;

    public BannerViewPager(Context context) {
        this(context, null);
    }

    public BannerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        handler = new MyHandler();
        setViewPagerScroller();
    }

    /**
     * 反射机制修改Viewpager的滑动速度
     */
    private void setViewPagerScroller() {
        try {
            Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Field interpolatorField = ViewPager.class
                    .getDeclaredField("sInterpolator");
            interpolatorField.setAccessible(true);

            scroller = new CustomDurationScroller(getContext(),
                    (Interpolator) interpolatorField.get(null));
            scrollerField.set(this, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置滚动速度
     *
     * @param scrollFactor
     */
    public void setScrollDurationFactor(double scrollFactor) {
        scroller.setScrollDurationFactor(scrollFactor);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {

        try {

            return super.onInterceptTouchEvent(arg0);

        } catch (IllegalArgumentException ex) {
            LogUtil.d(TAG, "onInterceptTouchEvent IllegalArgumentException");
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        try {

            return super.onTouchEvent(arg0);

        } catch (IllegalArgumentException ex) {
            LogUtil.d(TAG, "onTouchEvent IllegalArgumentException");
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent arg0) {
//		LogUtil.d(TAG, "dispatchTouchEvent:"+arg0.getAction());
        try {
            if (stopScrollWhenTouch) {
                if (arg0.getAction() == MotionEvent.ACTION_DOWN && isAutoScroll) {
                    isStopByTouch = true;
                    stopAutoScroll();
                } else if ((arg0.getAction() == MotionEvent.ACTION_UP||arg0.getAction()==MotionEvent.ACTION_CANCEL)
                        && isStopByTouch) {
                    startAutoScroll();
                }

            }
            curP.x = arg0.getX();
            curP.y = arg0.getY();
            if (arg0.getAction() == MotionEvent.ACTION_DOWN) {
                // 记录按下时候的坐标
                // 切记不可用 downP = curP ，这样在改变curP的时候，downP也会改变
                downP.x = arg0.getX();
                downP.y = arg0.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (arg0.getAction() == MotionEvent.ACTION_UP) {
                // 在up时判断是否按下和松手的坐标为一个点
            } else if (arg0.getAction() == MotionEvent.ACTION_MOVE) {
                float distanceX = Math.abs(downP.x - curP.x);
                float distanceY = Math.abs(downP.y - curP.y);
//				LogUtil.d(TAG, "distanceX:" + distanceX);
                if ((distanceX > distanceY)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
            }
            return super.dispatchTouchEvent(arg0);
        } catch (IllegalArgumentException ex) {
            // 捕获异常防止crash；
            LogUtil.d(TAG, "dispatchTouchEvent IllegalArgumentException");
        }
        return false;
    }

    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case SCROLL_WHAT:
                    scrollOnce();
                    sendScrollMessage(interval);
                default:
                    break;
            }
        }
    }

    /**
     * scroll only once
     */
    public void scrollOnce() {
        PagerAdapter adapter = getAdapter();
        int currentItem = getCurrentItem();
        int totalCount;
        if (adapter == null || (totalCount = adapter.getCount()) <= 1) {
            return;
        }

        int nextItem = (direction == LEFT) ? --currentItem : ++currentItem;
        if (nextItem < 0) {
            if (isCycle) {
                setCurrentItem(totalCount - 1, isBorderAnimation);
            }
        } else if (nextItem == totalCount) {
            if (isCycle) {
                setCurrentItem(0, isBorderAnimation);
            }
        } else {
            setCurrentItem(nextItem, true);
        }
    }

    /**
     * start auto scroll, first scroll delay time is {@link #getInterval()}
     */
    public void startAutoScroll() {
        isAutoScroll = true;
        sendScrollMessage(interval);
    }

    /**
     * start auto scroll
     *
     * @param delayTimeInMills
     *            first scroll delay time
     */
    public void startAutoScroll(int delayTimeInMills) {
        isAutoScroll = true;
        sendScrollMessage(delayTimeInMills);
    }

    /**
     * stop auto scroll
     */
    public void stopAutoScroll() {
        isAutoScroll = false;
        handler.removeMessages(SCROLL_WHAT);
    }

    private void sendScrollMessage(long delayTimeInMills) {
        /** remove messages before, keeps one message is running at most **/
        handler.removeMessages(SCROLL_WHAT);
        handler.sendEmptyMessageDelayed(SCROLL_WHAT, delayTimeInMills);
    }

    public long getInterval() {
        return interval;
    }

    /**
     * set auto scroll time in milliseconds, default is
     * {@link #DEFAULT_INTERVAL}
     *
     * @param interval
     *            the interval to set
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * get auto scroll direction
     *
     * @return {@link #LEFT} or {@link #RIGHT}, default is {@link #RIGHT}
     */
    public int getDirection() {
        return (direction == LEFT) ? LEFT : RIGHT;
    }

    /**
     * set auto scroll direction
     *
     * @param direction
     *            {@link #LEFT} or {@link #RIGHT}, default is {@link #RIGHT}
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
