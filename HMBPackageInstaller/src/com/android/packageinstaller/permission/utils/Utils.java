/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.packageinstaller.permission.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;

import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.PermissionApps.PermissionApp;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    private static final String LOG_TAG = "Utils";

    public static final String OS_PKG = "android";

    public static final String[] MODERN_PERMISSION_GROUPS = {
            Manifest.permission_group.CALENDAR,
            Manifest.permission_group.CAMERA,
            Manifest.permission_group.CONTACTS,
            Manifest.permission_group.LOCATION,
            Manifest.permission_group.SENSORS,
            Manifest.permission_group.SMS,
            Manifest.permission_group.PHONE,
            Manifest.permission_group.MICROPHONE,
            Manifest.permission_group.STORAGE
    };

    private static Map<String, Integer> PERMISSION_GROUPS_ICON_MAP;

    private static final Intent LAUNCHER_INTENT = new Intent(Intent.ACTION_MAIN, null)
                            .addCategory(Intent.CATEGORY_LAUNCHER);

    private Utils() {
        /* do nothing - hide constructor */
    }

    public static Drawable loadDrawable(PackageManager pm, String pkg, int resId) {
        try {
            return pm.getResourcesForApplication(pkg).getDrawable(resId, null);
        } catch (Resources.NotFoundException | PackageManager.NameNotFoundException e) {
            Log.d(LOG_TAG, "Couldn't get resource", e);
            return null;
        }
    }

    public static boolean isModernPermissionGroup(String name) {
        for (String modernGroup : MODERN_PERMISSION_GROUPS) {
            if (modernGroup.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldShowPermission(AppPermissionGroup group, String packageName) {
        // We currently will not show permissions fixed by the system.
        // which is what the system does for system components.
        if (group.isSystemFixed() && !LocationUtils.isLocationGroupAndProvider(
                group.getName(), packageName)) {
            return false;
        }

        final boolean isPlatformPermission = group.getDeclaringPackage().equals(OS_PKG);
        // Show legacy permissions only if the user chose that.
        if (isPlatformPermission
                && !Utils.isModernPermissionGroup(group.getName())) {
            return false;
        }
        return true;
    }

    public static boolean shouldShowPermission(PermissionApp app) {
        // We currently will not show permissions fixed by the system
        // which is what the system does for system components.
        if (app.isSystemFixed() && !LocationUtils.isLocationGroupAndProvider(
                app.getPermissionGroup().getName(), app.getPackageName())) {
            return false;
        }

        return true;
    }

    public static Drawable applyTint(Context context, Drawable icon, int attr) {
        Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(attr, typedValue, true);
        icon = icon.mutate();
        icon.setTint(context.getColor(typedValue.resourceId));
        return icon;
    }

    public static Drawable applyTint(Context context, int iconResId, int attr) {
        return applyTint(context, context.getDrawable(iconResId), attr);
    }

    public static Drawable applyTintByColor(Context context, Drawable icon, int colorId) {
        if (icon != null) {
            Theme theme = context.getTheme();
            icon = icon.mutate();
            icon.setTint(context.getColor(colorId));
        }
        return icon;
    }

    public static Drawable applyTintByColor(Context context, int iconResId, int colorId) {
        return applyTintByColor(context, context.getDrawable(iconResId), colorId);
    }

    public static Drawable getPermissionGroupsIcon(Context context, String groupName, Drawable defaultIcon) {
        return getPermissionGroupsIcon(context, groupName, defaultIcon, true);
    }

    public static Drawable getPermissionGroupsIcon(Context context, String groupName, Drawable defaultIcon, boolean tint) {
        if (PERMISSION_GROUPS_ICON_MAP == null) {
            PERMISSION_GROUPS_ICON_MAP = new HashMap<String, Integer>();
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.CALENDAR, R.drawable.perm_group_calendar);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.CAMERA, R.drawable.perm_group_camera);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.CONTACTS, R.drawable.perm_group_contacts);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.LOCATION, R.drawable.perm_group_location);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.SENSORS, R.drawable.perm_group_sensors);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.SMS, R.drawable.perm_group_messages);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.PHONE, R.drawable.perm_group_phone_calls);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.MICROPHONE, R.drawable.perm_group_recording);
            PERMISSION_GROUPS_ICON_MAP.put(Manifest.permission_group.STORAGE, R.drawable.perm_group_storage);
        }

        if (PERMISSION_GROUPS_ICON_MAP.get(groupName) != null) {
            return context.getDrawable(PERMISSION_GROUPS_ICON_MAP.get(groupName));
        } else {
            if (tint) {
                return applyTintByColor(context, defaultIcon, R.color.color_range_title);
            } else {
                return defaultIcon;
            }

        }
    }

    public static ArraySet<String> getLauncherPackages(Context context) {
        ArraySet<String> launcherPkgs = new ArraySet<>();
        for (ResolveInfo info :
            context.getPackageManager().queryIntentActivities(LAUNCHER_INTENT, 0)) {
            launcherPkgs.add(info.activityInfo.packageName);
        }

        return launcherPkgs;
    }

    public static boolean isSystem(PermissionApp app, ArraySet<String> launcherPkgs) {
        ApplicationInfo info = app.getAppInfo();
        /// M: Hide system apps if MTK's runtime permission is used
        if (MTK_RUNTIME_PERMISSON_SUPPORT) {
            return info.isSystemApp()
                && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                && (info.flagsEx & ApplicationInfo.FLAG_EX_OPERATOR) == 0;
        }
        return info.isSystemApp() && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                && !launcherPkgs.contains(info.packageName);
    }

    public static boolean isTelevision(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /// M: Support MTK's runtime permission control
    public static final boolean MTK_RUNTIME_PERMISSON_SUPPORT = SystemProperties.get(
                                               "ro.mtk_runtime_permission").equals("1");

    // protruly linxiaobin start

    /**
     * 是否是系统软件
     * @return
     */
    public static boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    /**
     * 是否是系统软件或者是系统软件的更新软件
     * @return
     */
    public static boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    private static final String KEY_HAS_LAUNCH = "key_has_launch";

    public static boolean hasLaunch(Context context) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            boolean result = sp.getBoolean(KEY_HAS_LAUNCH, false);
            return result;
        }
        return true;
    }

    public static void setHasLaunch(Context context, boolean hasLaunch) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(KEY_HAS_LAUNCH, hasLaunch).commit();
        }
    }

    // protruly linxiaobin end

}
