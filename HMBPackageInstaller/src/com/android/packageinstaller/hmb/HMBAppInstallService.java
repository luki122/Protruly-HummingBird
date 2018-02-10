package com.android.packageinstaller.hmb;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.IPackageInstallObserver;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import android.content.pm.PackageParser;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by xiaobin on 17-7-20.
 */

public class HMBAppInstallService extends Service {

    private static final String TAG = "HMBAppInstallService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return mBinder;
    }

    private final IHMBAppInstallService.Stub mBinder = new IHMBAppInstallService.Stub() {

        @Override
        public int installApp(String packageName, String apkFilePath, IPackageInstallObserver observer) throws RemoteException {
            File apkFile = new File(apkFilePath);
            if (apkFile == null) {
                return -1;
            }

            mInstallApp(HMBAppInstallService.this, packageName, apkFile, observer);

            return 0;
        }

    };

    public static int mInstallApp(Context context, String packageName, File apkFile,
                                  IPackageInstallObserver observer) {
        PackageManager pm = context.getPackageManager();
        if (TextUtils.isEmpty(packageName)) {
            PackageParser.Package parsed = getPackageInfo(apkFile);
            packageName = parsed.packageName;
        }
        int result = 0;
        int installFlags = 0;
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            if (pi != null) {
                installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
                result = 1;
            }
        } catch (NameNotFoundException e) {
            // e.printStackTrace();
        }

        Uri mPackageURI = Uri.fromFile(apkFile);
        pm.installPackage(mPackageURI, observer, installFlags, null);
        return result;
    }

    private static PackageParser.Package getPackageInfo(File sourceFile) {

        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        Object pkg = null;
        final String archiveFilePath = sourceFile.getAbsolutePath();
        try {
            Class<?> clazz = Class.forName("android.content.pm.PackageParser");
            Object instance = getParserObject(archiveFilePath);
            if (Build.VERSION.SDK_INT >= 21) {
                Method method = clazz.getMethod("parsePackage", File.class, int.class);
                pkg = method.invoke(instance, sourceFile, 0);
            } else {
                Method method = clazz.getMethod("parsePackage", File.class, String.class, DisplayMetrics.class,
                        int.class);
                pkg = method.invoke(instance, sourceFile, archiveFilePath, metrics, 0);
            }
            instance = null;
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return (PackageParser.Package) pkg;
    }

    private static Object getParserObject(String archiveFilePath) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        Class<?> clazz = Class.forName("android.content.pm.PackageParser");
        return Build.VERSION.SDK_INT >= 21 ? clazz.getConstructor().newInstance()
                : clazz.getConstructor(String.class).newInstance(archiveFilePath);
    }

}
