package com.hb.thememanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.WallpaperUtils;
import android.view.View;
import com.hb.thememanager.R;

public class LocalWallpaperApplyButton extends Button implements View.OnClickListener {
	private Wallpaper mWallpaper;
	private boolean mApplied = false;

	public LocalWallpaperApplyButton(Context context) {
        this(context, null);
	}

	public LocalWallpaperApplyButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(this);
	}
	
	public void setWallpaper(Wallpaper wallpaper) {
		mWallpaper = wallpaper;
		String path = null;
		if(mWallpaper.type == Theme.WALLPAPER) {
			path = SharePreferenceManager.getStringPreference(getContext(), SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, "");
		}else if(mWallpaper.type == Theme.LOCKSCREEN_WALLPAPER) {
			path = SharePreferenceManager.getStringPreference(getContext(), SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, "");
		}
		if(mWallpaper.themeFilePath.equals(path)) {
			setText(R.string.download_state_applied);
			mApplied = true;
		}else {
			setText(R.string.download_state_apply);
			mApplied = false;
		}
	}

	@Override
	public void onClick(View v) {
		if(mApplied) {
			return;
		}else {
			if(mWallpaper.type == Theme.WALLPAPER) {
				WallpaperUtils.setDesktopWallpaper(getContext(), mWallpaper.themeFilePath);
				SharePreferenceManager.setStringPreference(getContext(), SharePreferenceManager.KEY_APPLIED_WALLPAPER_ID, mWallpaper.themeFilePath);
			}else if(mWallpaper.type == Theme.LOCKSCREEN_WALLPAPER) {
				WallpaperUtils.setLockScreenWallpaper(getContext(), mWallpaper.themeFilePath);
				SharePreferenceManager.setStringPreference(getContext(), SharePreferenceManager.KEY_APPLIED_LOCKSCREEN_WALLPAPER_ID, mWallpaper.themeFilePath);
			}
			setText(R.string.download_state_applied);
			mApplied = true;
		}
	}
	
}

