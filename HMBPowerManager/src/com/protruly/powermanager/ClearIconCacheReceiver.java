package com.protruly.powermanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.hb.themeicon.theme.IconManager;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.utils.LogUtils;


public class ClearIconCacheReceiver extends BroadcastReceiver {
    private static final String TAG = "ClearIconCacheReceiver";

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action  = intent.getAction();
        if ("com.hb.theme.ACTION_THEME_CHANGE".equals(action)){
            LogUtils.d(TAG, "onReceive() -> " + action);
            mContext = context;
            forceLoadAppIcon();
        }
    }

    private void forceLoadAppIcon() {
        IconManager.getInstance(mContext, true, false).clearCaches();
        new Thread() {
            @Override
            public void run() {
                AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).
                        getAppInfoModel().getThirdPartyAppsInfo();
                for (int i = 0; i < userAppsInfo.size(); i++) {
                    AppInfo appInfo = (AppInfo) userAppsInfo.get(i);
                    if (appInfo == null || !appInfo.getIsInstalled()) {
                        continue;
                    }
                    IconManager iconManager = IconManager.getInstance(mContext, true, false);
                    Drawable icon = iconManager.getIconDrawable(appInfo.getPackageName(), UserHandle.CURRENT);
                    if (icon == null) {
                        icon = appInfo.getApplicationInfo().loadIcon(mContext.getPackageManager());
                    }
                    appInfo.setIconDrawable(icon);
                }
            }
        }.start();
    }
}
