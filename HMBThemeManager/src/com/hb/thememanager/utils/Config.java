package com.hb.thememanager.utils;

import java.io.File;
import java.util.HashMap;
import com.hb.thememanager.R;
import com.hb.thememanager.model.Theme;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class Config {

	public static final boolean DEBUG = true;
	
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
	 * 已下载的字体包信息存放地址
	 */
	public static final String LOCAL_THEME_FONTS_INFO_DIR = LOCAL_THEME_FONTS_PATH+"font_info/";
	
	/**
	 * 已下载的铃声存放地址
	 */
	public static final String LOCAL_THEME_RINGTONG_PATH = "sdcard/Ringtones/";
	
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
	 * 系统内置字体包信息存放地址
	 */
	public static final String SYSTEM_FONTS_INFO_PATH = "/system/hummingbird/theme/fonts_info/";

	/**
	 * 系统内置字体包存放地址
	 */
	public static final String SYSTEM_FONTS_PATH = "/system/hummingbird/theme/fonts/";

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

	public static final String DEFAULT_THEME_ID =  "DFT00000000" ;//默认主题包ID

	public static final String DEFAULT_FONT_ID =  "DFF00000000" ;//默认字体包ID
	
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
		/**
		 * 用于标识用户已购买的主题类型
		 */
		public static final int THEME_USERS = 0X06;

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
		 * Theme type, one of{@link com.hb.thememanager.model.Theme#THEME_PKG},{@link com.hb.thememanager.model.Theme#RINGTONE},
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

		public static final String HAS_NEW_VERSION = "new_version";

		public static final String PAID = "paid";

		public static final String PRICE = "price";

		public static final String IS_CHARGE = "is_charge";

		public static final String DESIGNER_ID = "designer_id";

		public static final String USER_ID = "user_id";
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
		public static final String KEY_WALLPAPER_DETAIL_URL = "wallpaper_loader_more_url";
		public static final String KEY_WALLPAPER_DETAIL_ID = "wallpaper_id";
		public static final String KEY_WALLPAPER_DETAIL_TYPE = "wallpaper_type";
		public static final String KEY_WALLPAPER_DETAIL_SEARCH_KEY = "wallpaper_search_key";
		
		public static final String KEY_THEME_PKG_DETAIL = "theme:theme_detail";
		
		public static final String KEY_FAST_ENTRY = "theme:fast_entry_theme_type";
		
		public static final String KEY_HOME_THEME_ITEM = "theme:home_theme_list_item";

		public static final String KEY_JUMP_TITLE = "page_title";

		public static final String KEY_SHOW_COMMENTS = "theme:show_comments";
		public static final String KEY_HANDLE_COMMENTS = "theme:handle_comments";
		public static final String KEY_APPLY_THEME_IN_SERVICE = "theme:apply_in_service_theme";

		public static final String KEY_LOAD_MORE_TYPE = "theme:load_more_type";
		public static final String KEY_LOAD_MORE_NAME = "theme:load_more_name";
		public static final String KEY_LOAD_MORE_DESIGNER_ID = "theme:load_more_designer_id";
		public static final String KEY_LOAD_MORE_ID = "theme:load_more_id";
		public static final String KEY_LOAD_MORE_LIST = "theme:load_more_list";
		public static final String KEY_LOAD_MORE_FROM_ADV = "theme:load_more_list_from_adv";
        public static final String KEY_CATEGORY_ID = "theme:category_id";

		/**
		 * 点击广告进入广告详情的Key
		 */
		public static final String KEY_ADV_DETAIL = "theme:adv_detail";

		/*
		 *专题类Action Key
		 */

		public static final String KEY_TOPIC_ID = "theme:topic_id";
		public static final String KEY_TOPIC_TYPE = "theme:topic_type";
		public static final String KEY_TOPIC_TITLE = "theme:topic_title";

		public static final String KEY_APPLIED_THEME_ID = "theme:appied_theme_id";


	}
	
	/**
	 * Actions for start Activity or other components
	 *
	 */
	public static class Action{
		public static final String ACTION_WALLPAPER_SET = "com.hb.action.ACTION_WALLPAPER_SET";
		public static final String ACTION_THEME_CHANGE = "com.hb.theme.ACTION_THEME_CHANGE";
		public static final String ACTION_HOME_THEME_LIST_ITEM_DETAIL = "com.hb.theme.HOME_THEME_ITEM_DETAIL";
		public static final String ACTION_WALLPAPER_DETAIL = "com.hb.theme.WALLPAPER_DETAIL";
		
		public static final String ACTION_RANK = "com.hb.theme.ACTION_RANK";
		public static final String ACTION_CATEGORY = "com.hb.theme.ACTION_CATEGORY";
		public static final String ACTION_TOPIC = "com.hb.theme.ACTION_TOPIC";
		public static final String ACTION_USER = "com.hb.theme.ACTION_USER";
		public static final String ACTION_SEARCH = "com.hb.theme.ACTION_SEARCH";

/**
		 * 应用主题资源时需要启动一个service来在后台执行，
		 * 保证用户在还没有切换完就退出界面导致应用失败的问题不会发生
		 */
		public static final String ACTION_APPLY_IN_SERVICE = "com.hb.theme.ACTION_APPLY_IN_SERVICE";

		/**
		 * 设计师导入本地主题的Action
		 */
		public static final String ACTION_THEME_PICKER = "com.hb.thememanager.ACTION_THEME_PICKER";
		public static final String ACTION_SHOW_COMMENTS = "com.hb.theme.ACTION_SHOW_COMMENTS";

		public static final String ACTION_ADD_COMMENTS = "com.hb.theme.ACTION_ADD_COMMENTS";

		public static final String UPGRADE_THEME_SERVICE = "com.hb.thememanager.UPGRADE_THEME_SERVICE";

		public static final String CHECK_USER_THEME_SERVICE = "com.hb.thememanager.CHECK_USER_SERVICE";
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
		public static final String FEEDBACK = "feedback";
		public static final String SETTING = "setting";
	}
	
	public static final class HandlerIntMessage {

		public static final int MSG_IMAGE_LOAD_DONE = 0x01;

		public static final int MSG_IMAGE_LOAD_ERROR = 0x02;

	}
	
	public static final class Color{
		public static final int WHITE = 0xFFFFFFFF;
		public static final int BLACK = 0xE5000000;
	}

	public static final class LocalId{
		public static final String THEME_PREFIX = "LZTB";
		public static final String WALLPAPER_PREFIX = "LBZ";
		public static final String LOCK_WALLPAPER_PREFIX = "LLCBZ";
		public static final String FONT_PREFIX = "LZT";
	}


	public static final class HttpUrl{
		private static HashMap<Integer,String> sHomeThemeUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeDetailUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeRankUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeTopicUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeTopicDetailHeaderUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeTopicDetailListUrl = new HashMap<Integer,String>();
		private static HashMap<Integer,String> sThemeHotRecommendMoreUrl = new HashMap<>();
		private static HashMap<Integer,String> sCategoryUrl = new HashMap<>();
		private static HashMap<Integer,String> sCategoryDetailUrl = new HashMap<>();
//		private static final String HOST = "http://192.168.16.243:8080/themestore-web-api/";//Test Host
		private static final String HOST = "http://61.147.171.31/themestore-web-api/";
		/*
		 *Urls for Home page list
		 */
		private static final String HOME_THEME_PKG = "themeHotRecommend";
		private static final String HOME_WALLPAPER = "wallpaperHotRecommend";
		private static final String HOME_FONTS = "fontHotRecommend";
		private static final String HOME_RINGTONG = "";

		/*
		 *Urls for theme detail
		 */
		private static final String DETAIL_THEME_PKG = "themeDetails";
		private static final String DETAIL_WALLPAPER = "wallpaperDetails";
		private static final String DETAIL_FONTS = "fontDetails";


		/**
		 * Urls for theme rank
		 */
		private static final String RANK_THEME_PKG = "themeRank";
		private static final String RANK_WALLPAPER = "wallpaperRank";
		private static final String RANK_FONTS = "fontRank";

		/**
		 * Urls for theme topic
		 */
		private static final String TOPIC_THEME_PKG = "themeSpecial";
		private static final String TOPIC_WALLPAPER = "wallpaperSpecial";
		private static final String TOPIC_FONTS = "fontSpecial";

		/**
		 * 分类url
		 */
		private static final String CATEGORY_THEME_PKG = "themeType";
		private static final String CATEGORY_WALLPAPER = "wallpaperType";
		private static final String CATEGORY_FONTS = "fontType";

		/**
		 * 分类详情url
		 */
		private static final String CATEGORY_DETAIL_THEME_PKG = "themeTypeDetails";
		private static final String CATEGORY_DETAIL_WALLPAPER = "wallpaperTypeDetails";
		private static final String CATEGORY_DETAIL_FONTS = "fontTypeDetails";

		/**
		 * 广告url
		 */
		public static final String BANNER = HOST+"banner";
		/**
		 * 评论url
		 */
		public static final String COMMENTS_URL = HOST+"commentList";

		/**
		 * 添加评论地址
		 */
		public static final String ADD_COMMENTS_URL = HOST+"report/comment";

		/**
		 * 主题评分接口
		 */
		public static final String COMMENTS_SCORE_URL =HOST+"commentScore";

		public static final String DISIGNER_THEME_URL = HOST+"otherWorks";

		/**
		 * 搜索接口
		 */
		public static final String SEARCH_ASSIST_URL = HOST+"searchKey";
		/**
		 * 支付宝url
		 */
		public static final String ALIPAY_URL = HOST+"alipayOrder";
		/**
		 * 微信支付url
		 */
		public static final String WECHATPAY_URL = HOST+"weixinOrder";
		public static final String WECHATPAY_APPID = "wxa6e0001554511814";

		public static final String THEME_UPGRADE = HOST+"versionUpgrade";


		/**
		 * 搜索
		 */
		public static final String SEARCH_URL = HOST+"search";

		public static final String DOWNLOAD_POST = HOST+"report/download";

		/**
		 * feedback
		 */
		public static final String FEEDBACK_URL = HOST + "report/feedback";

		/**
		 * url for purchase record
		 */
		public static final String PURCHASE_RECORD_URL = HOST + "paymentHistory";

		/**
		 * urls for load more theme list
		 */
		public static final String WALPPAPER_HOTRECOMMEND_MORE ="wallpaperHotRecommendMore";

		public static final String FONTS_HOTRECOMMEND_MORE ="fontHotRecommendMore";

		public static final String THEME_HOTRECOMMEND_MORE ="themeHotRecommendMore";


		static {
			/*
			 *Initial home theme urls
			 */
			sHomeThemeUrl.put(Theme.THEME_PKG,HOST+HOME_THEME_PKG);
			sHomeThemeUrl.put(Theme.WALLPAPER,HOST+HOME_WALLPAPER);
			sHomeThemeUrl.put(Theme.FONTS,HOST+HOME_FONTS);

			/*
			 *Initial Theme detail urls
			 */
			sThemeDetailUrl.put(Theme.THEME_PKG,HOST+DETAIL_THEME_PKG);
			sThemeDetailUrl.put(Theme.WALLPAPER,HOST+DETAIL_WALLPAPER);
			sThemeDetailUrl.put(Theme.FONTS,HOST+DETAIL_FONTS);

			/*
			 *Initial Theme rank urls
			 */
			sThemeRankUrl.put(Theme.THEME_PKG,HOST+RANK_THEME_PKG);
			sThemeRankUrl.put(Theme.WALLPAPER,HOST+RANK_WALLPAPER);
			sThemeRankUrl.put(Theme.FONTS,HOST+RANK_FONTS);

			/*
			 *Initial Theme topic urls
			 */
			sThemeTopicUrl.put(Theme.THEME_PKG,HOST+TOPIC_THEME_PKG);
			sThemeTopicUrl.put(Theme.WALLPAPER,HOST+TOPIC_WALLPAPER);
			sThemeTopicUrl.put(Theme.FONTS,HOST+TOPIC_FONTS);


			/*
			 *Initial Theme Hot recommend urls
			 */
			sThemeHotRecommendMoreUrl.put(Theme.THEME_PKG,HOST+THEME_HOTRECOMMEND_MORE);
			sThemeHotRecommendMoreUrl.put(Theme.WALLPAPER,HOST+WALPPAPER_HOTRECOMMEND_MORE);
			sThemeHotRecommendMoreUrl.put(Theme.FONTS,HOST+FONTS_HOTRECOMMEND_MORE);


	/*
			 *Initial category urls
			 */
			sCategoryUrl.put(Theme.THEME_PKG,HOST+CATEGORY_THEME_PKG);
			sCategoryUrl.put(Theme.WALLPAPER,HOST+CATEGORY_WALLPAPER);
			sCategoryUrl.put(Theme.FONTS,HOST+CATEGORY_FONTS);

			/*
			 *Initial category detail urls
			 */
			sCategoryDetailUrl.put(Theme.THEME_PKG,HOST+CATEGORY_DETAIL_THEME_PKG);
			sCategoryDetailUrl.put(Theme.WALLPAPER,HOST+CATEGORY_DETAIL_WALLPAPER);
			sCategoryDetailUrl.put(Theme.FONTS,HOST+CATEGORY_DETAIL_FONTS);

			/*
			 *专题详情头部URL
			 */
			sThemeTopicDetailHeaderUrl.put(Theme.THEME_PKG,HOST+"themeSpecialDetails");
			sThemeTopicDetailHeaderUrl.put(Theme.WALLPAPER,HOST+"wallpaperSpecialDetails");
			sThemeTopicDetailHeaderUrl.put(Theme.FONTS,HOST+"fontSpecialDetails");


			/*
			 *专题详情列表URL
			 */
			sThemeTopicDetailListUrl.put(Theme.THEME_PKG,HOST+"themeSpecialDetailsThemeList");
			sThemeTopicDetailListUrl.put(Theme.WALLPAPER,HOST+"wallpaperSpecialDetailswallpaperList");
			sThemeTopicDetailListUrl.put(Theme.FONTS,HOST+"fontSpecialDetailsFontList");
		}

		/**
		 * 通过主题类型获取首页资源url
		 * @param themeType
		 * @return
		 */
		public static String getHomeThemeUrl(int themeType){
			return sHomeThemeUrl.get(themeType);
		}

		/**
		 * 通过主题类型获取主题详情url
		 * @param themeType
		 * @return
		 */
		public static String getDetailUrl(int themeType){
			return sThemeDetailUrl.get(themeType);
		}

		/**
		 * 通过主题类型获取排行榜url
		 * @param themeType
		 * @return
		 */
		public static String getRankUrl(int themeType){
			return sThemeRankUrl.get(themeType);
		}

		/**
		 * 通过主题类型获取排行榜url
		 * @param themeType
		 * @return
		 */
		public static String getPurchaseRecordUrl(){
			return PURCHASE_RECORD_URL;
		}
		
		/**
		 * 通过主题类型获取专题url
		 * @param themeType
		 * @return
		 */
		public static String getTopicUrl(int themeType){
			return sThemeTopicUrl.get(themeType);
		}

		/**
		 * 获取主题首页分类更多页面的地址
		 * @param themeType
		 * @return
		 */
		public static String getHotRecommendUrl(int themeType){
			return sThemeHotRecommendMoreUrl.get(themeType);
		}


	/**
		 * 获取分类页面的地址
		 * @param themeType
		 * @return
		 */
		public static String getCategoryUrl(int themeType){
			return sCategoryUrl.get(themeType);
		}

		/**
		 * 获取分类页面的地址
		 * @param themeType
		 * @return
		 */
		public static String getCategoryDetailUrl(int themeType){
			return sCategoryDetailUrl.get(themeType);
		}
		public static String getTopicDetailHeaderUrl(int type){
			return sThemeTopicDetailHeaderUrl.get(type);
		}
		public static String getTopicDetailListUrl(int type){
			return sThemeTopicDetailListUrl.get(type);
		}

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

	public static String getThemeDownloadPath(int themeType){
		switch (themeType){
			case Theme.FONTS:
				return LOCAL_THEME_FONTS_PATH;
			case Theme.WALLPAPER:
				return LOCAL_THEME_WALLPAPER_PATH;
			case Theme.THEME_PKG:
				return LOCAL_THEME_PACKAGE_PATH;
			case Theme.RINGTONE:
				return LOCAL_THEME_RINGTONG_PATH;
			default:
				return LOCAL_THEME_PACKAGE_PATH;
		}
	}

}
