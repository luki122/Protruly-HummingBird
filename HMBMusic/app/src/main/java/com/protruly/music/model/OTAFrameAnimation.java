package com.protruly.music.model;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by hujianwei on 17-8-31.
 */

public class OTAFrameAnimation {

    private Handler handler;
    private View view;
    private AnimationDrawable drawable;
    private AnimationImageListener animationImageListener;

    private FrameCallback[] callbacks;
    private Drawable[] frames;
    private int[] ids;
    private int[] durations;
    private int frameCount;

    private boolean isRun;
    private boolean fillAfter;
    private boolean isOneShot;
    private boolean isLimitless;
    private int repeatTime;

    private int currentRepeat;
    private int currentFrame;
    private int currentTime;

    private ArrayList<Integer> mImageId;
    private ArrayList<Integer> mDurations;
    private Context mContext;
    private Runnable nextFrameRun = new Runnable() {
        public void run() {
            if(!isRun) {
                end();
                return;
            }

            currentTime += durations[currentFrame];
            if(callbacks[currentFrame] != null) {
                callbacks[currentFrame].onFrameEnd(currentFrame);
            }
            nextFrame();
        }
    };

    public OTAFrameAnimation(Context context,View view) {

        this.view = view;
        this.handler = new Handler();
        mContext = context;

    }

    public void initRes(ArrayList<Integer> images,ArrayList<Integer> durations){
        mImageId = images;
        mDurations = durations;
        init();
    }

    private void init() {
        this.frameCount = mImageId.size();//getNumberOfFrames();
        this.frames = new Drawable[frameCount];
        this.ids = new int[frameCount];
        this.durations = new int[frameCount];
        this.callbacks = new FrameCallback[frameCount];
        this.isRun = false;
        this.fillAfter = false;
        this.isOneShot = true;
        this.isLimitless = false;
        this.repeatTime = 2;

        for(int i = 0; i < frameCount; i++) {
            int id = mImageId.get(i);
            if(id != 0){
                //frames[i] = mContext.getResources().getDrawable(id);
                ids[i] = id;
            }
            durations[i] = mDurations.get(i);
        }

    }

    public void start() {
        if(isRun) {
            return;
        }
        this.isRun = true;
        this.currentRepeat = -1;
        this.currentFrame = -1;
        this.currentTime = 0;
        if(animationImageListener != null) {
            animationImageListener.onAnimationStart();
        }
        startProcess();
    }

    public void stop() {
        this.isRun = false;
    }

    private void startProcess() {
        this.currentFrame = 0;
        this.currentTime = 0;
        this.currentRepeat ++;
        if(animationImageListener != null) {
            animationImageListener.onRepeat(currentRepeat);
        }
        nextFrame();
    }

    private void endProcess() {
        if(isOneShot || (!isLimitless && currentRepeat >= repeatTime - 1) || !isRun) {
            end();
        } else {
            startProcess();
        }
    }

    private void end() {
        if(!fillAfter && frameCount > 0) {
            view.setBackgroundResource(ids[1]);
        }
        if(animationImageListener != null) {
            animationImageListener.onAnimationEnd();
        }
        this.isRun = false;
    }

    private void nextFrame() {
        if(currentFrame == frameCount - 1) {
            endProcess();
            return;
        }

        currentFrame ++;

        changeFrame(currentFrame);
        handler.postDelayed(nextFrameRun, durations[currentFrame]);
    }

    private void changeFrame(int frameIndex) {

//        view.setBackgroundDrawable(frames[frameIndex]);
        view.setBackgroundResource(ids[frameIndex]);
        if(animationImageListener != null) {
            animationImageListener.onFrameChange(currentRepeat, frameIndex, currentTime);
        }

        if(callbacks[currentFrame] != null) {
            callbacks[currentFrame].onFrameStart(frameIndex);
        }
    }

    public int getSumDuration() {
        int sumDuration = 0;
        for(int duration : durations) {
            sumDuration += duration;
        }
        return sumDuration;
    }

    public boolean isOneShot() {
        return isOneShot;
    }

    public void setOneShot(boolean isOneShot) {
        this.isOneShot = isOneShot;
    }

    public void stopAnimation(){
        endProcess();
    }

    public boolean isFillAfter() {
        return fillAfter;
    }

    public void setFillAfter(boolean fillAfter) {
        this.fillAfter = fillAfter;
    }

    public boolean isLimitless() {
        return isLimitless;
    }

    public void setLimitless(boolean isLimitless) {
        if(isLimitless) {
            setOneShot(false);
        }
        this.isLimitless = isLimitless;
    }

    public void addFrameCallback(int index, FrameCallback callback) {
        this.callbacks[index] = callback;
    }

    public void setAnimationImageListener(AnimationImageListener animationImageListener) {
        this.animationImageListener = animationImageListener;
    }

    public int getRepeatTime() {
        return repeatTime;
    }

    public void setRepeatTime(int repeatTime) {
        this.repeatTime = repeatTime;
    }

    public interface AnimationImageListener{
        public void onAnimationStart();
        public void onAnimationEnd();
        public void onRepeat(int repeatIndex);
        public void onFrameChange(int repeatIndex, int frameIndex, int currentTime);
    }

    public interface FrameCallback {
        public void onFrameStart(int startTime);
        public void onFrameEnd(int endTime);
    }
}
