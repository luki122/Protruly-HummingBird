package com.protruly.powermanager.purebackground.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AutoStartInfo;
import com.protruly.powermanager.purebackground.Info.AutoStartRecordInfo;
import com.protruly.powermanager.purebackground.activity.AutoStartMgrActivity;
import com.protruly.powermanager.purebackground.provider.AutoStartAppProvider;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AutoStart Model.
 */
public class AutoStartModel {
    private static final String TAG = AutoStartModel.class.getSimpleName();

    private Context context;
    private static AutoStartModel instance;

    private HashMap<String, String> mAppsShutDownMap;
    private HashMap<String, AutoStartInfo> mAppsAutoStartMap;
    private HashMap<String, AutoStartInfo> mBackupAppsAutoStartMap;
    private HashMap<String, AutoStartInfo> mNoDealAutoStartMap;

    private AutoStartMgrActivity mgrActivity = null;

    private MyHandler handler;
    private ActivityManager activityManager;

    private List<InputMethodInfo> inputMethodAppList;

    private HashMap<String, AutoStartRecordInfo> recordMap;

    private AtomicBoolean isDuringGetInfo = new AtomicBoolean(false);

    private final String[] Just_ACTION_BOOT_COMPLETED = {
            Intent.ACTION_BOOT_COMPLETED
    };

    private final String[] NeedDealActions = {
            Intent.ACTION_BOOT_COMPLETED //必须
            , Intent.ACTION_TIME_CHANGED //必须
            , Intent.ACTION_BATTERY_CHANGED//必须
            , Intent.ACTION_USER_PRESENT //必须
            , Intent.ACTION_MEDIA_MOUNTED //必须
            , "android.net.conn.CONNECTIVITY_CHANGE"//必须
            , "android.intent.action.SERVICE_STATE"
            , "android.net.wifi.WIFI_STATE_CHANGED"
            , Intent.ACTION_POWER_CONNECTED
            , Intent.ACTION_POWER_DISCONNECTED
            , "android.provider.Telephony.SMS_RECEIVED"
            , "android.intent.action.PHONE_STATE"
            , Intent.ACTION_NEW_OUTGOING_CALL
            , Intent.ACTION_PACKAGE_ADDED
            , Intent.ACTION_PACKAGE_REMOVED
    };

    private final String[] NoNeedDealActions = {
            "android.appwidget.action.APPWIDGET_UPDATE"
    };

    public static synchronized AutoStartModel getInstance() {
        return instance;
    }

    public static synchronized AutoStartModel getInstance(Context context) {
        if (instance == null) {
            instance = new AutoStartModel(context);
        }
        return instance;
    }

    private AutoStartModel(Context context) {
        this.context = context.getApplicationContext();

        mAppsAutoStartMap = new HashMap<String, AutoStartInfo>();
        mNoDealAutoStartMap = new HashMap<String, AutoStartInfo>();
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        handler = new MyHandler(Looper.getMainLooper());
        readCacheStr(context);
    }

    public boolean isAllAppOpened(Context context) {
        HashSet<String> autoStartAppList = AutoStartAppProvider.getAutoStartAppList(context);
        return autoStartAppList.size() == mAppsAutoStartMap.size();
    }

    /**
     * Get AutoStartInfo by packageName.
     *
     * @param packageName
     * @return
     */
    public AutoStartInfo getAutoStartInfo(String packageName) {
        if (mBackupAppsAutoStartMap == null) {
            return null;
        }
        return mBackupAppsAutoStartMap.get(packageName);
    }

    public void attachMgrActivity(AutoStartMgrActivity activity) {
        mgrActivity = activity;
    }

    /**
     * Try Change AutoStart State.
     *
     * @param pkgName
     * @param isOpen
     */
    public void tryChangeAutoStartState(String pkgName, boolean isOpen) {
        changeAutoStartStateInDB(pkgName, isOpen);
        boolean pkgIsInWhiteList = isInAutoStartWhiteList(pkgName);
        if (!pkgIsInWhiteList) {
            changeAutoStartState(pkgName, isOpen);
        } else if (isOpen) {
            changeAutoStartState(pkgName, isOpen);
        } else {
            //in other case we do nothing
            LogUtils.d(TAG, "tryChangeAutoStartState()-> can't set whitelist app to false, do nothing");
        }
    }

    public void changeAutoStartStateInDB(String packageName, boolean isOpen) {
        if (mAppsAutoStartMap == null || packageName == null) {
            return;
        }
        AutoStartInfo autoStartInfo = mAppsAutoStartMap.get(packageName);
        if (autoStartInfo == null) {
            return;
        }
        if (isOpen) {
            AutoStartAppProvider.addAllowAutoStartApp(context, packageName);
        } else {
            AutoStartAppProvider.removeAllowAutoStartApp(context, packageName);
        }
    }

    public void changeAutoStartState(String packageName, boolean isOpen) {
        if (mAppsAutoStartMap == null || packageName == null) {
            return;
        }
        AutoStartInfo autoStartInfo = mAppsAutoStartMap.get(packageName);
        if (autoStartInfo == null) {
            return;
        }
        if (isOpen) {
            ApkUtils.openApkAutoStart(context, autoStartInfo, packageName);
        } else {
            ApkUtils.closeApkAutoStart(context, autoStartInfo, packageName);
        }
        changeRecord(packageName, isOpen);
    }

    /**
     * [AutoStart Module Entry]
     * This function is called when the application is launched
     * and all application data is load completed.
     */
    public void applicationStart() {
        dealFunc(false, null);
    }

    public void inStallApp(AppInfo appInfo) {
        if (appInfo == null) {
            Log.e(TAG, "installApp() -> appInfo is null");
            return;
        }
        dealFunc(false, appInfo.getPackageName());
    }


    public void coverInStallApp(AppInfo appInfo) {
        dealFunc(false, null);
    }

    public void unInStallApp(String pkgName) {
        if (pkgName == null) {
            return;
        }

        if (mAppsAutoStartMap != null && mAppsAutoStartMap.containsKey(pkgName)) {
            synchronized (mAppsAutoStartMap) {
                mAppsAutoStartMap.remove(pkgName);
            }
        }

        if (recordMap != null && recordMap.containsKey(pkgName)) {
            synchronized (recordMap) {
                recordMap.remove(pkgName);
            }
        }

        AutoStartAppProvider.removeAllowAutoStartApp(context, pkgName);
        LogUtils.d(TAG, "unInStallApp() -> pkgName = " + pkgName);
    }

    public void externalAppAvailable(List<String> pkgList) {
        dealFunc(false, null);
    }

    public void externalAppUnAvailable(List<String> pkgList) {
        if (mAppsAutoStartMap == null || pkgList == null) {
            return;
        }

        for (int i = 0; i < pkgList.size(); i++) {
            synchronized (mAppsAutoStartMap) {
                if (mAppsAutoStartMap.containsKey(pkgList.get(i))) {
                    mAppsAutoStartMap.remove(pkgList.get(i));
                }
            }
        }
    }

    /**
     * 关机时调用,根据自启动配置表，重置个应用广播开启的状态
     */
    public void shutDown() {
        dealFunc(true, null);
    }

    /**
     * Initialization operation.
     *
     * @param isShutDown
     * @param pkgNameOfNewApp
     */
    private void dealFunc(final boolean isShutDown, final String pkgNameOfNewApp) {
        if (isDuringGetInfo.get()) {
            return;
        }
        isDuringGetInfo.set(true);

        if (mAppsAutoStartMap == null) {
            mAppsAutoStartMap = new HashMap<String, AutoStartInfo>();
        } else {
            synchronized (mAppsAutoStartMap) {
                mAppsAutoStartMap.clear();
            }
        }
        mBackupAppsAutoStartMap = (HashMap<String, AutoStartInfo>) mAppsAutoStartMap.clone();

        if (mNoDealAutoStartMap == null) {
            mNoDealAutoStartMap = new HashMap<String, AutoStartInfo>();
        } else {
            synchronized (mNoDealAutoStartMap) {
                mNoDealAutoStartMap.clear();
            }
        }

        LogUtils.d(TAG, "dealFunc() begin >>>>> isShutDown = " + isShutDown
                + ", pkgNameOfNewApp = " + pkgNameOfNewApp);
        new Thread() {
            @Override
            public void run() {
                if (isShutDown) {
                    resetShutDownReceiveResolveInfoList(context);
                }
                // 1.根据NoNeedDealActions收集不需处理的noDealAutoStartMap
                resetNoDealAutoStartResolveList(context);
                // 2.根据NeedDealActions及noDealAutoStartMap收集需要处理的appsAutoStartMap
                resetbootReceiveResolveInfoList(context);
                // 3.从appsAutoStartMap中移除系统应用
                removeSysApp(context);
                // 4.根据appsAutoStartMap初始化每个三方应用自启动状态
                checkAllApkAutoStart(isShutDown);
                AutoStartAppProvider.initProvider(context);

                if (pkgNameOfNewApp != null) {
                    synchronized (mAppsAutoStartMap) {
                        if (mAppsAutoStartMap.get(pkgNameOfNewApp) != null) {
                            boolean newAppIsOpen = mAppsAutoStartMap.get(pkgNameOfNewApp).getIsOpen();
                            LogUtils.d(TAG, "dealFunc() -> pkgNameOfNewApp = " + pkgNameOfNewApp
                                    + ", newAppIsOpen = " + newAppIsOpen);
                            if (newAppIsOpen) {
                                AutoStartAppProvider.addAllowAutoStartApp(context, pkgNameOfNewApp);
                            } else {
                                LogUtils.d(TAG, "dealFunc() -> New app is not auto-startable & is in autostartmap");
                            }
                        } else {
                            LogUtils.d(TAG, "dealFunc() -> New app is not auto-startable & not in autostartmap");
                        }
                    }
                }

                LogUtils.d(TAG, "dealFunc() end >>>>>");
                if (!isShutDown) {
                    handler.sendEmptyMessage(0);
                }
                isDuringGetInfo.set(false);
            }
        }.start();
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //TODO
//            PersistentModel.getInstance().dealPersistentApp(context);
            mBackupAppsAutoStartMap = (HashMap<String, AutoStartInfo>) mAppsAutoStartMap.clone();
            if (mgrActivity != null) {
                mgrActivity.updateOfInStall();
            }
        }
    }

    /**
     * 重置所有可以接受关机事件的应用列表
     *
     * @param context
     */
    private void resetShutDownReceiveResolveInfoList(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SHUTDOWN);
        List<ResolveInfo> resolveInfoList = null;
        try {
            resolveInfoList = context.getPackageManager()
                    .queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetAppsShutDownMap();
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (resolveInfo == null ||
                        resolveInfo.activityInfo == null ||
                        resolveInfo.activityInfo.packageName == null) {
                    continue;
                }
                mAppsShutDownMap.put(resolveInfo.activityInfo.packageName, "test");
            }
        }
    }

    /**
     * 重置可以接收关机事件广播的应用
     */
    private void resetAppsShutDownMap() {
        if (mAppsShutDownMap == null) {
            mAppsShutDownMap = new HashMap<String, String>();
        } else {
            mAppsShutDownMap.clear();
        }
        /**
         * 多米音乐虽然没有接收关机事件的广播，但是它会一直去改变自身广播的接收状态，
         * 所以在关机重置广播接收状态时，也需要结束掉多米音乐
         */
        mAppsShutDownMap.put("com.duomi.android", "test");

    }

    /**
     * Reset appsAutoStartMap.
     *
     * @param context
     */
    private void resetbootReceiveResolveInfoList(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = null;
        List<ResolveInfo> resolveInfoList = null;
        String[] actions;
        if (Config.isAutoStartControlReceive) {
            actions = NeedDealActions;
        } else {
            actions = Just_ACTION_BOOT_COMPLETED;
        }
        for (String action : actions) {
            intent = new Intent(action);
            try {
                resolveInfoList = context.getPackageManager()
                        .queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dealResolveInfoList(resolveInfoList, action);
        }
    }

    private void dealResolveInfoList(List<ResolveInfo> resolveInfoList, String action) {
        if (resolveInfoList == null) {
            return;
        }
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo == null ||
                    resolveInfo.activityInfo == null ||
                    resolveInfo.activityInfo.packageName == null) {
                continue;
            }

            if (!checkNeedDeal(resolveInfo)) {
                continue;
            }

            AutoStartInfo autoStartInfo = mAppsAutoStartMap.get(resolveInfo.activityInfo.packageName);
            if (autoStartInfo == null) {
                autoStartInfo = new AutoStartInfo();
                autoStartInfo.AddResolveInfo(resolveInfo);
                synchronized (mAppsAutoStartMap) {
                    mAppsAutoStartMap.put(resolveInfo.activityInfo.packageName, autoStartInfo);
                }
            } else {
                autoStartInfo.AddResolveInfo(resolveInfo);
            }
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                autoStartInfo.flags |= AutoStartInfo.FLAG_BOOT_AUTO_START;
            } else {
                autoStartInfo.flags |= AutoStartInfo.FLAG_BACKGROUND_AUTO_START;
            }
        }
    }

    /**
     * Reset noDealAutoStartMap.
     */
    private void resetNoDealAutoStartResolveList(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = null;
        List<ResolveInfo> resolveInfoList = null;
        if (Config.isAutoStartControlReceive) {
            for (String action : NoNeedDealActions) {
                intent = new Intent(action);
                try {
                    resolveInfoList = context.getPackageManager()
                            .queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dealResolveListForNoDealAutoStart(resolveInfoList);
            }
        }
    }

    private void dealResolveListForNoDealAutoStart(List<ResolveInfo> resolveInfoList) {
        if (resolveInfoList == null) {
            return;
        }
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo == null ||
                    resolveInfo.activityInfo == null ||
                    resolveInfo.activityInfo.packageName == null ||
                    resolveInfo.activityInfo.name == null) {
                continue;
            }
            AutoStartInfo autoStartInfo = mNoDealAutoStartMap.get(resolveInfo.activityInfo.packageName);
            if (autoStartInfo == null) {
                autoStartInfo = new AutoStartInfo();
                autoStartInfo.AddResolveInfo(resolveInfo);
                synchronized (mNoDealAutoStartMap) {
                    mNoDealAutoStartMap.put(resolveInfo.activityInfo.packageName, autoStartInfo);
                }
            } else {
                autoStartInfo.AddResolveInfo(resolveInfo);
            }
        }
    }

    /**
     * 从appsAutoStartMap中移除系统应用
     */
    private void removeSysApp(Context context) {
        if (mAppsAutoStartMap == null) {
            return;
        }
        List<String> pkgList = new ArrayList<String>();
        synchronized (mAppsAutoStartMap) {
            Set<String> packageNames = mAppsAutoStartMap.keySet();
            for (String packageName : packageNames) {
                pkgList.add(packageName);
            }
        }

        for (int i = 0; i < pkgList.size(); i++) {
            if (ApkUtils.isUserApp(ApkUtils.getApplicationInfo(context, pkgList.get(i)))) {
                continue;
            }
            synchronized (mAppsAutoStartMap) {
                mAppsAutoStartMap.remove(pkgList.get(i));
            }
        }
    }

    /**
     * 根据noDealAutoStartMap判断该ResolveInfo是否需要做自启动的处理.
     *
     * @param checkResolveInfo
     * @return true:需要做处理　false:不需要做处理
     */
    private boolean checkNeedDeal(ResolveInfo checkResolveInfo) {
        String checkName;
        boolean isNeedDeal = true;
        if (mNoDealAutoStartMap == null || (checkName = checkResolveInfo.activityInfo.name) == null) {
            return isNeedDeal;
        }
        AutoStartInfo noDealAutoStartInfo = mNoDealAutoStartMap.get(checkResolveInfo.activityInfo.packageName);
        if (noDealAutoStartInfo == null ||
                noDealAutoStartInfo.getResolveInfoList() == null) {
            return isNeedDeal;
        }
        List<ResolveInfo> noDealResolveList = noDealAutoStartInfo.getResolveInfoList();

        for (int i = 0; i < noDealResolveList.size(); i++) {
            String noDealClassName = noDealResolveList.get(i).activityInfo.name;
            if (checkName.equals(noDealClassName)) {
                isNeedDeal = false;
                LogUtils.d(TAG, "checkNeedDeal() -> [NO_DEAL] pkg = "
                        + checkResolveInfo.activityInfo.packageName
                        + ", className = " + checkResolveInfo.activityInfo.name);
                break;
            }
        }
        return isNeedDeal;
    }

    /**
     * 检测所有三方应用的自启动广播是否开启
     */
    private void checkAllApkAutoStart(boolean isShutDown) {
        if (mAppsAutoStartMap == null) {
            return;
        }

        List<String> pkgList = new ArrayList<String>();
        /**
         * Notice：同步块的区域一定要小，最好不要包含自己定义的方法，
         * 因为有可能同步块内自己写的方法中也加在同样的同步锁，这样就会出现死锁的情况，
         * 所以下面的代码就做了这样的处理，把checkOneApkAutoStart（）和initOneApkAutoStart（）方法
         * 放到同步块外面去了。
         */
        synchronized (mAppsAutoStartMap) {
            Set<String> packageNames = mAppsAutoStartMap.keySet();
            for (String packageName : packageNames) {
                pkgList.add(packageName);
            }
        }
        boolean hasAutoStart = false;
        for (int i = 0; i < pkgList.size(); i++) {
            String pkgName = pkgList.get(i);
            LogUtils.d(TAG, "checkAllApkAutoStart() -> [CHECK] -----> " + pkgName);
            checkOneApkAutoStart(mAppsAutoStartMap.get(pkgName), pkgName);
            hasAutoStart = getOneApkHasAutoStart(mAppsAutoStartMap.get(pkgName), pkgName);
            AutoStartInfo info = mAppsAutoStartMap.get(pkgName);
            if (null == info) {
                return;
            }
            mAppsAutoStartMap.get(pkgName).setHasAutoStart(hasAutoStart);
            initOneApkAutoStart(isShutDown, pkgName, mAppsAutoStartMap.get(pkgName), context);

            HashSet<String> autostartApps = AutoStartAppProvider.getAutoStartAppList(context);
            mAppsAutoStartMap.get(pkgName).setAutoStartOfUser(autostartApps.contains(pkgName));
        }
    }

    public HashMap<String, Boolean> getListHasAutoStart() {
        HashMap<String, Boolean> listHasAutoStart = new HashMap<String, Boolean>();
        if (mAppsAutoStartMap == null) {
            return listHasAutoStart;
        }

        List<String> pkgList = new ArrayList<String>();
        /**
         * 重点：同步块的区域一定要小，最好不要包含自己定义的方法，
         * 因为有可能同步块内自己写的方法中也加在同样的同步锁，这样就会出现死锁的情况，
         * 所以下面的代码就做了这样的处理，把checkOneApkAutoStart（）和initOneApkAutoStart（）方法
         * 放到同步块外面去了。
         */
        synchronized (mAppsAutoStartMap) {
            Set<String> packageNames = mAppsAutoStartMap.keySet();
            for (String packageName : packageNames) {
                pkgList.add(packageName);
            }
        }

        boolean hasAutoStart = false;
        for (int i = 0; i < pkgList.size(); i++) {
            String pkgName = pkgList.get(i);
            hasAutoStart = getOneApkHasAutoStart(mAppsAutoStartMap.get(pkgName), pkgName);
            listHasAutoStart.put(pkgName, hasAutoStart);
        }
        return listHasAutoStart;
    }

    /**
     * 检测某个应用的自启动广播是否开启
     *
     * @param autoStartInfo
     */
    private synchronized void checkOneApkAutoStart(AutoStartInfo autoStartInfo, String pkgName) {
        if (autoStartInfo == null) {
            return;
        }

        List<ResolveInfo> resolveInfoList = autoStartInfo.getResolveInfoList();
        for (int i = 0; i < resolveInfoList.size(); i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            if (resolveInfo == null) {
                continue;
            }
            if (isReceiveOpen(resolveInfo, context)) {
                autoStartInfo.setIsOpen(true);
                LogUtils.d(TAG, "checkOneApkAutoStart() -> " + pkgName + " AutoStart is Opened!");
                return;
            }
        }
        LogUtils.d(TAG, "checkOneApkAutoStart() -> " + pkgName + " AutoStart is Closed!");
        autoStartInfo.setIsOpen(false);
    }

    /**
     *
     * @param autoStartInfo
     * @param pkgName
     * @return
     */
    private synchronized boolean getOneApkHasAutoStart(AutoStartInfo autoStartInfo, String pkgName) {
        if (autoStartInfo == null) {
            return false;
        }

        List<ResolveInfo> resolveInfoList = autoStartInfo.getResolveInfoList();
        for (int i = 0; i < resolveInfoList.size(); i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);
            if (resolveInfo == null) {
                continue;
            }
            if (isReceiveOpen(resolveInfo, context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化某个应用的自启动广播是否开启
     *
     * @param autoStartInfo
     */
    private synchronized void initOneApkAutoStart(boolean isShutDown, String packageName,
                                                  AutoStartInfo autoStartInfo, Context context) {
        if (autoStartInfo == null || packageName == null) {
            return;
        }
        boolean isNeedOpen = isNeedOpenAutoStart(packageName);
        LogUtils.d(TAG, "initOneApkAutoStart() -> packageName = " + packageName + ", isNeedOpen = " + isNeedOpen);
        if (isNeedOpen) {
            ApkUtils.openApkAutoStart(context, autoStartInfo, packageName);
        } else {
            if (isShutDown) {
                if (!isInputMethodApp(packageName)) {
                    activityManager.forceStopPackage(packageName);
                }
                ApkUtils.closeApkAutoStart(context, autoStartInfo, packageName);
            } else {
                ApplicationInfo applicationInfo = ApkUtils.getApplicationInfo(context, packageName);
                if (applicationInfo != null
                        && (applicationInfo.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
                    autoStartInfo.setIsOpen(false);//如果该应用正在运行，则仅仅修改标志量，而不去关闭广播
                } else {
                    ApkUtils.closeApkAutoStart(context, autoStartInfo, packageName);
                }
            }
        }
    }

    /**
     * 判断当前应用是不是输入法,如果是输入法，则在关机的时候不能停止运行；
     * 如果把输入法停止运行，那么输入法的默认值会改变。
     *
     * @param pkgName
     * @return
     */
    private boolean isInputMethodApp(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        if (inputMethodAppList == null) {
             inputMethodAppList = ApkUtils.getEnabledInputMethodList(context);
        }
        int size = inputMethodAppList == null ? 0 : inputMethodAppList.size();
        for (int i = 0; i < size; i++) {
            InputMethodInfo tmpItem = (InputMethodInfo) inputMethodAppList.get(i);
            if (tmpItem == null) {
                continue;
            }
            if (pkgName.equals(tmpItem.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前软件的开机启动是否打开（说明：这个软件必须有开机动的广播）
     * @param resolveInfo
     * @param context
     * @return
     */
    private boolean isReceiveOpen(ResolveInfo resolveInfo, Context context) {
        if (resolveInfo == null || context == null) {
            return false;
        }

        ComponentName tmpComponent = new ComponentName(resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name);
        try {
            return ApkUtils.getComponentEnabledState(context, tmpComponent);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInAutoStartWhiteList(String packageName) {
        for (int i = 0; i < Config.autoStartWhiteList.length; i++) {
            if (packageName != null && packageName.toLowerCase(Locale.US)
                            .contains(Config.autoStartWhiteList[i].toLowerCase(Locale.US))) {
                LogUtils.d(TAG, "isInAutoStartWhiteList() -> " + packageName + " In WhiteList");
                return true;
            }
        }
        return false;
    }

    public static void releaseObject() {
        if (instance != null) {
            if (Config.SET_NULL_OF_CONTEXT) {
                instance.context = null;
            }
            if (instance.mAppsAutoStartMap != null) {
                synchronized (instance.mAppsAutoStartMap) {
                    instance.mAppsAutoStartMap.clear();
                }
            }

            if (instance.mAppsShutDownMap != null) {
                instance.mAppsShutDownMap.clear();
            }

            instance = null;
        }
    }

    /**
     * 判断这个应用是否应该打开自启动
     *
     * @param packageName
     * @return
     */
    private boolean isNeedOpenAutoStart(String packageName) {
        if (packageName == null) {
            return false;
        }
        AutoStartRecordInfo recordData = null;
        if (recordMap != null) {
            recordData = recordMap.get(packageName);
        }
        if (recordData != null) {
            return recordData.getIsOpen();
        } else if (isInAutoStartWhiteList(packageName)) {
            return true;
        } else {
            return false;
        }
    }

    private void changeRecord(String packageName, boolean isOpen) {
        if (packageName == null) {
            return;
        }

        if (recordMap == null) {
            recordMap = new HashMap<String, AutoStartRecordInfo>();
        }

        AutoStartRecordInfo recordData = recordMap.get(packageName);
        if (recordData == null) {
            recordData = new AutoStartRecordInfo();
            recordData.setPackageName(packageName);
            recordData.setIsOpen(isOpen);
            synchronized (recordMap) {
                recordMap.put(packageName, recordData);
            }
        } else {
            recordData.setIsOpen(isOpen);
        }
        saveCacheStr(context);
    }

    private boolean readCacheStr(Context context) {
        boolean result = true;
        String str = null;
        synchronized (Config.cache_file_name_of_autoStart) {
            str = FileModel.getInstance(context).readFile(Config.cache_file_name_of_autoStart);
            LogUtils.d(TAG, "readCacheStr() -> str = " + str);
        }
        if (StringUtils.isEmpty(str)) {
            return false;
        }

        try {
            parseItem(str);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private void parseItem(String str) throws Exception {
        if (recordMap == null) {
            recordMap = new HashMap<String, AutoStartRecordInfo>();
        } else {
            synchronized (recordMap) {
                recordMap.clear();
            }
        }

        JSONObject json = JSON.parseObject(str);
        if (json != null && !json.isEmpty()) {
            JSONArray list = json.getJSONArray("list");
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    if (!item.isEmpty()) {
                        AutoStartRecordInfo recordData = new AutoStartRecordInfo();
                        if (recordData.parseJson(item)) {
                            synchronized (recordMap) {
                                recordMap.put(recordData.getPackageName(), recordData);
                            }
                        }
                    }
                }
            }
        }
    }

    private void saveCacheStr(Context context) {
        String needSaveStr = getNeedSaveStr();
        if (context == null || StringUtils.isEmpty(needSaveStr)) {
            return;
        }

        synchronized (Config.cache_file_name_of_autoStart) {
            LogUtils.d(TAG, "saveCacheStr() -> needSaveStr = " + needSaveStr);
            FileModel.getInstance(context).writeFile(Config.cache_file_name_of_autoStart, needSaveStr);
        }
    }

    private String getNeedSaveStr() {
        JSONObject json = new JSONObject();
        JSONArray jsonList = new JSONArray();

        if (recordMap != null && recordMap.size() > 0) {
            synchronized (recordMap) {
                Set<String> keySet = recordMap.keySet();
                for (String packageName : keySet) {
                    AutoStartRecordInfo flowData = recordMap.get(packageName);
                    jsonList.add(flowData.getJson());
                }
            }
            json.put("list", jsonList);
        }
        return json.toJSONString();
    }
}