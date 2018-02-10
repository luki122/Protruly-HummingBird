package com.hb.thememanager;

import java.io.File;

import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageCache;
import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.job.MultiTaskDealer;
import com.hb.thememanager.model.Theme;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.thememanager.model.User;
import com.hb.thememanager.model.getUserInfoCallBack;
import com.hb.thememanager.ui.HomePage;
import com.hb.thememanager.ui.MainActivity;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;
import com.alipay.sdk.app.EnvUtils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import com.hb.thememanager.R;
public class ThemeManagerApplication extends Application implements Application.ActivityLifecycleCallbacks{
	
	private static final float IMAGE_CACHE_PERCENT = 0.2f;
	private static final String IMAGE_DISK_CACHE_DIR = "thememanager";
	private ThemeManager mThemeManager;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		registerActivityLifecycleCallbacks(this);
		getThemeManager();
		createThemeDirs();
		ImageCache.ImageCacheParams sImageCacheParams = new ImageCache.ImageCacheParams(getApplicationContext(),
				IMAGE_DISK_CACHE_DIR);
		sImageCacheParams.setMemCacheSizePercent(IMAGE_CACHE_PERCENT);
		HbImageLoader.setupImageCacheParam(sImageCacheParams);
        	EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX);
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
		
		mThemeManager.loadSystemThemeIntoDatabase(Theme.THEME_PKG);
	}

	public void loadInternalTheme(int themeType){
		mThemeManager.loadSystemThemeIntoDatabase(themeType);
	}
	
	public void loadInternalWallpaper(){
		mThemeManager.loadSystemThemeIntoDatabase(Theme.WALLPAPER);
	}
	
	public void loadInternalLockScreenWallpaper(){
		mThemeManager.loadSystemThemeIntoDatabase(Theme.LOCKSCREEN_WALLPAPER);
	}
	
	public void loadInternalFonts(){
		
	}
	
	public void loadInternalRingtone(){
		mThemeManager.loadThemesFromDatabase(Theme.RINGTONE);
	}
	
	public ThemeManager getThemeManager(){
		if(mThemeManager == null){
			mThemeManager = ThemeManagerImpl.getInstance(this);
		}
		return mThemeManager;
	}

	private void initialUserInfo(final Activity activity){
		User user = User.getInstance(getApplicationContext());
		if(user.isLogin()){
			startCheckUserThemeService(activity);
			return;
		}

		if(user.isSaveLogin()){
			user.jumpLogin(new getUserInfoCallBack() {
				@Override
				public void getUserInfoSuccess() {
					startCheckUserThemeService(activity);
				}
			}, activity);
		}
	}

	private void startCheckUserThemeService(Context context){
		Intent intent = new Intent(context,CheckUserThemesService.class);
		startService(intent);
	}


	@Override
	public void onActivityCreated(Activity activity, Bundle bundle) {
			if(activity instanceof HomePage || activity instanceof MainActivity){
				initialUserInfo(activity);
			}
	}

	@Override
	public void onActivityStarted(Activity activity) {

	}

	@Override
	public void onActivityResumed(Activity activity) {

	}

	@Override
	public void onActivityPaused(Activity activity) {

	}

	@Override
	public void onActivityStopped(Activity activity) {

	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

	}

	@Override
	public void onActivityDestroyed(Activity activity) {

	}
}
