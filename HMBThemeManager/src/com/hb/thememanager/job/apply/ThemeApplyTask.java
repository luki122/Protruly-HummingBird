package com.hb.thememanager.job.apply;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.DensityUtils;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.WallpaperUtils;

public class ThemeApplyTask  extends IntentService{

	private static final String TAG = "ThemeManager";
	private static final int MAX_TIME_LIMIT = 200;
	private static final boolean DBG = false;
	private Theme mTheme;
	private static StateManager sStateManager;
	private Context mContext;
	private long mStartTime;
	private long mEndTime;
	private Handler mHandler;
	private boolean mReStartLauncher = false;

	public ThemeApplyTask(){
		super(TAG);
		mHandler = new Handler();
	}


	public ThemeApplyTask(String name) {
		super(name);
		mHandler = new Handler();
	}


	public static void attachState(StateManager stateManager){
		sStateManager = stateManager;
	}



	@Override
	protected void onHandleIntent(Intent intent) {
		mContext = getApplicationContext();
		String action = intent.getAction();
		mTheme = intent.getParcelableExtra(Config.ActionKey.KEY_APPLY_THEME_IN_SERVICE);
		mStartTime = 0L;
		mEndTime = 0L;
		switch (mTheme.type) {
			case Theme.THEME_PKG:
				applyPkgTheme();
				break;
			case Theme.WALLPAPER:
				applyWallpaperTheme();
				break;
			case Theme.RINGTONG:
				applyRingTongTheme();
				break;
			case Theme.FONTS:
				applyFontTheme();
				break;
			case Theme.LOCKSCREEN_WALLPAPER:
				applyLockScreenWallpaper();
				break;
		}

	}
	
	public void setWallpaper(Theme theme){
		
	}
	
	public void setStateManager(StateManager stateManager){
		sStateManager = stateManager;
	}
	
	/**
	 * 应用桌面壁纸
	 */
	private void applyWallpaperTheme(){
		Wallpaper wallpaper = (Wallpaper) mTheme;
		String wallpaperFilePath = wallpaper.themeFilePath;
		if(!TextUtils.isEmpty(wallpaperFilePath)){
			sStateManager.postState(State.STATE_START_APPLY);
			boolean success = WallpaperUtils.setDesktopWallpaper(mContext, wallpaperFilePath);
			if(success){
				SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, wallpaperFilePath);
			}
			sStateManager.postState(success?State.STATE_APPLIED:State.STATE_FAIL);
		}
	}
	
	/**
	 * 应用字体
	 */
	private void applyFontTheme(){
		
	}
	
	/**
	 * 应用铃声
	 */
	private void applyRingTongTheme(){
		
	}
	
	/**
	 * 应用锁屏壁纸
	 */
	private void applyLockScreenWallpaper(){
		Wallpaper wallpaper = (Wallpaper) mTheme;
		String wallpaperFilePath = wallpaper.themeFilePath;
		if(!TextUtils.isEmpty(wallpaperFilePath)){
			boolean success = WallpaperUtils.setLockScreenWallpaper(mContext, wallpaperFilePath);
			if(success){
				SharePreferenceManager.setStringPreference(mContext, SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, wallpaperFilePath);
			}
			sStateManager.postState(success?State.STATE_APPLIED:State.STATE_FAIL);
		}
	}
	
	/**
	 * 应用主题
	 */
	private void applyPkgTheme(){
		if(mTheme != null){
			/*
			 * 先判断当前主题是不是系统默认主题，如果是系统默认主题的话直接将上一次
			 * 使用的主题清楚即可
			 */
			if(mTheme.isDefaultTheme()){
				startApply();
				FileUtils.deleteDirectoryChildren(Config.THEME_APPLY_DIR);
				SharePreferenceManager.setIntPreference(mContext, SharePreferenceManager.KEY_APPLIED_THEME_ID, mTheme.id);
				File systemWallpaper = new File(Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH);
				if(systemWallpaper.exists() && systemWallpaper.isDirectory()){
					File[] children = systemWallpaper.listFiles();
					if(children != null && children.length > 0){
						String defaultWallpaperPath = children[0].getAbsolutePath();
						WallpaperUtils.setDesktopWallpaper(mContext, defaultWallpaperPath);
						WallpaperUtils.setLockScreenWallpaper(mContext, defaultWallpaperPath);
					}
				}
				endApply();
				return;
			}
			//主题文件路径是否为空
			if(TextUtils.isEmpty(mTheme.themeFilePath)){
				sStateManager.postState(State.STATE_FILE_NOT_EXISTS);
				return;
			}
			File themeFile = new File(mTheme.themeFilePath);
			//主题文件是否存在
			if(!themeFile.exists()){
				sStateManager.postState(State.STATE_FILE_NOT_EXISTS);
				return;
			}
			//准备开始应用
			startApply();
			try {
				File currentThemeDir = new File(Config.THEME_APPLY_DIR);
				if(currentThemeDir.exists()){
					//先对上一个可用的主题进行备份
					backupCurrentTheme();
					FileUtils.deleteDirectoryChildren(Config.THEME_APPLY_DIR);
				}else{
					currentThemeDir.mkdir();
				}
				unzipThemePkgFile(themeFile.getAbsolutePath(), Config.THEME_APPLY_DIR);
				SharePreferenceManager.setIntPreference(mContext, SharePreferenceManager.KEY_APPLIED_THEME_ID, mTheme.id);
			} catch (Exception e) {
				sStateManager.postState(State.STATE_FAIL);
				Log.e(TAG, "apply theme fail:"+e);
				e.printStackTrace();
				rollbackLastTheme();
				
			}
			int density = DensityUtils.getBestDensity();
			String densityDir = DensityUtils.getMatchedDrawableDir(density);
			StringBuilder wallpaperPath = new StringBuilder();
			wallpaperPath.append(Config.THEME_APPLY_WALLPAPER_DIR);
			wallpaperPath.append(densityDir);
			
			File wallpaperDir = new File(wallpaperPath.toString());
			if(wallpaperDir.exists() && wallpaperDir.isDirectory()){
				File[] files = wallpaperDir.listFiles();
				if(files != null && files.length > 0){
					String wallpaper = files[0].getAbsolutePath();
					WallpaperUtils.setDesktopWallpaper(mContext, wallpaper);
				}
			}
			
			File lockScreenWallpaperDir = new File(Config.THEME_APPLY_LOCKSCREEN_WALLPAPER_DIR);
			if(lockScreenWallpaperDir.exists() && lockScreenWallpaperDir.isDirectory()){
				File[] files = lockScreenWallpaperDir.listFiles();
				if(files != null && files.length > 0){
					String wallpaper = files[0].getAbsolutePath();
					WallpaperUtils.setLockScreenWallpaper(mContext, wallpaper);
				}
			}
			mReStartLauncher = true;
			FileUtils.chmodThemeApplyDir();
			//应用结束
			endApply();
		}else{
			sStateManager.postState(State.STATE_FILE_NOT_EXISTS);
		}
		
	}
	
	private void rollbackLastTheme(){
		FileUtils.deleteDirectoryChildren(Config.THEME_APPLY_DIR);
		try {
			FileUtils.copyDirectiory(Config.THEME_APPLY_BACKUP_DIR,Config.THEME_APPLY_DIR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void backupCurrentTheme() throws IOException{
		File backupDir = new File(Config.THEME_APPLY_BACKUP_DIR);
		if(backupDir.exists()){
			FileUtils.deleteDirectoryChildren(Config.THEME_APPLY_BACKUP_DIR);
		}else{
			backupDir.mkdir();
		}
		FileUtils.copyDirectiory(Config.THEME_APPLY_DIR, Config.THEME_APPLY_BACKUP_DIR);
	}
	
	
	private void unzipThemePkgFile(String fileName, String unZipDir) throws Exception {
        File f = new File(unZipDir);

        if (!f.exists()) {
            f.mkdirs();
        }

        BufferedInputStream is = null;
        ZipEntry entry;
        ZipFile zipfile = new ZipFile(fileName);
        Enumeration<?> enumeration = zipfile.entries();
        byte data[] = new byte[FileUtils.FILE_BUFFER_SIZE];

        while (enumeration.hasMoreElements()) {
            entry = ( ZipEntry ) enumeration.nextElement();
			if(DBG)Log.d(TAG,"unzip file->"+entry.getName());
            if (entry.isDirectory()) {
            	String dirName = entry.getName();
                File f1 = new File(unZipDir + "/" + dirName);
                if("previews/".equals(dirName)){
                	continue;
                }
                if (!f1.exists()) {
                    f1.mkdirs();
                }
            } else {
                is = new BufferedInputStream(zipfile.getInputStream(entry));
                int count;
                String name = unZipDir  + entry.getName();
                if(name.contains("description.xml") || name.contains("previews/")){
                	continue;
                }
                RandomAccessFile m_randFile = null;
                File file = new File(name);
                if (file.exists()) {
                    file.delete();
                }

                file.createNewFile();
                m_randFile = new RandomAccessFile(file, "rw");
                int begin = 0;

                while ((count = is.read(data, 0, FileUtils.FILE_BUFFER_SIZE)) != -1) {
                    try {
                        m_randFile.seek(begin);
                    } catch (Exception ex) {
                    }

                    m_randFile.write(data, 0, count);
                    begin = begin + count;
                }

                m_randFile.close();
                is.close();
            }
        }

	}
	
	/**
	 * 用于处理比较耗时的主题应用操作
	 */
	private void startApply(){
		mStartTime = System.currentTimeMillis();
		sStateManager.postState(State.STATE_START_APPLY);
	}
	
//	ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
//	am.forceStopPackage(Config.LAUNCHER_PKG_NAME);
	
	/**
	 * 用于处理需要界面刷新的主题应用操作，这类
	 * 主题包括主题包和字体
	 */
	private void endApply(){
		mEndTime = System.currentTimeMillis();
		try{
		   if(!sStateManager.isFromSetupApp()){
				mContext.sendStickyBroadcast(new Intent(Config.Action.ACTION_THEME_CHANGE));
				ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
//				am.forceStopPackage(Config.LAUNCHER_PKG_NAME);
		   }
		}catch(Exception e){
			
		}
		if(mEndTime - mStartTime < MAX_TIME_LIMIT){
			 mHandler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					sStateManager.postState(State.STATE_APPLY_SUCCESS);
				}
			}, MAX_TIME_LIMIT);
		}else{
			sStateManager.postState(State.STATE_APPLY_SUCCESS);
		}
		
		
	}
	
	public void setContext(Context context) {
		// TODO Auto-generated method stub
		mContext = context;
	}



	

   

}
