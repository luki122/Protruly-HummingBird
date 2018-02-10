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
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.database.ThemePkgDbController;
import com.hb.thememanager.job.apply.ThemeApplyTask;
import com.hb.thememanager.job.parser.LocalThemeParser;
import com.hb.thememanager.job.parser.ThemeParser;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.ThemeZip;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;
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
			stateManager.postState(ThemeState.STATE_APPLIED);
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
			return getAppliedThemeId(SharePreferenceManager.KEY_APPLIED_THEME_ID).equals(theme.id);
		case Theme.WALLPAPER:
			return false;					//   fix BUG #1034
		case Theme.LOCKSCREEN_WALLPAPER:
			return  false;
		case Theme.RINGTONE:
			
			return false;
		case Theme.FONTS:
			
			return getAppliedThemeId(SharePreferenceManager.KEY_APPLIED_FONT_ID).equals(theme.id);
		}
		return false;
	}

	@Override
	public boolean themeExists(Theme theme) {
		ThemeDatabaseController<Theme> controller = DatabaseFactory.createDatabaseController(theme.type,mContext);
		if(controller == null){
			return false;
		}
		if(TextUtils.isEmpty(theme.id)){
			return false;
		}
		Theme dbTheme = controller.getThemeById(theme.id);

		if(dbTheme == null){
			return false;
		}
		String themeFilePath = dbTheme.themeFilePath;
		boolean downloaded = dbTheme.downloadStatus == Theme.DOWNLOADED;
		File themeFile = new File(themeFilePath);
		theme.themeFilePath = themeFilePath;
		theme.buyStatus = dbTheme.buyStatus;
		return downloaded && themeFile.exists();
	}


	@Override
	public boolean themeIsBuyByUser(final Theme theme,final int userId){
		if(theme != null && userId != 0) {
					ThemeDatabaseController dbController = DatabaseFactory.createDatabaseController(
							Config.DatabaseColumns.THEME_USERS,mContext);
					Theme dbtheme = dbController.getThemeById(theme.id);
					if(dbtheme != null){
						if(dbtheme.userId == userId){
							theme.buyStatus = Theme.PAID;
							return true;
						}
					}else{
						return false;
					}
		}
		return false;
	}

	public void savePaiedThemeForUser(final Theme theme,final int userId){
		if(theme != null && userId != 0){
			MultiTaskDealer thread = MultiTaskDealer.startDealer("savePaiedThemeForUser", 1);
			thread.addTask(new Runnable() {
				@Override
				public void run() {
					ThemeDatabaseController dbController = DatabaseFactory.createDatabaseController(
							Config.DatabaseColumns.THEME_USERS,mContext);
					theme.userId = userId;
					dbController.insertTheme(theme);
					dbController.close();
				}
			});
		}
	}




	private String getAppliedThemeId(String saveType){
		return SharePreferenceManager.getStringPreference(mContext,saveType,"");
	}

	@Override
	public boolean updateThemeFromInternet(Theme theme) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateThemeinDatabase(Theme theme) {
		// TODO Auto-generated method stub

		ThemeDatabaseController dbController = DatabaseFactory.createDatabaseController(theme.type,mContext);
		Theme existTheme = dbController.getThemeById(theme.id);
		if(existTheme != null) {
			dbController.updateTheme(theme);
		}else{
			dbController.insertTheme(theme);
		}

		return false;
	}

	@Override
	public void deleteTheme(Theme theme) {
		// TODO Auto-generated method stub

		final Theme t = theme;
		if(t != null){
			MultiTaskDealer dealer = MultiTaskDealer.startDealer("delete_theme", 1);
				dealer.addTask(new Runnable() {
					@Override
					public void run() {
						ThemeDatabaseController<Theme> controller = DatabaseFactory
								.createDatabaseController(t.type,mContext);
						t.loaded = Theme.UNLOADED;
						controller.updateTheme(t);
						FileUtils.deleteFile(t.themeFilePath);
						FileUtils.deleteDirectory(t.loadedPath);

						controller.close();
					}
				});
		}
	}

	@Override
	public void deleteTheme(List<Theme> themes) {
		// TODO Auto-generated method stub

		final List<Theme> deleteThemes = themes;
		if(deleteThemes != null && deleteThemes.size() > 0){
			final Theme t = deleteThemes.get(0);
			MultiTaskDealer dealer = MultiTaskDealer.startDealer("delete_theme", 1);
				dealer.addTask(new Runnable() {
					@Override
					public void run() {
						ThemeDatabaseController<Theme> controller = DatabaseFactory
								.createDatabaseController(t.type,mContext);
						for(Theme tt : deleteThemes){
							tt.loaded = Theme.UNLOADED;
							controller.updateTheme(tt);
							FileUtils.deleteFile(tt.themeFilePath);
							FileUtils.deleteDirectory(tt.loadedPath);
							tt.loaded = Theme.UNLOADED;
							if (Theme.RINGTONE == tt.type) {
								Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
								scanIntent.setData(Uri.fromFile(new File(tt.themeFilePath)));
								mContext.sendBroadcast(scanIntent);
								controller.deleteTheme(tt);
							} else {
								controller.updateTheme(tt);
							}
						}
						controller.close();
					}
				});
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
		}else if(type == Theme.FONTS ){
			loadFontFromDatabase(dbController);
		}else if(type == Theme.RINGTONE){
			loadRingtoneFromDatabase(dbController);
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

	private void loadFontFromDatabase(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_font_from_database", 2);
		AbsThemeDatabaseTaskAction thread = new FontDbAction(mThemeLoadListener, dbController);
		thread.setActionFlag(AbsThemeDatabaseTaskAction.FLAG_READ);
		dealer.addTask(thread);
	}

	private void loadRingtoneFromDatabase(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_ringtone_from_database", 2);
		AbsThemeDatabaseTaskAction thread = new RingtoneDbAction(mThemeLoadListener, dbController);
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
		}else if(themeType == Theme.FONTS){
			loadFontIntoDatabase(dbController,themePath);
		}
		
	}

	@Override
	public void loadThemeIntoDatabase(Theme theme) {
		final ThemeDatabaseController<Theme> dbController = DatabaseFactory.createDatabaseController(theme.type, mContext);
		if(theme.type == Theme.THEME_PKG){
			loadThemePkgIntoDatabase(dbController,theme);
		}else if(theme.type == Theme.WALLPAPER){
			loadDesktopWallpaperIntoDatabase(dbController, theme);
		}else if(theme.type == Theme.LOCKSCREEN_WALLPAPER){
			loadLockScreenWallpaperIntoDatabase(dbController, theme);
		}else if(theme.type == Theme.FONTS){
			loadFontIntoDatabase(dbController, theme);
		}
	}

	private void loadFontIntoDatabase(final ThemeDatabaseController<Theme> dbController
			,final String themePath){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_font_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new FontDbAction(mThemeLoadListener, dbController, themePath);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(LocalScreenWallpaperDbAction.FLAG_WRITE);
		thread.setUserImport(true);
		task.addTask(thread);
	}

	private void loadFontIntoDatabase(final ThemeDatabaseController<Theme> dbController
			,Theme theme){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_font_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new FontDbAction(mThemeLoadListener, dbController, theme);
		thread.setActionFlag(LocalScreenWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadDesktopWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController
			, final String themePath){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_wallpaper_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new DesktopWallpaperDbAction(mThemeLoadListener, dbController, themePath);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(DesktopWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadLockScreenWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController
			,final String themePath){
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
		thread.setUserImport(true);
		task.addTask(thread);
	
	}




	private void loadDesktopWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController
			, Theme theme){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_wallpaper_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new DesktopWallpaperDbAction(mThemeLoadListener, dbController, theme);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(DesktopWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadLockScreenWallpaperIntoDatabase(final ThemeDatabaseController<Theme> dbController
			, Theme theme){
		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_wallpaper_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new LocalScreenWallpaperDbAction(mThemeLoadListener, dbController, theme);
		thread.openTransactionIfNeed(true);
		thread.setActionFlag(LocalScreenWallpaperDbAction.FLAG_WRITE);
		task.addTask(thread);
	}

	private void loadThemePkgIntoDatabase(final ThemeDatabaseController<Theme> dbController, Theme theme){

		MultiTaskDealer task = MultiTaskDealer.startDealer("load_local_theme_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new ThemePackageDbAction(mThemeLoadListener, dbController,theme);
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
		if(dbController.getCount() == 0){
			if(type == Theme.THEME_PKG){
				loadSystemThemeIntoDatabaseInner(dbController);
			}else if(type == Theme.WALLPAPER){
				loadDesktopWallpaperIntoDatabase(dbController, Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH);
			}else if(type == Theme.LOCKSCREEN_WALLPAPER){
				loadLockScreenWallpaperIntoDatabase(dbController, Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH);
			}else if(type == Theme.FONTS){
				loadSystemFontsIntoDatabaseInner(dbController);
			}

		}
		loadThemesFromDatabase(themeType);

	}
	
	private void loadSystemThemeIntoDatabaseInner(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_system_theme_into_database", 2);
		AbsThemeDatabaseTaskAction thread = new ThemePackageDbAction(mThemeLoadListener, dbController);
		thread.setActionFlag(ThemePackageDbAction.FLAG_WRITE);
		thread.openTransactionIfNeed(true);
		dealer.addTask(thread);
	}

	private void loadSystemFontsIntoDatabaseInner(final ThemeDatabaseController<Theme> dbController){
		MultiTaskDealer dealer = MultiTaskDealer.startDealer("load_local_font_into_db", 2);
		AbsThemeDatabaseTaskAction thread = new FontDbAction(mThemeLoadListener, dbController);
		thread.setActionFlag(ThemePackageDbAction.FLAG_WRITE);
		thread.openTransactionIfNeed(true);
		dealer.addTask(thread);
	}
	
	
}
	
	

