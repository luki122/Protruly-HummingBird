package com.android.mms.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.util.MmsLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lichao on 17-4-26.
 */

public class MyTextViewSnippet extends TextView {
    private static final String TAG = "Mms/MyTextViewSnippet";
    private static final boolean DEBUG = false;

    private static String sEllipsis = "\u2026";

    private static int sTypefaceHighlight = Typeface.BOLD;

    private String mFullText;
    private String mTargetString;
    private Pattern mPattern;

    public MyTextViewSnippet(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTextViewSnippet(Context context) {
        super(context);
    }

    public MyTextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * We have to know our width before we can compute the snippet string.  Do that
     * here and then defer to super for whatever work is normally done.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //lichao add for NullPointerException begin
        if(DEBUG) Log.d(TAG, "\n\n onLayout, changed="+changed+", left="+left+", top="+top+", right="+right+", bottom="+bottom);
        if(DEBUG) Log.d(TAG, "onLayout, mFullText="+mFullText);
        if(DEBUG) Log.d(TAG, "onLayout, mTargetString="+mTargetString);
        if (TextUtils.isEmpty(mFullText)) {
            if(DEBUG) Log.d(TAG, "onLayout, mFullText isEmpty, setText()");
            setText("");
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        if (TextUtils.isEmpty(mTargetString)) {
            if(DEBUG) Log.d(TAG, "onLayout, mTargetString isEmpty, setText(mFullText)");
            //setText(mFullText);
            setText(new SpannableString(mFullText));
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        //lichao add for NullPointerException end

        String fullTextLower = mFullText.toLowerCase();
        if(DEBUG) Log.d(TAG, "onLayout, fullTextLower="+fullTextLower);
        String targetStringLower = mTargetString.toLowerCase();
        if(DEBUG) Log.d(TAG, "onLayout, targetStringLower="+targetStringLower);

        int startPos = 0;
        int searchStringLength = targetStringLower.length();
        int bodyLength = fullTextLower.length();

        String patternString = Pattern.quote(mTargetString);
        if(DEBUG) Log.d(TAG, "onLayout, patternString=" + patternString);
        mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        Matcher matcher = mPattern.matcher(mFullText);
        if (matcher.find(0)) {
            startPos = matcher.start();
        }
        if(DEBUG) Log.d(TAG, "onLayout startPos = " + startPos);

        TextPaint tp = getPaint();

        /*
        //2017-05-11 log:
        Mms/MyTextViewSnippet:  onLayout, changed=false, left=684, top=6, right=984, bottom=63
        Mms/MyTextViewSnippet: onLayout, mFullText=18124559395
        Mms/MyTextViewSnippet: onLayout, mTargetString=1
        Mms/MyTextViewSnippet: onLayout, fullTextLower=18124559395
        Mms/MyTextViewSnippet: onLayout, targetStringLower=1
        Mms/MyTextViewSnippet: onLayout, patternString=\Q1\E
        Mms/MyTextViewSnippet: onLayout startPos = 0
        Mms/MyTextViewSnippet: onLayout searchStringWidthPos = 24.0
        Mms/MyTextViewSnippet: onLayout textFieldWidth = 300.0
        Mms/MyTextViewSnippet: onLayout fullTextWidth = 264.0
        Mms/MyTextViewSnippet: onLayout fullTextLength = 11
        Mms/MyTextViewSnippet: onLayout textFieldWidth = 300.0
        Mms/MyTextViewSnippet: onLayout 333 snippetString = 18124559395
        Mms/MyTextViewSnippet: onLayout 444 snippetString = 18124559395
        Mms/MyTextViewSnippet: onLayout(): start = 0, matcher.end() = 1
        Mms/MyTextViewSnippet: onLayout(): start = 1, matcher.end() = 3
        */
        float searchStringWidth = tp.measureText(mTargetString);
        if(DEBUG) Log.d(TAG, "onLayout searchStringWidthPos = " + searchStringWidth);
        // when two textview in one line, if use getWidth() here,
        // should have a definite width in xml, such as android:layout_width="100dip"
        float textFieldWidth = getWidth();
        if(DEBUG) Log.d(TAG, "onLayout textFieldWidth = " + textFieldWidth);

        float fullTextWidth = tp.measureText(mFullText);
        if(DEBUG) Log.d(TAG, "onLayout fullTextWidth = " + fullTextWidth);

        int fullTextLength = mFullText.length();
        if(DEBUG) Log.d(TAG, "onLayout fullTextLength = " + fullTextLength);

        /// M: google jb.mr1 patch, Modify to take Ellipsis for avoiding JE
        /// assume we'll need one on both ends @{
        float ellipsisWidth = tp.measureText(sEllipsis);
        //减去省略号的长度
        if(fullTextWidth > textFieldWidth){
            textFieldWidth -= (2F * ellipsisWidth);
        }

        if(DEBUG) Log.d(TAG, "onLayout textFieldWidth = " + textFieldWidth);
        /// @}
        String snippetString = null;
        /// M: add "=".
        if (searchStringWidth >= textFieldWidth) {
            /// M: Code analyze 006, For fix bug ALPS00280615, The tips mms
            // has stopped show and JE happen after clicking the longer
            // search suggestion. @{
            try {
                snippetString = mFullText.substring(startPos, startPos + searchStringLength);
                if(DEBUG) Log.d(TAG, "onLayout 111 snippetString = " + snippetString);
            } catch (Exception e) {
                MmsLog.w(TAG, " StringIndexOutOfBoundsException ");
                e.printStackTrace();
                /// M: for search je.
                snippetString = mFullText;
            }
            /// @}
        } else {
            int offset = -1;
            int start = -1;
            int end = -1;
                /* TODO: this code could be made more efficient by only measuring the additional
                 * characters as we widen the string rather than measuring the whole new
                 * string each time.
                 */
            while (true) {
                offset += 1;

                int newstart = Math.max(0, startPos - offset);
                int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

                if (newstart == start && newend == end) {
                    // if we couldn't expand out any further then we're done
                    break;
                }
                start = newstart;
                end = newend;

                // pull the candidate string out of the full text rather than body
                // because body has been toLower()'ed
                String candidate = mFullText.substring(start, end);
                if (tp.measureText(candidate) > textFieldWidth) {
                    // if the newly computed width would exceed our bounds then we're done
                    // do not use this "candidate"
                    break;
                }

                snippetString = String.format(
                        "%s%s%s",
                        start == 0 ? "" : sEllipsis,
                        candidate,
                        end == bodyLength ? "" : sEllipsis);
                //if(DEBUG) Log.d(TAG, "onLayout 222 snippetString = " + snippetString);
            }
        }
        if(DEBUG) Log.d(TAG, "onLayout 333 snippetString = " + snippetString);
        if (snippetString == null) {
            //lichao: should not use mFullText.length() here
            if (textFieldWidth >= fullTextWidth) {
                snippetString = mFullText;
            } else {
                snippetString = mFullText.substring(0, (int) textFieldWidth);
            }
        }
        if(DEBUG) Log.d(TAG, "onLayout 444 snippetString = " + snippetString);
        SpannableString spannable = new SpannableString(snippetString);
        int start = 0;

        matcher = mPattern.matcher(snippetString);
        while (matcher.find(start)) {
            if(DEBUG) MmsLog.w(TAG, "onLayout(): start = " + start + ", matcher.end() = " + matcher.end());
            if (start == matcher.end()) {
                break;
            }
            //lichao modify in 2017-04-10 begin
            //spannable.setSpan(new StyleSpan(sTypefaceHighlight), matcher.start(), matcher.end(), 0);
            int hightlight_color = mContext.getResources().getColor(R.color.search_hightlight_color);
            spannable.setSpan(new ForegroundColorSpan(hightlight_color), matcher.start(), matcher.end(), 0);
            //lichao modify in 2017-04-10 end
            start = matcher.end();
        }
        setText(spannable);
        // do this after the call to setText() above
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setText(String fullText, String target) {
        if(DEBUG) Log.d(TAG, "\n\n setText, fullText=" + fullText);
        if(target.isEmpty()){
            if(DEBUG) Log.d(TAG, "setText, target is empty");
            mPattern = null;
        }else{
            if(DEBUG) Log.d(TAG, "setText, target=" + target);
            // Use a regular expression to locate the target string
            // within the full text.  The target string must be
            // found as a word start so we use \b which matches
            // word boundaries.
            String patternString = Pattern.quote(target);
            if(DEBUG) Log.d(TAG, "setText, patternString=" + patternString);
            mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        }
        mFullText = fullText;
        mTargetString = target;
        if(DEBUG) Log.d(TAG, "setText, >>>requestLayout()");
        requestLayout();
    }
}
