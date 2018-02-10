/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui.widget.multiwaveview;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Vibrator;
import android.util.Log;
import com.android.incallui.R;

public class CenterTargetDrawable extends TargetDrawable {
    private static final String TAG = "CenterTargetDrawable";
    protected float mRotation = 0.0f;
	private Paint mPointPaint, mRingerPaint;
	private float mPointRadius = 12;  
	private float mRingerRadius = 109; 
	CircleManager mCircleManager[];
	
    public void setRotation(float x) {
    	mRotation = x;
    }
    
	public class CircleManager {
	    protected float mScaleX = 1.0f;
	    protected float mScaleY = 1.0f;
	    protected float mAlpha = 1.0f;

	    public void setScaleX(float x) {
	        mScaleX = x;
	    }

	    public void setScaleY(float y) {
	        mScaleY = y;
	    }

	    public void setAlpha(float alpha) {
	        mAlpha = alpha;
	    }
	    
	    public float getScaleX() {
	        return mScaleX;
	    }

	    public float getScaleY() {
	    	return mScaleY;
	    }

	    public float getAlpha() {
	    	return mAlpha;
	    }
	};
    
    public CenterTargetDrawable(Resources res, int resId) {
    	super(res, resId, 1);    	
    	
    	mPointPaint = new Paint();
    	mPointPaint.setAntiAlias(true);
    	mPointPaint.setDither(true);
    	mPointPaint.setColor(0x7f015554);	
    	
    	mRingerPaint = new Paint();
    	mRingerPaint.setAntiAlias(true);
    	mRingerPaint.setDither(true);
    	mRingerPaint.setColor(0xFFFFFFFF);
    	mRingerPaint.setStyle(Paint.Style.STROKE);
    	mRingerPaint.setStrokeWidth(5);
    	
    	mPointRadius = res.getDimensionPixelSize(
				R.dimen.glow_center_point_radius);
    	mRingerRadius = res.getDimensionPixelSize(
				R.dimen.glow_center_ringer_radius);
    	
    	mCircleManager = new CircleManager[2];
    	for(int i = 0; i< mCircleManager.length; i++) {
    		mCircleManager[i] = new CircleManager();
    	}

    }

    public CenterTargetDrawable(TargetDrawable other) {
    	super(other);  
    }
    
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    	for(int i = 0; i< mCircleManager.length; i++) {
    		mCircleManager[i].setAlpha(alpha);
    	}
    }

    public void draw(Canvas canvas) {    
        if (mDrawable == null || !mEnabled) {
            return;
        }
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.translate(mTranslationX + mPositionX, mTranslationY + mPositionY);        
        mPointPaint.setAlpha((int) Math.round(mAlpha * 255f));
        canvas.drawCircle(0, 0, mPointRadius, mPointPaint);
        canvas.restore();
        
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(mCircleManager[0].getScaleX(), mCircleManager[0].getScaleY(), mPositionX, mPositionY);
        canvas.translate(mTranslationX + mPositionX, mTranslationY + mPositionY);        
        mRingerPaint.setAlpha((int) Math.round(mCircleManager[0].getAlpha() * 255f));
        canvas.drawCircle(0, 0, mRingerRadius, mRingerPaint);
        canvas.restore();
        
//        canvas.save(Canvas.MATRIX_SAVE_FLAG);
//        canvas.scale(mCircleManager[1].getScaleX(), mCircleManager[1].getScaleY(), mPositionX, mPositionY);
//        canvas.translate(mTranslationX + mPositionX, mTranslationY + mPositionY);        
//        mRingerPaint.setAlpha((int) Math.round(mCircleManager[1].getAlpha() * 255f));
//        canvas.drawCircle(0, 0, mRingerRadius, mRingerPaint);
//        canvas.restore();
    }
}
