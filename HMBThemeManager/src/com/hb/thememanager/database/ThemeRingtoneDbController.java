package com.hb.thememanager.database;

import android.content.Context;

import com.hb.thememanager.model.Ringtone;

public class ThemeRingtoneDbController extends ThemeDatabaseController<Ringtone> {

	public  ThemeRingtoneDbController(Context context,int themeType) {
		super(context,themeType);
		// TODO Auto-generated constructor stub
		
	}

	@Override
	protected Ringtone createTypeInstance() {
		return new Ringtone();
	}


}
