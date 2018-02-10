package com.hb.note.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

public class NoteBulletSpan extends ImageSpan {
    public NoteBulletSpan(Drawable d, String source) {
        super(d, source);
    }

    @Override
    public int getSize(Paint paint, CharSequence text,
                       int start, int end,
                       Paint.FontMetricsInt fm) {
        return super.getSize(paint, text, start, end, fm);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, Paint paint) {
        Drawable d = getDrawable();
        canvas.save();
        canvas.translate(x, top + 9);
        d.draw(canvas);
        canvas.restore();
    }
}
