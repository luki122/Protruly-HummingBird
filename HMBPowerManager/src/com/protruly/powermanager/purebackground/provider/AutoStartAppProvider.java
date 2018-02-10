package com.protruly.powermanager.purebackground.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AppsInfo;
import com.protruly.powermanager.purebackground.Info.AutoStartInfo;
import com.protruly.powermanager.purebackground.Info.PBArrayList;
import com.protruly.powermanager.purebackground.model.AutoStartModel;
import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.utils.LogUtils;

import java.util.HashSet;

/**
 * Provider for accessing allow auto start app list.
 */
public class AutoStartAppProvider extends BaseContentProvider {
	private static final String TAG = AutoStartAppProvider.class.getSimpleName();

    private static final String URL_STR
			= "content://com.protruly.powermanager.purebackground.provider.AutoStartAppProvider";
    public static final Uri CONTENT_URI = Uri.parse(URL_STR);

	private final static HashSet<String> mAutoStartAppList = new HashSet<String>();

	/**
	 * Get allow auto start app list.
	 * @param context
	 * @return
     */
    public static HashSet<String> getAutoStartAppList(Context context) {
    	PBArrayList<String> autoStartAppList = queryAllAppsInfo(context);
    	mAutoStartAppList.clear();
    	for(String app: autoStartAppList.getDataList()) {
    		mAutoStartAppList.add(app);
    	}
    	return mAutoStartAppList;
    }

	/**
	 * Autostart list in database only could be edited by user.
	 * @param context
	 */
	public static void initProvider(Context context){
		if(context == null){
			return;
		}
		PBArrayList<String> autoStartAppList = getAutoStartAppsInfo(context);
		PBArrayList<String> sqliteAppList = queryAllAppsInfo(context);
		
		for (String app1: autoStartAppList.getDataList()) {
			LogUtils.d(TAG, "initProvider() -> autostart app in actual = " + app1);
		}
		
		for (String app: sqliteAppList.getDataList()) {
			LogUtils.d(TAG, "initProvider() -> autostart app in cfg = " + app);
		}
	}

	/**
	 * Get the list of applications that are currently enabled for AutoStart.
	 * @param context
	 * @return not be null
	 */
	private static PBArrayList<String> getAutoStartAppsInfo(Context context){
		PBArrayList<String> autoStartAppList = new PBArrayList<String>();
		AppsInfo userAppsInfo = ConfigModel.getInstance(context).
				getAppInfoModel().getThirdPartyAppsInfo();
		if (userAppsInfo == null) {
			return autoStartAppList;
		}

		for(int i = 0; i < userAppsInfo.size(); i++){
			AppInfo appInfo = (AppInfo)userAppsInfo.get(i);
			if(appInfo == null || !appInfo.getIsInstalled()){
				continue;
			}

			AutoStartInfo autoStartInfo = AutoStartModel.getInstance(context)
					.getAutoStartInfo(appInfo.getPackageName());
			if (autoStartInfo == null) {
				continue ;
			}

			if (autoStartInfo.getIsOpen()) {
				autoStartAppList.add(appInfo.getPackageName());
			}
		}
		return autoStartAppList;
	}

	/**
	 * App is in autostart db.
	 * @param context
	 * @param pkgName
     * @return
     */
	public static boolean isAllowAutoStartApp(Context context, String pkgName) {
		return isInDB(context, getQueryWhere(), getQueryValue(pkgName), CONTENT_URI);
	}

	/**
	 * Add app to autostart db.
	 * @param context
	 * @param pkgName
	 */
	public static void addAllowAutoStartApp(Context context, String pkgName){
		insertOrUpdateDate(context, pkgName);
	}
	
	/**
	 * Remove app form autostart db
	 * @param context
	 * @param pkgName
	 */
	public static void removeAllowAutoStartApp(Context context, String pkgName){
		deleteDate(context,pkgName);
	}
	
	private static void insertOrUpdateDate(Context context, String pkgName) {
		if (context == null || pkgName == null) {
        	return ;
        }
		
		ContentValues values = new ContentValues();
		values.put(DbHelper.PACKAGE_NAME, pkgName);
		
		if(isInDB(context,
				getQueryWhere(),
				getQueryValue(pkgName),
				CONTENT_URI)){
            //do nothing
			LogUtils.d(TAG, "insertOrUpdateDate() -> " + pkgName + " is already in DB");
		} else {
			LogUtils.d(TAG, "insertOrUpdateDate() -> pkgName = " + pkgName);
			context.getContentResolver().insert(CONTENT_URI,values);
		}
	}
	
	private static void deleteDate(Context context, String packageName) {
		if (context == null || packageName == null) {
        	return ;
        }

		LogUtils.d(TAG, "deleteDate() -> packageName = " + packageName);
		context.getContentResolver().delete(
				CONTENT_URI, 
				getQueryWhere(),
				getQueryValue(packageName));
	}

	private static PBArrayList<String> queryAllAppsInfo(Context context) {
		PBArrayList<String> appInfoList = new PBArrayList<String>();
		
		if (context == null) {
        	return appInfoList;
        }
		
		String[] columns = {DbHelper.PACKAGE_NAME};
		
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(CONTENT_URI, columns, null, null, null);
		} catch(Exception e){
			//do nothing
		}
		
		synchronized(CONTENT_URI) {
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

	private static String getQueryWhere(){
		return DbHelper.PACKAGE_NAME + " = ?";
	}

	private static String[] getQueryValue(String packageName){
		return new String[]{packageName};
	}

	@Override
	public Uri getContentUri() {
		return CONTENT_URI;
	}

	@Override
	public String getTableName() {
		return DbHelper.TABLE_AllowAutoStartApp;
	}
}