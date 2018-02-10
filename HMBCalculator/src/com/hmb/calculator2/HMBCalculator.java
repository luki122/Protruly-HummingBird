package com.hmb.calculator2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.hmb.calculator2.CalculatorEditText.OnTextSizeChangeListener;
import com.hmb.calculator2.CalculatorExpressionEvaluator.EvaluateCallback;

/**
 * Created by xiaobin on 17-6-19.
 */

public class HMBCalculator extends Activity implements EvaluateCallback, OnTextSizeChangeListener {

    private static final String TAG = "HMBCalculator";

    private static final String NAME = Calculator.class.getName();

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;
    public static final int INPUT_DIGIT_MAX = 11;

    private enum CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }

    private boolean flagDealInfiniteDecimal = false;       // 为了标示结果是无限循环小数导致的无法输入数字问题

    private final TextWatcher mFormulaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.toString().equals(getString(R.string.error_syntax))) {
                return;
            }
            setState(CalculatorState.INPUT);
            flagDealInfiniteDecimal = false;
            mEvaluator.evaluate(editable, HMBCalculator.this);
        }
    };

    private final View.OnKeyListener mFormulaOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        onEquals();
                    }
                    // ignore all other actions
                    return true;
            }
            return false;
        }
    };

    private final Editable.Factory mFormulaEditableFactory = new Editable.Factory() {
        @Override
        public Editable newEditable(CharSequence source) {
            final boolean isEdited = mCurrentState == CalculatorState.INPUT
                    || mCurrentState == CalculatorState.ERROR;
            return new HMBCalculatorExpressionBuilder(source, mTokenizer, isEdited);
        }
    };

    private CalculatorState mCurrentState;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;

    private CalculatorEditText mFormulaEditText;
    private CalculatorEditText mResultEditText;

    private Animator mCurrentAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hmb_activity_calculator);

        mFormulaEditText = (CalculatorEditText) findViewById(R.id.formula);
        mResultEditText = (CalculatorEditText) findViewById(R.id.result);

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
        setState(CalculatorState.values()[
                savedInstanceState.getInt(KEY_CURRENT_STATE, CalculatorState.INPUT.ordinal())]);
        mFormulaEditText.setText(mTokenizer.getLocalizedExpression(
                savedInstanceState.getString(KEY_CURRENT_EXPRESSION, "0")));
        mEvaluator.evaluate(mFormulaEditText.getText(), this);

        mFormulaEditText.setEditableFactory(mFormulaEditableFactory);
        mFormulaEditText.addTextChangedListener(mFormulaTextWatcher);
        mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
        mFormulaEditText.setOnTextSizeChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText().toString()));
    }

    private void setState(CalculatorState state) {
        if (mCurrentState != state) {
            mCurrentState = state;

            if (state == CalculatorState.ERROR) {
                final int errorColor = getResources().getColor(R.color.calculator_error_color);
                mFormulaEditText.setTextColor(errorColor);
                mResultEditText.setTextColor(errorColor);
//                getWindow().setStatusBarColor(errorColor);
            } else {
                mFormulaEditText.setTextColor(
                        getResources().getColor(R.color.display_formula_text_color));
                mResultEditText.setTextColor(
                        getResources().getColor(R.color.display_result_text_color));
//                getWindow().setStatusBarColor(
//                        getResources().getColor(R.color.calculator_accent_color));
            }
        }
    }

    public void onButtonClick(View view) {
        if (mCurrentState == CalculatorState.ERROR) {
            resetFormulaEditText();
        }

        switch (view.getId()) {
            case R.id.digit_0:
            case R.id.digit_1:
            case R.id.digit_2:
            case R.id.digit_3:
            case R.id.digit_4:
            case R.id.digit_5:
            case R.id.digit_6:
            case R.id.digit_7:
            case R.id.digit_8:
            case R.id.digit_9:
                if (canAppendDigit() || flagDealInfiniteDecimal) {
                    clearStartZero();
                    mFormulaEditText.append(((HMBCalculatorPadButton) view).getTag().toString());
                }
                break;
            case R.id.op_point:
                if (mCurrentState != CalculatorState.INPUT) {
                    mFormulaEditText.append("0");
                }
                mFormulaEditText.append(((HMBCalculatorPadButton) view).getTag().toString());
                break;
            case R.id.op_pi:
            case R.id.op_sqrt:
//            case R.id.op_e:
            case R.id.op_ex:
            case R.id.op_lparen:
            case R.id.op_rparen:
                if (mFormulaEditText.getText().toString().equals("0")) {
                    mFormulaEditText.getEditableText().clear();
                }
                mFormulaEditText.append(((HMBCalculatorPadButton) view).getTag().toString());
                break;
            case R.id.op_clear:
                resetFormulaEditText();
                break;
            case R.id.op_del:
                onDelete();
                break;
            case R.id.op_eq:
                onEquals();
                break;
            case R.id.op_po_and_ne:
                dealPositiveAndNegative();
                break;
            case R.id.op_cos:
            case R.id.op_ln:
            case R.id.op_log:
            case R.id.op_sin:
            case R.id.op_tan:
                if (mFormulaEditText.getText().toString().equals("0")) {
                    mFormulaEditText.getEditableText().clear();
                }
                mFormulaEditText.append(((HMBCalculatorPadButton) view).getTag().toString() + "(");
                break;
            case R.id.op_1x:
                if (!mFormulaEditText.getText().toString().equals("0")) {
                    onEquals1X();
                }
                break;
            case R.id.op_abs:
                onEqualsABS();
                break;
            default:
                mFormulaEditText.append(((HMBCalculatorPadButton) view).getTag().toString());
                break;
        }
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
            mResultEditText.setText(result);
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)) {
            onResult(result);
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT);
        }

        mFormulaEditText.requestFocus();
    }

    @Override
    public void onTextSizeChanged(final TextView textView, float oldSize) {
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return;
        }

        // Calculate the values needed to perform the scale and translation animations,
        // maintaining the same apparent baseline for the displayed text.
        final float textScale = oldSize / textView.getTextSize();
        final float translationX = (1.0f - textScale) *
                (textView.getWidth() / 2.0f - textView.getPaddingEnd());
        final float translationY = (1.0f - textScale) *
                (textView.getHeight() / 2.0f - textView.getPaddingBottom());

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, View.SCALE_X, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.SCALE_Y, textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_X, translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, translationY, 0.0f));
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate(mFormulaEditText.getText(), this);
        }
    }

    private void onEquals1X() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate("1/(" + mFormulaEditText.getText() + ")", this);
        }
    }

    private void onEqualsABS() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate("abs(" + mFormulaEditText.getText() + ")", this);
        }
    }

    private void onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        final Editable formulaText = mFormulaEditText.getEditableText();
        final int formulaLength = formulaText.length();
//        if (formulaLength == 1) {
//            resetFormulaEditText();
//        } else if (formulaLength > 0) {
//            formulaText.delete(formulaLength - 1, formulaLength);
//        }
        if (formulaLength > 0) {
            formulaText.delete(formulaLength - 1, formulaLength);
        }
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText.setText(errorResourceId);
            return;
        }

        setState(CalculatorState.ERROR);
        mResultEditText.setText(errorResourceId);

        mFormulaEditText.setText(getString(R.string.error_syntax));
    }

    private void onResult(final String result) {
        // Calculate the values needed to perform the scale and translation animations,
        // accounting for how the scale will affect the final position of the text.
        final float resultScale =
                mFormulaEditText.getVariableTextSize(result) / mResultEditText.getTextSize();
        final float resultTranslationX = (1.0f - resultScale) *
                (mResultEditText.getWidth() / 2.0f - mResultEditText.getPaddingEnd());
        final float resultTranslationY = (1.0f - resultScale) *
                (mResultEditText.getHeight() / 2.0f - mResultEditText.getPaddingBottom()) +
                (mFormulaEditText.getBottom() - mResultEditText.getBottom()) +
                (mResultEditText.getPaddingBottom() - mFormulaEditText.getPaddingBottom());
        final float formulaTranslationY = -mFormulaEditText.getBottom();

        // Use a value animator to fade to the final text color over the course of the animation.
        final int resultTextColor = mResultEditText.getCurrentTextColor();
        final int formulaTextColor = mFormulaEditText.getCurrentTextColor();
        final ValueAnimator textColorAnimator =
                ValueAnimator.ofObject(new ArgbEvaluator(), resultTextColor, formulaTextColor);
        textColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mResultEditText.setTextColor((int) valueAnimator.getAnimatedValue());
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                textColorAnimator,
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_X, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.SCALE_Y, resultScale),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_X, resultTranslationX),
                ObjectAnimator.ofFloat(mResultEditText, View.TRANSLATION_Y, resultTranslationY),
                ObjectAnimator.ofFloat(mFormulaEditText, View.TRANSLATION_Y, formulaTranslationY));
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mResultEditText.setText(result);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset all of the values modified during the animation.
                mResultEditText.setTextColor(resultTextColor);
                mResultEditText.setScaleX(1.0f);
                mResultEditText.setScaleY(1.0f);
                mResultEditText.setTranslationX(0.0f);
                mResultEditText.setTranslationY(0.0f);
                mFormulaEditText.setTranslationY(0.0f);

                // Finally update the formula to use the current result.
                mFormulaEditText.setText(result);
                setState(CalculatorState.RESULT);

                mCurrentAnimator = null;

                // 超大字体时会导致显示不全，因此把结果光标显示到最前
                mFormulaEditText.setSelection(0);

                flagDealInfiniteDecimal = true;
            }
        });

        mCurrentAnimator = animatorSet;
        animatorSet.start();
    }

    private void resetFormulaEditText() {
        if (mFormulaEditText != null) {
            mFormulaEditText.getEditableText().clear();
            mFormulaEditText.getEditableText().append("0");
        }
    }

    private void dealPositiveAndNegative() {
        String text = mFormulaEditText.getText().toString();

        if (text.equals("0")) {
            mFormulaEditText.getEditableText().clear();
            mFormulaEditText.append(getString(R.string.tag_sub));
        } else if (text.equals(getString(R.string.tag_sub))) {
            mFormulaEditText.getEditableText().clear();
            mFormulaEditText.getEditableText().append("0");
        } else {
            // 获取最后数字前的运算符号位置
            int start =  text.length() - 1;
            while(start >= 0) {
                char c = text.charAt(start);
                if (c == getString(R.string.tag_point).charAt(0) || Character.isDigit(c)) {
                    start--;
                } else {
                    break;
                }
            }

            if (start == -1) {  // 无运算符号， 直接加负号
                mFormulaEditText.getText().insert(0, getString(R.string.tag_sub));
            } else if (start == 0) {    // 第一位, 符号变正， 例: -1 -> 1
                char c = text.charAt(start);
                if (c == getString(R.string.tag_sub).charAt(0)) {
                    mFormulaEditText.getText().delete(start, start + 1);
                }
            } else if (start > 0) { // 非第一位
                char c = text.charAt(start);
                if (c == getString(R.string.tag_sub).charAt(0)) {  // 运算符号为-
                    if (Character.isDigit(text.charAt(start - 1))) { // 运算符号的前一位是数字， 直接变为加号
                        mFormulaEditText.getText().replace(start, start + 1, getString(R.string.tag_add));
                    } else { // 前一位不为数字， 删除负号
                        mFormulaEditText.getText().delete(start, start + 1);
                    }
                } else if (c == getString(R.string.tag_add).charAt(0)) { // 运算符号为+号， 直接变为-号
                    mFormulaEditText.getText().replace(start, start + 1, getString(R.string.tag_sub));
                } else { // 直接插入负号
                    mFormulaEditText.getText().insert(start + 1, getString(R.string.tag_sub));
                }
            }

        }
    }

    private boolean canAppendDigit() {
        String text = mFormulaEditText.getText().toString();

        // 获取最后数字前的运算符号位置
        int start =  text.length() - 1;
        while(start >= 0) {
            char c = text.charAt(start);
            if (c == getString(R.string.tag_point).charAt(0) || Character.isDigit(c)) {
                start--;
            } else {
                break;
            }
        }

        if (start + 1 < text.length()) {
            String endText = text.substring(start + 1);
            if (endText.length() >= INPUT_DIGIT_MAX) {
                return false;
            }
        }

        return true;
    }

    private void clearStartZero() {
        String text = mFormulaEditText.getText().toString();

        // 获取最后数字前的运算符号位置(包含小数点)
        int start =  text.length() - 1;
        while(start >= 0) {
            char c = text.charAt(start);
            if (c == getString(R.string.tag_point).charAt(0) || Character.isDigit(c)) {
                start--;
            } else {
                break;
            }
        }

        if (start + 1 < text.length()) {
            String endText = text.substring(start + 1);
            if (endText.startsWith("0.")) {
                return;
            }

            if (text.substring(start + 1, start + 2).equals("0")) {
                mFormulaEditText.getText().delete(start + 1, start + 2);
            }
        }

    }


}
