package com.android.packageinstaller.permission.utils;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xiaobin on 17-7-10.
 */

public class HMBBootResetAppPermissionService extends Service {

    public static final String TAG = "HMBBootResetService";

    public static final String OPERATION = "operation";
    public static final int OP_RESET = 1;

    private static Set<String> SYS_NEED_RESET_APP_SET;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int operation = intent.getIntExtra(OPERATION, 0);

            if (operation == OP_RESET) {

                new Thread() {
                    @Override
                    public void run() {
                        final PackageManager packageManager = getPackageManager();
                        List<PackageInfo> ret = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
                        if (ret != null) {
                            for (PackageInfo packageInfo : ret) {
                                if (packageInfo != null) {
                                    if (Utils.isSystemApp(packageInfo) || Utils.isSystemUpdateApp(packageInfo)) {
                                        if (isNeedResetSysApp(packageInfo)) {
                                            AppPermissions appPermissions = new AppPermissions(getApplicationContext(),
                                                    packageInfo, null, false, null);
                                            appPermissions.loadPermissionGroups();

                                            List<AppPermissionGroup> groups = appPermissions.getPermissionGroups();
                                            if (groups == null) {
                                                continue;
                                            }

                                            switch (packageInfo.packageName) {
//                                                case "com.protruly.gallery3d.app":
//                                                    for (AppPermissionGroup group : groups) {
//                                                        if (group.getName().equals(Manifest.permission_group.LOCATION)) {
//                                                            group.revokeRuntimePermissions(false);
//                                                        }
//                                                    }
//                                                    break;
//
//                                                case "com.bql.camera":
//                                                    for (AppPermissionGroup group : groups) {
//                                                        boolean grantedByDefault = group.hasGrantedByDefaultPermission();
//                                                        if (grantedByDefault || !group.hasRuntimePermission()) {
//                                                            continue;
//                                                        }
//
//                                                        group.revokeRuntimePermissions(false);
//                                                    }
//                                                    break;
                                                case "com.android.calendar":
                                                case "com.android.deskclock":
                                                case "com.android.dlauncher":
                                                case "com.android.quicksearchbox":
                                                case "com.android.mms":
                                                    for (AppPermissionGroup group : groups) {
                                                        group.grantRuntimePermissions(false);
                                                    }
                                                    break;
                                            }
                                        }
                                    }
//                                    else {
//                                        AppPermissions appPermissions = new AppPermissions(getApplicationContext(),
//                                                packageInfo, null, false, null);
//                                        appPermissions.loadPermissionGroups();
//                                        List<AppPermissionGroup> groups = appPermissions.getPermissionGroups();
//                                        if (groups == null) {
//                                            continue;
//                                        }
//                                        for (AppPermissionGroup group : groups) {
//                                            boolean grantedByDefault = group.hasGrantedByDefaultPermission();
//                                            if (grantedByDefault || !group.hasRuntimePermission()) {
//                                                continue;
//                                            }
//
//                                            group.revokeRuntimePermissions(false);
//                                        }
//                                    }
                                }
                            }
                        }

                    }
                }.start();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private static boolean isNeedResetSysApp(PackageInfo pInfo) {
        if (SYS_NEED_RESET_APP_SET == null) {
            SYS_NEED_RESET_APP_SET = new HashSet<String>();
            SYS_NEED_RESET_APP_SET.add("com.bql.camera");
            SYS_NEED_RESET_APP_SET.add("com.android.calendar");
            SYS_NEED_RESET_APP_SET.add("com.android.quicksearchbox");
            SYS_NEED_RESET_APP_SET.add("com.android.mms");
            SYS_NEED_RESET_APP_SET.add("com.protruly.gallery3d.app");
            SYS_NEED_RESET_APP_SET.add("com.android.dlauncher");
            SYS_NEED_RESET_APP_SET.add("com.android.deskclock");
        }

        if (pInfo != null) {
            if (SYS_NEED_RESET_APP_SET.contains(pInfo.packageName)) {
                return true;
            }
        }
        return false;
    }


}
