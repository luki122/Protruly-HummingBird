package com.protruly.powermanager.purebackground.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.provider.ForbitAlarmAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.PowerSPUtils;

public class ForbitAlarmModel {

    private static final String TAG = "ForbitAlarmModel";

    private Context mContext;

    private static ForbitAlarmModel mInstance;
    
    private static final String KEY_FORBIT_ALARM_IS_FIRST_RUN = "forbit_alarm_is_first_run";
    
    private static final String ACTION_SAVEPOWER_UPDATEXML = "com.protruly.powermanager.savepower.forbitalarmapplistchanged";

    private ForbitAlarmModel(Context context) {
        this.mContext = context.getApplicationContext();
    }

    /**
     * mAllUserAppsInfoMap [ForbitAlarm Module Entry] This function is called
     * when the application is launched and all application data is load
     * completed.
     */
    public void applicationStart() {
        writeAllForbitAlarmAppsIntoXML();
    }

    private void writeAllForbitAlarmAppsIntoXML() {
        boolean isFirstRun = PowerSPUtils.instance(mContext).getBooleanValue(KEY_FORBIT_ALARM_IS_FIRST_RUN, true);
        LogUtils.d(TAG, "ForbitAlarmModel, isFirstRun = " + isFirstRun);

        if (isFirstRun) {
            new Thread() {
                @Override
                public void run() {
                    List<String> pkgList = new ArrayList<String>();
                    AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).getAppInfoModel()
                            .getThirdPartyAppsInfo();
                    if (userAppsInfo != null) {
                        for (int i = 0; i < userAppsInfo.size(); i++) {
                            AppInfo appInfo = (AppInfo) userAppsInfo.get(i);
                            if (appInfo == null || !appInfo.getIsInstalled()) {
                                continue;
                            }

                            if (!isInForbitAlarmWhiteList(appInfo.getPackageName())) {
                                pkgList.add(appInfo.getPackageName());
                                LogUtils.d(TAG, "applicationStart -> set forbit alarm for pkg = "
                                        + appInfo.getPackageName());
                            }
                        }

                        ForbitAlarmAppProvider.writeForbitAlarmAppsIntoXML(pkgList);
                    }

                    sendUpdateForbitAlarmAppsBroadcast();
                }
            }.start();
        }
        PowerSPUtils.instance(mContext).setBooleanValue(KEY_FORBIT_ALARM_IS_FIRST_RUN, false);
    }

    protected void sendUpdateForbitAlarmAppsBroadcast() {
        Intent updateXML = new Intent();
        updateXML.setAction(ACTION_SAVEPOWER_UPDATEXML);
        mContext.sendBroadcastAsUser(updateXML, UserHandle.CURRENT);
        LogUtils.d(TAG, "send update forbit alarm apps broadcast");
    }

    public static ForbitAlarmModel getInstance() {
        return mInstance;
    }

    public static ForbitAlarmModel getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ForbitAlarmModel(context);
        }

        return mInstance;
    }

    public boolean isAllAppOpened(Context context) {
        AppsInfo userAppsInfo = ConfigModel.getInstance(mContext).getAppInfoModel()
                .getThirdPartyAppsInfo();
//        HashSet<String> autoCleanAppList = ForbitAlarmAppProvider.getForbitAlarmAppList(context);
        List<String> autoCleanAppList = ForbitAlarmAppProvider.getForbitAlarmAppList(context);

        return userAppsInfo == null || autoCleanAppList.size() == userAppsInfo.size();
    }

    public void tryChangeForbitAlarmState(String pkgName, boolean isOpen) {
        setAppForbitAlarmState(pkgName, isOpen);
    }

    private void setAppForbitAlarmState(String packageName, boolean isOpen) {
        if (packageName == null) {
            return;
        }

        if (isOpen) {
            ForbitAlarmAppProvider.addForbitAlarmAppInToXML(mContext, packageName);
        } else {
            ForbitAlarmAppProvider.removeForbitAlarmAppFromXML(mContext, packageName);
        }
    }

    public void unInStallApp(String pkgName) {
        if (pkgName == null) {
            return;
        }

        if (isInForbitAlarmState(pkgName)) {
            ForbitAlarmAppProvider.removeForbitAlarmAppFromXML(mContext, pkgName);
        }
    }

    private boolean isInForbitAlarmState(String packageName) {
        return ForbitAlarmAppProvider.isInForbitAlarmAppList(mContext, packageName);
    }

    public boolean isInForbitAlarmWhiteList(String packageName) {
        for (int i = 0; i < Config.forbitAlarmWhiteList.length; i++) {
            if (packageName != null
                    && packageName.toLowerCase(Locale.US).contains(
                            Config.autoStartWhiteList[i].toLowerCase(Locale.US))) {
                LogUtils.d(TAG, packageName + " In forbitalarmWhiteList");
                return true;
            }
        }
        return false;
    }

    public void installApp(AppInfo appInfo) {
        if (appInfo == null) {
            return;
        }

        if (ApkUtils.isUserApp(appInfo.getApplicationInfo())
                && !isInForbitAlarmWhiteList(appInfo.getPackageName())) {
            ForbitAlarmAppProvider.addForbitAlarmAppInToXML(mContext, appInfo.getPackageName());
        }
    }

}
