package com.protruly.powermanager.purebackground.service;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.UserHandle;

import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.provider.AutoCleanAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.PowerSPUtils;
import com.protruly.powermanager.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class BGCleanService extends IntentService {
    private static final String TAG = BGCleanService.class.getSimpleName();

    private Context mContext;

    public static final int AUTO_BG_CLEAN = 0;
    public static final int OVERLOAD_BG_CLEAN = 1;

    public static final String BG_CLEAN_TYPE = "bg_clean_type";
    public static final String ACTION_CLEAN_BG = "hmb.intent.action.BG_CLEAN";

    public BGCleanService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int cleanType = intent.getIntExtra(BG_CLEAN_TYPE, AUTO_BG_CLEAN);
        LogUtils.d(TAG, "BG_CLEAN -> CLEAN_BACKGROUND BEGIN, cleanType = " + cleanType);

        // switch
        boolean cleanSwitch = PowerSPUtils.instance(mContext)
                .getBooleanValue(PowerSPUtils.KEY_AUTO_CLEAN_BACKGROUND, true);
        if (!cleanSwitch || isCharging()) {
            return;
        }

        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        // Get RunningAppProcessInfo
        ActivityManager am =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        // Get RecentTaskInfo
        List<ActivityManager.RecentTaskInfo> recentTaskInfos =
                am.getRecentTasksForUser(10, ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS |
                        ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                        ActivityManager.RECENT_INCLUDE_PROFILES, UserHandle.myUserId());
        List<String> recentPkgs = new ArrayList<>();
        for (ActivityManager.RecentTaskInfo info : recentTaskInfos) {
            if (info.baseIntent != null) {
                recentPkgs.add(info.baseIntent.getComponent().getPackageName());
                LogUtils.d(TAG, "BG_CLEAN -> add recentPkg = "
                        + info.baseIntent.getComponent().getPackageName());
            }
        }

        // Launcher
        String launcherPkg = ApkUtils.getDefaultLauncherPkg(mContext);

        // InputMethod
        String inputMethod = null;
        if (Utils.getDefInputMethod(mContext) != null) {
            inputMethod = Utils.getDefInputMethod(mContext).getPackageName();
        }

        List<String> wallPaperPkgs = ApkUtils.getWallPaperPackageList(mContext);

        // TopApp
        String topAppName = ApkUtils.getTopActivityPackageName(mContext);

        HashSet<String> autoSleepOpenApps = AutoCleanAppProvider.getAutoCleanAppList(mContext);

        for (ActivityManager.RunningAppProcessInfo runinfo : runningAppProcesses) {
            for (String pkgName : runinfo.pkgList) {

                if (launcherPkg != null && pkgName.equalsIgnoreCase(launcherPkg)) {
                    LogUtils.d(TAG, "BG_CLEAN -> ignore Launcher = " + pkgName);
                    continue;
                }

                if (inputMethod != null && pkgName.equalsIgnoreCase(inputMethod)) {
                    LogUtils.d(TAG, "BG_CLEAN -> ignore inputMethod = " + pkgName);
                    continue;
                }

                if (audioManager.isAppInFocus(pkgName)) {
                    if (audioManager.isMusicActive()) {
                        LogUtils.d(TAG, "BG_CLEAN -> ignore audioAppInFocus = " + pkgName);
                        continue;
                    }
                }

                if (pkgName.equalsIgnoreCase(topAppName)) {
                    LogUtils.d(TAG, "BG_CLEAN -> ignore topAppName = " + pkgName);
                    continue;
                }

                if (wallPaperPkgs.contains(pkgName)) {
                    LogUtils.d(TAG, "BG_CLEAN -> ignore wallPaper = " + pkgName);
                    continue;
                }

                LocationManager locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                List<String> locationList = locMgr.getRequestLocationAppList();
                if (locationList != null && locationList.contains(pkgName)
                        && ApkUtils.isInLocationWhiteList(pkgName)) {
                    LogUtils.d(TAG, "BG_CLEAN -> ignore locationApp -->" + pkgName);
                    continue;
                }

                LogUtils.d(TAG, "BG_CLEAN -> running-->" + pkgName);
                if (cleanType == AUTO_BG_CLEAN) {
                    if (autoSleepOpenApps.contains(pkgName)) {
                        am.forceStopPackage(pkgName);
                        LogUtils.d(TAG, "BG_CLEAN -> ***kill*** -->" + pkgName);
                    }
                } else if (cleanType == OVERLOAD_BG_CLEAN) {
                    if (recentPkgs.contains(pkgName)) {
                        LogUtils.d(TAG, "BG_CLEAN -> ignore recentPkg = " + pkgName);
                        continue;
                    }
                    if (ApkUtils.isSystemApp(this, pkgName)) {
                        LogUtils.d(TAG, "BG_CLEAN -> ignore SystemApp = " + pkgName);
                        continue;
                    }
                    am.forceStopPackage(pkgName);
                    LogUtils.d(TAG, "BG_CLEAN -> ***kill*** -->" + pkgName);
                }
            }
        }

        LogUtils.d(TAG, "BG_CLEAN -> CLEAN_BACKGROUND END");
    }

    private boolean isInBlackAppList(String pkgName) {
        for (int i = 0; i < Config.blackAppList.length; i++) {
            if (pkgName != null && pkgName.toLowerCase(Locale.US)
                    .contains(Config.blackAppList[i].toLowerCase(Locale.US))) {
                LogUtils.d(TAG, "isInBlackAppList() -> " + pkgName);
                return true;
            }
        }
        return false;
    }

    private boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            LogUtils.d(TAG, "isCharging -> status = " + status);
            return status == BatteryManager.BATTERY_STATUS_CHARGING;
        }

        return false;
    }
}