package com.android.launcher3.colors;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.launcher3.R;

/**
 * Created by lijun on 17-4-6.
 */

public class ChameleonTextView extends TextView implements ColorManager.IWallpaperChange {

    private ColorStateList mDarkColor, mLightColor;

    public ChameleonTextView(Context context) {
        this(context, null);
    }

    public ChameleonTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChameleonTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ChameleonTextView, defStyleAttr, 0);
        mDarkColor = a.getColorStateList(R.styleable.ChameleonTextView_dark_color);
        mLightColor = a.getColorStateList(R.styleable.ChameleonTextView_light_color);
        a.recycle();
    }

    @Override
    public void onWallpaperChange() {
        //do nothing
    }

    @Override
    public void onColorChange(int[] colors) {
        if (mDarkColor != null && mLightColor != null) {
            if (ColorManager.getInstance().isBlackText()) {
                setTextColor(mDarkColor);
            } else {
                setTextColor(mLightColor);
            }
        } else {
            this.setTextColor(colors[0]);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mDarkColor != null && mLightColor != null) {
            if (ColorManager.getInstance().isBlackText()) {
                setTextColor(mDarkColor);
            } else {
                setTextColor(mLightColor);
            }
        } else {
            this.setTextColor(ColorManager.getInstance().getColors()[0]);
        }
        ColorManager.getInstance().addWallpaperCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ColorManager.getInstance().removeWallpaperCallback(this);
    }
}
