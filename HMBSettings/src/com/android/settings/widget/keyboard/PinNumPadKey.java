/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;

/**
 * 
 * create by wxue 20170425
 *
 */
public class PinNumPadKey extends ViewGroup {
    // list of "ABC", etc per digit, starting with '0'
    static String sKlondike[];

    private int mDigit = -1;
    private int mTextViewResId;
    private PinPasswordTextView mTextView;
    private TextView mDigitText;
    private TextView mKlondikeText;
    private boolean mEnableHaptics;
    private PowerManager mPM;

    private View.OnClickListener mListener = new View.OnClickListener() {
        @Override
        public void onClick(View thisView) {
            if (mTextView == null && mTextViewResId > 0) {
                final View v = PinNumPadKey.this.getRootView().findViewById(mTextViewResId);
                if (v != null && v instanceof PinPasswordTextView) {
                    mTextView = (PinPasswordTextView) v;
                }
            }
            if (mTextView != null && mTextView.isEnabled()) {
                mTextView.append(Character.forDigit(mDigit, 10));
            }
            userActivity();
            doHapticKeyClick();
        }
    };

    public void userActivity() {
        mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    public PinNumPadKey(Context context) {
        this(context, null);
    }

    public PinNumPadKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinNumPadKey(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFocusable(true);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumPadKey);

        try {
            mDigit = a.getInt(R.styleable.NumPadKey_digit, mDigit);
            mTextViewResId = a.getResourceId(R.styleable.NumPadKey_textView, 0);
        } finally {
            a.recycle();
        }

        setOnClickListener(mListener);
//        setOnHoverListener(new LiftToActivateListener(context));
//        setAccessibilityDelegate(new ObscureSpeechDelegate(context));

        mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();

        mPM = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hb_keyguard_num_pad_key, this, true);

        mDigitText = (TextView) findViewById(R.id.digit_text);
        mDigitText.setText(Integer.toString(mDigit));
        mKlondikeText = (TextView) findViewById(R.id.klondike_text);
        mKlondikeText.setVisibility(View.GONE);
        setBackground(mContext.getDrawable(R.drawable.ripple_drawable));
        setContentDescription(mDigitText.getText().toString());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Reset the "announced headset" flag when detached.
//        ObscureSpeechDelegate.sAnnouncedHeadset = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int digitHeight = mDigitText.getMeasuredHeight();
        int klondikeHeight = mKlondikeText.getMeasuredHeight();
        int totalHeight = digitHeight + klondikeHeight;
        int top = getHeight() / 2 - totalHeight / 2;
        int centerX = getWidth() / 2;
        int left = centerX - mDigitText.getMeasuredWidth() / 2;
        int bottom = top + digitHeight;
        mDigitText.layout(left, top, left + mDigitText.getMeasuredWidth(), bottom);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Cause a VIRTUAL_KEY vibration
    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }
}
