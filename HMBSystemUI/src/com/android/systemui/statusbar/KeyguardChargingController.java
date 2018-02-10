package com.android.systemui.statusbar;

import com.android.systemui.KeyguardChargingView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;

/**
 * @author wxue
 */
public class KeyguardChargingController {
	private static final String TAG = "KeyguradChargingController";
	private boolean mPowerPluggedIn;
	private boolean mLastPluggedIn = false;
    private boolean mPowerCharged;
    private Context mContext;
    private KeyguardChargingView mKeyguardChargingView;
    private int mBarStatus;
    private boolean mBouncerShowing;
    private static final int MSG_HIDE_CHARGING = 1;
    private static final int SHOW_TIMEOUT = 2100;
    
    public KeyguardChargingController(Context context, KeyguardChargingView keyguardChargingView, BatteryController batteryController){
    	mContext = context;
    	mKeyguardChargingView = keyguardChargingView;
    	KeyguardUpdateMonitor.getInstance(context).registerCallback(mUpdateMonitor);
    	mKeyguardChargingView.setBatteryController(batteryController);
    }
    
    public void setBouncerShowing(boolean bouncerShowing){
    	if(mBouncerShowing == bouncerShowing){
    		return;
    	}
    	mBouncerShowing = bouncerShowing;
    	if(mBouncerShowing){
    		updateChargingViewVisiblity();
    	}
    }
    
    public void setBarState(int barStatus){
    	if(mBarStatus == barStatus){
    		return;
    	}
    	mBarStatus = barStatus;
    	updateChargingViewVisiblity();
    }
    
	KeyguardUpdateMonitorCallback mUpdateMonitor = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
        	//Log.i(TAG,"---onRefreshBatteryInfo()---");
            boolean isChargingOrFull = status.status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status.status == BatteryManager.BATTERY_STATUS_FULL;
            mPowerPluggedIn = status.isPluggedIn() && isChargingOrFull;
            mPowerCharged = status.isCharged();
            updateChargingViewVisiblity();
        }
    };
    
    private void updateChargingViewVisiblity(){
    	//Log.i(TAG,"---updateChargingViewVisiblity()---mBarStatus = " + mBarStatus + " mBouncerShowing = " + mBouncerShowing + " mPowerPluggedIn = " + mPowerPluggedIn
    	//		+ " mLastPluggedIn = " + mLastPluggedIn);
    	if(mPowerPluggedIn && mBarStatus == StatusBarState.KEYGUARD && !mBouncerShowing){
    		if(!mLastPluggedIn){
    			mKeyguardChargingView.setVisibility(View.VISIBLE);
    			mHandler.removeMessages(MSG_HIDE_CHARGING);
        		mHandler.sendEmptyMessageDelayed(MSG_HIDE_CHARGING, SHOW_TIMEOUT);
        		mLastPluggedIn = mPowerPluggedIn;
    		}
    	}else{
    		if(!mPowerPluggedIn){
    			mLastPluggedIn = mPowerPluggedIn;
    		}
    		mKeyguardChargingView.setVisibility(View.GONE);
    	}
    }
    
    public void setChargingScrimAlpha(float alpha){
    	mKeyguardChargingView.setScrimAlpha(alpha*0.5f);
    	mKeyguardChargingView.setAlpha(alpha);
    }
    
    private final Handler mHandler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		if(msg.what == MSG_HIDE_CHARGING){
    			mKeyguardChargingView.setVisibility(View.GONE);
    		}
    	};
    };
}
