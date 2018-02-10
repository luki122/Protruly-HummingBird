package com.protruly.music.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.protruly.music.R;

/**
 * Created by hujianwei on 17-8-31.
 */

public class BackView extends View {
    private Paint paint;
    int centre = 0;
    int width;
    int hight;
    int margin;
    int oldColor;
    Activity mActivity;

    public BackView(Context context) {
        this(context, null);
        // TODO Auto-generated constructor stub
        mActivity = (Activity) context;
    }

    public BackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        // TODO Auto-generated constructor stub
        mActivity = (Activity) context;
    }

    public BackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
        mActivity = (Activity) context;
        paint = new Paint();
        paint.setColor(Color.WHITE);
        oldColor = Color.WHITE;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((float)(mActivity.getResources().getDimension(
                R.dimen.hb_Stroke_width)));
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        width = getWidth();
        hight = getHeight();
        centre = width / 2;
        float start = mActivity.getResources().getDimension(
                R.dimen.hb_backview_start);
        float r = 0;
        float y = 0;
        if ((mActivity.getResources()
                .getDimensionPixelSize(R.dimen.hb_action_bar_height))
                / (mActivity.getResources().getDisplayMetrics().scaledDensity) == 43) {
            r = mActivity.getResources().getDimension(
                    R.dimen.hb_backview_width);
            y = mActivity.getResources().getDimension(
                    R.dimen.hb_backview_height);
            paint.setStrokeWidth((float)(mActivity.getResources().getDimension(
                    R.dimen.hb_Stroke_width) + 0.5));
        } else {
            r = mActivity.getResources().getDimension(
                    R.dimen.hb_backview_width2);
            y = mActivity.getResources().getDimension(
                    R.dimen.hb_backview_height2);
        }
        canvas.drawLine(start - 1, (hight / 2) + 1, start + r, (hight / 2) - y,
                paint);
        canvas.drawLine(
                mActivity.getResources().getDimension(
                        R.dimen.hb_backview_start), (hight / 2), start + r,
                (hight / 2) + y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        if (event.getAction() == MotionEvent.ACTION_DOWN && mActivity != null) {
            paint.setColor(getResources().getColor(
                    R.color.hb_download_cicrle));
            setBackgroundResource(R.drawable.hb_playing_press);
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP
                && event.getX() <= ((View) getParent()).getWidth()
                && event.getY() <= ((View) getParent()).getHeight()
                && event.getY() >= 0 && event.getX() > 0) {
            mActivity.finish();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP
                || event.getX() > ((View) getParent()).getWidth()
                || event.getX() < 0
                || event.getY() > ((View) getParent()).getHeight()
                || event.getY() < 0) {
            paint.setColor(oldColor);
            setBackground(null);
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void changeColor(int color) {
        paint.setColor(color);
        oldColor = color;
        invalidate();
    }
}

