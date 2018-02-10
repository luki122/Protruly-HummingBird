package com.hb.thememanager.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.Config.DatabaseColumns;

public class ThemeWallpaperDbController extends ThemeDatabaseController<Wallpaper> {

	public  ThemeWallpaperDbController(Context context,int themeType) {
		super(context,themeType);
		// TODO Auto-generated constructor stub
		
	}

	@Override
	protected Wallpaper createTypeInstance() {
		return new Wallpaper();
	}


}
