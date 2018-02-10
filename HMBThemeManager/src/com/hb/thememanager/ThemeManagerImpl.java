package com.hb.thememanager;

import java.io.File;
import java.util.List;

import android.content.Context;

import com.hb.thememanager.job.Task;
import com.hb.thememanager.job.ThemeManagerTask;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;

public class ThemeManagerImpl implements ThemeManager {
	
	private static final Object mLock = new Object();
	
	private static  ThemeManager mInstance;
	
	private Task mThemeTask;
	
	private ThemeManagerImpl(Context context){
		
		mThemeTask = new ThemeManagerTask(context);
	}
	
	public static ThemeManager getInstance(Context context){
		synchronized (mLock) {
			if(mInstance == null){
				mInstance = new ThemeManagerImpl(context.getApplicationContext());
			}
			return mInstance;
		}
	}

	@Override
	public boolean applyTheme(Theme theme, Context context,StateManager stateManager) {
		// TODO Auto-generated method stub
		return mThemeTask.applyTheme(theme, context,stateManager);
	}

	@Override
	public boolean themeApplied(Theme theme) {
		// TODO Auto-generated method stub
		return mThemeTask.themeApplied(theme);
	}

	@Override
	public boolean updateThemeFromInternet(Theme theme) {
		// TODO Auto-generated method stub
		return mThemeTask.updateThemeFromInternet(theme);
	}

	@Override
	public boolean updateThemeinDatabase(Theme theme) {
		// TODO Auto-generated method stub
		return mThemeTask.updateThemeinDatabase(theme);
	}

	@Override
	public void deleteTheme(Theme theme) {
		// TODO Auto-generated method stub
		mThemeTask.deleteTheme(theme);
	}

	@Override
	public void deleteTheme(List<Theme> themes) {
		// TODO Auto-generated method stub
		mThemeTask.deleteTheme(themes);
	}

	@Override
	public void loadThemesFromDatabase(int themeType) {
		// TODO Auto-generated method stub
		 mThemeTask.loadThemesFromDatabase(themeType);
	}

	@Override
	public void loadThemeIntoDatabase(String themePath,int themeType) {
		// TODO Auto-generated method stub
		 mThemeTask.loadThemeIntoDatabase(themePath, themeType);
	}

	@Override
	public void loadThemeIntoDatabase(Theme theme) {
		mThemeTask.loadThemeIntoDatabase(theme);
	}

	@Override
	public void setThemeLoadListener(OnThemeLoadedListener listener) {
		// TODO Auto-generated method stub
		mThemeTask.setThemeLoadListener(listener);
	}

	@Override
	public void loadSystemThemeIntoDatabase(int themeType) {
		// TODO Auto-generated method stub
		mThemeTask.loadSystemThemeIntoDatabase(themeType);
	}

	@Override
	public boolean themeExists(Theme theme) {
		return mThemeTask.themeExists(theme);
	}

	@Override
	public boolean themeIsBuyByUser(Theme theme,int userId){
		return mThemeTask.themeIsBuyByUser(theme,userId);
	}
	@Override
	public void savePaiedThemeForUser(Theme theme,int userId){
		mThemeTask.savePaiedThemeForUser(theme,userId);
	}
}
