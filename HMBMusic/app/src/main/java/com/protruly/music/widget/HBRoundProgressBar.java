package com.protruly.music.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.protruly.music.R;


/**
 * Created by hujianwei on 17-8-30.
 */

public class HBRoundProgressBar extends View {
    private Paint paint;
    private int roundColor;
    private int roundProgressColor;
    private float roundMargin;

    private float roundWidth;

    private long max;

    private long progress;
    private int style;

    public static final int STROKE = 0;
    public static final int FILL = 1;

    public HBRoundProgressBar(Context context) {
        this(context, null);
    }

    public HBRoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HBRoundProgressBar(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();

        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.hb_RoundProgressBar);

        roundColor = mTypedArray.getColor(
                R.styleable.hb_RoundProgressBar_roundColor, Color.RED);
        roundProgressColor = mTypedArray.getColor(
                R.styleable.hb_RoundProgressBar_roundProgressColor,
                Color.GREEN);
        roundWidth = mTypedArray.getDimension(
                R.styleable.hb_RoundProgressBar_roundWidth, 5);
        roundMargin = mTypedArray.getDimension(
                R.styleable.hb_RoundProgressBar_roundMargin, 6);
        style = mTypedArray
                .getInt(R.styleable.hb_RoundProgressBar_style, 0);

        mTypedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(max==0){
            return;
        }
        int centre = getWidth() / 2;
        int radius = (int) (centre - roundWidth / 2 - roundMargin);

        paint.setColor(roundColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(roundWidth);
        paint.setAntiAlias(true);
        paint.setColor(roundProgressColor);
        RectF oval = new RectF(centre - radius, centre - radius, centre + radius, centre + radius);

        switch (style) {
            case STROKE: {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawArc(oval, -90, 360 * progress / max, false, paint);
                break;
            }
            case FILL: {
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                if (progress != 0)
                    canvas.drawArc(oval, -90, 360 * progress / max, true, paint);
                break;
            }
        }

    }

    public synchronized long getMax() {
        return max;
    }

    public synchronized void setMax(long max) {
        if (max < 0) {
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
    }

    public synchronized long getProgress() {
        return progress;
    }

    public synchronized void setProgress(long progress) {
        if (progress < 0) {
            throw new IllegalArgumentException("progress not less than 0");
        }
        if (progress >= max) {
            progress = 0;
        }
        if (progress < max) {
            this.progress = progress;
            postInvalidate();
        }

    }

    public int getCricleColor() {
        return roundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.roundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return roundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.roundProgressColor = cricleProgressColor;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }
}
