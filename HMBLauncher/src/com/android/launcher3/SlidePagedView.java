

package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.widget.Scroller;


public abstract class SlidePagedView extends PagedView {
    private static final float SMOOTHING_SPEED = 0.75f;
    private static final float SMOOTHING_CONSTANT = (float) (0.016 / Math.log(SMOOTHING_SPEED));

    private float mBaseLineFlingVelocity;
    private float mFlingVelocityInfluence;

    static final int DEFAULT_MODE = 0;
    static final int X_LARGE_MODE = 1;

    int mScrollMode;

    private Interpolator mScrollInterpolator;
	protected int mPageAnimType = 0;

    public static class OvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 1.3f;
        private float mTension;

        public OvershootInterpolator() {
            mTension = DEFAULT_TENSION;
        }

        public void setDistance(int distance) {
            mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
        }

        public void disableSettle() {
            mTension = 0.f;
        }

        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public SlidePagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public SlidePagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mScrollMode = getScrollMode();

        // This means that we'll take care of updating the scroll parameter ourselves (we do it
        // in computeScroll), we only do this in the OVERSHOOT_MODE, ie. on phones

        initMode();
    }

    final protected int getScrollMode() {
        return X_LARGE_MODE;
    }

    /**
     * Initializes various states for this workspace.
     */
    protected void initMode() {
        if (mScrollMode == DEFAULT_MODE) {
            mBaseLineFlingVelocity = 2500.0f;
            mFlingVelocityInfluence = 0.4f;
            mScrollInterpolator = new OvershootInterpolator();
        }
    }

    @Override
    protected void snapToDestination() {
        if (mScrollMode == X_LARGE_MODE) {
            super.snapToDestination();
        } else {
            snapToPageWithVelocity(getPageNearestToCenterOfScreen(), 0);
        }
    }

    @Override
    protected void snapToPageWithVelocity(int whichPage, int velocity) {
        if (mScrollMode == X_LARGE_MODE) {
            super.snapToPageWithVelocity(whichPage, velocity);
        } else {
            snapToPageWithVelocity(whichPage, 0, true);
        }
    }

    private void snapToPageWithVelocity(int whichPage, int velocity, boolean settle) {
            // if (!mScroller.isFinished()) return;

        whichPage = Math.max(0, Math.min(whichPage, getChildCount() - 1));

        final int screenDelta = Math.max(1, Math.abs(whichPage - mCurrentPage));

        final int newX = getScrollForPage(whichPage); 
        int delta = newX - getUnboundedScrollX();
		
        int duration = (screenDelta + 1) * 100;

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        if (settle) {
            ((OvershootInterpolator) mScrollInterpolator).setDistance(screenDelta);
        } else {
            ((OvershootInterpolator) mScrollInterpolator).disableSettle();
        }

        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration += (duration / (velocity / mBaseLineFlingVelocity)) * mFlingVelocityInfluence;
        } else {
            duration += 100;
        }

        snapToPage(whichPage, delta, duration);
    }

    @Override
    public void snapToPage(int whichPage) {
       if (mScrollMode == X_LARGE_MODE) {
           super.snapToPage(whichPage);
       } else {
           snapToPageWithVelocity(whichPage, 0, false);
       }
    }

    @Override
    public void computeScroll() {
        if (mScrollMode == X_LARGE_MODE) {
            super.computeScroll();
        } else {
            boolean scrollComputed = computeScrollHelper();

            if (!scrollComputed && mTouchState == TOUCH_STATE_SCROLLING) {
                final float now = System.nanoTime() / NANOTIME_DIV;
                final float e = (float) Math.exp((now - mSlideTime) / SMOOTHING_CONSTANT);

                final float dx = mTouchX - mUnboundedScrollX;
                scrollTo(Math.round(mUnboundedScrollX + dx * e), getScrollY());
                mSlideTime = now;

                // Keep generating points as long as we're more than 1px away from the target
                if (dx > 1.f || dx < -1.f) {
                    invalidate();
                }
            }
        }
    }	
}
