package com.hmb.manager.bean;

import android.graphics.drawable.Drawable;

public class AppInfo {
	private String appLabel;
	private Drawable appIcon;
	private String pkgName;
	private int pid;
	private String processName;
	private long pkgSize;
	public long getPkgSize() {
		return pkgSize;
	}

	public void setPkgSize(long pkgSize) {
		this.pkgSize = pkgSize;
	}

	public String getAppLabel() {
		return appLabel;
	}

	public void setAppLabel(String appLabel) {
		this.appLabel = appLabel;
	}

	public Drawable getAppIcon() {
		return appIcon;
	}

	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}

	public String getPkgName() {
		return pkgName;
	}

	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

}
