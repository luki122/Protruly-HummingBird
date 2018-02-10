package com.protruly.music.widget;

/**
 * Created by hujianwei on 17-8-31.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import com.protruly.music.util.LogUtil;

public class HBLinearLayout extends LinearLayout {

    private static final String TAG ="HBLinearLayout";
    private int mTouchSlop;
    private boolean shouldClicked=false;
    public HBLinearLayout(Context context) {
        this(context, null);

    }

    public HBLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public HBLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        shouldClicked=false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int x = (int) event.getX();
                int y = (int) event.getY();
                setPressed((x >= -this.mTouchSlop)
                        && (x < getWidth() + this.mTouchSlop)
                        && (y >= -this.mTouchSlop)
                        && (y < getHeight() + this.mTouchSlop));
                break;
            case MotionEvent.ACTION_UP:
                LogUtil.d(TAG, "ACTION_UP "+isPressed()+"--shouldClicked:"+shouldClicked);
                if (isPressed()) {
                    if(shouldClicked){
                        performClick();
                    }
                    setPressed(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                LogUtil.d(TAG, "ACTION_CANCEL "+isPressed());
                if (isPressed()) {
                    setPressed(false);
                }
                break;
        }
        if(shouldClicked){
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setPressed(boolean pressed) {
        LogUtil.d(TAG, "setPressed "+isPressed()+"---pressed:"+pressed);
        if (pressed != isPressed()) {
            if (pressed) {
                setAlpha(0.6f);
            } else {
                setAlpha(1.0f);
            }
        }
        super.setPressed(pressed);
    }

    public void setShouldClicked(boolean is){
        shouldClicked=is;
    }
}

