package com.hb.thememanager.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.job.apply.ThemeApplyTask;
import com.hb.thememanager.job.parser.LocalThemeParser;
import com.hb.thememanager.job.parser.ThemeParser;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.ThemeZip;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.utils.ArrayUtils;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.security.SecurityManager;
import com.hb.thememanager.utils.FileUtils;

public class ThemeManagerTask  implements Task{
	private static final String TAG = "ThemeManagerTask";

    
	private OnThemeLoadedListener<Theme> mThemeLoadListener;
	private Context mContext;
	public ThemeManagerTask(Context context){
		mContext = context;
	}

	@Override
	public boolean applyTheme(Theme theme, Context context,StateManager stateManager) {
		// TODO Auto-generated method stub
		if(theme == null){
			return false;
		}
		if(themeApplied(theme)){
			stateManager.postState(State.STATE_APPLIED);
			return false;
		}
		Intent applyIntent = new Intent();
		applyIntent.setClass(context,ThemeApplyTask.class);
		applyIntent.putExtra(Config.ActionKey.KEY_APPLY_THEME_IN_SERVICE,theme);
		ThemeApplyTask.attachState(stateManager);
		context.startService(applyIntent);
		return true;
	}

	@Override
	public boolean themeApplied(Theme theme) {
		// TODO Auto-generated method stub
		if(theme == null){
			return false;
		}
		switch (theme.type) {
		case Theme.THEME_PKG:
			return getAppliedThemeId(mContext) == theme.id;
		case Theme.WALLPAPER:
//			return SharePreferenceManager
//					.getStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, "")
//					.equals(theme.themeFilePath);
			return false;					//   fix BUG #1034
		case Theme.LOCKSCREEN_WALLPAPER:
			return /*SharePreferenceManager
					.getStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, "")
					.equals(theme.themeFilePath)*/ false;	
		case Theme.RINGTONG:
			
			return getAppliedRingTongId(mContext) == theme.id;
		case Theme.FONTS:
			
			return getAppliedFontsId(mContext) == theme.id;
		}
		return false;
	}
	

	@Override
	public int getAppliedThemeId(Context context) {
		// TODO Auto-generated method stub
		return SharePreferenceManager.getIntPreference(context, SharePreferenceManager.KEY_APPLIED_THEME_ID, -1);
	}

	@Override
	public int getAppliedWallpaperId(Context context) {
		// TODO Auto-generated method stub
		return SharePreferenceManager.getIntPreference(context, SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, -1);
	}

	@Override
	public int getAppliedFontsId(Context context) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAppliedRingTongId(Context context) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean updateThemeFromInternet(Theme theme) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateThemeinDatabase(Theme theme) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void deleteTheme(Theme theme) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteTheme(List<Theme> themes) {
		// TODO Auto-generated method stub

		final List<Theme> deleteThemes = themes;
		if(deleteThemes != null && deleteThemes.size() > 0){
			final Theme t = deleteThemes.get(0);
			if(t.type == Theme.WALLPAPER || t.type == Theme.LOCKSCREEN_WALLPAPER){
				MultiTaskDealer dealer = MultiTaskDealer.startDealer("delete_wallpaper", 1);
				dealer.addTask(new Runnable() {
					@Override
					public void run() {
						ThemeDatabaseController<Theme> controller = DatabaseFactory
								.createDatabaseController(t.type,mContext);
						for(Theme tt : deleteThemes){
							controller.deleteTheme(tt);
							FileUtils.deleteFile(tt.themeFilePath);
						}

						controller.close();
					}
				});
			}
		}


	}

	@Override
	public void loadThemesFromDatabase(int themeType) {
		// TODO Auto-generated method stub
		if(themeType == Theme.THEME_NULL){
			return;
		}
		final int type = themeType;
		final ThemeDatabaseController<Theme> dbController = DatabaseFactory.createDatabaseController(type, mContext);
		if(type == Theme.THEME_PKG){
			loadThemesFromDatabaseInner(dbController);
		}else if(type == Theme.WALLPAPER ||type == Theme.LOCKSCREEN_WALLPAPER ){
			loadWallpaperFromDatabase(dbController,type);
		}else{
			//do nothing now
		}
	}
	
	private void loadWallpaperFromDatabase(final ThemeDatabaseController<Theme> dbController,int type){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_wallpaper_from_database", 2);
		AbsThemeDatabaseTaskAction thread;
		if(type == Theme.LOCKSCREEN_WALLPAPER){
			thread = new LocalScreenWallpaperDbAction(mThemeLoadListener, dbController);
		}else{
			thread = new DesktopWallpaperDbAction(mThemeLoadListener, dbController);
		}
		thread.setActionFlag(AbsThemeDatabaseTaskAction.FLAG_READ);
		dealer.addTask(thread);
	}

	private void loadThemesFromDatabaseInner(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_theme_from_database", 2);
		AbsThemeDatabaseTaskAction thread = new ThemePackageDbAction(mThemeLoadListener, dbController);
		thread.setActionFlag(ThemePackageDbAction.FLAG_READ);
		dealer.addTask(thread);
	}
	
	@Override
	public void loadThemeIntoDatabase(final String themePath, int themeType) {
		// TODO Auto-generated method stub
		final ThemeDatabaseController<Theme> dbController = DatabaseFactory.createDatabaseController(themeType, mContext);
		if(themeType == Theme.THEME_PKG){
			loadThemePkgIntoDatabase(dbController, themePath);
		}else if(themeType == Theme.WALLPAPER){
			loadDesktopWallpaperIntoDatabase(dbController, themePath);
		}else if(themeType == Theme.LOCKSCREEN_WALLPAPER){

			loadLockScreenWallpaperIntoDatabase(dbController, themePath);
		}
		
	}
	
	private void loadDesktopWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController,final String themePath){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_wallpaper_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new DesktopWallpaperDbAction(mThemeLoadListener, dbController, themePath);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(DesktopWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadLockScreenWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController,final String themePath){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_wallpaper_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new LocalScreenWallpaperDbAction(mThemeLoadListener, dbController, themePath);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(LocalScreenWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadThemePkgIntoDatabase(final ThemeDatabaseController<Theme> dbController,final String themePath){

		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new ThemePackageDbAction(mThemeLoadListener, dbController,themePath);
		thread.setActionFlag(ThemePackageDbAction.FLAG_WRITE);
		task.addTask(thread);
	
	}

	@Override
	public void setThemeLoadListener(OnThemeLoadedListener listener) {
		// TODO Auto-generated method stub
		mThemeLoadListener = listener;
	}


	@Override
	public void loadSystemThemeIntoDatabase(int themeType) {
		// TODO Auto-generated method stub
		final int type = themeType;
		ThemeDatabaseController<Theme> dbController = DatabaseFactory.createDatabaseController(type, mContext);
		if(type == Theme.THEME_PKG){
		loadSystemThemeIntoDatabaseInner(dbController);
		}else if(type == Theme.WALLPAPER){
			if(dbController.getCount() == 0){
				loadDesktopWallpaperIntoDatabase(dbController, Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH);
			}else{
				if(mThemeLoadListener != null){
					mThemeLoadListener.initialFinished(true,Theme.WALLPAPER);
				}
			}
		}else if(type == Theme.LOCKSCREEN_WALLPAPER){
			if(dbController.getCount() == 0){
				loadLockScreenWallpaperIntoDatabase(dbController, Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH);
			}else{
				if(mThemeLoadListener != null){
					mThemeLoadListener.initialFinished(true,Theme.LOCKSCREEN_WALLPAPER);
				}
			}
		}
	}
	
	private void loadSystemThemeIntoDatabaseInner(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_system_theme_into_database", 2);
		AbsThemeDatabaseTaskAction thread = new ThemePackageDbAction(mThemeLoadListener, dbController);
		thread.setActionFlag(ThemePackageDbAction.FLAG_WRITE);
		thread.openTransactionIfNeed(true);
		dealer.addTask(thread);
	}
	
	
}
	
	
