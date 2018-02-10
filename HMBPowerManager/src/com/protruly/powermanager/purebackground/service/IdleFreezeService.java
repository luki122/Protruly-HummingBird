package com.protruly.powermanager.purebackground.service;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.BatteryManager;

import com.protruly.powermanager.purebackground.provider.AutoCleanAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class IdleFreezeService extends IntentService {
    private static final String TAG = "IdleFreezeService";

    private Context mContext;

    public static final int OP_FREEZE = 0;
    public static final int OP_UNFREEZE = 1;
    public static final String IDLE_FREEZE_OP = "op_freeze";
    public static final String ACTION_IDLE_FREEZE = "hmb.intent.action.IDLE_FREEZE";

    public IdleFreezeService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int op = intent.getIntExtra(IDLE_FREEZE_OP, OP_FREEZE);
        switch (op) {
            case OP_FREEZE:
                long startFreeze = System.currentTimeMillis();
                LogUtils.d(TAG, "------[ENTER DEVICE-IDLE BEGIN][" + startFreeze + "]------");
                handleFreezeApps();
                long endFreeze = System.currentTimeMillis();
                LogUtils.d(TAG, "------[ENTER DEVICE-IDLE END]-[" + endFreeze + "]-["
                        + (endFreeze - startFreeze) + "]------");
                break;
            case OP_UNFREEZE:
                long startUnFreeze = System.currentTimeMillis();
                LogUtils.d(TAG, "------[EXIT DEVICE-IDLE BEGIN][" + startUnFreeze + "]------");
                handleUnFreezeApps();
                long endUnFreeze = System.currentTimeMillis();
                LogUtils.d(TAG, "------[EXIT DEVICE-IDLE END][" + startUnFreeze + "]["
                        + (endUnFreeze - startUnFreeze) + "]------");
                break;
            default:
                stopSelf();
                break;
        }
    }

    private void handleFreezeApps() {
        if (isCharging()) {
            return;
        }

        ArrayList<String> freezeApps = AutoCleanAppProvider.getAutoCleanAppList2(mContext);
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        // Launcher
        String launcherPkg = ApkUtils.getDefaultLauncherPkg(mContext);

        // InputMethod
        String inputMethod = null;
        if (Utils.getDefInputMethod(mContext) != null) {
            inputMethod = Utils.getDefInputMethod(mContext).getPackageName();
        }

        // TopApp
        String topAppName = ApkUtils.getTopActivityPackageName(mContext);

        // WallPaper
        List<String> wallPaperPkgs = ApkUtils.getWallPaperPackageList(mContext);

        freezeApps.add("com.moji.daling");
        freezeApps.add("com.android.browser");
        for (String pkgName : freezeApps) {
            if (launcherPkg != null && pkgName.equalsIgnoreCase(launcherPkg)) {
                LogUtils.d(TAG, "handleFreezeApps -> ignore Launcher = " + pkgName);
                continue;
            }

            if (inputMethod != null && pkgName.equalsIgnoreCase(inputMethod)) {
                LogUtils.d(TAG, "handleFreezeApps -> ignore inputMethod = " + pkgName);
                continue;
            }

            if (audioManager.isAppInFocus(pkgName)) {
                if (audioManager.isMusicActive()) {
                    LogUtils.d(TAG, "handleFreezeApps -> ignore audioAppInFocus = " + pkgName);
                    continue;
                }
            }

            if (wallPaperPkgs.contains(pkgName)) {
                LogUtils.d(TAG, "handleFreezeApps -> ignore wallPaper = " + pkgName);
                continue;
            }

            if (ApkUtils.isInLocationWhiteList(pkgName)
                    && pkgName.equalsIgnoreCase(topAppName)) {
                LogUtils.d(TAG, "handleFreezeApps -> ignore Top locationApp -->" + pkgName);
                continue;
            }

            LogUtils.d(TAG, "------> App Standby [" + pkgName + "] <------");
            handleFreezeOp(pkgName);
        }
    }

    private void handleUnFreezeApps() {
        ArrayList<String> unfreezeApps = AutoCleanAppProvider.getAutoCleanAppList2(mContext);
        unfreezeApps.add("com.moji.daling");
        unfreezeApps.add("com.android.browser");
        for (String app: unfreezeApps) {
            LogUtils.d(TAG, "------> App Active [" + app + "] <------");
            handleUnfreezeOp(app);
        }
    }

    private void handleFreezeOp(String pkgName) {
        if (pkgName == null || !ApkUtils.isAppInstalled(mContext, pkgName)) {
            return;
        }

        PackageManager pm = mContext.getPackageManager();
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        try {
            PackageInfo packageInfo = pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS
                    | PackageManager.GET_SERVICES
                    | PackageManager.GET_RECEIVERS);
            am.forceStopPackage(pkgName);
            if (packageInfo.receivers != null) {
                for (ActivityInfo info : packageInfo.receivers) {
                    LogUtils.d(TAG, "handleFreezeOp -> FREZZ (" + info.packageName + "/" + info.name + ")");
                    ComponentName targetComponent = new ComponentName(info.packageName, info.name);
                    pm.setComponentEnabledSetting(targetComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }

            if (packageInfo.services != null) {
                for (ServiceInfo info : packageInfo.services) {
                    ComponentName targetComponent = new ComponentName(info.packageName, info.name);
                    LogUtils.d(TAG, "handleFreezeOp -> FREZZ (" + info.packageName + "/" + info.name + ")");
                    pm.setComponentEnabledSetting(targetComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
            am.forceStopPackage(pkgName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleUnfreezeOp(String pkgName) {
        if (pkgName == null || !ApkUtils.isAppInstalled(mContext, pkgName)) {
            return;
        }

        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS
                    | PackageManager.GET_SERVICES
                    | PackageManager.GET_RECEIVERS);
            if (packageInfo.receivers != null) {
                for (ActivityInfo info : packageInfo.receivers) {
                    LogUtils.d(TAG, "handleUnfreezeOp -> UNFREZZ (" + info.packageName + "/" + info.name + ")");
                    ComponentName targetComponent = new ComponentName(info.packageName, info.name);
                    pm.setComponentEnabledSetting(targetComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
            }

            if (packageInfo.services != null) {
                for (ServiceInfo info : packageInfo.services) {
                    ComponentName targetComponent = new ComponentName(info.packageName, info.name);
                    LogUtils.d(TAG, "handleUnfreezeOp -> UNFREZZ (" + info.packageName + "/" + info.name + ")");
                    pm.setComponentEnabledSetting(targetComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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