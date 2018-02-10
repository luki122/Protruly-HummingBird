package com.protruly.music.model;

/**
 * Created by hujianwei on 17-8-31.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import java.util.ArrayList;

import com.protruly.music.model.OTAFrameAnimation.AnimationImageListener;



public class OTAMainPageFrameLayout extends ImageButton {

    private OTAFrameAnimation mFrameAnimation;
    private Context mContext;
    private ArrayList<Integer> mImageId;
    private ArrayList<Integer> mDurations;
    private AnimationUtils utils;
    private int animationXml;

    public OTAMainPageFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public OTAMainPageFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public OTAMainPageFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mFrameAnimation = new OTAFrameAnimation(context,this);
        utils = new AnimationUtils();
    }

    public void setAnimationListener(AnimationImageListener mAnimationListener){
        mFrameAnimation.setAnimationImageListener(mAnimationListener);
    }

    public void setFrameAnimationList(int resId){
        this.animationXml = resId;
        utils.parseFrame(mContext, animationXml);
        mImageId =utils.getImages();
        mDurations = utils.getDuration();
    }

    public void startAnim() {
        mFrameAnimation.initRes(mImageId, mDurations);
        mFrameAnimation.start();
    }

    public void stopAnim(){
        mFrameAnimation.stop();
    }
}
