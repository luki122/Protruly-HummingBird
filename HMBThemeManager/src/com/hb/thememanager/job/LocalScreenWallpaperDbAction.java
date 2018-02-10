package com.hb.thememanager.job;

import java.io.File;
import java.util.List;

import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;

public class LocalScreenWallpaperDbAction extends AbsThemeDatabaseTaskAction {

	private Theme mTheme;
	public LocalScreenWallpaperDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController) {
		super(loadListener, dbController);
		// TODO Auto-generated constructor stub
	}
	

	public LocalScreenWallpaperDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController, String themeFilePath) {
		super(loadListener, dbController, themeFilePath);
		// TODO Auto-generated constructor stub
	}
	public LocalScreenWallpaperDbAction(OnThemeLoadedListener<Theme> loadListener,
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
			writeLockscreenWallpaperIntoDb(listener, dbController);
		}else{
			readLockScreenWallpapersFromDb(listener, dbController);
		}
	
	}
	
	private void readLockScreenWallpapersFromDb(OnThemeLoadedListener<Theme> listener,
			ThemeDatabaseController<Theme> dbController){
		List<Theme> wallpapers = dbController.getThemes();
		if(wallpapers != null && wallpapers.size() > 0){
			for(Theme w : wallpapers){
				if(w.loaded == Theme.UNLOADED){
					continue;
				}
				if(listener != null){
					listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, (Wallpaper)w);
				}
			}
		}
	}
	
	private void writeLockscreenWallpaperIntoDb(OnThemeLoadedListener<Theme> listener,
			ThemeDatabaseController<Theme> dbController){
		final String themePath = getFilePath();
		File wallpaperFile = new File(themePath);
		if(!wallpaperFile.exists()){
			if(listener != null) {
				listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_NOT_EXISTS, null);
			}
			return;
		} else {
			if (wallpaperFile.isDirectory()){
				if( wallpaperFile
						.getAbsolutePath()
						.equals(Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH)) {
					File[] systemWallpapers = wallpaperFile.listFiles();
					if(systemWallpapers != null && systemWallpapers.length > 0){
						int index = -1;
						for(File sysW : systemWallpapers){
							Wallpaper wallpaper = new Wallpaper();
							index ++;
							wallpaper.id = Config.LocalId.LOCK_WALLPAPER_PREFIX+index;
							wallpaper.type = Wallpaper.LOCKSCREEN_WALLPAPER;
							wallpaper.isSystemTheme = Wallpaper.SYSTEM_THEME;
							wallpaper.applyStatus = Wallpaper.UN_APPLIED;
							wallpaper.loaded = Wallpaper.LOADED;
							wallpaper.themeFilePath = sysW.getAbsolutePath();
							wallpaper.loadedPath = sysW.getAbsolutePath();
							dbController.insertTheme(wallpaper);
						}
					}
					if(listener != null){
						listener.initialFinished(true,Theme.LOCKSCREEN_WALLPAPER);
					}
				}else{
					if(listener != null) {
						listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_FILE_ERROR, null);
					}
					return;
				}
			}else{
				dbController.insertTheme(mTheme);
			}
		}

	}

}
