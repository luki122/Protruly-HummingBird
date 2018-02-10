package com.hmb.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;

/**
 * Performs a number of miscellaneous, non-system-critical actions for HMBManager
 * after the system has finished booting.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive() -> " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            checkDatabaseUpdate(context);
        }
    }

    private void checkDatabaseUpdate(Context context) {
        SPUtils spUtils = SPUtils.instance(context);
        long lastCheck = spUtils.getLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, 0);
        if (lastCheck > System.currentTimeMillis()) {
            spUtils.setLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, lastCheck, 0);
        }
        ManagerUtils.scheduleUpdateService(context);
    }
}