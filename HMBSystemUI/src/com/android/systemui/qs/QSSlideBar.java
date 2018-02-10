package com.android.systemui.qs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.systemui.R;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.screenshot.GlobalScreenshot;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;

import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.ContentResolver;

import android.content.res.Configuration;
import android.graphics.BitmapFactory;
/**
 * 
 * @author storktang
 *
 */
/**hb tangjun add begin*/
public class QSSlideBar extends LinearLayout {

	Context						mContext;
	WindowManager				mWM;		
	WindowManager.LayoutParams	mHandlerBarMParams;	
	WindowManager.LayoutParams  mQuickSettingPanelParams;
	View						mHandlerBarView;
	public QSPanelViewForPullUp mQSPanelViewForPullUp;
	public View mQSPanelView;
	public View mQSPullDownView;
	private QSPanelBlurView mQSBlurView;
    private float mDownMotionY;
    
    private VelocityTracker mVelocityTracker;

	private int totalMoveDistant = 0;

    public static final int ANIMATION_CONTINUE_ENTER_DURATION = 450;
	
    public static final int AUTO_ENTER_ANIMATION_DURATION = 500;
    
    private int mMinVelocityPx = -100;
    
    private PhoneStatusBar mPhoneStatusBar;
    
    public static final String DISABLE_PULLUP_QSPANEL = "disable_pullup_qspanel";
    public static final String ENABLE_PULLUP_QSPANEL = "enable_pullup_qspanel";
    private boolean mDisablePullUpQSpanel;
    
    private int mQuickSettingDistance = 0;
    private OnTouchListener mPullDownOnTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
            if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
                return false;
            }
            
            acquireVelocityTracker(event); 
			final int action = event.getAction();
			
			float distantY =  Utilities.isOrientationPortrait(mContext) ? event.getRawY() : event.getRawX();
			if(Math.abs(distantY - mDownMotionY) < 3) {
				return false;
			}
			Log.d("333333", "---mQSPullDownView onTouch +  getAction() = " + event.getAction());
			Log.d("333333", "---mQSPullDownView onTouch +  getY() = " + event.getY() + ", event.getRawY() = " + event.getRawY() + ", mDownMotionY = " + mDownMotionY);
			if (action == MotionEvent.ACTION_DOWN) {
				mDownMotionY = distantY;
				//mQSPanelViewForPullUp.closeScroll();
				releaseVelocityTracker();
			}else if (action == MotionEvent.ACTION_MOVE) {
				if(distantY - mDownMotionY > mQuickSettingDistance) {
					if(Utilities.isOrientationPortrait(mContext)) {
						mQSPanelViewForPullUp.scrollTo(0, mQSPanelViewForPullUp.getInitQuickSettingInitTranslation() - (int)(distantY -  mDownMotionY));
					} else {
						mQSPanelViewForPullUp.scrollTo(mQSPanelViewForPullUp.getInitQuickSettingInitTranslation() - (int)(distantY -  mDownMotionY), 0);
					}
					Log.d("333333", "---mQSPullDownView mQSPanelViewForPullUp.getScrollY() = " + mQSPanelViewForPullUp.getScrollY());
					mQSPanelViewForPullUp.setAlphaByScroll(Utilities.isOrientationPortrait(mContext) ? mQSPanelViewForPullUp.getScrollY() : mQSPanelViewForPullUp.getScrollX());
					mQSBlurView.postInvalidateOnAnimation();
				}
			}else if (action ==  MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
				mVelocityTracker.computeCurrentVelocity(50); 
	    		//final float velocityX = mVelocityTracker.getXVelocity(); 
	    		final float velocityY = Utilities.isOrientationPortrait(mContext) ?mVelocityTracker.getYVelocity():mVelocityTracker.getXVelocity();
	    		if(velocityY >= 100.0f || distantY -  mDownMotionY> 200.0f) {
	    			mQSPanelViewForPullUp.smoothScrollTo(0, 0);
	    		} else {
	    			if(Utilities.isOrientationPortrait(mContext)) {
	    				mQSPanelViewForPullUp.smoothScrollTo(0, mQSPanelViewForPullUp.getInitQuickSettingInitTranslation());
	    			} else {
	    				mQSPanelViewForPullUp.smoothScrollTo(mQSPanelViewForPullUp.getInitQuickSettingInitTranslation(), 0);
	    			}
	    		}
			}
			return true;
		}
	};
	public QSSlideBar(Context context) {
		super(context);
		mContext = context;
		mHandlerBarMParams = new WindowManager.LayoutParams();
        mQuickSettingPanelParams = new WindowManager.LayoutParams();
	}
	
	public void init() {
		mWM = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mHandlerBarView = LayoutInflater.from(mContext).inflate(R.layout.qs_slidebar, null);
		mQSPanelViewForPullUp = (QSPanelViewForPullUp)LayoutInflater.from(mContext).inflate(R.layout.quicksetting_panel, null);
		mQuickSettingDistance = (int)(mContext.getResources().getDimension(R.dimen.quicksetting_panel_distance));
		mQSPanelView = mQSPanelViewForPullUp.getQuickSettingView();
		mQSBlurView = mQSPanelViewForPullUp.getBlurView();
		mQSPullDownView = mQSPanelViewForPullUp.getPullDownView();
		mQSPullDownView.setOnTouchListener(mPullDownOnTouchListener);
		mQSPanelView.setOnTouchListener(mPullDownOnTouchListener);
		mHandlerBarView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if(mQSPanelViewForPullUp.getQSPanelAlreadyShow() || mDisablePullUpQSpanel) {
					return false;
				}
				
                if (MotionEvent.ACTION_OUTSIDE == event.getAction()) {
                    return false;
                }
                acquireVelocityTracker(event); 
				final int action = event.getAction();
				
				float distantY =  Utilities.isOrientationPortrait(mContext) ? event.getY() : event.getX();
				if (action == MotionEvent.ACTION_DOWN) {
					mDownMotionY = distantY;
					mQSPanelViewForPullUp.closeScroll();
					//releaseVelocityTracker();
					mQSBlurView.takeFastblurScreenShot();
					setQSPanelViewForPullUpSystemUiVisibility();
				}else if (action == MotionEvent.ACTION_MOVE) {
					if(distantY < mDownMotionY && ((distantY - mDownMotionY)  > ((mQuickSettingDistance -mQSPanelViewForPullUp.getInitQuickSettingInitTranslation()) * 5 / 4))) {
						//if(!mQSPanelViewForPullUp.isShown()) {
						if(mQSPanelViewForPullUp.getVisibility() != View.VISIBLE) {
							mQSPanelViewForPullUp.setVisibility(View.VISIBLE);
							//mView.setBackgroundColor(Color.parseColor("#000000"));
						}
						if(Utilities.isOrientationPortrait(mContext)) {
							mQSPanelViewForPullUp.scrollTo(0, (int)((mDownMotionY - distantY ) * 4 / 5));
						} else {
							mQSPanelViewForPullUp.scrollTo((int)((mDownMotionY - distantY ) * 4 / 5), 0);
						}
						mQSBlurView.postInvalidateOnAnimation();
						mQSPanelViewForPullUp.setAlphaByScroll(Utilities.isOrientationPortrait(mContext) ? mQSPanelViewForPullUp.getScrollY() : mQSPanelViewForPullUp.getScrollX());
					}
				}else if (action ==  MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
					mVelocityTracker.computeCurrentVelocity(50); 
		    		//final float velocityX = mVelocityTracker.getXVelocity(); 
		    		final float velocityY = Utilities.isOrientationPortrait(mContext) ?mVelocityTracker.getYVelocity():mVelocityTracker.getXVelocity();
		    		Log.d("111111", "---velocityY = " + velocityY);
		    		Log.d("111111", "---mDownMotionY - distantY = " + String.valueOf(mDownMotionY - distantY));
		    		if(velocityY <= -100.0f || mDownMotionY - distantY > 200.0f) {
		    			if(Utilities.isOrientationPortrait(mContext)) {
		    				Log.d("111111", "---smoothScrollTo init getScrollY = " + getScrollY());
		    				mQSPanelViewForPullUp.smoothScrollTo(0, mQSPanelViewForPullUp.getInitQuickSettingInitTranslation());
		    			} else {
		    				mQSPanelViewForPullUp.smoothScrollTo(mQSPanelViewForPullUp.getInitQuickSettingInitTranslation(), 0);
		    			}
		    		} else {
		    			mQSPanelViewForPullUp.smoothScrollTo(0, 0);
		    		}
				}
				return true;
			}
		});
    	
    	WindowManager wm = mWM;
		mHandlerBarMParams.type = 2003; 
		mHandlerBarMParams.flags = 40;
		mHandlerBarMParams.gravity = Gravity.BOTTOM; 
		mHandlerBarMParams.width = LayoutParams.MATCH_PARENT;
		mHandlerBarMParams.height = 0;
		mHandlerBarMParams.format = -3; 
		wm.addView(mHandlerBarView, mHandlerBarMParams);
		
        mQSPanelViewForPullUp.setVisibility(View.GONE);
        mQuickSettingPanelParams.type = WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL;
        //mQuickSettingPanelParams.type = WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;
        //mQuickSettingPanelParams.type = WindowManager.LayoutParams.TYPE_TOP_MOST;
        mQuickSettingPanelParams.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mQuickSettingPanelParams.width = LayoutParams.MATCH_PARENT;
        mQuickSettingPanelParams.height = LayoutParams.MATCH_PARENT;
        mQuickSettingPanelParams.format = -3;
        //setSystemUiVisibility不能少
        mQSPanelViewForPullUp.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        wm.addView(mQSPanelViewForPullUp, mQuickSettingPanelParams);
        
		IntentFilter filter = new IntentFilter();
		filter.addAction(DISABLE_PULLUP_QSPANEL);
		filter.addAction(ENABLE_PULLUP_QSPANEL);
		mContext.registerReceiver(new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				if (intent.getAction().equals(DISABLE_PULLUP_QSPANEL)) {
					mDisablePullUpQSpanel = true;
				} else {
					mDisablePullUpQSpanel = false;
				}
			}
		}, filter);
	}
	
    public void setBar(PhoneStatusBar bar) {
    	mPhoneStatusBar = bar;
    }
    
	private void setQSPanelViewForPullUpSystemUiVisibility() {
		int flag = mQSPanelViewForPullUp.getSystemUiVisibility();
		if(mPhoneStatusBar.getLightNavigationBar()) {
			flag = flag | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
		} else {
			flag = flag & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
		}
		if(mPhoneStatusBar.getLightNavigationBarAddLine()) {
			flag = flag | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR_ADD_LINE;
		} else {
			flag = flag & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR_ADD_LINE;
		}
		mQSPanelViewForPullUp.setSystemUiVisibility(flag);
	}
	
    private void acquireVelocityTracker(final MotionEvent event) { 
        if(null == mVelocityTracker) { 
        	mVelocityTracker= VelocityTracker.obtain();
        } 
        mVelocityTracker.addMovement(event); 
    }
    
    private void releaseVelocityTracker() { 
        if(null != mVelocityTracker) {
        	mVelocityTracker.clear(); 
        	mVelocityTracker.recycle(); 
        	mVelocityTracker = null; 
        } 
    }

    public View getHandlerBarView() {
        if (mHandlerBarView == null)
            return null;
        else
            return mHandlerBarView;
    }
    
    public QSPanelViewForPullUp getQSPanelPanelView( ) {
    	return mQSPanelViewForPullUp;
    }
    
    private NavigationBarView mView;
    public void setNavigationBarView(NavigationBarView view) {
    	mView = view;
    	mQSPanelViewForPullUp.setNavigationBarView(mView);
    }
}
