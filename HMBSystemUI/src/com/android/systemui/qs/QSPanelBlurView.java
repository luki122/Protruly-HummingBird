package com.android.systemui.qs;

import java.lang.ref.SoftReference;

import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.screenshot.GlobalScreenshot;
import com.android.systemui.Blur;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Scroller;
/**
 * 
 * @author storktang
 *
 */
public class QSPanelBlurView extends View{
	private Context mContext;
    private static SoftReference<Bitmap> mSoftRef = null;
    private Bitmap mScreenBitmap;
    private Paint mPaint = new Paint();
    private Rect mSrc = new Rect();
    private Rect mDest = new Rect();
    private RectF mClip = new RectF();
    private Path mPath = new Path();
    private QSPanelViewForPullUp mParentView;
    private static final int HORIZONTAL_DIS_PORT = 25;
    private static final int TOP_DIS_PORT = 22;
    private static final int TOP_DIS_LAND = 20;
    private static final int BOTTOM_DIS_PORT = 28;
    private static final int BOTTOM_DIS_LAND = 20;
    private static final int HORIZONTAL_RIGHT_DIS_LAND = 28;
    private static final float BLURBACK_RADIUS = 25f;
    private static final float[] BLURBACK_RADIUS_ARRAY_PORT = 
    		new float[]{BLURBACK_RADIUS, BLURBACK_RADIUS, BLURBACK_RADIUS, BLURBACK_RADIUS, 0, 0, 0, 0};
    private static final float[] BLURBACK_RADIUS_ARRAY_LAND = 
    		new float[]{BLURBACK_RADIUS, BLURBACK_RADIUS, 0, 0, 0, 0, BLURBACK_RADIUS, BLURBACK_RADIUS};
    private WindowManager mWindowManager;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private int mNavBarHeight;
    private int mBottomPort;
    private int mRightLand;
    
    private PaintFlagsDrawFilter mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    
    private static final float BLUR_SCALE = 0.08f;
    
    public QSPanelBlurView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QSPanelBlurView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        //由于做成控制中心上拉盖住虚拟键，所以就不用把虚拟键高度在ondraw时减掉了
        mNavBarHeight = mContext.getResources().getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);
    	//mDisplay = mWindowManager.getDefaultDisplay();
        mDisplay.getRealMetrics(mDisplayMetrics);
        mNavBarHeight = mContext.getResources().getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
    }
    
    @Override
    protected void onAttachedToWindow() {
    	// TODO Auto-generated method stub
    	super.onAttachedToWindow();
    	mParentView = (QSPanelViewForPullUp)getParent();
    }
    
	public void takeFastblurScreenShot() {
		GlobalScreenshot gs = new GlobalScreenshot(mContext);
		long a = System.currentTimeMillis();
		mScreenBitmap = gs.takeScreenshot(false, false);
		if (mSoftRef != null) {
			mSoftRef.clear();
			mSoftRef = null;
		}
		long b = System.currentTimeMillis();
		if (mScreenBitmap != null) {
			mScreenBitmap = Blur.onStackBlur(Utilities.magnifyBitmap(mScreenBitmap, BLUR_SCALE, BLUR_SCALE), 11);
			mSoftRef = new SoftReference<Bitmap>(mScreenBitmap);
			//mScreenBitmap.recycle();
			//mScreenBitmap = null;
		}
		long c = System.currentTimeMillis();
		long d = System.currentTimeMillis();
		Log.d("111111", "---11cost = " + String.valueOf(b - a));
		Log.d("111111", "---22cost = " + String.valueOf(c - b));
		Log.d("111111", "---33cost = " + String.valueOf(d - c));
		
		//setBackgroundColor(Color.parseColor("#50ff0000"));
	}

    @Override
    protected void onDraw(Canvas canvas) {
    	// TODO Auto-generated method stub
    	super.onDraw(canvas);
    	canvas.save();
    	if(mScreenBitmap != null) {
	    	//HORIZONTAL_DIS, TOP_DIS， BLURBACK_RADIUS等是根据设计给的圆角矩形背景图确定的
	    	if(Utilities.isOrientationPortrait(mContext)) {
	    		if(mParentView.getScrollY() >= mParentView.getInitQuickSettingInitTranslation() - BOTTOM_DIS_PORT) {
	    			mBottomPort = mParentView.getInitQuickSettingInitTranslation();
	    			mClip.set(HORIZONTAL_DIS_PORT, TOP_DIS_PORT, mDisplayMetrics.widthPixels - HORIZONTAL_DIS_PORT, mBottomPort - BOTTOM_DIS_PORT);
	        		mSrc .set(0, (int)((mDisplayMetrics.heightPixels - mParentView.getScrollY()) * BLUR_SCALE), 
	        				mScreenBitmap.getWidth(), (int)((mDisplayMetrics.heightPixels -  mParentView.getScrollY() + mBottomPort) * BLUR_SCALE));
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS, BLURBACK_RADIUS, Path.Direction.CW);
	        		if(mParentView.getScrollY() >= mParentView.getInitQuickSettingInitTranslation()) {
	        			mDest.set(0, 0, mDisplayMetrics.widthPixels, mBottomPort);
	        		} else {
	        			mDest.set(0, 0, mDisplayMetrics.widthPixels, mParentView.getScrollY());
	        		}
	    		} else {
	    			mBottomPort = mParentView.getScrollY();
	    			//mClip.set(HORIZONTAL_DIS_PORT, TOP_DIS_PORT, mDisplayMetrics.widthPixels - HORIZONTAL_DIS_PORT, mBottomPort - BOTTOM_DIS_PORT);
	    			mClip.set(HORIZONTAL_DIS_PORT, TOP_DIS_PORT, mDisplayMetrics.widthPixels - HORIZONTAL_DIS_PORT, mBottomPort);
	        		mSrc .set(0, (int)((mDisplayMetrics.heightPixels -  mParentView.getScrollY()) * BLUR_SCALE), 
	        				mScreenBitmap.getWidth(), (int)(mDisplayMetrics.heightPixels  * BLUR_SCALE));
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS_ARRAY_PORT, Path.Direction.CW);
	        		mDest.set(0, 0, mDisplayMetrics.widthPixels, mBottomPort);
	    		}
	    	} else {
	    		if(mParentView.getScrollX() >= mParentView.getInitQuickSettingInitTranslation() - HORIZONTAL_RIGHT_DIS_LAND) {
	    			mRightLand = mParentView.getInitQuickSettingInitTranslation();
	    			mClip.set(0, TOP_DIS_LAND, mRightLand - HORIZONTAL_RIGHT_DIS_LAND, mDisplayMetrics.heightPixels - BOTTOM_DIS_LAND);
	        		mSrc .set( (int)((mDisplayMetrics.widthPixels - mParentView.getScrollX()) * BLUR_SCALE), 0, 
	        				(int)((mDisplayMetrics.widthPixels - mParentView.getScrollX() + mRightLand) * BLUR_SCALE), mScreenBitmap.getHeight());
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS, BLURBACK_RADIUS, Path.Direction.CW);
	        		if(mParentView.getScrollX() >= mParentView.getInitQuickSettingInitTranslation()) {
	        			mDest.set(0, 0, mRightLand, mDisplayMetrics.heightPixels);
	        		} else {
	        			mDest.set(0, 0, mParentView.getScrollX(), mDisplayMetrics.heightPixels);
	        		}
	    		} else {
	    			mRightLand = mParentView.getScrollX();
	    			//mClip.set(0, TOP_DIS_LAND, mRightLand - HORIZONTAL_RIGHT_DIS_LAND, mDisplayMetrics.heightPixels - BOTTOM_DIS_LAND);
	    			mClip.set(0, TOP_DIS_LAND, mRightLand, mDisplayMetrics.heightPixels - BOTTOM_DIS_LAND);
	        		mSrc .set( (int)((mDisplayMetrics.widthPixels -  mParentView.getScrollX()) * BLUR_SCALE), 0, 
	        				(int)(mDisplayMetrics.widthPixels * BLUR_SCALE), mScreenBitmap.getHeight());
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS_ARRAY_LAND, Path.Direction.CW);
	        		mDest.set(0, 0, mRightLand, mDisplayMetrics.heightPixels);
	    		}
	    	}
	    	canvas.clipPath(mPath);
	    	canvas.drawBitmap(mScreenBitmap, mSrc, mDest, null);
	    	canvas.setDrawFilter(mPaintFlagsDrawFilter); 
	    	//用paint绘制会有小格子
	    	//canvas.drawBitmap(mScreenBitmap, mSrc, mDest, mPaint);
	    	canvas.restore();
    	} else {
	    	if(Utilities.isOrientationPortrait(mContext)) {
	    		if(mParentView.getScrollY() >= mParentView.getInitQuickSettingInitTranslation() - BOTTOM_DIS_PORT) {
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS, BLURBACK_RADIUS, Path.Direction.CW);
	    		} else {
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS_ARRAY_PORT, Path.Direction.CW);
	    		}
	    	} else {
	    		if(mParentView.getScrollX() >= mParentView.getInitQuickSettingInitTranslation() - HORIZONTAL_RIGHT_DIS_LAND) {
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS, BLURBACK_RADIUS, Path.Direction.CW);
	    		} else {
	        		mPath.reset();
	        		//裁剪圆角矩形
	        		mPath.addRoundRect(mClip, BLURBACK_RADIUS_ARRAY_LAND, Path.Direction.CW);
	    		}
	    	}
    	}
    	canvas.save();
    	canvas.clipPath(mPath);
    	canvas.drawColor(Color.parseColor("#60000000"));
    	canvas.restore();
    }
}
