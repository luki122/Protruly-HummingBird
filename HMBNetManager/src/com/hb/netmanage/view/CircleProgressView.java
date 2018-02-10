package com.hb.netmanage.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hb.netmanage.R;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.StringUtil;

/**
 * Created by zhaolaichao on 17-5-3.
 *
 * 绘制圆形进度条
 */

public class CircleProgressView extends View {
    private static final String TAG = "CircleProgressBar";
    //需要执行动画的参数名
    private static final String PROGRESS_PROPERTY = "progress";

    /**
     * 初始化圆弧色
     */
    public static int ARC_COLOR = Color.rgb(195, 195, 195);
    /**
     * 绿色
     */
    public static int COMMON_ARC_COLOR = Color.rgb(6, 192, 18);
    /**
     * 橙色
     */
    public static int WARN_ARC_COLOR = Color.rgb(255, 138, 44);
    /**
     *#FFF45454
     */
    public static int OVER_ARC_COLOR = Color.rgb(255, 69, 70);

    public static int TEXT_COLOR_DEFAULT = Color.rgb(0, 0, 0);
    /**
     * 文字1颜色
     */
    private int mText1Color = TEXT_COLOR_DEFAULT;
    private int mText2Color = TEXT_COLOR_DEFAULT;
    private float mText1Size = 10;
    private float mText2Size = 10;
    private int mMaxProgress = 100;

    private float mProgress = 0;
    private float mWarnProgress = 0;
    private float mUsedProgress = 0;
    private long mUsedData = 0;
    private int mColor = ARC_COLOR;

    private float mCircleLineStrokeWidth = 16;

    private final int mTxtStrokeWidth = 2;

    // 画圆所在的距形区域
    private RectF mRectF;

    private Paint mPaint;

    private Context mContext;

    private String mTxtHint2Default = "--" ;
    private String mTxtHint1 = mTxtHint2Default;

    private String mTxtHint2 = mTxtHint2Default;

    private long mTotalData;
    private long mRemainData;

    /**
     * 超额开关状态
     */
    private boolean mIsWarnState;
    private boolean mIsAniming;

    public CircleProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
        mRectF = new RectF();
        mPaint = new Paint();
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleProgressbar, 0 , 0);
        mCircleLineStrokeWidth = typeArray.getDimension(R.styleable.CircleProgressbar_roundWidth, mCircleLineStrokeWidth);
        mText1Color = typeArray.getColor(R.styleable.CircleProgressbar_text1Color, TEXT_COLOR_DEFAULT);
        mText2Color = typeArray.getColor(R.styleable.CircleProgressbar_text2Color, TEXT_COLOR_DEFAULT);
        mText1Size = typeArray.getDimension(R.styleable.CircleProgressbar_text1Size, mText1Size);
        mText2Size = typeArray.getDimension(R.styleable.CircleProgressbar_text2Size, mText2Size);
        mColor = ARC_COLOR;
    }


    public CircleProgressView(Context context) {
        super(context);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();

        if (width != height) {
            int min = Math.min(width, height);
            width = min;
            height = min;
        }

        // 设置画笔相关属性
        mPaint.setAntiAlias(true);
        mPaint.setColor(ARC_COLOR);
        canvas.drawColor(Color.TRANSPARENT);
        mPaint.setStrokeWidth(mCircleLineStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        // 位置
        mRectF.left = mCircleLineStrokeWidth / 2; // 左上角x
        mRectF.top = mCircleLineStrokeWidth / 2; // 左上角y
        mRectF.right = width - mCircleLineStrokeWidth / 2; // 左下角x
        mRectF.bottom = height - mCircleLineStrokeWidth / 2; // 右下角y

        // 绘制圆圈，进度条背景
        canvas.drawArc(mRectF, -90, 360, false, mPaint);
        long updateData = mTotalData - (long)(mTotalData *(mProgress / mProgress));
        float currentProgress = -((float) mProgress / mMaxProgress) * 360;
        if (mTotalData > 0) {
            //初始化100%流量剩余
            mPaint.setColor(COMMON_ARC_COLOR);
            canvas.drawArc(mRectF, -90, 360, false, mPaint);
            if (mWarnProgress >= mUsedProgress && mUsedProgress > 0) {
                //已用流量
                mPaint.setColor(ARC_COLOR);
                canvas.drawArc(mRectF, -90, -((float) mProgress / mMaxProgress) * 360, false, mPaint);
                boolean isOver = false; //是否超出
                if (!mIsWarnState && mProgress > 0) {
                    if (mTotalData < mUsedData) {
                        //本月流量已超出
                        mTxtHint1 = StringUtil.formatDataFlowSize(mContext, Math.abs(mTotalData - mUsedData));
                        mPaint.setColor(mColor);
                        canvas.drawArc(mRectF, -90, 360, false, mPaint);
                        isOver = true;
                    } else {
                        if (mRemainData > 0 && mRemainData < mTotalData * 1 / 100 && mProgress == 100) {
                            //剩余流量小于1%时则显示1%
                            mPaint.setColor(mColor);
                            canvas.drawArc(mRectF, -90, -((float) 1 / mMaxProgress) * 360, false, mPaint);
//                            currentProgress = 359;
//                            canvas.drawArc(mRectF, -90, currentProgress, false, mPaint);
                        }
                    }
                }
                if (!isOver) {
                    if (updateData  <= mRemainData) {
                        //减少偏差
                        mTxtHint1 = StringUtil.formatDataFlowSize(mContext, mRemainData);
                    } else {
                        mTxtHint1 = StringUtil.formatDataFlowSize(mContext, mTotalData);
                    }
                }
            } else if (mWarnProgress < mUsedProgress && mUsedProgress <= 100 && mTotalData - mUsedData >= 0) {
                if (mProgress <= mWarnProgress) {
                    mPaint.setColor(COMMON_ARC_COLOR);
                    canvas.drawArc(mRectF, -mProgress, 360, false, mPaint);
                } else {
                    //实际剩余流量
                    mPaint.setColor(mColor);
                    if (mTotalData - mRemainData > 0 && (mRemainData > 0 && mRemainData < mTotalData * 1 / 100) && mProgress == 100) {
                        //剩余流量小于1%时则显示1%
                        canvas.drawArc(mRectF, -90, -((float) 1 / mMaxProgress) * 360, false, mPaint);
                        currentProgress = 359;
                    } else {
                        canvas.drawArc(mRectF, -mProgress, 360, false, mPaint);
                    }
                }
                //已用流量
                mPaint.setColor(ARC_COLOR);
                canvas.drawArc(mRectF, -90, currentProgress, false, mPaint);
                if (updateData  <= mRemainData) {
                    //减少偏差
                    mTxtHint1 = StringUtil.formatDataFlowSize(mContext, mRemainData);
                } else {
                    mTxtHint1 = StringUtil.formatDataFlowSize(mContext, mTotalData);
                }
            } else {
                //本月流量已超出
                mTxtHint1 = StringUtil.formatDataFlowSize(mContext, mTotalData);
                if (mProgress > 0) {
                    mTxtHint1 = StringUtil.formatDataFlowSize(mContext, Math.abs(mTotalData - mUsedData));
                    mPaint.setColor(mColor);
                    canvas.drawArc(mRectF, -90, 360, false, mPaint);
                }
            }
        } else {
            //默认为0
            mTxtHint1 = StringUtil.formatDataFlowSize(mContext, 0);
        }
        // 绘制进度文案显示
        int textWidth = 0;
        if (!TextUtils.isEmpty(mTxtHint1)) {
            mPaint.setStrokeWidth(mTxtStrokeWidth);
            mPaint.setTextSize(mText1Size);
            if (mProgress == 0) {
                mPaint.setColor(TEXT_COLOR_DEFAULT);
            } else {
                mPaint.setColor(mText1Color);
            }
            textWidth = (int) mPaint.measureText(mTxtHint1, 0, mTxtHint1.length());
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(mTxtHint1, width / 2 - textWidth / 2, height / 2 - mText1Size / 3, mPaint);
        }
        if (!TextUtils.isEmpty(mTxtHint2)) {
            mPaint.setStrokeWidth(mTxtStrokeWidth);
            mPaint.setTextSize(mText2Size);
            if (mProgress == 0) {
                mTxtHint2Default = mContext.getString(R.string.month_remain);
                mPaint.setColor(TEXT_COLOR_DEFAULT);
            } else {
                mTxtHint2Default = mTxtHint2;
                mPaint.setColor(mText2Color);
            }
            textWidth = (int) mPaint.measureText(mTxtHint2, 0, mTxtHint2.length());
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(mTxtHint2Default, width / 2 - textWidth / 2, height / 2 + mText2Size, mPaint);
        }

    }

    public void doAnimation(boolean isAnimation, float warnProgress, float progress, float usedProgress) {
        this.mWarnProgress = warnProgress;
        this.mUsedProgress = usedProgress;

        if (isAnimation && !mIsAniming) {
            AnimatorSet animation = new AnimatorSet();
            ObjectAnimator progressAnimation = ObjectAnimator.ofFloat(this, PROGRESS_PROPERTY, 0, progress);
            progressAnimation.setDuration(2000);// 动画执行时间
            progressAnimation.setInterpolator(new LinearInterpolator());
            animation.playTogether(progressAnimation);//动画同时执行,可以做多个动画
            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIsAniming = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsAniming = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mIsAniming = false;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                    mIsAniming = true;
                }
            });
            animation.start();
        } else {
            invalidate();
        }
    }

    public int getmColor() {
        return mColor;
    }

    public void setmColor(int mColor) {
        this.mColor = mColor;
    }

    public int getmText2Color() {
        return mText2Color;
    }

    public void setmText2Color(int mText2Color) {
        this.mText2Color = mText2Color;
    }

    public int getmText1Color() {
        return mText1Color;
    }

    public void setmText1Color(int mText1Color) {
        this.mText1Color = mText1Color;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    public void setProgress(float progress) {
        this.mProgress = progress;
        invalidate();
    }

    public void setProgressNotInUiThread(int progress) {
        this.mProgress = progress;
        postInvalidate();
    }

    public String getmTxtHint1() {
        return mTxtHint1;
    }

    public void setmTxtHint1(long totalData, long remainData) {
        mTotalData = totalData;
        mRemainData = remainData;
    }

    public String getmTxtHint2() {
        return mTxtHint2;
    }

    public void setmTxtHint2(String mTxtHint2) {
        this.mTxtHint2 = mTxtHint2;
    }

    public long getmUsedData() {
        return mUsedData;
    }

    public void setmUsedData(long mUsedData) {
        this.mUsedData = mUsedData;
    }

    public boolean ismIsWarnState() {
        return mIsWarnState;
    }

    public void setmIsWarnState(boolean mIsWarnState) {
        this.mIsWarnState = mIsWarnState;
    }
}
