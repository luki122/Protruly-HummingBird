package com.protruly.music.model;

import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBAnimationModel  extends Animation {

    private static final String TAG = "HBAnimationModel";
    private OnAnimationListener mListener;
    private final WeakReference<View> mView;
    private static final WeakHashMap<View, HBAnimationModel> mPoex = new WeakHashMap<View, HBAnimationModel>();

    private Handler mHandler;
    private float mInterpolatedTime = 0f;
    private Transformation mtTransformation;


    public static interface OnAnimationListener{
        public void onAnimationCallBack(View view, float interpolatedTime, Transformation t);
    }

    public static HBAnimationModel createAnimation(View view) {
        HBAnimationModel proxy = mPoex.get(view);
        if (proxy == null || proxy != view.getAnimation()) {
            proxy = new HBAnimationModel(view);
            mPoex.put(view, proxy);
        }
        return proxy;
    }

    public HBAnimationModel(View view) {
        mView = new WeakReference<View>(view);
        this.mHandler = new Handler();
    }

    public void setMyAnimationListener(OnAnimationListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        View view = mView.get();
        if (mListener != null) {
            mListener.onAnimationCallBack(view, interpolatedTime, t);
        }
    }

    public static void clear() {
        if (mPoex != null) {
            mPoex.clear();
        }
        return;
    }
}
