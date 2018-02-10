package cn.com.protruly.soundrecorder.clip;

import android.graphics.Canvas;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-9-4.
 */

public abstract class Animation {

    private static final String TAG = "Animation";


    private boolean mIsStarted;

    private long mStartTime;
    private static final int DURATION = 250;
    private Interpolator mInterpolator;

    private AnimationListener mAnimationListener;
    private int mAnimType;
    public static final int ANIM_TYPE_ZOOM_IN = 1;
    public static final int ANIM_TYPE_ZOOM_OUT = 2;

    private float mProgress;

    public Animation(int animType) {
        mAnimType = animType;
    }

    public void start() {
        if(!mIsStarted) {
            mStartTime = System.currentTimeMillis();
            mIsStarted = true;
        }
    }

    public int getType() {
        return mAnimType;
    }

    public void stop() {
        mIsStarted = false;
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public boolean isActive() {
        return isStarted() && getProgress() < 1.0f;
    }

    protected float calculateProgress() {
        if(!isStarted()) {
            throw new IllegalStateException("start animation before calculating progress");
        }
        //float progress = mInterpolator.getInterpolation();
        long now = System.currentTimeMillis();
        float progress = (float)(now - mStartTime) / (float)DURATION;
        if(mInterpolator != null) {
            progress = mInterpolator.getInterpolation(progress);
        }
        //LogUtil.i(TAG, "calculateProgress:" + progress);
        if(now >= mStartTime + DURATION) return 1.0f;
        return progress;
    }

    public void setAnimationListener(AnimationListener listener) {
        mAnimationListener = listener;
    }

    /**
     * override this function to apply animation
     * @param canvas
     * @return true indicates more frame to draw, false indicates animation ends.
     */
    public boolean draw(Canvas canvas) {
        mProgress = calculateProgress();
        onDraw(canvas);
        if(mProgress < 1.0f) {
            //LogUtil.i(TAG, "......................11111111: " + mProgress);
            return true;
        }
        if(mProgress == 1.0f) {
            //LogUtil.i(TAG, "......................1111111111111111111!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!END");
            mAnimationListener.onAnimationEnd(mAnimType);
        }
        return false;
    }

    public float getProgress() {
        return mProgress;
    }

    public abstract boolean onDraw(Canvas canvas);

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public interface AnimationListener {
        void onAnimationStart(int animType);
        void onAnimationEnd(int animType);
    }
}