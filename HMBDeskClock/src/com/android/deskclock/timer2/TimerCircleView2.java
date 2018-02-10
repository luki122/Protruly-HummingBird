/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.timer2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.android.deskclock.R;
import com.android.deskclock.data.Timer;

/**
 * Custom view that draws timer progress as a circle.
 */
public final class TimerCircleView2 extends View {

    private static final float MARK_DEGREE = 0.9f;
    private static final float CELL_DEGREE = 3.6f;

    private static final int CELL_COUNT = 100;

    /** An amount to subtract from the true radius to account for drawing thicknesses. */
    private final float mRadiusOffset;

    private final int mGrayColor;
    private final int mRedColor;

    private final Paint mPaint = new Paint();
    private final RectF mArcRect = new RectF();

    private Timer mTimer;

    @SuppressWarnings("unused")
    public TimerCircleView2(Context context) {
        this(context, null);
    }

    public TimerCircleView2(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources resources = context.getResources();

        float strokeSize = resources.getDimension(R.dimen.timer_circle_stroke_size);
        mRadiusOffset = strokeSize / 2;

        mGrayColor = resources.getColor(R.color.timer_cricle_gray, null);
        mRedColor = resources.getColor(R.color.timer_cricle_red, null);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeSize);
    }

    void update(Timer timer) {
        if (mTimer != timer) {
            mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTimer == null) {
            return;
        }

        if (mTimer.isReset()) {
            drawTimerView(canvas, 0);
        } else if (mTimer.isExpired()) {
            drawTimerView(canvas, 100);
        } else {
            float redPercent = Math.min(1,
                    (float) mTimer.getElapsedTime() / (float) mTimer.getTotalLength());
            int splitCount = (int) (redPercent * CELL_COUNT);
            drawTimerView(canvas, splitCount);
        }

        if (mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    private void drawTimerView(Canvas canvas, int splitCount) {
        final int xCenter = getWidth() / 2;
        final int yCenter = getHeight() / 2;
        final float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        mArcRect.top = yCenter - radius;
        mArcRect.bottom = yCenter + radius;
        mArcRect.left = xCenter - radius;
        mArcRect.right = xCenter + radius;

        mPaint.setColor(mRedColor);
        for (int i = 0; i < CELL_COUNT; i++) {
            if (i == splitCount) {
                mPaint.setColor(mGrayColor);
            }
            canvas.drawArc(mArcRect, -90.0f - MARK_DEGREE / 2 + CELL_DEGREE * i, MARK_DEGREE,
                    false, mPaint);
        }
    }
}
