package com.hb.thememanager.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.hb.thememanager.model.Theme;

public class SharePreferenceManager {

	private static final String NAME = "Hummingbird_ThemeManager";
	public static final String KEY_APPLIED_THEME_ID = "apply_theme_id";

	public static final String KEY_APPLIED_NORMAL_THEME_ID = "applied_normal_theme_id";
	public static final String KEY_APPLIED_WALLPAPER_ID = "apply_wallpaper_id";


	public static final String KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID = "apply_lockscreen_wallpaper_id";
	public static final String KEY_APPLIED_RINGTONG_ID = "apply_ringtong_id";
	public static final String KEY_APPLIED_FONT_ID = "apply_font_id";
	public static final String KEY_APPLIED_NORMAL_FONT_ID = "applied_normal_font_id";
	public static final String KEY_SEARCH_HISTORY_ID = "search_history";
	/**
	 * 启动主题中心时是否显示授权弹窗
	 */
	public static final String KEY_SHOW_AUTHORIZATION = "show_authorization";

	/**
	 * 存储本地主题最后一个ID，本地主题主要指系统内置主题和从本地手动导入的主题
	 */
	public static final String KEY_LOCAL_THEME_LAST_ID = "last_theme_local_id";

	/**
	 * 存储本地字体最后一个ID，本地字体主要指系统内置字体和从本地手动导入的字体
	 */
	public static final String KEY_LOCAL_FONT_LAST_ID = "last_font_local_id";


	/**
	 * 存储本地壁纸最后一个ID，本地壁纸主要指系统内置字体和从本地手动导入的壁纸
	 */
	public static final String KEY_LOCAL_WALLPAPER_LAST_ID = "last_wallpaper_local_id";

	/**
	 * 是否使用移动流量下载的开关
	 */
	public static final String KEY_DOWNLOAD_WITH_MOBILE_NETWORK = "dowload_with_mobile_network";

	/**
	 * 提醒使用移动流量下载的弹窗的显示次数
	 */
	public static final String KEY_TIMES_SHOW_MOBILE_CONFIRM_DIALOG = "CONFIRM_DIALOG_TIME";
	
	public static void setBooleanPreference(Context context, String key, boolean value) {
			SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        SharedPreferences.Editor editor = preferences.edit();
	        editor.putBoolean(key, value);
	        editor.commit();
	    }
	    
	    public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
	        SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        boolean result = preferences.getBoolean(key, defaultValue);
	        return result;
	    }
	    
	    public static void setIntPreference(Context context, String key, int value) {
	        SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        SharedPreferences.Editor editor = preferences.edit();
	        editor.putInt(key, value);
	        editor.commit();
	    }
	    
	    public static int getIntPreference(Context context, String key, int defaultValue) {
	        SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        int result = preferences.getInt(key, defaultValue);
	        return result;
	    }
	    
	    public static void setStringPreference(Context context, String key, String value) {
	        SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        SharedPreferences.Editor editor = preferences.edit();
	        editor.putString(key, value);
	        editor.commit();
	    }
	    
	    public static String getStringPreference(Context context, String key, String defaultValue) {
	        SharedPreferences preferences = context.getSharedPreferences(NAME, 
	        		Context.MODE_PRIVATE);
	        String result = preferences.getString(key, defaultValue);
	        return result;
	    }

	/**
	 * Only used for Font and Theme pkg
	 * @param mContext
	 * @param themeType
	 * @param themeId
	 */
	public static void saveNormalAppliedThemeId(Context mContext,int themeType,String themeId){
			setStringPreference(mContext,
					themeType == Theme.FONTS? SharePreferenceManager.KEY_APPLIED_NORMAL_FONT_ID:
							SharePreferenceManager.KEY_APPLIED_NORMAL_THEME_ID, themeId);
		}
	    
	    
}
