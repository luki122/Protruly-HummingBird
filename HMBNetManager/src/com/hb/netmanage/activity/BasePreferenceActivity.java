package com.hb.netmanage.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;

import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.ToolsUtil;

import hb.preference.PreferenceActivity;

/**
 * 
 * @author zhaolaichao
 *
 */
public abstract class BasePreferenceActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
//		setSimStateChange();
		registerReciver();
	}

    @Override
    protected void onResume() {
    	ToolsUtil.registerHomeKeyReceiver(this);
    	super.onResume();
    }
    
	@Override
	protected void onPause() {
		ToolsUtil.unregisterHomeKeyReceiver(this);
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		//退出时不再监听卡状态变化，不通知更新界面
		unRegisterReceiver();
		super.onDestroy();
	}

	/**
	 * 监听sim状态变化, 可以用来更新UI
	 */
	public abstract void setSimStateChangeListener(int simState);
	private void registerReciver() {
		if (simStateReciver != null) {
			IntentFilter filter = new IntentFilter(SimStateReceiver.ACTION_HMB_SIM_STATUS);
			registerReceiver(simStateReciver, filter);
		}
	}
	private void unRegisterReceiver() {
		if (simStateReciver != null) {
			unregisterReceiver(simStateReciver);
		}
	}
	/**
	 * 监听sim状态变化
	 */
	private BroadcastReceiver simStateReciver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (TextUtils.equals(SimStateReceiver.ACTION_HMB_SIM_STATUS, intent.getAction())) {
				int simState = intent.getIntExtra("SIM_STATE", -1);
				// 监听sim状态变化, 可以用来更新UI
				setSimStateChangeListener(simState);
			}
		}
	};
	
}
