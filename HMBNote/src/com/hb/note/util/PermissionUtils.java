package com.hb.note.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    /**
     * 检查整个程序默认会需要的权限
     * @param activity
     * @param permissions
     * @return
     */
    public static boolean checkAppDefaultPermissions(Activity activity) {
        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        return checkPermissions(activity, permissions);
    }

    /**
     * 检查权限
     * @param activity
     * @param permissions
     * @return
     */
    public static boolean checkPermissions(Activity activity, String[] permissions) {
        boolean result = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * 请求整个程序默认会需要的权限
     * @param activity
     * @param requestCode
     */
    public static void requestAppDefaultPermissions(Activity activity, int requestCode) {
        String[] permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(activity, permissions, requestCode);
    }

    /**
     * 请求权限
     * @param activity
     * @param permissions
     * @param requestCode
     */
    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissions == null) {
            return;
        }

        List<String> needRequestPermissionList = new ArrayList<String>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                needRequestPermissionList.add(permission);
            }
        }

        if (needRequestPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(activity,
                    needRequestPermissionList.toArray(new String[needRequestPermissionList.size()]),
                    requestCode);
        }
    }

}
