package com.hmb.manager.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.hmb.manager.Constant;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;

import static com.hmb.manager.update.HMBUpdateManager.UPDATE_ACTION_AUTO;

public class UpdateReceiver extends BroadcastReceiver {
	public static final String TAG = "UpdateReceiver";

    public static final String ACTION_AUTO_CHECK = "com.hmb.manager.AUTO_CHECK";
	
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive() -> action = " + action);
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info != null && info.getState().equals(NetworkInfo.State.CONNECTED)
                    && checkExpiredTime(context)) {
                ManagerUtils.scheduleUpdateService(context);
            }
        } else if (action.equals(ACTION_AUTO_CHECK)) {
            HMBUpdateManager.getInstance(context).scheduleUpdate(UPDATE_ACTION_AUTO, false);
        }
    }

    boolean checkExpiredTime(Context context) {
        SPUtils spUtils = SPUtils.instance(context);
        long lastCheck = spUtils.getLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, 0);
        if (lastCheck > System.currentTimeMillis()){
            spUtils.setLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, lastCheck, 0);
            lastCheck = 0;
        }
        long lCurrentMini = System.currentTimeMillis();
        Log.d(TAG, "checkExpiredTime -> lastCheck = " + lastCheck + ", updateFrequency = "
                + Constant.AUTO_CHECK_INTERVAL + ", currentTime = " + lCurrentMini);

        return Constant.AUTO_CHECK_INTERVAL + lastCheck <= lCurrentMini;
    }
}