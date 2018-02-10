package com.android.incallui.widget.multiwaveview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.android.incallui.R;

public class SlideAnimView {

	private int mDistance;
	private Bitmap mArrowBg, mArrowShader;
	private Paint mPaintShader, mPaintBg;
	private int mHeight, mWidth;
	private int tempdelta, templeft;
	private Matrix mMatrix;
	private Bitmap mTempBmp;
    private Canvas mTempCanvas;

	private Path mPath;
	private int r = 212;
	private Canvas mBitmapCanvas;
	private int[] mAlpha = new int[]{0x7f,  0x77,  0x33};

	public SlideAnimView(Context context) {

		mArrowBg = BitmapFactory.decodeResource(context.getResources(),  R.drawable.ring_arrow_bg);
		mArrowShader = BitmapFactory.decodeResource(context.getResources(),  R.drawable.ring_arrow_fg);
		mPaintShader = new Paint();
		BitmapShader bs = new BitmapShader(mArrowShader, Shader.TileMode.CLAMP,
				Shader.TileMode.CLAMP);
		mPaintShader.setShader(bs);

		mPaintBg = new Paint();
		mPaintBg.setAlpha(0xbb);

		mPath = new Path();
		Resources res = context.getResources();
        mHeight = mArrowBg.getHeight();
        mWidth = mArrowBg.getWidth();
        
		r = context.getResources().getDimensionPixelSize(
				R.dimen.hb_point_radius_touch);		
		
		mMatrix = new Matrix();
		mMatrix.setScale(-1, 1);
		mMatrix.postTranslate(mWidth, 0);
		
	    mTempBmp = Bitmap.createBitmap(mWidth, mHeight, mArrowBg.getConfig());
	    mTempCanvas = new Canvas(mTempBmp);
	}
	
	public void setDistance(int value) {
		mDistance = value;
	}
	
	
	private float mCenterX;
	private float mCenterY;
	public void setCenter(float x, float y) {
		mCenterX = x;
		mCenterY = y;
	}

	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub		
		
		mTempBmp.eraseColor(Color.TRANSPARENT);
        mTempCanvas.drawBitmap(mArrowBg, 0, 0, mPaintBg);
		
		final int distance = 80 - mDistance;

		for (int i =0; i < 3; i++) {
			mPath.rewind();
			tempdelta = - i * 20;
			templeft = -20 + distance + tempdelta;			
			mPath.addRect(templeft, 0, templeft + 20, mHeight, Path.Direction.CW);
			mPath.close();
			mPaintShader.setAlpha(mAlpha[i]);
			mTempCanvas.drawPath(mPath, mPaintShader);
		}
				
		for (int i =0; i < 3; i++) {
			mPath.rewind();
			tempdelta = i * 20;
			templeft = distance + tempdelta;			
			mPath.addRect(templeft, 0, templeft + 20, mHeight, Path.Direction.CW);
			mPath.close();
			mPaintShader.setAlpha(mAlpha[i]);
			mTempCanvas.drawPath(mPath, mPaintShader);
		}
		
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(mCenterX, mCenterY);
		canvas.translate(-r, -0.5f * mHeight);
		canvas.drawBitmap(mTempBmp, 0, 0, null);
		canvas.restore();

		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.translate(mCenterX, mCenterY);
		canvas.translate(r - mWidth, -0.5f * mHeight);
		canvas.drawBitmap(mTempBmp, mMatrix, null);
		canvas.restore();					
	
	}


	public void release() {
		if (mArrowBg != null && (!mArrowBg.isRecycled())) {
			mArrowBg.recycle();
			mArrowBg = null;
		}
		if (mArrowShader != null && (!mArrowShader.isRecycled())) {
			mArrowShader.recycle();
			mArrowShader = null;
		}
	}
	

}
