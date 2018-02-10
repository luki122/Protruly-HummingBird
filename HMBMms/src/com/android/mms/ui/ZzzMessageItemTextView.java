package com.android.mms.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;

import com.android.mms.R;

/** 
 * tangyisen add
 */  
public class ZzzMessageItemTextView extends TextView {

    private OnDoubleClickListener mOnDoubleClickListener;
    private OnSingleClickListener mOnSingleClickListener;

    private GestureDetector mGestureDetector;

    private String mCurrClickUrl = null;
    private URLSpan mCurrClickSpan = null;
    private final static String RECE_SPAN_COLOR = "#FF6EB91D";
    private final static String SEND_SPAN_COLOR = "#DE000000";
    private boolean mIsOutgoing;

    public ZzzMessageItemTextView(Context context) {
        this(context, null);
    }

    public ZzzMessageItemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContent(CharSequence charSequence, boolean isInbox) {
        setText(charSequence);
        stripUnderlines(isInbox);
    }

    public void setIsOutgoing(boolean isOutgoing) {
        mIsOutgoing = isOutgoing;
    }

    /*@Override
    public void setPadding(int left, int top, int right, int bottom) {
        // the super call will requestLayout()
        super.setPadding(0, 0, 0, 0);
        invalidate();
    }*/

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        return super.dispatchTouchEvent(ev);
    }

    private boolean touchFlag = false;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            
            if (!touchFlag) {
                touchFlag = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try{
                            Thread.sleep(300);
                        }catch(Exception e){
                            e.printStackTrace();
                        }finally{
                            if (touchFlag && null != mCurrClickSpan && null != mCurrClickUrl) {
                                post(new Runnable() {
                                    public void run() {
                                        MessageUtils.onMessageSpansClick(getContext(), ZzzMessageItemTextView.this, mCurrClickUrl, mCurrClickSpan);
                                        mCurrClickUrl = null;
                                        mCurrClickSpan = null;
                                    }
                                });
                            }
                            touchFlag = false;
                        }
                    } 
               }).start();
            }else {
                touchFlag = false;
                if(mOnDoubleClickListener != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            //myHandler.rem
                            mOnDoubleClickListener.onDoubleClick();
                        }
                    });
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public interface OnDoubleClickListener {
        void onDoubleClick();
    }

    public interface OnSingleClickListener {
        void onSingleClick();
    }

    public void setOnDoubleClickListener(OnDoubleClickListener listener) {
        mOnDoubleClickListener = listener;
    }

    public void setOnSingleClickListener(OnSingleClickListener listener) {
        mOnSingleClickListener = listener;
    }

    private boolean hasSpan() {
        final URLSpan[] spans = getUrls();
        if (spans.length == 0) {
            return false;
        }
        return true;
    }

    public void stripUnderlines(boolean isInbox) {
        if (getText() instanceof Spannable) {
            Spannable s = (Spannable)getText();
            URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
            for (URLSpan span : spans) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                s.removeSpan(span);
                if (!isInbox) {
                    span = new URLSpanNoUnderline(span.getURL(), SEND_SPAN_COLOR);
                } else {
                    span = new URLSpanNoUnderline(span.getURL(), RECE_SPAN_COLOR );
                }
                s.setSpan(span, start, end, 0);
            }
            //textView.setText(s);
        }
    }

    private class URLSpanNoUnderline extends URLSpan {
        private boolean isCallSuperClick = false;
        private static final String MAIL_TO_PREFIX = "mailto:";
        private String mColor; 
        public URLSpanNoUnderline(String url, String color) {
            super(url);
            mColor = color;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(Color.parseColor(mColor));
            //ds.setUnderlineText(false);
        }

        @Override
        public void onClick(View view) {
            // TODO Auto-generated method stub
            if (!isCallSuperClick) {
                String url = new String(getURL());
                if (MessageUtils.isWebUrl(url)) {
                    // showUrlOptions(context, url);
                    isCallSuperClick = false;
                }
                else {
                    final String telPrefix = "tel:";
                    if (url.startsWith(telPrefix)) {
                        url = url.substring(telPrefix.length());
                        if (PhoneNumberUtils.isWellFormedSmsAddress(url)) {
                            // showNumberOptions(context, url);
                            isCallSuperClick = false;
                        }
                    }
                    else if (url.startsWith(MAIL_TO_PREFIX)) {
                        // url = url.substring(MAIL_TO_PREFIX.length());
                        // showEmailOptions(context, url);
                        isCallSuperClick = false;
                    }
                    else {
                        // span.onClick(contentText);
                        isCallSuperClick = true;
                    }
                }
                mCurrClickSpan = this;
                mCurrClickUrl = getURL();
            } else {
                super.onClick(view);
            }
        }
    }
}