package com.hb.thememanager.utils;

import java.io.File;
import java.util.HashMap;
import com.hb.thememanager.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Environment;

public class Config {

	public static final boolean DEBUG = false;
	
	/*
	 * 以下路径用来存放在线主题被下载之后的文件，在存放主题文件时内置主题和
	 * 在线主题需要分开。
	 */
	
	/**
	 * 已下载的主题存放地址
	 */
	public static final String LOCAL_THEME_PATH = "sdcard/.hummingbird/theme/";
	
	/**
	 * 已下载的主题包存放地址
	 */
	public static final String LOCAL_THEME_PACKAGE_PATH = LOCAL_THEME_PATH+"pkg/";
	
	/**
	 * 已下载的主题包信息存放地址
	 */
	public static final String LOCAL_THEME_PACKAGE_INFO_DIR = LOCAL_THEME_PACKAGE_PATH+"theme_info/";
	
	
	/**
	 * 已下载的字体存放地址
	 */
	public static final String LOCAL_THEME_FONTS_PATH = LOCAL_THEME_PATH+"fonts/";
	
	/**
	 * 已下载的铃声存放地址
	 */
	public static final String LOCAL_THEME_RINGTONG_PATH = LOCAL_THEME_PATH+"ringtong/";
	
	/**
	 * 已下载的壁纸存放地址
	 */
	public static final String LOCAL_THEME_WALLPAPER_PATH = LOCAL_THEME_PATH+"wallpaper/";
	
	/**
	 * 已下载主题描述文件名
	 */
	public static final String LOCAL_THEME_DESCRIPTION_FILE_NAME = "description.xml";
	
	/**
	 * 已下载主题预览图片存放文件夹名
	 */
	public static final String LOCAL_THEME_PREVIEW_DIR_NAME = "previews/";

	
	/*
	 * 以下路径用来存放系统内置主题
	 */
	
	/**
	 * 系统内置主题包信息存放地址
	 */
	public static final String SYSTEM_THEME_INFO_DIR = "/system/hummingbird/theme/theme_info/";
	
	
	/**
	 * 系统内置主题包存放地址
	 */
	public static final String SYSTEM_THEME_DIR = "/system/hummingbird/theme/theme_pkg/";
	
	
	/**
	 * 系统默认主题包存放地址
	 */
	public static final String THEME_DEFAULT_THEME_PATH = SYSTEM_THEME_DIR+"Darling";
	
	/**
	 * 应用主题时临时存放正在操作主题文件的路径
	 * @deprecated
	 */
	public static final String THEME_EXTRACT_TMP_DIR = "sdcard/.hummingbird/theme/tmp/";
	
	/**
	 * 应用主题时备份上一个起作用的主题的存放地址
	 */
	public static final String THEME_APPLY_BACKUP_DIR = "/data/hummingbird/theme/bak/";
	/**
	 * 当前正在被使用的主题存放地址
	 */
	public static final String THEME_APPLY_DIR = "/data/hummingbird/theme/current/";
	
	public static final String THEME_ROOT_DIR = "/data/hummingbird/theme/";
	/**
	 * 当前正在被使用的主题的图标存放地址
	 */
	public static final String THEME_APPLY_ICON_DIR = THEME_APPLY_DIR+"icons/";
	
	public static final String THEME_APPLY_WALLPAPER_DIR = "/data/hummingbird/theme/current/wallpaper/desktop/";
	public static final String THEME_APPLY_LOCKSCREEN_WALLPAPER_DIR = "/data/hummingbird/theme/current/wallpaper/lockscreen/";
	
	@Deprecated
	public static final String THEME_APPLY_ICON_DIR_TMP = "/data/hummingbird/theme/current/icons_tmp/";
	public static final String THEME_APPLY_ICON_TEMP_DIR = THEME_EXTRACT_TMP_DIR+"icons/";
	
	
	public static final String KEY_PICK_THEME_FILE_PATH = "key_picker_local_theme";

	public static final String LOCKSCREEN_WALLPAPER_FILENAME = "lockscreen_wallpaper";
	
	public static final String THEME_APPLY_CUSTOM_WALLPAPER = THEME_APPLY_DIR+"wallpaper/";
	public static final String THEME_APPLY_CUSTOM_DESKTOP_WALLPAPER = THEME_APPLY_CUSTOM_WALLPAPER+"desktop/";
	public static final String THEME_APPLY_LOCKSCREEN_WALLPAPER = "/data/hummingbird/theme/lockscreen_wallpaper/";
	
	public static final String LAUNCHER_THEME_PKG_NAME = "com.hummingbird.launcher.theme";
	
	public static final String LAUNCHER_THEME_APK_NAME = THEME_EXTRACT_TMP_DIR+"hummingbird_launcher_theme.apk";
	
	public static final String LAUNCHER_PKG_NAME="com.android.dlauncher";
	
	public static final String LAUNCHER_COMPONENT_NAME = "com.android.launcher3.Launcher";

	public static final int DEFAULT_THEME_ID =  -1 ;
	
	public static final String DEFAULT_THEME_COVER = "file:///android_asset/default_theme_previews/1.jpg";
	
	
	public static final String IMAGE_LOAD_CACHE = "sdcard/.hummingbird/theme/image_cache/";
	
	public static final int DEFAULT_THEHE_DESKTOP_WALLPAPER = 0;
	
	/**
	 * Flag for update or reload wallpaper list
	 */
	public static boolean NEED_UPDATE_WALLPAPER_LIST = false;
	public static boolean NEED_UPDATE_LOCKSCREEN_WALLPAPER_LIST = false;

	public static final String[] DEFAUTL_THEME_PREVIEWS = {
		"file:///android_asset/default_theme_previews/2.jpg",
		"file:///android_asset/default_theme_previews/3.jpg"
	};


	
	
	public static File getDiskCacheDir(String cacheDir){
		File file = new File(IMAGE_LOAD_CACHE);
		if(!file.exists()){
			file.mkdir();
		}
		return file;
		
	}
	
	/**
	 * Information for parse Theme from sdcard. These tags are declared in
	 * description.xml , description.xml must write like follow:
	 * <pre>
	 *  &lt;?xml version="1.0" encoding="utf-8"?&gt;
	 * 	&lt;Theme>
	 * 		&lt;!-- 主题作者 -->
	 * 		&lt;Designer>DUI 22&lt;/Designer>
	 * 		&lt;!-- 主题名字 -->
	 * 		&lt;Name>DUI&lt;/Name>
	 * 		&lt;!-- description of current theme -->
	 * 		&lt;Description>
	 * 		Darling UI OS &lt;/Description>
	 * 		&lt;!-- 版本号 -->
	 * 		&lt;Version>1.0.1&lt;/Version>
	 * 		&lt;!-- Size of this theme package -->
	 * 		&lt;Size>16M&lt;/Size>
	 * 	&lt;/Theme>
	 *</pre>
	 */
	public static final class ThemeDescription{
		public static final String ROOT = "Theme";
		/**
		 * Theme Designer
		 */
		public static final String TAG_DESIGNER = "Designer";
		
		/**
		 * Theme Name
		 */
		public static final String TAG_NAME = "Name";
		
		/**
		 * Theme Description
		 */
		public static final String TAG_DESCRIPTION = "Description";
		
		/**
		 * Theme Package Size
		 */
		public static final String TAG_SIZE = "Size";
		
		/**
		 * Theme Version
		 */
		public static final String TAG_VERSION = "Version";
		
	}
	
	
	public static  interface DatabaseColumns{
		public static final String _ID = "_id";
		
		public static final String NAME = "name";
		
		public static final String DESGINER = "desginer";
		
		public static final String DESCRIPTION = "description";
		
		/**
		 * Real Theme file path
		 */
		public static final String FILE_PATH = "file_path";
		
		/**
		 * Theme information's saved path
		 */
		public static final String LOADED_PATH = "loaded_path";
		
		public static final String LOADED = "loaded";

		/**
		 * Theme type, one of{@link com.hb.thememanager.model.Theme#THEME_PKG},{@link com.hb.thememanager.model.Theme#RINGTONG},
		 * {@link com.hb.thememanager.model.Theme#WALLPAPER},or {@link com.hb.thememanager.model.Theme#FONTS}
		 */
		public static final String TYPE = "type";
		
		/**
		 * Version Code for current theme.
		 */
		public static final String VERSION = "version";
		
		public static final String LAST_MODIFIED_TIME = "last_modified_time";
		
		public static final String URI = "url";
		
		public static final String APPLY_STATUS = "apply_status";
		
		public static final String DOWNLOAD_STATUS = "download_status";
		
		public static final String TOTAL_BYTES = "total_bytes";
		
		public static final String CURRENT_BYTES = "current_bytes";
		
		public static final String IS_SYSTEM_THEME = "is_system";
		
		public static final String SIZE = "size";
	}
	
	/**
	 * Status for load theme 
	 */
	public static class LoadThemeStatus{
		public static final int STATUS_SUCCESS = 0;
		public static final int STATUS_THEME_FILE_ERROR = 1;
		public static final int STATUS_FAIL = 2;
		public static final int STATUS_THEME_NOT_EXISTS = 3;
	
	}
	
	/**
	 * Status for apply theme
	 *
	 */
	public static class ThemeApplyStatus{
		public static final int STATUS_CURRENT_APPLIED = 0;
		public static final int STATUS_FAILED = 1;
		public static final int STATUS_APPLING = 2;
		public static final int STATUS_SUCCESS = 3;
		public static final int STATUS_THEME_FILE_NOT_EXITS = 4;
	}
	
	/**
	 *壁纸配置信息，例如内置壁纸存放位置等 
	 *
	 */
	public static class Wallpaper{
		/**
		 * 系统内置壁纸存放根路径
		 */
		public static final String SYSTEM_WALLPAPER_PATH = "/system/hummingbird/theme/wallpaper/";
		/**
		 * 系统内置桌面壁纸存放路径
		 */
		public static final String SYSTEM_DESKTOP_WALLPAPER_PATH = SYSTEM_WALLPAPER_PATH +"desktop";
		/**
		 * 系统内置锁屏壁纸存放路径
		 */
		public static final String SYSTEM_LOCKSCREEN_WALLPAPER_PATH = SYSTEM_WALLPAPER_PATH + "lockscreen";
		/**
		 * 用户自定义桌面壁纸存放路径
		 */
		public static final String CUSTOM_DESKTOP_WALLPAPER_PATH = LOCAL_THEME_WALLPAPER_PATH+"desktop/user/";
		
		/**
		 * 用户自定义锁屏壁纸存放路径
		 */
		public static final String CUSTOM_LOCKSCEEN_WALLPAPER_PATH = LOCAL_THEME_WALLPAPER_PATH+"lockscreen/user/";
	}
	
	/**
	 * Keys for transfer data by Intent
	 *
	 */
	public static class ActionKey{
		public static final String KEY_WALLPAPER_PREVIEW_BUNDLE = "wallpaper_preview_bundle";
		public static final String KEY_WALLPAPER_PREVIEW_LIST = "wallpaper_preview_list";
		public static final String KEY_WALLPAPER_PERVIEW_CURRENT_ITEM = "wallpaper_preview_current_item";
		public static final String KEY_WALLPAPER_PREVIEW_TYPE = "wallpaper_type";
		
		public static final String KEY_THEME_PKG_DETAIL = "theme:theme_detail";

		public static final String KEY_APPLY_THEME_IN_SERVICE = "theme:apply_in_service_theme";
	}
	
	/**
	 * Actions for start Activity or other components
	 *
	 */
	public static class Action{
		/**
		 * 设置壁纸的action
		 */
		public static final String ACTION_WALLPAPER_SET = "com.hb.action.ACTION_WALLPAPER_SET";
		/**
		 * 主题改变之后会发送广播给其他APP，其他APP通过该广播处理一些资源加载的逻辑
		 */
		public static final String ACTION_THEME_CHANGE = "com.hb.theme.ACTION_THEME_CHANGE";
		/**
		 * 应用主题资源时需要启动一个service来在后台执行，
		 * 保证用户在还没有切换完就退出界面导致应用失败的问题不会发生
		 */
		public static final String ACTION_APPLY_IN_SERVICE = "com.hb.theme.ACTION_APPLY_IN_SERVICE";

		/**
		 * 设计师导入本地主题的Action
		 */
		public static final String ACTION_THEME_PICKER = "com.hb.thememanager.ACTION_THEME_PICKER";
	}
	
	/**
	 *Component of theme manager,used this component to start 
	 *target page in {@link com.hb.thememanager.ui.MainActivity#onCreate(android.os.Bundle, 
	 *android.os.PersistableBundle)} method
	 */
	public static class ThemeComponent{
		public static final String WALLPAPER = "wallpaper";
		public static final String THEME = "theme";
		public static final String FONTS = "fonts";
		public static final String RINGTONG = "ringtong";
	}
	
	public static final class HandlerIntMessage {

		public static final int MSG_IMAGE_LOAD_DONE = 0x01;

		public static final int MSG_IMAGE_LOAD_ERROR = 0x02;

	}
	
	public static final class Color{
		public static final int WHITE = 0xFFFFFFFF;
		public static final int BLACK = 0xE5000000;
	}
	
	/**
	 * This drawable just only used for start wallpaper preview activity.
	 * It must be recycle when preview activity's starting animation is
	 * finished
	 */
	public static Drawable sStaringImageInPreview;


	private static HashMap<String,Integer> SYSTEM_THEME_NAMES_MAP = new HashMap<String,Integer>();
	static{
		SYSTEM_THEME_NAMES_MAP.put("default",R.string.gold_legend);
		SYSTEM_THEME_NAMES_MAP.put("/system/hummingbird/theme/theme_pkg//Fashion",R.string.tactful);
	}

	public static String getSystemThemeName(String targetThemePath,Context context){
		Integer nameId = SYSTEM_THEME_NAMES_MAP.get(targetThemePath);
		if(null == nameId){
			return null;
		}
		int id = nameId.intValue();
		if(id != 0){
			return context.getString(id);
		}

		return null;
	}

}
