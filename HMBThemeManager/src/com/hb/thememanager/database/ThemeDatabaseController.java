package com.hb.thememanager.database;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;


public abstract class ThemeDatabaseController<T extends Theme> {

	
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
	
	public abstract T getThemeById(int themeId);
	
	public abstract List<T> getThemes();
	
	public abstract List<T> getThemesByPage(int pageCountLimit,int page);
	
	public abstract boolean isLoaded(int themeId);
	
	public abstract boolean updateTheme(T t);
	
	public abstract boolean deleteTheme(T t);
	
	public abstract boolean deleteTheme(int themeId);
	
	public abstract void insertTheme(T theme);
	

	
	public abstract int getCount();
	
	
	
}
