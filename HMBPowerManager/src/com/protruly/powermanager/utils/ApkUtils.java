package com.protruly.powermanager.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.hb.themeicon.theme.IconManager;
import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AutoStartInfo;
import com.protruly.powermanager.purebackground.model.AutoStartModel;
import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.purebackground.provider.AutoStartAppProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * This is a utility class for retrieving all of the information we know
 * about a particular package/application.
 */
public class ApkUtils {
    private static final String TAG = ApkUtils.class.getSimpleName();

    /**
     * Get uid by packageName.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static int getUidByPackageName(Context context, String packageName) {
        int uid = -1;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            if (ai != null) {
                uid = ai.uid;
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return uid;
    }

    /**
     * Retrieve the official name associated with a user id.
     *
     * @param context
     * @param uid
     * @return
     */
    public static synchronized String getPackageNameByUid (Context context, int uid) {
        if (context == null) {
            return null;
        }
        String packageName = "";
        try {
            packageName = context.getPackageManager().getNameForUid(uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packageName;
    }

    /**
     * Get appName by PackageName.
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static synchronized String getAppNameByPackageName(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            return null;
        }
        String appName = "";

        PackageManager pm = context.getPackageManager();
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(pkgName,
                    PackageManager.GET_META_DATA)).toString();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }

    /**
     * Retrieve the names of all packages that are associated with a particular user id
     *
     * @param context
     * @param uid
     * @return
     */
    public static synchronized String[] getPackagesForUid(Context context, int uid) {
        if (context == null) {
            return null;
        }
        try {
            return context.getPackageManager().getPackagesForUid(uid);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get Apk versionName.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized String getApkVersionName (Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return "";
        }
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Get Apk versionCode.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized int getApkVersionCode(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return -1;
        }
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getApkName(Context context, ApplicationInfo applicationInfo) {
        if (context == null || applicationInfo == null) {
            return null;
        }
        PackageManager pm = ConfigModel.getInstance(context).getPackageManager();
        CharSequence label = applicationInfo.loadLabel(pm);
        String apkName = label != null ? label.toString() : applicationInfo.packageName;
        return apkName;
    }

    public static synchronized void initAppNameInfo(Context context, AppInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        String appName = appInfo.getAppName();
        if (appName == null) {
            appName = getApkName(context, appInfo.getApplicationInfo());
            appInfo.setAppName(appName);
        }
        appInfo.setAppNamePinYin(Utils.getSpell(appName));
    }

    /**
     * Get services information about the package.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized ServiceInfo[] getApkServiceInfos(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            return packageInfo.services;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get receivers information about the package.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized ActivityInfo[] getApkReceiveInfos(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_RECEIVERS);
            return packageInfo.receivers;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static synchronized boolean canFindAppInfo(Context context, String packageName) {
        return ApkUtils.getApplicationInfo(context, packageName) != null;
    }

    /**
     * Get applicationInfo by packageName.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized ApplicationInfo getApplicationInfo(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return null;
        }
        ApplicationInfo appinfo = null;
        try {
            appinfo = context.getPackageManager().getPackageInfo(packageName, 0).applicationInfo;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return appinfo;
    }

    /**
     * Get PackageInfo by packageName.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized PackageInfo getPackageInfo(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return null;
        }

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo;
    }

    /**
     * Is user app.
     * @param context
     * @param packageName
     * @return true - UserApp,  false - System app
     */
    public static synchronized boolean filterApp(Context context, String packageName) {
        return filterApp(getApplicationInfo(context, packageName));
    }

    /**
     * Filter User App
     * @param info
     * @return true - UserApp,  false - System app
     */
    public static synchronized boolean filterApp(ApplicationInfo info) {
        return info != null && isUserApp(info);
    }

    public static boolean isSystemApp(Context context, String pkgName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkgName, 0);
            return isSystemApp(info);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isSystemApp(ApplicationInfo info) {
        return info != null && ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static boolean isSystemUpdateApp(ApplicationInfo info) {
        return info != null && ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public static boolean isUserApp(ApplicationInfo info) {
        return info != null && (!isSystemApp(info) && !isSystemUpdateApp(info));
    }

    /**
     * Get Full AppInfo.
     *
     * @param context
     * @param packageInfo
     * @return
     */
    public static synchronized AppInfo getAppFullInfo(Context context,
                                                      PackageInfo packageInfo) {
        AppInfo appInfo = getAppBasicInfo(context, packageInfo);
        // add more info ?
        if (appInfo == null) {
            return null;
        }
        return appInfo;
    }

    /**
     * Get Basic AppInfo form PackageInfo.
     *
     * @param context
     * @param packageInfo
     * @return
     */
    public static synchronized AppInfo getAppBasicInfo(Context context, PackageInfo packageInfo) {
        if (context == null || packageInfo == null) {
            return null;
        }
        AppInfo appInfo = new AppInfo();
        appInfo.setApplicationInfo(packageInfo.applicationInfo);
        appInfo.setPackageName(packageInfo.packageName);
        appInfo.setUid(packageInfo.applicationInfo.uid);
        appInfo.setVersionCode(packageInfo.versionCode);
        appInfo.setVersionName(packageInfo.versionName);
        appInfo.setReceivers(packageInfo.receivers);
        appInfo.setServices(packageInfo.services);

        IconManager iconManager = IconManager.getInstance(context, true, false);
        Drawable icon = iconManager.getIconDrawable(packageInfo.packageName, UserHandle.CURRENT);
        if (icon == null) {
            icon = packageInfo.applicationInfo.loadIcon(context.getPackageManager());
        }
        appInfo.setIconDrawable(icon);

        if (filterApp(packageInfo.applicationInfo)
                && (!packageInfo.packageName.equals("com.baidu.map.location"))
                && (!packageInfo.packageName.equals("com.tencent.android.location"))) {
            appInfo.setIsUserApp(true);
        } else {
            appInfo.setIsUserApp(false);
            appInfo.setIsHome(isHome(context, packageInfo.packageName));
            appInfo.setIsHaveLauncher(isHaveLauncher(context, packageInfo.packageName));
        }
        return appInfo;
    }

    /**
     * Get Default Launcher packageName.
     * @param context
     * @return
     */
    public static String getDefaultLauncherPkg(Context context) {
        String pkg = "com.android.dlauncher";
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo match = context.getPackageManager()
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (match != null && match.activityInfo != null) {
                pkg = match.activityInfo.packageName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkg;
    }

    /**
     * Is displayed in the top-level launcher.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized boolean isHaveLauncher(Context context, String packageName) {
        ResolveInfo resolveInfo = getApkMainResolveInfo(context, packageName);
        if (resolveInfo != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * queryIntentActivities(Intent.ACTION_MAIN Intent.CATEGORY_LAUNCHER)
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized ResolveInfo getApkMainResolveInfo(Context context, String packageName) {
        if (context == null || packageName == null) {
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);
        List<ResolveInfo> homes = context.getPackageManager().queryIntentActivities(intent, 0);
        if ((homes != null && homes.size() > 0)) {
            return homes.get(0);
        } else {
            return null;
        }
    }

    /**
     * Is Launcher application.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized boolean isHome(Context context, String packageName) {
        if (context == null || packageName == null) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(packageName);
        List<ResolveInfo> homes = context.getPackageManager().queryIntentActivities(intent, 0);
        if ((homes != null && homes.size() > 0)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close APK AutoStart.
     *
     * @param context
     * @param appInfo
     */
    public static synchronized void closeApkAutoStart(Context context, AppInfo appInfo) {
        if (context == null ||
                appInfo == null ||
                !appInfo.getIsInstalled()) {
            return;
        }
        AutoStartInfo autoStartInfo = AutoStartModel.getInstance(context)
                .getAutoStartInfo(appInfo.getPackageName());
        closeApkAutoStart(context, autoStartInfo, appInfo.getPackageName());
    }

    /**
     * Disable AutoStart capability.
     *
     * @param context
     * @param autoStartInfo
     * @param pkgName
     */
    public static synchronized void closeApkAutoStart(Context context, AutoStartInfo autoStartInfo,
                                                      String pkgName) {
        if (context == null || autoStartInfo == null) {
            return;
        }
        ComponentName tmpComponent = null;

        //bootreceiver
        ResolveInfo resolveInfo = autoStartInfo.getBootReceiveResolveInfo();
        if (resolveInfo != null) {
            tmpComponent = new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            setComponentEnabledState(context,
                    tmpComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        }

        List<ResolveInfo> resolveInfoList = autoStartInfo.getResolveInfoList();
        if (resolveInfoList != null) {
            for (ResolveInfo tmpresolveInfo : resolveInfoList) {
                if (tmpresolveInfo == null) {
                    continue;
                }
                tmpComponent = new ComponentName(
                        tmpresolveInfo.activityInfo.packageName,
                        tmpresolveInfo.activityInfo.name);

                setComponentEnabledState(context,
                        tmpComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
        }

        //service
        ServiceInfo[] serviceInfos = autoStartInfo.getServiceInfo();
        if (serviceInfos != null) {
            for (ServiceInfo serviceInfo : serviceInfos) {
                if (serviceInfo == null) {
                    continue;
                }
                tmpComponent = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                setComponentEnabledState(context,
                        tmpComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
        }

        //receive
        ActivityInfo[] receiveInfos = autoStartInfo.getReceiveInfo();
        if (receiveInfos != null) {
            for (ActivityInfo receiveInfo : receiveInfos) {
                if (receiveInfo == null) {
                    continue;
                }
                tmpComponent = new ComponentName(receiveInfo.packageName, receiveInfo.name);
                setComponentEnabledState(context,
                        tmpComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
        }

        LogUtils.d(TAG, "closeApkAutoStart() -> pkgName = " +  pkgName);
        autoStartInfo.setIsOpen(false);
        HashSet<String> autostartApps = AutoStartAppProvider.getAutoStartAppList(context);
        autoStartInfo.setAutoStartOfUser(autostartApps.contains(pkgName));
    }

    /**
     * Open APK AutoStart.
     *
     * @param context
     * @param appInfo
     */
    public static synchronized void openApkAutoStart(Context context, AppInfo appInfo) {
        if (context == null || appInfo == null || !appInfo.getIsInstalled()) {
            return;
        }

        AutoStartInfo autoStartInfo = AutoStartModel.getInstance(context).
                getAutoStartInfo(appInfo.getPackageName());
        openApkAutoStart(context, autoStartInfo, appInfo.getPackageName());
    }

    /**
     * Enable AutoStart capability.
     *
     * @param context
     * @param autoStartInfo
     * @param pkgName
     */
    public static synchronized void openApkAutoStart(Context context, AutoStartInfo autoStartInfo,
                                                     String pkgName) {
        if (context == null || autoStartInfo == null) {
            return;
        }

        ComponentName tmpComponent = null;

        //bootreceiver
        ResolveInfo resolveInfo = autoStartInfo.getBootReceiveResolveInfo();
        if (resolveInfo != null) {
            tmpComponent = new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);

            setComponentEnabledState(context,
                    tmpComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        }

        try {
            List<ResolveInfo> resolveInfoList = autoStartInfo.getResolveInfoList();
            if (resolveInfoList != null) {
                int length = resolveInfoList.size();
                ResolveInfo tmpresolveInfo;
                for (int i = 0; i < length; i++) {
                    tmpresolveInfo = resolveInfoList.get(i);
                    if (tmpresolveInfo == null) {
                        continue;
                    }
                    tmpComponent = new ComponentName(
                            tmpresolveInfo.activityInfo.packageName,
                            tmpresolveInfo.activityInfo.name);

                    setComponentEnabledState(context,
                            tmpComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.toString());
        }

        //service
        ServiceInfo[] serviceInfos = autoStartInfo.getServiceInfo();
        if (serviceInfos != null) {
            for (ServiceInfo serviceInfo : serviceInfos) {
                if (serviceInfo == null) {
                    continue;
                }
                tmpComponent = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                setComponentEnabledState(context,
                        tmpComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            }
        }

        //receive
        ActivityInfo[] receiveInfos = autoStartInfo.getReceiveInfo();
        if (receiveInfos != null) {
            for (ActivityInfo receiveInfo : receiveInfos) {
                if (receiveInfo == null) {
                    continue;
                }
                tmpComponent = new ComponentName(receiveInfo.packageName, receiveInfo.name);
                setComponentEnabledState(context,
                        tmpComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            }
        }

        LogUtils.d(TAG, "openApkAutoStart() -> pkgName = " +  pkgName);
        autoStartInfo.setIsOpen(true);
        HashSet<String> autostartApps = AutoStartAppProvider.getAutoStartAppList(context);
        autoStartInfo.setAutoStartOfUser(autostartApps.contains(pkgName));
    }

    /**
     * IS App inStalled.
     *
     * @param context
     * @param packageName
     * @return
     */
    public static synchronized boolean isAppInstalled(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, 0);
            installed = true;
        } catch (NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    /**
     * Set Component Enabled State.
     *
     * @param context
     * @param tmpCpName
     * @param targetState
     *        (1)PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
     *        (2)PackageManager.COMPONENT_ENABLED_STATE_ENABLED
     *        (3)PackageManager.COMPONENT_ENABLED_STATE_DISABLED
     */
    public static boolean setComponentEnabledState(Context context,
                                                   ComponentName tmpCpName,
                                                   int targetState) {
        boolean result = true;
        PackageManager pm = context.getPackageManager();
        try {
            if (isAppInstalled(context, tmpCpName.getPackageName())) {
                pm.setComponentEnabledSetting(tmpCpName, targetState, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get Component Enabled State.
     *
     * @param context
     * @param tmpCpName
     * @return true：open  false: close
     */
    public static boolean getComponentEnabledState(Context context,
                                                   ComponentName tmpCpName) throws Exception {
        if (context == null || tmpCpName == null) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        int autoStartState = pm.getComponentEnabledSetting(tmpCpName);
        return autoStartState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    public static List<InputMethodInfo> getEnabledInputMethodList(Context context) {
        if (context == null) {
            return null;
        }
        InputMethodManager mImm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> mImis = mImm.getEnabledInputMethodList();
        return mImis;
    }

    public static String getTopActivityPackageName(Context context) {
        String pkgName = null;
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
            if (runningTaskInfos != null && runningTaskInfos.get(0) != null
                    && runningTaskInfos.get(0).baseActivity != null) {
                pkgName = runningTaskInfos.get(0).baseActivity.getPackageName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pkgName;
    }

    public static ArrayList<String> getWallPaperPackageList(Context context) {
        ArrayList<String> wallPaperPkgs = new ArrayList<>();
        Intent intent = new Intent("android.service.wallpaper.WallpaperService");
        List<ResolveInfo> resolveInfoList = context.getPackageManager().
                queryIntentServices(intent, PackageManager.GET_DISABLED_COMPONENTS);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo == null ||
                    resolveInfo.serviceInfo == null ||
                    resolveInfo.serviceInfo.packageName == null ||
                    resolveInfo.serviceInfo.name == null) {
                continue;
            }

            wallPaperPkgs.add(resolveInfo.serviceInfo.packageName);
        }

        return wallPaperPkgs;
    }

    public static boolean isInLocationWhiteList(String pkgName) {
        for (int i = 0; i < Config.locationWhiteList.length; i++) {
            if (pkgName != null && pkgName.toLowerCase(Locale.US)
                    .contains(Config.locationWhiteList[i].toLowerCase(Locale.US))) {
                LogUtils.d(TAG, "isInLocationWhiteList() -> " + pkgName);
                return true;
            }
        }
        return false;
    }
}
