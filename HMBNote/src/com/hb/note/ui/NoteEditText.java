package com.hb.note.ui;

import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.hb.note.util.Globals;
import com.hb.note.util.PatternUtils;
import com.hb.note.util.SystemUtils;

public class NoteEditText extends EditText {

    private static final String TAG = "NoteEditText";

    private static final int SPAN_RIGHT_HOT_AREA = 30;

    private InputMethodManager mImm;

    private float mDownY;
    private int mScaleTouchSlop;
    private int mSelStart = -1;
    private boolean mChangeSelection, mLongPressPerformed, mShouldInterceptTouchUp, mHandleEvent;

    private NoteImageSpan mSelectedSpan;
    private CheckLongPress mCheckLongPress;

    private OnSpanClickListener mOnSpanClickListener;
    private OnSpanLongClickListener mOnSpanLongClickListener;
    private OnCursorLineChangedListener mOnCursorLineChangedListener;

    public interface OnSpanClickListener {
        boolean onSpanClick(Editable editable, NoteImageSpan span, int selStart);
    }

    public interface OnSpanLongClickListener {
        boolean onSpanLongClick(Editable editable, NoteImageSpan span);
    }

    public interface OnCursorLineChangedListener {
        void onCursorLineChanged(String rowStr);
    }

    public void setOnSpanClickListener(OnSpanClickListener onSpanClickListener) {
        mOnSpanClickListener = onSpanClickListener;
    }

    public void setOnSpanLongClickListener(OnSpanLongClickListener onSpanLongClickListener) {
        mOnSpanLongClickListener = onSpanLongClickListener;
    }

    public void setOnCursorLineChangedListener(
            OnCursorLineChangedListener onCursorLineChangedListener) {
        mOnCursorLineChangedListener = onCursorLineChangedListener;
    }

    private class CheckLongPress implements Runnable {
        private int mWindowAttachCount;

        @Override
        public void run() {
            if (mWindowAttachCount == getWindowAttachCount()) {
                if (handleSpanLongClick()) {
                    mLongPressPerformed = true;
                }
            }
        }

        private void rememberWindowAttachCount() {
            mWindowAttachCount = getWindowAttachCount();
        }
    }

    public NoteEditText(Context context) {
        this(context, null);
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mScaleTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onDetachedFromWindow() {
        getText().clearSpans();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean result = super.onTextContextMenuItem(id);
        switch (id) {
            case android.R.id.cut:
            case android.R.id.copy:
                ClipboardManager cm =
                        (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                String text = cm.getText().toString();
                if (!TextUtils.isEmpty(text)) {
                    text = PatternUtils.replaceAllSpans(text);
                    cm.setText(text);
                }
                break;
        }
        return result;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (mChangeSelection) {
            mChangeSelection = false;
            return;
        }

        clearSelectedSpan();

        if (selStart >= 0 && selStart == selEnd) {
            final String content = getText().toString();
            int rowStart = SystemUtils.getRowStart(content, selStart);
            int rowEnd = SystemUtils.getRowEnd(content, selStart);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (!TextUtils.isEmpty(rowStr) &&
                    rowStr.startsWith(Globals.SPAN_START) && selStart == rowStart) {
                if (mSelStart == -1 || mSelStart < rowStart) {
                    mSelStart = rowEnd;
                } else if (mSelStart > content.length()) {
                    mSelStart = content.length();
                }
                mChangeSelection = true;
                setSelection(mSelStart);
            } else {
                mSelStart = selStart;
            }

            notifyCursorLineChange(rowStr);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mHandleEvent) {
            return super.onTouchEvent(event);
        }

        final int start = getStart(event);

        if (event.getX() - getTotalPaddingLeft() <= 0) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getY();

                clearSelectedSpan();
                selectSpan(start, event);

                mLongPressPerformed = false;
                if (mSelectedSpan != null) {
                    startCheckLongPress();
                    return true;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (mSelectedSpan != null) {
                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    NoteImageSpan span = getSpan(start);
                    if (Math.abs(event.getY() - mDownY) > mScaleTouchSlop ||
                            span == null || !mSelectedSpan.equals(span.getSpan(x, y))) {
                        clearSelectedSpan();
                        removeCheckLongPress();
                    }
                }

                if (Math.abs(event.getY() - mDownY) > mScaleTouchSlop &&
                        getLayout() != null && getLayout().getHeight() < getHeight()) {
                    mShouldInterceptTouchUp = true;
                }

                break;
            case MotionEvent.ACTION_UP:
                if (mShouldInterceptTouchUp) {
                    mShouldInterceptTouchUp = false;
                    event.setAction(MotionEvent.ACTION_CANCEL);
                } else {
                    clearSelectedSpan();

                    if (!mLongPressPerformed) {
                        removeCheckLongPress();

                        if (Math.abs(event.getY() - mDownY) <= mScaleTouchSlop &&
                                event.getActionIndex() == 0 && !hasSelection()) {
                            final int x = (int) event.getX();
                            final int y = (int) event.getY();

                            NoteImageSpan span = getSpan(start);

                            if (span != null && span.contains(x, y) &&
                                    handleSpanClick(span, x, start)) {
                                return true;
                            }
                        }
                    } else {
                        return true;
                    }
                }

                break;
            case MotionEvent.ACTION_CANCEL:
                clearSelectedSpan();
                removeCheckLongPress();

                if (Math.abs(event.getY() - mDownY) > mScaleTouchSlop) {
                    hideSoftInput();
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    private void notifyCursorLineChange(String rowStr) {
        if (mOnCursorLineChangedListener != null) {
            mOnCursorLineChangedListener.onCursorLineChanged(rowStr);
        }
    }

    private boolean handleSpanClick(NoteImageSpan span, int x, int selStart) {
        Log.d(TAG, "handleSpanClick() x = " + x + ", selStart = " + selStart);
        return (span.getType() == NoteImageSpan.Type.Symbol ||
                (span.getType() == NoteImageSpan.Type.Image &&
                        x <= span.getRight() - SPAN_RIGHT_HOT_AREA)) &&
                mOnSpanClickListener != null &&
                mOnSpanClickListener.onSpanClick(getText(), span, selStart);
    }

    private boolean handleSpanLongClick() {
        Log.d(TAG, "handleSpanLongClick()");
        return mSelectedSpan != null &&
                mOnSpanLongClickListener != null &&
                mOnSpanLongClickListener.onSpanLongClick(getText(), mSelectedSpan);
    }

    private void startCheckLongPress() {
        Log.d(TAG, "startCheckLongPress()");
        if (mCheckLongPress == null) {
            mCheckLongPress = new CheckLongPress();
        }
        mCheckLongPress.rememberWindowAttachCount();
        postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout());
    }

    private void removeCheckLongPress() {
        Log.d(TAG, "removeCheckLongPress()");
        if (mCheckLongPress != null) {
            removeCallbacks(mCheckLongPress);
        }
    }

    public void hideSoftInput() {
        if (mImm == null) {
            mImm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        if (mImm.isActive(this)) {
            mImm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private int getStart(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= getTotalPaddingLeft();
        y -= getTotalPaddingTop();

        x += getScrollX();
        y += getScrollY();

        int line = getLayout().getLineForVertical(y);
        return getLayout().getOffsetForHorizontal(line, x);
    }

    private <T> T[] getSpans(int start, Class<T> type) {
        return getText().getSpans(start, start, type);
    }

    private NoteImageSpan getSpan(int start) {
        NoteImageSpan[] spans = getSpans(start, NoteImageSpan.class);
        if (spans != null && spans.length > 0) {
            return spans[0];
        }
        return null;
    }

    private void selectSpan(int start, MotionEvent event) {
        Log.d(TAG, "selectSpan() start = " + start);
        NoteImageSpan span = getSpan(start);
        if (span != null &&
                span.getType() == NoteImageSpan.Type.Image &&
                span.contains((int) event.getX(), (int) event.getY())) {
            mSelectedSpan = span;
        }
    }

    public void refreshSpan(NoteImageSpan span) {
        final Editable editable = getText();
        editable.setSpan(span, editable.getSpanStart(span),
                editable.getSpanEnd(span), editable.getSpanFlags(span));
    }

    public void selectSpan(NoteImageSpan span) {
        if (span != null) {
            span.setSelected(true);
            refreshSpan(span);
        }
        mSelectedSpan = span;
    }

    public void clearSelectedSpan() {
        Log.d(TAG, "clearSelectedSpan()");
        if (mSelectedSpan != null && mSelectedSpan.isSelected()) {
            mSelectedSpan.setSelected(false);
            refreshSpan(mSelectedSpan);
        }
        mSelectedSpan = null;
    }

    public void emptySelectedSpan() {
        mSelectedSpan = null;
    }

    public void setHandleEvent(boolean handleEvent) {
        mHandleEvent = handleEvent;
    }
}
