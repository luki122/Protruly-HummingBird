/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.pageindicators;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherViewPropertyAnimator;
import com.android.launcher3.R;
import com.android.launcher3.colors.ColorManager;

/**
 * created by lijun 2017.3.30
 */
public class PageIndicatorUnderline extends PageIndicator implements ColorManager.IWallpaperChange {

    private static int APPEAR_ANIMATE_DURATION = 100;
    private static int DISAPPEAR_ANIMATE_DURATION = 1500;
    private int mActivePage;

    private int pageindicatorHeightPx;
    private int pageindicatorWidthPx;
    private int pageindicatorHandlerWidthPx;

    private RectF indicatorHandlerRect = new RectF();

    private float drawLeft = -1;
    private float drawCount = -1;

    private static int SCROLL_STATE_NORMAL = 0;
    private static int SCROLL_STATE_SCROLLING = 1;
    private static int SCROLL_STATE_ANIMATING = 2;
    private int scrollState = SCROLL_STATE_NORMAL;

    Animator mAnimator;

    private Paint mPaint;
    private int mLineColor;

    public PageIndicatorUnderline(Context context) {
        this(context, null);
    }

    public PageIndicatorUnderline(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorUnderline(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setCaretDrawable(new CaretDrawable(context));//lijun add for pageindicator
        mPaint = new Paint();
        mPaint.setAlpha(127);
    }

    public void animateToAlpha(final float finalAlpha) {
        if (scrollState == SCROLL_STATE_ANIMATING) {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
        }
        final boolean toShow = finalAlpha == 1.0;
        scrollState = SCROLL_STATE_ANIMATING;
        mAnimator = new LauncherViewPropertyAnimator(this)
                .alpha(finalAlpha)
                .setDuration(toShow ? APPEAR_ANIMATE_DURATION : DISAPPEAR_ANIMATE_DURATION);
        if (toShow) {
            mAnimator.setInterpolator(new DecelerateInterpolator(0.5f));
        } else {
            mAnimator.setInterpolator(new DecelerateInterpolator(2));
        }
        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                scrollState = (finalAlpha == 0) ? SCROLL_STATE_NORMAL : SCROLL_STATE_SCROLLING;
            }
        });
        mAnimator.start();
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        scrollState = SCROLL_STATE_SCROLLING;

        float left = currentScroll / (float) totalScroll * (pageindicatorWidthPx - pageindicatorHandlerWidthPx);
        if (drawLeft != left || drawCount != mNumPages) {

            indicatorHandlerRect.left = left;
            drawLeft = left;
            drawCount = mNumPages;
            indicatorHandlerRect.right = indicatorHandlerRect.left + pageindicatorHandlerWidthPx;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        canvas.drawRoundRect(indicatorHandlerRect, pageindicatorHeightPx / (float) 2, pageindicatorHeightPx / (float) 2, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        pageindicatorHeightPx = idp.portraitProfile.widgetPagedviewIndicatorHeightPx;
        pageindicatorWidthPx = idp.portraitProfile.widthPx;

        indicatorHandlerRect.top = 0;
        indicatorHandlerRect.bottom = pageindicatorHeightPx;
    }

    @Override
    public void setActiveMarker(int activePage) {
        if (mActivePage != activePage) {
            mActivePage = activePage;
        }
    }

    @Override
    protected void onPageCountChanged() {
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        pageindicatorHeightPx = idp.portraitProfile.widgetPagedviewIndicatorHeightPx;
        pageindicatorWidthPx = idp.portraitProfile.widthPx;
        if (mNumPages > 1) {
            pageindicatorHandlerWidthPx = pageindicatorWidthPx / mNumPages;
        }
        requestLayout();
    }

    @Override
    public void onWallpaperChange() {

    }

    @Override
    public void onColorChange(int[] colors) {
        if (ColorManager.getInstance().isBlackText()) {
            mLineColor = getResources().getColor(R.color.page_indicator_dot_light_color_active);
        } else {
            mLineColor = getResources().getColor(R.color.page_indicator_dot_color_active);
        }
        mPaint.setColor(mLineColor);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ColorManager.getInstance().isBlackText()) {
            mLineColor = getResources().getColor(R.color.page_indicator_dot_light_color_active);
        } else {
            mLineColor = getResources().getColor(R.color.page_indicator_dot_color_active);
        }
        mPaint.setColor(mLineColor);
        invalidate();
        ColorManager.getInstance().addWallpaperCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ColorManager.getInstance().removeWallpaperCallback(this);
    }

}
