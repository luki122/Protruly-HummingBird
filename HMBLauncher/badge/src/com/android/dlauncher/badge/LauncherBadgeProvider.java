package com.android.dlauncher.badge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.launcher3.Launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by lijun on 17-7-27.
 */

public class LauncherBadgeProvider extends ContentProvider {
    public static final boolean DEBUG = true;
    private static final String TAG = "LauncherBadgeProvider";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "launcherbadge.db";


    private static final String DB_CREATE = "CREATE TABLE IF NOT EXISTS " + Badge.TABLE_NAME +
            " (" + Badge.ID + " integer primary key autoincrement, " +
            Badge.PACKAGE_NAME + " text not null, " +
            Badge.COUNT + " integer, " +
            Badge.APP_SHORTCUT_CREATOR + " integer, " +
            Badge.SHORTCUT_CUSTOM_ID + " text not null DEFAULT '', " +
            Badge.LAST_MODIFY + " TIMESTAMP not null DEFAULT (datetime('now', 'localtime')))";// DEFAULT 0 "DEFAULT (datetime('now', 'localtime')))"

    private DBHelper dbHelper = null;

    private WeakReference<BadgeChangedCallBack> mCallbacks;

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(BadgeChangedCallBack callbacks) {
        mCallbacks = new WeakReference<BadgeChangedCallBack>(callbacks);
        if (DEBUG) {
            Log.d(TAG, "initialize: callbacks = " + callbacks
                    + ", mCallbacks = " + mCallbacks);
        }
//
//        setBadge("com.android.settings", 1);
//        setBadge("com.android.calendar",2);
//        setBadge("com.android.deskclock",3);
//        setBadge("com.android.dialer",44);
//        setBadge("com.tencent.qqmusic",444);
    }

    //test for qq
    private boolean setUnreadNumberForQQ(Context context, int count) {
        String method = "setBadge";
        Bundle b = new Bundle();
        b.putInt("count", count);
        try {
            Uri uri = Uri.parse("content://com.android.dlauncher.badge/badge");
            Bundle bundle = context.getContentResolver().call(uri, method, null, b);
            if (bundle != null && bundle.getBoolean("result")) {
                Log.d("Badge", "setUnreadNumber true");
                return true;
            } else {
                Log.d("Badge", "setUnreadNumber false");
                return false;
            }
        } catch (Exception e) {
            Log.d("Badge", "setUnreadNumber exception : " + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    //test for wechat
    private boolean setUnreadNumberForWeChat(Context context, int count) {
        String method = "setAppBadgeCount";
        Bundle b = new Bundle();
        b.putStringArrayList("app_shortcut_custom_id", null);
        b.putInt("app_badge_count", 10);
        try {
            Uri uri = Uri.parse("content://com.android.dlauncher.badge/badge");
            Bundle bundle = context.getContentResolver().call(uri, method, null, b);
            if (bundle != null && bundle.getBoolean("result")) {
                Log.d("lijun22", "setUnreadNumberForWeChat true");
                return true;
            } else {
                Log.d("lijun22", "setUnreadNumberForWeChat false");
                return false;
            }
        }catch (Exception e){
            Log.d("lijun22", "setUnreadNumberForWeChat exception : " + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    public interface BadgeChangedCallBack {
        void bindComponentUnreadChanged(String packageName, int count, String shortcutCustomId);

        void bindWorkspaceUnreadInfo(ArrayList<BadgeInfo> unreadApps);
    }

    @Override
    public boolean onCreate() {
        if (DEBUG) {
            Log.d(TAG, "onCreate");
        }
        Context context = getContext();
        dbHelper = new DBHelper(context, DB_NAME, null, DB_VERSION);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        BadgeController.setLauncherBadgeProvider(this);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not supported with this drawable");
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not supported with this drawable");
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported with this drawable");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported with this drawable");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported with this drawable");
    }

    @Nullable
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle bundle = new Bundle();
        if ("setBadge".equals(method)) {
            int count = extras.getInt("count");
            if (extras == null) {
                Log.e(TAG, "call need count");
                bundle.putBoolean("result", false);
            } else {
                bundle.putBoolean("result", setBadge(count));
            }
        } else if ("setAppBadgeCount".equals(method)) {
            ArrayList<String> ids = extras.getStringArrayList("app_shortcut_custom_id");
            int badgeCount = extras.getInt("app_badge_count");
            if (!setAppBadgeCount(badgeCount, ids)) {
                return null;
            } else {
                bundle.putBoolean("result", true);
                return bundle;
            }
        } else if ("getShortcutList".equals(method)) {
            return getShortcutList(null);
        }
        return bundle;
    }

    /**
     * opened for third app (QQ)
     *
     * @param count
     * @return
     */
    public boolean setBadge(int count) {
        return setBadge(getCallingPackage(), count);
    }

    /**
     * opened for third app (WeChat)
     *
     * @param badgeCount
     * @param shortcutCustomIds
     * @return
     */
    public boolean setAppBadgeCount(int badgeCount, ArrayList<String> shortcutCustomIds) {
        return setBadge(getCallingPackage(), shortcutCustomIds, badgeCount);
    }

    /**
     * for wechat to get badges
     *
     * @param extras
     * @return
     */
    public Bundle getShortcutList(Bundle extras) {
        Bundle bundle = new Bundle();
        String packageName = getCallingPackage();

        ArrayList<String> shortcutCustomIds = new ArrayList<String>();//数据库中读取 need to
        if (mCallbacks != null && mCallbacks.get() != null) {
            Launcher launcher = (Launcher) mCallbacks.get();
            shortcutCustomIds = launcher.getModel().getCustomShortcutIds(launcher, packageName);
        }

        JSONArray list = new JSONArray();
        try {
            for (String sci : shortcutCustomIds) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("app_package_name", packageName);
                jsonObject.put("app_shortcut_custom_id", sci);
                int count = getBadge(packageName, sci);
                jsonObject.put("app_badge_count", count);
                list.put(jsonObject);
            }
        } catch (JSONException e) {
            Log.e(TAG, "getShortcutList JSONException :" + e.toString());
            e.printStackTrace();
        }
        bundle.putString("shortcut_list", list.toString());
        return bundle;
    }

    private boolean setBadge(String packageName, ArrayList<String> shortcutCustomIds, int badgeCount) {
        if (badgeCount < 0) {
            throw new IllegalArgumentException("setAppBadgeCount(): "
                    + "badgeCount must be equal or greater than 0");
        }
        if (shortcutCustomIds == null || shortcutCustomIds.size() == 0) {
            return setBadge(packageName, badgeCount);
        } else {
            for (String sci : shortcutCustomIds) {
                if (!setBadge(packageName, sci, badgeCount)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean setBadge(String packageName, String shortcutCustomId, int count) {
        Log.d(TAG, "setBadge packageName:" + packageName + ", shortcutCustomId = " + shortcutCustomId + ", count:" + count);

        if (shortcutCustomId == null || "".equals(shortcutCustomId)) {
            shortcutCustomId = getDefaultShortcutCustomId(packageName);
        }
        int currentCount = getBadge(packageName, shortcutCustomId);
        if (currentCount == count) return true;

        final ContentValues badgeValue = new ContentValues();
        badgeValue.put(Badge.PACKAGE_NAME, packageName);
        badgeValue.put(Badge.COUNT, count);
        badgeValue.put(Badge.SHORTCUT_CUSTOM_ID, shortcutCustomId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            db.delete(Badge.TABLE_NAME, Badge.PACKAGE_NAME + " = ? AND " + Badge.SHORTCUT_CUSTOM_ID + " = ?", new String[]{packageName, shortcutCustomId});
            db.insertOrThrow(Badge.TABLE_NAME, null, badgeValue);
        } catch (SQLException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            return false;
        } finally {

        }

        if (mCallbacks != null) {
            BadgeChangedCallBack callbacks = mCallbacks.get();
            if (callbacks != null) {
                callbacks.bindComponentUnreadChanged(packageName, count, shortcutCustomId);
            }
        }
        return true;
    }

    protected boolean setBadge(String packageName, int count) {
        return setBadge(packageName, getDefaultShortcutCustomId(packageName), count);
    }

    private int getBadge(String packageName) {
        return getBadge(packageName, getDefaultShortcutCustomId(packageName));
    }

    private int getBadge(String packageName, String shortcutCustomId) {
        if (shortcutCustomId == null) {
            shortcutCustomId = getDefaultShortcutCustomId(packageName);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int cc = 0;
        try {
            String query = "SELECT * FROM badge WHERE ( package_name = '" + packageName + "' and shortcutCustomId = '" + shortcutCustomId + "' );";
            Log.d(TAG, "test get query : " + query);
            cursor = db.rawQuery(query, null);
            if (cursor == null || cursor.getCount() <= 0) {
                Log.d(TAG, "test get null");
                return 0;
            } else {
                int srcCol = cursor.getColumnIndex(Badge.COUNT);
                cursor.moveToFirst();
                cc = cursor.getInt(srcCol);
            }
        } catch (Exception e) {
            Log.d(TAG, "getBadge Exception : " + e.toString());
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.e(TAG, "test get result : " + packageName + ", " + cc);
        return cc;
    }

    protected void reloadBadges() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int cc = 0;
        String pkg = null;
        ArrayList<BadgeInfo> badgeInfos = new ArrayList<BadgeInfo>();
        try {
            String query = "SELECT * FROM badge";
            Log.d(TAG, "reloadBadges query : " + query);
//            Log.d("lijun22", "reloadBadges query : " + query);
            cursor = db.rawQuery(query, null);
            if (cursor == null || cursor.getCount() <= 0) {
                Log.d("lijun22", "reloadBadges null");
                return;
            } else {
                if (mCallbacks == null || mCallbacks.get() == null) {
                    return;
                }
                BadgeChangedCallBack callbacks = mCallbacks.get();
                int idCol = cursor.getColumnIndex(Badge.ID);
                int countCol = cursor.getColumnIndex(Badge.COUNT);
                int pkgCol = cursor.getColumnIndex(Badge.PACKAGE_NAME);
                int shortcutIdCol = cursor.getColumnIndex(Badge.SHORTCUT_CUSTOM_ID);
                int lastModifyCol = cursor.getColumnIndex(Badge.LAST_MODIFY);
                int creatorCol = cursor.getColumnIndex(Badge.APP_SHORTCUT_CREATOR);
                while (cursor.moveToNext()) {
                    BadgeInfo badgeInfo = new BadgeInfo();
                    badgeInfo.id = cursor.getInt(idCol);
                    badgeInfo.badgeCount = cursor.getInt(countCol);
                    badgeInfo.pkgName = cursor.getString(pkgCol);
                    badgeInfo.shortcutCustomId = cursor.getString(shortcutIdCol);
                    badgeInfo.lastModifyTime = cursor.getString(lastModifyCol);
                    badgeInfo.creator = cursor.getInt(creatorCol);
                    badgeInfos.add(badgeInfo);
                    Log.d("lijun22", "reloadBadges : " + cc + ", " + pkg);
                }
                callbacks.bindWorkspaceUnreadInfo(badgeInfos);
            }
        } catch (Exception e) {
            Log.d("lijun22", "reloadBadges Exception : " + e.toString());
            return;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected ArrayList<BadgeInfo> getBadges() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        int cc = 0;
        String pkg = null;
        ArrayList<BadgeInfo> badgeInfos = new ArrayList<BadgeInfo>();
        try {
            String query = "SELECT * FROM badge";
            Log.d(TAG, "getBadges query : " + query);
            cursor = db.rawQuery(query, null);
            if (cursor == null || cursor.getCount() <= 0) {
                Log.d(TAG, "getBadges null");
                return null;
            } else {
                int idCol = cursor.getColumnIndex(Badge.ID);
                int countCol = cursor.getColumnIndex(Badge.COUNT);
                int pkgCol = cursor.getColumnIndex(Badge.PACKAGE_NAME);
                int shortcutIdCol = cursor.getColumnIndex(Badge.SHORTCUT_CUSTOM_ID);
                int lastModifyCol = cursor.getColumnIndex(Badge.LAST_MODIFY);
                int creatorCol = cursor.getColumnIndex(Badge.APP_SHORTCUT_CREATOR);
                while (cursor.moveToNext()) {
                    BadgeInfo badgeInfo = new BadgeInfo();
                    badgeInfo.id = cursor.getInt(idCol);
                    badgeInfo.badgeCount = cursor.getInt(countCol);
                    badgeInfo.pkgName = cursor.getString(pkgCol);
                    badgeInfo.shortcutCustomId = cursor.getString(shortcutIdCol);
                    badgeInfo.lastModifyTime = cursor.getString(lastModifyCol);
                    badgeInfo.creator = cursor.getInt(creatorCol);
                    badgeInfos.add(badgeInfo);
                    Log.d(TAG, "getBadges : " + cc + ", " + pkg);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getBadge Exception : " + e.toString());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return badgeInfos;
    }

    protected boolean removeBadge(String packageName) {
        Log.d(TAG, "removeBadge packageName:" + packageName);

        final int currentCount = getBadge(packageName);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            db.delete(Badge.TABLE_NAME, Badge.PACKAGE_NAME + " = ?", new String[]{packageName});
        } catch (SQLException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            return false;
        } finally {

        }

        if (currentCount > 0 && mCallbacks != null) {
            BadgeChangedCallBack callbacks = mCallbacks.get();
            if (callbacks != null) {
                callbacks.bindComponentUnreadChanged(packageName, 0, null);
            }
        }
        return true;
    }

    protected boolean removeBadge(String[] packageNames) {
        for(String pkg : packageNames){
            if(!removeBadge(pkg)){
                return false;
            }
        }
        return true;
    }

    protected void clearBadges() {
        dbHelper.clearDB(dbHelper.getReadableDatabase());
    }

    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            Log.i(TAG, "DBHelper ..." + name);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (DEBUG) {
                Log.i(TAG, "DBHelper Create");
            }
            db.execSQL(DB_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + Badge.TABLE_NAME);
            onCreate(db);
        }

        private void clearDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + Badge.TABLE_NAME);
            onCreate(db);
        }
    }

    private String getDefaultShortcutCustomId(String pkg) {
        return Badge.DEFAULT_SHORTCUT_CUSTOM_ID;
    }

}
