package com.hb.thememanager.database;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

import android.content.Context;

public class DatabaseFactory {
	
	/**
	 * Create Database controller.
	 * @param type
	 * @return
	 */
	public static ThemeDatabaseController createDatabaseController(int type,Context context){
		if(type == Theme.THEME_PKG){
			return new ThemePkgDbController(context,type);
		}else if(type == Theme.WALLPAPER){
			return new ThemeWallpaperDbController(context, type);
		}else if(type == Theme.LOCKSCREEN_WALLPAPER){
			return new ThemeWallpaperDbController(context, type);
		}else if(type == Theme.RINGTONE) {
			return new ThemeRingtoneDbController(context, type);
		}else if(type == Theme.FONTS){
			return new ThemeFontsDbController(context, type);
		}else if(type == Config.DatabaseColumns.THEME_USERS){
			return new UserThemesController(context,type);
		}
		return null;
	}

	
	
}
