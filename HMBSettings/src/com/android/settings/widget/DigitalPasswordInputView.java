package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.settings.R;


/**
 * Digital password input view.
 *
 * @date Liuqin on 2017-04-10
 */
public class DigitalPasswordInputView extends View implements View.OnClickListener{
    private GradientDrawable mPasswordDrawable;
    private int mDrawableWidth = 0;
    private int mDrawableInterval = 0;
    private int mDrawableCount = 0;
    private int mNormalColor = Color.DKGRAY;
    private int mInputColor = Color.BLACK;
    private int mInputCount = 0;
    private InputMethodManager mInputMethodManager;

    private View mEditText;


    public DigitalPasswordInputView(Context context) {
        super(context);
        init(null, 0);
    }

    public DigitalPasswordInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public DigitalPasswordInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.DigitalPasswordInputView, defStyle, 0);

        mDrawableWidth = (int) a.getDimension(
                R.styleable.DigitalPasswordInputView_drawableWidth,
                mDrawableWidth);
        mDrawableInterval = (int) a.getDimension(
                R.styleable.DigitalPasswordInputView_drawableInterval,
                mDrawableInterval);
        mDrawableCount = a.getInt(
                R.styleable.DigitalPasswordInputView_drawableCount,
                mDrawableCount);
        mNormalColor = a.getColor(
                R.styleable.DigitalPasswordInputView_normalColor,
                mNormalColor);
        mInputColor = a.getColor(
                R.styleable.DigitalPasswordInputView_inputColor,
                mInputColor);

        if (a.hasValue(R.styleable.DigitalPasswordInputView_passwordDrawable)) {
            mPasswordDrawable = (GradientDrawable) a.getDrawable(
                    R.styleable.DigitalPasswordInputView_passwordDrawable);
            mPasswordDrawable.setCallback(this);
        }

        a.recycle();

        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        setOnClickListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw the example drawable on top of the text.
        if (mPasswordDrawable != null) {
            int drawableWidth = contentHeight < mDrawableWidth ? contentHeight : mDrawableWidth;
            int allDrawableWidth = drawableWidth * mDrawableCount + mDrawableInterval * (mDrawableCount - 1);

            int startPadding = (getWidth() - allDrawableWidth) / 2;
            int topOffset = (contentHeight - drawableWidth) / 2;
            if (topOffset < 0) {
                topOffset = 0;
            }
            for (int i = 0; i < mDrawableCount; i++) {
                if (i < mInputCount) {
                    mPasswordDrawable.setColor(mInputColor);
                } else {
                    mPasswordDrawable.setColor(mNormalColor);
                }
                int leftOffset = paddingLeft + startPadding + (drawableWidth + mDrawableInterval) * i;
                mPasswordDrawable.setBounds(leftOffset, paddingTop,
                        leftOffset + drawableWidth, paddingTop + topOffset + drawableWidth);
                mPasswordDrawable.draw(canvas);
            }
        }
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getPasswordDrawable() {
        return mPasswordDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param drawable The example drawable attribute value to use.
     */
    public void setPasswordDrawable(GradientDrawable drawable) {
        mPasswordDrawable = drawable;
    }

    public void updateInputCount(int mInputCount) {
        this.mInputCount = mInputCount;
        invalidate();
    }

    @Override
    public void onClick(View view) {
        if (mEditText != null && mEditText.isEnabled()) {
            mInputMethodManager.showSoftInput(mEditText, 0);
        }
    }

    public void setEditText(View mEditText) {
        this.mEditText = mEditText;
    }

}
