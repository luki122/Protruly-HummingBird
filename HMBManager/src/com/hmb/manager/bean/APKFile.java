package com.hmb.manager.bean;

import android.graphics.drawable.Drawable;

public class APKFile {
	String mPackageName;
	String mVersionName;
	int mInstalled;
	int mVersionCode;
	String mFilePath;
	String appLabel;
	Drawable icon;
	long mSize;
	public long getmSize() {
		return mSize;
	}
	public void setmSize(long mSize) {
		this.mSize = mSize;
	}
	public Drawable getIcon() {
		return icon;
	}
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public String getmPackageName() {
		return mPackageName;
	}
	public void setmPackageName(String mPackageName) {
		this.mPackageName = mPackageName;
	}
	public String getmVersionName() {
		return mVersionName;
	}
	public void setmVersionName(String mVersionName) {
		this.mVersionName = mVersionName;
	}
	public int getmInstalled() {
		return mInstalled;
	}
	public void setmInstalled(int mInstalled) {
		this.mInstalled = mInstalled;
	}
	public int getmVersionCode() {
		return mVersionCode;
	}
	public void setmVersionCode(int mVersionCode) {
		this.mVersionCode = mVersionCode;
	}
	public String getmFilePath() {
		return mFilePath;
	}
	public void setmFilePath(String mFilePath) {
		this.mFilePath = mFilePath;
	}
	public String getAppLabel() {
		return appLabel;
	}
	public void setAppLabel(String appLabel) {
		this.appLabel = appLabel;
	}
	
}
