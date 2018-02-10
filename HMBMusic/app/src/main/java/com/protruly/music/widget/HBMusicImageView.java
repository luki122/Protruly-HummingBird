package com.protruly.music.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by hujianwei on 17-9-5.
 */

public class HBMusicImageView extends ImageView {

    private FrameLayout.LayoutParams mLayoutParams;

    public HBMusicImageView(Context context) {
        this(context, null);

    }

    public HBMusicImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public HBMusicImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mLayoutParams=(FrameLayout.LayoutParams)getLayoutParams();
    }

    public void setHight(int height){
        if(mLayoutParams!=null){
            mLayoutParams.height=height;
            setLayoutParams(mLayoutParams);
        }
    }

    public int getHight(){
        return getHeight();
    }

    public void setMargin(int margin){
        if (mLayoutParams != null) {
            mLayoutParams.setMargins(0, margin, 0, margin);
            setLayoutParams(mLayoutParams);
        }

    }

    public int getMargin(){
        if (mLayoutParams != null) {
            return mLayoutParams.bottomMargin;
        }
        return 0;
    }
}
