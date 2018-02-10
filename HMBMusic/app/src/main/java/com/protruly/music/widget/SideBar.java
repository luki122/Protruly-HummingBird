package com.protruly.music.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.protruly.music.util.DisplayUtil;


/**
 * Created by hujianwei on 17-8-30.
 */

public class SideBar extends View {

    private static final String TAG = "SideBar";
    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    // 26个字母
    public static String[] b = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#" };
    public static int[] mauroray;
    private int choose = -1;// 选中
    private Paint paint = new Paint();
    private TextView mTextDialog;
    private int mBaseW = 57;
    private int mBaseH = 57;
    private int mBasePadding = 25;
    private final int DEF_TEXT_SIZE = 13;
    int height = 0;
    int width = 0;
    int startX = 0;
    int marginBottom = 0;
    int marginTop = 0;
    private Typeface mFace;

    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }

    public SideBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        //TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.custom_sidebar);
        //startX = a.getInt(R.styleable.custom_sidebar_siderbar_marginBottom, 0);
        startX = (int) (DisplayUtil.getScale(getContext()) * startX);
        //marginBottom = a.getInt(R.styleable.custom_sidebar_siderbar_fontBottom, 0);
        marginBottom = (int) (DisplayUtil.getScale(getContext()) * marginBottom);
        //marginTop = a.getInt(R.styleable.custom_sidebar_siderbar_marginTop, 0);
        marginTop = (int) (DisplayUtil.getScale(getContext()) * marginTop);
        initView(context);
    }

    public SideBar(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {
        mauroray = new int[27];
        mFace = Typeface.createFromFile("system/fonts/Roboto-Light.ttf");
        paint = new Paint();
        paint.setTypeface(mFace);
        paint.setAntiAlias(true);
        paint.setTextSize(DEF_TEXT_SIZE * DisplayUtil.getScale(getContext()));
    }

    /**
     * 重写这个方法
     */
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 获取焦点改变背景颜色.
        height = getHeight() - startX - marginBottom;// 获取对应高度
        width = getWidth(); // 获取对应宽度
        int singleHeight = height / b.length;// 获取每一个字母的高度

        for (int i = 0; i < b.length; i++) {
            // 选中的状态
            if (i == choose) {
                paint.setColor(Color.parseColor("#ff9000"));
            } else {
                paint.setColor(Color.parseColor("#000000"));
            }
            // x坐标等于中间-字符串宽度的一半.
            float xPos = width / 2 - paint.measureText(b[i]) / 2;
            float yPos = singleHeight * i + (singleHeight / 2) + startX + 10;
            canvas.drawText(b[i], xPos, yPos, paint);
            mauroray[i] = (int) yPos;
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float py = event.getY();
        final float y = py - startX;// 点击y坐标
        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;
        final int c = (int) (y / height * b.length);// 点击y坐标所占总高度的比例*b数组的长度就等于点击b中的个数.
        switch (action) {
            case MotionEvent.ACTION_UP:
                invalidate();
                if (mTextDialog != null) {
                    mTextDialog.setVisibility(View.INVISIBLE);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                showActionDialog(c, listener);
                break;
		/*case MotionEvent.ACTION_DOWN:
			showActionDialog(c, listener);
			break;*/

        }
        return true;
    }

    private void showActionDialog(final int c, final OnTouchingLetterChangedListener listener) {
        if (c >= 0 && c < b.length) {
            if (listener != null) {
                listener.onTouchingLetterChanged(b[c]);
            }
            if (mTextDialog != null) {
                mTextDialog.setText(b[c]);
                int w = (int) (DisplayUtil.getScale(getContext()) * mBaseW);
                int h = (int) (DisplayUtil.getScale(getContext()) * mBaseH);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(w, h);

                layoutParams.leftMargin = getLeft() - ((int) (DisplayUtil.getScale(getContext()) * mBasePadding) + w);
                if (c == 0)
                    layoutParams.topMargin = mauroray[c] - h / 2 + marginTop;
                else
                    layoutParams.topMargin = mauroray[c] - h / 2 - 16 + marginTop;
                if (layoutParams.topMargin < 0) {
                    layoutParams.topMargin = 0;
                }
                mTextDialog.setLayoutParams(layoutParams);
                mTextDialog.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 向外公开的方法
     * @param onTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    /**
     * 接口
     * @author coder
     */
    public interface OnTouchingLetterChangedListener {
        public void onTouchingLetterChanged(String s);
    }

    public void setCurChooseTitle(String string) {
        boolean flag = true;
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i].equals(string)) {
                choose = i;
                invalidate();
                flag = false;
                break;
            }
        }
        if (flag) {
            choose = b.length - 1;
            invalidate();
        }
    }
}
