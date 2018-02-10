package com.android.deskclock.stopwatch;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class SWCircleTimerView extends View {

    private final long intervalTime = 2000l;

    private ValueAnimator percentAnimator;
    private float percent = 0;

    private static float mStrokeSize = 4;
    private static float mDotRadius = 6;
    private final Paint mFill = new Paint();
    private float mRadiusOffset;   // amount to remove from radius to account for markers on circle

    @SuppressWarnings("unused")
    public SWCircleTimerView(Context context) {
        this(context, null);
    }

    public SWCircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void reset() {
        percentAnimator.cancel();
        percent = 0;
        postInvalidate();
    }

    public void startIntervalAnimation() {
        if (percentAnimator.isStarted()) {
            percentAnimator.resume();
        } else {
            percentAnimator.start();
        }
    }

    public void stopIntervalAnimation() {
        percentAnimator.pause();
    }

    private void init(Context c) {
        Resources resources = c.getResources();
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        float dotDiameter = resources.getDimension(R.dimen.stopwatch_dot_size);
        mRadiusOffset = dotDiameter / 2f;
        mFill.setAntiAlias(true);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(c.getColor(R.color.stopwatch_dot_color));
        mDotRadius = dotDiameter / 2f;

        percentAnimator = ValueAnimator.ofFloat(0, 1f);
        percentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                percent = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        percentAnimator.setDuration(intervalTime);
        percentAnimator.setInterpolator(new LinearInterpolator());
        percentAnimator.setRepeatCount(ValueAnimator.INFINITE);
        percentAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int xCenter = getWidth() / 2 + 1;
        int yCenter = getHeight() / 2;

        float radius = Math.min(xCenter, yCenter) - mRadiusOffset;

        drawRedDot(canvas, percent, xCenter, yCenter, radius);
   }

    protected void drawRedDot(
            Canvas canvas, float degrees, int xCenter, int yCenter, float radius) {

        float dotPercent = 270 + 360 * degrees;

        final double dotRadians = Math.toRadians(dotPercent);

        canvas.drawCircle(xCenter + (float) (radius * Math.cos(dotRadians)),
                yCenter + (float) (radius * Math.sin(dotRadians)), mDotRadius, mFill);
    }

}
