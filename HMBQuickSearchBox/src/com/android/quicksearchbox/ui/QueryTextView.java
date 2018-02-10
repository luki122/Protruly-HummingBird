/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.android.quicksearchbox.R;

/**
 * The query text field.
 */
public class QueryTextView extends EditText {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.QueryTextView";

    private CommitCompletionListener mCommitCompletionListener;
    //liuzuo add
    private Drawable mRightDrawable;
    private boolean isHasFocus;
    private boolean mCleanVisible;

    public QueryTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public QueryTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QueryTextView(Context context) {
        super(context);
        init();
    }

    /**
     * Sets the text selection in the query text view.
     *
     * @param selectAll If {@code true}, selects the entire query.
     *        If {@false}, no characters are selected, and the cursor is placed
     *        at the end of the query.
     */
    public void setTextSelection(boolean selectAll) {
        if (selectAll) {
            selectAll();
        } else {
            setSelection(length());
        }
    }

    protected void replaceText(CharSequence text) {
        clearComposingText();
        setText(text);
        setTextSelection(false);
    }

    public void setCommitCompletionListener(CommitCompletionListener listener) {
        mCommitCompletionListener = listener;
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void showInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null) {
            imm.showSoftInput(this, 0);
        }
    }

    public void hideInputMethod() {
        InputMethodManager imm = getInputMethodManager();
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    @Override
    public void onCommitCompletion(CompletionInfo completion) {
        if (DBG) Log.d(TAG, "onCommitCompletion(" + completion + ")");
        hideInputMethod();
        replaceText(completion.getText());
        if (mCommitCompletionListener != null) {
            mCommitCompletionListener.onCommitCompletion(completion.getPosition());
        }
    }
    //liuzuo add for clear text begin
    public void setClearDrawableVisible(boolean isVisible) {
        if(mRightDrawable==null){
            mRightDrawable=getResources().getDrawable(R.drawable.ic_clear_search);
        }
        Drawable rightDrawable;
        if (isVisible) {
            mRightDrawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.DST);
            rightDrawable = mRightDrawable;
            //setBackground(getResources().getDrawable(R.drawable.folder_bg));
        } else {
            mRightDrawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC);
            rightDrawable = mRightDrawable;
            setBackground(null);
        }
        setCompoundDrawablesRelativeWithIntrinsicBounds(getCompoundDrawables()[0],getCompoundDrawables()[1],
                rightDrawable,getCompoundDrawables()[3]);
        mCleanVisible = isVisible;
    }

    private class TextWatcherImpl implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            boolean isVisible=getText().toString().length()>=1;
            setClearDrawableVisible(isVisible);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }


    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:

                boolean isClean =(event.getX() > (getWidth() - getTotalPaddingRight()))&&
                        (event.getX() < (getWidth() - getPaddingRight()));
                if (isClean&&mCleanVisible) {
                    setText("");
                }
                break;

            default:
                break;
        }
        return super.onTouchEvent(event);
    }
    private class FocusChangeListenerImpl implements OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            isHasFocus=hasFocus;
            if (isHasFocus) {
                boolean isVisible=getText().toString().length()>=1;
                setClearDrawableVisible(isVisible);
            } else {
                setClearDrawableVisible(false);
            }
        }


    }
    private void init(){

        Drawable[] drawables=this.getCompoundDrawables();

        mRightDrawable = drawables[2];
        this.setOnFocusChangeListener(new QueryTextView.FocusChangeListenerImpl());
        this.addTextChangedListener((TextWatcher) new QueryTextView.TextWatcherImpl());
        setClearDrawableVisible(false);
    }
    //liuzuo add for clear text end
    public interface CommitCompletionListener {
        void onCommitCompletion(int position);
    }
}
