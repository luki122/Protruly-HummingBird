package com.hb.thememanager.model;

import android.content.ComponentName;
import android.graphics.Bitmap;

public class VrWallpaperInfo {
    private Bitmap mThumb;
    private String mTitle;
    private ComponentName mComponent;
    
	public VrWallpaperInfo(Bitmap mThumb, String mTitle,
			ComponentName mComponent) {
		this.mThumb = mThumb;
		this.mTitle = mTitle;
		this.mComponent = mComponent;
	}

	public Bitmap getmThumb() {
		return mThumb;
	}

	public void setmThumb(Bitmap mThumb) {
		this.mThumb = mThumb;
	}

	public String getmTitle() {
		return mTitle;
	}

	public void setmTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public ComponentName getmComponent() {
		return mComponent;
	}

	public void setmComponent(ComponentName mComponent) {
		this.mComponent = mComponent;
	}

	@Override
	public String toString() {
		return "VrWallpaperInfo [mThumb=" + mThumb + ", mTitle=" + mTitle
				+ ", mComponent=" + mComponent + "]";
	}
}
