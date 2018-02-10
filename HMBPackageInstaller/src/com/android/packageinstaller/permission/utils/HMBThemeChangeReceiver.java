package com.android.packageinstaller.permission.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hb.themeicon.theme.IconManager;

/**
 * Created by xiaobin on 17-8-3.
 */

public class HMBThemeChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        IconManager iconManager = IconManager.getInstance(context, true, false);
        iconManager.clearCaches();
    }

}
