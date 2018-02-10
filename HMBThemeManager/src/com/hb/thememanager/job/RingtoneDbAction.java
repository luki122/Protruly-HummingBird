package com.hb.thememanager.job;

import android.util.Log;

import java.io.File;
import java.util.List;

import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Ringtone;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;

public class RingtoneDbAction extends AbsThemeDatabaseTaskAction {

	private Theme mTheme;

	public RingtoneDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController) {
		super(loadListener, dbController);
		// TODO Auto-generated constructor stub
	}
	

	public RingtoneDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController, String themeFilePath) {
		super(loadListener, dbController, themeFilePath);
		// TODO Auto-generated constructor stub
	}


	public RingtoneDbAction(OnThemeLoadedListener<Theme> loadListener,
									ThemeDatabaseController<Theme> dbController, Theme theme) {
		super(loadListener, dbController, theme);
		mTheme = theme;
	}

	@Override
	protected void doJob() {
		final int flag = getActionFlag();
		final ThemeDatabaseController<Theme> dbController = getDatabaseController();
		final OnThemeLoadedListener<Theme> listener = getListener();
		if(flag == FLAG_WRITE){
			Log.e("huliang",  "can't write ringtone DB");
		}else{
			readRingtoneFromDb(listener, dbController);
		}
	
	}
	
	private void readRingtoneFromDb(OnThemeLoadedListener<Theme> listener,
			ThemeDatabaseController<Theme> dbController){
		List<Theme> ringtones = dbController.getThemes();
		if(ringtones != null && ringtones.size() > 0){
			for(Theme R : ringtones){
				if(listener != null){
					listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, (Ringtone)R);
				}
			}
		}
	}

}
