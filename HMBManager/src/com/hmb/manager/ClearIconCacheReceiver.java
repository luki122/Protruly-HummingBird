package com.hmb.manager;

import com.hb.themeicon.theme.IconManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ClearIconCacheReceiver extends BroadcastReceiver {
    private static final String TAG = "IconCache";
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action  = intent.getAction();
        if("com.hb.theme.ACTION_THEME_CHANGE".equals(action)){
            Log.d(TAG, "theme changed:"+action);
            IconManager.clearCaches();
        }
    }

}