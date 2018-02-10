package com.hb.thememanager.utils;

import hb.utils.DisplayUtils;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

import com.hb.thememanager.R;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.content.ComponentName;
import android.view.WindowManager;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;
import android.service.wallpaper.WallpaperService;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import com.hb.thememanager.model.VrWallpaperInfo;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;

public class WallpaperUtils {
	private  static final String TAG = "ThemeManager";
	
	
	
	public static boolean setDesktopWallpaper(Context context,String wallpaperPath){
		final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();

		FileOutputStream currentOut = null;
		InputStream input = null;
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);

		    Bitmap bitmap = BitmapFactory.decodeFile(wallpaperPath, new BitmapFactory.Options());
			int width = bitmap.getWidth();  
	        int height = bitmap.getHeight();
	        float scaleWidth = ((float) dm.widthPixels) / width;
	        float scaleHeight = ((float) dm.heightPixels) / height;
	        Matrix matrix = new Matrix();  
	        matrix.postScale(scaleWidth, scaleHeight);// 缩放  
	        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	        
	        //wallpaperManager.setBitmap(bitmap);		//设置速度慢不使用

			File currentPath = new File(Config.THEME_APPLY_LOCKSCREEN_WALLPAPER);
			if(!currentPath.exists()) {
				currentPath.mkdirs();
			}
			File currentWallpaper = new File(currentPath, "desk_wallpaper");
			currentOut = new FileOutputStream(currentWallpaper);
			
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, currentOut);
			input = FileUtils.getStreamFromFile(currentWallpaper.getAbsolutePath(), false);

			if (input != null) {
				wallpaperManager.setStream(input);
				if (Config.DEBUG) {
					Log.d(TAG, "setDeskwallpaper---->" + wallpaperPath);
				}
				input.close();
				input = null;
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "set wallpaper catched exception:" + e);
		} finally {
			if(currentOut != null){
				try{currentOut.flush();}catch(Exception e){}
				try{currentOut.close();}catch(Exception e){}
			}
		}
		return false;
		
//		final android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance(context);
//    	final String path = wallpaperPath;
//		InputStream input = null;
//		try {
//			if (Config.DEBUG) {
//				Log.d(TAG, "setDeskwallpaper---->" + path);
//			}
//			input = FileUtils.getStreamFromFile(path,
//					false);
//			if (input != null) {
//				wallpaperManager.setStream(input);
//				input.close();
//				input = null;
//			}
//			input = FileUtils.getStreamFromFile(path,
//					false);
//			if (input != null) {
//				Bitmap bitmap = BitmapFactory.decodeStream(input);
//				input.close();
//				input = null;
//				int width = DisplayUtils.getWidthPixels(context);
//				int height = DisplayUtils.getHeightPixels(context);
//				if (bitmap != null) {
//					final int wallpaperWidthBefore = bitmap.getWidth();
//					final int wallpaperHeightBefore = bitmap.getHeight();
//					if (wallpaperWidthBefore < wallpaperHeightBefore) {
//						wallpaperManager
//								.suggestDesiredDimensions(width, height);
//					} else {
//						wallpaperManager.suggestDesiredDimensions(2 * width,
//								height);
//					}
//				}
//				if (bitmap != null && !bitmap.isRecycled()) {
//					bitmap.recycle();
//				}
//			}
//			return true;
//		} catch (Exception e) {
//			Log.e(TAG, "set wallpaper catched exception:" + e);
//		}
//    	
//		return false;
	}

	public static boolean setLockScreenWallpaper(Context context,Bitmap bitmap){
		FileOutputStream currentOut = null;
		try {
			File currentPath = new File(Config.THEME_APPLY_LOCKSCREEN_WALLPAPER);
			if(!currentPath.exists()) {
				currentPath.mkdirs();
			}
			File currentWallpaper = new File(currentPath,Config.LOCKSCREEN_WALLPAPER_FILENAME);
			currentOut = new FileOutputStream(currentWallpaper);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, currentOut);
			Intent intent = IntentUtils.buildSetLockScreenWallpaperBroadcast(currentWallpaper.getAbsolutePath());
			context.sendBroadcast(intent);
			WallpaperManager.getInstance(context).forgetLoadedWallpaper();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "set wallpaper catched exception:" + e);
		} finally {
			if(currentOut != null){
				try{currentOut.flush();}catch(Exception e){}
				try{currentOut.close();}catch(Exception e){}
			}
		}
		return false;
	}
	public static boolean setLockScreenWallpaper(Context context,InputStream input){
		boolean ret = false;
		try {
			if (input != null) {
				Bitmap bitmap = BitmapFactory.decodeStream(input);
				ret = setLockScreenWallpaper(context,bitmap);
				if (bitmap != null && !bitmap.isRecycled()) {
					bitmap.recycle();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "set wallpaper catched exception:" + e);
		}

		return ret;
	}
	public static boolean setLockScreenWallpaper(Context context,String path){
		boolean ret = false;
		try {
			if (Config.DEBUG) {
				Log.d(TAG, "setLockScreenWallpaper---->" + path);
			}
			if (!TextUtils.isEmpty(path)) {
				ret = setLockScreenWallpaper(context,new FileInputStream(path));
			}
		} catch (Exception e) {
			Log.e(TAG, "set wallpaper catched exception:" + e);
		}

		return ret;
	}
	
	
public static ArrayList<VrWallpaperInfo> findVrWallpapers(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentServices(
                new Intent(WallpaperService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);

        Collections.sort(list, new Comparator<ResolveInfo>() {
            public int compare(ResolveInfo info1, ResolveInfo info2) {
                Collator collator = Collator.getInstance();
                return collator.compare(info1.loadLabel(packageManager),
                        info2.loadLabel(packageManager));
            }
        });

        ArrayList<VrWallpaperInfo> wallpaperInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : list) {
        	if(!resolveInfo.system) {
        		continue;
        	}
            WallpaperInfo info;
            try {
                info = new WallpaperInfo(context, resolveInfo);
            } catch (Exception e) {
                Log.w(TAG, "Skipping this wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            }
            Drawable thumb = info.loadThumbnail(packageManager);
            String title = (String) info.loadLabel(packageManager);
            ComponentName component = info.getComponent();
            
            int dimen = context.getResources().getDimensionPixelSize(R.dimen.size_vr_wallpaper_item_height);
            dimen += dimen / 10;
            VrWallpaperInfo wallpaperInfo = new VrWallpaperInfo(BitmapUtils.zoom(BitmapUtils.drawable2bitmap(thumb), dimen, dimen), title, component);
            wallpaperInfos.add(wallpaperInfo);
        }
        return wallpaperInfos;
    }

}
