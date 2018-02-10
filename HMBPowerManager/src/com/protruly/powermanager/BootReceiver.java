package com.protruly.powermanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.protruly.powermanager.powersave.PowerSaveService;
import com.protruly.powermanager.purebackground.service.MonitorService;
import com.protruly.powermanager.utils.LogUtils;

/**
 * Performs a number of miscellaneous, non-system-critical actions for HMBPowerManager
 * after the system has finished booting.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "HMBPMBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.d(TAG, "onReceive() -> " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            startCoreServices(context);
        }
    }

    /**
     * Starts some core services after the system has finished booting.
     * @param context context
     */
    private void startCoreServices(Context context) {
        Intent powerSaveIntent = new Intent(context, PowerSaveService.class);
        context.startService(powerSaveIntent);

        Intent monitorIntent = new Intent(context, MonitorService.class);
        context.startService(monitorIntent);
    }
}