package com.protruly.powermanager.purebackground.model;

import android.content.Context;

import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.provider.AutoCleanAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.PowerSPUtils;

import java.util.HashSet;
import java.util.Locale;

/**
 * Background auto clean model.
 */
public class AutoCleanModel {
    private static final String TAG = AutoCleanModel.class.getSimpleName();

    private static final String KEY_IS_FIRST_RUN = "is_first_run";

    private Context mContext;
    private static AutoCleanModel instance;

    private AutoCleanModel(Context context) {
        this.mContext = context.getApplicationContext();
    }

    /**
     * [AutoClean Module Entry]
     * This function is called when the application is launched
     * and all application data is load completed.
     */
    public void applicationStart() {
        dealFunc();
    }

    private void dealFunc() {
        boolean isFirstRun = PowerSPUtils.instance(mContext).getBooleanValue(KEY_IS_FIRST_RUN, true);
        LogUtils.d(TAG, "AutoCleanModel() -> isFirstRun = " + isFirstRun);
        if (isFirstRun) {
            new Thread() {
                @Override
                public void run() {
                    AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).getAppInfoModel()
                            .getThirdPartyAppsInfo();
                    if (userAppsInfo != null) {
                        for (int i = 0; i < userAppsInfo.size(); i++) {
                            AppInfo appInfo = (AppInfo) userAppsInfo.get(i);
                            if (appInfo == null || !appInfo.getIsInstalled()) {
                                continue;
                            }
                            if (!isInAutoCleanDefaultWhiteList(appInfo.getPackageName())) {
                                setPkgAutoClean(appInfo.getPackageName(), true);
                                LogUtils.d(TAG, "dealFunc() -> setPkgAutoClean pkg = "
                                        + appInfo.getPackageName());
                            }
                        }
                    }
                }
            }.start();
            PowerSPUtils.instance(mContext).setBooleanValue(KEY_IS_FIRST_RUN, false);
        }
    }

    public static synchronized AutoCleanModel getInstance() {
        return instance;
    }

    public static synchronized AutoCleanModel getInstance(Context context) {
        if (instance == null) {
            instance = new AutoCleanModel(context);
        }
        return instance;
    }

    public boolean isAllAppOpened(Context context) {
        AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).getAppInfoModel()
                .getThirdPartyAppsInfo();
        HashSet<String> autoCleanAppList = AutoCleanAppProvider.getAutoCleanAppList(context);
        return userAppsInfo == null || autoCleanAppList.size() == userAppsInfo.size();
    }

    public void tryChangeAutoCleanState(String pkgName, boolean isOpen) {
        setPkgAutoClean(pkgName, isOpen);
    }

    private void setPkgAutoClean(String packageName, boolean isOpen) {
        if (packageName == null) {
            return;
        }

        if (isOpen) {
            AutoCleanAppProvider.addAutoCleanApp(mContext, packageName);
        } else {
            AutoCleanAppProvider.removeAutoCleanApp(mContext, packageName);
        }
    }

    private boolean isInAutoCleanState(String packageName) {
        return AutoCleanAppProvider.isInAutoCleanAppList(mContext, packageName);
    }

    public void installApp(AppInfo appInfo) {
        if (appInfo == null) {
            return;
        }

        if (ApkUtils.isUserApp(appInfo.getApplicationInfo())
                && !appInfo.getPackageName().equals("com.baidu.map.location")
                && !appInfo.getPackageName().equals("com.tencent.android.location")
                && !isInAutoCleanDefaultWhiteList(appInfo.getPackageName())) {
            AutoCleanAppProvider.addAutoCleanApp(mContext, appInfo.getPackageName());
        }
    }

    public void unInStallApp(String pkgName) {
        if (pkgName == null) {
            return;
        }

        if (isInAutoCleanState(pkgName)) {
            AutoCleanAppProvider.removeAutoCleanApp(mContext, pkgName);
        }
    }

    private boolean isInAutoCleanDefaultWhiteList(String pkgName) {
        for (int i = 0; i < Config.autoCleanDefaultWhiteList.length; i++) {
            if (pkgName != null && pkgName.toLowerCase(Locale.US)
                    .contains(Config.autoCleanDefaultWhiteList[i].toLowerCase(Locale.US))) {
                LogUtils.d(TAG, "isInAutoCleanDefaultWhiteList() -> " + pkgName + "is in whitelist");
                return true;
            }
        }
        return false;
    }
}