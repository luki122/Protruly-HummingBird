package com.android.settings.widget.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;

/**
 * Created by liuqin on 17-6-2.
 *
 * @date Liuqin on 2017-06-02
 */
public class PinFunctionPadKey extends TextView{
    private PinPasswordTextView mTextView;
    private int mTextViewResId;
    private boolean mEnableHaptics;
    private PowerManager mPM;
    private OnClickListener mOnClickListener;

    public PinFunctionPadKey(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumPadKey);

        try {
            mTextViewResId = a.getResourceId(R.styleable.NumPadKey_textView, 0);
        } finally {
            a.recycle();
        }

        mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();
        mPM = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                if (id == R.id.delete_button) {
                    ensureEditText();
                    if (mTextView != null && mTextView.isEnabled()) {
                        mTextView.deleteLastChar();
                    }
                } else if (id == R.id.comfirm_button) {
//                    ensureEditText();
//                    if (mTextView != null && mTextView.isEnabled()) {
//                        OnEditorActionListener listener = mTextView.getOnEditorActionListener();
//                        if (listener != null) {
//                            listener.onEditorAction(mTextView, EditorInfo.IME_ACTION_NEXT, null);
//                        }
//                    }
                }
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(view);
                }
                userActivity();
                doHapticKeyClick();
            }
        });
    }

    private void ensureEditText() {
        if (mTextView == null && mTextViewResId > 0) {
            final View v = PinFunctionPadKey.this.getRootView().findViewById(mTextViewResId);
            if (v != null && v instanceof PinPasswordTextView) {
                mTextView = (PinPasswordTextView) v;
            }
        }
    }

    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                            | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    public void userActivity() {
        mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }
}
