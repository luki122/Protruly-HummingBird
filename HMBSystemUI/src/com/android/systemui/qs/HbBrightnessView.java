package com.android.systemui.qs;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.view.View.OnClickListener; 
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserTracker;

/**
 * 
 * @author storktang
 *
 */

public class HbBrightnessView extends LinearLayout {
	
	public static String TAG = "HbBrightnessView";

	private final int mMinimumBacklight;
	private final int mMaximumBacklight;
	
	private SeekBar mSeekBar;
	private ImageButton mAutoBrightnessImageButton;
	
	private Context mContext;
	private final IPowerManager mPower;
	private final Handler mHandler;
	private final BrightnessObserver mBrightnessObserver;
	private final CurrentUserTracker mUserReceiver;
	
	private boolean mExternalChange;
	private boolean mAutomatic;
	private final boolean mAutomaticAvailable;
	private boolean mTracking;
	private static final float BRIGHTNESS_ADJ_RESOLUTION = 100;
	private TextView mBrightnessText;
	
	public HbBrightnessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mContext = context;
		mHandler = new Handler();
		mUserReceiver = new CurrentUserTracker(mContext) {
	            @Override
	            public void onUserSwitched(int newUserId) {	            	
	                updateMode();
	                updateSlider();
	            }
	    };
		
		mBrightnessObserver = new BrightnessObserver(mHandler);
		
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
		mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();

		mAutomaticAvailable = context.getResources().getBoolean(com.android.internal.R.bool.config_automatic_brightness_available); 
		mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        
    }
	
	@Override
    public void onFinishInflate() {
        super.onFinishInflate();
    }
	
	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();

		mSeekBar = (SeekBar) findViewById(R.id.seekbar);
		mAutoBrightnessImageButton = (ImageButton)findViewById(R.id.hb_auto_brightness);
		mBrightnessText = (TextView)findViewById(R.id.hb_brightness);
				
		mBrightnessObserver.startObserving();
		mUserReceiver.startTracking();			           
        updateMode();
		updateSlider();
				
		mSeekBar.setOnSeekBarChangeListener(mSeekListener);
		mAutoBrightnessImageButton.setOnClickListener(mClickListener); 	 

	}
	
    protected void onDetachedFromWindow() {   	
    	super.onDetachedFromWindow();
    	mBrightnessObserver.stopObserving();
    	mUserReceiver.stopTracking();
    }
	
    public void onBrightnessChanged(boolean tracking, int value) {
		if (mExternalChange)
			return;

		if (!mAutomatic) {
			final int val = value + mMinimumBacklight;
			setBrightness(val);
			if (!tracking) {
				AsyncTask.execute(new Runnable() {
					public void run() {
						Settings.System.putIntForUser(
								mContext.getContentResolver(),
								Settings.System.SCREEN_BRIGHTNESS, val,
								UserHandle.USER_CURRENT);
					}
				});
			}
		} else {
			final float adj = value / (BRIGHTNESS_ADJ_RESOLUTION / 2f) - 1;
			setBrightnessAdj(adj);
			if (!tracking) {
				AsyncTask.execute(new Runnable() {
					public void run() {
						Settings.System.putFloatForUser(
								mContext.getContentResolver(),
								Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ,
								adj, UserHandle.USER_CURRENT);
					}
				});
			}
		}
		int curBrightness = (int)((float)value / mSeekBar.getMax() * 100);
		mBrightnessText.setText(String.valueOf(curBrightness));
	}

	private void setBrightness(int brightness) {
		try {
			mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
		} catch (RemoteException ex) {
		}
	}

	private void setBrightnessAdj(float adj) {
		try {
			mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
		} catch (RemoteException ex) {
		}
	}

	private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			onBrightnessChanged(mTracking, progress);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mTracking = true;
			onBrightnessChanged(mTracking, seekBar.getProgress());
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mTracking = false;
			onBrightnessChanged(mTracking, seekBar.getProgress());
		}
	};
    
	View.OnClickListener mClickListener = new View.OnClickListener() {          
        @Override  
        public void onClick(View v) {  
        	if (mAutomatic) {
			    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
		    } else {
		        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
		    }       	
        }  
    };  
	
    /** ContentObserver to watch brightness **/
	private class BrightnessObserver extends ContentObserver {

		private final Uri BRIGHTNESS_MODE_URI = Settings.System
				.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
		private final Uri BRIGHTNESS_URI = Settings.System
				.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
		private final Uri BRIGHTNESS_ADJ_URI = Settings.System
				.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);

		public BrightnessObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			if (selfChange)
				return;
			try {
				mExternalChange = true;
				if (BRIGHTNESS_MODE_URI.equals(uri)) {
					updateMode();
					updateSlider();
				} else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
					updateSlider();
				} else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
					updateSlider();
				} else {
					updateMode();
					updateSlider();
				}
			} finally {
				mExternalChange = false;
			}
		}

		public void startObserving() {
			final ContentResolver cr = mContext.getContentResolver();
			cr.unregisterContentObserver(this);
			cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, this,
					UserHandle.USER_ALL);
			cr.registerContentObserver(BRIGHTNESS_URI, false, this,
					UserHandle.USER_ALL);
			cr.registerContentObserver(BRIGHTNESS_ADJ_URI, false, this,
					UserHandle.USER_ALL);
		}

		public void stopObserving() {
			final ContentResolver cr = mContext.getContentResolver();
			cr.unregisterContentObserver(this);
		}
	}
    
    /** Fetch the brightness mode from the system settings and update the icon */
	private void updateMode() {
		if (mAutomaticAvailable) {
			int automatic;
			automatic = Settings.System.getIntForUser(
					mContext.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE,
					Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
					UserHandle.USER_CURRENT);
			mAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
			if (mAutomatic) {
				mAutoBrightnessImageButton.setBackgroundResource(R.drawable.hb_auto_brightness_on);
			} else {
				mAutoBrightnessImageButton.setBackgroundResource(R.drawable.hb_auto_brightness_off);
			}
		}
	}

	/** Fetch the brightness from the system settings and update the slider */
	private void updateSlider() {
		if (mAutomatic) {
			float value = Settings.System.getFloatForUser(
					mContext.getContentResolver(),
					Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0,
					UserHandle.USER_CURRENT);
			mSeekBar.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
			mSeekBar.setProgress((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
		} else {
			int value = Settings.System.getIntForUser(
					mContext.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
					UserHandle.USER_CURRENT);
			mSeekBar.setMax(mMaximumBacklight - mMinimumBacklight);
			mSeekBar.setProgress(value - mMinimumBacklight);
		}
		int curBrightness = (int)((float)mSeekBar.getProgress() / mSeekBar.getMax()* 100);
		mBrightnessText.setText(String.valueOf(curBrightness));
	}
    
}
