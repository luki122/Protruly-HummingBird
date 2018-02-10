package com.android.systemui.statusbar.phone;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Blur;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.StatusBarState;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

/**
 * created by wxue 20170414
 */
public class LockedWallpaperController {
	private static final String TAG = "LockedWallpaperController";
	private static final String ACTION_LOCKSCREEN_WALLPAPER_CHANGED = "com.hb.thememanager.intent.ACTION_SET_LOCKSCREEN_WALLPAPER";
	private static final String ACTION_VR_WALLPAPER = "android.wallpaper.settings.keyguard";
	private static final String PREF_LOCK_WALLPAPER_INFO = "lock_wallpaper_info";
	private static final String SHOW_VR_WALLPAPER = "show_vr_wallpaper";
	private Context mContext;
	private WallpaperManager mWallpaperManager;
	private Handler mHandler = new Handler();
	private ImageView mWallpaperImageView;
	private ImageView mBouncerImageView;
	private Bitmap mSrcBitmap;
	private Bitmap mBouncerBitmap;
	private Bitmap mWallpaperBitmap;
	private boolean mStartTracking;
	private int mState;
	private boolean mLaunchTransitionOccluding;  //  the keyguard is occluding by another window(starting camera)
	private boolean mHasBouncer;
	private boolean mShowVRWallpaper;
	private SharedPreferences mSharedPreferences;
	private float mLastExpandedHeight;
	private final Interpolator mInterpolator = new DecelerateInterpolator();
	private Object mutex = new Object();
	private LockPatternUtils mLockPatternUtils;
	private boolean mBouncerShowing;

	public LockedWallpaperController(Context context, ImageView wallpaperImageView, ImageView bouncerImageView) {
		mContext = context;
		mWallpaperImageView = wallpaperImageView;
		mBouncerImageView = bouncerImageView;
		mWallpaperManager = (WallpaperManager) mContext.getSystemService(Context.WALLPAPER_SERVICE);
		mSharedPreferences = mContext.getSharedPreferences(PREF_LOCK_WALLPAPER_INFO, Context.MODE_PRIVATE);
		mLockPatternUtils = new LockPatternUtils(mContext);

		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_LOCKSCREEN_WALLPAPER_CHANGED);
		filter.addAction(ACTION_VR_WALLPAPER);
		filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
		mContext.registerReceiver(mWallpaperChangedReceiver, filter);
		
		// initialize lockscreen wallpaper
		generateWallpaperImage();
		mShowVRWallpaper = showVrWallpaper();
		if(mShowVRWallpaper && mWallpaperManager.getWallpaperInfo() != null){
			Log.i(TAG,"---LockedWallpaperController()--show vr lockwallpaper");
				updateWallpaperViewVisibility();
				updateBouncerViewVisibility();
		}
	}

	 public void setBouncerShowing(boolean bouncerShowing){
	    	if(mBouncerShowing == bouncerShowing){
	    		return;
	    	}
	    	mBouncerShowing = bouncerShowing;
	    	updateBouncerViewVisibility();
	 }
	
	private boolean showVrWallpaper(){
		return mSharedPreferences.getBoolean(SHOW_VR_WALLPAPER, false);
	}
	
	private void writeVrWallpaperState(boolean showVrWallpaper){
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(SHOW_VR_WALLPAPER, showVrWallpaper);
		editor.commit();
	}
	
	public void setBarState(int barState) {
		//Log.i(TAG,"---setBarState()---barState = " + barState + " mHasBouncer = " + mHasBouncer + " mLaunchTransitionOccluding = " + mLaunchTransitionOccluding);
		if (mState == barState) {
			return;
		}
		mState = barState;
		updateWallpaperViewVisibility();
		updateBouncerViewVisibility();
	}
	
	public void setHasBouncer(boolean hasBouncer){
		//Log.i(TAG,"---setHasBouncer()---hasBouncer = " + hasBouncer);
		if (mHasBouncer == hasBouncer) {
			return;
		}
		mHasBouncer = hasBouncer;
		updateBouncerViewVisibility();
	}
	
	public void setLaunchTransitionOccluding(boolean occluding){
		//Log.i(TAG,"---setLaunchTransitionOccluding()---occluding = " + occluding);
		if(mLaunchTransitionOccluding == occluding){
			return;
		}
		mLaunchTransitionOccluding = occluding;
		updateWallpaperViewVisibility();
		updateBouncerViewVisibility();
	}

	/*private void setBouncerImageShow(boolean show) {
		if (show) {
			mBouncerImageView.setVisibility(View.VISIBLE);
			mBouncerImageView.setAlpha(0f);
			mBouncerImageView.animate().alpha(1f).setStartDelay(0).setDuration(220)
					.setInterpolator(mInterpolator)
					.start();
		} else {
			mBouncerImageView.setVisibility(View.GONE);
		}
	}*/
	
	private void updateBouncerViewVisibility(){
		if(mShowVRWallpaper){
			mBouncerImageView.setVisibility(View.GONE);
		}else if(mState == StatusBarState.SHADE && !mBouncerShowing){
			mBouncerImageView.setVisibility(View.GONE);
		}else if(mState == StatusBarState.KEYGUARD && (!mHasBouncer || mLaunchTransitionOccluding)){
			mBouncerImageView.setVisibility(View.GONE);
		}else{
			mBouncerImageView.setVisibility(View.VISIBLE);
		}
	}

	private void updateWallpaperViewVisibility() {
		if (mState == StatusBarState.SHADE || mShowVRWallpaper) {
			mWallpaperImageView.setVisibility(View.GONE);
		} else if(mState == StatusBarState.KEYGUARD && mLaunchTransitionOccluding){
			mWallpaperImageView.setVisibility(View.GONE);
		} else{
			mWallpaperImageView.setVisibility(View.VISIBLE);
		}
	}

	private void updateBitmap() {
		if (mBouncerBitmap != null) {
			mBouncerImageView.setImageBitmap(mBouncerBitmap);
		}
		if (mWallpaperBitmap != null) {
			mWallpaperImageView.setImageBitmap(mWallpaperBitmap);
		}
	}
	
	public void updateWallpaperViewHeight(float expandedHeight){
		if(mState == StatusBarState.KEYGUARD){
			int  y = (int)expandedHeight - mWallpaperImageView.getHeight();
			mWallpaperImageView.setY(y);
			boolean changed = mLastExpandedHeight != expandedHeight;
			mLastExpandedHeight = expandedHeight;
			if(!mStartTracking && mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())){
				setWallpaperImageViewAlpha(expandedHeight);
			}
			if(y == 0 && changed){
	    	    mWallpaperImageView.animate().alpha(1f).setStartDelay(0).setDuration(220)
					.setInterpolator(mInterpolator)
					.start();
	    	    mStartTracking = false;
			}
		}
	}
	
	private void setWallpaperImageViewAlpha(float expandedHeight){
		float fractor = Math.abs((mWallpaperImageView.getHeight() - expandedHeight) / 240);
    	if(fractor > 1){
    		fractor = 1;
    	}
    	float alpha = 1 - fractor;
    	mWallpaperImageView.setAlpha(alpha);
    }
	
	public void onTrackingStarted() {
		if(mState == StatusBarState.KEYGUARD && mHasBouncer){
			mStartTracking = true;
			mWallpaperImageView.setAlpha(1f);
			mWallpaperImageView.animate().alpha(0f).setStartDelay(0).setDuration(220)
					.setInterpolator(mInterpolator)
					.start();
		}
    }
	
    public void onTrackingStopped(boolean expand) {
    	if(mState == StatusBarState.KEYGUARD && mStartTracking && mHasBouncer){
    		if(expand){
	    		mWallpaperImageView.setAlpha(0f);
	    	    mWallpaperImageView.animate().alpha(1f).setStartDelay(0).setDuration(220)
					.setInterpolator(mInterpolator)
					.start();
    		}
    	    mStartTracking = false;
    	}
    }
    
	private void generateWallpaperImage() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (mutex) {
					mSrcBitmap = mWallpaperManager.getLockscreenBitmap();
					if(mSrcBitmap == null){
						return;
					}
					mWallpaperBitmap = handleImageEffect(mSrcBitmap, 0.0f, 1.0f, 0.95f);
					mBouncerBitmap = Blur.onStackBlur(Utilities.magnifyBitmap(mSrcBitmap, 1.0f / 8, 1.0f / 8), 15);
					Log.i(TAG,"---generateWallpaperImage--threadId = " +  Thread.currentThread().getId());
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						updateBitmap();
						synchronized (mutex) {
							Log.i(TAG,"---recycle bitmap ---" + " thread = " + Thread.currentThread().getId());
							if(mSrcBitmap != null && !mSrcBitmap.isRecycled()){
								mSrcBitmap.recycle();
							}
						}
					}
				});
			}
		}).start();
	}

	/**
    *
    * @param bm 图像 （不可修改）
    * @param hue 色相 (0~360度)
    * @param saturation 饱和度(它使用从0%（灰色）至100%（完全饱和）的百分比来度量)
    * @param lum 亮度(使用从0%（黑色）至100%（白色）的百分比来度量)
    * @return
    */
   public static Bitmap handleImageEffect(Bitmap bm, float hue, float saturation, float lum) {

       Bitmap bmp = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);

       Canvas canvas = new Canvas(bmp);

       Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

       ColorMatrix hueMatrix = new ColorMatrix();
       hueMatrix.setRotate(0, hue); // R
       hueMatrix.setRotate(1, hue); // G
       hueMatrix.setRotate(2, hue); // B

       ColorMatrix saturationMatrix = new ColorMatrix();
       saturationMatrix.setSaturation(saturation);

       ColorMatrix lumMatrix = new ColorMatrix();
       lumMatrix.setScale(lum, lum, lum, 1);

       //融合
       ColorMatrix imageMatrix = new ColorMatrix();
       imageMatrix.postConcat(hueMatrix);
       imageMatrix.postConcat(saturationMatrix);
       imageMatrix.postConcat(lumMatrix);

       paint.setColorFilter(new ColorMatrixColorFilter(imageMatrix));
       canvas.drawBitmap(bm, 0, 0, paint);

       return bmp;
   }

	private final BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG,"---action = " + action);
			if (action.equals(ACTION_LOCKSCREEN_WALLPAPER_CHANGED)) {
				mShowVRWallpaper = false;
				mWallpaperManager.forgetLoadedWallpaper();
				generateWallpaperImage();
				writeVrWallpaperState(false);
			} else if(action.equals(ACTION_VR_WALLPAPER)){
				mShowVRWallpaper = true;
				updateWallpaperViewVisibility();
				updateBouncerViewVisibility();
				writeVrWallpaperState(true);
			} else if(action.equals(Intent.ACTION_WALLPAPER_CHANGED)){
				mShowVRWallpaper = false;
				if(mWallpaperBitmap == null || mBouncerBitmap == null){
					Log.i(TAG,"---need to generateWallpaperImage again !");
					mWallpaperManager.forgetLoadedWallpaper();
					generateWallpaperImage();
				}
				writeVrWallpaperState(false);
			}
		};
	};
}
