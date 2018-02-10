package com.android.keyguard;

import java.util.ArrayList;
import java.util.Stack;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

/**
 * A View similar to a textView which contains password text and can animate when the text is
 * changed
 * add by wxue 
 */
public class PinPasswordTextView extends BasePasswordTextView {
	private static final int PASSWORD_LENGTH = 6;
    private static final long APPEAR_DURATION = 160;
    private static final long DISAPPEAR_DURATION = 160;
    private static final long RESET_DELAY_PER_ELEMENT = 40;
    private static final long RESET_MAX_DELAY = 200;

    private ArrayList<CharState> mTextChars = new ArrayList<>();
    private String mText = "";
    private Stack<CharState> mCharPool = new Stack<>();
    private int mDotSize;
    private int mHollowDotSize;
    private PowerManager mPM;
    private int mDotMargin;
    private final Paint mDrawPaint = new Paint();
    private Interpolator mAppearInterpolator;
    private Interpolator mDisappearInterpolator;
    private Interpolator mFastOutSlowInInterpolator;
    private boolean mShowPassword;
    private UserActivityListener mUserActivityListener;
    private boolean shakeAnimationIsGrowing;

    public PinPasswordTextView(Context context) {
        this(context, null);
    }

    public PinPasswordTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PinPasswordTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PinPasswordTextView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setFocusableInTouchMode(true);
        setFocusable(true);
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setStrokeWidth(2.0f);
        mDotSize = getContext().getResources().getDimensionPixelSize(R.dimen.password_dot_size);
        mHollowDotSize = getContext().getResources().getDimensionPixelSize(R.dimen.password_dot_hollow_size);
        mDotMargin = getContext().getResources().getDimensionPixelSize(R.dimen.password_dot_margin);
        mShowPassword = true;
        mAppearInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.linear_out_slow_in);
        mDisappearInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.fast_out_linear_in);
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.fast_out_slow_in);
        mPM = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
    	float totalDrawingWidth = getDrawingWidth();
        float currentDrawPosition = getWidth() / 2 - totalDrawingWidth / 2; // 画笔的起始位置
        int length = mTextChars.size();
        float yPosition = getHeight() / 2;
        // 画出固定的圆环
        drawFixedCircle(canvas, currentDrawPosition, yPosition);
        for (int i = 0; i < length; i++) {
            CharState charState = mTextChars.get(i);
            float charWidth = charState.draw(canvas, currentDrawPosition, yPosition);
            currentDrawPosition += charWidth;
        }
    }
    
    private void drawFixedCircle(Canvas canvas, float currentDrawPosition,float yPosition){
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setColor(0x80ffffff);
        currentDrawPosition = currentDrawPosition + (mDotSize - mHollowDotSize) / 2;
        for(int i=0; i<PASSWORD_LENGTH ;i++){
            canvas.save();
            float centerX = currentDrawPosition + mHollowDotSize / 2 ;
            canvas.translate(centerX, yPosition);
            canvas.drawCircle(0, 0, mHollowDotSize / 2, mDrawPaint);
            canvas.restore();
            currentDrawPosition = currentDrawPosition + mHollowDotSize + mDotMargin;
        }
        mDrawPaint.setStyle(Paint.Style.FILL);
        mDrawPaint.setColor(0xffffffff);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private float getDrawingWidth() {
    	return (mDotMargin- (mDotSize - mHollowDotSize) ) * (PASSWORD_LENGTH -1) + PASSWORD_LENGTH * mDotSize;
    }

    public void append(char c) {
    	if(shakeAnimationIsGrowing){
    		return;
    	}
        int visibleChars = mTextChars.size();
        String textbefore = mText;
        mText = mText + c;
        int newLength = mText.length();
        CharState charState;
        if (newLength > visibleChars) {
            charState = obtainCharState(c);
            mTextChars.add(charState);
        } else {
            charState = mTextChars.get(newLength - 1);
            charState.whichChar = c;
        }
        charState.startAppearAnimation();

        // ensure that the previous element is being swapped
        if (newLength > 1) {
            CharState previousState = mTextChars.get(newLength - 2);
            if (previousState.isDotSwapPending) {
                previousState.swapToDotWhenAppearFinished();
            }
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length(), 0, 1);
    }

    public void setUserActivityListener(UserActivityListener userActivitiListener) {
        mUserActivityListener = userActivitiListener;
    }

    private void userActivity() {
        mPM.userActivity(SystemClock.uptimeMillis(), false);
        if (mUserActivityListener != null) {
            mUserActivityListener.onUserActivity();
        }
    }

    public void deleteLastChar() {
        int length = mText.length();
        String textbefore = mText;
        if (length > 0) {
            mText = mText.substring(0, length - 1);
            CharState charState = mTextChars.get(length - 1);
            charState.startRemoveAnimation(0);
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length() - 1, 1, 0);
    }

    public String getText() {
        return mText;
    }

    private CharState obtainCharState(char c) {
        CharState charState;
        if(mCharPool.isEmpty()) {
            charState = new CharState();
        } else {
            charState = mCharPool.pop();
            charState.reset();
        }
        charState.whichChar = c;
        return charState;
    }

    public void reset(boolean animated) {
        if(animated){
        	shakeAnimationIsGrowing = true;
            startShakeAnimation();
        }else{
            resetView(animated);
        }
    }
    
    private void startShakeAnimation(){
        ObjectAnimator shakeAnimator = getShakeAnimation(this);
        shakeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                resetView(true);
                shakeAnimationIsGrowing = false;
            }
        });
        shakeAnimator.start();
    }
    
    private ObjectAnimator getShakeAnimation(View view) {
        int delta = 50;
        PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                Keyframe.ofFloat(0f, 0),
                Keyframe.ofFloat(.10f, -delta),
                /*Keyframe.ofFloat(.26f, delta),
                Keyframe.ofFloat(.42f, -delta),
                Keyframe.ofFloat(.58f, delta),
                Keyframe.ofFloat(.74f, -delta),*/
                Keyframe.ofFloat(.90f, delta),
                Keyframe.ofFloat(1f, 0f)
        );
        return ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).setDuration(300);
    }
	
    private void resetView(boolean animated){
		String textbefore = mText;
        mText = "";
        int length = mTextChars.size();
        int middleIndex = (length - 1) / 2;
        long delayPerElement = RESET_DELAY_PER_ELEMENT;
        for (int i = 0; i < length; i++) {
            CharState charState = mTextChars.get(i);
            if (animated) {
                charState.startRemoveAnimation(0);
                charState.removeDotSwapCallbacks();
            } else {
                charState.reset();
                mCharPool.push(charState);
            }
        }
        if (!animated) {
            mTextChars.clear();
        }
        sendAccessibilityEventTypeViewTextChanged(textbefore, 0, textbefore.length(), 0);
    }

    void sendAccessibilityEventTypeViewTextChanged(String beforeText, int fromIndex,
                                                   int removedCount, int addedCount) {
        if (AccessibilityManager.getInstance(mContext).isEnabled() &&
                (isFocused() || isSelected() && isShown())) {
            if (!shouldSpeakPasswordsForAccessibility()) {
                beforeText = null;
            }
            AccessibilityEvent event =
                    AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
            event.setFromIndex(fromIndex);
            event.setRemovedCount(removedCount);
            event.setAddedCount(addedCount);
            event.setBeforeText(beforeText);
            event.setPassword(true);
            sendAccessibilityEventUnchecked(event);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        event.setClassName(PasswordTextView.class.getName());
        event.setPassword(true);
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        if (shouldSpeakPasswordsForAccessibility()) {
            final CharSequence text = mText;
            if (!TextUtils.isEmpty(text)) {
                event.getText().add(text);
            }
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        info.setClassName(PasswordTextView.class.getName());
        info.setPassword(true);

        if (shouldSpeakPasswordsForAccessibility()) {
            info.setText(mText);
        }

        info.setEditable(true);

        info.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    /**
     * @return true if the user has explicitly allowed accessibility services
     * to speak passwords.
     */
    private boolean shouldSpeakPasswordsForAccessibility() {
        return (Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0,
                UserHandle.USER_CURRENT_OR_SELF) == 1);
    }

    private class CharState {
        char whichChar;
        Animator dotAnimator;
        boolean dotAnimationIsGrowing;
        float currentDotSizeFactor;
        boolean isDotSwapPending;

        Animator.AnimatorListener removeEndListener = new AnimatorListenerAdapter() {
            private boolean mCancelled;
            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCancelled) {
                    mTextChars.remove(CharState.this);
                    mCharPool.push(CharState.this);
                    reset();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mCancelled = false;
            }
        };

        Animator.AnimatorListener dotFinishListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dotAnimator = null;
            }
        };

        private ValueAnimator.AnimatorUpdateListener dotSizeUpdater
                = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentDotSizeFactor = (float) animation.getAnimatedValue();
                invalidate();
            }
        };

        private Runnable dotSwapperRunnable = new Runnable() {
            @Override
            public void run() {
                performSwap();
                isDotSwapPending = false;
            }
        };

        void reset() {
            whichChar = 0;
            currentDotSizeFactor = 0.0f;
            cancelAnimator(dotAnimator);
            dotAnimator = null;
            removeDotSwapCallbacks();
        }

        void startRemoveAnimation(long startDelay) {
            boolean dotNeedsAnimation = (currentDotSizeFactor > 0.0f && dotAnimator == null)
                    || (dotAnimator != null && dotAnimationIsGrowing);
            if (dotNeedsAnimation) {
                startDotDisappearAnimation(startDelay);
            }
        }

        void startAppearAnimation() {
            if (mShowPassword) {
                postDotSwap(0);
            }
        }

        /**
         * Posts a runnable which ensures that the input will be replaced by a dot 
         */
        private void postDotSwap(long delay) {
            removeDotSwapCallbacks();
            postDelayed(dotSwapperRunnable, delay);
            isDotSwapPending = true;
        }

        private void removeDotSwapCallbacks() {
            removeCallbacks(dotSwapperRunnable);
            isDotSwapPending = false;
        }

        void swapToDotWhenAppearFinished() {
            removeDotSwapCallbacks();
            performSwap();

        }

        private void performSwap() {
            startDotAppearAnimation(0);
        }

        private void startDotDisappearAnimation(long startDelay) {
            cancelAnimator(dotAnimator);
            ValueAnimator animator = ValueAnimator.ofFloat(currentDotSizeFactor, 0.0f);
            animator.addUpdateListener(dotSizeUpdater);
            animator.addListener(removeEndListener);
            animator.addListener(dotFinishListener);
            animator.setInterpolator(mDisappearInterpolator);
            long duration = (long) (DISAPPEAR_DURATION * Math.min(currentDotSizeFactor, 1.0f));
            animator.setDuration(duration);
            animator.setStartDelay(startDelay);
            animator.start();
            dotAnimator = animator;
            dotAnimationIsGrowing = false;
        }

        private void startDotAppearAnimation(long delay) {
            cancelAnimator(dotAnimator);
            if (mShowPassword) {
                ValueAnimator growAnimator = ValueAnimator.ofFloat(currentDotSizeFactor, 1.0f);
                growAnimator.addUpdateListener(dotSizeUpdater);
                growAnimator.setDuration((long) (APPEAR_DURATION * (1.0f - currentDotSizeFactor)));
                growAnimator.addListener(dotFinishListener);
                growAnimator.setStartDelay(delay);
                growAnimator.start();
                dotAnimator = growAnimator;
            }
            dotAnimationIsGrowing = true;
        }

        private void cancelAnimator(Animator animator) {
            if (animator != null) {
                animator.cancel();
            }
        }

        /**
         * Draw dot to the canvas.
         * @return The width this character contributes, including padding.
         */
        public float draw(Canvas canvas, float currentDrawPosition, float yPosition) {
            boolean dotVisible = currentDotSizeFactor > 0;
            float charWidth = mDotSize;
            if (dotVisible) {
                canvas.save();
                float centerX = currentDrawPosition + mDotSize / 2;
                canvas.translate(centerX, yPosition);
                canvas.drawCircle(0, 0, mDotSize / 2 * currentDotSizeFactor, mDrawPaint);
                canvas.restore();
            }
            return charWidth + (mDotMargin- (mDotSize - mHollowDotSize)) ;
        }
    }
}

