package com.hb.note.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class NoteEditorLayout extends RelativeLayout {

    public interface OnSizeChangedListener {
        void onSizeChanged(int w, int h, int oldw, int oldh);
    }

    private OnSizeChangedListener onSizeChangedListener;

    public void setOnSizeChangedListener(OnSizeChangedListener onSizeChangedListener) {
        this.onSizeChangedListener = onSizeChangedListener;
    }

    public NoteEditorLayout(Context context) {
        super(context);
    }

    public NoteEditorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoteEditorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (onSizeChangedListener != null) {
            onSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }
}
