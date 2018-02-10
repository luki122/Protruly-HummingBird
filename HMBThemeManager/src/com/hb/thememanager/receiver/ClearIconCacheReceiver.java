package com.hb.thememanager.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hb.themeicon.theme.IconManager;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

public class ClearIconCacheReceiver extends BroadcastReceiver {
	private static final String TAG = "IconCache";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action  = intent.getAction();
		if(Config.Action.ACTION_THEME_CHANGE.equals(action)){
			TLog.d(TAG, "theme changed:"+action);
			IconManager.clearCaches();
		}
	}

}
