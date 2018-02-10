package com.hb.thememanager.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import com.hb.thememanager.R;
import android.content.res.TypedArray;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by alexluo on 17-7-18.
 */

public class DownloadProgressBar extends Button {

    private static int DEFAULT_BACKGROUND_COLOR = 0xFF06C012;

    private static int DEFAULT_SECOND_BACKGROUND_COLOR = 0xFFCBCBCB;


    //背景画笔
    private Paint mBackgroundPaint;
    //按钮文字画笔
    private volatile Paint mTextPaint;

    //背景颜色
    private int mBackgroundColor;
    //下载中后半部分后面背景颜色
    private int mBackgroundSecondColor;
    //文字颜色
    private int mTextColor;
    //覆盖后颜色
    private int mTextCoverColor;

    private float mButtonRadius;
    //边框宽度
    private float mBorderWidth;

    private float mProgress = 0;
    private float mToProgress;
    private int mMaxProgress;
    private int mMinProgress;
    private float mProgressPercent;


    private float mTextRightBorder;
    private float mTextBottomBorder;
    //点的间隙
    private float mBallSpacing = 4;
    //点的半径
    private RectF mBackgroundBounds;
    private LinearGradient mProgressTextGradient;

    //下载平滑动画
    private ValueAnimator mProgressAnimation;

    //记录当前文字
    private CharSequence mCurrentText = "";


    private int mState;

    private int mTextSize;


    public DownloadProgressBar(Context context) {
        this(context, null);
    }

    public DownloadProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DownloadProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
            initAttrs(context, attrs);
            init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DownloadProgressBar);
        try {
            mBackgroundColor = a.getColor(R.styleable.DownloadProgressBar_progressColor, DEFAULT_BACKGROUND_COLOR);
            mBackgroundSecondColor = a.getColor(R.styleable.DownloadProgressBar_secondProgressColor, DEFAULT_SECOND_BACKGROUND_COLOR);
            mButtonRadius = a.getDimension(R.styleable.DownloadProgressBar_backgroundRadius, 0);
            mTextColor = a.getColor(R.styleable.DownloadProgressBar_progressTextColor, mBackgroundColor);
            mTextCoverColor = a.getColor(R.styleable.DownloadProgressBar_protressTextCoverColor, Color.WHITE);
        } finally {
            a.recycle();
        }
        mTextSize = getResources().getDimensionPixelSize(R.dimen.download_progress_text_size);
    }

    private void init() {

        mMaxProgress = 100;
        mMinProgress = 0;
        mProgress = 0;


        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundBounds = new RectF();
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);


        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        mProgressPercent = mProgress / (mMaxProgress + 0f);
        drawing(canvas);
        super.onDraw(canvas);
    }

    private void drawing(Canvas canvas) {
        drawBackground(canvas);
        drawText(canvas);
    }

    private void drawText(Canvas canvas){
        //计算Baseline绘制的Y坐标
        final float y = canvas.getHeight() / 2 - (mTextPaint.descent() / 2 + mTextPaint.ascent() / 2);
        if (mCurrentText == null) {
            mCurrentText = "";
        }
        final float textWidth = mTextPaint.measureText(mCurrentText.toString());
        mTextBottomBorder = y;
        mTextRightBorder = (getMeasuredWidth() + textWidth) / 2;
        mTextPaint.setColor(mTextCoverColor);
        canvas.drawText(mCurrentText.toString(), (getMeasuredWidth() - textWidth) / 2, y, mTextPaint);

    }

    private void drawBackground(Canvas canvas) {

        int width = getWidth();
        int height = getHeight();
        mBackgroundBounds.left =  0;
        mBackgroundBounds.top =  0;
        mBackgroundBounds.right = width;
        mBackgroundBounds.bottom = height;
        final float progressRight = width * mProgressPercent;
        int layerId = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
        mBackgroundPaint.setColor(mBackgroundSecondColor);
        canvas.drawRoundRect(mBackgroundBounds,mButtonRadius,mButtonRadius,mBackgroundPaint);
        mBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        mBackgroundPaint.setColor(mBackgroundColor);

        canvas.drawRect(mBackgroundBounds.left,mBackgroundBounds.top,progressRight,mBackgroundBounds.bottom,mBackgroundPaint);
        mBackgroundPaint.setXfermode(null);
        canvas.restoreToCount(layerId);

    }









    public int getState() {
        return mState;
    }

    public void setState(int state) {
        if (mState != state) {//状态确实有改变
            this.mState = state;
            invalidate();
        }

    }

    /**
     * 设置当前按钮文字
     */
    public void setCurrentText(CharSequence charSequence) {
        mCurrentText = charSequence;
        invalidate();
    }

    public void setCurrentText(int resId){
        setCurrentText(getResources().getString(resId));
    }





    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        this.mProgress = progress;
        invalidate();
    }

    public void setProgress(String progressText,float progress){
        mCurrentText = progressText;
        setProgress(progress);
    }



    public int getMinProgress() {
        return mMinProgress;
    }

    public void setMinProgress(int minProgress) {
        mMinProgress = minProgress;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
    }




}
