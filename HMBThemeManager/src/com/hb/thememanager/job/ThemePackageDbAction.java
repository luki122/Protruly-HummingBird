package com.hb.thememanager.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.job.parser.LocalThemeParser;
import com.hb.thememanager.job.parser.ThemeParser;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.ThemeZip;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.security.SecurityManager;
import com.hb.thememanager.utils.ArrayUtils;
import com.hb.thememanager.utils.Config;

public class ThemePackageDbAction extends AbsThemeDatabaseTaskAction {

	private static final String TAG = "Load_Theme_Pkg";

	public ThemePackageDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController) {
		super(loadListener, dbController);
		// TODO Auto-generated constructor stub
	}
	
	

	public ThemePackageDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController, String themeFilePath) {
		super(loadListener, dbController, themeFilePath);
		// TODO Auto-generated constructor stub
	}



	@Override
	protected void doJob() {
		// TODO Auto-generated method stub
		final OnThemeLoadedListener<Theme> listener = getListener();
		final ThemeDatabaseController<Theme> dbController = getDatabaseController();
		if(getActionFlag() == FLAG_WRITE){
			//If path of theme is not empty,load theme package
			if(!TextUtils.isEmpty(getFilePath())){
				writeSingleThemePackageIntoDb(listener, dbController, getFilePath());
			}else{
				writeThemePkgIntoDb(listener, dbController);
			}
		}else{
			readThemePkgFromDb(listener, dbController);
		}
	}
	
	private void readThemePkgFromDb(OnThemeLoadedListener<Theme> loadListener
			,ThemeDatabaseController<Theme> dbController){

		
		Theme defaultTheme = new Theme();
		defaultTheme.type = Theme.THEME_PKG;
		defaultTheme.id = Config.DEFAULT_THEME_ID;
		defaultTheme.loaded = Theme.LOADED;
		defaultTheme.themeFilePath = Config.SYSTEM_THEME_DIR;
		defaultTheme.isSystemTheme = Theme.SYSTEM_THEME;
		defaultTheme.applyStatus = Theme.APPLIED;
		if(loadListener != null){
			loadListener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, defaultTheme);
		}
		List<Theme> themes = dbController.getThemes();
		if(themes != null && themes.size() > 0){
			if(loadListener != null){
				for(Theme t:themes){
					loadListener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, t);
				}
			}
		}
	
	}
	
	private void writeSingleThemePackageIntoDb(OnThemeLoadedListener<Theme> loadListener
			,final ThemeDatabaseController<Theme> dbController
			,final String themePath){

		// TODO Auto-generated method stub
		
		/*
		 * 先对目标文件进行检查看是不是正常的主题包，如果不是给出提示并直接返回
		 */
		
		if(!SecurityManager.checkThemePackage(themePath)){
			if(loadListener != null){
				loadListener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_FILE_ERROR, null);
			}
			return;
		}
		
		ThemeParser<Theme, InputStream> themeParser = new LocalThemeParser();
		ThemeZip themeZip = null;
		try {
			themeZip = new ThemeZip(new File(themePath));
		} catch (ZipException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			
			InputStream input = null;
			ZipEntry entry = themeZip.getEntry(Config.LOCAL_THEME_DESCRIPTION_FILE_NAME);
			if (entry != null) {
				input = themeZip.getInputStream(entry);
			}
				Theme theme = themeParser.parser(input);
				if(theme != null){
					//加载之后存放的路径采用主题名生成的MD5码
					final String loadedDir = MD5Utils.encryptString(theme.name);
					theme.type = Theme.THEME_PKG;
					theme.previewArrays = themeZip.getPreviewCache();
					theme.wallpaperArrays = themeZip.getWallpaperCache();
					theme.themeZipFile = themeZip;
					theme.loadedPath = Config.LOCAL_THEME_PACKAGE_INFO_DIR+loadedDir;
					theme.applyStatus = Theme.UN_APPLIED;
					theme.isSystemTheme = Theme.LOCAL_THEME;
					theme.loaded = Theme.LOADED;
					theme.themeFilePath = themePath;
					themeZip.loadInfo(loadedDir);
					if(SecurityManager.checkThemeExists(theme,dbController)){
						if(loadListener != null){
							loadListener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_NOT_EXISTS, theme);
						}
						return;
					}
					dbController.insertTheme(theme);
					if(loadListener != null){
						loadListener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, theme);
					}
				}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(themeZip != null){
				try {
					themeZip.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	
	}
	
	private void writeThemePkgIntoDb(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController){

		// TODO Auto-generated method stub
		ArrayList<Theme> themes = new ArrayList<Theme>();
		int count = dbController.getCount();
		if(count == 0){
			File systemDirFile = new File(Config.SYSTEM_THEME_INFO_DIR);
			File[] themeFiles = systemDirFile.listFiles();
			
			if(ArrayUtils.isEmpty(themeFiles)){
				if(loadListener != null){
					loadListener.initialFinished(true,Theme.THEME_PKG);
				}
				return;
			}
			
			ThemeParser<Theme, InputStream> parser = new LocalThemeParser();
			for(File file:themeFiles){
				String parentPath = file.getAbsolutePath();
				int nameIndex = parentPath.lastIndexOf("/");
				String name = "";
				if(nameIndex != -1){
					name = parentPath.substring(nameIndex,parentPath.length());
				}
				String descriptionXml = parentPath+File.separatorChar+Config.LOCAL_THEME_DESCRIPTION_FILE_NAME;
				try{
				FileInputStream descriptionStream = new FileInputStream(descriptionXml);
				Theme theme = parser.parser(descriptionStream);
				if(theme != null){
					 theme.loadedPath = parentPath;
					 theme.type = Theme.THEME_PKG;
					 theme.loaded = Theme.LOADED;
					 theme.applyStatus = Theme.UN_APPLIED;
					 theme.lastModifiedTime = System.currentTimeMillis();
					 theme.themeFilePath = Config.SYSTEM_THEME_DIR+name;
					 theme.isSystemTheme = Theme.SYSTEM_THEME;
					 theme.applyStatus = Theme.UN_APPLIED;
					 dbController.insertTheme(theme);
				}
				}catch(Exception e){
					//do nothing
					Log.e(TAG, "load system theme catch exception-->"+e);
				}
			}
		}
		
		if(loadListener != null){
			loadListener.initialFinished(true,Theme.THEME_PKG);
		}
	
	}
	

}
