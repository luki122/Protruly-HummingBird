package com.hb.note.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;
import android.util.Log;

import com.hb.note.util.Globals;

public class NoteImageSpan extends ImageSpan {

    private static final String TAG = "NoteImageSpan";
    private static final int SYMBOL_OFFSET = 9;
    private static final int IMAGE_OFFSET = 6;

    private int mLeft, mTop;
    private boolean mSelected;

    private Drawable mDrawable, mSelectedDrawable;
    private Type mType;
    private int mRightExtraSpace, mLeftPadding, mTopPadding;

    public enum Type {
        Symbol,
        Image
    }

    public NoteImageSpan(Drawable d, String source, Type type,
                         int rightExtraSpace, int leftPadding, int topPadding,
                         Drawable selectedDrawable) {
        super(d, source);

        mDrawable = d;
        mType = type;
        mRightExtraSpace = rightExtraSpace;
        mLeftPadding = leftPadding;
        mTopPadding = topPadding;
        mSelectedDrawable = selectedDrawable;
    }

    public Type getType() {
        return mType;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    public int getWidth() {
        return getDrawable().getBounds().width();
    }

    public int getHeight() {
        return getDrawable().getBounds().height();
    }

    public int getLeft() {
        return mLeft;
    }

    public void setLeft(int mLeft) {
        this.mLeft = mLeft;
    }

    public int getTop() {
        return mTop;
    }

    public void setTop(int mTop) {
        this.mTop = mTop;
    }

    public int getRight() {
        return getLeft() + getWidth();
    }

    public int getBottom() {
        return getTop() + getHeight();
    }

    public boolean contains(int x, int y) {
        return x >= getLeft() && x <= getRight() && y >= getTop() && y <= getBottom();
    }

    public String getSource(int x, int y) {
        if (contains(x, y)) {
            return getSource();
        }
        return null;
    }

    public Type getType(int x, int y) {
        if (contains(x, y)) {
            return getType();
        }
        return null;
    }

    public NoteImageSpan getSpan(int x, int y) {
        if (contains(x, y)) {
            return this;
        }
        return null;
    }

    @Override
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();

        Log.d(TAG, rect.toString() + Globals.NEW_LINE + (fm != null ? fm.toString() : ""));

        if (fm != null) {
            fm.ascent = -rect.bottom;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }

        if (mType == Type.Symbol) {
            return rect.right;
        } else {
            return rect.right + mRightExtraSpace;
        }
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        Drawable d = getDrawable();
        canvas.save();

        int transY = top + IMAGE_OFFSET;
        if (mType == Type.Symbol) {
            transY = top + SYMBOL_OFFSET;
        }

        canvas.translate(x, transY);
        setLeft((int) x + mLeftPadding);
        setTop(transY + mTopPadding);
        d.draw(canvas);

        if (mType == Type.Image && mSelected && mSelectedDrawable != null) {
            mSelectedDrawable.setBounds(d.getBounds());
            mSelectedDrawable.draw(canvas);
        }

        canvas.restore();
    }
}
