package com.hb.netmanage.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.ToolsUtil;

import hb.app.HbActivity;

public abstract class BaseActivity extends HbActivity {

	private long mTime = 0;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case 1000:
					int simState = msg.arg1;
					mTime = System.currentTimeMillis();
					setSimStateChangeListener(simState);
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		registerReciver();
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	ToolsUtil.registerHomeKeyReceiver(this);
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		ToolsUtil.unregisterHomeKeyReceiver(this);
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
		super.onDestroy();
		//退出时不再监听卡状态变化，不通知更新界面
		unRegisterReceiver();
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
				final int simState = intent.getIntExtra("SIM_STATE", -1);
				// 监听sim状态变化, 可以用来更新UI
				if (simState == SimStateReceiver.SIM_VALID) {
					if (System.currentTimeMillis() - mTime < 2000) {
						mTime = System.currentTimeMillis();
						return;
					}
					Message msg = mHandler.obtainMessage();
					msg.what = 1000;
					msg.arg1 = simState;
					mHandler.sendMessage(msg);
				} else {
					mTime = System.currentTimeMillis();
					setSimStateChangeListener(simState);
				}
			}
		}
	};
}
