package com.protruly.powermanager.purebackground.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.protruly.powermanager.purebackground.Info.PBArrayList;
import com.protruly.powermanager.utils.LogUtils;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Provider for accessing auto clean app list.
 */
public class AutoCleanAppProvider extends BaseContentProvider {
    private static final String TAG = AutoCleanAppProvider.class.getSimpleName();

    private static final String URL_STR
            = "content://com.protruly.powermanager.purebackground.provider.AutoCleanAppProvider";
    private static final Uri CONTENT_URI = Uri.parse(URL_STR);

    private final static HashSet<String> mAutoCleanAppList = new HashSet<>();

    /**
     * Get all auto clean app list.
     * @param context
     * @return
     */
    public static HashSet<String> getAutoCleanAppList(Context context) {
        PBArrayList<String> autoCleanAppList = queryAllAppData(context);
        mAutoCleanAppList.clear();
        for (String app : autoCleanAppList.getDataList()) {
            mAutoCleanAppList.add(app);
        }
        return mAutoCleanAppList;
    }

    public static ArrayList<String> getAutoCleanAppList2(Context context) {
        PBArrayList<String> autoCleanAppList = queryAllAppData(context);
        ArrayList<String> apps = new ArrayList<>();
        for (String app : autoCleanAppList.getDataList()) {
            apps.add(app);
        }
        return apps;
    }

    /**
     * Add app to auto clean app list.
     * @param context
     * @param pkgName
     */
    public static void addAutoCleanApp(Context context, String pkgName) {
        insertOrUpdateData(context, pkgName);
    }

    /**
     * Remove app from auto clean app list.
     * @param context
     * @param pkgName
     */
    public static void removeAutoCleanApp(Context context, String pkgName) {
        deleteData(context, pkgName);
    }

    /**
     * Whether app is in auto clean app list.
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isInAutoCleanAppList(Context context, String pkgName) {
        return isInDB(context, getQueryWhere(), getQueryValue(pkgName), CONTENT_URI);
    }

    private static void insertOrUpdateData(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(DbHelper.PACKAGE_NAME, pkgName);

        if (isInDB(context, getQueryWhere(), getQueryValue(pkgName), CONTENT_URI)) {
            //do nothing
            LogUtils.d(TAG, "insertOrUpdateData() -> pkgName " + pkgName + " is In DB");
        } else {
            LogUtils.d(TAG, "insertOrUpdateData() -> pkgName = " + pkgName);
            context.getContentResolver().insert(CONTENT_URI, values);
        }
    }

    private static void deleteData(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            return;
        }
        LogUtils.d(TAG, "deleteData() -> pkgName = " + pkgName);
        context.getContentResolver()
                .delete(CONTENT_URI, getQueryWhere(), getQueryValue(pkgName));
    }

    private static PBArrayList<String> queryAllAppData(Context context) {
        PBArrayList<String> appInfoList = new PBArrayList<String>();

        if (context == null) {
            return appInfoList;
        }

        String[] columns = {DbHelper.PACKAGE_NAME};

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CONTENT_URI, columns, null, null, null);
        } catch (Exception e) {
            //do nothing
        }

        synchronized (CONTENT_URI) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(
                            cursor.getColumnIndexOrThrow(DbHelper.PACKAGE_NAME));
                    appInfoList.add(pkgName);
                }
                cursor.close();
            }
        }
        return appInfoList;
    }

    private static String getQueryWhere() {
        return DbHelper.PACKAGE_NAME + " = ?";
    }

    private static String[] getQueryValue(String packageName) {
        return new String[]{packageName};
    }

    @Override
    public Uri getContentUri() {
        return CONTENT_URI;
    }

    @Override
    public String getTableName() {
        return DbHelper.TABLE_AutoCleanApp;
    }
}