package com.hb.manager;

import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.InCallApp;
import com.android.incallui.InCallPresenter;
import com.android.incallui.InCallPresenter.InCallState;
import com.hb.utils.SettingUtils;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.BatteryManager;
import android.util.Log;

public class FlashLightManager extends HbManagerBase {
	private static final String LOG_TAG = "FlashLightManager";

	private volatile boolean mIsFlashing = false;
	private Camera mCamera;
	KeyguardManager mKeyguardManager;
	private boolean isBatteryLow = false;
	private int mSystemLowBattery;
	
	public FlashLightManager(Context context) {
		mContext = context;
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mSystemLowBattery = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
	}

	public void tearDown() {	
		super.tearDown();
	}

	private static FlashLightManager mFlashLightManager;

	public static synchronized FlashLightManager getInstance() {
		if (mFlashLightManager == null) {
			mFlashLightManager = new FlashLightManager(InCallApp.getInstance());
		}
		return mFlashLightManager;
	}

	@Override
	public void onIncomingCall(InCallState oldState, InCallState newState,
			Call call) {
		// TODO Auto-generated method stub
		handleFlashLight();
	}

	@Override
	public void onStateChange(InCallState oldState, InCallState newState,
			CallList callList) {
		// TODO Auto-generated method stub
		handleFlashLight();
	}

	private void handleFlashLight() {
		InCallState state = InCallPresenter.getInstance().getInCallState();
		Log.d(LOG_TAG, "handleFlashLight state = " + state);
		if (state == InCallState.INCOMING && isSwitchOn()) {
			if (!mIsFlashing) {
				mIsFlashing = true;
				flash();
			}
		} else {
			if (mIsFlashing) {
				mIsFlashing = false;
			}
		}
	}

	private void flash() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mIsFlashing) {
                	flashOn();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                	flashOff();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }                    
                }
            }
        }).start();
	}
	
	private void flashOn() {
		 synchronized (this) {
			try {
				mCamera = Camera.open();
				Camera.Parameters mParameters = mCamera.getParameters();
				mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(mParameters);
				mCamera.startPreview(); // 开始亮灯
	//			mCamera.autoFocus(new AutoFocusCallback() {
	//				public void onAutoFocus(boolean success, Camera camera) {
	//				}
	//			});
			} catch (Exception ex) {
			}
		}
	}

	private void flashOff() {
		 synchronized (this) {
			try {
				Camera.Parameters mParameters = mCamera.getParameters();
				mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				mCamera.setParameters(mParameters);
				mCamera.stopPreview();// 关掉亮灯
				mCamera.release();
			} catch (Exception ex) {
			}
		}
	}

	private boolean isSwitchOn() {
		return SettingUtils.getSetting(InCallApp.getInstance(), "flash") && mKeyguardManager.isKeyguardLocked() && !isBatteryLow;
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
				int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
				int mBatteryPercentage = (level * 100) / scale;
				Log.d(LOG_TAG, "mBatteryPercentage=" + mBatteryPercentage);
				isBatteryLow = (mBatteryPercentage < mSystemLowBattery  && plugType == 0) ? true
						: false;
				if(isBatteryLow)  {
					mIsFlashing = false;
				}
			}
		}
           
    };
}
