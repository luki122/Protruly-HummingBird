package com.hb.interception.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.text.TextUtils;
import com.hb.interception.R;

public class ZzzDetailCheckOverSizeTextView extends TextView {

    protected boolean isOverSize;
    private boolean mIsCallChangedListener = true;
    private OnOverSizeChangedListener mOnOverSizeChangedListener;
    private boolean isEvenOverSize = false;
    private int mMaxHeight;

    public ZzzDetailCheckOverSizeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
        initRef(context);
    }

    public ZzzDetailCheckOverSizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        initRef(context);
    }

    public ZzzDetailCheckOverSizeTextView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        initRef(context);
    }

    private void initRef(Context context)  {
        mMaxHeight = (int)context.getResources().getDimension(R.dimen.mms_detail_text_max_height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsCallChangedListener && mOnOverSizeChangedListener != null) {
            mOnOverSizeChangedListener.onChanged(checkOverLine());
        }
    }

    public boolean checkOverLine() {
        isOverSize = getHeight() > mMaxHeight ? true : false;
        return isOverSize;
    }

    public boolean isOverSize() {
        return isOverSize;
    }

    public void displayAll() {
        /*setMaxLines(Integer.MAX_VALUE);
        setEllipsize(null);*/
        isEvenOverSize = true;
    }

    public boolean getIsEvenOverSize() {
        return isEvenOverSize;
    }

    public void setOnOverLineChangedListener(OnOverSizeChangedListener changedListener) {
        this.mOnOverSizeChangedListener = changedListener;
    }

    public void setIsCallChangedListener(boolean isCall) {
        this.mIsCallChangedListener = isCall;
    }

    public interface OnOverSizeChangedListener {
        /**
         * <span style="color:purple">when invalide,the method will be called
         * and tell you whether the content text is over size
         * 
         * @param isOverLine
         *            whether content text is over size
         */
        public void onChanged(boolean isOverSize);
    };
}
