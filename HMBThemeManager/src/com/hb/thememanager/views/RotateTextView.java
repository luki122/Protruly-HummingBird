package com.hb.thememanager.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import com.hb.thememanager.R;

/**
 * Created by alexluo on 17-8-16.
 */

public class RotateTextView extends TextView {
    private static final int DEFAULT_DEGREES = 0;
    private int mDegrees;
    private Paint mBackgroundPaint;
    private int mBackgroundColor;
    private int mSolidWidth;
    public RotateTextView(Context context) {
        super(context, null);
    }

    public RotateTextView(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.textViewStyle);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RotateTextView);
        mDegrees = a.getInteger(R.styleable.RotateTextView_degree,
                DEFAULT_DEGREES);
        mBackgroundColor = a.getColor(R.styleable.RotateTextView_degreeBackgroundColor,Color.RED);
        mSolidWidth = a.getDimensionPixelOffset(R.styleable.RotateTextView_solidSize,100);
        a.recycle();
        mBackgroundPaint.setColor(mBackgroundColor);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(mSolidWidth, 0);
        path.lineTo(0, mSolidWidth);
        path.close();
        canvas.drawPath(path, mBackgroundPaint);
        canvas.save();
        canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());
        canvas.rotate(mDegrees, this.getWidth() / 2f, this.getHeight() / 2f);

        super.onDraw(canvas);
        canvas.restore();

    }

    public void setDegrees(int degrees) {
        mDegrees = degrees;
    }
}
