package com.protruly.music.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.protruly.music.R;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBImageButton extends RelativeLayout {

    private static final String TAG = "HBImageButton";
    private Context mContext;
    private boolean mChecked;
    private ImageView mButton = null;
    private TextView mTextView = null;



    public HBImageButton(Context context) {
        //this(context, null, 0);
        super(context);
        init(context);
    }

    public HBImageButton(Context context, AttributeSet attrs) {
        //this(context, attrs, 0);
        super(context, attrs);
        init(context);
    }

    public HBImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {

        mContext = context;
        LayoutInflater.from(mContext).inflate(R.layout.hb_imagebutton, this);
        mButton = (ImageView)findViewById(R.id.my_bt_img);
        mTextView = (TextView)findViewById(R.id.my_bt_text);

        this.setClickable(true);
        this.setFocusable(true);

        return;
    }

    public void setImgResource(int resId) {
        if (mButton != null) {
            mButton.setImageResource(resId);
        }
    }

    public void setText(int resid) {
        if (mTextView != null) {
            mTextView.setText(resid);
        }
    }

    public void setText(String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

}
