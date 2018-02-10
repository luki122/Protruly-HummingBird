/*
 * Copyright (C) 2012 The Android Open Source Project
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

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;
import android.util.Log;
import com.android.incallui.R;

public class HbPoint {
	private static final String TAG = "HbPoint";
	private float mCenterX;
	private float mCenterY;
	private float mScale = 1.0f;
	private int r = 212;
	private int gap = 22;
	private int pr = 7;
	final private int[] anim = {  -2, -1, 1, 2, 4, 6, 100};
	final private int[] anim2 = {  -2, -1, 2, 4, 7, 10, 100};
//	private Paint mPaint;
	private Paint[] mPaint;
	
	PointManager mPointManager = new PointManager();

	public class PointManager {
		private float point = 0f;
		private float point2 = 0f;
		private float point3 = 0f;
		private float alpha = 0f;

		public void setPoint(float r) {
			point = r;
		}

		public float getPoint() {
			return point;
		}
		
		public void setPoint2(float r) {
			point2 = r;
		}

		public float getPoint2() {
			return point2;
		}
		
		public void setPoint3(float r) {
			point3 = r;
		}

		public float getPoint3() {
			return point3;
		}

		public void setAlpha(float a) {
			alpha = a;
		}

		public float getAlpha() {
			return alpha;
		}
	};

	public HbPoint(Context context) {
//		mPaint = new Paint();
//		mPaint.setAntiAlias(true);
//		mPaint.setDither(true);
//		mPaint.setColor(0x7fffffff);
		
		mPaint = new Paint[4];
		for(int i = 0; i < 4; i ++) {
			mPaint[i] = new Paint();
			mPaint[i].setAntiAlias(true);
			mPaint[i].setDither(true);
			mPaint[i].setColor(0xFFFFFFFF);
		}

		r = context.getResources().getDimensionPixelSize(
				R.dimen.hb_point_radius_touch);
		gap = context.getResources().getDimensionPixelSize(
				R.dimen.hb_point_touch_gap);
		pr = context.getResources().getDimensionPixelSize(
				R.dimen.hb_point_radius);

	}

	public void setCenter(float x, float y) {
		mCenterX = x;
		mCenterY = y;
	}

	public void setScale(float scale) {
		mScale = scale;
	}

	public float getScale() {
		return mScale;
	}

//	public void draw(Canvas canvas) {
//		
//		canvas.save(Canvas.MATRIX_SAVE_FLAG);
//		canvas.translate(mCenterX, mCenterY);
//		canvas.scale(mScale, mScale, mCenterX, mCenterY);
//		
//
//		float delta = mPointManager.getPoint();
//		float delta2  = mPointManager.getPoint2();
//		float delta3 =  mPointManager.getPoint3();
//		if(delta3 > 0) {
//			canvas.drawCircle(-r, 0, pr, mPaint);						
//			canvas.drawCircle(r, 0, pr, mPaint);	
//		}	
//        if(delta > 0) {				
//			float rr2= r - (gap + pr)  + delta2;
//			float rr1= rr2 - delta2 -  (gap + pr) + delta;											
//			if(delta2 > 0) {
//				canvas.drawCircle(-rr2, 0, pr, mPaint);		
//				canvas.drawCircle(rr2, 0, pr, mPaint);		
//			}
//			canvas.drawCircle(-rr1, 0, pr, mPaint);	
//			canvas.drawCircle(rr1, 0, pr, mPaint);
//		} 		
//		canvas.restore();	
//		
//	}
	
	public void draw(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(mCenterX, mCenterY);
		canvas.scale(mScale, mScale, mCenterX, mCenterY);

		canvas.translate(-r, 0);

		int z = 1, y = 2, x = 3;
		float alphax = 0f;
		float alphay = 0f;
		float alphaz = 0f;
		if(mPointManager.getAlpha() <= 1) {
			alphaz = mPointManager.getAlpha();
		} else if(mPointManager.getAlpha() > 1 && mPointManager.getAlpha() <=2) {
			alphaz = 1f; 
			alphay = mPointManager.getAlpha() -1; 	
		} else {
			alphaz = 1f; 
			alphay = 1f;
			alphax = mPointManager.getAlpha() -2 ;
		}
		
		mPaint[z].setAlpha((int) Math.round(alphaz * 255f));
		mPaint[y].setAlpha((int) Math.round(alphay * 255f));
		mPaint[x].setAlpha((int) Math.round(alphax * 255f));

		canvas.drawCircle(0, 0, pr, mPaint[x]);
		canvas.translate(gap + 2 *pr, 0);
		canvas.drawCircle(0, 0, pr, mPaint[y]);
		canvas.translate(gap + 2 * pr, 0);
		canvas.drawCircle(0, 0, pr, mPaint[z]);

		canvas.translate(- 2 *gap + 2 * r - 4*pr, 0);
		canvas.drawCircle(0, 0, pr, mPaint[x]);
		canvas.translate(-gap - 2 * pr, 0);
		canvas.drawCircle(0, 0, pr, mPaint[y]);
		canvas.translate(-gap - 2 * pr, 0);
		canvas.drawCircle(0, 0, pr, mPaint[z]);

		canvas.restore();
	}

}