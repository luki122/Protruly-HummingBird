package com.protruly.powermanager.purebackground.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.utils.LogUtils;

/**
 * Monitor Package Change.
 *
 * 1.卸载一个应用，对应的广播：
 *   Intent.ACTION_PACKAGE_REMOVED （注：在系统中找不到该应的ApplicationInfo）
 *   Intent.ACTION_PACKAGE_DATA_CLEARED
 *
 * 2.安装一个应用，对应的广播：
 *   Intent.ACTION_PACKAGE_ADDED
 *
 * 3.覆盖安装一个应用：
 *   Intent.ACTION_PACKAGE_REMOVED （注：在系统中找到该应的ApplicationInfo）
 *   Intent.ACTION_PACKAGE_ADDED
 *   Intent.ACTION_PACKAGE_REPLACED
 *
 * 4.清除一个应用的用户数据：
 *   Intent.ACTION_PACKAGE_RESTARTED
 *   Intent.ACTION_PACKAGE_DATA_CLEARED
 *
 * 5.一个应用被强制停止：
 *  Intent.ACTION_PACKAGE_RESTARTED
 */
public class PackageChangeReceiver extends BroadcastReceiver {
    private static final String TAG = PackageChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && intent.getData() != null) {
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            LogUtils.d(TAG, "onReceive() -> action = " + intent.getAction()
                    + ", pkgName = " + intent.getData().getSchemeSpecificPart()
                    + ", replacing = " + replacing);
            if (!replacing) {
                dealFunc(context, intent.getData().getSchemeSpecificPart(), intent.getAction());
            }
        }
    }

    private void dealFunc(Context context, String packageName, String action) {
        LogUtils.d(TAG, "dealFunc() -> action = " + action + ", pkgName = " + packageName);
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)){
            installOrCoverApk(context, packageName);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            unInstallApk(context, packageName);
        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {

        }
    }

    private void installOrCoverApk(final Context context, final String packageName){
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ConfigModel.getInstance(context).getAppInfoModel().installOrCoverPackage(packageName);
            }
        };
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);//必须要延时
                } catch (Exception e){
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        }.start();
    }

    private void unInstallApk(Context context,String packageName){
        ConfigModel.getInstance(context).getAppInfoModel().UninstallPackage(packageName);
    }
}