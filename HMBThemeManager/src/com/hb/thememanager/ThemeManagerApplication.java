package com.hb.thememanager;

import java.io.File;

import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageCache;
import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.job.MultiTaskDealer;
import com.hb.thememanager.model.Theme;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;

import android.app.Application;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import com.hb.thememanager.R;
public class ThemeManagerApplication extends Application {
	
	private static final float IMAGE_CACHE_PERCENT = 0.2f;
	private static final String IMAGE_DISK_CACHE_DIR = "thememanager";
	private ThemeManager mThemeManager;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		getThemeManager();
		createThemeDirs();
		ImageCache.ImageCacheParams sImageCacheParams = new ImageCache.ImageCacheParams(getApplicationContext(),
				IMAGE_DISK_CACHE_DIR);
		sImageCacheParams.setMemCacheSizePercent(IMAGE_CACHE_PERCENT);
		HbImageLoader.setupImageCacheParam(sImageCacheParams);
	}
	
	private void createThemeDirs(){
		FileUtils.createDirectory(Config.LOCAL_THEME_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_PACKAGE_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_RINGTONG_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_WALLPAPER_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_FONTS_PATH);
		
		FileUtils.createDirectory(Config.THEME_ROOT_DIR);
		FileUtils.createDirectory(Config.THEME_APPLY_DIR);
		FileUtils.createDirectory(Config.THEME_APPLY_BACKUP_DIR);
		FileUtils.createDirectory(Config.THEME_APPLY_BACKUP_DIR);
		
		CommonUtil.chmodFile(Config.THEME_ROOT_DIR);
		
	}
	
	




	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
		HbImageLoader imageLoader = HbImageLoader.getInstance(getApplicationContext());
		imageLoader.clearMemoryCache();
	}
	public void loadInternalTheme(){
		
		File systemThemeLoadDir = new File(Config.SYSTEM_THEME_INFO_DIR);
//		if(!systemThemeLoadDir.exists()){
//			return;
//		}
		mThemeManager.loadSystemThemeIntoDatabase(Theme.THEME_PKG);
	}
	
	public void loadInternalWallpaper(){
		mThemeManager.loadSystemThemeIntoDatabase(Theme.WALLPAPER);
	}
	
	public void loadInternalLockScreenWallpaper(){
		mThemeManager.loadSystemThemeIntoDatabase(Theme.LOCKSCREEN_WALLPAPER);
	}
	
	public void loadInternalFonts(){
		
	}
	
	public void loadInternalRingTong(){
		
	}
	
	public ThemeManager getThemeManager(){
		if(mThemeManager == null){
			mThemeManager = ThemeManagerImpl.getInstance(this);
		}
		return mThemeManager;
	}
	
}
