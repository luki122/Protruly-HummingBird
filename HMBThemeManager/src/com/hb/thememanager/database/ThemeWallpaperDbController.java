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

public class ThemeWallpaperDbController extends ThemeDatabaseController<Wallpaper> implements DatabaseColumns {

	public  ThemeWallpaperDbController(Context context,int themeType) {
		super(context,themeType);
		// TODO Auto-generated constructor stub
		
	}
	
	
	@Override
	public Wallpaper getThemeById(int themeId) {
		// TODO Auto-generated method stub
		Wallpaper theme = null;
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, Config.DatabaseColumns._ID + "=?", new String[]{String.valueOf(themeId)}, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				
				if (cursor.moveToNext()) {
					theme = new Wallpaper();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.totalBytes = cursor.getLong(cursor.getColumnIndex(TOTAL_BYTES));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getInt(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED));
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));
					theme.type = getThemeType();
				}
			}
			cursor.close();
		}
		return theme;
	}

	@Override
	public List<Wallpaper> getThemes() {
		// TODO Auto-generated method stub
		ArrayList<Wallpaper> themes = new ArrayList<Wallpaper>();
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					Wallpaper theme = new Wallpaper();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.themeFilePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.totalBytes = cursor.getLong(cursor.getColumnIndex(TOTAL_BYTES));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getInt(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED)) ;
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));
					theme.type = getThemeType();
					themes.add(theme);
				}
			}
			cursor.close();
		}
		return themes;
		
	}

	@Override
	public boolean isLoaded(int themeId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateTheme(Wallpaper theme) {
		if (getDatabase() != null) {
			ContentValues values = new ContentValues();
			values.put(Config.DatabaseColumns.URI, theme.downloadUrl);
			values.put(Config.DatabaseColumns.FILE_PATH, theme.themeFilePath);
			
			values.put(Config.DatabaseColumns.LOADED_PATH, theme.loadedPath);
			values.put(Config.DatabaseColumns.NAME, theme.name);
			
			values.put(Config.DatabaseColumns.TYPE, theme.type);
			values.put(Config.DatabaseColumns.APPLY_STATUS, theme.applyStatus);
			
			values.put(Config.DatabaseColumns.LOADED, theme.loaded);
			values.put(Config.DatabaseColumns.DOWNLOAD_STATUS, theme.downloadStatus);
			
			values.put(Config.DatabaseColumns.LAST_MODIFIED_TIME, theme.lastModifiedTime);
			values.put(Config.DatabaseColumns.DESGINER, theme.designer);
			
			values.put(Config.DatabaseColumns.VERSION, theme.version);
			values.put(Config.DatabaseColumns.DESCRIPTION, theme.description);
			
			values.put(Config.DatabaseColumns.TOTAL_BYTES, theme.totalBytes);
			
			values.put(Config.DatabaseColumns.IS_SYSTEM_THEME,theme.isSystemTheme);
			values.put(Config.DatabaseColumns.SIZE, theme.size);
			update( values, Config.DatabaseColumns.LOADED_PATH + "=?", new String[]{String.valueOf(theme.loadedPath)});
		}
		return true;
	}

	@Override
	public boolean deleteTheme(Wallpaper theme) {
		// TODO Auto-generated method stub
		return deleteTheme(theme.id);
	}

	@Override
	public boolean deleteTheme(int themeId) {
		return delete(Config.DatabaseColumns._ID + "=?"
				, new String[]{String.valueOf(themeId)}) != 0;
	}


	@Override
	public void insertTheme(Wallpaper theme) {
		// TODO Auto-generated method stub
		if (getDatabase() != null) {
			ContentValues values = new ContentValues();
			values.put(Config.DatabaseColumns.URI, theme.downloadUrl);
			values.put(Config.DatabaseColumns.FILE_PATH, theme.themeFilePath);
			
			values.put(Config.DatabaseColumns.LOADED_PATH, theme.loadedPath);
			values.put(Config.DatabaseColumns.NAME, theme.name);
			
			values.put(Config.DatabaseColumns.TYPE, theme.type);
			values.put(Config.DatabaseColumns.APPLY_STATUS, theme.applyStatus);
			
			values.put(Config.DatabaseColumns.LOADED, theme.loaded);
			values.put(Config.DatabaseColumns.DOWNLOAD_STATUS, theme.downloadStatus);
			
			values.put(Config.DatabaseColumns.LAST_MODIFIED_TIME, theme.lastModifiedTime);
			values.put(Config.DatabaseColumns.DESGINER, theme.designer);
			
			values.put(Config.DatabaseColumns.VERSION, theme.version);
			values.put(Config.DatabaseColumns.DESCRIPTION, theme.description);
			
			values.put(Config.DatabaseColumns.TOTAL_BYTES, theme.totalBytes);
			values.put(Config.DatabaseColumns.IS_SYSTEM_THEME,theme.isSystemTheme);
			values.put(Config.DatabaseColumns.SIZE, theme.size);
			long ok = insert(values);
		}
	}





	@Override
	public int getCount() {
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, null, null, null, null, null);
			if (cursor != null) {
				int count = cursor.getCount();
				cursor.close();
				return count;
			}
		}
		return 0;
	}


	@Override
	public List<Wallpaper> getThemesByPage(int pageCountLimit, int page) {
		// TODO Auto-generated method stub
		return null;
	}

	
	

}
