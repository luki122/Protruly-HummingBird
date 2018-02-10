package com.hb.note.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class NoteNullSpan extends ImageSpan {
    public NoteNullSpan(Drawable d, String source) {
        super(d, source);
    }

    @Override
    public Drawable getDrawable() {
        return null;
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        if (fm != null) {
            fm.ascent = 0;
            fm.descent = 0;

            fm.top = fm.ascent;
            fm.bottom = 0;
        }
        return 0;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {

    }
}
