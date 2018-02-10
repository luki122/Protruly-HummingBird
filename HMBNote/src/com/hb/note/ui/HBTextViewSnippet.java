package com.hb.note.ui;

import android.content.Context;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.hb.note.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HBTextViewSnippet extends TextView {

    private String mFullText;
    private String mTargetString;
    private Pattern mPattern;
    private int highlightColorId;

    public HBTextViewSnippet(Context context) {
        super(context);
        this.highlightColorId = R.color.home_text_color;
    }

    public HBTextViewSnippet(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.highlightColorId = R.color.home_text_color;
    }

    /**
     * We have to know our width before we can compute the snippet string.
     * Do that here and then defer to super for whatever work is normally done.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!TextUtils.isEmpty(mTargetString)) {
            String fullTextLower = mFullText.toLowerCase();
            int bodyLength = fullTextLower.length();

            TextPaint tp = getPaint();

            float textFieldWidth = getWidth() - getPaddingLeft() - getPaddingRight();
            float fullTextWidth = tp.measureText(mFullText);

            String snippetString = null;

            if (fullTextWidth <= textFieldWidth) {
                snippetString = mFullText;
            } else {
                for (int i = 1; i < bodyLength; i++) {
                    String candidate = mFullText.substring(0, i);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        snippetString = candidate;
                        break;
                    }
                }
            }

            if (snippetString == null) {
                return;
            }

            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;

            Matcher m = mPattern.matcher(snippetString);
            while (m.find(start)) {
                spannable.setSpan(
                        new ForegroundColorSpan(
                                getContext().getResources().getColor(highlightColorId)),
                        m.start(), m.end(), 0);
                start = m.end();
            }

            setText(spannable);
        }

        // do this after the call to setText() above
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setText(String fullText, String target, int highlightColorId) { //java.lang.StringIndexOutOfBoundsException
        // Use a regular expression to locate the target string
        // within the full text.  The target string must be
        // found as a word start so we use \b which matches
        // word boundaries.

        if (highlightColorId != 0) {
            this.highlightColorId = highlightColorId;
        }

        if (target != null) {
            if (target.contains("/%")) {
                target = target.replaceAll("/%", "%");
            }
            if (target.contains("//")) {
                target = target.replaceAll("//", "/");
            }
        }

        String patternString = Pattern.quote(target);
        mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

        mFullText = fullText;
        mTargetString = target;

        setText(fullText);
        requestLayout();
    }
}