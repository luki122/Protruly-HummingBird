package com.hb.thememanager.utils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.SetDesktopWallpaperActivity;
import com.hb.thememanager.ui.SetLockScreenWallpaperActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class IntentUtils {
	public static final int REQUEST_CODE_ADD_DESKTOP_WALLPAPER = 100;
	public static final int REQUEST_CODE_ADD_LOCKSCREEN_WALLPAPER = 101;
	public static final String IMAGE_TYPE="image/*";
	public static final String LOCKSCREEN_WALLPAPER_KEY = "lockscreen_wallpaper";
	public static final String ACTION_SET_LOCKSCREEN_WALLPAPER = "com.hb.thememanager.intent.ACTION_SET_LOCKSCREEN_WALLPAPER";
	public static Intent buildPickerDesktopWallpaperIntent(){
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setType(IMAGE_TYPE);
		return intent;
	}

	public static Intent buildSetLockScreenWallpaperBroadcast(String path){
		Intent intent = new Intent(ACTION_SET_LOCKSCREEN_WALLPAPER);
		intent.putExtra(LOCKSCREEN_WALLPAPER_KEY,path);
		return intent;
	}

	public static Intent buildCropWallpaperIntent(int type, Uri uri, Uri imageUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 9);
        intent.putExtra("aspectY", 16);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());//输出图片的格式
        intent.putExtra("noFaceDetection", true); // 头像识别
        
		return intent;
	}
	
	public static Intent buildFastEntryIntent(int themeType,String action){
		Intent intent = new Intent(action);
		intent.putExtra(Config.ActionKey.KEY_FAST_ENTRY, themeType);
		
		return intent;
	}

	public static Intent buildJumpIntent(String action, ContentValues parame){
		Intent intent = new Intent(action);
		for(String key : parame.keySet()){
			Object value = parame.get(key);
			if(value instanceof Serializable) {
				intent.putExtra(key,(Serializable)value);
			}
		}
		return intent;
	}
	
	public static Intent buildHomeThemeListIntent(String action,Theme theme){
		Intent intent = new Intent(action);
		intent.putExtra(Config.ActionKey.KEY_HOME_THEME_ITEM, theme);
		return intent;
	}
	
	
}
