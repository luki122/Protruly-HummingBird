package com.hb.thememanager.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.Config.DatabaseColumns;

public class ThemePkgDbController  extends ThemeDatabaseController<Theme>  {
	
	public  ThemePkgDbController(Context context,int themeType) {
		super(context,themeType);
		// TODO Auto-generated constructor stub
		
	}


	@Override
	protected Theme createTypeInstance() {
		return new Theme();
	}

	@Override
	protected void updateThemeOtherStatus(Theme theme, ContentValues values) {
		values.put(PRICE,theme.price);
		values.put(PAID,theme.buyStatus);
		values.put(HAS_NEW_VERSION,theme.hasNewVersion);
		values.put(IS_CHARGE,theme.isCharge);
		values.put(DESIGNER_ID,theme.designerId);
	}

	@Override
	protected void insertThemeOtherStatus(Theme theme, ContentValues values) {
		values.put(PRICE,theme.price);
		values.put(PAID,theme.buyStatus);
		values.put(HAS_NEW_VERSION,theme.hasNewVersion);
		values.put(IS_CHARGE,theme.isCharge);
		values.put(DESIGNER_ID,theme.designerId);
	}


	@Override
	protected void getThemesOtherStatus(Theme theme, Cursor cursor) {
		theme.price = cursor.getString(cursor.getColumnIndex(PRICE));
		theme.buyStatus = cursor.getInt(cursor.getColumnIndex(PAID));
		theme.hasNewVersion = cursor.getInt(cursor.getColumnIndex(HAS_NEW_VERSION));
		theme.isCharge = cursor.getInt(cursor.getColumnIndex(IS_CHARGE));
		theme.designerId = cursor.getInt(cursor.getColumnIndex(DESIGNER_ID));

	}




}
