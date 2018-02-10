package com.android.packageinstaller.permission.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by xiaobin on 17-7-8.
 */

public class HMBBootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "HMBBootCompleted";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.i(TAG, "onReceive()");

        // 初次启动把系统预装的第三方APP权限设置为拒绝
        if (!Utils.hasLaunch(context)) {
            Utils.setHasLaunch(context, true);

            Intent service = new Intent(context, HMBBootResetAppPermissionService.class);
            service.putExtra(HMBBootResetAppPermissionService.OPERATION,
                    HMBBootResetAppPermissionService.OP_RESET);
            context.startService(service);
        }

    }



}
