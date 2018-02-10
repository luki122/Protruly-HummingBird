package com.hmb.manager;

public class Constant {
	public final static String RUBBLISH_KEY="rubblish_key";
	public final static String CACHE_KEY="cache_key";
	public final static String APK_INSTALLED="已安装";
	public final static String APK_UNINSTALLED="未安装";
	public final static String APK_OLDVERSION="旧版";
	public final static String APK_NEWVERSION="新版";
	public final static String SHARED_PREFERENCES_KEY_QSCAN_TIME = "qscan_time";

	// wlan自动更新数据库
	public final static String SHARED_PREFERENCES_KEY_AUTO_UPDATE_DATABASE
			= "auto_update_database";

	// 上次检查数据库更新时间
	public final static String SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME
			= "last_check_update_time";

	// 上次数据库更新时间
	public final static String SHARED_PREFERENCES_LAST_UPDATE_TIME
			= "last_update_time";

	// 自动检查数据库更新时间, 三天
	public static final int AUTO_CHECK_INTERVAL = 86400 * 3 * 1000;

	public final static String ONEKEY_CLEANUP_TIME="onekey_cleanup_time";
	
	public final static String CACHE_CLEANUP_TIME="cache_cleanup_time";
	
	public final static String RUBBLISH_CLEANUP_TIME="rubblish_cleanup_time";
	
	public final static String MEMORY_CLEAN_TIME="memory_cleanup_time";
	
	public final static String RUBBLISH_CLEANUP_SIZE="rubblish_cleanup_size";
}