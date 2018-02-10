package com.hb.thememanager.database;

import com.hb.thememanager.model.Theme;

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
		}
		return null;
	}

	
	
}
