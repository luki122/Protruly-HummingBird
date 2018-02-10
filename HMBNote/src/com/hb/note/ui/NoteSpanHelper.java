package com.hb.note.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import com.hb.note.NoteApplication;
import com.hb.note.R;
import com.hb.note.util.Globals;
import com.hb.note.util.SystemUtils;
import com.hb.note.util.Utils;

import static com.hb.note.util.BitmapUtils.getBitmap;

public class NoteSpanHelper {

    private static final String TAG = "NoteSpanHelper";

    private static int getTextSize(int resId) {
        return NoteApplication.getInstance().getResources().getDimensionPixelSize(resId);
    }

    private static Drawable getBulletDrawable() {
        Drawable d = NoteApplication.getInstance().getDrawable(R.drawable.ic_bullet);
        if (d != null) {
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
        return d;
    }

    private static Drawable getSymbolDrawable(String dest) {
        int resId = R.drawable.ic_bill;
        if (Globals.SPAN_SYMBOL_BILL_DONE.equals(dest)) {
            int[] resIds = Utils.getResIds();
            resId = resIds[8];
        }

        Drawable d = NoteApplication.getInstance().getDrawable(resId);
        if (d != null) {
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
        return d;
    }

    private static Drawable getDrawable(String imagePath, int width) {
        imagePath = imagePath.startsWith(Globals.FILE_PROTOCOL) ?
                imagePath.substring(Globals.FILE_PROTOCOL_LENGTH) : imagePath;

        Bitmap bitmap = null;
        try {
            bitmap = getBitmap(imagePath, width);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "get bitmap failed by OutOfMemoryError!");
        }

        Drawable d;
        if (bitmap != null) {
            d = new BitmapDrawable(NoteApplication.getInstance().getResources(), bitmap);
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            d = NoteApplication.getInstance().getDrawable(R.drawable.image_load_failed);
            if (d != null) {
                d.setBounds(0, 0, width, width * 122 / 155);
            }
        }
        return d;
    }

    private static SpannableString getSpannableString(String source, Object span, int flags) {
        SpannableString ss = new SpannableString(source);
        ss.setSpan(span, 0, source.length(), flags);
        return ss;
    }

    private static SpannableString getTitleSpan(String source) {
        return getSpannableString(
                source, new AbsoluteSizeSpan(getTextSize(R.dimen.editor_title_size)),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getSubtitleSpan(String source) {
        return getSpannableString(
                source, new AbsoluteSizeSpan(getTextSize(R.dimen.editor_subtitle_size)),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getUnderLineSpan(String source) {
        return getSpannableString(
                source, new UnderlineSpan(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getStrikeThroughSpan(String source) {
        return getSpannableString(
                source, new StrikethroughSpan(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getTextSpan(String dest, String text) {
        SpannableString ss;
        if (Globals.SPAN_TITLE.equals(dest)) {
            ss = getTitleSpan(text);
        } else if (Globals.SPAN_SUBTITLE.equals(dest)) {
            ss = getSubtitleSpan(text);
        } else if (Globals.SPAN_UNDER_LINE.equals(dest)) {
            ss = getUnderLineSpan(text);
        } else {
            ss = getStrikeThroughSpan(text);
        }
        return ss;
    }

    private static SpannableString getBulletSpan(String source) {
        return getSpannableString(
                source, new NoteBulletSpan(getBulletDrawable(), source),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getNullSpan(String source) {
        NoteNullSpan span = new NoteNullSpan(null, source);
        return getSpannableString(source, span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static SpannableString getSymbolSpan(
            String source, String dest, int leftPadding, int topPadding) {

        NoteImageSpan span = new NoteImageSpan(getSymbolDrawable(dest), dest,
                NoteImageSpan.Type.Symbol, -1, leftPadding, topPadding, null);
        return getSpannableString(source, span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static SpannableString getImageSpan(
            String source, String dest, int leftPadding, int topPadding,
            int width, int rightExtraSpace) {

        String imagePath = dest.substring(Globals.SPAN_IMAGE_LENGTH);
        NoteImageSpan span = new NoteImageSpan(getDrawable(imagePath, width), imagePath,
                NoteImageSpan.Type.Image, rightExtraSpace, leftPadding, topPadding, null);
        return getSpannableString(source, span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static SpannableString string2SpannableString(
            String content, int width, int rightExtraSpace, int leftPadding, int topPadding) {

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        int pos = 0;
        do {
            int spanStart = content.indexOf(Globals.SPAN_START, pos);
            if (spanStart == -1) {
                ssb.append(content, pos, content.length());
                break;
            } else {
                ssb.append(content, pos, spanStart);
                pos = spanStart;
            }

            int spanEndIndex = content.indexOf(Globals.SPAN_END, pos);
            if (spanEndIndex == -1) {
                Log.w(TAG, "Span has start but have not end.");
                ssb.append(content, pos, content.length());
                break;
            } else {
                /*int start = content.lastIndexOf(Globals.SPAN_START, spanEnd);
                if (spanStart < start) {
                    Log.w(TAG, "Span has start but have not end.");
                    ssb.append(content, spanStart, start);
                    pos = start;
                    continue;
                }*/

                pos = spanEndIndex + Globals.SPAN_END_LENGTH;
                String source = content.substring(spanStart, pos);
                String dest = content.substring(spanStart + Globals.SPAN_START_LENGTH, spanEndIndex);

                if (Globals.SPAN_BULLET.equals(dest)) {
                    ssb.append(getBulletSpan(source));
                } else if (dest.startsWith(Globals.SPAN_SYMBOL)) {
                    ssb.append(
                            getSymbolSpan(source, dest, leftPadding, topPadding));

                    if (dest.equals(Globals.SPAN_SYMBOL_BILL_DONE)) {
                        int rowEnd = SystemUtils.getRowEnd(content, pos);
                        if (rowEnd > pos) {
                            ssb.append(getTextSpan(
                                    Globals.SPAN_STRIKE_THROUGH, content.substring(pos, rowEnd)));
                            pos = rowEnd;
                        }
                    }
                } else if (dest.startsWith(Globals.SPAN_IMAGE)) {
                    ssb.append(
                            getImageSpan(source, dest, leftPadding, topPadding,
                                    width, rightExtraSpace));
                } else {
                    ssb.append(getNullSpan(source));

                    int rowEnd = SystemUtils.getRowEnd(content, pos);
                    if (rowEnd > pos) {
                        ssb.append(getTextSpan(dest, content.substring(pos, rowEnd)));
                        pos = rowEnd;
                    }
                }
            }
        } while (pos < content.length());

        return SpannableString.valueOf(ssb);
    }
}
