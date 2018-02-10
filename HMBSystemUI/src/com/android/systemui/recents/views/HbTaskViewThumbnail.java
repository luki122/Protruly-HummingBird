/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;


/**
 * The task thumbnail view.  It implements an image view that allows for animating the dim and
 * alpha of the thumbnail image.
 */
public class HbTaskViewThumbnail extends View {

    RecentsConfiguration mConfig;

    // Drawing
    float mDimAlpha;
    Matrix mScaleMatrix = new Matrix();
    Paint mDrawPaint = new Paint();
    RectF mBitmapRect = new RectF();
    RectF mLayoutRect = new RectF();
    BitmapShader mBitmapShader;
    LightingColorFilter mLightingColorFilter = new LightingColorFilter(0xffffffff, 0);

    // Thumbnail alpha
    float mThumbnailAlpha;

    // Task bar clipping, the top of this thumbnail can be clipped against the opaque header
    // bar that overlaps this thumbnail
    View mTaskBar;
    Rect mClipRect = new Rect();

    // Visibility optimization, if the thumbnail height is less than the height of the header
    // bar for the task view, then just mark this thumbnail view as invisible
    boolean mInvisible;
    
    Bitmap mBitmap;
    
    private Matrix mMatrix = new Matrix();
	private float targetX, targetY;
	private int mDegrees;
	final float[] values = new float[9];

    public HbTaskViewThumbnail(Context context) {
        this(context, null);
    }

    public HbTaskViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HbTaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public HbTaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = RecentsConfiguration.getInstance();
        mDrawPaint.setColorFilter(mLightingColorFilter);
        mDrawPaint.setFilterBitmap(true);
        mDrawPaint.setAntiAlias(true);
    }

    @Override
    protected void onFinishInflate() {
        mThumbnailAlpha = mConfig.taskViewThumbnailAlpha;
        updateThumbnailPaintFilter();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mLayoutRect.set(0, 0, getWidth(), getHeight());
            updateThumbnailScale();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mInvisible) {
            return;
        }
        // Draw the thumbnail with the rounded corners
//        canvas.drawRoundRect(0, 0, getWidth(), getHeight(),
//                mConfig.taskViewRoundedCornerRadiusPx,
//                mConfig.taskViewRoundedCornerRadiusPx, mDrawPaint);
//    	Log.d("tangjun222", "---onDraw mBitmap = " + mBitmap);
        mMatrix.reset();
        mDegrees = 0;
        if(mBitmap != null) {
        	if(mBitmap.getHeight() > mBitmap.getWidth() && mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        		mDegrees = -90;
        		mMatrix.setRotate(-90, (float)mBitmap.getWidth()/2, (float)mBitmap.getHeight()/2);
        	} else if (mBitmap.getHeight() <= mBitmap.getWidth() && mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        		mDegrees = 90;
        		mMatrix.setRotate(90, (float)mBitmap.getWidth()/2, (float)mBitmap.getHeight()/2);
        	}
        	if(mDegrees != 0) {
        		if (mDegrees == 90) {
        			targetX = mBitmap.getHeight();
        			targetY = 0;
        		} else {
        			targetX = 0;
        			targetY = mBitmap.getWidth();
        		}
        		mMatrix.getValues(values);
        		float x1 = values[Matrix.MTRANS_X];
        		float y1 = values[Matrix.MTRANS_Y];

        		mMatrix.postTranslate(targetX - x1, targetY - y1);
        		canvas.drawBitmap(mBitmap, mMatrix,  null);
        	} else {
        		canvas.drawBitmap(mBitmap, 0, 0,  null);
        	}
        } else {
        	canvas.drawColor(Color.parseColor("#ffffff"));
        }
    }

    /** Sets the thumbnail to a given bitmap. */
    void setThumbnail(Bitmap bm) {
    	mBitmap = bm;
//        if (bm != null) {
//            mBitmapShader = new BitmapShader(bm, Shader.TileMode.CLAMP,
//                    Shader.TileMode.CLAMP);
//            mDrawPaint.setShader(mBitmapShader);
//            mBitmapRect.set(0, 0, bm.getWidth(), bm.getHeight());
//            updateThumbnailScale();
//        } else {
//            mBitmapShader = null;
//            mDrawPaint.setShader(null);
//        }
        updateThumbnailPaintFilter();
    }

    /** Updates the paint to draw the thumbnail. */
    void updateThumbnailPaintFilter() {
//        if (mInvisible) {
//            return;
//        }
//        int mul = (int) ((1.0f - mDimAlpha) * mThumbnailAlpha * 255);
//        int add = (int) ((1.0f - mDimAlpha) * (1 - mThumbnailAlpha) * 255);
//        if (mBitmapShader != null) {
//            mLightingColorFilter.setColorMultiply(Color.argb(255, mul, mul, mul));
//            mLightingColorFilter.setColorAdd(Color.argb(0, add, add, add));
//            mDrawPaint.setColorFilter(mLightingColorFilter);
//            mDrawPaint.setColor(0xffffffff);
//        } else {
//            int grey = mul + add;
//            mDrawPaint.setColorFilter(null);
//            mDrawPaint.setColor(Color.argb(255, grey, grey, grey));
//        }
//        Log.d("111111", "---updateThumbnailPaintFilter");
        invalidate();
    }

    /** Updates the thumbnail shader's scale transform. */
    void updateThumbnailScale() {
//        if (mBitmapShader != null) {
//            mScaleMatrix.setRectToRect(mBitmapRect, mLayoutRect, Matrix.ScaleToFit.FILL);
//            mBitmapShader.setLocalMatrix(mScaleMatrix);
//        }
    }
    
    /**hb: add by tangjun for rotate bitmap begin*/
    public Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            Bitmap b2 = Bitmap.createBitmap(
            		b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b != b2) {
            	//hb: tangjun TODO need to do begin
            	//b.recycle();
            	//hb: tangjun TODO need to do end
            	b = b2;
            }
        }
        return b;
    }
    /**hb: add by tangjun for rotate bitmap end*/

    /** Binds the thumbnail view to the task */
    void rebindToTask(Task t) {
         /**hb: add by tangjun for rotate bitmap begin*/
//    	 Bitmap thumbnail = t.thumbnail;
//     	if(thumbnail != null && !thumbnail.isRecycled()) {
//     		Log.d("222222", "---loadTaskData activityLabel = " + t.activityLabel);
//     		long a = System.currentTimeMillis();
//     		if(thumbnail.getHeight() > thumbnail.getWidth() && mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//     			thumbnail = rotate(thumbnail, -90);
//     		} else if (thumbnail.getHeight() <= thumbnail.getWidth() && mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//     			thumbnail = rotate(thumbnail, 90);
//     		}
//     		long b = System.currentTimeMillis() - a;
//     		Log.d("222222", "--cost  = " + b);
//     		t.thumbnail = thumbnail;
//     	}
     	/**hb: add by tangjun for rotate bitmap end*/
        if (t.thumbnail != null) {
            setThumbnail(t.thumbnail);
            //hb: mod by tangjun for redraw when thumbnail is not null begin
            //postInvalidate();
            invalidate();
            //hb: mod by tangjun for redraw when thumbnail is not null end
        } else {
            setThumbnail(null);
        }
    }

    /** Unbinds the thumbnail view from the task */
    void unbindFromTask() {
        setThumbnail(null);
    }
}
