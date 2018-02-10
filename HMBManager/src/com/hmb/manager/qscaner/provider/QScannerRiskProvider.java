package com.hmb.manager.qscaner.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.hmb.manager.qscaner.bean.RiskEntity;

/**
 * Provider for accessing QScanner risk data.
 */
public class QScannerRiskProvider extends BaseContentProvider {
	private static final String TAG = "QScannerRiskProvider";

    private static final String URL_STR
			= "content://com.hmb.manager.qscaner.provider.QScannerRiskProvider";
    public static final Uri CONTENT_URI = Uri.parse(URL_STR);

	/**
	 * Record Risk Info.
	 * @param context
	 * @param entity
	 */
	public static void saveRiskEntity(Context context, RiskEntity entity) {
		if (entity.riskType == RiskEntity.RISK_TYPE_APP) {
			insertOrUpdateAppData(context, entity);
		} else if (entity.riskType == RiskEntity.RISK_TYPE_APK) {
			insertOrUpdateApkData(context, entity);
		}
	}

	private static void insertOrUpdateAppData(Context context, RiskEntity entity) {
		if (context == null || entity == null) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put(DbHelper.RISK_FILE_TYPE, entity.riskType);
		values.put(DbHelper.PACKAGE_NAME, entity.packageName);
		values.put(DbHelper.SOFT_NAME, entity.softName);
		values.put(DbHelper.VERSION, entity.version);
		values.put(DbHelper.VERSION_CODE, entity.versionCode);
		values.put(DbHelper.PATH, entity.path);
		values.put(DbHelper.SCAN_RESULT, entity.scanResult);
		values.put(DbHelper.VIRUS_NAME, entity.virusName);
		values.put(DbHelper.VIRUS_DISCRIPTION, entity.virusDiscription);
		values.put(DbHelper.VIRUS_URL, entity.virusUrl);

		if (isInDB(context, getAPPSelection(),
				getSelectionArgs(entity.riskType, entity.packageName), CONTENT_URI)) {
			//do nothing
			Log.d(TAG, "insertOrUpdateData() -> pkgName " + entity.packageName + " is In Database!!!");
		} else {
			Log.d(TAG, "insertOrUpdateData() -> pkgName = " + entity.packageName);
			context.getContentResolver().insert(CONTENT_URI, values);
		}
	}

	private static void insertOrUpdateApkData(Context context, RiskEntity entity) {
		if (context == null || entity == null) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put(DbHelper.RISK_FILE_TYPE, entity.riskType);
		values.put(DbHelper.PACKAGE_NAME, entity.packageName);
		values.put(DbHelper.SOFT_NAME, entity.softName);
		values.put(DbHelper.VERSION, entity.version);
		values.put(DbHelper.VERSION_CODE, entity.versionCode);
		values.put(DbHelper.PATH, entity.path);
		values.put(DbHelper.SCAN_RESULT, entity.scanResult);
		values.put(DbHelper.VIRUS_NAME, entity.virusName);
		values.put(DbHelper.VIRUS_DISCRIPTION, entity.virusDiscription);
		values.put(DbHelper.VIRUS_URL, entity.virusUrl);
		if (isInDB(context, getAPKSelection(),
				getSelectionArgs(entity.riskType, entity.path), CONTENT_URI)) {
			//do nothing
			Log.d(TAG, "insertOrUpdateData() -> pkgName " + entity.packageName + " is In Database!!!");
		} else {
			Log.d(TAG, "insertOrUpdateData() -> pkgName = " + entity.packageName);
			context.getContentResolver().insert(CONTENT_URI, values);
		}
	}

	/**
	 * Get Risk Info.
	 * @param context
	 * @param riskFileType
	 * @param pkgNameOrPath
	 */
	public static RiskEntity getRiskEntity(Context context, int riskFileType, String pkgNameOrPath) {
		RiskEntity riskEntity = null;
		if (context == null || pkgNameOrPath == null) {
			return null;
		}
		String[] columns = {
				DbHelper.RISK_FILE_TYPE,
				DbHelper.PACKAGE_NAME,
				DbHelper.SOFT_NAME,
				DbHelper.VERSION,
				DbHelper.VERSION_CODE,
				DbHelper.PATH,
				DbHelper.SCAN_RESULT,
				DbHelper.VIRUS_NAME,
				DbHelper.VIRUS_DISCRIPTION,
				DbHelper.VIRUS_URL};

		Cursor cursor = null;
		try{
			String selection;
			if (riskFileType == RiskEntity.RISK_TYPE_APK) {
				selection = getAPKSelection();
			} else {
				selection = getAPPSelection();
			}
			cursor = context.getContentResolver().query(CONTENT_URI,
					columns,
					selection,
					getSelectionArgs(riskFileType, pkgNameOrPath),
					null);
		}catch(Exception e){
			//nothing
		}

		synchronized(CONTENT_URI){
			if (cursor != null){
				if (cursor.moveToFirst()) {
					riskEntity = new RiskEntity();
					riskEntity.riskType = cursor.getInt(
							cursor.getColumnIndexOrThrow(DbHelper.RISK_FILE_TYPE));
					riskEntity.packageName = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.PACKAGE_NAME));
					riskEntity.softName = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.SOFT_NAME));
					riskEntity.version = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.VERSION));
					riskEntity.versionCode = cursor.getInt(
							cursor.getColumnIndexOrThrow(DbHelper.VERSION_CODE));
					riskEntity.path = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.PATH));
					riskEntity.virusName = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.VIRUS_NAME));
					riskEntity.virusDiscription = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.VIRUS_DISCRIPTION));
					riskEntity.virusUrl = cursor.getString(
							cursor.getColumnIndexOrThrow(DbHelper.VIRUS_URL));
				}
				cursor.close();
			}
		}
		return riskEntity;
	}

	/**
	 * Remove Risk Info.
	 * @param context
	 * @param riskFileType
	 * @param pkgNameOrPath
	 */
	public static void removeRiskEntity(Context context, int riskFileType, String pkgNameOrPath) {
		if (context == null || pkgNameOrPath == null) {
			return;
		}
		String where;
		if (riskFileType == RiskEntity.RISK_TYPE_APK) {
			where = getAPKSelection();
		} else {
			where = getAPPSelection();
		}
		context.getContentResolver()
				.delete(CONTENT_URI, where, getSelectionArgs(riskFileType, pkgNameOrPath));
	}

	private static String getAPPSelection(){
		return DbHelper.RISK_FILE_TYPE + " = ? and " + DbHelper.PACKAGE_NAME + " = ? ";
	}

	private static String getAPKSelection(){
		return DbHelper.RISK_FILE_TYPE + " = ? and " + DbHelper.PATH + " = ? ";
	}

	private static String[] getSelectionArgs(int riskFileType, String pkgNameOrPath){
		return new String[]{Integer.valueOf(riskFileType).toString(), pkgNameOrPath};
	}

	@Override
	public Uri getContentUri() {
		return CONTENT_URI;
	}

	@Override
	public String getTableName() {
		return DbHelper.TABLE_RISK;
	}
}