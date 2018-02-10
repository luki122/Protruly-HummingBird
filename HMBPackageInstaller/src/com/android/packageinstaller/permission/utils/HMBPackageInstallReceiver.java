package com.android.packageinstaller.permission.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.PermissionGroup;

import java.util.List;

/**
 * Created by xiaobin on 17-7-8.
 */

public class HMBPackageInstallReceiver extends BroadcastReceiver {

    private static final String TAG = "HMBPackageInstallReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            switch(intent.getAction()) {
                case Intent.ACTION_PACKAGE_ADDED:
                    String packageName = intent.getData().getSchemeSpecificPart();

                    PackageManager pm = context.getPackageManager();
                    PackageInfo packageInfo = null;
                    try {
                        packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (packageInfo != null &&
                            !(Utils.isSystemUpdateApp(packageInfo) || Utils.isSystemApp(packageInfo))) {
                        final PackageInfo finalPackageInfo = packageInfo;
                        new Thread() {
                            @Override
                            public void run() {
                                AppPermissions appPermissions = new AppPermissions(context, finalPackageInfo, null, false, null);
                                appPermissions.loadPermissionGroups();
                                List<AppPermissionGroup> groups = appPermissions.getPermissionGroups();
                                if (groups == null) {
                                    return;
                                }
                                for (AppPermissionGroup group : groups) {
                                    boolean grantedByDefault = group.hasGrantedByDefaultPermission();
                                    if (grantedByDefault || !group.hasRuntimePermission()) {
                                        continue;
                                    }

                                    group.revokeRuntimePermissions(false);
                                }
                            }
                        }.start();

                    }

                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    break;
                case Intent.ACTION_PACKAGE_REPLACED:
                    break;
            }
        }
    }

}
