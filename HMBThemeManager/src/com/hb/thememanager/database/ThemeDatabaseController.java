package com.hb.thememanager.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;


public abstract class ThemeDatabaseController<T extends Theme> implements Config.DatabaseColumns {

	
	private ThemeDataBaseHelper mHelper;
	
	private SQLiteDatabase mDb;

	private int mThemeType;
	
	
	public  ThemeDatabaseController(Context context,int themeType) {
		// TODO Auto-generated constructor stub
		mHelper = new ThemeDataBaseHelper(context,themeType);
		mThemeType = themeType;
	}

	protected int getThemeType(){
		return mThemeType;
	}
	
	protected String getTableName(){
		return mHelper.getTable();
	}
	
	protected SQLiteDatabase getDatabase(){
		open();
		return mDb;
	}
	
	public void close() {
		// TODO Auto-generated method stub
		if (mDb != null && mDb.isOpen()) {
			mDb.close();
		}
	}

	public void beginTransaction(){
		if(mDb == null){
			mDb = mHelper.getWritableDatabase();
		}
		mDb.beginTransaction();
	}
	
	public void endTransaction(){
		if(mDb == null){
			mDb = mHelper.getWritableDatabase();
		}
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
	}
	
	
	public Cursor query(String[] columns, String selection, String[] 
			 selectionArgs, String groupBy, String having, String orderBy){
		open();
		return mDb.query(mHelper.getTable(), columns, selection, selectionArgs, groupBy, having, orderBy);
	}

	protected void open() {
		// TODO Auto-generated method stub
		if(mDb == null || !mDb.isOpen()){
			mDb = mHelper.getWritableDatabase();
		}
		
	}
	
	protected long insert(ContentValues values){
		return mHelper.insert(getDatabase(), values);
	}
	
	protected int delete(String whereClause, String[] whereArgs){
		int result = getDatabase().delete(getTableName(), whereClause, whereArgs);
		close();
		return result;
	}
	
	protected int update(ContentValues values, String whereClause, String[] 
			 whereArgs){
		int result = getDatabase().update(getTableName(), values, whereClause, whereArgs);
		close();
		return result;
	}
	
	public T getThemeById(String themeId){
		T theme = null;
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, Config.DatabaseColumns._ID + "=?", new String[]{themeId}, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {

				if (cursor.moveToNext()) {
					theme = createTypeInstance();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getString(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED));
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.themeFilePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));
					theme.type = getThemeType();
					getThemesOtherStatus(theme,cursor);
				}
			}
			cursor.close();
		}
		return theme;
	}
	
	public T getThemeByPath(String filePath){
		T theme = null;
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, Config.DatabaseColumns.FILE_PATH + "=?", new String[]{filePath}, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {

				if (cursor.moveToNext()) {
					theme = createTypeInstance();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getString(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED));
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.themeFilePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));
					theme.type = getThemeType();
					getThemesOtherStatus(theme,cursor);
				}
			}
			cursor.close();
		}
		return theme;
	}
	
	public List<T> getThemes(){
		// TODO Auto-generated method stub
		ArrayList<T> themes = new ArrayList<T>();
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					T theme = createTypeInstance();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.themeFilePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getString(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED)) ;
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));

					getThemesOtherStatus(theme,cursor);
					themes.add(theme);
				}
			}
			cursor.close();
		}
		return themes;
	}

	protected abstract T createTypeInstance();

	protected void getThemesOtherStatus(T theme,Cursor cursor){

	}
	
	public List<T> getThemesByPage(int pageCountLimit,int page){
		return null;
	}
	
	public boolean isLoaded(String themeId){
		return false;
	}
	
	public boolean updateTheme(T theme){
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

			values.put(Config.DatabaseColumns.IS_SYSTEM_THEME,theme.isSystemTheme);
			values.put(Config.DatabaseColumns.SIZE, theme.size);
			updateThemeOtherStatus(theme,values);
			update( values, Config.DatabaseColumns._ID + "=?", new String[]{theme.id});
		}
		return true;
	}

	protected void updateThemeOtherStatus(T theme,ContentValues values){

	}

	
	public boolean deleteTheme(T t){
		return deleteTheme(t.id);
	}
	
	public boolean deleteTheme(String themeId){
		return delete(Config.DatabaseColumns._ID + "=?"
				, new String[]{themeId}) != 0;
	}
	
	public  void insertTheme(T theme){
		// TODO Auto-generated method stub
		if (getDatabase() != null) {
			ContentValues values = new ContentValues();
			values.put(Config.DatabaseColumns._ID,theme.id);
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

			values.put(Config.DatabaseColumns.IS_SYSTEM_THEME,theme.isSystemTheme);
			values.put(Config.DatabaseColumns.SIZE, theme.size);
			insertThemeOtherStatus(theme,values);
			long ok = insert(values);
		}

	}

	protected void insertThemeOtherStatus(T theme,ContentValues values){

	}

	public List<T> getSystemTheme(){
		// TODO Auto-generated method stub
		ArrayList<T> themes = new ArrayList<T>();
		if (getDatabase() != null) {
			Cursor cursor = getDatabase().query(getTableName(),
					null, Config.DatabaseColumns.IS_SYSTEM_THEME + "=?", new String[]{String.valueOf(Theme.SYSTEM_THEME)}, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					T theme = createTypeInstance();
					theme.name = cursor.getString(cursor.getColumnIndex(NAME));
					theme.themeFilePath = cursor.getString(cursor.getColumnIndex(FILE_PATH));
					theme.description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
					theme.designer = cursor.getString(cursor.getColumnIndex(DESGINER));
					theme.downloadStatus = cursor.getInt(cursor.getColumnIndex(DOWNLOAD_STATUS));
					theme.downloadUrl = cursor.getString(cursor.getColumnIndex(URI));
					theme.id = cursor.getString(cursor.getColumnIndex(_ID));
					theme.lastModifiedTime = cursor.getLong(cursor.getColumnIndex(LAST_MODIFIED_TIME));
					theme.loaded = cursor.getInt(cursor.getColumnIndex(LOADED)) ;
					theme.loadedPath = cursor.getString(cursor.getColumnIndex(LOADED_PATH));
					theme.version = cursor.getString(cursor.getColumnIndex(VERSION));
					theme.isSystemTheme = cursor.getInt(cursor.getColumnIndex(IS_SYSTEM_THEME));
					theme.size = cursor.getString(cursor.getColumnIndex(SIZE));
					getThemesOtherStatus(theme,cursor);
					themes.add(theme);
				}
			}
			cursor.close();
		}
		return themes;
	}

	
	public  int getCount(){
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

	public int getLastThemeId(){

		return 0;
	}

	public List<T> getThemesByUser(int userId){
		return  null;
	}
	
}

