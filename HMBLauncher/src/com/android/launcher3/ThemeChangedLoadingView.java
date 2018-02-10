package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by lijun on 17-6-29.
 */

public class ThemeChangedLoadingView extends FrameLayout {

    ImageView loadingView;
    ObjectAnimator rot;
    AnimatorSet mHideAnimator = null;

    public ThemeChangedLoadingView(Context context) {
        this(context, null);
    }

    public ThemeChangedLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemeChangedLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        loadingView = (ImageView) findViewById(R.id.theme_loading_icon);
    }

    public void showLoading() {
        this.setVisibility(View.VISIBLE);
        this.setAlpha(1.0f);
        this.setTranslationY(0);
        rot = ObjectAnimator.ofFloat(loadingView, "rotation", 0, 359);
        rot.setDuration(2000);
        rot.setRepeatCount(-1);
        rot.setInterpolator(new LinearInterpolator());
        rot.start();
    }

    public void hideLoading() {
        if (rot != null) {
            rot.cancel();
            rot = null;
        }
        if(mHideAnimator !=null) {
//            mHideAnimator.cancel();
//            mHideAnimator = null;
            return;
        }

        LauncherViewPropertyAnimator animation = new LauncherViewPropertyAnimator(this);
        animation.translationY(this.getMeasuredHeight());
        animation.setDuration(600);

        mHideAnimator = LauncherAnimUtils.createAnimatorSet();
        mHideAnimator.play(animation);
        mHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mHideAnimator = null;
                ThemeChangedLoadingView.this.setVisibility(View.GONE);
                ThemeChangedLoadingView.this.setTranslationY(0.0f);
            }
        });
        mHideAnimator.start();
    }

    public boolean isThemeLoading() {
        return getVisibility() == VISIBLE && getAlpha() == 1.0f;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getAlpha() > 0.01 && getVisibility() == View.VISIBLE) {
            return true;//if is showing ,return
        }
        return super.onTouchEvent(event);
    }
}
