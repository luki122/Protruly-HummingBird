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
import com.android.mms.R;

/** 
 * tangyisen add
 */  
public class ZzzMmsParentView extends LinearLayout {

    //type:send and recv
    public static final int TYPE_SEND = 0;
    public static final int TYPE_RECV = 1;

    private TextView contentText;
    ZzzCircleImageView mZzzCircleImageView;

    private boolean mIsSend;

    private OnDoubleClickListener mOnDoubleClickListener;
    private OnSingleClickListener mOnSingleClickListener;


    private String mCurrClickUrl = null;
    private URLSpan mCurrClickSpan = null;

    public ZzzMmsParentView(Context context) {
        this(context, null);
    }

    public ZzzMmsParentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        /*TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ZzzMmsParentView);
        int type = typedArray.getInt(R.styleable.StretchyTextView_strtype, TYPE_SEND);
        View view = null;
        if (TYPE_SEND == type) {
            mIsSend = true;
            view = inflate(context, R.layout.stretchy_text_send_layout, this);
        } else {
            mIsSend = false;
            view = inflate(context, R.layout.stretchy_text_recv_layout, this);
        }
        contentText = (TextView) view.findViewById(R.id.content_textview);*/
        //setOnLongClickListener(null);
    }

    private boolean touchFlag = false;

    public final void setContent(CharSequence charSequence) {
        //operateText.setVisibility(View.GONE);
        contentText.setText(charSequence);
        stripUnderlines(contentText);
    }

   public TextView getContentView() {
       return (TextView)contentText ;
   }

    public void setContentTextColor(int color){
        this.contentText.setTextColor(color);
    }

    public void setContentTextSize(float size){
        this.contentText.setTextSize(size);
    }

    @Override
    protected void onFinishInflate() {
    	// TODO Auto-generated method stub
    	super.onFinishInflate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        int action = ev.getAction();
        if (!MessageUtils.inRangeOfView( contentText, ev )) {
        	return false;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!touchFlag) {
                touchFlag = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try{
                            Thread.sleep(500);
                        }catch(Exception e){
                            e.printStackTrace();
                        }finally{
                            if (touchFlag && null != mCurrClickSpan && null != mCurrClickUrl) {
                                /*if(mOnSingleClickListener != null) {
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            mOnSingleClickListener.onSingleClick();
                                        }
                                    });
                                }*/
                                post(new Runnable() {
                                    public void run() {
                                        MessageUtils.onMessageSpansClick(getContext(), contentText, mCurrClickUrl, mCurrClickSpan);
                                        mCurrClickUrl = null;
                                        mCurrClickSpan = null;
                                    }
                                });
                            }
                            touchFlag = false;
                        }
                    } 
               }).start();
                //return false;
            }else {
                touchFlag = false;
                if(mOnDoubleClickListener != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            mCurrClickUrl = null;
                            mCurrClickSpan = null;
                            mOnDoubleClickListener.onDoubleClick();
                        }
                    });
                }
            }
        }
       // return false;
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        return super.dispatchTouchEvent(ev);
    }

    private boolean touchFlag2 = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        int action = event.getAction();
        if (!MessageUtils.inRangeOfView( contentText, event )) {
        	return super.onTouchEvent( event );
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!touchFlag2) {
                touchFlag2 = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try{
                            Thread.sleep(300);
                        }catch(Exception e){
                            e.printStackTrace();
                        }finally{
                            if (touchFlag && null != mCurrClickUrl) {
                                if(mOnSingleClickListener != null) {
                                    post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            mOnSingleClickListener.onSingleClick();
                                        }
                                    });
                                }
                            }
                            touchFlag2 = false;
                        }
                    } 
               }).start();
            }else {
                touchFlag2 = false;
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
        final URLSpan[] spans = contentText.getUrls();
        if (spans.length == 0) {
            return false;
        }
        return true;
    }

    private static String SPAN_TEXT_COLOR_RECV = "#FF6EB91D";
    private static String SPAN_TEXT_COLOR_SEND = "#FFFFFFFF";
    public void stripUnderlines(TextView textView) {
        if (null != textView && textView.getText() instanceof Spannable) {
            Spannable s = (Spannable)textView.getText();
            URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
            for (URLSpan span : spans) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                s.removeSpan(span);
                span = new URLSpanNoUnderline(span.getURL(), mIsSend ? SPAN_TEXT_COLOR_SEND : SPAN_TEXT_COLOR_RECV);
                s.setSpan(span, start, end, 0);
            }
            //textView.setText(s);
        }
    }

    private class URLSpanNoUnderline extends URLSpan {
        private boolean isCallSuperClick = false;
        private static final String MAIL_TO_PREFIX = "mailto:";
        String mSpanColor;
        public URLSpanNoUnderline(String url, String spanColor) {
            super(url);
            mSpanColor = spanColor;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(Color.parseColor(mSpanColor));
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