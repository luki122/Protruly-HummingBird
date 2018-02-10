package com.hmb.manager.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hmb.manager.R;

public class HMBProgressBar extends View {
    private static final String TAG = "HMBProgressBar";

    /**
     * 进度条所占用的角度
     */
    private static final int ARC_FULL_DEGREE = 360;
    /**
     * 进度条个数
     */
    private static final int COUNT = 100;
    /**
     * 每个进度条所占用角度
     */
    private static final float ARC_EACH_PROGRESS = ARC_FULL_DEGREE * 1.0f / (COUNT - 1);
    /**
     * 弧线细线条的长度
     */
    private int ARC_LINE_LENGTH;
    /**
     * 弧线细线条的宽度
     */
    private int ARC_LINE_WIDTH;
    /**
     * 组件的宽，高
     */
    private int width, height;
    /**
     * 进度条最大值和当前进度值
     */
    private float max = 100, progress;
    /**
     * 绘制弧线的画笔
     */
    private Paint progressPaint;
    /**
     * 绘制进度的画笔
     */
    private int progressPaintColor;
    /**
     * 绘制刻度的画笔
     */
    private int progressBgPaintColor;
    /**
     * 绘制文字的画笔
     */
    private Paint textPaint;
    /**
     * 绘制文字的画笔
     */
    private String textPaintMsg;
    /**
     * 绘制文字背景圆形的画笔
     */
    private Paint textBgPaint;
    /**
     * 圆弧的半径
     */
    private int circleRadius;
    /**
     * 圆弧圆心位置
     */
    private int centerX, centerY;
    /**
     * 是否显示单位"分"
     */
    private boolean isShowNumberText;
    /**
     * title string
     */
    private String title;
    /**
     * title size
     */
    private int titleSize;
    /**
     * title size
     */
    private int titleColor;
    /**
     * title size
     */
    private int summarySize;
    /**
     * title size
     */
    private int summaryColor;
    /**
     * Animation Duration
     */
    private static final int DURATION = 800;
    /**
     * Is odd round
     */
    private boolean isOddRound = true;
    /**
     * Progress Animation
     */
    private ObjectAnimator progressAnim;


    private static final int DEFAULT_PROGRESS_PAINT_COLOR = Color.parseColor("#71B7C8");
    private static final int DEFAULT_PROGRESS_BG_PAINT_COLOR = Color.parseColor("#88aaaaaa");
    private static final boolean DEFAULT_SHOW_NUMBER_TEXT = true;
    private static final int DEFAULT_TITLE_COLOR = Color.parseColor("#B2000000");
    private static final int DEFAULT_TITLE_SIZE = 300 >> 1;
    private static final int DEFAULT_SUMMARY_COLOR = Color.parseColor("#66000000");
    private static final int DEFAULT_SUMMARY_SIZE = 50;

    public HMBProgressBar(Context context) {
        this(context, null);
    }

    public HMBProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HMBProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        progressPaint = new Paint();
        progressPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);

        textBgPaint = new Paint();
        textBgPaint.setAntiAlias(true);

        TypedArray attributes =
                context.obtainStyledAttributes(attrs, R.styleable.HMBProgressBar, defStyleAttr, 0);
        progressPaintColor = attributes.getColor(R.styleable.HMBProgressBar_hpb_primary_color, DEFAULT_PROGRESS_PAINT_COLOR);
        progressBgPaintColor = attributes.getColor(R.styleable.HMBProgressBar_hpb_bg_color, DEFAULT_PROGRESS_BG_PAINT_COLOR);
        progressBgPaintColor = attributes.getColor(R.styleable.HMBProgressBar_hpb_title_size, DEFAULT_PROGRESS_BG_PAINT_COLOR);
        progressBgPaintColor = attributes.getColor(R.styleable.HMBProgressBar_hpb_bg_color, DEFAULT_PROGRESS_BG_PAINT_COLOR);
        isShowNumberText = attributes.getBoolean(R.styleable.HMBProgressBar_hpb_show_number_text, DEFAULT_SHOW_NUMBER_TEXT);
        titleSize = attributes.getColor(R.styleable.HMBProgressBar_hpb_title_size, DEFAULT_TITLE_SIZE);
        titleColor = attributes.getInt(R.styleable.HMBProgressBar_hpb_title_color, DEFAULT_TITLE_COLOR);
        summarySize = attributes.getColor(R.styleable.HMBProgressBar_hpb_summary_size, DEFAULT_SUMMARY_SIZE);
        summaryColor = attributes.getInt(R.styleable.HMBProgressBar_hpb_summary_color, DEFAULT_SUMMARY_COLOR);
        attributes.recycle();
    }

    private Rect textBounds = new Rect();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (width == 0 || height == 0) {
            width = getWidth();
            height = getHeight();

            //计算圆弧半径和圆心点
            circleRadius = Math.min(width, height) / 2;
            ARC_LINE_LENGTH = circleRadius / 6;
            ARC_LINE_WIDTH = ARC_LINE_LENGTH / 8;

            centerX = width / 2;
            centerY = height / 2;
        }

        Log.d(TAG, "circleRadius = " + circleRadius);

        float start = (360 - ARC_FULL_DEGREE) >> 1; //进度条起始角度
        float sweep1 = ARC_FULL_DEGREE * (progress / max); //进度划过的角度

        progressPaint.setStrokeWidth(ARC_LINE_WIDTH);
        float drawDegree = 1.6f;
        while (drawDegree <= ARC_FULL_DEGREE) {
            double a = (start + drawDegree) / 180 * Math.PI;
            float lineStartX = centerX - circleRadius * (float) Math.sin(a);
            float lineStartY = centerY + circleRadius * (float) Math.cos(a);
            float lineStopX = lineStartX + ARC_LINE_LENGTH * (float) Math.sin(a);
            float lineStopY = lineStartY - ARC_LINE_LENGTH * (float) Math.cos(a);

            if (drawDegree > sweep1) {
                //绘制进度条背景
                if (isOddRound) {
                    progressPaint.setColor(progressBgPaintColor);
                } else {
                    progressPaint.setColor(progressPaintColor);
                }
            } else {
                if (isOddRound) {
                    progressPaint.setColor(progressPaintColor);
                } else {
                    progressPaint.setColor(progressBgPaintColor);
                }
            }
            canvas.drawLine(lineStartX, lineStartY, lineStopX, lineStopY, progressPaint);
            drawDegree += ARC_EACH_PROGRESS;
        }

        //绘制文字背景圆形
        textBgPaint.setStyle(Paint.Style.FILL);//设置填充
        //textBgPaint.setColor(Color.parseColor("#41668b"));
        textBgPaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, (circleRadius - ARC_LINE_LENGTH) * 0.94f, textBgPaint);

        textBgPaint.setStyle(Paint.Style.STROKE);//设置空心
        textBgPaint.setStrokeWidth(2);
        textBgPaint.setColor(Color.parseColor("#F0F0F0"));
        canvas.drawCircle(centerX, centerY, (circleRadius - ARC_LINE_LENGTH) * 0.92f, textBgPaint);

        textPaint.setTextSize(titleSize);
        textPaint.setColor(titleColor);
        String text = "";
        //上一行文字
        if (isShowNumberText) {
            text = (int) (100 * progress / max) + "";
        }  else {
            text = title;
        }
        //计算文字高度
        float textLen = textPaint.measureText(text);
        textPaint.getTextBounds("8", 0, 1, textBounds);
        float h1 = textBounds.height();
        canvas.drawText(text, centerX - textLen / 2, centerY - circleRadius / 10 + h1 / 2, textPaint);

        //分
        if (isShowNumberText) {
            textPaint.setTextSize(circleRadius >> 3);
            textPaint.getTextBounds("分", 0, 1, textBounds);
            float h11 = textBounds.height();
            canvas.drawText("分", centerX + textLen / 2 + 5, centerY - circleRadius / 10 + h1 / 2 - (h1 - h11), textPaint);
        }

        //下一行文字
        textPaint.setTextSize(summarySize);
        textPaint.setColor(summaryColor);
        if(textPaintMsg != null){
          text = textPaintMsg;
        } else {
        	text="保持不错!";
        }
        textLen = textPaint.measureText(text);
        canvas.drawText(text, centerX - textLen / 2, centerY + circleRadius / 2.5f, textPaint);
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgressValue(float progress, boolean isInfinite) {
        Log.d(TAG, "setProgressValue()");
        if (progressAnim != null && progressAnim.isRunning()) {
            return;
        }
        progressAnim = ObjectAnimator.ofFloat(this, "progress", 0, progress);
        progressAnim.setDuration(DURATION);
        progressAnim.setInterpolator(new LinearInterpolator());
        if (isInfinite) {
            progressAnim.setRepeatCount(ValueAnimator.INFINITE);
        }
        progressAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                isOddRound = !isOddRound;
            }
        });
        progressAnim.start();
    }


    /**
     * 计算渐变效果中间的某个颜色值。
     * 仅支持 #aarrggbb 模式,例如 #ccc9c9b2
     */
    public String calColor(float fraction, String startValue, String endValue) {
        int start_a, start_r, start_g, start_b;
        int end_a, end_r, end_g, end_b;

        //start
        start_a = getIntValue(startValue, 1, 3);
        start_r = getIntValue(startValue, 3, 5);
        start_g = getIntValue(startValue, 5, 7);
        start_b = getIntValue(startValue, 7, 9);

        //end
        end_a = getIntValue(endValue, 1, 3);
        end_r = getIntValue(endValue, 3, 5);
        end_g = getIntValue(endValue, 5, 7);
        end_b = getIntValue(endValue, 7, 9);

        return "#" + getHexString((int) (start_a + fraction * (end_a - start_a)))
                + getHexString((int) (start_r + fraction * (end_r - start_r)))
                + getHexString((int) (start_g + fraction * (end_g - start_g)))
                + getHexString((int) (start_b + fraction * (end_b - start_b)));
    }

    //从原始#AARRGGBB颜色值中指定位置截取，并转为int.
    private int getIntValue(String hexValue, int start, int end) {
        return Integer.parseInt(hexValue.substring(start, end), 16);
    }

    private String getHexString(int value) {
        String a = Integer.toHexString(value);
        if (a.length() == 1) {
            a = "0" + a;
        }

        return a;
    }

	public int getProgressPaintColor() {
		return progressPaintColor;
	}

	public void setProgressPaintColor(int progressPaintColor) {
		this.progressPaintColor = progressPaintColor;
	}

    public String getTitleText() {
        return title;
    }

    public void setTitleText(String title) {
        this.title = title;
    }

	public String getTextPaintMsg() {
		return textPaintMsg;
	}

	public void setTextPaintMsg(String textPaintMsg) {
		this.textPaintMsg = textPaintMsg;
	}

	public boolean isShowNumberText() {
		return isShowNumberText;
	}

	public void setShowNumberText(boolean isShowNumberText) {
		this.isShowNumberText = isShowNumberText;
	}

	public int getTitleSize() {
		return titleSize;
	}

	public void setTitleSize(int titleSize) {
		this.titleSize = titleSize;
	}

	public int getTitleColor() {
		return titleColor;
	}

	public void setTitleColor(int titleColor) {
		this.titleColor = titleColor;
	}
	
}